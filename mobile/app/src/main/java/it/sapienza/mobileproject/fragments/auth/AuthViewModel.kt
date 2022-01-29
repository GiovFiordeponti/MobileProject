package it.sapienza.mobileproject.fragments.auth

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import android.util.Patterns

import it.sapienza.mobileproject.R
import it.sapienza.mobileproject.entities.auth.AuthFormState
import it.sapienza.mobileproject.entities.auth.LoginResult

class AuthViewModel() : ViewModel() {

    private val _loginForm = MutableLiveData<AuthFormState>()
    val authFormState: LiveData<AuthFormState> = _loginForm

    private val _loginResult = MutableLiveData<LoginResult>()
    val loginResult: LiveData<LoginResult> = _loginResult

    fun loginDataChanged(username: String, password: String) {
        if (!isUserNameValid(username)) {
            _loginForm.value =
                AuthFormState(
                    usernameError = R.string.invalid_username
                )
        } else if (!isPasswordValid(password)) {
            _loginForm.value =
                AuthFormState(
                    passwordError = R.string.invalid_password
                )

        } else {
            _loginForm.value =
                AuthFormState(
                    isDataValid = true
                )
        }
    }

    fun registerDataChanged(
        username: String,
        password: String,
        confirmPassword: String,
        isChecked: Boolean
    ) {
        if (!isUserNameValid(username)) {
            _loginForm.value =
                AuthFormState(
                    usernameError = R.string.invalid_username
                )
        } else if (!isPasswordValid(password)) {
            _loginForm.value =
                AuthFormState(
                    passwordError = R.string.invalid_password
                )
        } else if (!isPasswordMatch(password, confirmPassword)) {
            _loginForm.value =
                AuthFormState(
                    passwordConfirmError = R.string.password_do_not_match
                )

        } else if (isChecked) {
            _loginForm.value =
                AuthFormState(
                    isDataValid = true
                )
        } else {
            _loginForm.value =
                AuthFormState(
                    isDataValid = false
                )
        }
    }

    // A placeholder username validation check
    private fun isUserNameValid(username: String): Boolean {
        return if (username.contains("@")) {
            Patterns.EMAIL_ADDRESS.matcher(username).matches()
        } else {
            username.isNotBlank()
        }
    }

    // A placeholder password validation check
    private fun isPasswordValid(password: String): Boolean {
        return password.length > 5
    }

    // A placeholder password validation check
    private fun isPasswordMatch(password: String, confirmPassword: String): Boolean {
        return password != "" && password == confirmPassword
    }
}