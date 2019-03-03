package dk.ku.sund.handler

import com.google.gson.GsonBuilder
import dk.ku.sund.database.db
import dk.ku.sund.model.Rest
import dk.ku.sund.model.Token
import io.javalin.Context
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.future.asCompletableFuture
import org.litote.kmongo.ascending
import org.litote.kmongo.eq
import org.litote.kmongo.gte
import org.litote.kmongo.lte
import java.text.SimpleDateFormat

class RestHandler {
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

    fun getAll(ctx: Context) {
        val token = token(ctx) ?: return
        val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ")
        val from = format.parse(ctx.queryParam("from"))
        val to = format.parse(ctx.queryParam("to"))

        val deferred = GlobalScope.async {
            val rests = restCollection.find(
                Rest::userId eq token.userId,
                Rest::startTime gte from,
                Rest::endTime lte to
            ).sort(ascending(Rest::startTime))
            val list = rests.toList()
            gson.toJson(list)
        }
        ctx.result(deferred.asCompletableFuture())
    }
}