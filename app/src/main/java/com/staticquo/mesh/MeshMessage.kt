package com.staticquo.mesh

import java.util.UUID

data class MeshMessage(
    val id: String = UUID.randomUUID().toString(),
    val senderId: String,
    val senderName: String,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val hopCount: Int = 0,
    val maxHops: Int = 5
)
