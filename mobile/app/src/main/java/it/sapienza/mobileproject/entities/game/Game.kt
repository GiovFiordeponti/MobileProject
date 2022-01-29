package it.sapienza.mobileproject.entities.game

import com.google.firebase.database.IgnoreExtraProperties
import it.sapienza.mobileproject.Constants

@IgnoreExtraProperties
data class Game(
        var gameId: String? = "prova",
        var hasStarted: Boolean = false,
        var gameOver: Boolean = false,
        var ready: Boolean = false,
        var playerTurn: String = Constants.DEFAULT_PLAYER,
        var player1: Player = Player(),
        var player2: Player = Player(),
        var maxScore: Int = Constants.MAX_SCORE,
        val isFriendGame: Boolean = false
)