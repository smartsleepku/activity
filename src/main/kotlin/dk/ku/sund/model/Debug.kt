package dk.ku.sund.model

import org.bson.codecs.pojo.annotations.BsonId
import java.util.*

class Debug(id: String?, userId: String?) {
    @BsonId
    var id: String? = id
    var userId: String? = userId
    var time: Date? = null
    var model: String? = null
    var manufacturer: String? = null
    var systemVersion: String? = null
    var systemName: String? = null
}
