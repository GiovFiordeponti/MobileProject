package it.sapienza.mobileproject.entities

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseUser
import it.sapienza.mobileproject.entities.dto.PongStatistics
import it.sapienza.mobileproject.entities.room.entity.GameInfo
import it.sapienza.mobileproject.entities.dto.PongUser
import it.sapienza.mobileproject.entities.room.entity.PongFriend

class PongViewModel : ViewModel() {
    /** writers */
    private val currentUser = MutableLiveData<FirebaseUser>()
    private val pongUser = MutableLiveData<PongUser>()
    private val pongFriends = MutableLiveData<List<PongFriend>>()
    private val nicknameUpdated = MutableLiveData<Boolean>()
    private val gameInfo = MutableLiveData<GameInfo>()
    private val friendNotifications = MutableLiveData<Boolean>()
    private val gameNotifications = MutableLiveData<Boolean>()
    private val pictureUpdated = MutableLiveData<Uri>()
    private val updateFriends = MutableLiveData<Boolean>()
    private val statistics = MutableLiveData<PongStatistics>()
    /** listeners */
    val selectedUser: LiveData<FirebaseUser> get() = currentUser
    val friendSelectedUser: LiveData<FirebaseUser> get() = currentUser
    val homeNicknameUpdated: LiveData<Boolean> get() = nicknameUpdated
    val gameInfoUpdated: LiveData<GameInfo> get() = gameInfo
    val homeFriendNotifications: LiveData<Boolean> get() = friendNotifications
    val fragmentFriendNotifications: LiveData<Boolean> get() = friendNotifications
    val homeGameNotifications: LiveData<Boolean> get() = gameNotifications
    val fragmentGameNotifications: LiveData<Boolean> get() = gameNotifications
    val selectedPongUser: LiveData<PongUser> get() = pongUser
    val selectedPictureUpdated: LiveData<Uri> get() = pictureUpdated
    val selectedPongFriends: LiveData<List<PongFriend>> get() = pongFriends
    val selectedUpdatedFriends: LiveData<Boolean> get() = updateFriends
    val selectedStatistics: LiveData<PongStatistics> get() = statistics

    fun setUser(user: FirebaseUser) {
        currentUser.value = user
    }

    fun setNickNameUpdated(updated: Boolean){
        nicknameUpdated.value = updated
    }

    fun setGameInfo(gameInfo: GameInfo){
        this.gameInfo.value = gameInfo
    }

    fun hasFriendNotifications(hasNotifications: Boolean){
        friendNotifications.value = hasNotifications
    }

    fun hasGameNotifications(hasNotifications: Boolean){
        gameNotifications.value = hasNotifications
    }

    fun setPongUser(newPongUser: PongUser){
        pongUser.value = newPongUser
    }

    fun setPictureUpdated(fileUri: Uri?){
        pictureUpdated.value = fileUri!!
    }

    fun setPongFriends(pongFriendList: List<PongFriend>){
        pongFriends.value = pongFriendList
    }

    fun setUpdateFriends(updated: Boolean){
        updateFriends.value = updated
    }

    fun setStatistics(stats: PongStatistics){
        statistics.value = stats
    }
}
