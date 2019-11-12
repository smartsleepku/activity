package dk.ku.sund.handler

import com.google.gson.GsonBuilder
import com.mongodb.client.model.FindOneAndReplaceOptions
import com.mongodb.client.model.FindOneAndUpdateOptions
import com.mongodb.client.model.ReturnDocument
import com.mongodb.client.model.UpdateOptions
import dk.ku.sund.database.db
import dk.ku.sund.model.Attendee
import dk.ku.sund.model.AttendeeLog
import dk.ku.sund.model.Token
import io.javalin.Context
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.future.asCompletableFuture
import org.litote.kmongo.eq
import java.time.ZoneId
import java.util.*

class AttendeeHandler {
    val collection = db.getCollection<Attendee>("attendees")
    var collectionLog = db.getCollection<AttendeeLog>("attendeelogs")

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

    fun update(ctx: Context) {
        val token = token(ctx) ?: return
        val attendee = ctx.body<Attendee>()
        val attendeeLog = AttendeeLog(null, null)
        attendee.userId = token.userId
        attendeeLog.id = attendee.id
        attendeeLog.userId = attendee.userId
        attendeeLog.weekdayMorning = attendee.weekdayMorning
        attendeeLog.weekdayEvening = attendee.weekdayEvening
        attendeeLog.weekendMorning = attendee.weekendMorning
        attendeeLog.weekendEvening = attendee.weekendEvening
        attendeeLog.gmtOffset = attendee.gmtOffset
        val deferred = GlobalScope.async {
            attendee.id = collection.findOne(Attendee::userId eq token.userId)?.id

            // Is tomorrow a weekend? (assuming CET timezone since this is a danish study)
            val calendar = Calendar.getInstance(TimeZone.getTimeZone(ZoneId.of( "Europe/Paris" )))
            val weekend = isTomorrowWeekend(calendar)

            calendar.time = nextMorning(weekend, attendee)
            attendee.nextPush = nextPush(calendar)

            var isChanged = true;
            if (attendee.id != null) {
              val curAttendee = collection.findOne(Attendee::userId eq token.userId)
              if (curAttendee?.weekdayMorning == attendeeLog.weekdayMorning &&
                  curAttendee?.weekdayEvening == attendeeLog.weekdayEvening &&
                  curAttendee?.weekendMorning == attendeeLog.weekendMorning &&
                  curAttendee?.weekendEvening == attendeeLog.weekendEvening)
                  isChanged = false;
            }
            if (isChanged) {
              attendeeLog.changedDatetime = Date()
              collectionLog.insertOne(attendeeLog)
            }

            if (attendee.id == null) {
                collection.insertOne(attendee)
            } else {
                collection.updateOne(
                    Attendee::userId eq token.userId,
                    attendee
                )
            }
            attendee
        }
        ctx.result(deferred.asCompletableFuture())
    }

    private fun isTomorrowWeekend(calendar: Calendar): Boolean {
        calendar.time = Date()
        val dow = calendar.get(Calendar.DAY_OF_WEEK)
        val weekend = dow == Calendar.FRIDAY || dow == Calendar.SATURDAY
        return weekend
    }

    private fun nextPush(calendar: Calendar): Date {
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)
        calendar.time = Date()
        calendar.add(Calendar.DATE, 1)
        calendar.set(Calendar.HOUR_OF_DAY, hour)
        calendar.set(Calendar.MINUTE, minute)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.time
    }

    private fun nextMorning(weekend: Boolean, attendee: Attendee): Date {
        val nextMorning: Date
        if (weekend) {
            nextMorning = attendee.weekendMorning!!
        } else {
            nextMorning = attendee.weekdayMorning!!
        }
        return nextMorning
    }
}
