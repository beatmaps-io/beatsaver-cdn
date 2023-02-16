package io.beatmaps.cdn

import io.beatmaps.cdn.db.MapTable
import io.beatmaps.cdn.db.VersionTable
import io.beatmaps.common.localAudioFolder
import io.beatmaps.common.localAvatarFolder
import io.beatmaps.common.localCoverFolder
import io.beatmaps.common.localPlaylistCoverFolder
import io.beatmaps.common.returnFile
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.locations.Location
import io.ktor.server.locations.get
import io.ktor.server.locations.options
import io.ktor.server.plugins.NotFoundException
import io.ktor.server.response.header
import io.ktor.server.routing.Route
import io.ktor.util.pipeline.PipelineContext
import org.jetbrains.exposed.sql.JoinType
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.not
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.File

@Location("/cdn")
class CDN {
    @Location("/{file}.jpg")
    data class Cover(val file: String, val api: CDN)
    @Location("/avatar/{user}.png")
    data class Avatar(val user: String, val api: CDN)
    @Location("/avatar/{user}.jpg")
    data class AvatarSimple(val user: String, val api: CDN)
    @Location("/{file}.mp3")
    data class Audio(val file: String, val api: CDN)
    @Location("/beatsaver/{file}.mp3")
    data class BSAudio(val file: String, val api: CDN)
    @Location("/playlist/{file}.jpg")
    data class PlaylistCover(val file: String, val api: CDN)
    @Location("/playlist/{size}/{file}.jpg")
    data class PlaylistCoverSized(val file: String, val size: Int, val api: CDN)
}

fun Route.cdnRoute() {
    options<CDN.Cover> {
        call.response.header("Access-Control-Allow-Origin", "*")
    }

    get<CDN.Audio> {
        if (it.file.isBlank()) {
            throw NotFoundException()
        }

        getAudio(it.file)
    }

    get<CDN.BSAudio> {
        try {
            transaction {
                MapTable
                    .join(VersionTable, JoinType.FULL, onColumn = MapTable.id, otherColumn = VersionTable.mapId, additionalConstraint = { VersionTable.published eq true })
                    .select {
                        (MapTable.id eq it.file.toInt(16)) and not(MapTable.deleted)
                    }
                    .firstOrNull()?.let { map ->
                        map[VersionTable.id].value
                    }
            }
        } catch (_: NumberFormatException) {
            null
        }?.let {
            getAudio(it)
        } ?: throw NotFoundException()
    }

    get<CDN.Cover> {
        if (it.file.isBlank()) {
            throw NotFoundException()
        }

        call.response.header("Access-Control-Allow-Origin", "*")
        returnFile(File(localCoverFolder(it.file), "${it.file}.jpg"))
    }

    get<CDN.PlaylistCover> {
        if (it.file.isBlank()) {
            throw NotFoundException()
        }

        returnFile(File(localPlaylistCoverFolder(), "${it.file}.jpg"))
    }

    get<CDN.PlaylistCoverSized> {
        if (it.file.isBlank()) {
            throw NotFoundException()
        }

        returnFile(File(localPlaylistCoverFolder(it.size), "${it.file}.jpg"))
    }

    get<CDN.Avatar> {
        returnFile(File(localAvatarFolder(), "${it.user}.png"))
    }

    get<CDN.AvatarSimple> {
        returnFile(File(localAvatarFolder(), "${it.user}.jpg"))
    }
}

suspend fun PipelineContext<Unit, ApplicationCall>.getAudio(hash: String) =
    returnFile(
        File(localAudioFolder(hash), "$hash.mp3")
    )
