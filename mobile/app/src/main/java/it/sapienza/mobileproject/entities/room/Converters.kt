package it.sapienza.mobileproject.entities.room

import androidx.room.TypeConverter
import com.google.gson.Gson
import it.sapienza.mobileproject.entities.room.entity.GameInfo
import it.sapienza.mobileproject.entities.dto.PongStatistics
import it.sapienza.mobileproject.entities.room.entity.PongFriend

class Converters {
    @TypeConverter
    fun fromStatistics(value: String): PongStatistics? {
        return value.split("_")?.let { PongStatistics(it[0].toInt(), it[1].toInt()) }
    }

    @TypeConverter
    fun statisticsToString(statistics: PongStatistics): String? {
        return "${statistics.win}_${statistics.losses}"
    }

    @TypeConverter
    fun pongFriendToString(friend: PongFriend?): String? {
        return if(friend!=null) Gson().toJson(friend) else null
    }

    @TypeConverter
    fun fromPongFriend(value: String?): PongFriend? {
        return if(value!=null && value.isNotEmpty()) Gson().fromJson<PongFriend>(value, PongFriend::class.java) else null
    }

    @TypeConverter
    fun gameInfoToString(gameInfo: GameInfo?): String? {
        return if(gameInfo!=null) Gson().toJson(gameInfo) else null
    }

    @TypeConverter
    fun fromGameInfo(gameInfo: String?): GameInfo? {
        return if(gameInfo!=null && gameInfo.isNotEmpty()) Gson().fromJson<GameInfo>(gameInfo, GameInfo::class.java) else null
    }
}
