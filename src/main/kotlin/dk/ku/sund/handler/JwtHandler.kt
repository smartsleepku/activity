package dk.ku.sund.handler

import com.google.gson.Gson
import dk.ku.sund.model.Token
import io.javalin.Context
import org.slf4j.LoggerFactory
import java.util.*

val logger = LoggerFactory.getLogger("MainKt")

fun authorized(ctx: Context) {
    try {
        val authorization = ctx.req.getHeader("Authorization")
        val borne = authorization.split(" ")[1]
        val payload = borne.split(".")[1]
        val json = String(Base64.getDecoder().decode(payload))

        ctx.attribute("jwt", Gson().fromJson(json, Token::class.java))
    } catch (error: Throwable) {
        logger.error("Failed getting token: ${error}")
    }
}
