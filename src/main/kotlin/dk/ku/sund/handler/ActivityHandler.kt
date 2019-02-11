package dk.ku.sund.handler

import com.google.gson.GsonBuilder
import dk.ku.sund.database.db
import dk.ku.sund.model.Activity
import dk.ku.sund.model.Token
import io.javalin.Context
import io.javalin.apibuilder.CrudHandler
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.future.asCompletableFuture
import org.litote.kmongo.eq

class ActivityHandler: CrudHandler {

    val collection = db.getCollection<Activity>("activities")

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
        val found = collection.findOne(Activity::id eq activityId, Activity::userId eq token?.userId)
        if (found == null) setDenied(context)
        found
    }

    override fun getAll(ctx: Context) {
        val token = token(ctx) ?: return

        val deferred = GlobalScope.async {
            val activities = collection.find(Activity::userId eq token.userId)
            val list = activities.toList()
            gson.toJson(list)
        }
        ctx.result(deferred.asCompletableFuture())
    }

    override fun create(ctx: Context) {
        val token = token(ctx) ?: return

        val deferred = GlobalScope.async {
            val activity = ctx.body<Activity>()
            activity.userId = token.userId
            activity.id = null
            collection.insertOne(activity)
        }
        ctx.result(deferred.asCompletableFuture())
    }

    override fun delete(ctx: Context, resourceId: String) {
        val deferred = GlobalScope.async {
            val activity = owned(ctx, resourceId)
            if (activity != null) collection.deleteOne(Activity::id eq resourceId)
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
                collection.updateOne(activity.id!!, activity)
                gson.toJson(activity)
            } else {
                ""
            }
        }
        ctx.result(deferred.asCompletableFuture())
    }
}
