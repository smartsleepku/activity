import dk.ku.sund.handler.ActivityHandler
import dk.ku.sund.handler.RestHandler
import dk.ku.sund.handler.authorized
import io.javalin.Javalin
import io.javalin.apibuilder.ApiBuilder.*
import org.slf4j.LoggerFactory

fun main(args: Array<String>) {
    val logger = LoggerFactory.getLogger("MainKt")
    val restHandler = RestHandler()
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
    }

    app.start(7000)
}
