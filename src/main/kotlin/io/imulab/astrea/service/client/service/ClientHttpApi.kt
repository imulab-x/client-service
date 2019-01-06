package io.imulab.astrea.service.client.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.typesafe.config.Config
import io.imulab.astrea.sdk.oauth.error.InvalidRequest
import io.imulab.astrea.sdk.oauth.error.OAuthException
import io.imulab.astrea.sdk.oauth.error.ServerError
import io.imulab.astrea.service.client.persistence.ClientStorage
import io.vertx.core.http.HttpServerOptions
import io.vertx.core.http.HttpServerResponse
import io.vertx.core.json.Json
import io.vertx.ext.healthchecks.HealthCheckHandler
import io.vertx.ext.healthchecks.Status
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.api.contract.RouterFactoryOptions
import io.vertx.ext.web.api.contract.openapi3.OpenAPI3RouterFactory
import io.vertx.ext.web.api.validation.ValidationException
import io.vertx.kotlin.core.http.listenAwait
import io.vertx.kotlin.core.json.JsonObject
import io.vertx.kotlin.core.json.json
import io.vertx.kotlin.core.json.obj
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import org.slf4j.LoggerFactory
import java.time.ZoneOffset

class ClientHttpApi(
    private val appConfig: Config,
    private val apiMapper: ObjectMapper,
    private val clientService: ClientService,
    private val clientStorage: ClientStorage,
    private val healthCheckHandler: HealthCheckHandler
) : CoroutineVerticle() {

    private val logger = LoggerFactory.getLogger(ClientHttpApi::class.java)

    override suspend fun start() {
        val router = io.vertx.kotlin.ext.web.api.contract.openapi3.OpenAPI3RouterFactory.createAwait(vertx, "client-api-schema.yml").apply {
            options = RouterFactoryOptions().apply {
                isRequireSecurityHandlers = false
                isMountValidationFailureHandler = false
            }

            addFailureHandlerByOperationId("client.create", this@ClientHttpApi::errorHandler)
            addFailureHandlerByOperationId("client.read", this@ClientHttpApi::errorHandler)
            addFailureHandlerByOperationId("client.delete", this@ClientHttpApi::errorHandler)
            addFailureHandlerByOperationId("client.update", this@ClientHttpApi::errorHandler)

            addSuspendHandlerByOperationId("client.create", this@ClientHttpApi::createClient)
            addSuspendHandlerByOperationId("client.read", this@ClientHttpApi::getClient)
            addSuspendHandlerByOperationId("client.delete", this@ClientHttpApi::deleteClient)
            addSuspendHandlerByOperationId("client.update", this@ClientHttpApi::updateClient)
        }.router

        router.get("/health").handler(healthCheckHandler)

        vertx.createHttpServer(HttpServerOptions().apply {
            port = appConfig.getInt("service.restPort")
        }).requestHandler(router).listenAwait()

        logger.info("Client HTTP API server started.")
        healthCheckHandler.register("client_http_api") { h -> h.complete(Status.OK()) }
    }

    private suspend fun createClient(rc: RoutingContext) {
        val client = apiMapper.readValue<AstreaClientJsonAdapter>(rc.bodyAsString).toDefaultClient()
        val plainSecret = clientService.createClient(client)

        // todo: issue access token

        rc.response().setStatusCode(201).applicationJson {
            json {
                val fields = mutableListOf(
                    "client_id" to client.id,
                    "registration_client_uri" to "/client/${client.id}",
                    "client_id_issued_at" to client.creationTime.toEpochSecond(ZoneOffset.UTC),
                    "client_secret_expires_at" to 0
                )
                if (plainSecret.isNotEmpty())
                    fields.add("client_secret" to plainSecret)
                obj(*fields.toTypedArray())
            }
        }

        vertx.eventBus().publish("client.created", JsonObject("id" to client.id))
    }

    private suspend fun updateClient(rc: RoutingContext) {
        val id = rc.pathParam("clientId")
        val update = apiMapper.readValue<AstreaClientJsonAdapter>(rc.bodyAsString).toDefaultClient()

        // todo check authorization

        clientService.updateClient(id, update)
        rc.response().setStatusCode(204).end()

        vertx.eventBus().publish("client.updated", JsonObject("id" to id))
    }

    private suspend fun deleteClient(rc: RoutingContext) {
        val id = rc.pathParam("clientId")

        // todo check authorization

        clientStorage.delete(id)
        rc.response().setStatusCode(204).end()

        vertx.eventBus().publish("client.deleted", JsonObject("id" to id))
    }

    private suspend fun getClient(rc: RoutingContext) {
        val clientId = rc.pathParam("clientId")
        val client = clientStorage.get(clientId)

        rc.response()
            .putHeader("Cache-Control", "no-store")
            .putHeader("Pragma", "no-cache")
            .applicationJson(apiMapper, AstreaClientJsonAdapter(client))
    }

    private fun errorHandler(rc: RoutingContext) {
        val ex = rc.failure()
        when (ex) {
            is OAuthException -> {
                rc.response().apply {
                    statusCode = ex.status
                    ex.headers.forEach { t, u -> putHeader(t, u) }
                    applicationJson(json = ex.data)
                }
            }
            is ValidationException -> {
                rc.response().apply {
                    statusCode = InvalidRequest.status
                    applicationJson {
                        json {
                            obj(
                                "error" to InvalidRequest.code,
                                "error_description" to ex.message
                            )
                        }
                    }
                }
            }
            else -> {
                rc.response().apply {
                    statusCode = ServerError.status
                    applicationJson {
                        json {
                            obj(
                                "error" to ServerError.code,
                                "error_description" to ex.message
                            )
                        }
                    }
                }
            }
        }
    }

    private fun OpenAPI3RouterFactory.addSuspendHandlerByOperationId(operationId: String, block: suspend (RoutingContext) -> Unit): OpenAPI3RouterFactory {
        addHandlerByOperationId(operationId) { rc ->
            val deferred = CoroutineScope(rc.vertx().dispatcher()).async {
                block(rc)
            }
            deferred.invokeOnCompletion { e ->
                if (e != null)
                    rc.fail(e)
            }
        }
        return this
    }

    private fun HttpServerResponse.applicationJson(mapper: ObjectMapper = Json.prettyMapper, json: Any) {
        putHeader("Content-Type", "application/json")
        end(mapper.writeValueAsString(json))
    }

    private fun HttpServerResponse.applicationJson(mapper: ObjectMapper = Json.prettyMapper, block: () -> Any) {
        putHeader("Content-Type", "application/json")
        end(mapper.writeValueAsString(block()))
    }
}