package com.staticquo.mesh

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattServer
import android.bluetooth.BluetoothGattServerCallback
import android.bluetooth.BluetoothGattService
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
@SuppressLint("MissingPermission")
class MeshGattServer @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val bleManager by lazy {
        context.getSystemService(Context.BLUETOOTH_SERVICE) as? android.bluetooth.BluetoothManager
    }

    private var gattServer: BluetoothGattServer? = null
    var onMessageReceived: ((MeshMessage, String) -> Unit)? = null

    private val callback = object : BluetoothGattServerCallback() {
        override fun onCharacteristicWriteRequest(
            device: BluetoothDevice,
            requestId: Int,
            characteristic: BluetoothGattCharacteristic,
            preparedWrite: Boolean,
            responseNeeded: Boolean,
            offset: Int,
            value: ByteArray
        ) {
            gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, null)

            try {
                val json = String(value, Charsets.UTF_8)
                val obj = JSONObject(json)
                val message = MeshMessage(
                    id = obj.optString("id", java.util.UUID.randomUUID().toString()),
                    senderId = obj.getString("senderId"),
                    senderName = obj.optString("senderName", "Unknown"),
                    content = obj.getString("content"),
                    timestamp = obj.optLong("ts", System.currentTimeMillis()),
                    hopCount = obj.optInt("hops", 0),
                    maxHops = obj.optInt("maxHops", 5)
                )
                onMessageReceived?.invoke(message, device.address)
            } catch (_: Exception) {
            }
        }

        override fun onConnectionStateChange(device: BluetoothDevice, status: Int, newState: Int) {
        }
    }

    fun start(): Boolean {
        val manager = bleManager ?: return false
        val server = manager.openGattServer(context, callback) ?: return false

        val service = BluetoothGattService(
            MeshConstants.SERVICE_UUID,
            BluetoothGattService.SERVICE_TYPE_PRIMARY
        )

        val characteristic = BluetoothGattCharacteristic(
            MeshConstants.MESSAGE_CHAR_UUID,
            BluetoothGattCharacteristic.PROPERTY_WRITE or BluetoothGattCharacteristic.PROPERTY_NOTIFY,
            BluetoothGattCharacteristic.PERMISSION_WRITE
        )

        service.addCharacteristic(characteristic)
        server.addService(service)
        gattServer = server
        return true
    }

    fun stop() {
        gattServer?.close()
        gattServer = null
    }
}
