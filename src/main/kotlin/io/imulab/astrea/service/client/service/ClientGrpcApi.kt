package io.imulab.astrea.service.client.service

import com.typesafe.config.Config
import io.grpc.stub.StreamObserver
import io.imulab.astrea.sdk.client.DefaultClient
import io.imulab.astrea.sdk.client.toClientLookupResponse
import io.imulab.astrea.sdk.commons.client.ClientLookupGrpc
import io.imulab.astrea.sdk.commons.client.ClientLookupRequest
import io.imulab.astrea.sdk.commons.client.ClientLookupResponse
import io.imulab.astrea.sdk.commons.op.Failure
import io.imulab.astrea.sdk.commons.toFailure
import io.imulab.astrea.sdk.oauth.error.OAuthException
import io.imulab.astrea.sdk.oauth.error.ServerError
import io.imulab.astrea.service.client.persistence.ClientStorage
import io.vertx.core.AbstractVerticle
import io.vertx.core.Future
import io.vertx.ext.healthchecks.HealthCheckHandler
import io.vertx.ext.healthchecks.Status
import io.vertx.grpc.VertxServerBuilder
import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread
import kotlin.coroutines.CoroutineContext

class ClientGrpcApi(
    private val appConfig: Config,
    private val clientLookupService: ClientLookupService,
    private val healthCheckHandler: HealthCheckHandler
) : AbstractVerticle() {

    private val logger = LoggerFactory.getLogger(ClientGrpcApi::class.java)

    override fun start(startFuture: Future<Void>?) {
        val server = VertxServerBuilder
            .forPort(vertx, appConfig.getInt("service.grpcPort"))
            .addService(clientLookupService)
            .build()

        Runtime.getRuntime().addShutdownHook(thread(start = false) {
            logger.info("ClientGrpcVerticle shutting down...")
            server.shutdown()
            server.awaitTermination(10, TimeUnit.SECONDS)
        })

        server.start { ar ->
            if (ar.failed()) {
                logger.error("ClientGrpcVerticle failed to start.", ar.cause())
                startFuture?.fail(ar.cause())
            } else {
                startFuture?.complete()
                logger.info("ClientGrpcVerticle started...")
            }
        }

        healthCheckHandler.register("client_grpc_api") { h ->
            if (server.isTerminated)
                h.complete(Status.KO())
            else
                h.complete(Status.OK())
        }
    }

    class ClientLookupService(
        private val concurrency: Int = 4,
        private val clientStorage: ClientStorage
    ) : ClientLookupGrpc.ClientLookupImplBase(), CoroutineScope {

        private val logger = LoggerFactory.getLogger(ClientLookupService::class.java)

        override val coroutineContext: CoroutineContext
            get() = Executors.newFixedThreadPool(concurrency).asCoroutineDispatcher()

        override fun find(request: ClientLookupRequest?, responseObserver: StreamObserver<ClientLookupResponse>?) {
            launch {
                val response = try {
                    clientStorage.get(request?.id ?: "").toClientLookupResponse()
                } catch (e: Exception) {
                    logger.error("ClientLookupService encountered error.", e)
                    ClientLookupResponse.newBuilder()
                        .setSuccess(false)
                        .setFailure(
                            if (e is OAuthException)
                                e.toFailure()
                            else
                                ServerError.wrapped(e).toFailure()
                        )
                        .build()
                }

                responseObserver?.onNext(response)
                responseObserver?.onCompleted()
            }
        }
    }
}