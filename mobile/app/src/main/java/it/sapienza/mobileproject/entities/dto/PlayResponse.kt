package it.sapienza.mobileproject.entities.dto

import it.sapienza.mobileproject.entities.room.entity.GameInfo

data class PlayResponse(
    var result: Boolean = false,
    var delete: Boolean = false,
    var created: Boolean = false,
    var info: GameInfo?
)