package com.staticquo.mesh

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
@SuppressLint("MissingPermission")
class MeshScanner @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val bleAdapter: BluetoothAdapter? by lazy {
        val manager = context.getSystemService(Context.BLUETOOTH_SERVICE)
        (manager as? android.bluetooth.BluetoothManager)?.adapter
    }

    private var activeCallback: ScanCallback? = null

    fun startScan(onDeviceFound: (PeerInfo) -> Unit) {
        val adapter = bleAdapter ?: return
        val scanner = adapter.bluetoothLeScanner ?: return

        stopScan()

        val callback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                val device = result.device
                val name = device.name ?: return
                if (!name.startsWith(MeshConstants.DEVICE_NAME_PREFIX)) return

                onDeviceFound(
                    PeerInfo(
                        address = device.address,
                        name = name,
                        rssi = result.rssi,
                        lastSeen = System.currentTimeMillis()
                    )
                )
            }
        }

        activeCallback = callback

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_POWER)
            .build()

        scanner.startScan(null, settings, callback)
    }

    fun stopScan() {
        val callback = activeCallback ?: return
        bleAdapter?.bluetoothLeScanner?.stopScan(callback)
        activeCallback = null
    }
}
