package dk.ku.sund.model

import org.bson.codecs.pojo.annotations.BsonId
import java.util.*


class Activity(id: String?, userId: String?) {

    @BsonId
    var id: String? = id
    var userId: String? = userId
    val type: String? = null
    val confidence: Int? = null
    val time: Date? = null
}
