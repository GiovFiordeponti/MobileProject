package it.sapienza.mobileproject.entities.dto

import it.sapienza.mobileproject.entities.room.entity.PongFriend
import it.sapienza.mobileproject.entities.room.entity.PongNotification

data class PongUser (
    val friendCode: String,
    val friends: List<String>,
    val matches: List<PongFriendMatch>,
    val friendsDataList: List<PongFriend>,
    val statistics: PongStatistics,
    val money: Int,
    val requests: List<PongNotification>
)