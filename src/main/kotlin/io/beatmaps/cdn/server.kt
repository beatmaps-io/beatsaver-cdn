package io.beatmaps.cdn

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.beatmaps.common.KotlinTimeModule
import io.beatmaps.common.db.setupDB
import io.beatmaps.common.genericQueueConfig
import io.beatmaps.common.rabbitHost
import io.beatmaps.common.setupAMQP
import io.beatmaps.common.setupLogging
import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.ContentNegotiation
import io.ktor.features.NotFoundException
import io.ktor.features.StatusPages
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.resources
import io.ktor.http.content.static
import io.ktor.jackson.jackson
import io.ktor.locations.Locations
import io.ktor.response.respond
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import pl.jutupe.ktor_rabbitmq.RabbitMQ

val port = System.getenv("LISTEN_PORT")?.toIntOrNull() ?: 3030
val host = System.getenv("LISTEN_HOST") ?: "127.0.0.1"
val cdnPrefix = System.getenv("CDN_PREFIX") ?: error("No CDN prefix set")

fun main() {
    setupLogging()
    setupDB()

    embeddedServer(Netty, port = port, host = host, module = Application::cdn).start(wait = true)
}

data class ErrorResponse(val error: String)

fun Application.cdn() {
    install(ContentNegotiation) {
        jackson {
            enable(SerializationFeature.INDENT_OUTPUT)
            registerModule(JavaTimeModule())
            registerModule(KotlinTimeModule())
            disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            setSerializationInclusion(JsonInclude.Include.NON_NULL)
        }
    }

    install(Locations)
    install(StatusPages) {
        exception<NotFoundException> {
            call.respond(HttpStatusCode.NotFound, ErrorResponse("Not Found"))
        }

        exception<Throwable> { cause ->
            call.respond(HttpStatusCode.InternalServerError, "Internal Server Error")
            throw cause
        }
    }

    if (rabbitHost.isNotEmpty()) {
        install(RabbitMQ) {
            setupAMQP {
                queueDeclare("cdn.$cdnPrefix", true, false, false, genericQueueConfig)
                queueBind("cdn.$cdnPrefix", "beatmaps", "cdn.#")
            }
        }
    }

    rabbitSub()

    routing {
        cdnRoute()

        static("/static") {
            resources()
        }
    }
}
