package io.imulab.astrea.service.client.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.imulab.astrea.sdk.client.AstreaClient
import io.imulab.astrea.sdk.client.SampleClients
import io.imulab.astrea.service.client.Unit
import io.imulab.astrea.service.client.compareWith
import io.kotlintest.Tag
import io.kotlintest.shouldBe
import io.kotlintest.specs.WordSpec
import org.skyscreamer.jsonassert.JSONAssert

class JsonTest : WordSpec() {

    override fun tags(): Set<Tag> = setOf(Unit)

    init {
        val mapper = ObjectMapper()
        val fooJson = """
        {
           "client_id":"foo",
           "client_name":"Foo",
           "client_type":"confidential",
           "redirect_uris":[
              "http://localhost:3000/callback",
              "http://localhost:3001/callback"
           ],
           "response_types":[
              "code",
              "token",
              "id_token"
           ],
           "grant_types":[
              "authorization_code",
              "implicit",
              "refresh_token",
              "client_credentials"
           ],
           "scopes":[
              "foo",
              "bar",
              "offline_access",
              "openid"
           ],
           "application_type":"web",
           "contacts":[
              "foo@imulab.io",
              "bar@imulab.io"
           ],
           "logo_uri":"http://localhost:3000/logo.png",
           "client_uri":"http://localhost:3000/client",
           "policy_uri":"http://localhost:3000/policy",
           "tos_uri":"http://localhost:3000/terms_of_service",
           "subject_type":"public",
           "id_token_signed_response_alg":"RS256",
           "id_token_encrypted_response_alg":"none",
           "id_token_encrypted_response_enc":"none",
           "request_object_signing_alg":"RS256",
           "request_object_encryption_alg":"none",
           "request_object_encryption_enc":"none",
           "userinfo_signed_response_alg":"none",
           "userinfo_encrypted_response_alg":"none",
           "userinfo_encrypted_response_enc":"none",
           "token_endpoint_auth_method":"client_secret_post",
           "token_endpoint_auth_signing_alg":"none",
           "default_max_age":3600,
           "require_auth_time":true
        }
    """.trimIndent()

        "Sample client foo" should {
            "Serialize to JSON properly" {
                val json = mapper.writeValueAsString(AstreaClientJsonAdapter(SampleClients.foo()))
                JSONAssert.assertEquals(fooJson, json, false)
            }
        }

        "JSON of sample client foo" should {
            "Deserialize to JSON properly" {
                val foo: AstreaClient = mapper.readValue<AstreaClientJsonAdapter>(fooJson).toDefaultClient()

                foo compareWith SampleClients.foo()
                foo.secret shouldBe ByteArray(0)
            }
        }
    }
}