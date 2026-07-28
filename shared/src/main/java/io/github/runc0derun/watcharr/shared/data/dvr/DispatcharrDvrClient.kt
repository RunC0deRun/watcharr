package io.github.runc0derun.watcharr.shared.data.dvr

import io.github.runc0derun.watcharr.shared.data.db.ChannelEntity
import io.github.runc0derun.watcharr.shared.data.db.ProgramEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.time.Instant
import java.util.Base64
import java.util.UUID

class DispatcharrDvrClient {

    data class DispatcharrChannel(
        val id: Int,
        val name: String,
        val channelNumber: Int?,
        val tvgId: String?
    )

    @Volatile
    private var cachedCsrfToken: String? = null
    @Volatile
    private var cachedJwtToken: String? = null
    private val cachedCookies = mutableSetOf<String>()

    private val cachedChannelsMap = mutableListOf<DispatcharrChannel>()
    @Volatile
    private var lastChannelsFetchTime = 0L

    private fun sanitizeUrl(url: String): String {
        return url.replace(Regex("https?://[^@]+@"), if (url.startsWith("https://")) "https://" else "http://")
    }

    private fun extractCookies(connection: HttpURLConnection) {
        val headerFields = connection.headerFields
        val setCookieHeaders = headerFields["Set-Cookie"] ?: headerFields["set-cookie"] ?: return
        for (header in setCookieHeaders) {
            val parts = header.split(";")
            if (parts.isNotEmpty()) {
                val cookie = parts[0].trim()
                if (cookie.isNotEmpty()) {
                    cachedCookies.add(cookie)
                    if (cookie.startsWith("csrftoken=")) {
                        cachedCsrfToken = cookie.substringAfter("csrftoken=").trim()
                    }
                }
            }
        }
    }

    private fun getOrCreateCsrfToken(): String {
        var token = cachedCsrfToken
        if (token.isNullOrEmpty()) {
            token = UUID.randomUUID().toString().replace("-", "")
            cachedCsrfToken = token
            cachedCookies.add("csrftoken=$token")
        }
        return token
    }

    private fun obtainJwtToken(cleanUrl: String, username: String, pass: String): String? {
        if (username.isEmpty()) return null
        var conn: HttpURLConnection? = null
        try {
            val hostUrl = cleanUrl.replace(Regex("https?://[^@]+@"), if (cleanUrl.startsWith("https://")) "https://" else "http://")
            val url = URL("$hostUrl/api/accounts/token/")
            conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.connectTimeout = 10000
            conn.readTimeout = 10000
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("Accept", "application/json")

            val payload = JSONObject().apply {
                put("username", username)
                put("password", pass)
            }
            conn.outputStream.use { os ->
                os.write(payload.toString().toByteArray(Charsets.UTF_8))
            }

            extractCookies(conn)

            if (conn.responseCode in 200..299) {
                val responseText = conn.inputStream.bufferedReader().use(BufferedReader::readText)
                val json = JSONObject(responseText)
                val access = json.optString("access", "")
                if (access.isNotEmpty()) {
                    cachedJwtToken = access
                    return access
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            conn?.disconnect()
        }
        return null
    }

    fun getAuthenticatedRecordingStreamUrl(cleanUrl: String, rawStreamUrl: String): String {
        var url = rawStreamUrl.trim()
        if (url.isEmpty()) return ""
        val token = cachedJwtToken ?: run {
            val parsed = try { URL(cleanUrl) } catch (e: Exception) { null }
            val userInfo = parsed?.userInfo
            if (!userInfo.isNullOrEmpty() && userInfo.contains(":")) {
                val user = userInfo.substringBefore(":")
                val pass = userInfo.substringAfter(":")
                obtainJwtToken(cleanUrl, java.net.URLDecoder.decode(user, "UTF-8"), java.net.URLDecoder.decode(pass, "UTF-8"))
            } else null
        }
        if (!token.isNullOrEmpty() && !url.contains("token=")) {
            val sep = if (url.contains("?")) "&" else "?"
            url = "$url${sep}token=$token"
        }
        return url
    }

    private fun applyHeaders(connection: HttpURLConnection, cleanUrl: String) {
        val csrfToken = getOrCreateCsrfToken()
        val sanitizedHost = sanitizeUrl(cleanUrl)
        connection.setRequestProperty("Accept", "application/json")
        connection.setRequestProperty("Referer", "$sanitizedHost/")
        connection.setRequestProperty("Origin", sanitizedHost)
        if (cachedCookies.isNotEmpty()) {
            connection.setRequestProperty("Cookie", cachedCookies.joinToString("; "))
        }
        connection.setRequestProperty("X-CSRFToken", csrfToken)
        connection.setRequestProperty("X-CSRF-Token", csrfToken)

        try {
            val parsed = URL(cleanUrl)
            val userInfo = parsed.userInfo
            if (!userInfo.isNullOrEmpty()) {
                if (userInfo.contains(":")) {
                    val user = userInfo.substringBefore(":")
                    val pass = userInfo.substringAfter(":")
                    val token = cachedJwtToken ?: obtainJwtToken(cleanUrl, java.net.URLDecoder.decode(user, "UTF-8"), java.net.URLDecoder.decode(pass, "UTF-8"))
                    if (!token.isNullOrEmpty()) {
                        connection.setRequestProperty("Authorization", "Bearer $token")
                    } else {
                        val basicAuth = "Basic " + Base64.getEncoder().encodeToString(userInfo.toByteArray(Charsets.UTF_8))
                        connection.setRequestProperty("Authorization", basicAuth)
                    }
                } else {
                    connection.setRequestProperty("Authorization", "Bearer $userInfo")
                    connection.setRequestProperty("X-API-Key", userInfo)
                }
            } else if (!cachedJwtToken.isNullOrEmpty()) {
                connection.setRequestProperty("Authorization", "Bearer $cachedJwtToken")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun ensureCsrfToken(cleanUrl: String) {
        var conn: HttpURLConnection? = null
        try {
            val url = URL("$cleanUrl/api/channels/recordings/")
            conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.connectTimeout = 10000
            conn.readTimeout = 10000
            applyHeaders(conn, cleanUrl)
            conn.responseCode
            extractCookies(conn)
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            conn?.disconnect()
        }
    }

    private fun getDispatcharrChannels(cleanUrl: String): List<DispatcharrChannel> {
        val now = System.currentTimeMillis()
        if (cachedChannelsMap.isNotEmpty() && (now - lastChannelsFetchTime) < 300_000) {
            return cachedChannelsMap
        }

        val endpoints = listOf(
            "$cleanUrl/api/channels/channels/",
            "$cleanUrl/api/channels/"
        )

        for (endpoint in endpoints) {
            var connection: HttpURLConnection? = null
            try {
                val url = URL(endpoint)
                connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 10000
                connection.readTimeout = 10000
                applyHeaders(connection, cleanUrl)

                if (connection.responseCode in 200..299) {
                    val responseText = connection.inputStream.bufferedReader().use(BufferedReader::readText)
                    val jsonArray = try { JSONArray(responseText) } catch (e: Exception) { JSONArray() }
                    val list = mutableListOf<DispatcharrChannel>()
                    for (i in 0 until jsonArray.length()) {
                        val obj = jsonArray.getJSONObject(i)
                        val id = obj.optInt("id", -1)
                        if (id != -1) {
                            val name = obj.optString("name", obj.optString("channel_name", ""))
                            val chNum = if (obj.has("channel_number")) obj.optInt("channel_number") else if (obj.has("number")) obj.optInt("number") else null
                            val tvgId = obj.optString("tvg_id", obj.optString("tvgId", ""))
                            list.add(DispatcharrChannel(id = id, name = name, channelNumber = chNum, tvgId = tvgId.ifEmpty { null }))
                        }
                    }
                    if (list.isNotEmpty()) {
                        cachedChannelsMap.clear()
                        cachedChannelsMap.addAll(list)
                        lastChannelsFetchTime = now
                        return list
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                connection?.disconnect()
            }
        }
        return cachedChannelsMap
    }

    private fun resolveDispatcharrChannelId(
        cleanUrl: String,
        program: ProgramEntity,
        channel: ChannelEntity?
    ): Int? {
        val dispatcharrChannels = getDispatcharrChannels(cleanUrl)
        if (dispatcharrChannels.isEmpty()) return null

        val targetName = channel?.name?.trim()?.lowercase() ?: ""
        val targetTvgId = channel?.tvgId?.trim()?.lowercase() ?: program.channelId.trim().lowercase()
        val targetChNum = channel?.tvgName?.toIntOrNull() ?: channel?.tvgId?.toIntOrNull()

        if (targetTvgId.isNotEmpty()) {
            val match = dispatcharrChannels.firstOrNull { it.tvgId?.trim()?.lowercase() == targetTvgId }
            if (match != null) return match.id
        }

        if (targetName.isNotEmpty()) {
            val match = dispatcharrChannels.firstOrNull { it.name.trim().lowercase() == targetName }
            if (match != null) return match.id
        }

        if (targetName.isNotEmpty()) {
            val match = dispatcharrChannels.firstOrNull {
                val dName = it.name.trim().lowercase()
                dName.contains(targetName) || targetName.contains(dName)
            }
            if (match != null) return match.id
        }

        if (targetChNum != null) {
            val match = dispatcharrChannels.firstOrNull { it.channelNumber == targetChNum || it.id == targetChNum }
            if (match != null) return match.id
        }

        return null
    }

    private fun parseIsoToEpochMs(iso: String): Long {
        val str = iso.trim()
        if (str.isEmpty()) return 0L
        val longVal = str.toLongOrNull()
        if (longVal != null && longVal > 0L) {
            return if (longVal < 10_000_000_000L) longVal * 1000L else longVal
        }
        val doubleVal = str.toDoubleOrNull()
        if (doubleVal != null && doubleVal > 0.0) {
            val longFromDouble = doubleVal.toLong()
            return if (longFromDouble < 10_000_000_000L) longFromDouble * 1000L else longFromDouble
        }
        try {
            return Instant.parse(str).toEpochMilli()
        } catch (e: Exception) {}

        val normalized = if (str.contains(" ") && !str.contains("T")) str.replace(" ", "T") else str
        val withZ = if (!normalized.contains("Z") && !normalized.contains("+") && !normalized.substringAfterLast("T", "").contains("-")) "${normalized}Z" else normalized
        try {
            return Instant.parse(withZ).toEpochMilli()
        } catch (e: Exception) {}

        try {
            val ldt = java.time.LocalDateTime.parse(normalized.take(19))
            return ldt.toInstant(java.time.ZoneOffset.UTC).toEpochMilli()
        } catch (e: Exception) {}

        return 0L
    }

    private fun parseTimestamp(
        primaryObj: JSONObject,
        secondaryObj: JSONObject?,
        epochKeys: List<String>,
        isoKeys: List<String>
    ): Long {
        val objs = listOfNotNull(primaryObj, secondaryObj)
        for (obj in objs) {
            for (key in epochKeys) {
                if (obj.has(key) && !obj.isNull(key)) {
                    val rawStr = obj.optString(key, "")
                    val longVal = rawStr.toLongOrNull() ?: obj.optLong(key, 0L)
                    if (longVal > 0L) {
                        return if (longVal < 10_000_000_000L) longVal * 1000L else longVal
                    }
                }
            }
            for (key in isoKeys) {
                if (obj.has(key) && !obj.isNull(key)) {
                    val rawStr = obj.optString(key, "")
                    if (rawStr.isNotEmpty()) {
                        val parsed = parseIsoToEpochMs(rawStr)
                        if (parsed > 0L) return parsed
                    }
                }
            }
        }
        return 0L
    }

    suspend fun fetchRecordings(baseUrl: String): Result<List<DvrRecording>> = withContext(Dispatchers.IO) {
        val cleanUrl = baseUrl.trim().removeSuffix("/")
        val sanitizedHost = sanitizeUrl(cleanUrl)
        if (cleanUrl.isEmpty()) {
            return@withContext Result.failure(IllegalArgumentException("Dispatcharr URL is empty"))
        }

        val candidateEndpoints = listOf(
            "$cleanUrl/api/channels/recordings/",
            "$cleanUrl/api/dvr/recordings/",
            "$cleanUrl/api/dvr/schedules/"
        )

        var lastError: Exception? = null

        for (endpoint in candidateEndpoints) {
            var connection: HttpURLConnection? = null
            val safeEndpoint = sanitizeUrl(endpoint)
            try {
                val url = URL(endpoint)
                connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 15000
                connection.readTimeout = 15000
                applyHeaders(connection, cleanUrl)

                val responseCode = connection.responseCode
                extractCookies(connection)

                if (responseCode in 200..299) {
                    val responseText = connection.inputStream.bufferedReader().use(BufferedReader::readText)
                    val recordingsList = mutableListOf<DvrRecording>()
                    val jsonArray = try { JSONArray(responseText) } catch (e: Exception) { JSONArray() }

                    for (i in 0 until jsonArray.length()) {
                        val obj = jsonArray.getJSONObject(i)
                        val id = obj.optString("id", "rec_$i")

                        val customProps = obj.optJSONObject("custom_properties")
                        val progObj = customProps?.optJSONObject("program")
                        val channelObj = obj.optJSONObject("channel")

                        val title = when {
                            obj.has("title") && obj.optString("title").isNotEmpty() -> obj.optString("title")
                            obj.has("program_title") && obj.optString("program_title").isNotEmpty() -> obj.optString("program_title")
                            progObj != null && progObj.has("title") && progObj.optString("title").isNotEmpty() -> progObj.optString("title")
                            obj.has("programTitle") && obj.optString("programTitle").isNotEmpty() -> obj.optString("programTitle")
                            else -> "Unknown Program"
                        }

                        val channelName = when {
                            obj.has("channel_name") && obj.optString("channel_name").isNotEmpty() -> obj.optString("channel_name")
                            customProps != null && customProps.has("channel_name") && customProps.optString("channel_name").isNotEmpty() -> customProps.optString("channel_name")
                            channelObj != null && channelObj.has("name") && channelObj.optString("name").isNotEmpty() -> channelObj.optString("name")
                            obj.has("channelName") && obj.optString("channelName").isNotEmpty() -> obj.optString("channelName")
                            else -> "Unknown Channel"
                        }

                        val channelId = when {
                            channelObj != null && channelObj.has("id") -> channelObj.optString("id")
                            channelObj != null && channelObj.has("number") -> channelObj.optString("number")
                            customProps != null && customProps.has("tvg_id") && customProps.optString("tvg_id").isNotEmpty() -> customProps.optString("tvg_id")
                            customProps != null && customProps.has("channel_id") && customProps.optString("channel_id").isNotEmpty() -> customProps.optString("channel_id")
                            obj.has("channel_id") && obj.optString("channel_id").isNotEmpty() -> obj.optString("channel_id")
                            obj.has("channel") && obj.optJSONObject("channel") == null -> obj.optString("channel")
                            obj.has("channelId") -> obj.optString("channelId")
                            else -> ""
                        }

                        val start = parseTimestamp(
                            obj,
                            progObj,
                            listOf("startEpochMs", "start", "start_epoch", "start_time_epoch", "start_timestamp"),
                            listOf("start_time", "start_at", "started_at", "start")
                        )

                        val stop = parseTimestamp(
                            obj,
                            progObj,
                            listOf("stopEpochMs", "stop", "end", "end_epoch", "stop_epoch", "end_time_epoch", "stop_timestamp", "end_timestamp"),
                            listOf("end_time", "stop_time", "end_at", "stop_at", "ended_at", "finished_at", "completed_at", "end", "stop")
                        )

                        val now = System.currentTimeMillis()
                        val rawStatus = (if (obj.has("status")) obj.optString("status") else (customProps?.optString("status", "") ?: "")).trim().lowercase()

                        val status = when {
                            rawStatus.contains("fail") || rawStatus.contains("error") || rawStatus.contains("cancel") -> DvrStatus.FAILED
                            rawStatus.contains("complete") || rawStatus.contains("finish") || rawStatus.contains("recorded") || rawStatus.contains("done") -> DvrStatus.COMPLETED
                            stop > 0L && now >= stop -> DvrStatus.COMPLETED
                            start > 0L && stop > 0L && now in start..stop -> DvrStatus.RECORDING
                            start > 0L && now < start -> DvrStatus.SCHEDULED
                            rawStatus.isNotEmpty() -> DvrStatus.fromString(rawStatus)
                            else -> DvrStatus.COMPLETED
                        }

                        val rawStreamUrl = obj.optString("file", obj.optString("streamUrl", "$sanitizedHost/api/channels/recordings/$id/file/"))
                        val authenticatedStreamUrl = getAuthenticatedRecordingStreamUrl(cleanUrl, rawStreamUrl)
                        val posterUrl = obj.optString("icon_url", obj.optString("posterUrl", progObj?.optString("icon_url", "") ?: ""))
                        val desc = obj.optString("description", obj.optString("desc", progObj?.optString("description", "") ?: ""))

                        recordingsList.add(
                            DvrRecording(
                                id = id,
                                programTitle = title,
                                channelName = channelName,
                                channelId = channelId.ifEmpty { null },
                                startEpochMs = start,
                                stopEpochMs = stop,
                                status = status,
                                streamUrl = authenticatedStreamUrl,
                                posterUrl = posterUrl.ifEmpty { null },
                                description = desc.ifEmpty { null }
                            )
                        )
                    }

                    return@withContext Result.success(recordingsList)
                } else if (responseCode == 401) {
                    cachedJwtToken = null
                    lastError = Exception("HTTP 401 Unauthorized from $safeEndpoint")
                    continue
                } else if (responseCode == 404 || responseCode == 405) {
                    lastError = Exception("HTTP $responseCode from $safeEndpoint")
                    continue
                } else {
                    val errorBody = try { connection.errorStream?.bufferedReader()?.use(BufferedReader::readText) ?: "" } catch (e: Exception) { "" }
                    return@withContext Result.failure(Exception("HTTP $responseCode from $safeEndpoint${if (errorBody.isNotEmpty()) ": $errorBody" else ""}"))
                }
            } catch (e: Exception) {
                lastError = Exception(sanitizeUrl(e.message ?: "Network error"))
            } finally {
                connection?.disconnect()
            }
        }

        Result.failure(lastError ?: Exception("Failed to fetch recordings from Dispatcharr"))
    }

    suspend fun scheduleRecording(
        baseUrl: String,
        program: ProgramEntity,
        channel: ChannelEntity?
    ): Result<DvrRecording> = withContext(Dispatchers.IO) {
        val cleanUrl = baseUrl.trim().removeSuffix("/")
        val sanitizedHost = sanitizeUrl(cleanUrl)
        if (cleanUrl.isEmpty()) {
            return@withContext Result.failure(IllegalArgumentException("Dispatcharr URL is empty"))
        }

        ensureCsrfToken(cleanUrl)

        val candidateEndpoints = listOf(
            "$cleanUrl/api/channels/recordings/",
            "$cleanUrl/api/dvr/schedules/",
            "$cleanUrl/api/dvr/recordings/"
        )

        var lastError: Exception? = null

        val startIso = try { Instant.ofEpochMilli(program.start).toString() } catch (e: Exception) { "" }
        val stopIso = try { Instant.ofEpochMilli(program.stop).toString() } catch (e: Exception) { "" }

        val dispatcharrChannelId = resolveDispatcharrChannelId(cleanUrl, program, channel)

        for (endpoint in candidateEndpoints) {
            var connection: HttpURLConnection? = null
            val safeEndpoint = sanitizeUrl(endpoint)
            try {
                val url = URL(endpoint)
                connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.connectTimeout = 15000
                connection.readTimeout = 15000
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json")
                applyHeaders(connection, cleanUrl)

                val customProps = JSONObject().apply {
                    put("channel_name", channel?.name ?: program.channelId)
                    put("tvg_id", channel?.tvgId ?: program.channelId)
                    put("status", "scheduled")
                    val programObj = JSONObject().apply {
                        put("title", program.title)
                        put("start_time", startIso)
                        put("end_time", stopIso)
                        put("description", program.desc ?: "")
                        put("icon_url", program.iconUrl ?: channel?.logoUrl ?: "")
                    }
                    put("program", programObj)
                }

                val payload = JSONObject().apply {
                    if (dispatcharrChannelId != null) {
                        put("channel", dispatcharrChannelId)
                    }
                    put("title", program.title)
                    put("program_title", program.title)
                    put("programTitle", program.title)
                    put("channel_name", channel?.name ?: program.channelId)
                    put("channelName", channel?.name ?: program.channelId)
                    put("channelId", channel?.tvgId ?: program.channelId)
                    put("tvg_id", channel?.tvgId ?: program.channelId)
                    put("start_time", startIso)
                    put("end_time", stopIso)
                    put("startEpochMs", program.start)
                    put("stopEpochMs", program.stop)
                    put("start", program.start)
                    put("stop", program.stop)
                    put("description", program.desc ?: "")
                    put("posterUrl", program.iconUrl ?: channel?.logoUrl ?: "")
                    put("icon_url", program.iconUrl ?: channel?.logoUrl ?: "")
                    put("custom_properties", customProps)
                }

                connection.outputStream.use { os ->
                    os.write(payload.toString().toByteArray(Charsets.UTF_8))
                }

                val responseCode = connection.responseCode
                extractCookies(connection)

                if (responseCode in 200..299) {
                    val responseText = connection.inputStream.bufferedReader().use(BufferedReader::readText)
                    val obj = try { JSONObject(responseText) } catch (e: Exception) { JSONObject() }
                    val id = obj.optString("id", "rec_${System.currentTimeMillis()}")
                    val rawStreamUrl = obj.optString("file", obj.optString("streamUrl", "$sanitizedHost/api/channels/recordings/$id/file/"))
                    val authenticatedStreamUrl = getAuthenticatedRecordingStreamUrl(cleanUrl, rawStreamUrl)

                    val recording = DvrRecording(
                        id = id,
                        programTitle = obj.optString("title", obj.optString("programTitle", program.title)),
                        channelName = obj.optString("channel_name", obj.optString("channelName", channel?.name ?: program.channelId)),
                        channelId = channel?.tvgId ?: program.channelId,
                        startEpochMs = program.start,
                        stopEpochMs = program.stop,
                        status = DvrStatus.fromString(obj.optString("status", "SCHEDULED")),
                        streamUrl = authenticatedStreamUrl,
                        posterUrl = program.iconUrl ?: channel?.logoUrl,
                        description = program.desc
                    )
                    return@withContext Result.success(recording)
                } else if (responseCode == 401) {
                    cachedJwtToken = null
                    lastError = Exception("HTTP 401 Unauthorized from $safeEndpoint")
                    continue
                } else if (responseCode == 404 || responseCode == 405) {
                    lastError = Exception("HTTP $responseCode from $safeEndpoint")
                    continue
                } else {
                    val errorBody = try { connection.errorStream?.bufferedReader()?.use(BufferedReader::readText) ?: "" } catch (e: Exception) { "" }
                    return@withContext Result.failure(Exception("HTTP $responseCode from $safeEndpoint${if (errorBody.isNotEmpty()) ": $errorBody" else ""}"))
                }
            } catch (e: Exception) {
                lastError = Exception(sanitizeUrl(e.message ?: "Network error"))
            } finally {
                connection?.disconnect()
            }
        }

        Result.failure(lastError ?: Exception("Failed to schedule recording on Dispatcharr"))
    }

    suspend fun deleteRecording(baseUrl: String, recordingId: String): Result<Boolean> = withContext(Dispatchers.IO) {
        val cleanUrl = baseUrl.trim().removeSuffix("/")
        val sanitizedHost = sanitizeUrl(cleanUrl)
        if (cleanUrl.isEmpty()) {
            return@withContext Result.failure(IllegalArgumentException("Dispatcharr URL is empty"))
        }

        ensureCsrfToken(cleanUrl)

        val candidateEndpoints = listOf(
            "$cleanUrl/api/channels/recordings/$recordingId/",
            "$cleanUrl/api/dvr/recordings/$recordingId/"
        )

        var lastError: Exception? = null

        for (endpoint in candidateEndpoints) {
            var connection: HttpURLConnection? = null
            val safeEndpoint = sanitizeUrl(endpoint)
            try {
                val url = URL(endpoint)
                connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "DELETE"
                connection.connectTimeout = 15000
                connection.readTimeout = 15000
                applyHeaders(connection, cleanUrl)

                val responseCode = connection.responseCode
                extractCookies(connection)

                if (responseCode in 200..299) {
                    return@withContext Result.success(true)
                } else if (responseCode == 401) {
                    cachedJwtToken = null
                    lastError = Exception("HTTP 401 Unauthorized from $safeEndpoint")
                    continue
                } else if (responseCode == 404 || responseCode == 405) {
                    lastError = Exception("HTTP $responseCode from $safeEndpoint")
                    continue
                } else {
                    val errorBody = try { connection.errorStream?.bufferedReader()?.use(BufferedReader::readText) ?: "" } catch (e: Exception) { "" }
                    return@withContext Result.failure(Exception("HTTP $responseCode deleting recording on $safeEndpoint${if (errorBody.isNotEmpty()) ": $errorBody" else ""}"))
                }
            } catch (e: Exception) {
                lastError = Exception(sanitizeUrl(e.message ?: "Network error"))
            } finally {
                connection?.disconnect()
            }
        }

        Result.failure(lastError ?: Exception("Failed to delete recording on Dispatcharr"))
    }
}
