package io.imulab.astrea.service.client.persistence

import com.mongodb.client.MongoCollection
import io.imulab.astrea.sdk.client.SampleClients
import io.imulab.astrea.sdk.oauth.error.OAuthException
import io.imulab.astrea.service.client.compareWith
import io.kotlintest.Description
import io.kotlintest.Spec
import io.kotlintest.extensions.TestListener
import io.kotlintest.matchers.collections.shouldContain
import io.kotlintest.should
import io.kotlintest.shouldBe
import io.kotlintest.specs.FeatureSpec
import kotlinx.coroutines.runBlocking
import org.bson.Document

class MongoClientStorageTest : FeatureSpec() {

    var mongoCollection: MongoCollection<Document>? = null

    override fun listeners(): List<TestListener> = listOf(
        object : TestListener {
            override fun beforeSpec(description: Description, spec: Spec) {
                mongoCollection = EmbeddedMongo.start()
            }

            override fun afterSpec(description: Description, spec: Spec) {
                EmbeddedMongo.stop()
            }
        }
    )

    init {
        feature("storage") {
            checkNotNull(mongoCollection)
            val storage = MongoClientStorage(mongoCollection!!)
            val sample = SampleClients.foo

            scenario("should be able to insert a client") {
                val result = runCatching {
                    runBlocking { storage.insert(sample) }
                }

                result.isSuccess shouldBe true
            }

            scenario("should be able to retrieve it") {
                val result = runCatching {
                    runBlocking { storage.get(sample.id) }
                }

                result.isSuccess shouldBe true
                val saved = result.getOrThrow()

                saved compareWith sample
            }

            scenario("and then update it") {
                sample.requestUris = mutableListOf("http://localhost:3000/request")
                sample.requests = mutableMapOf("http://localhost:3000/request" to "some-request")

                val result = runCatching {
                    runBlocking { storage.update(sample) }
                }

                result.isSuccess shouldBe true
            }

            scenario("and then retrieve it") {
                val result = runCatching {
                    runBlocking { storage.get(sample.id) }
                }

                result.isSuccess shouldBe true
                val saved = result.getOrThrow()

                saved compareWith sample
                saved.requestUris shouldContain "http://localhost:3000/request"
                saved.requests.size shouldBe 1
                saved.requests["http://localhost:3000/request"] shouldBe "some-request"
                println(saved)
            }

            scenario("and delete it") {
                val result = runCatching {
                    runBlocking { storage.delete(sample.id) }
                }

                result.isSuccess shouldBe true
            }

            scenario("and then retrieve it should throw exception") {
                val result = runCatching {
                    runBlocking { storage.get(sample.id) }
                }

                result.isFailure shouldBe true
                result.exceptionOrNull() should { it is OAuthException }
            }
        }
    }
}