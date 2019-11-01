package dk.ku.sund.model

import org.bson.codecs.pojo.annotations.BsonId
import java.util.*

class Device {
    var deviceId: String? = null
    var deviceType: String? = null
}

class Attendee(id: String?, userId: String?) {
    @BsonId
    var id: String? = id
    var userId: String? = userId
    var gmtOffset: Int? = null
    var weekdayMorning: Date? = null
    var weekdayEvening: Date? = null
    var weekendMorning: Date? = null
    var weekendEvening: Date? = null
    var devices = mutableListOf<Device>()
    var nextPush: Date? = null
}

class AttendeeLog(id: String?, userId: String?) {
    @BsonId
    var id: String? = id
    var userId: String? = userId
    var changedDatetime: Date? = null
    var gmtOffset: Int? = null
    var weekdayMorning: Date? = null
    var weekdayEvening: Date? = null
    var weekendMorning: Date? = null
    var weekendEvening: Date? = null
}
