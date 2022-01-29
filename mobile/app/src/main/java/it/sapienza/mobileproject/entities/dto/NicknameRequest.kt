package it.sapienza.mobileproject.entities.dto

data class NicknameRequest(
    var nickname: String? = null,
    var check: Boolean = true
)