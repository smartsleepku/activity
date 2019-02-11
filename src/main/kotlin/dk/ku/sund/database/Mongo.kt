package dk.ku.sund.database

import dk.ku.sund.config.databaseConfig
import org.litote.kmongo.coroutine.*
import org.litote.kmongo.reactivestreams.KMongo

private val uri = "mongodb://" +
    (databaseConfig["username"] ?: "") + ":" +
    (databaseConfig["password"] ?: "") + "@" +
    (databaseConfig["hostname"] ?: "db") +
    ":" + (databaseConfig["port"] ?: "27017") +
    "/" + (databaseConfig["db"] ?: "activity") +
    "?authSource=" + (databaseConfig["authSource"] ?: "admin")

val mongoClient = KMongo.createClient(uri).coroutine

val db = mongoClient.getDatabase(databaseConfig["db"] ?: "activity")
