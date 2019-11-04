package dk.ku.sund.handler

import com.google.gson.GsonBuilder
import dk.ku.sund.database.db
import dk.ku.sund.model.Heartbeat
import dk.ku.sund.model.Token
import io.javalin.Context
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.future.asCompletableFuture
import kotlinx.coroutines.runBlocking
import org.litote.kmongo.*
import org.slf4j.LoggerFactory
import java.text.SimpleDateFormat

private val logger = LoggerFactory.getLogger(HeartbeatHandler::class.java)

class HeartbeatHandler {
    val heartbeatCollection = db.getCollection<Heartbeat>("heartbeats")

    val gson = GsonBuilder()
        .setDateFormat("yyyy-MM-dd'T'HH:mm:ssZ")
        .addSerializationExclusionStrategy(userIdExclusionStrategy)
        .create()

    fun setDenied(context: Context) = context.status(403)

    fun token(context: Context): Token? {
        val token = context.attribute<Token>("jwt")
        if (token == null) setDenied(context).result("")
        return token
    }

    fun getAll(ctx: Context) {
        val token = token(ctx) ?: return
        val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ")
        val from = format.parse(ctx.queryParam("from"))
        val to = format.parse(ctx.queryParam("to"))

        val deferred = GlobalScope.async {
            val heartbeats = heartbeatCollection.find(
                Heartbeat::userId eq token.userId,
                Heartbeat::time gte from,
                Heartbeat::time lte to
            ).sort(ascending(Heartbeat::time))
            val list = heartbeats.toList()
            gson.toJson(list)
        }
        ctx.result(deferred.asCompletableFuture())
    }

    fun create(ctx: Context) {
        val token = token(ctx) ?: return

        val deferred = GlobalScope.async {
            val heartbeat = ctx.body<Heartbeat>()
            heartbeat.userId = token.userId
            heartbeat.id = null

            logger.info("inserted heartbeat: ${gson.toJson(heartbeat)}")

            heartbeatCollection.insertOne(heartbeat)
        }
        ctx.result(deferred.asCompletableFuture())
    }

    fun createBulk(ctx: Context) {
        val token = token(ctx) ?: return

        val deferred = GlobalScope.async {
            val heartbeats = ctx.body<Array<Heartbeat>>()
            heartbeats.forEach { heartbeat ->
                heartbeat.userId = token.userId
                heartbeat.id = null

                logger.info("inserted heartbeat: ${gson.toJson(heartbeat)}")

                heartbeatCollection.insertOne(heartbeat)
            }
        }
        ctx.result(deferred.asCompletableFuture())

    }
}
