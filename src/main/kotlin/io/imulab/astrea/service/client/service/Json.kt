package io.imulab.astrea.service.client.service

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import io.imulab.astrea.sdk.client.AstreaClient
import io.imulab.astrea.sdk.client.DefaultClient
import io.imulab.astrea.sdk.oauth.reserved.AuthenticationMethod
import io.imulab.astrea.sdk.oauth.reserved.ClientType
import io.imulab.astrea.sdk.oauth.token.JwtSigningAlgorithm
import io.imulab.astrea.sdk.oidc.reserved.ApplicationType
import io.imulab.astrea.sdk.oidc.reserved.JweContentEncodingAlgorithm
import io.imulab.astrea.sdk.oidc.reserved.JweKeyManagementAlgorithm

@JsonInclude(JsonInclude.Include.NON_EMPTY)
class AstreaClientJsonAdapter @JsonCreator constructor() {

    @get:JsonProperty("client_id") var id: String = ""
    @get:JsonProperty("client_name") var clientName: String = ""
    @get:JsonProperty("client_type") var clientType: String = ClientType.confidential
    @get:JsonProperty("redirect_uris") var redirectUris: Set<String> = emptySet()
    @get:JsonProperty("response_types") var responseTypes: Set<String> = emptySet()
    @get:JsonProperty("grant_types") var grantTypes: Set<String> = emptySet()
    @get:JsonProperty("scopes") var scopes: Set<String> = emptySet()
    @get:JsonProperty("application_type") var applicationType: String = ApplicationType.web
    @get:JsonProperty("contacts") var contacts: LinkedHashSet<String> = LinkedHashSet()
    @get:JsonProperty("logo_uri") var logoUri: String = ""
    @get:JsonProperty("client_uri") var clientUri: String = ""
    @get:JsonProperty("policy_uri") var policyUri: String = ""
    @get:JsonProperty("tos_uri") var tosUri: String = ""
    @get:JsonProperty("jwks_uri") var jwksUri: String = ""
    @get:JsonProperty("jwks") var jwks: String = ""
    @get:JsonProperty("sector_identifier_uri") var sectorIdentifierUri: String = ""
    @get:JsonProperty("subject_type") var subjectType: String = ""
    @get:JsonProperty("id_token_signed_response_alg") var idTokenSignedResponseAlg: String = JwtSigningAlgorithm.RS256.spec
    @get:JsonProperty("id_token_encrypted_response_alg") var idTokenEncryptedResponseAlg: String = JweKeyManagementAlgorithm.None.spec
    @get:JsonProperty("id_token_encrypted_response_enc") var idTokenEncryptedResponseEnc: String = JweContentEncodingAlgorithm.None.spec
    @get:JsonProperty("request_object_signing_alg") var requestObjectSigningAlg: String = JwtSigningAlgorithm.RS256.spec
    @get:JsonProperty("request_object_encryption_alg") var requestObjectEncryptionAlg: String = JweKeyManagementAlgorithm.None.spec
    @get:JsonProperty("request_object_encryption_enc") var requestObjectEncryptionEnc: String = JweContentEncodingAlgorithm.None.spec
    @get:JsonProperty("userinfo_signed_response_alg") var userinfoSignedResponseAlg: String = JwtSigningAlgorithm.None.spec
    @get:JsonProperty("userinfo_encrypted_response_alg") var userinfoEncryptedResponseAlg: String = JweKeyManagementAlgorithm.None.spec
    @get:JsonProperty("userinfo_encrypted_response_enc") var userinfoEncryptedResponseEnc: String = JweContentEncodingAlgorithm.None.spec
    @get:JsonProperty("token_endpoint_auth_method") var tokenEndpointAuthMethod: String = AuthenticationMethod.clientSecretBasic
    @get:JsonProperty("token_endpoint_auth_signing_alg") var tokenEndpointAuthenticationSigningAlg: String = JwtSigningAlgorithm.None.spec
    @get:JsonProperty("default_max_age") var defaultMaxAge: Long = 0
    @get:JsonProperty("require_auth_time") var requireAuthTime: Boolean = false
    @get:JsonProperty("default_acr_values") var defaultAcrValues: List<String> = emptyList()
    @get:JsonProperty("initiate_login_uri") var initiateLoginUri: String = ""
    @get:JsonProperty("request_uris") var requestUris: List<String> = emptyList()

    constructor(client: AstreaClient) : this() {
        id = client.id
        clientName = client.name
        clientType = client.type
        redirectUris = client.redirectUris
        responseTypes = client.responseTypes
        grantTypes = client.grantTypes
        scopes = client.scopes
        applicationType = client.applicationType
        contacts = client.contacts
        logoUri = client.logoUri
        clientUri = client.clientUri
        policyUri = client.policyUri
        tosUri = client.tosUri
        jwksUri = client.jwksUri
        jwks = if (client.jwksUri.isEmpty()) client.jwks else ""
        sectorIdentifierUri = client.sectorIdentifierUri
        subjectType = client.subjectType
        idTokenSignedResponseAlg = client.idTokenSignedResponseAlgorithm.spec
        idTokenEncryptedResponseAlg = client.idTokenEncryptedResponseAlgorithm.spec
        idTokenEncryptedResponseEnc = client.idTokenEncryptedResponseEncoding.spec
        requestObjectSigningAlg = client.requestObjectSigningAlgorithm.spec
        requestObjectEncryptionAlg = client.requestObjectEncryptionAlgorithm.spec
        requestObjectEncryptionEnc = client.requestObjectEncryptionEncoding.spec
        userinfoSignedResponseAlg = client.userInfoSignedResponseAlgorithm.spec
        userinfoEncryptedResponseAlg = client.userInfoEncryptedResponseAlgorithm.spec
        userinfoEncryptedResponseEnc = client.userInfoEncryptedResponseEncoding.spec
        tokenEndpointAuthMethod = client.tokenEndpointAuthenticationMethod
        tokenEndpointAuthenticationSigningAlg = client.tokenEndpointAuthenticationSigningAlgorithm.spec
        defaultMaxAge = client.defaultMaxAge
        requireAuthTime = client.requireAuthTime
        defaultAcrValues = client.defaultAcrValues
        initiateLoginUri = client.initiateLoginUri
        requestUris = client.requestUris
    }

    fun toDefaultClient(): DefaultClient =
        DefaultClient(
            id = this.id,
            clientName = this.clientName,
            clientType = this.clientType,
            redirectUris = this.redirectUris.toMutableSet(),
            responseTypes = this.responseTypes.toMutableSet(),
            grantTypes = this.grantTypes.toMutableSet(),
            scopes = this.scopes.toMutableSet(),
            applicationType = this.applicationType,
            contacts = this.contacts,
            logoUri = this.logoUri,
            clientUri = this.clientUri,
            policyUri = this.policyUri,
            tosUri = this.tosUri,
            jwksUri = this.jwksUri,
            jwks = this.jwks,
            sectorIdentifierUri = this.sectorIdentifierUri,
            subjectType = this.subjectType,
            idTokenSignedResponseAlg = this.idTokenSignedResponseAlg,
            idTokenEncryptedResponseAlg = this.idTokenEncryptedResponseAlg,
            idTokenEncryptedResponseEnc = this.idTokenEncryptedResponseEnc,
            requestObjectSigningAlg = this.requestObjectSigningAlg,
            requestObjectEncryptionAlg = this.requestObjectEncryptionAlg,
            requestObjectEncryptionEnc = this.requestObjectEncryptionEnc,
            userinfoSignedResponseAlg = this.userinfoSignedResponseAlg,
            userinfoEncryptedResponseAlg = this.userinfoEncryptedResponseAlg,
            userinfoEncryptedResponseEnc = this.userinfoEncryptedResponseEnc,
            tokenEndpointAuthMethod = this.tokenEndpointAuthMethod,
            tokenEndpointAuthenticationSigningAlg = this.tokenEndpointAuthenticationSigningAlg,
            defaultMaxAge = this.defaultMaxAge,
            requireAuthTime = this.requireAuthTime,
            defaultAcrValues = this.defaultAcrValues.toMutableList(),
            initiateLoginUri = this.initiateLoginUri,
            requestUris = this.requestUris.toMutableList()
        )
}