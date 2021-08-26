package io.beatmaps.cdn

import io.beatmaps.cdn.db.MapTable
import io.beatmaps.cdn.db.VersionTable
import io.beatmaps.common.CDNUpdate
import io.beatmaps.common.consumeAck
import io.beatmaps.common.db.upsert
import io.beatmaps.common.downloadFilename
import io.beatmaps.common.rabbitOptional
import io.ktor.application.Application
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.lang.Integer.toHexString

fun Application.rabbitSub() {
    rabbitOptional {
        consumeAck("cdn.$cdnPrefix", CDNUpdate::class) { _, update ->
            transaction {
                MapTable.upsert(MapTable.id) {
                    it[id] = update.mapId

                    val updateSongName = update.songName
                    val updateLevelAuthorName = update.levelAuthorName
                    if (updateSongName != null && updateLevelAuthorName != null) {
                        it[fileName] = downloadFilename(toHexString(update.mapId), updateSongName, updateLevelAuthorName)
                    }
                    updateSongName?.let { sn -> it[songName] = sn }
                    updateLevelAuthorName?.let { lan -> it[levelAuthorName] = lan }
                    it[deleted] = update.deleted
                }

                // On deletion we won't have a version update
                update.hash?.let { hash ->
                    if (update.published == true) {
                        VersionTable.update({ (VersionTable.mapId eq update.mapId) and (VersionTable.id neq hash) }) {
                            it[published] = false
                        }
                    }

                    VersionTable.upsert(VersionTable.id) {
                        it[id] = hash
                        it[mapId] = update.mapId
                        update.published?.let { pub ->
                            it[published] = pub
                        }
                    }
                }
            }
        }
    }
}