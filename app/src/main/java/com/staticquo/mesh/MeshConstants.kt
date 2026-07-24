package com.staticquo.mesh

import java.util.UUID

object MeshConstants {
    const val SERVICE_UUID_STRING = "a3c87500-8b5a-4c8b-9c3e-7e8a3f5b1d20"
    const val MESSAGE_CHAR_UUID_STRING = "a3c87501-8b5a-4c8b-9c3e-7e8a3f5b1d20"

    val SERVICE_UUID: UUID = UUID.fromString(SERVICE_UUID_STRING)
    val MESSAGE_CHAR_UUID: UUID = UUID.fromString(MESSAGE_CHAR_UUID_STRING)

    const val DEVICE_NAME_PREFIX = "StaticQuo-"
    const val SCAN_DURATION_MS = 10_000L
    const val ADVERTISE_DURATION_MS = 30_000L
    const val ADVERTISE_TX_POWER = -50
    const val MAX_MESSAGE_SIZE = 500
}
