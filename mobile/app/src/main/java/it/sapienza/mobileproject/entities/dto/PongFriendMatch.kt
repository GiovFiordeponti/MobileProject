package it.sapienza.mobileproject.entities.dto

import it.sapienza.mobileproject.entities.room.entity.GameInfo
import it.sapienza.mobileproject.entities.room.entity.PongFriend

data class PongFriendMatch(
    val gameInfo: GameInfo,
    val friend: PongFriend
)