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

                                // Read the request line
                                val requestLine = reader.readLine() ?: ""
                                val requestParts = requestLine.split(" ")
                                if (requestParts.size < 2) {
                                    writer.println("HTTP/1.1 400 Bad Request")
                                    writer.println("Connection: close")
                                    writer.println()
                                    writer.flush()
                                    return@use
                                }

                                val method = requestParts[0]
                                val path = requestParts[1]

                                // Read HTTP headers
                                var contentLength = 0
                                var line: String?
                                while (reader.readLine().also { line = it } != null) {
                                    if (line!!.isEmpty()) break
                                    if (line!!.startsWith("Content-Length:", ignoreCase = true)) {
                                        contentLength = line!!.substringAfter(":").trim().toIntOrNull() ?: 0
                                    }
                                }

                                if (method == "OPTIONS") {
                                    writer.println("HTTP/1.1 204 No Content")
                                    writer.println("Access-Control-Allow-Origin: *")
                                    writer.println("Access-Control-Allow-Methods: POST, OPTIONS")
                                    writer.println("Access-Control-Allow-Headers: Content-Type")
                                    writer.println("Connection: close")
                                    writer.println()
                                    writer.flush()
                                    return@use
                                }

                                if (method != "POST" || path != "/setup") {
                                    writer.println("HTTP/1.1 405 Method Not Allowed")
                                    writer.println("Allow: POST, OPTIONS")
                                    writer.println("Connection: close")
                                    writer.println()
                                    writer.flush()
                                    return@use
                                }

                                // Cap payload size to 10 KB to prevent OOM
                                if (contentLength > 10240) {
                                    writer.println("HTTP/1.1 413 Payload Too Large")
                                    writer.println("Connection: close")
                                    writer.println()
                                    writer.flush()
                                    return@use
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

                                    try {
                                        // Parse and validate JSON
                                        val json = JSONObject(bodyString)
                                        val playlistUrl = json.getString("playlistUrl")
                                        val epgUrl = json.optString("epgUrl", "")

                                        // Basic URL validation
                                        if (playlistUrl.isEmpty() || !playlistUrl.startsWith("http")) {
                                            throw IllegalArgumentException("Invalid playlist URL")
                                        }

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
                                    } catch (e: Exception) {
                                        writer.println("HTTP/1.1 400 Bad Request")
                                        writer.println("Content-Type: application/json")
                                        writer.println("Connection: close")
                                        writer.println()
                                        writer.println("{\"status\":\"error\",\"message\":\"${e.message}\"}")
                                    }
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
