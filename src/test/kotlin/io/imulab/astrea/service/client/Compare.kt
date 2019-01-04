package io.imulab.astrea.service.client

import io.imulab.astrea.sdk.client.AstreaClient
import io.kotlintest.matchers.collections.shouldContainAll
import io.kotlintest.shouldBe

infix fun AstreaClient.compareWith(expect: AstreaClient) {
    infix operator fun String.invoke(expect: String) { this shouldBe expect }
    infix operator fun Long.invoke(expect: Long) { this shouldBe expect }
    infix operator fun Boolean.invoke(expect: Boolean) { this shouldBe expect }
    infix operator fun Collection<*>.invoke(expect: Collection<*>) { this shouldContainAll expect }

    this.id(expect.id)
    this.name(expect.name)
    this.type(expect.type)
    this.redirectUris(expect.redirectUris)
    this.responseTypes(expect.responseTypes)
    this.grantTypes(expect.grantTypes)
    this.scopes(expect.scopes)
    this.applicationType(expect.applicationType)
    this.contacts(expect.contacts)
    this.logoUri(expect.logoUri)
    this.clientUri(expect.clientUri)
    this.policyUri(expect.policyUri)
    this.tosUri(expect.tosUri)
    this.subjectType(expect.subjectType)
    this.idTokenSignedResponseAlgorithm.spec(expect.idTokenSignedResponseAlgorithm.spec)
    this.idTokenEncryptedResponseAlgorithm.spec(expect.idTokenEncryptedResponseAlgorithm.spec)
    this.idTokenEncryptedResponseEncoding.spec(expect.idTokenEncryptedResponseEncoding.spec)
    this.requestObjectSigningAlgorithm.spec(expect.requestObjectSigningAlgorithm.spec)
    this.requestObjectEncryptionAlgorithm.spec(expect.requestObjectEncryptionAlgorithm.spec)
    this.requestObjectEncryptionEncoding.spec(expect.requestObjectEncryptionEncoding.spec)
    this.userInfoSignedResponseAlgorithm.spec(expect.userInfoSignedResponseAlgorithm.spec)
    this.userInfoEncryptedResponseAlgorithm.spec(expect.userInfoEncryptedResponseAlgorithm.spec)
    this.userInfoEncryptedResponseEncoding.spec(expect.userInfoEncryptedResponseEncoding.spec)
    this.tokenEndpointAuthenticationMethod(expect.tokenEndpointAuthenticationMethod)
    this.tokenEndpointAuthenticationSigningAlgorithm.spec(expect.tokenEndpointAuthenticationSigningAlgorithm.spec)
    this.defaultMaxAge(expect.defaultMaxAge)
    this.requireAuthTime(expect.requireAuthTime)
}