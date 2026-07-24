package com.staticquo.lora

import javax.inject.Inject
import javax.inject.Singleton

sealed class LoRaResult<T> {
    data class Success<T>(val data: T) : LoRaResult<T>()
    data class Error<T>(val message: String) : LoRaResult<T>()
}

@Singleton
class LoRaRepository @Inject constructor(
    private val serialDevice: LoRaSerialDevice
) {

    private val receivedPackets = mutableListOf<LoRaPacket>()
    private var currentFrequency = LoRaConstants.DEFAULT_FREQUENCY_MHZ
    private var currentSpreadingFactor = LoRaConstants.DEFAULT_SPREADING_FACTOR

    fun getReceivedPackets(): List<LoRaPacket> = receivedPackets.toList()

    fun isConnected(): Boolean = serialDevice.isConnected

    fun findDevice(): LoRaResult<String> {
        val device = serialDevice.findDevice()
        return if (device != null) {
            LoRaResult.Success(
                "Found: ${device.productName ?: device.deviceName}"
            )
        } else {
            LoRaResult.Error("No USB LoRa module detected")
        }
    }

    fun connect(vendorId: Int? = null): LoRaResult<Unit> {
        val device = serialDevice.findDevice(vendorId)
            ?: return LoRaResult.Error("No LoRa device found")

        return when (val result = serialDevice.connect(device)) {
            is SerialResult.Success -> LoRaResult.Success(Unit)
            is SerialResult.Error -> LoRaResult.Error(result.message)
        }
    }

    fun disconnect() {
        serialDevice.disconnect()
    }

    fun sendMessage(content: String): LoRaResult<Unit> {
        if (!serialDevice.isConnected) {
            return LoRaResult.Error("LoRa module not connected")
        }

        val data = buildLoRaData(content)
        return when (val result = serialDevice.send(data)) {
            is SerialResult.Success -> {
                receivedPackets.add(
                    LoRaPacket(
                        payload = content,
                        frequency = currentFrequency,
                        spreadingFactor = currentSpreadingFactor,
                        isOutgoing = true
                    )
                )
                LoRaResult.Success(Unit)
            }
            is SerialResult.Error -> LoRaResult.Error(result.message)
        }
    }

    fun tryReceive() {
        if (!serialDevice.isConnected) return
        when (val result = serialDevice.receive()) {
            is SerialResult.Success -> {
                val content = parseLoRaData(result.data)
                if (content != null) {
                    receivedPackets.add(
                        LoRaPacket(
                            payload = content,
                            frequency = currentFrequency,
                            spreadingFactor = currentSpreadingFactor,
                            isOutgoing = false
                        )
                    )
                }
            }
            is SerialResult.Error -> {}
        }
    }

    fun setFrequency(freq: Double) {
        currentFrequency = freq
    }

    fun setSpreadingFactor(sf: Int) {
        currentSpreadingFactor = sf
    }

    private fun buildLoRaData(payload: String): ByteArray {
        return payload.toByteArray(Charsets.UTF_8)
    }

    private fun parseLoRaData(data: ByteArray): String? {
        return try {
            String(data, Charsets.UTF_8)
        } catch (_: Exception) { null }
    }
}
