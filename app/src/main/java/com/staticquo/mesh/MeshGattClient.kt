package com.staticquo.mesh

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothProfile
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MeshGattClient @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var currentGatt: BluetoothGatt? = null
    private var messageCharacteristic: BluetoothGattCharacteristic? = null

    var onConnected: ((String) -> Unit)? = null
    var onDisconnected: ((String) -> Unit)? = null
    var onMessageSent: ((String) -> Unit)? = null

    private val callback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    gatt.discoverServices()
                    onConnected?.invoke(gatt.device.address)
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    onDisconnected?.invoke(gatt.device.address)
                    gatt.close()
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status != BluetoothGatt.GATT_SUCCESS) return

            val service = gatt.getService(MeshConstants.SERVICE_UUID) ?: return
            val characteristic = service.getCharacteristic(MeshConstants.MESSAGE_CHAR_UUID) ?: return
            messageCharacteristic = characteristic
        }
    }

    fun connect(address: String) {
        val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? android.bluetooth.BluetoothManager
        val adapter = manager?.adapter ?: return
        val device = adapter.getRemoteDevice(address) ?: return

        disconnect()
        currentGatt = device.connectGatt(context, false, callback)
    }

    fun sendMessage(message: MeshMessage): Boolean {
        val gatt = currentGatt ?: return false
        val characteristic = messageCharacteristic ?: return false

        try {
            val json = JSONObject().apply {
                put("id", message.id)
                put("senderId", message.senderId)
                put("senderName", message.senderName)
                put("content", message.content)
                put("ts", message.timestamp)
                put("hops", message.hopCount)
                put("maxHops", message.maxHops)
            }

            val data = json.toString().toByteArray(Charsets.UTF_8)
            if (data.size > MeshConstants.MAX_MESSAGE_SIZE) return false

            characteristic.setValue(data)
            characteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT)
            return gatt.writeCharacteristic(characteristic)
        } catch (_: Exception) {
            return false
        }
    }

    fun disconnect() {
        currentGatt?.disconnect()
        currentGatt?.close()
        currentGatt = null
        messageCharacteristic = null
    }
}
