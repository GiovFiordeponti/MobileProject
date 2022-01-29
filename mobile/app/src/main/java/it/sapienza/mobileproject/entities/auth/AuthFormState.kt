package it.sapienza.mobileproject.entities.auth

/**
 * Data validation state of the login form.
 */
data class AuthFormState(
    val usernameError: Int? = null,
    val passwordError: Int? = null,
    val passwordConfirmError: Int? = null,
    val isDataValid: Boolean = false
)