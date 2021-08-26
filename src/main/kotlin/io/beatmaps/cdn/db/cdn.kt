package io.beatmaps.cdn.db

import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Column

object VersionTable : IdTable<String>("version") {
    override val id: Column<EntityID<String>> = char("hash", 40).entityId()
    val mapId = reference("mapId", MapTable)

    val published = bool("published")
}

data class VersionDao(val key: EntityID<String>) : Entity<String>(key) {
    companion object : EntityClass<String, VersionDao>(VersionTable)

    val mapId by VersionTable.mapId
    val published by VersionTable.published
}

object MapTable : IntIdTable("map", "mapId") {
    val fileName = text("fileName").nullable()
    val songName = text("songName")
    val levelAuthorName = text("levelAuthorName")

    val deleted = bool("deleted")
}

data class MapDao(val key: EntityID<Int>) : IntEntity(key) {
    companion object : IntEntityClass<MapDao>(MapTable)

    val fileName by MapTable.fileName
    val songName by MapTable.songName
    val levelAuthorName by MapTable.levelAuthorName

    val deleted by MapTable.deleted
}
