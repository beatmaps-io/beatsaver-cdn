package io.beatmaps.cdn

import io.beatmaps.common.StatusPagesCustom
import io.beatmaps.common.db.setupDB
import io.beatmaps.common.genericQueueConfig
import io.beatmaps.common.installMetrics
import io.beatmaps.common.jackson
import io.beatmaps.common.json
import io.beatmaps.common.rabbitHost
import io.beatmaps.common.setupAMQP
import io.beatmaps.common.setupLogging
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.ContentConverter
import io.ktor.serialization.jackson.JacksonConverter
import io.ktor.serialization.kotlinx.KotlinxSerializationConverter
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.http.content.staticResources
import io.ktor.server.locations.Locations
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.NotFoundException
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.forwardedheaders.XForwardedHeaders
import io.ktor.server.response.respond
import io.ktor.server.routing.routing
import io.ktor.util.reflect.TypeInfo
import io.ktor.utils.io.ByteReadChannel
import org.flywaydb.core.Flyway
import pl.jutupe.ktor_rabbitmq.RabbitMQ
import java.nio.charset.Charset

val port = System.getenv("LISTEN_PORT")?.toIntOrNull() ?: 3030
val host = System.getenv("LISTEN_HOST") ?: "127.0.0.1"
val cdnPrefix = System.getenv("CDN_PREFIX") ?: error("No CDN prefix set")

fun main() {
    setupLogging()
    setupDB("cdn").let { ds ->
        Flyway.configure()
            .dataSource(ds)
            .locations("db")
            .load()
            .migrate()
    }

    embeddedServer(Netty, port = port, host = host, module = Application::cdn).start(wait = true)
}

data class ErrorResponse(val error: String)

fun Application.cdn() {
    installMetrics()

    install(ContentNegotiation) {
        val kotlinx = KotlinxSerializationConverter(json)
        val jsConv = JacksonConverter(jackson)

        register(
            ContentType.Application.Json,
            object : ContentConverter {
                override suspend fun deserialize(charset: Charset, typeInfo: TypeInfo, content: ByteReadChannel) =
                    try {
                        kotlinx.deserialize(charset, typeInfo, content)
                    } catch (e: Exception) {
                        null
                    } ?: jsConv.deserialize(charset, typeInfo, content)

                override suspend fun serializeNullable(contentType: ContentType, charset: Charset, typeInfo: TypeInfo, value: Any?) =
                    try {
                        kotlinx.serializeNullable(contentType, charset, typeInfo, value)
                    } catch (e: Exception) {
                        null
                    } ?: jsConv.serializeNullable(contentType, charset, typeInfo, value)
            }
        )
    }

    install(XForwardedHeaders)

    install(Locations)
    install(StatusPagesCustom) {
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

        staticResources("/static", "assets")
    }
}
