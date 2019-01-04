package io.imulab.astrea.service.client.persistence

import io.imulab.astrea.sdk.client.AstreaClient
import io.imulab.astrea.sdk.client.DefaultClient
import io.imulab.astrea.sdk.commons.toLocalDateTime
import io.imulab.astrea.sdk.commons.toUnixTimestamp
import org.bson.Document
import java.nio.charset.StandardCharsets
import java.util.*
import kotlin.collections.HashMap
import kotlin.collections.LinkedHashSet

/**
 * Helper to convert from [Document] to [AstreaClient]
 */
fun Document.toAstreaClient(): AstreaClient =
    DefaultClient(
        id = getString(Field.id),
        creationTime = getLong(Field.creationTime).toLocalDateTime(),
        lastUpdateTime = getLong(Field.lastUpdateTime).toLocalDateTime(),
        clientName = getString(Field.clientName),
        clientSecret = getString(Field.clientSecret).fromBase64().utf8(),
        clientType = getString(Field.clientType),
        redirectUris = get(Field.redirectUris, Collection::class.java).toStringSet(),
        responseTypes = get(Field.responseTypes, Collection::class.java).toStringSet(),
        grantTypes = get(Field.grantTypes, Collection::class.java).toStringSet(),
        scopes = get(Field.scopes, Collection::class.java).toStringSet(),
        applicationType = getString(Field.applicationType),
        contacts = LinkedHashSet(get(Field.contacts, Collection::class.java).map { it.toString() }),
        logoUri = getString(Field.logoUri),
        clientUri = getString(Field.clientUri),
        policyUri = getString(Field.policyUri),
        tosUri = getString(Field.tosUri),
        jwksUri = getString(Field.jwksUri),
        jwks = getString(Field.jwks),
        sectorIdentifierUri = getString(Field.sectorIdentifierUri),
        subjectType = getString(Field.subjectType),
        idTokenSignedResponseAlg = getString(Field.idTokenSignedResponseAlg),
        idTokenEncryptedResponseAlg = getString(Field.idTokenEncryptedResponseAlg),
        idTokenEncryptedResponseEnc = getString(Field.idTokenEncryptedResponseEnc),
        requestObjectSigningAlg = getString(Field.requestObjectSigningAlg),
        requestObjectEncryptionAlg = getString(Field.requestObjectEncryptionAlg),
        requestObjectEncryptionEnc = getString(Field.requestObjectEncryptionEnc),
        userinfoSignedResponseAlg = getString(Field.userinfoSignedResponseAlg),
        userinfoEncryptedResponseAlg = getString(Field.userinfoEncryptedResponseAlg),
        userinfoEncryptedResponseEnc = getString(Field.userinfoEncryptedResponseEnc),
        tokenEndpointAuthMethod = getString(Field.tokenEndpointAuthMethod),
        tokenEndpointAuthenticationSigningAlg = getString(Field.tokenEndpointAuthSigningAlg),
        defaultMaxAge = getLong(Field.defaultMaxAge),
        requireAuthTime = getBoolean(Field.requireAuthTime),
        defaultAcrValues = get(Field.defaultAcrValues, Collection::class.java).toStringList(),
        initiateLoginUri = getString(Field.initiateLoginUri),
        requestUris = get(Field.requestUris, Collection::class.java).toStringList(),
        requests = get(Field.requests, Map::class.java).toStringMap()
    )

/**
 * Helper to convert from [AstreaClient] to [Document] for persistence.
 */
fun AstreaClient.toDocument(): Document =
    Document().apply {
        put(Field.id, this@toDocument.id)
        put(Field.creationTime, this@toDocument.creationTime.toUnixTimestamp())
        put(Field.lastUpdateTime, this@toDocument.lastUpdateTime.toUnixTimestamp())
        put(Field.clientName, this@toDocument.name)
        put(Field.clientSecret, this@toDocument.secret.toBase64())
        put(Field.clientType, this@toDocument.type)
        put(Field.redirectUris, this@toDocument.redirectUris)
        put(Field.responseTypes, this@toDocument.responseTypes)
        put(Field.grantTypes, this@toDocument.grantTypes)
        put(Field.scopes, this@toDocument.scopes)
        put(Field.applicationType, this@toDocument.applicationType)
        put(Field.contacts, this@toDocument.contacts)
        put(Field.logoUri, this@toDocument.logoUri)
        put(Field.clientUri, this@toDocument.clientUri)
        put(Field.policyUri, this@toDocument.policyUri)
        put(Field.tosUri, this@toDocument.tosUri)
        put(Field.jwksUri, this@toDocument.jwksUri)
        put(Field.jwks, this@toDocument.jwks)
        put(Field.sectorIdentifierUri, this@toDocument.sectorIdentifierUri)
        put(Field.subjectType, this@toDocument.subjectType)
        put(Field.idTokenSignedResponseAlg, this@toDocument.idTokenSignedResponseAlgorithm.spec)
        put(Field.idTokenEncryptedResponseAlg, this@toDocument.idTokenEncryptedResponseAlgorithm.spec)
        put(Field.idTokenEncryptedResponseEnc, this@toDocument.idTokenEncryptedResponseEncoding.spec)
        put(Field.requestObjectSigningAlg, this@toDocument.requestObjectSigningAlgorithm.spec)
        put(Field.requestObjectEncryptionAlg, this@toDocument.requestObjectEncryptionAlgorithm.spec)
        put(Field.requestObjectEncryptionEnc, this@toDocument.requestObjectEncryptionEncoding.spec)
        put(Field.userinfoSignedResponseAlg, this@toDocument.userInfoSignedResponseAlgorithm.spec)
        put(Field.userinfoEncryptedResponseAlg, this@toDocument.userInfoEncryptedResponseAlgorithm.spec)
        put(Field.userinfoEncryptedResponseEnc, this@toDocument.userInfoEncryptedResponseEncoding.spec)
        put(Field.tokenEndpointAuthMethod, this@toDocument.tokenEndpointAuthenticationMethod)
        put(Field.tokenEndpointAuthSigningAlg, this@toDocument.tokenEndpointAuthenticationSigningAlgorithm.spec)
        put(Field.defaultMaxAge, this@toDocument.defaultMaxAge)
        put(Field.requireAuthTime, this@toDocument.requireAuthTime)
        put(Field.defaultAcrValues, this@toDocument.defaultAcrValues)
        put(Field.initiateLoginUri, this@toDocument.initiateLoginUri)
        put(Field.requestUris, this@toDocument.requestUris)
        put(Field.requests, this@toDocument.requests)
    }

// private extension methods
private fun ByteArray.toBase64(): String = Base64.getEncoder().withoutPadding().encodeToString(this)
private fun String.fromBase64(): ByteArray = Base64.getDecoder().decode(this)
private fun ByteArray.utf8(): String = this.toString(StandardCharsets.UTF_8)
private fun <T> Collection<T>.toStringSet(): MutableSet<String> = this.map { it.toString() }.toMutableSet()
private fun <T> Collection<T>.toStringList(): MutableList<String> = this.map { it.toString() }.toMutableList()
private fun <K, V> Map<K, V>.toStringMap(): MutableMap<String, String> = HashMap<String, String>().apply {
    this@toStringMap.forEach { t, u -> this[t.toString()] = u.toString() }
}

// MongoDB persistence fields.
private object Field {
    const val id = "_id"
    const val creationTime = "create_at"
    const val lastUpdateTime = "update_at"
    const val clientName = "name"
    const val clientSecret = "secret"
    const val clientType = "type"
    const val redirectUris = "redirect_uris"
    const val responseTypes = "response_types"
    const val grantTypes = "grant_types"
    const val scopes = "scopes"
    const val applicationType = "app_ype"
    const val contacts = "contacts"
    const val logoUri = "logo_uri"
    const val clientUri = "client_uri"
    const val policyUri = "policy_uri"
    const val tosUri = "tos_uri"
    const val jwksUri = "jwks_uri"
    const val jwks = "jwks"
    const val sectorIdentifierUri = "sec_id_uri"
    const val subjectType = "subj_type"
    const val idTokenSignedResponseAlg = "id_tok_sig_alg"
    const val idTokenEncryptedResponseAlg = "id_tok_encrypt_alg"
    const val idTokenEncryptedResponseEnc = "id_tok_encrypt_enc"
    const val requestObjectSigningAlg = "req_obj_sig_alg"
    const val requestObjectEncryptionAlg = "req_obj_encrypt_alg"
    const val requestObjectEncryptionEnc = "req_obj_encrypt_enc"
    const val userinfoSignedResponseAlg = "uinfo_sig_alg"
    const val userinfoEncryptedResponseAlg = "uinfo_encrypt_alg"
    const val userinfoEncryptedResponseEnc = "uinfo_encrypt_enc"
    const val tokenEndpointAuthMethod = "tok_auth"
    const val tokenEndpointAuthSigningAlg = "tok_auth_sig_alg"
    const val defaultMaxAge = "max_age"
    const val requireAuthTime = "req_auth"
    const val defaultAcrValues = "acr"
    const val initiateLoginUri = "init_login_uri"
    const val requestUris = "req_uris"
    const val requests = "req"
}