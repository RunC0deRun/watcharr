package io.github.runc0derun.watcharr.shared.playback

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import java.io.File
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.ProxySelector
import java.net.SocketAddress
import java.net.URI

object TsnetManager {
    private const val TAG = "TsnetManager"

    private val _status = MutableStateFlow("")
    val status: StateFlow<String> = _status.asStateFlow()

    private var proxyPort = 0
    private var isEnabled = false
    private var isLibraryLoaded = false

    private val defaultProxySelector = ProxySelector.getDefault()

    init {
        try {
            System.loadLibrary("tsnet")
            isLibraryLoaded = true
            android.util.Log.i(TAG, "Successfully loaded native libtsnet")
        } catch (e: UnsatisfiedLinkError) {
            android.util.Log.e(TAG, "Failed to load native libtsnet: ${e.localizedMessage}")
        }
        
        // Register our custom ProxySelector globally
        ProxySelector.setDefault(TsnetProxySelector)
    }

    fun start(context: Context, authKey: String) {
        if (!isLibraryLoaded) {
            android.util.Log.e(TAG, "Cannot start tsnet: native library libtsnet.so not loaded")
            _status.value = "Error: Native library libtsnet.so not found or failed to load"
            isEnabled = false
            return
        }

        if (isEnabled) {
            stop()
        }

        if (authKey.isEmpty()) return

        val stateDir = File(context.filesDir, "tailscale")
        if (!stateDir.exists()) {
            stateDir.mkdirs()
        }

        isEnabled = true
        _status.value = "Authenticating"

        try {
            val callback = object : TsnetCallback {
                override fun onStatusChanged(status: String) {
                    _status.value = status
                    android.util.Log.i(TAG, "Tailscale status updated: $status")
                }
            }
            proxyPort = nativeStart(authKey, stateDir.absolutePath, callback)
            android.util.Log.i(TAG, "Tailscale tsnet started on proxy port: $proxyPort")
        } catch (e: Throwable) {
            android.util.Log.e(TAG, "Error starting native tsnet server: ${e.localizedMessage}")
            _status.value = "Error: ${e.localizedMessage ?: "JNI Linkage error"}"
            isEnabled = false
        }
    }

    fun stop() {
        if (!isEnabled) return
        isEnabled = false
        proxyPort = 0
        _status.value = ""
        if (!isLibraryLoaded) return
        try {
            nativeStop()
            android.util.Log.d(TAG, "Tailscale tsnet stopped")
        } catch (e: Throwable) {
            android.util.Log.e(TAG, "Error stopping native tsnet server: ${e.localizedMessage}")
        }
    }

    fun getProxyPort(): Int = proxyPort

    fun isEnabled(): Boolean = isEnabled

    suspend fun awaitConnection(): Boolean {
        if (!isEnabled) return true
        val current = _status.value
        if (current == "Connected") return true
        if (current.startsWith("Error")) return false
        
        val finalStatus = _status.first { it == "Connected" || it.startsWith("Error") }
        return finalStatus == "Connected"
    }

    fun isTailnetUrl(urlStr: String): Boolean {
        // When Tailscale is enabled, route ALL traffic through the Go proxy.
        // The Go proxy's dialer (tsServer.Dial) handles DNS resolution natively
        // via the Tailnet's internal resolver - it knows about split-DNS, MagicDNS,
        // and custom domains mapped to CGNAT IPs. We don't need to pre-resolve here.
        android.util.Log.i(TAG, "isTailnetUrl: routing $urlStr through Tailnet proxy")
        return true
    }

    private object TsnetProxySelector : ProxySelector() {
        override fun select(uri: URI): List<Proxy> {
            val urlStr = uri.toString()
            android.util.Log.i(TAG, "ProxySelector.select called for: $urlStr, isEnabled=$isEnabled, proxyPort=$proxyPort")
            if (isEnabled && proxyPort > 0 && isTailnetUrl(urlStr)) {
                android.util.Log.i(TAG, "Routing Tailnet connection for $urlStr through local proxy on 127.0.0.1:$proxyPort")
                return listOf(Proxy(Proxy.Type.HTTP, InetSocketAddress("127.0.0.1", proxyPort)))
            }
            return defaultProxySelector?.select(uri) ?: listOf(Proxy.NO_PROXY)
        }

        override fun connectFailed(uri: URI, sa: SocketAddress, ioe: IOException) {
            defaultProxySelector?.connectFailed(uri, sa, ioe)
        }
    }

    interface TsnetCallback {
        fun onStatusChanged(status: String)
    }

    private external fun nativeStart(authKey: String, stateDir: String, callback: TsnetCallback): Int
    private external fun nativeStop()
}
