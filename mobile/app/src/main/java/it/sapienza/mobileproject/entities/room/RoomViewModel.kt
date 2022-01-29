package it.sapienza.mobileproject.entities.room

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import it.sapienza.mobileproject.Constants
import it.sapienza.mobileproject.entities.room.dao.PongDao
import it.sapienza.mobileproject.entities.room.entity.PongFriend
import it.sapienza.mobileproject.entities.room.entity.PongNotification
import kotlinx.coroutines.launch

class RoomViewModel(
    val database: PongDao,
    application: Application
) : AndroidViewModel(application) {

    val friendNotifications = database.getNotifications(Constants.REQUEST_TYPE_FRIEND)
    val gameNotifications = database.getNotifications(Constants.REQUEST_TYPE_GAME)
    val acceptedGameNotifications = database.getNotifications(Constants.REQUEST_TYPE_ACCEPT_GAME)

    val friends = database.getFriends()

    val leaderBoardFriends = database.getLeaderBoardFriends()


    fun insertFriends(pongFriends: List<PongFriend>){
        viewModelScope.launch {
            pongFriends.forEach{
                database.insertFriends(it)
            }
        }
    }

    fun deleteNotification(pongNotification: PongNotification){
        viewModelScope.launch {
            database.deleteNotification(notification = pongNotification)
        }
    }





}
