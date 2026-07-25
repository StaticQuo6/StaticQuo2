package com.staticquo.lora

import android.content.Context
import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbEndpoint
import android.hardware.usb.UsbInterface
import android.hardware.usb.UsbManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import javax.inject.Singleton

sealed class SerialResult<T> {
    data class Success<T>(val data: T) : SerialResult<T>()
    data class Error<T>(val message: String) : SerialResult<T>()
}

@Singleton
class LoRaSerialDevice @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var connection: UsbDeviceConnection? = null
    private var connectedDevice: UsbDevice? = null
    private var readEndpoint: UsbEndpoint? = null
    private var writeEndpoint: UsbEndpoint? = null

    val isConnected: Boolean get() = connection != null

    fun findDevice(vendorId: Int? = null): UsbDevice? {
        val manager = context.getSystemService(Context.USB_SERVICE) as UsbManager
        for (device in manager.deviceList.values) {
            if (vendorId == null) return device
            if (device.vendorId == vendorId) return device
        }
        return null
    }

    fun connect(device: UsbDevice): SerialResult<Unit> {
        val manager = context.getSystemService(Context.USB_SERVICE) as UsbManager

        if (!manager.hasPermission(device)) {
            return SerialResult.Error("USB permission not granted")
        }

        val conn = manager.openDevice(device)
            ?: return SerialResult.Error("Failed to open USB device")

        val usbInterface = findDataInterface(device)
            ?: run {
                conn.close()
                return SerialResult.Error("No compatible interface found")
            }

        val claimed = conn.claimInterface(usbInterface, true)
        if (!claimed) {
            conn.close()
            return SerialResult.Error("Failed to claim interface")
        }

        val endpoints = getEndpoints(usbInterface)
        connection = conn
        connectedDevice = device
        readEndpoint = endpoints.first
        writeEndpoint = endpoints.second

        configureSerial(conn, usbInterface)

        return SerialResult.Success(Unit)
    }

    fun disconnect() {
        try {
            connection?.releaseInterface(
                findDataInterface(
                    connectedDevice ?: return
                ) ?: return
            )
        } catch (_: Exception) {}
        connection?.close()
        connection = null
        connectedDevice = null
        readEndpoint = null
        writeEndpoint = null
    }

    fun send(data: ByteArray): SerialResult<Unit> {
        val conn = connection ?: return SerialResult.Error("Not connected")
        val endpoint = writeEndpoint ?: return SerialResult.Error("No write endpoint")

        val framed = framePacket(data)
        val result = conn.bulkTransfer(endpoint, framed, framed.size, LoRaConstants.WRITE_TIMEOUT_MS)
        if (result < 0) {
            return SerialResult.Error("USB write failed")
        }
        return SerialResult.Success(Unit)
    }

    fun receive(): SerialResult<ByteArray> {
        val conn = connection ?: return SerialResult.Error("Not connected")
        val endpoint = readEndpoint ?: return SerialResult.Error("No read endpoint")

        val buffer = ByteArray(LoRaConstants.MAX_PACKET_SIZE)
        val result = conn.bulkTransfer(endpoint, buffer, buffer.size, LoRaConstants.READ_TIMEOUT_MS)
        if (result < 0) {
            return SerialResult.Error("USB read timeout")
        }
        val data = buffer.copyOf(result)
        val unframed = unframePacket(data)
        return SerialResult.Success(unframed ?: data)
    }

    private fun findDataInterface(device: UsbDevice): UsbInterface? {
        for (i in 0 until device.interfaceCount) {
            val intf = device.getInterface(i)
            for (j in 0 until intf.endpointCount) {
                val ep = intf.getEndpoint(j)
                if (ep.type == UsbConstants.USB_ENDPOINT_XFER_BULK) {
                    return intf
                }
            }
        }
        return null
    }

    private fun getEndpoints(intf: UsbInterface): Pair<UsbEndpoint?, UsbEndpoint?> {
        var read: UsbEndpoint? = null
        var write: UsbEndpoint? = null
        for (i in 0 until intf.endpointCount) {
            val ep = intf.getEndpoint(i)
            when (ep.direction) {
                UsbConstants.USB_DIR_IN -> read = ep
                UsbConstants.USB_DIR_OUT -> write = ep
            }
        }
        return Pair(read, write)
    }

    private fun configureSerial(conn: UsbDeviceConnection, intf: UsbInterface) {
        conn.controlTransfer(
            0x40, 0x00, 0x00, 0x00, null, 0, 0
        )
    }

    private fun framePacket(data: ByteArray): ByteArray {
        val bos = ByteArrayOutputStream()
        bos.write(LoRaConstants.FRAME_START_BYTE.toInt())
        bos.write(data.size and 0xFF)
        bos.write((data.size shr 8) and 0xFF)
        bos.write(data)
        bos.write(LoRaConstants.FRAME_END_BYTE.toInt())
        return bos.toByteArray()
    }

    private fun unframePacket(data: ByteArray): ByteArray? {
        if (data.size < 4) return null
        var start = -1
        var end = -1
        for (i in data.indices) {
            when (data[i]) {
                LoRaConstants.FRAME_START_BYTE -> start = i
                LoRaConstants.FRAME_END_BYTE -> end = i
            }
        }
        if (start < 0 || end <= start + 3) return null
        val payloadLength = (data[start + 1].toInt() and 0xFF) or
                ((data[start + 2].toInt() and 0xFF) shl 8)
        if (end - start - 3 != payloadLength) return null
        return data.copyOfRange(start + 3, end)
    }
}
