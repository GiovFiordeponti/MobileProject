package it.sapienza.mobileproject.entities.dto

import it.sapienza.mobileproject.entities.room.entity.PongNotification

data class FcmNotification(
   val msg: PongNotification,
   val topic: String
)