package com.example.run.presentation.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.core.content.ContextCompat

fun ComponentActivity.shouldShowLocationPermissionRationale(): Boolean =
    shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)

fun ComponentActivity.shouldShowNotificationPermissionRationale(): Boolean =
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)

fun Context.hasLocationPermission(): Boolean =
    hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)

fun Context.hasNotificationPermission(): Boolean =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        hasPermission(Manifest.permission.POST_NOTIFICATIONS)
    } else true

private fun Context.hasPermission(permission: String): Boolean =
    ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED