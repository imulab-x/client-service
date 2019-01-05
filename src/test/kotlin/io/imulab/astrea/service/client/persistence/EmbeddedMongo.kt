package io.imulab.astrea.service.client.persistence

import com.mongodb.client.MongoClients
import com.mongodb.client.MongoCollection
import de.flapdoodle.embed.mongo.MongodExecutable
import de.flapdoodle.embed.mongo.MongodProcess
import de.flapdoodle.embed.mongo.MongodStarter
import de.flapdoodle.embed.mongo.config.MongodConfigBuilder
import de.flapdoodle.embed.mongo.config.Net
import de.flapdoodle.embed.mongo.distribution.Version
import de.flapdoodle.embed.process.runtime.Network
import org.bson.Document
import java.util.concurrent.locks.ReentrantLock

object EmbeddedMongo {

    private val lock = ReentrantLock()
    private val starter = MongodStarter.getDefaultInstance();
    private var executable: MongodExecutable? = null
    private var process: MongodProcess? = null

    fun start(
        host: String = "localhost",
        port: Int = 27017,
        database: String = "test",
        collection: String = "clients"
    ): MongoCollection<Document> {
        lock.lock()

        return try {
            executable = starter.prepare(MongodConfigBuilder()
                .version(Version.Main.PRODUCTION)
                .net(Net(host, port, Network.localhostIsIPv6()))
                .build())
            process = executable!!.start()

            MongoClients.create("mongodb://$host:$port")
                .getDatabase(database)
                .getCollection(collection)
        } catch (t: Throwable) {
            lock.unlock()
            throw t
        }
    }

    fun stop() {
        lock.unlock()
    }
}