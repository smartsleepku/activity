package dk.ku.sund.model

import org.bson.codecs.pojo.annotations.BsonId
import java.util.*

class Sleep(id: String?, userId: String?) {
    @BsonId
    var id: String? = id
    var userId: String? = userId
    var time: Date? = null
    var sleeping: Boolean? = null
}
