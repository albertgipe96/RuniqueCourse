package com.example.auth.presentation.login

import com.example.core.presentation.ui.UiText

sealed interface LoginEvent {
    data class LoginError(val error: UiText) : LoginEvent
    data object LoginSuccess : LoginEvent
}