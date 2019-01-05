package io.imulab.astrea.service.client.service

import com.nhaarman.mockitokotlin2.mock
import io.imulab.astrea.sdk.client.SampleClients
import io.imulab.astrea.sdk.discovery.SampleDiscovery
import io.imulab.astrea.sdk.oauth.client.pwd.BCryptPasswordEncoder
import io.imulab.astrea.sdk.oauth.error.OAuthException
import io.imulab.astrea.sdk.oauth.reserved.GrantType
import io.imulab.astrea.sdk.oauth.token.JwtSigningAlgorithm
import io.imulab.astrea.sdk.oidc.reserved.AuthenticationMethod
import io.imulab.astrea.sdk.oidc.reserved.JweContentEncodingAlgorithm
import io.imulab.astrea.sdk.oidc.reserved.JweKeyManagementAlgorithm
import io.imulab.astrea.service.client.Unit
import io.kotlintest.Tag
import io.kotlintest.should
import io.kotlintest.shouldBe
import io.kotlintest.specs.FeatureSpec
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient

class ClientServiceTest : FeatureSpec() {

    override fun tags(): Set<Tag> = setOf(Unit)

    init {
        val service = ClientService(
            clientStorage = mock(),
            discovery = SampleDiscovery.default(),
            okHttpClient = OkHttpClient(),
            passwordEncoder = BCryptPasswordEncoder()
        )

        feature("value generation") {
            scenario("generates password") {
                val client = SampleClients.foo().apply {
                    grantTypes = mutableSetOf(GrantType.authorizationCode)
                    tokenEndpointAuthMethod = io.imulab.astrea.sdk.oauth.reserved.AuthenticationMethod.clientSecretBasic
                }
                val result = runCatching {
                    runBlocking { service.createClient(client)}
                }

                result.isSuccess shouldBe true
                result.getOrNull() should { it != null && it.isNotBlank() }
            }
        }

        feature("client validation") {
            scenario("rejects id_token encryption algorithm non-parity") {
                val client = SampleClients.foo().apply {
                    idTokenEncryptedResponseAlg = JweKeyManagementAlgorithm.RSA1_5.spec
                    idTokenEncryptedResponseEnc = JweContentEncodingAlgorithm.None.spec
                }
                val result = runCatching {
                    runBlocking { service.createClient(client) }
                }

                result.isFailure shouldBe true
                result.exceptionOrNull() shouldBe service.encryptionAlgNonParityError
            }

            scenario("rejects request object encryption algorithm non-parity") {
                val client = SampleClients.foo().apply {
                    requestObjectEncryptionAlg = JweKeyManagementAlgorithm.RSA1_5.spec
                    requestObjectEncryptionEnc = JweContentEncodingAlgorithm.None.spec
                }
                val result = runCatching {
                    runBlocking { service.createClient(client) }
                }

                result.isFailure shouldBe true
                result.exceptionOrNull() shouldBe service.encryptionAlgNonParityError
            }

            scenario("rejects user_info encryption algorithm non-parity") {
                val client = SampleClients.foo().apply {
                    userinfoEncryptedResponseAlg = JweKeyManagementAlgorithm.RSA1_5.spec
                    userinfoEncryptedResponseEnc = JweContentEncodingAlgorithm.None.spec
                }
                val result = runCatching {
                    runBlocking { service.createClient(client) }
                }

                result.isFailure shouldBe true
                result.exceptionOrNull() shouldBe service.encryptionAlgNonParityError
            }

            scenario("rejects unsupported response type") {
                val client = SampleClients.foo().apply {
                    responseTypes = mutableSetOf("custom")
                }
                val result = runCatching {
                    runBlocking { service.createClient(client) }
                }

                result.isFailure shouldBe true
                result.exceptionOrNull() should { it is OAuthException &&
                    it.description.contains("unsupported") &&
                    it.description.contains("response_type")
                }
            }

            scenario("rejects unsupported grant type") {
                val client = SampleClients.foo().apply {
                    grantTypes = mutableSetOf(GrantType.clientCredentials)
                }
                val result = runCatching {
                    runBlocking { service.createClient(client) }
                }

                result.isFailure shouldBe true
                result.exceptionOrNull() should { it is OAuthException &&
                    it.description.contains("unsupported") &&
                    it.description.contains("grant_type")
                }
            }

            scenario("rejects unsupported token auth method") {
                val client = SampleClients.foo().apply {
                    tokenEndpointAuthMethod = AuthenticationMethod.none
                }
                val result = runCatching {
                    runBlocking { service.createClient(client) }
                }

                result.isFailure shouldBe true
                result.exceptionOrNull() should { it is OAuthException &&
                    it.description.contains("unsupported") &&
                    it.description.contains("token_endpoint_auth_method")
                }
            }

            scenario("rejects unsupported token auth signature alg") {
                val client = SampleClients.foo().apply {
                    tokenEndpointAuthenticationSigningAlg = JwtSigningAlgorithm.HS256.spec
                }
                val result = runCatching {
                    runBlocking { service.createClient(client) }
                }

                result.isFailure shouldBe true
                result.exceptionOrNull() should { it is OAuthException &&
                    it.description.contains("unsupported") &&
                    it.description.contains("token_endpoint_auth_signing_alg")
                }
            }

            scenario("rejects unsupported id_token_signed_response_alg") {
                val client = SampleClients.foo().apply {
                    idTokenSignedResponseAlg = JwtSigningAlgorithm.HS256.spec
                }
                val result = runCatching {
                    runBlocking { service.createClient(client) }
                }

                result.isFailure shouldBe true
                result.exceptionOrNull() should { it is OAuthException &&
                    it.description.contains("unsupported") &&
                    it.description.contains("id_token_signed_response_alg")
                }
            }

            scenario("rejects unsupported id_token_encrypted_response_alg") {
                val client = SampleClients.foo().apply {
                    idTokenEncryptedResponseAlg = JweKeyManagementAlgorithm.PBES2_HS256_A128KW.spec
                }
                val result = runCatching {
                    runBlocking { service.createClient(client) }
                }

                result.isFailure shouldBe true
                result.exceptionOrNull() should { it is OAuthException &&
                    it.description.contains("unsupported") &&
                    it.description.contains("id_token_encrypted_response_alg")
                }
            }

            scenario("rejects unsupported id_token_encrypted_response_enc") {
                val client = SampleClients.foo().apply {
                    idTokenEncryptedResponseEnc = JweContentEncodingAlgorithm.A192GCM.spec
                }
                val result = runCatching {
                    runBlocking { service.createClient(client) }
                }

                result.isFailure shouldBe true
                result.exceptionOrNull() should { it is OAuthException &&
                    it.description.contains("unsupported") &&
                    it.description.contains("id_token_encrypted_response_enc")
                }
            }

            scenario("rejects unsupported request_object_signing_alg") {
                val client = SampleClients.foo().apply {
                    requestObjectSigningAlg = JwtSigningAlgorithm.HS256.spec
                }
                val result = runCatching {
                    runBlocking { service.createClient(client) }
                }

                result.isFailure shouldBe true
                result.exceptionOrNull() should { it is OAuthException &&
                    it.description.contains("unsupported") &&
                    it.description.contains("request_object_signing_alg")
                }
            }

            scenario("rejects unsupported request_object_encryption_alg") {
                val client = SampleClients.foo().apply {
                    requestObjectEncryptionAlg = JweKeyManagementAlgorithm.PBES2_HS256_A128KW.spec
                }
                val result = runCatching {
                    runBlocking { service.createClient(client) }
                }

                result.isFailure shouldBe true
                result.exceptionOrNull() should { it is OAuthException &&
                    it.description.contains("unsupported") &&
                    it.description.contains("request_object_encryption_alg")
                }
            }

            scenario("rejects unsupported request_object_encryption_enc") {
                val client = SampleClients.foo().apply {
                    requestObjectEncryptionEnc = JweContentEncodingAlgorithm.A192GCM.spec
                }
                val result = runCatching {
                    runBlocking { service.createClient(client) }
                }

                result.isFailure shouldBe true
                result.exceptionOrNull() should { it is OAuthException &&
                    it.description.contains("unsupported") &&
                    it.description.contains("request_object_encryption_enc")
                }
            }

            scenario("rejects unsupported userinfo_signed_response_alg") {
                val client = SampleClients.foo().apply {
                    userinfoSignedResponseAlg = JwtSigningAlgorithm.HS256.spec
                }
                val result = runCatching {
                    runBlocking { service.createClient(client) }
                }

                result.isFailure shouldBe true
                result.exceptionOrNull() should { it is OAuthException &&
                    it.description.contains("unsupported") &&
                    it.description.contains("userinfo_signed_response_alg")
                }
            }

            scenario("rejects unsupported userinfo_encrypted_response_alg") {
                val client = SampleClients.foo().apply {
                    userinfoEncryptedResponseAlg = JweKeyManagementAlgorithm.PBES2_HS256_A128KW.spec
                }
                val result = runCatching {
                    runBlocking { service.createClient(client) }
                }

                result.isFailure shouldBe true
                result.exceptionOrNull() should { it is OAuthException &&
                    it.description.contains("unsupported") &&
                    it.description.contains("userinfo_encrypted_response_alg")
                }
            }

            scenario("rejects unsupported userinfo_encrypted_response_enc") {
                val client = SampleClients.foo().apply {
                    userinfoEncryptedResponseEnc = JweContentEncodingAlgorithm.A192GCM.spec
                }
                val result = runCatching {
                    runBlocking { service.createClient(client) }
                }

                result.isFailure shouldBe true
                result.exceptionOrNull() should { it is OAuthException &&
                    it.description.contains("unsupported") &&
                    it.description.contains("userinfo_encrypted_response_enc")
                }
            }
        }
    }
}