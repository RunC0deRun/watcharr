package com.iptv.tv.ui

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.ServerSocket

class TvSetupServer(private val onConfigReceived: (m3uUrl: String, epgUrl: String) -> Unit) {
    private var serverSocket: ServerSocket? = null
    private var job: Job? = null

    fun start(scope: CoroutineScope): Int {
        val socket = ServerSocket(0) // bind to any free port
        serverSocket = socket
        val port = socket.localPort
        job = scope.launch(Dispatchers.IO) {
            try {
                while (isActive) {
                    val client = socket.accept()
                    launch(Dispatchers.IO) {
                        try {
                            client.use {
                                val reader = BufferedReader(InputStreamReader(it.inputStream))
                                val writer = PrintWriter(it.outputStream)

                                // Read HTTP headers
                                var contentLength = 0
                                var line: String?
                                while (reader.readLine().also { line = it } != null) {
                                    if (line!!.isEmpty()) break
                                    if (line!!.startsWith("Content-Length:", ignoreCase = true)) {
                                        contentLength = line!!.substringAfter(":").trim().toInt()
                                    }
                                }

                                if (contentLength > 0) {
                                    // Read HTTP body
                                    val body = CharArray(contentLength)
                                    var read = 0
                                    while (read < contentLength) {
                                        val chunk = reader.read(body, read, contentLength - read)
                                        if (chunk == -1) break
                                        read += chunk
                                    }
                                    val bodyString = String(body, 0, read)

                                    // Parse JSON
                                    val json = JSONObject(bodyString)
                                    val playlistUrl = json.getString("playlistUrl")
                                    val epgUrl = json.optString("epgUrl", "")

                                    withContext(Dispatchers.Main) {
                                        onConfigReceived(playlistUrl, epgUrl)
                                    }

                                    // Send HTTP response
                                    writer.println("HTTP/1.1 200 OK")
                                    writer.println("Content-Type: application/json")
                                    writer.println("Access-Control-Allow-Origin: *")
                                    writer.println("Connection: close")
                                    writer.println()
                                    writer.println("{\"status\":\"success\"}")
                                } else {
                                    writer.println("HTTP/1.1 400 Bad Request")
                                    writer.println("Connection: close")
                                    writer.println()
                                }
                                writer.flush()
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            } catch (e: Exception) {
                // Server socket closed
            }
        }
        return port
    }

    fun stop() {
        job?.cancel()
        try {
            serverSocket?.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
