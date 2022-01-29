package it.sapienza.mobileproject.entities.dto

import it.sapienza.mobileproject.entities.room.entity.GameInfo

data class GameDeleteRequest(
    val gameInfo: GameInfo,
    val win: Boolean,
    val draw: Boolean
)