package com.example.run.presentation.active_run.maps

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.example.core.domain.location.LocationTimestamp
import com.google.android.gms.maps.model.JointType
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.Polyline

@Composable
fun RuniquePolylines(
    locations: List<List<LocationTimestamp>>
) {
    val polylines = remember(locations) {
        locations.map {
            it.zipWithNext { locationTimestamp1, locationTimestamp2 ->
                PolylineUi(
                    location1 = locationTimestamp1.location.location,
                    location2 = locationTimestamp2.location.location,
                    color = PolylineColorCalculator.locationsToColor(
                        location1 = locationTimestamp1,
                        location2 = locationTimestamp2
                    )
                )
            }
        }
    }

    polylines.forEach { polyline ->
        polyline.forEach { polylineUi ->
            Polyline(
                points = listOf(
                    LatLng(polylineUi.location1.lat, polylineUi.location1.long),
                    LatLng(polylineUi.location2.lat, polylineUi.location2.long)
                ),
                color = polylineUi.color,
                jointType = JointType.BEVEL
            )
        }
    }
}