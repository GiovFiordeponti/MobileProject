package it.sapienza.mobileproject.entities.room.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import it.sapienza.mobileproject.Constants
import it.sapienza.mobileproject.entities.room.entity.GameInfo
import it.sapienza.mobileproject.entities.room.entity.PongFriend
import it.sapienza.mobileproject.entities.room.entity.PongNotification

@Dao
interface PongDao {
    /** PONG NOTIFICATION */
    @Query("SELECT * FROM pongNotification WHERE type LIKE :notificationType ORDER BY id DESC")
    fun getNotifications(notificationType: String): LiveData<List<PongNotification>>

    @Query("SELECT * FROM pongNotification WHERE id IN (:userIds) ORDER BY id DESC")
    fun loadAllByIds(userIds: IntArray): LiveData<List<PongNotification>>

    //@Query("SELECT * FROM user WHERE first_name LIKE :first AND " +
    //        "last_name LIKE :last LIMIT 1")
    //fun findByName(first: String, last: String): PongNotificationDaoDao

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertNotifications(vararg notifications: PongNotification)

    @Delete
    suspend fun deleteNotification(notification: PongNotification)

    /** PONG FRIED */
    @Query("SELECT * FROM pongFriend ORDER BY nickname ASC")
    fun getFriends(): LiveData<List<PongFriend>>

    @Query("SELECT * FROM pongFriend ORDER BY statistics_win DESC")
    fun getLeaderBoardFriends(): LiveData<List<PongFriend>>

    @Query("SELECT * FROM pongFriend WHERE id IN (:userIds) ORDER BY nickname ASC")
    fun loadFriendsByIds(userIds: IntArray): LiveData<List<PongFriend>>

    //@Query("SELECT * FROM user WHERE first_name LIKE :first AND " +
    //        "last_name LIKE :last LIMIT 1")
    //fun findByName(first: String, last: String): PongNotificationDaoDao

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFriends(vararg users: PongFriend)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertFriendsUnsuspended(vararg users: PongFriend)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertGameInfoUnsuspended(vararg gameInfo: GameInfo)

    @Delete
    suspend fun deleteFriend(user: PongFriend)
}
