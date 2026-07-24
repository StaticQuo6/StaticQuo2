package com.staticquo.mesh

import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.provider.Settings
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.CopyOnWriteArrayList
import javax.inject.Inject
import javax.inject.Singleton

sealed class MeshInitResult {
    data object Success : MeshInitResult()
    data class PermissionsDenied(val missing: List<String>) : MeshInitResult()
    data class BluetoothNotAvailable(val detail: String) : MeshInitResult()
    data class Error(val throwable: Throwable) : MeshInitResult()
}

@Singleton
class MeshRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val advertiser: MeshAdvertiser,
    private val scanner: MeshScanner,
    private val gattServer: MeshGattServer,
    private val gattClient: MeshGattClient
) {

    private val peers = CopyOnWriteArrayList<PeerInfo>()
    private val messages = CopyOnWriteArrayList<MeshMessage>()

    val nodeId: String by lazy {
        Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
            ?: "unknown"
    }

    val nodeName: String by lazy {
        val btName = BluetoothAdapter.getDefaultAdapter()?.name
        btName?.takeIf { it.startsWith(MeshConstants.DEVICE_NAME_PREFIX) }
            ?: "${MeshConstants.DEVICE_NAME_PREFIX}${nodeId.take(8)}"
    }

    fun checkPermissions(): MeshPermissionState = getMeshPermissions(context)

    fun getPeers(): List<PeerInfo> = peers.toList()

    fun getMessages(): List<MeshMessage> = messages.toList()

    fun initialize(): MeshInitResult {
        val perms = checkPermissions()
        if (!perms.allGranted) {
            val missing = mutableListOf<String>()
            if (!perms.bluetoothScanGranted) missing.add("BLUETOOTH_SCAN")
            if (!perms.bluetoothConnectGranted) missing.add("BLUETOOTH_CONNECT")
            if (!perms.bluetoothAdvertiseGranted) missing.add("BLUETOOTH_ADVERTISE")
            if (!perms.locationGranted) missing.add("ACCESS_FINE_LOCATION")
            return MeshInitResult.PermissionsDenied(missing)
        }

        val adapter = BluetoothAdapter.getDefaultAdapter()
        if (adapter == null || !adapter.isEnabled) {
            return MeshInitResult.BluetoothNotAvailable("Bluetooth disabled or unavailable")
        }

        if (!context.packageManager.hasSystemFeature(
                android.content.pm.PackageManager.FEATURE_BLUETOOTH_LE
            )
        ) {
            return MeshInitResult.BluetoothNotAvailable("Device lacks BLE hardware")
        }

        try {
            adapter.name = "${MeshConstants.DEVICE_NAME_PREFIX}${nodeId.take(8)}"
        } catch (_: SecurityException) {
        }

        if (!gattServer.start()) {
            return MeshInitResult.Error(RuntimeException("Failed to start GATT server"))
        }

        advertiser.startAdvertising {
            advertiser.startAdvertising()
        }

        gattServer.onMessageReceived = { message, senderAddress ->
            if (messages.none { it.id == message.id }) {
                val forwarded = message.copy(
                    hopCount = message.hopCount + 1,
                    senderId = nodeId
                )
                messages.add(forwarded)
                if (forwarded.hopCount < forwarded.maxHops) {
                    for (peer in peers) {
                        gattClient.connect(peer.address)
                        gattClient.sendMessage(forwarded)
                    }
                }
            }
        }

        startPeriodicScan()

        return MeshInitResult.Success
    }

    fun sendMessage(content: String): Boolean {
        val message = MeshMessage(
            senderId = nodeId,
            senderName = nodeName,
            content = content
        )
        messages.add(message)
        for (peer in peers) {
            gattClient.connect(peer.address)
            gattClient.sendMessage(message)
        }
        return true
    }

    fun shutdown() {
        gattClient.disconnect()
        gattServer.stop()
        advertiser.stopAdvertising()
        scanner.stopScan()
        peers.clear()
    }

    private fun startPeriodicScan() {
        scanner.startScan { peer ->
            val existing = peers.find { it.address == peer.address }
            if (existing == null) {
                peers.add(peer)
            } else {
                val index = peers.indexOf(existing)
                if (index >= 0) peers[index] = peer
            }
        }
    }
}
