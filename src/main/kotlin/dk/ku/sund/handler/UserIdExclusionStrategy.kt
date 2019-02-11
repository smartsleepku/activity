package dk.ku.sund.handler

import com.google.gson.ExclusionStrategy
import com.google.gson.FieldAttributes

val userIdExclusionStrategy = object : ExclusionStrategy {
    override fun shouldSkipField(field: FieldAttributes): Boolean =
        field.name == "userId"

    override fun shouldSkipClass(clazz: Class<*>): Boolean = false
}
