package it.sapienza.mobileproject.entities.dto

data class PlayRequest(
    var debug: Boolean =  false,
    var friendId: String? = null,
    var randomMode: Boolean = false
)