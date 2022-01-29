package it.sapienza.mobileproject.entities.game

import com.google.firebase.database.IgnoreExtraProperties

@IgnoreExtraProperties
data class Player(
        var label: String = "",
        var position: Double = 0.0,
        var score: Int = 0,
        var ready: Boolean = false,
        var nickname: String = "",
        var friendCode: String = ""
)