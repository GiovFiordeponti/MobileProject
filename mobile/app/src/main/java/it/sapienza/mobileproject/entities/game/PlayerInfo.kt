package it.sapienza.mobileproject.entities.game

data class PlayerInfo(
    var position: Double = 0.0,
    var label: String = "",
    var box: Box = Box(),
    var ball: Coordinates = Coordinates(0f, 0f),
    var score: Int = 0
)