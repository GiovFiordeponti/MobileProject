package it.sapienza.mobileproject.entities.game

import it.sapienza.mobileproject.Constants


data class Ball(
        var radius: Float = Constants.DEFAULT_BALL_RADIUS,
        var center: Coordinates = Coordinates(Constants.DEFAULT_BALL_CENTER, Constants.DEFAULT_BALL_CENTER),
        var velocity: Coordinates = Coordinates(Constants.DEFAULT_BALL_VELOCITY, Constants.DEFAULT_BALL_VELOCITY)
)
