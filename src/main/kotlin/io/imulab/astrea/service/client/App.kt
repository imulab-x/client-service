package io.imulab.astrea.service.client

import com.fasterxml.jackson.databind.ObjectMapper
import com.mongodb.BasicDBObject
import com.mongodb.client.MongoClients
import com.mongodb.client.MongoCollection
import com.mongodb.client.MongoDatabase
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import io.github.resilience4j.retry.Retry
import io.github.resilience4j.retry.RetryConfig
import io.grpc.ManagedChannelBuilder
import io.imulab.astrea.sdk.discovery.RemoteDiscoveryService
import io.imulab.astrea.sdk.discovery.SampleDiscovery
import io.imulab.astrea.sdk.oauth.client.pwd.BCryptPasswordEncoder
import io.imulab.astrea.sdk.oauth.client.pwd.PasswordEncoder
import io.imulab.astrea.sdk.oidc.discovery.Discovery
import io.imulab.astrea.service.client.persistence.ClientStorage
import io.imulab.astrea.service.client.persistence.MongoClientStorage
import io.imulab.astrea.service.client.service.ClientGrpcApi
import io.imulab.astrea.service.client.service.ClientHttpApi
import io.imulab.astrea.service.client.service.ClientService
import io.vavr.control.Try
import io.vertx.core.Vertx
import io.vertx.ext.healthchecks.HealthCheckHandler
import io.vertx.ext.healthchecks.Status
import io.vertx.kotlin.coroutines.awaitResult
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import org.bson.Document
import org.kodein.di.Kodein
import org.kodein.di.generic.bind
import org.kodein.di.generic.eagerSingleton
import org.kodein.di.generic.instance
import org.kodein.di.generic.singleton
import org.slf4j.LoggerFactory
import java.time.Duration


private val logger = LoggerFactory.getLogger("io.imulab.astrea.service.client.AppKt")

suspend fun main(args: Array<String>) {
    val vertx = Vertx.vertx()
    val config = ConfigFactory.load()
    val app = App(vertx, config).bootstrap()

    val httpApi by app.instance<ClientHttpApi>()
    val grpcApi by app.instance<ClientGrpcApi>()

    try {
        awaitResult<String> { h -> vertx.deployVerticle(httpApi, h) }
        awaitResult<String> { h -> vertx.deployVerticle(grpcApi, h) }
    } catch (t: Throwable) {
        logger.error("Error deploying API.", t)
        throw t
    }
}

@Suppress("MemberVisibilityCanBePrivate")
open class App(vertx: Vertx, config: Config) {

    open fun bootstrap() = Kodein {
        importOnce(persistence)
        importOnce(discovery)
        importOnce(health)
        importOnce(service)
        importOnce(api)
    }

    val persistence = Kodein.Module("persistence") {
        bind<MongoDatabase>() with singleton {
            val connectionString = "mongodb://${config.getString("mongo.host")}:${config.getInt("mongo.port")}"

            val retry = Retry.of("discovery", RetryConfig.Builder()
                .maxAttempts(5)
                .waitDuration(Duration.ofSeconds(10))
                .retryExceptions(Exception::class.java)
                .build())

            val connect = Retry.decorateSupplier(retry) {
                MongoClients
                    .create(connectionString)
                    .getDatabase(config.getString("mongo.db"))
            }

            Try.ofSupplier(connect).getOrElse { throw IllegalStateException("Could not connect to MongoDB.") }
        }

        bind<MongoCollection<Document>>() with singleton {
            instance<MongoDatabase>().getCollection(config.getString("mongo.collection"))
        }

        bind<ClientStorage>() with singleton { MongoClientStorage(instance()) }
    }

    val health = Kodein.Module("health") {
        bind<HealthCheckHandler>() with singleton {
            HealthCheckHandler.create(vertx).apply {
                register("mongodb") { h ->
                    try {
                        instance<MongoDatabase>().runCommand(BasicDBObject("ping", "1"))
                        h.complete(Status.OK())
                    } catch (t: Throwable) {
                        h.complete(Status.KO())
                    }
                }
            }
        }
    }

    val discovery = Kodein.Module("discovery") {
        bind<Discovery>() with eagerSingleton {
            if (config.getBoolean("discovery.useSample")) {
                logger.info("Using default discovery instead of remote.")
                SampleDiscovery.default()
            } else {
                val channel = ManagedChannelBuilder.forAddress(
                    config.getString("discovery.host"),
                    config.getInt("discovery.port")
                ).enableRetry().maxRetryAttempts(10).usePlaintext().build()

                runBlocking {
                    RemoteDiscoveryService(channel).getDiscovery()
                }
            }
        }
    }

    val service = Kodein.Module("service") {
        bind<PasswordEncoder>() with singleton { BCryptPasswordEncoder() }

        bind<ClientService>() with singleton {
            ClientService(
                clientStorage = instance(),
                passwordEncoder = instance(),
                discovery = instance(),
                okHttpClient = OkHttpClient.Builder()
                    .callTimeout(Duration.ofSeconds(10))
                    .followRedirects(false)
                    .build()
            )
        }
    }

    val api = Kodein.Module("api") {
        bind<ClientHttpApi>() with singleton {
            ClientHttpApi(
                appConfig = config,
                clientStorage = instance(),
                healthCheckHandler = instance(),
                clientService = instance(),
                apiMapper = ObjectMapper()
            )
        }

        bind<ClientGrpcApi>() with singleton {
            ClientGrpcApi(
                appConfig = config,
                healthCheckHandler = instance(),
                clientLookupService = ClientGrpcApi.ClientLookupService(
                    concurrency = Math.max(Runtime.getRuntime().availableProcessors() - 1, 1),
                    clientStorage = instance()
                )
            )
        }
    }
}
