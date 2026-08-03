package io.github.runc0derun.watcharr.shared.data.dvr

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.net.InetAddress
import java.net.Socket

class NetworkStorageManager(private val context: Context) {

    data class NetworkShareConfig(
        val protocol: String = "SMB", // "SMB" or "NFS"
        val host: String = "",
        val sharePath: String = "",
        val username: String = "",
        val password: String = ""
    )

    data class TestResult(
        val isSuccessful: Boolean,
        val message: String
    )

    /**
     * Test reachability of the SMB/NFS share host and port.
     * Called during onboarding verification and before initiating scheduled recordings.
     */
    suspend fun testConnection(config: NetworkShareConfig): TestResult = withContext(Dispatchers.IO) {
        if (config.host.isBlank()) {
            return@withContext TestResult(false, "Host/IP address cannot be empty.")
        }
        val cleanHost = config.host.trim().removePrefix("smb://").removePrefix("nfs://")

        try {
            // First check basic ICMP/IP reachability
            val inet = InetAddress.getByName(cleanHost)
            val isReachable = inet.isReachable(3000)

            // Next check protocol specific ports (SMB: 445/139, NFS: 2049)
            val port = if (config.protocol.uppercase() == "NFS") 2049 else 445
            var socketConnected = false
            try {
                Socket(inet, port).use {
                    socketConnected = true
                }
            } catch (e: Exception) {
                // If port 445 fails for SMB, try fallback port 139
                if (config.protocol.uppercase() == "SMB") {
                    try {
                        Socket(inet, 139).use {
                            socketConnected = true
                        }
                    } catch (_: Exception) {}
                }
            }

            if (isReachable || socketConnected) {
                TestResult(true, "Successfully reached ${config.protocol} share host ($cleanHost).")
            } else {
                TestResult(false, "Could not reach ${config.protocol} server at $cleanHost. Check IP and network.")
            }
        } catch (e: Exception) {
            TestResult(false, "Network check failed: ${e.localizedMessage ?: e.message}")
        }
    }

    /**
     * Get target directory for storing local/network recordings.
     * Falls back to local device storage if Network Share is requested but unreachable.
     */
    suspend fun getRecordingOutput(
        recordingId: String,
        fileName: String,
        storageType: String,
        config: NetworkShareConfig
    ): RecordingDestination = withContext(Dispatchers.IO) {
        val localDir = context.getExternalFilesDir("Recordings") ?: File(context.filesDir, "Recordings")
        if (!localDir.exists()) {
            localDir.mkdirs()
        }

        if (storageType.uppercase() == "NETWORK_SHARE") {
            val reachability = testConnection(config)
            if (!reachability.isSuccessful) {
                // Fallback to local storage with warning
                val fallbackFile = File(localDir, fileName)
                return@withContext RecordingDestination(
                    file = fallbackFile,
                    outputStream = FileOutputStream(fallbackFile),
                    usedFallback = true,
                    fallbackReason = "Network share unreachable (${reachability.message}). Saved locally."
                )
            }

            // Attempt writing to mounted share location or local share buffer folder
            val shareBase = File(localDir, "network_shares/${config.sharePath.trim('/')}")
            if (!shareBase.exists()) {
                shareBase.mkdirs()
            }
            val targetFile = File(shareBase, fileName)
            return@withContext RecordingDestination(
                file = targetFile,
                outputStream = FileOutputStream(targetFile),
                usedFallback = false,
                fallbackReason = null
            )
        } else {
            val targetFile = File(localDir, fileName)
            return@withContext RecordingDestination(
                file = targetFile,
                outputStream = FileOutputStream(targetFile),
                usedFallback = false,
                fallbackReason = null
            )
        }
    }

    data class RecordingDestination(
        val file: File,
        val outputStream: OutputStream,
        val usedFallback: Boolean,
        val fallbackReason: String?
    )
}
