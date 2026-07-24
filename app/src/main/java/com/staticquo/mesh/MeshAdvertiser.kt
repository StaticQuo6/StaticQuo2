package com.staticquo.mesh

import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.BluetoothLeAdvertiser
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MeshAdvertiser @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val bleAdapter by lazy {
        val manager = context.getSystemService(Context.BLUETOOTH_SERVICE)
        (manager as? android.bluetooth.BluetoothManager)?.adapter
    }

    private var activeCallback: AdvertiseCallback? = null

    fun startAdvertising(onError: ((String) -> Unit)? = null) {
        val adapter = bleAdapter ?: run {
            onError?.invoke("Bluetooth not available")
            return
        }
        val advertiser: BluetoothLeAdvertiser = adapter.bluetoothLeAdvertiser ?: run {
            onError?.invoke("BLE advertising not supported on this device")
            return
        }

        stopAdvertising()

        val settings = AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_POWER)
            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_LOW)
            .setConnectable(true)
            .build()

        val data = AdvertiseData.Builder()
            .setIncludeDeviceName(true)
            .addServiceUuid(android.os.ParcelUuid(MeshConstants.SERVICE_UUID))
            .build()

        val callback = object : AdvertiseCallback() {
            override fun onStartSuccess(settingsInEffect: AdvertiseSettings) {}
            override fun onStartFailure(errorCode: Int) {
                onError?.invoke("Advertising failed (code $errorCode)")
            }
        }

        activeCallback = callback
        advertiser.startAdvertising(settings, data, callback)
    }

    fun stopAdvertising() {
        val callback = activeCallback ?: return
        bleAdapter?.bluetoothLeAdvertiser?.stopAdvertising(callback)
        activeCallback = null
    }
}
