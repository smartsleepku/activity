import dk.ku.sund.handler.*
import io.javalin.Javalin
import io.javalin.apibuilder.ApiBuilder.*
import org.slf4j.LoggerFactory

fun main(args: Array<String>) {
    val logger = LoggerFactory.getLogger("MainKt")
    val restHandler = RestHandler()
    val sleepHandler = SleepHandler()
    val heartbeatHandler = HeartbeatHandler()
    val attendeeHandler = AttendeeHandler()
    val debugHandler = DebugHandler()
    val app = Javalin.create()

    app.requestLogger { ctx, timeMs -> logger.info("${ctx.method()} ${ctx.path()} took $timeMs ms") }
    app.before("/*") { ctx -> authorized(ctx) }
    app.before("/*") { ctx -> ctx.header("Content-Type", "Content-Type: application/json; charset=utf-8") }

    app.routes {
        get("/") { ctx -> ctx
            .header("Content-Type", "Content-Type: text/plain; charset=utf-8")
            .result("Nothing to see here")
        }
        crud("/activity/:id", ActivityHandler())
        get("/rest", restHandler::getAll)
        get("/sleep", sleepHandler::getAll)
        get("/heartbeat", heartbeatHandler::getAll)
        post("/sleep", sleepHandler::create)
        post("/sleep/bulk", sleepHandler::createBulk)
        post("/heartbeat", heartbeatHandler::create)
        post("/heartbeat/bulk", heartbeatHandler::createBulk)
        put("/attendee", attendeeHandler::update)
        post("/debug", debugHandler::create)
    }

    app.start(7000)
}
