package dk.ku.sund.model

import org.bson.codecs.pojo.annotations.BsonId
import java.util.*

class Rest(id: String?, userId: String?) {
    @BsonId
    var id: String? = id
    var userId: String? = userId
    var resting: Boolean? = null
    var startTime: Date? = null
    var endTime: Date? = null
}
