package com.staticquo.lora

object LoRaConstants {
    const val DEFAULT_FREQUENCY_MHZ = 868.0
    const val DEFAULT_SPREADING_FACTOR = 9
    const val DEFAULT_BANDWIDTH_KHZ = 125
    const val DEFAULT_CODING_RATE = 5

    const val BAUD_RATE = 115200
    const val DATA_BITS = 8
    const val STOP_BITS = 1

    const val FRAME_START_BYTE: Byte = 0x7E
    const val FRAME_END_BYTE: Byte = 0x7F

    const val READ_TIMEOUT_MS = 1000
    const val WRITE_TIMEOUT_MS = 1000
    const val MAX_PACKET_SIZE = 256

    val SUPPORTED_FREQUENCIES_MHZ = listOf(433.0, 868.0, 915.0)
    val SUPPORTED_SPREADING_FACTORS = listOf(7, 8, 9, 10, 11, 12)
    val SUPPORTED_BANDWIDTHS_KHZ = listOf(125, 250, 500)
}
