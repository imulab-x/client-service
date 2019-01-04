package io.imulab.astrea.service.client.service

import io.imulab.astrea.sdk.client.AstreaClient
import io.imulab.astrea.sdk.client.DefaultClient
import io.imulab.astrea.sdk.oauth.client.pwd.PasswordEncoder
import io.imulab.astrea.sdk.oauth.error.InvalidRequest
import io.imulab.astrea.sdk.oauth.error.OAuthException
import io.imulab.astrea.sdk.oauth.error.ServerError
import io.imulab.astrea.sdk.oauth.reserved.AuthenticationMethod
import io.imulab.astrea.sdk.oauth.reserved.GrantType
import io.imulab.astrea.sdk.oauth.reserved.Param
import io.imulab.astrea.sdk.oauth.reserved.ResponseType
import io.imulab.astrea.sdk.oidc.discovery.Discovery
import io.imulab.astrea.sdk.oidc.reserved.JweContentEncodingAlgorithm
import io.imulab.astrea.sdk.oidc.reserved.JweKeyManagementAlgorithm
import io.imulab.astrea.service.client.persistence.ClientStorage
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.slf4j.LoggerFactory
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.time.LocalDateTime
import java.util.*

class ClientService(
    private val clientStorage: ClientStorage,
    private val passwordEncoder: PasswordEncoder,
    private val discovery: Discovery,
    private val okHttpClient: OkHttpClient
) {

    private val logger = LoggerFactory.getLogger(ClientService::class.java)

    val encryptionAlgNonParityError = InvalidRequest.unmet("Encryption algorithm and encoding must be both provided or both none.")

    /**
     * Creates a client and returns its initial plain text secret, if any.
     */
    suspend fun createClient(client: DefaultClient): String {
        client.id = UUID.randomUUID().toString().replace("-", "").toLowerCase()

        client.creationTime = LocalDateTime.now()
        client.lastUpdateTime = LocalDateTime.now()

        client.clientName = client.clientName.withDefault("Client ${client.id}")

        client.responseTypes = client.responseTypes.withDefault(ResponseType.code)
        client.grantTypes = client.grantTypes.withDefault(GrantType.authorizationCode)

        ensureAlgorithmParity(client.idTokenEncryptedResponseAlgorithm, client.idTokenEncryptedResponseEncoding)
        ensureAlgorithmParity(client.requestObjectEncryptionAlgorithm, client.requestObjectEncryptionEncoding)
        ensureAlgorithmParity(client.userInfoEncryptedResponseAlgorithm, client.userInfoEncryptedResponseEncoding)

        ensureSupported(client)

        val secret = when (client.tokenEndpointAuthMethod) {
            AuthenticationMethod.clientSecretBasic,
            AuthenticationMethod.clientSecretPost,
            io.imulab.astrea.sdk.oidc.reserved.AuthenticationMethod.clientSecretJwt -> {
                val plainSecret = PasswordGenerator.generateAlphaNumericPassword(32)
                client.clientSecret = passwordEncoder.encode(plainSecret)
                plainSecret
            }
            else -> ""
        }

        client.jwks = this.resolveJwks(client.jwksUri, client.jwks)
        client.requests = client.requestUris.filter { it.isNotEmpty() }
            .associate { Pair(it, resolveRequest(it)) }.toMutableMap()

        clientStorage.insert(client)

        return secret
    }

    /**
     * Updates a client.
     */
    suspend fun updateClient(id: String, update: DefaultClient) {
        val original = clientStorage.get(id)

        update.id = original.id
        update.creationTime = original.creationTime
        update.lastUpdateTime = LocalDateTime.now()

        // password change is provided through another endpoint.
        update.clientSecret = original.secret.toString(StandardCharsets.UTF_8)

        ensureAlgorithmParity(update.idTokenEncryptedResponseAlgorithm, update.idTokenEncryptedResponseEncoding)
        ensureAlgorithmParity(update.requestObjectEncryptionAlgorithm, update.requestObjectEncryptionEncoding)
        ensureAlgorithmParity(update.userInfoEncryptedResponseAlgorithm, update.userInfoEncryptedResponseEncoding)

        ensureSupported(update)

        if (update.jwksUri != original.jwksUri && original.jwksUri.isNotEmpty())
            update.jwks = this.resolveJwks(update.jwksUri, update.jwks)

        original.requests.forEach { uri, req ->
            if (update.requestUris.contains(uri))
                update.requests[uri] = req
        }
        update.requestUris.forEach { uri ->
            if (!update.requests.containsKey(uri))
                update.requests[uri] = this.resolveRequest(uri)
        }

        clientStorage.update(update)
    }

    private fun resolveJwks(jwksUri: String, jwks: String): String {
        if (jwksUri.isNotEmpty() && jwks.isNotEmpty())
            throw InvalidRequest.unmet("Only one of jwks or jwks_uri can be used.")

        if (jwksUri.isEmpty())
            return jwks

        val response = try {
            okHttpClient.newCall(Request.Builder().url(jwksUri).build()).execute()
        } catch (e: IOException) {
            logger.error("Error retrieving from $jwksUri.", e)
            throw ServerError.internal("Error retrieving from jwks_uri.")
        }

        if (!response.isSuccessful)
            throw InvalidRequest.unmet("Calling jwks_uri returned non-2xx code.")

        val body = response.body()?.string() ?: throw InvalidRequest.unmet("Calling jwks_uri returned empty body.")
        val hash = HttpUrl.parse(jwksUri)?.fragment() ?: ""
        if (hash.isNotEmpty()) {
            val bodyHash = MessageDigest.getInstance("SHA-256").digest(body.toByteArray()).toString(StandardCharsets.UTF_8)
            if (hash != bodyHash)
                throw InvalidRequest.unmet("Hash from jwks_uri mismatch with body.")
        }

        return body
    }

    private fun resolveRequest(requestUri: String): String {
        if (requestUri.isEmpty())
            return ""

        val response = try {
            okHttpClient.newCall(Request.Builder().url(requestUri).build()).execute()
        } catch (e: IOException) {
            logger.error("Error retrieving from $requestUri.", e)
            throw ServerError.internal("Error retrieving from request_uri.")
        }

        if (!response.isSuccessful)
            throw InvalidRequest.unmet("Calling request_uri returned non-2xx code.")

        val body = response.body()?.string() ?: throw InvalidRequest.unmet("Calling request_uri returned empty body.")
        val hash = HttpUrl.parse(requestUri)?.fragment() ?: ""
        if (hash.isNotEmpty()) {
            val bodyHash = MessageDigest.getInstance("SHA-256").digest(body.toByteArray()).toString(StandardCharsets.UTF_8)
            if (hash != bodyHash)
                throw InvalidRequest.unmet("Hash from request_uri mismatch with body.")
        }

        return body
    }

    private fun ensureAlgorithmParity(alg: JweKeyManagementAlgorithm, enc: JweContentEncodingAlgorithm) {
        val k = if (alg == JweKeyManagementAlgorithm.None) 0 else 1
        val e = if (enc == JweContentEncodingAlgorithm.None) 0 else 1
        if (k + e == 1)
            throw encryptionAlgNonParityError
    }

    private fun ensureSupported(client: AstreaClient) {
        val unsupported: (String) -> Throwable = { param ->
            OAuthException(InvalidRequest.status, InvalidRequest.code, "Value for parameter $param is unsupported.")
        }

        // non-exhaustive list of support checks.
        // --------------------------------------
        // some checks are left to be performed at request time (i.e. request_uris).
        // some other checks are relaxed due to its non-exhaustive nature (i.e. scopes)
        with(discovery) {
            when {
                !responseTypesSupported.containsAll(client.responseTypes) ->
                    throw unsupported(Param.responseType)
                !grantTypesSupported.containsAll(client.grantTypes) ->
                    throw unsupported(Param.grantType)
                !idTokenSigningAlgorithmValuesSupported.contains(client.idTokenSignedResponseAlgorithm.spec) ->
                    throw unsupported("id_token_signed_response_alg")
                !idTokenEncryptionAlgorithmValuesSupported.contains(client.idTokenEncryptedResponseAlgorithm.spec) ->
                    throw unsupported("id_token_encrypted_response_alg")
                !idTokenEncryptionEncodingValuesSupported.contains(client.idTokenEncryptedResponseEncoding.spec) ->
                    throw unsupported("id_token_encrypted_response_enc")
                !requestObjectSigningAlgorithmValuesSupported.contains(client.requestObjectSigningAlgorithm.spec) ->
                    throw unsupported("request_object_signing_alg")
                !requestObjectEncryptionAlgorithmValuesSupported.contains(client.requestObjectEncryptionAlgorithm.spec) ->
                    throw unsupported("request_object_encryption_alg")
                !requestObjectEncryptionEncodingValuesSupported.contains(client.requestObjectEncryptionEncoding.spec) ->
                    throw unsupported("request_object_encryption_enc")
                !userInfoSigningAlgorithmValuesSupported.contains(client.userInfoSignedResponseAlgorithm.spec) ->
                    throw unsupported("userinfo_signed_response_alg")
                !userInfoEncryptionAlgorithmValuesSupported.contains(client.userInfoEncryptedResponseAlgorithm.spec) ->
                    throw unsupported("userinfo_encrypted_response_alg")
                !userInfoEncryptionEncodingValuesSupported.contains(client.userInfoEncryptedResponseEncoding.spec) ->
                    throw unsupported("userinfo_encrypted_response_enc")
                !tokenEndpointAuthenticationMethodsSupported.contains(client.tokenEndpointAuthenticationMethod) ->
                    throw unsupported("token_endpoint_auth_method")
                !tokenEndpointAuthenticationSigningAlgorithmValuesSupported.contains(client.tokenEndpointAuthenticationSigningAlgorithm.spec) ->
                    throw unsupported("token_endpoint_auth_signing_alg")
                else -> {}
            }
        }

    }

    private fun String.withDefault(value: String): String = if (isEmpty()) value else this
    private fun MutableSet<String>.withDefault(vararg value: String): MutableSet<String> =
        if (isEmpty()) value.toMutableSet() else this
}