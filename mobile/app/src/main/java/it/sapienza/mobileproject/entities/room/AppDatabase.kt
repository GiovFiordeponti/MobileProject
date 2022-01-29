package it.sapienza.mobileproject.entities.room

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import it.sapienza.mobileproject.Constants
import it.sapienza.mobileproject.entities.room.entity.GameInfo
import it.sapienza.mobileproject.entities.room.dao.PongDao
import it.sapienza.mobileproject.entities.room.entity.PongFriend
import it.sapienza.mobileproject.entities.room.entity.PongNotification


@Database(entities = [PongNotification::class, PongFriend::class, GameInfo::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun pongNotificationDao(): PongDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null
        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context,
                    AppDatabase::class.java, Constants.DATABASE_NAME
                ).build().also { it ->
                    INSTANCE = it
                }
            }
    }

}
