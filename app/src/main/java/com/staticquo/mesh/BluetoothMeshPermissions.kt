package com.staticquo.mesh

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

data class MeshPermissionState(
    val bluetoothScanGranted: Boolean = false,
    val bluetoothConnectGranted: Boolean = false,
    val bluetoothAdvertiseGranted: Boolean = false,
    val locationGranted: Boolean = false,
    val allGranted: Boolean = false
)

fun getMeshPermissions(context: Context): MeshPermissionState {
    val scanGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) ==
            PackageManager.PERMISSION_GRANTED
    } else true

    val connectGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) ==
            PackageManager.PERMISSION_GRANTED
    } else true

    val advertiseGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_ADVERTISE) ==
            PackageManager.PERMISSION_GRANTED
    } else true

    val locationGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
        PackageManager.PERMISSION_GRANTED

    return MeshPermissionState(
        bluetoothScanGranted = scanGranted,
        bluetoothConnectGranted = connectGranted,
        bluetoothAdvertiseGranted = advertiseGranted,
        locationGranted = locationGranted,
        allGranted = scanGranted && connectGranted && advertiseGranted && locationGranted
    )
}

fun getMeshRuntimePermissions(): Array<String> {
    val permissions = mutableListOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        permissions.addAll(
            listOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_ADVERTISE
            )
        )
    }
    return permissions.toTypedArray()
}
