package it.sapienza.mobileproject.entities.dto

import it.sapienza.mobileproject.entities.room.entity.PongFriend

data class FriendRequest(
    val friendCode: String,
    val pongFriend: PongFriend?
)