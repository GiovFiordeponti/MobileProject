package it.sapienza.mobileproject.entities.room.entity

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.android.parcel.Parcelize

@Parcelize
@Entity(tableName = "gameInfo")
data class GameInfo(
    @ColumnInfo(name = "token") val token: String,
    @PrimaryKey val matchId: String,
    @ColumnInfo(name = "playerRole") val playerRole: String
) : Parcelable