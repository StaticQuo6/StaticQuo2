package com.staticquo.mesh

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

sealed class MeshInitResult {
    data object Success : MeshInitResult()
    data class PermissionsDenied(val missingPermissions: List<String>) : MeshInitResult()
    data class BluetoothNotAvailable(val detail: String) : MeshInitResult()
    data class Error(val throwable: Throwable) : MeshInitResult()
}

@Singleton
class MeshRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private var initialized = false

    fun checkPermissions(): MeshPermissionState {
        return getMeshPermissions(context)
    }

    fun requiredRuntimePermissions(): Array<String> {
        return getMeshRuntimePermissions()
    }

    fun initialize(): MeshInitResult {
        if (initialized) return MeshInitResult.Success

        if (!context.packageManager.hasSystemFeature(
                android.content.pm.PackageManager.FEATURE_BLUETOOTH_LE
            )
        ) {
            return MeshInitResult.BluetoothNotAvailable("Device lacks BLE hardware")
        }

        val permissions = checkPermissions()
        if (!permissions.allGranted) {
            val missing = mutableListOf<String>()
            if (!permissions.bluetoothScanGranted) missing.add("BLUETOOTH_SCAN")
            if (!permissions.bluetoothConnectGranted) missing.add("BLUETOOTH_CONNECT")
            if (!permissions.bluetoothAdvertiseGranted) missing.add("BLUETOOTH_ADVERTISE")
            if (!permissions.locationGranted) missing.add("ACCESS_FINE_LOCATION")
            return MeshInitResult.PermissionsDenied(missing)
        }

        initialized = true
        return MeshInitResult.Success
    }
}
