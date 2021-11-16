package io.beatmaps.cdn

import io.beatmaps.cdn.db.MapDao
import io.beatmaps.cdn.db.MapTable
import io.beatmaps.cdn.db.VersionTable
import io.beatmaps.common.DownloadInfo
import io.beatmaps.common.DownloadType
import io.beatmaps.common.downloadFilename
import io.beatmaps.common.localAudioFolder
import io.beatmaps.common.localAvatarFolder
import io.beatmaps.common.localCoverFolder
import io.beatmaps.common.localFolder
import io.beatmaps.common.localPlaylistCoverFolder
import io.beatmaps.common.pub
import io.beatmaps.common.returnFile
import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.features.NotFoundException
import io.ktor.features.origin
import io.ktor.http.HttpHeaders
import io.ktor.locations.Location
import io.ktor.locations.get
import io.ktor.locations.options
import io.ktor.response.header
import io.ktor.routing.Route
import io.ktor.util.pipeline.PipelineContext
import org.jetbrains.exposed.sql.JoinType
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.not
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.io.File
import java.lang.Integer.toHexString

@Location("/cdn")
class CDN {
    @Location("/{file}.zip")
    data class Zip(val file: String, val api: CDN)
    @Location("/{file}.jpg")
    data class Cover(val file: String, val api: CDN)
    @Location("/avatar/{user}.png")
    data class Avatar(val user: Long, val api: CDN)
    @Location("/avatar/{user}.jpg")
    data class AvatarSimple(val user: Long, val api: CDN)
    @Location("/beatsaver/{file}.zip")
    data class BeatSaver(val file: String, val api: CDN)
    @Location("/{file}.mp3")
    data class Audio(val file: String, val api: CDN)
    @Location("/beatsaver/{file}.mp3")
    data class BSAudio(val file: String, val api: CDN)
    @Location("/playlist/{file}.jpg")
    data class PlaylistCover(val file: String, val api: CDN)
}

fun Route.cdnRoute() {
    options<CDN.Zip> {
        call.response.header("Access-Control-Allow-Origin", "*")
    }

    options<CDN.BeatSaver> {
        call.response.header("Access-Control-Allow-Origin", "*")
    }

    get<CDN.Zip> { zipCall ->
        if (zipCall.file.isBlank()) {
            throw NotFoundException()
        }

        val file = File(localFolder(zipCall.file), "${zipCall.file}.zip")
        val name = if (file.exists()) {
            transaction {
                MapDao.wrapRows(
                    MapTable
                        .join(VersionTable, JoinType.FULL, onColumn = MapTable.id, otherColumn = VersionTable.mapId)
                        .select {
                            (VersionTable.id eq zipCall.file) and not(MapTable.deleted)
                        }
                ).firstOrNull()?.let { map ->
                    if (map.fileName == null) {
                        downloadFilename(toHexString(map.id.value), map.songName, map.levelAuthorName).also { newFilename ->
                            MapTable.update({ MapTable.id eq map.id }) {
                                it[fileName] = newFilename
                            }
                        }
                    } else {
                        map.fileName
                    }
                }
            }?.also { _ ->
                call.pub("beatmaps", "download.hash.${zipCall.file}", null, DownloadInfo(zipCall.file, DownloadType.HASH, call.request.origin.remoteHost))
            }
        } else {
            null
        } ?: throw NotFoundException()

        call.response.header("Access-Control-Allow-Origin", "*")
        returnFile(file, name)
    }

    get<CDN.BeatSaver> {
        val res = try {
            transaction {
                MapTable
                    .join(VersionTable, JoinType.FULL, onColumn = MapTable.id, otherColumn = VersionTable.mapId, additionalConstraint = { VersionTable.published eq true })
                    .select {
                        (MapTable.id eq it.file.toInt(16)) and not(MapTable.deleted)
                    }
                    .firstOrNull()?.let { map ->
                        val hash = map[VersionTable.id].value
                        val file = File(localFolder(hash), "$hash.zip")

                        if (file.exists()) {
                            call.pub("beatmaps", "download.key.${it.file}", null, DownloadInfo(it.file, DownloadType.KEY, call.request.origin.remoteHost))
                        }

                        val mapId = map[MapTable.id].value
                        val mapFilename = map[MapTable.fileName] ?: run {
                            downloadFilename(toHexString(map[MapTable.id].value), map[MapTable.songName], map[MapTable.levelAuthorName]).also { newFilename ->
                                MapTable.update({ MapTable.id eq mapId }) {
                                    it[fileName] = newFilename
                                }
                            }
                        }

                        file to mapFilename
                    }
            }
        } catch (_: NumberFormatException) {
            null
        }

        call.response.header(HttpHeaders.AccessControlAllowOrigin, "*")
        returnFile(res?.first, res?.second)
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

        returnFile(File(localCoverFolder(it.file), "${it.file}.jpg"))
    }

    get<CDN.PlaylistCover> {
        if (it.file.isBlank()) {
            throw NotFoundException()
        }

        returnFile(File(localPlaylistCoverFolder(), "${it.file}.jpg"))
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
