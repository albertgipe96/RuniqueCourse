@file:OptIn(MapsComposeExperimentalApi::class)

package com.example.run.presentation.active_run.maps

import android.graphics.Bitmap
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.core.domain.location.Location
import com.example.core.domain.location.LocationTimestamp
import com.example.core.presentation.designsystem.RunIcon
import com.example.run.presentation.R
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapEffect
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.MapsComposeExperimentalApi
import com.google.maps.android.compose.MarkerComposable
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberMarkerState
import com.google.maps.android.ktx.awaitSnapshot
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun TrackerMap(
    modifier: Modifier = Modifier,
    isRunFinished: Boolean,
    currentLocation: Location?,
    locations: List<List<LocationTimestamp>>,
    onSnapshot: (Bitmap) -> Unit
) {
    val context = LocalContext.current
    val mapStyle = remember { MapStyleOptions.loadRawResourceStyle(context, R.raw.map_style) }
    val cameraPositionState = rememberCameraPositionState()
    val markerState = rememberMarkerState()
    
    val markerPositionLat by animateFloatAsState(
        targetValue = currentLocation?.lat?.toFloat() ?: 0F,
        animationSpec = tween(durationMillis = 500),
        label = ""
    )
    val markerPositionLong by animateFloatAsState(
        targetValue = currentLocation?.long?.toFloat() ?: 0F,
        animationSpec = tween(durationMillis = 500),
        label = ""
    )
    val markerPosition = remember(markerPositionLat, markerPositionLong) {
        LatLng(markerPositionLat.toDouble(), markerPositionLong.toDouble())
    }

    LaunchedEffect(markerPosition, isRunFinished) {
        if (!isRunFinished) {
            markerState.position = markerPosition
        }
    }
    LaunchedEffect(currentLocation, isRunFinished) {
        if (currentLocation != null && !isRunFinished) {
            val latLng = LatLng(currentLocation.lat, currentLocation.long)
            cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(latLng, 17F))
        }
    }

    var triggerCapture by remember { mutableStateOf(false) } // When set to true, we want to take the snapshot
    var createSnapshotJob: Job? = remember { null }

    GoogleMap(
        modifier = if (isRunFinished) {
            modifier
                .width(300.dp)
                .aspectRatio(16 / 9F)
                .alpha(0F)
                .onSizeChanged {
                    if (it.width >= 300) {
                        triggerCapture = true
                    }
                }
        } else modifier,
        cameraPositionState = cameraPositionState,
        properties = MapProperties(mapStyleOptions = mapStyle),
        uiSettings = MapUiSettings(zoomControlsEnabled = false)
    ) {
        RuniquePolylines(locations = locations)

        MapEffect(locations, isRunFinished, triggerCapture) { map -> // Launched effect specific to maps composable
            if (isRunFinished && triggerCapture && createSnapshotJob == null) {
                triggerCapture = false

                val boundsBuilder = LatLngBounds.builder()
                locations.flatten().forEach { locationTimestamp ->
                    boundsBuilder.include(
                        LatLng(locationTimestamp.location.location.lat, locationTimestamp.location.location.long)
                    )
                }
                map.moveCamera(
                    CameraUpdateFactory.newLatLngBounds(boundsBuilder.build(), 100)
                )

                map.setOnCameraIdleListener { // We want to take the snapshot when the camera finished moving
                    createSnapshotJob?.cancel()
                    createSnapshotJob = GlobalScope.launch {// Is GlobalScope to outlive the lifecycle and be sure that the snapshot is taken also if the composable is destroyed
                        delay(500L) // Need a little delay to wait for the camera to be focused again after moving and assure that the render is finished
                        map.awaitSnapshot()?.let { onSnapshot(it) }
                    }
                }
            }
        }

        if (!isRunFinished && currentLocation != null) {
            MarkerComposable(currentLocation, state = markerState) {
                Box(
                    modifier = Modifier
                        .size(35.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        modifier = Modifier.size(20.dp),
                        imageVector = RunIcon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
    }
}