@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3Api::class)

package com.example.run.presentation.active_run

import android.Manifest
import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.core.presentation.designsystem.StartIcon
import com.example.core.presentation.designsystem.StopIcon
import com.example.core.presentation.designsystem.components.RuniqueActionButton
import com.example.core.presentation.designsystem.components.RuniqueDialog
import com.example.core.presentation.designsystem.components.RuniqueFloatingActionButton
import com.example.core.presentation.designsystem.components.RuniqueOutlinedActionButton
import com.example.core.presentation.designsystem.components.RuniqueScaffold
import com.example.core.presentation.designsystem.components.RuniqueToolbar
import com.example.run.presentation.R
import com.example.run.presentation.active_run.components.RunDataCard
import com.example.run.presentation.active_run.maps.TrackerMap
import com.example.run.presentation.active_run.service.ActiveRunService
import com.example.run.presentation.util.hasLocationPermission
import com.example.run.presentation.util.hasNotificationPermission
import com.example.run.presentation.util.shouldShowLocationPermissionRationale
import com.example.run.presentation.util.shouldShowNotificationPermissionRationale
import org.koin.androidx.compose.koinViewModel
import java.io.ByteArrayOutputStream

@Composable
fun ActiveRunScreenRoot(
    viewModel: ActiveRunViewModel = koinViewModel(),
    onServiceToggle: (isServiceRunning: Boolean) -> Unit
) {
    ActiveRunScreen(
        state = viewModel.state,
        onServiceToggle = onServiceToggle,
        onAction = viewModel::onAction
    )
}

@Composable
fun ActiveRunScreen(
    state: ActiveRunState,
    onServiceToggle: (isServiceRunning: Boolean) -> Unit,
    onAction: (ActiveRunAction) -> Unit
) {
    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val hasCoarseLocationPermission = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        val hasFineLocationPermission = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val hasPostNotificationPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions[Manifest.permission.POST_NOTIFICATIONS] == true
        } else true

        val activity = context as ComponentActivity
        val showLocationRationale = activity.shouldShowLocationPermissionRationale()
        val showNotificationRationale = activity.shouldShowNotificationPermissionRationale()

        onAction(ActiveRunAction.SubmitLocationPermissionInfo(
            acceptedLocationPermission = hasCoarseLocationPermission && hasFineLocationPermission,
            showLocationRationale = showLocationRationale
        ))
        onAction(ActiveRunAction.SubmitNotificationPermissionInfo(
            acceptedNotificationPermission = hasPostNotificationPermission,
            showNotificationRationale = showNotificationRationale
        ))
    }

    LaunchedEffect(true) { // If user goes to background without accepting permissions, we want to show rationale again when reopening the app
        val activity = context as ComponentActivity
        val showLocationRationale = activity.shouldShowLocationPermissionRationale()
        val showNotificationRationale = activity.shouldShowNotificationPermissionRationale()

        onAction(ActiveRunAction.SubmitLocationPermissionInfo(
            acceptedLocationPermission = context.hasLocationPermission(),
            showLocationRationale = showLocationRationale
        ))
        onAction(ActiveRunAction.SubmitNotificationPermissionInfo(
            acceptedNotificationPermission = context.hasNotificationPermission(),
            showNotificationRationale = showNotificationRationale
        ))

        if (!showLocationRationale && !showNotificationRationale) {
            permissionLauncher.requestRuniquePermissions(context)
        }
    }

    LaunchedEffect(state.shouldTrack) {
        if (context.hasNotificationPermission() && state.shouldTrack && !ActiveRunService.isServiceActive) {
            onServiceToggle(true)
        }
    }

    LaunchedEffect(state.isRunFinished) {
        if (state.isRunFinished) {
            onServiceToggle(false)
        }
    }

    RuniqueScaffold(
        withGradient = false,
        topAppBar = {
            RuniqueToolbar(
                title = stringResource(id = R.string.active_run),
                showBackButton = true,
                onBackClick = { onAction(ActiveRunAction.OnBackClick) }
            )
        },
        floatingActionButton = {
            RuniqueFloatingActionButton(
                icon = if (state.shouldTrack) StopIcon else StartIcon,
                iconSize = 20.dp,
                contentDescription = if (state.shouldTrack) stringResource(id = R.string.pause_run) else stringResource(id = R.string.start_run),
                onClick = { onAction(ActiveRunAction.OnToggleRunClick) }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
        ) {
            TrackerMap(
                modifier = Modifier.fillMaxSize(),
                isRunFinished = state.isRunFinished,
                currentLocation = state.currentLocation,
                locations = state.runData.locations,
                onSnapshot = { bmp ->
                    val stream = ByteArrayOutputStream()
                    stream.use {
                        bmp.compress(Bitmap.CompressFormat.JPEG, 80, it)
                    }
                    onAction(ActiveRunAction.OnRunProcessed(stream.toByteArray()))
                }
            )
            RunDataCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(paddingValues)
                    .padding(16.dp),
                elapsedTime = state.elapsedTime,
                runData = state.runData
            )
        }
    }

    if (!state.shouldTrack && state.hasStartedRunning) {
        RuniqueDialog(
            title = stringResource(id = R.string.running_is_paused),
            description = stringResource(id = R.string.resume_or_finish_run),
            primaryButton = {
                RuniqueActionButton(
                    modifier = Modifier.weight(1F),
                    text = stringResource(id = R.string.resume),
                    onClick = { onAction(ActiveRunAction.OnResumeRunClick) }
                )
            },
            secondaryButton = {
                RuniqueOutlinedActionButton(
                    modifier = Modifier.weight(1F),
                    text = stringResource(id = R.string.finish),
                    isLoading = state.isSavingRun,
                    onClick = { onAction(ActiveRunAction.OnFinishRunClick) }
                )
            },
            onDismiss = { onAction(ActiveRunAction.OnResumeRunClick) }
        )
    }

    if (state.showLocationRationale || state.showNotificationRationale) {
        RuniqueDialog(
            title = stringResource(id = R.string.permission_required),
            description = when {
                state.showLocationRationale && state.showNotificationRationale -> stringResource(id = R.string.location_notification_rationale)
                state.showLocationRationale -> stringResource(id = R.string.location_rationale)
                else -> stringResource(id = R.string.notification_rationale)
            },
            primaryButton = {
                RuniqueOutlinedActionButton(
                    text = stringResource(id = R.string.okay),
                    onClick = {
                        onAction(ActiveRunAction.DismissRationaleDialog)
                        permissionLauncher.requestRuniquePermissions(context)
                    }
                )
            },
            onDismiss = { /* Normal dismissing not allowed for permissions request */ }
        )
    }
}

private fun ActivityResultLauncher<Array<String>>.requestRuniquePermissions(context: Context) {
    val hasLocationPermission = context.hasLocationPermission()
    val hasNotificationPermission = context.hasNotificationPermission()

    val locationPermissions = arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION)
    val notificationPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(Manifest.permission.POST_NOTIFICATIONS)
    } else emptyArray()

    when {
        !hasLocationPermission && !hasNotificationPermission -> launch(locationPermissions + notificationPermission)
        !hasLocationPermission -> launch(locationPermissions)
        !hasNotificationPermission -> launch(notificationPermission)
    }
}