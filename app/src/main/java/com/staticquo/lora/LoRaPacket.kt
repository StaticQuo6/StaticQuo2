package com.staticquo.lora

data class LoRaPacket(
    val id: String = java.util.UUID.randomUUID().toString(),
    val payload: String,
    val frequency: Double = LoRaConstants.DEFAULT_FREQUENCY_MHZ,
    val spreadingFactor: Int = LoRaConstants.DEFAULT_SPREADING_FACTOR,
    val bandwidth: Int = LoRaConstants.DEFAULT_BANDWIDTH_KHZ,
    val codingRate: Int = LoRaConstants.DEFAULT_CODING_RATE,
    val rssi: Int = 0,
    val snr: Float = 0f,
    val timestamp: Long = System.currentTimeMillis(),
    val isOutgoing: Boolean = true
)
