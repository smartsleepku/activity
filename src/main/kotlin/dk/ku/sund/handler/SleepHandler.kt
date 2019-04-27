package dk.ku.sund.handler

import com.google.gson.GsonBuilder
import dk.ku.sund.database.db
import dk.ku.sund.model.Rest
import dk.ku.sund.model.Sleep
import dk.ku.sund.model.Token
import io.javalin.Context
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.future.asCompletableFuture
import kotlinx.coroutines.runBlocking
import org.litote.kmongo.*
import org.slf4j.LoggerFactory
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.max

private val logger = LoggerFactory.getLogger(SleepHandler::class.java)

class SleepHandler {
    val sleepCollection = db.getCollection<Sleep>("sleeps")
    val restCollection = db.getCollection<Rest>("rests")

    val gson = GsonBuilder()
        .setDateFormat("yyyy-MM-dd'T'HH:mm:ssZ")
        .addSerializationExclusionStrategy(userIdExclusionStrategy)
        .create()

    suspend fun updateRest(userId: String, sleep: Sleep): Any = runBlocking {
        // Find the latest period of rest/unrest for this user
        val latest = restCollection
            .find(Rest::userId eq userId)
            .sort(descending(Rest::startTime))
            .limit(1)
            .toList()
            .firstOrNull()

        if (latest == null) {
            val rest = Rest(null, userId)
            rest.userId = userId
            rest.resting = sleep.sleeping
            rest.startTime = sleep.time
            restCollection.insertOne(rest)
            return@runBlocking
        }

        if (latest.resting == sleep.sleeping) return@runBlocking

        if (latest.endTime == null) {
            latest.endTime = sleep.time
            restCollection.updateOne(Rest::id eq latest.id, latest)
        }
        val rest = Rest(null, userId)
        rest.userId = userId
        rest.startTime = sleep.time
        rest.resting = sleep.sleeping
        restCollection.insertOne(rest)
    }

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
            val sleeps = sleepCollection.find(
                Sleep::userId eq token.userId,
                Sleep::time gte from,
                Sleep::time lte to
            ).sort(ascending(Sleep::time))
            val list = sleeps.toList()
            gson.toJson(list)
        }
        ctx.result(deferred.asCompletableFuture())
    }

    fun create(ctx: Context) {
        val token = token(ctx) ?: return

        val deferred = GlobalScope.async {
            val sleep = ctx.body<Sleep>()
            sleep.userId = token.userId
            sleep.id = null

            logger.info("inserted sleep: ${gson.toJson(sleep)}")

            updateRest(token.userId, sleep)

            sleepCollection.insertOne(sleep)
        }
        ctx.result(deferred.asCompletableFuture())
    }
}