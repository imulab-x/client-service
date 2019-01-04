package io.imulab.astrea.service.client.persistence

import com.mongodb.client.MongoCollection
import com.mongodb.client.model.Filters
import io.imulab.astrea.sdk.client.AstreaClient
import io.imulab.astrea.sdk.oauth.error.OAuthException
import org.bson.Document
import org.bson.conversions.Bson
import org.slf4j.LoggerFactory

/**
 * Persistence interface for managing client data.
 */
interface ClientStorage {

    suspend fun get(id: String): AstreaClient

    suspend fun insert(client: AstreaClient)

    suspend fun update(client: AstreaClient)

    suspend fun delete(id: String)
}

/**
 * MongoDB implementation to [ClientStorage].
 */
class MongoClientStorage(private val mongoCollection: MongoCollection<Document>) : ClientStorage {

    private val logger = LoggerFactory.getLogger(MongoClientStorage::class.java)

    override suspend fun get(id: String): AstreaClient {
        val doc = mongoCollection.find(byId(id)).singleOrNull()

        if (doc == null) {
            val message = "Client not found by id $id."
            logger.debug(message)
            throw OAuthException(404, "unknown_client", message, mapOf(
                "Cache-Control" to "no-store",
                "Pragma" to "no-cache"
            ))
        }

        return doc.toAstreaClient()
    }

    override suspend fun insert(client: AstreaClient) {
        mongoCollection.insertOne(client.toDocument())

        logger.debug("Inserted client by id ${client.id}.")
    }

    override suspend fun update(client: AstreaClient) {
        val replaced = mongoCollection.findOneAndReplace(byId(client.id), client.toDocument())

        if (replaced == null)
            logger.debug("Client by id ${client.id} is not found, thus not replaced.")
        else
            logger.debug("Client by id ${client.id} is replaced with new version.")
    }

    override suspend fun delete(id: String) {
        val result = mongoCollection.deleteOne(byId(id))

        if (result.deletedCount == 0L)
            logger.debug("No client by id $id was deleted.")
        else
            logger.debug("Client by id $id was deleted.")
    }

    private val byId: (String) -> Bson = { id -> Filters.eq("_id", id) }
}