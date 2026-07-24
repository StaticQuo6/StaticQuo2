package com.staticquo.mesh

data class PeerInfo(
    val address: String,
    val name: String,
    val rssi: Int,
    val lastSeen: Long,
    val isConnected: Boolean = false
)
