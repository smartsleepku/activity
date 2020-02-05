package dk.ku.sund.handler

import com.google.gson.GsonBuilder
import dk.ku.sund.database.db
import dk.ku.sund.model.Debug
import dk.ku.sund.model.Token
import io.javalin.Context
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.future.asCompletableFuture
import kotlinx.coroutines.runBlocking
import org.litote.kmongo.*
import org.slf4j.LoggerFactory
import java.text.SimpleDateFormat

private val logger = LoggerFactory.getLogger(DebugHandler::class.java)

class DebugHandler {
    val debugCollection = db.getCollection<Debug>("debugs")

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

    fun create(ctx: Context) {
        val token = token(ctx) ?: return

        val deferred = GlobalScope.async {
            val debug = ctx.body<Debug>()
            debug.userId = token.userId
            debug.id = null

            logger.info("inserted debug: ${gson.toJson(debug)}")

            debugCollection.insertOne(debug)
        }
        ctx.result(deferred.asCompletableFuture())
    }
}
