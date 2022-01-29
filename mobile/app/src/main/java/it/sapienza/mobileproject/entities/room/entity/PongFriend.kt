package it.sapienza.mobileproject.entities.room.entity

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import it.sapienza.mobileproject.entities.dto.PongStatistics
import java.util.*

@Entity(tableName = "pongFriend")
data class PongFriend(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "nickname") val nickname: String,
    @ColumnInfo(name = "email") val email: String,
    @ColumnInfo(name = "friendCode") val friendCode: String,
    @Embedded(prefix = "statistics_") val statistics: PongStatistics
)
