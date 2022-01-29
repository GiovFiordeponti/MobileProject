package it.sapienza.mobileproject.entities.room.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pongNotification")
data class PongNotification(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "ts") val ts: Long,
    @ColumnInfo(name = "type") val type: String?,
    @ColumnInfo(name = "user") val user: PongFriend?,
    @ColumnInfo(name = "gameInfo") val gameInfo: GameInfo?

)
