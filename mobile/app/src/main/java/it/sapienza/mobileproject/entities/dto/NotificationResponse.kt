package it.sapienza.mobileproject.entities.dto

data class NotificationResponse(
    val accept: Boolean,
    val friendId: String,
    val ts: Long
)