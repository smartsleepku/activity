package dk.ku.sund.handler

import com.google.gson.GsonBuilder
import dk.ku.sund.database.db
import dk.ku.sund.model.Activity
import dk.ku.sund.model.Rest
import dk.ku.sund.model.Token
import io.javalin.Context
import io.javalin.apibuilder.CrudHandler
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.future.asCompletableFuture
import kotlinx.coroutines.runBlocking
import org.litote.kmongo.descending
import org.litote.kmongo.eq
import org.slf4j.LoggerFactory
import java.util.*
import java.util.Calendar.MINUTE

private val logger = LoggerFactory.getLogger(ActivityHandler::class.java)

class ActivityHandler: CrudHandler {

    val activityCollection = db.getCollection<Activity>("activities")
    val restCollection = db.getCollection<Rest>("rests")

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

    suspend fun owned(context: Context, activityId: String): Activity? = run {
        val token = token(context)
        val found = activityCollection.findOne(Activity::id eq activityId, Activity::userId eq token?.userId)
        if (found == null) setDenied(context)
        found
    }

    override fun getAll(ctx: Context) {
        val token = token(ctx) ?: return

        val deferred = GlobalScope.async {
            val activities = activityCollection.find(Activity::userId eq token.userId)
            val list = activities.toList()
            gson.toJson(list)
        }
        ctx.result(deferred.asCompletableFuture())
    }

    suspend fun updateRest(userId: String, activity: Activity): Any = runBlocking {
        // Find the latest periods of rest/unrest for this user
        val results = restCollection
            .find(Rest::userId eq userId)
            .sort(descending(Rest::endTime))
            .limit(2)
            .toList()

        var rest: Rest? = results.firstOrNull()
        logger.info("first (of latest) rest: ${gson.toJson(rest)}")

        if (results.count() == 2 && activity.isResting() == false) {
            // Swallow resting periods up inside active periods
            val calendar = Calendar.getInstance()
            calendar.time = activity.time
            calendar.add(MINUTE, -5)
            val boundary = calendar.time
            if (results.first().resting == true &&
                results.last().resting == false &&
                results.last().endTime!! > boundary) {
                logger.info("swallow rest: ${gson.toJson(rest)}")
                restCollection.deleteOne(Rest::id eq results.first().id)
                rest = results.last()
                logger.info("latest rest is now: ${gson.toJson(rest)}")
            }
        }

        if (rest != null && rest.endTime!! >= activity.time && rest.resting == activity.isResting()) {
            // If this can be appended to an existing period of rest/unrest
            val calendar = Calendar.getInstance()
            calendar.time = activity.time
            calendar.add(MINUTE, 5)
            rest.endTime = calendar.time
            logger.info("appended period to rest: ${gson.toJson(rest)}")
            restCollection.updateOne(Rest::id eq rest.id, rest)
        } else {
            if (rest != null) {
                // If a previous rest/unrest period exists, then update its end time
                rest.endTime = activity.time
                logger.info("updated latest rest to now: ${gson.toJson(rest)}")
                restCollection.updateOne(Rest::id eq rest.id, rest)
            }
            // Start a new period of rest/unrest
            rest = Rest(null, userId)
            rest.resting = activity.isResting()
            rest.startTime = activity.time
            val calendar = Calendar.getInstance()
            calendar.time = activity.time
            calendar.add(MINUTE, 5)
            rest.endTime = calendar.time

            logger.info("inserted rest: ${gson.toJson(rest)}")
            restCollection.insertOne(rest)
        }
    }

    override fun create(ctx: Context) {
        val token = token(ctx) ?: return

        val deferred = GlobalScope.async {
            val activity = ctx.body<Activity>()
            activity.userId = token.userId
            activity.id = null

            updateRest(token.userId, activity)
            logger.info("inserted activity: ${gson.toJson(activity)}")

            activityCollection.insertOne(activity)
        }
        ctx.result(deferred.asCompletableFuture())
    }

    override fun delete(ctx: Context, resourceId: String) {
        val deferred = GlobalScope.async {
            val activity = owned(ctx, resourceId)
            if (activity != null) activityCollection.deleteOne(Activity::id eq resourceId)
            ""
        }
        ctx.result(deferred.asCompletableFuture())
    }

    override fun getOne(ctx: Context, resourceId: String) {
        val deferred = GlobalScope.async {
            owned(ctx, resourceId)
        }
        ctx.result(deferred.asCompletableFuture())
    }

    override fun update(ctx: Context, resourceId: String) {
        val token = token(ctx) ?: return
        val deferred = GlobalScope.async {
            owned(ctx, resourceId)
            if (owned(ctx, resourceId) != null) {
                val activity = ctx.body<Activity>()
                activity.id = resourceId
                activity.userId = token.userId
                activityCollection.updateOne(activity.id!!, activity)
                gson.toJson(activity)
            } else {
                ""
            }
        }
        ctx.result(deferred.asCompletableFuture())
    }
}
