package com.example.run.presentation.active_run

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.receiveAsFlow

class ActiveRunViewModel : ViewModel() {

    var state by mutableStateOf(ActiveRunState())
        private set

    private val eventChannel = Channel<ActiveRunEvent>()
    val eventsFlow = eventChannel.receiveAsFlow()

    private val _hasLocationPermission = MutableStateFlow(false) // We want it to start tracking user position when it becomes true

    fun onAction(action: ActiveRunAction) {
        when (action) {
            ActiveRunAction.OnBackClick -> TODO()
            ActiveRunAction.OnFinishRunClick -> TODO()
            ActiveRunAction.OnResumeRunClick -> TODO()
            ActiveRunAction.OnToggleRunClick -> TODO()
            is ActiveRunAction.SubmitLocationPermissionInfo -> {
                _hasLocationPermission.value = action.acceptedLocationPermission
                state = state.copy(showLocationRationale = action.showLocationRationale)
            }
            is ActiveRunAction.SubmitNotificationPermissionInfo -> state = state.copy(showNotificationRationale = action.showNotificationRationale)
            ActiveRunAction.DismissRationaleDialog -> state = state.copy(showLocationRationale = false, showNotificationRationale = false)
        }
    }

}