package io.github.runc0derun.watcharr.shared.data.dvr

import android.content.Context
import android.util.Log
import android.util.Xml
import io.github.runc0derun.watcharr.shared.data.db.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import java.io.File
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

private const val TAG = "WatcharrDvrEngine"

class WatcharrDvrEngine(private val context: Context) {

    private val database = AppDatabase.getDatabase(context)
    private val dao = database.dvrRecordingDao()
    private val networkStorageManager = NetworkStorageManager(context)

    suspend fun recordStream(
        recordingId: String,
        streamUrl: String,
        stopEpochMs: Long,
        storageType: String,
        shareConfig: NetworkStorageManager.NetworkShareConfig,
        onProgress: ((bytesRead: Long) -> Unit)? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        val recording = dao.getRecordingById(recordingId)
            ?: return@withContext Result.failure(Exception("Recording record $recordingId not found"))

        dao.updateStatus(recordingId, "RECORDING")

        val safeTitle = recording.programTitle.replace(Regex("[^a-zA-Z0-9._-]"), "_")
        val baseFileName = "Watcharr_${safeTitle}_${recordingId.take(8)}"

        try {
            // Probe the stream to determine its type
            val initialText = fetchText(streamUrl)
            val isDash = initialText != null && (initialText.trimStart().startsWith("<MPD") ||
                    initialText.contains("urn:mpeg:dash"))
            val isHls = !isDash && (streamUrl.contains(".m3u8") ||
                    (initialText != null && initialText.contains("#EXTM3U")))

            Log.d(TAG, "recordStream: isDash=$isDash, isHls=$isHls, url=$streamUrl")

            if (isDash) {
                recordDashStream(
                    recordingId = recordingId,
                    streamUrl = streamUrl,
                    initialManifest = initialText,
                    stopEpochMs = stopEpochMs,
                    baseFileName = baseFileName,
                    storageType = storageType,
                    shareConfig = shareConfig,
                    onProgress = onProgress
                )
            } else {
                recordRawOrHlsStream(
                    recordingId = recordingId,
                    recording = recording,
                    streamUrl = streamUrl,
                    initialText = initialText,
                    isHls = isHls,
                    stopEpochMs = stopEpochMs,
                    fileName = "$baseFileName.ts",
                    storageType = storageType,
                    shareConfig = shareConfig,
                    onProgress = onProgress
                )
            }
        } catch (e: Exception) {
            dao.updateStatus(recordingId, "FAILED", errorReason = e.localizedMessage)
            Result.failure(e)
        }
    }

    // -------------------------------------------------------------------------
    // DASH recording
    // -------------------------------------------------------------------------

    private data class DashManifest(
        val baseUrl: String,
        val videoRepresentationId: String,
        val audioRepresentationId: String?,
        val mediaTemplate: String,       // e.g. "{period}/$RepresentationID$-$Time$.m4s"
        val initTemplate: String,        // e.g. "{period}/init-$RepresentationID$.mp4"
        val timescale: Long,
        val segments: List<SegmentEntry> // ordered list of (time, duration) pairs
    )

    private data class SegmentEntry(val time: Long, val duration: Long)

    private suspend fun recordDashStream(
        recordingId: String,
        streamUrl: String,
        initialManifest: String,
        stopEpochMs: Long,
        baseFileName: String,
        storageType: String,
        shareConfig: NetworkStorageManager.NetworkShareConfig,
        onProgress: ((bytesRead: Long) -> Unit)?
    ): Result<String> {
        Log.d(TAG, "recordDashStream: starting DASH recording")

        val videoFileName = "${baseFileName}_video.mp4"
        val audioFileName = "${baseFileName}_audio.mp4"

        val videoDest = networkStorageManager.getRecordingOutput(recordingId, videoFileName, storageType, shareConfig)
        val audioDest = networkStorageManager.getRecordingOutput(recordingId, audioFileName, storageType, shareConfig)

        var totalBytesWritten = 0L
        val downloadedVideoSegments = mutableSetOf<String>()
        val downloadedAudioSegments = mutableSetOf<String>()
        var videoInitWritten = false
        var audioInitWritten = false

        var drmKeySetIdB64: String? = null
        val recordingEntity = dao.getRecordingById(recordingId)
        val targetChId = recordingEntity?.channelId ?: ""
        if (targetChId.isNotEmpty()) {
            val licenseUri = "https://delta-bridge.jstienstra.nl/license/$targetChId"
            val psshBytes = extractWidevinePssh(initialManifest)
            if (psshBytes != null && psshBytes.isNotEmpty()) {
                try {
                    Log.d(TAG, "recordDashStream: Attempting offline Widevine license acquisition for channel $targetChId from $licenseUri")
                    val eventDispatcher = androidx.media3.exoplayer.drm.DrmSessionEventListener.EventDispatcher()
                    val helper = androidx.media3.exoplayer.drm.OfflineLicenseHelper.newWidevineInstance(
                        licenseUri,
                        androidx.media3.datasource.DefaultHttpDataSource.Factory(),
                        eventDispatcher
                    )
                    val drmInitData = androidx.media3.common.DrmInitData(
                        androidx.media3.common.DrmInitData.SchemeData(
                            androidx.media3.common.C.WIDEVINE_UUID,
                            "video/mp4",
                            psshBytes
                        )
                    )
                    val format = androidx.media3.common.Format.Builder()
                        .setSampleMimeType("video/mp4")
                        .setDrmInitData(drmInitData)
                        .build()
                    val keySetIdBytes = helper.downloadLicense(format)
                    if (keySetIdBytes.isNotEmpty()) {
                        drmKeySetIdB64 = android.util.Base64.encodeToString(keySetIdBytes, android.util.Base64.NO_WRAP)
                        Log.d(TAG, "recordDashStream: Successfully acquired offline keySetId for recording $recordingId")
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "recordDashStream: Could not acquire offline Widevine DRM keySetId: ${e.message}")
                }
            } else {
                Log.d(TAG, "recordDashStream: Manifest does not contain Widevine PSSH, skipping offline license request.")
            }
        }

        try {
            videoDest.outputStream.use { videoOut ->
                audioDest.outputStream.use { audioOut ->
                    var currentManifest = initialManifest

                    while (System.currentTimeMillis() < stopEpochMs) {
                        val parsed = parseDashManifest(streamUrl, currentManifest)
                        if (parsed == null) {
                            Log.w(TAG, "recordDashStream: Failed to parse MPD, retrying in 4s")
                            delay(4000)
                            currentManifest = fetchFullText(streamUrl) ?: break
                            continue
                        }

                        // Write init segments once
                        if (!videoInitWritten) {
                            val initUrl = resolveSegmentUrl(parsed.baseUrl, parsed.initTemplate, parsed.videoRepresentationId, 0L)
                            Log.d(TAG, "recordDashStream: Fetching video init: $initUrl")
                            val initData = fetchBinary(initUrl)
                            if (initData != null && initData.isNotEmpty()) {
                                videoOut.write(initData)
                                videoOut.flush()
                                totalBytesWritten += initData.size
                                videoInitWritten = true
                                Log.d(TAG, "recordDashStream: Video init written (${initData.size} bytes)")
                            }
                        }

                        if (!audioInitWritten && parsed.audioRepresentationId != null) {
                            val initUrl = resolveSegmentUrl(parsed.baseUrl, parsed.initTemplate, parsed.audioRepresentationId, 0L)
                            Log.d(TAG, "recordDashStream: Fetching audio init: $initUrl")
                            val initData = fetchBinary(initUrl)
                            if (initData != null && initData.isNotEmpty()) {
                                audioOut.write(initData)
                                audioOut.flush()
                                totalBytesWritten += initData.size
                                audioInitWritten = true
                                Log.d(TAG, "recordDashStream: Audio init written (${initData.size} bytes)")
                            }
                        }

                        // Download new segments from the timeline
                        for (seg in parsed.segments) {
                            if (System.currentTimeMillis() >= stopEpochMs) break

                            val videoSegUrl = resolveSegmentUrl(parsed.baseUrl, parsed.mediaTemplate, parsed.videoRepresentationId, seg.time)
                            if (downloadedVideoSegments.add(videoSegUrl)) {
                                val data = fetchBinary(videoSegUrl)
                                if (data != null && data.isNotEmpty()) {
                                    videoOut.write(data)
                                    videoOut.flush()
                                    totalBytesWritten += data.size
                                    onProgress?.invoke(totalBytesWritten)
                                    Log.d(TAG, "recordDashStream: Video segment t=${seg.time} written (${data.size} bytes)")
                                }
                            }

                            if (parsed.audioRepresentationId != null) {
                                val audioSegUrl = resolveSegmentUrl(parsed.baseUrl, parsed.mediaTemplate, parsed.audioRepresentationId, seg.time)
                                if (downloadedAudioSegments.add(audioSegUrl)) {
                                    val data = fetchBinary(audioSegUrl)
                                    if (data != null && data.isNotEmpty()) {
                                        audioOut.write(data)
                                        audioOut.flush()
                                        totalBytesWritten += data.size
                                        onProgress?.invoke(totalBytesWritten)
                                    }
                                }
                            }
                        }

                        // Wait for the next manifest refresh (DASH minimumUpdatePeriod is typically 4s)
                        delay(4000)
                        if (System.currentTimeMillis() < stopEpochMs) {
                            currentManifest = fetchFullText(streamUrl) ?: break
                        }
                    }
                }
            }

            val hasAudio = audioInitWritten && downloadedAudioSegments.isNotEmpty()
            val combinedPath = if (hasAudio) {
                "${videoDest.file.absolutePath}|${audioDest.file.absolutePath}"
            } else {
                videoDest.file.absolutePath
            }
            // Remove empty audio file if no audio was recorded
            if (!hasAudio && audioDest.file.exists()) {
                audioDest.file.delete()
            }

            val finalStatus = if (totalBytesWritten > 0L) "COMPLETED" else "FAILED"
            val errorReason = if (totalBytesWritten == 0L) "No data received from DASH stream" else videoDest.fallbackReason

            val updated = dao.getRecordingById(recordingId)?.copy(
                status = finalStatus,
                localFilePath = if (totalBytesWritten > 0L) combinedPath else null,
                drmKeySetId = drmKeySetIdB64,
                errorReason = errorReason
            )
            if (updated != null) dao.updateRecording(updated)

            Log.d(TAG, "recordDashStream: Done. totalBytes=$totalBytesWritten, path=$combinedPath")
            return if (totalBytesWritten > 0L) Result.success(combinedPath)
            else Result.failure(Exception(errorReason))
        } catch (e: Exception) {
            Log.e(TAG, "recordDashStream: Error", e)
            // Clean up partial files
            if (videoDest.file.exists() && videoDest.file.length() == 0L) videoDest.file.delete()
            if (audioDest.file.exists() && audioDest.file.length() == 0L) audioDest.file.delete()
            dao.updateStatus(recordingId, "FAILED", errorReason = e.localizedMessage)
            return Result.failure(e)
        }
    }

    /**
     * Parses a DASH MPD manifest and extracts the information needed to download segments.
     * Handles SegmentTemplate with SegmentTimeline (isoff-live profile).
     */
    private fun parseDashManifest(manifestUrl: String, content: String): DashManifest? {
        return try {
            val parser = Xml.newPullParser()
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
            parser.setInput(content.reader())

            fun getAttr(attrName: String): String? {
                val v = parser.getAttributeValue(null, attrName)
                if (v != null) return v
                for (i in 0 until parser.attributeCount) {
                    val name = parser.getAttributeName(i)
                    if (name.equals(attrName, ignoreCase = true) || name.endsWith(":$attrName", ignoreCase = true)) {
                        return parser.getAttributeValue(i)
                    }
                }
                return null
            }

            var baseUrl = manifestUrl.substringBefore("?").let { u ->
                if (u.endsWith(".mpd")) u.substringBeforeLast("/") + "/" else u
            }
            var videoRepId = ""
            var audioRepId: String? = null
            var periodId = ""
            var currentAdaptationType = ""   // "video" or "audio"
            var currentMimeType = ""
            var currentTimescale = 90000L
            var currentMediaTemplate = ""
            var currentInitTemplate = ""
            var inVideoAdaptation = false
            var inAudioAdaptation = false
            var inSegmentTimeline = false
            var videoSegments = mutableListOf<SegmentEntry>()
            var audioSegments = mutableListOf<SegmentEntry>()
            var bestVideoBandwidth = -1
            var videoMediaTemplate = ""
            var videoInitTemplate = ""
            var videoTimescale = 90000L

            var eventType = parser.eventType
            while (eventType != XmlPullParser.END_DOCUMENT) {
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        val tagName = parser.name?.substringAfterLast(":") ?: ""
                        when (tagName) {
                            "BaseURL" -> {
                                parser.next()
                                if (parser.eventType == XmlPullParser.TEXT) {
                                    val rawBase = parser.text.trim()
                                    baseUrl = if (rawBase.startsWith("http")) rawBase
                                    else try { URL(URL(manifestUrl), rawBase).toString() } catch (e: Exception) { rawBase }
                                }
                            }
                            "Period" -> {
                                periodId = getAttr("id") ?: ""
                            }
                            "AdaptationSet" -> {
                                currentMimeType = getAttr("mimeType") ?: ""
                                currentAdaptationType = getAttr("contentType") ?: ""
                                if (currentMimeType.contains("video") || currentAdaptationType == "video") {
                                    inVideoAdaptation = true; inAudioAdaptation = false
                                } else if (currentMimeType.contains("audio") || currentAdaptationType == "audio") {
                                    inAudioAdaptation = true; inVideoAdaptation = false
                                }
                                inSegmentTimeline = false
                            }
                            "SegmentTemplate" -> {
                                val ts = getAttr("timescale")?.toLongOrNull() ?: 90000L
                                val media = getAttr("media") ?: ""
                                val init = getAttr("initialization") ?: ""
                                currentTimescale = ts
                                currentMediaTemplate = if (periodId.isNotEmpty() && !media.startsWith(periodId)) {
                                    "$periodId/$media"
                                } else media
                                currentInitTemplate = if (periodId.isNotEmpty() && !init.startsWith(periodId)) {
                                    "$periodId/$init"
                                } else init
                                if (inVideoAdaptation) {
                                    videoMediaTemplate = currentMediaTemplate
                                    videoInitTemplate = currentInitTemplate
                                    videoTimescale = currentTimescale
                                }
                            }
                            "SegmentTimeline" -> {
                                inSegmentTimeline = true
                            }
                            "S" -> {
                                if (inSegmentTimeline) {
                                    val t = getAttr("t")?.toLongOrNull()
                                    val d = getAttr("d")?.toLongOrNull() ?: 0L
                                    val r = getAttr("r")?.toIntOrNull() ?: 0
                                    val startTime = t ?: (if (if (inVideoAdaptation) videoSegments.isNotEmpty() else audioSegments.isNotEmpty()) {
                                        val list = if (inVideoAdaptation) videoSegments else audioSegments
                                        list.last().time + list.last().duration
                                    } else 0L)
                                    for (i in 0..r) {
                                        val entry = SegmentEntry(startTime + i * d, d)
                                        if (inVideoAdaptation) videoSegments.add(entry)
                                        else if (inAudioAdaptation) audioSegments.add(entry)
                                    }
                                }
                            }
                            "Representation" -> {
                                val repId = getAttr("id") ?: ""
                                val bandwidth = getAttr("bandwidth")?.toIntOrNull() ?: 0
                                if (inVideoAdaptation) {
                                    if (videoRepId.isEmpty() || bandwidth > bestVideoBandwidth) {
                                        bestVideoBandwidth = bandwidth
                                        videoRepId = repId
                                    }
                                } else if (inAudioAdaptation && audioRepId == null) {
                                    audioRepId = repId
                                }
                            }
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        val tagName = parser.name?.substringAfterLast(":") ?: ""
                        when (tagName) {
                            "SegmentTimeline" -> inSegmentTimeline = false
                            "AdaptationSet" -> {
                                inVideoAdaptation = false; inAudioAdaptation = false
                            }
                        }
                    }
                }
                eventType = parser.next()
            }

            if (videoRepId.isEmpty() || videoSegments.isEmpty()) {
                Log.w(TAG, "parseDashManifest: No video representation (repId=$videoRepId) or segments (count=${videoSegments.size}) found")
                return null
            }

            Log.d(TAG, "parseDashManifest: baseUrl=$baseUrl, videoRep=$videoRepId, audioRep=$audioRepId, segments=${videoSegments.size}")

            DashManifest(
                baseUrl = baseUrl,
                videoRepresentationId = videoRepId,
                audioRepresentationId = audioRepId,
                mediaTemplate = videoMediaTemplate,
                initTemplate = videoInitTemplate,
                timescale = videoTimescale,
                segments = videoSegments
            )
        } catch (e: Exception) {
            Log.e(TAG, "parseDashManifest: XML parse error", e)
            null
        }
    }

    private fun extractWidevinePssh(manifestXml: String): ByteArray? {
        return try {
            val parser = Xml.newPullParser()
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
            parser.setInput(manifestXml.reader())
            var eventType = parser.eventType
            var isWidevineScheme = false
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG) {
                    val name = parser.name
                    if (name == "ContentProtection" || name.endsWith(":ContentProtection")) {
                        val scheme = parser.getAttributeValue(null, "schemeIdUri") ?: ""
                        isWidevineScheme = scheme.contains("edef8ba9-79d6-4ace-a3c8-27dcd51d21ed", ignoreCase = true)
                    } else if (isWidevineScheme && (name == "pssh" || name.endsWith(":pssh"))) {
                        parser.next()
                        if (parser.eventType == XmlPullParser.TEXT) {
                            val b64 = parser.text.trim()
                            if (b64.isNotEmpty()) {
                                return android.util.Base64.decode(b64, android.util.Base64.DEFAULT)
                            }
                        }
                    }
                } else if (eventType == XmlPullParser.END_TAG) {
                    if (parser.name == "ContentProtection" || parser.name.endsWith(":ContentProtection")) {
                        isWidevineScheme = false
                    }
                }
                eventType = parser.next()
            }
            null
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Resolves a DASH segment URL from a template.
     * Handles: $RepresentationID$, $Time$, $Number$, and period-prefix patterns.
     */
    private fun resolveSegmentUrl(baseUrl: String, template: String, representationId: String, time: Long): String {
        val resolved = template
            .replace("\$RepresentationID\$", representationId)
            .replace("\$Time\$", time.toString())
            .replace("&amp;", "&")

        return if (resolved.startsWith("http://") || resolved.startsWith("https://")) {
            resolved
        } else {
            try { URL(URL(baseUrl), resolved).toString() } catch (e: Exception) { "$baseUrl$resolved" }
        }
    }

    // -------------------------------------------------------------------------
    // HLS / raw stream recording (unchanged logic, extracted for clarity)
    // -------------------------------------------------------------------------

    private suspend fun recordRawOrHlsStream(
        recordingId: String,
        recording: io.github.runc0derun.watcharr.shared.data.db.DvrRecordingEntity,
        streamUrl: String,
        initialText: String?,
        isHls: Boolean,
        stopEpochMs: Long,
        fileName: String,
        storageType: String,
        shareConfig: NetworkStorageManager.NetworkShareConfig,
        onProgress: ((bytesRead: Long) -> Unit)?
    ): Result<String> {
        val destination = networkStorageManager.getRecordingOutput(
            recordingId = recordingId,
            fileName = fileName,
            storageType = storageType,
            config = shareConfig
        )

        var totalBytesWritten = 0L

        try {
            destination.outputStream.use { output ->
                if (isHls) {
                    totalBytesWritten = recordHlsPlaylist(streamUrl, stopEpochMs, output, onProgress)
                } else {
                    var connection: HttpURLConnection? = null
                    var inputStream: InputStream? = null
                    val buffer = ByteArray(32 * 1024)
                    var retries = 0
                    val maxRetries = 3

                    while (System.currentTimeMillis() < stopEpochMs) {
                        if (connection == null) {
                            try {
                                val url = URL(streamUrl)
                                connection = url.openConnection() as HttpURLConnection
                                connection.connectTimeout = 15_000
                                connection.readTimeout = 15_000
                                connection.requestMethod = "GET"
                                connection.setRequestProperty("User-Agent", "Watcharr/1.0 (Android)")

                                val responseCode = connection.responseCode
                                if (responseCode !in 200..299) {
                                    if (totalBytesWritten == 0L) {
                                        dao.updateStatus(recordingId, "FAILED", errorReason = "HTTP $responseCode from stream server")
                                        return Result.failure(Exception("Stream HTTP error code $responseCode"))
                                    } else {
                                        break
                                    }
                                }
                                inputStream = connection.inputStream
                            } catch (e: Exception) {
                                if (totalBytesWritten == 0L && retries >= maxRetries) {
                                    throw e
                                }
                                retries++
                                delay(2000)
                                connection = null
                                continue
                            }
                        }

                        try {
                            val bytesRead = inputStream?.read(buffer) ?: -1
                            if (bytesRead == -1) {
                                inputStream?.close()
                                connection.disconnect()
                                connection = null
                                inputStream = null
                                if (System.currentTimeMillis() < stopEpochMs - 5000 && retries < maxRetries) {
                                    retries++
                                    delay(2000)
                                    continue
                                } else {
                                    break
                                }
                            }
                            retries = 0
                            output.write(buffer, 0, bytesRead)
                            totalBytesWritten += bytesRead
                            onProgress?.invoke(totalBytesWritten)
                        } catch (e: Exception) {
                            inputStream?.close()
                            connection?.disconnect()
                            connection = null
                            inputStream = null
                            if (System.currentTimeMillis() < stopEpochMs - 5000 && retries < maxRetries) {
                                retries++
                                delay(2000)
                            } else {
                                if (totalBytesWritten == 0L) throw e else break
                            }
                        }
                    }
                }
            }

            val finalPath = destination.file.absolutePath
            val isDrm = streamUrl.contains("license") || streamUrl.contains("widevine")
            val finalStatus = if (totalBytesWritten > 0L) "COMPLETED" else "FAILED"
            val errorReason = if (totalBytesWritten == 0L) "No data received from stream" else destination.fallbackReason

            val updated = recording.copy(
                status = finalStatus,
                localFilePath = if (totalBytesWritten > 0L) finalPath else null,
                isDrmDecrypted = isDrm,
                errorReason = errorReason
            )
            dao.updateRecording(updated)

            return if (totalBytesWritten > 0L) Result.success(finalPath) else Result.failure(Exception(errorReason))
        } catch (e: Exception) {
            dao.updateStatus(recordingId, "FAILED", errorReason = e.localizedMessage)
            return Result.failure(e)
        }
    }

    private suspend fun recordHlsPlaylist(
        streamUrl: String,
        stopEpochMs: Long,
        outputStream: java.io.OutputStream,
        onProgress: ((bytesRead: Long) -> Unit)?
    ): Long {
        var totalBytes = 0L
        val downloadedSegments = mutableSetOf<String>()
        var activePlaylistUrl = streamUrl

        while (System.currentTimeMillis() < stopEpochMs) {
            try {
                val content = fetchFullText(activePlaylistUrl) ?: break

                if (content.contains("#EXT-X-STREAM-INF")) {
                    val variantUrl = parseMasterVariantUrl(activePlaylistUrl, content)
                    if (variantUrl != null) {
                        activePlaylistUrl = variantUrl
                        continue
                    }
                }

                var currentKey: ByteArray? = null
                var currentIv: ByteArray? = null
                var segmentSeq = 0L

                val lines = content.lines()
                for (line in lines) {
                    val trimmed = line.trim()
                    if (trimmed.startsWith("#EXT-X-KEY")) {
                        val keyInfo = parseHlsKeyInfo(activePlaylistUrl, trimmed)
                        if (keyInfo != null) {
                            currentKey = keyInfo.first
                            currentIv = keyInfo.second
                        }
                    } else if (trimmed.startsWith("#EXT-X-MEDIA-SEQUENCE:")) {
                        segmentSeq = trimmed.substringAfter(":").toLongOrNull() ?: 0L
                    } else if (trimmed.isNotEmpty() && !trimmed.startsWith("#")) {
                        if (System.currentTimeMillis() >= stopEpochMs) break
                        val segUrl = try { URL(URL(activePlaylistUrl), trimmed).toString() } catch (e: Exception) { trimmed }
                        if (downloadedSegments.add(segUrl)) {
                            var segmentData = fetchBinary(segUrl)
                            if (segmentData != null && segmentData.isNotEmpty()) {
                                if (currentKey != null) {
                                    val iv = currentIv ?: createIvFromSeq(segmentSeq)
                                    segmentData = decryptAes128(segmentData, currentKey, iv) ?: segmentData
                                }
                                outputStream.write(segmentData)
                                outputStream.flush()
                                totalBytes += segmentData.size
                                onProgress?.invoke(totalBytes)
                            }
                        }
                        segmentSeq++
                    }
                }
                delay(3000)
            } catch (e: Exception) {
                delay(3000)
            }
        }
        return totalBytes
    }

    private fun parseHlsKeyInfo(baseUrl: String, keyLine: String): Pair<ByteArray, ByteArray?>? {
        return try {
            if (!keyLine.contains("METHOD=AES-128")) return null
            val uriMatch = Regex("""URI="([^"]+)"""").find(keyLine) ?: return null
            val keyUrl = URL(URL(baseUrl), uriMatch.groupValues[1]).toString()
            val keyBytes = fetchBinary(keyUrl) ?: return null

            val ivMatch = Regex("""IV=0x([0-9a-fA-F]+)""").find(keyLine)
            val ivBytes = ivMatch?.groupValues?.get(1)?.chunked(2)?.map { it.toInt(16).toByte() }?.toByteArray()

            Pair(keyBytes, ivBytes)
        } catch (e: Exception) {
            null
        }
    }

    private fun createIvFromSeq(seq: Long): ByteArray {
        val iv = ByteArray(16)
        for (i in 0 until 8) {
            iv[15 - i] = ((seq shr (i * 8)) and 0xFF).toByte()
        }
        return iv
    }

    private fun decryptAes128(data: ByteArray, key: ByteArray, iv: ByteArray): ByteArray? {
        return try {
            val cipher = javax.crypto.Cipher.getInstance("AES/CBC/PKCS5Padding")
            val keySpec = javax.crypto.spec.SecretKeySpec(key, "AES")
            val ivSpec = javax.crypto.spec.IvParameterSpec(iv)
            cipher.init(javax.crypto.Cipher.DECRYPT_MODE, keySpec, ivSpec)
            cipher.doFinal(data)
        } catch (e: Exception) {
            try {
                val cipher = javax.crypto.Cipher.getInstance("AES/CBC/NoPadding")
                val keySpec = javax.crypto.spec.SecretKeySpec(key, "AES")
                val ivSpec = javax.crypto.spec.IvParameterSpec(iv)
                cipher.init(javax.crypto.Cipher.DECRYPT_MODE, keySpec, ivSpec)
                cipher.doFinal(data)
            } catch (e2: Exception) {
                null
            }
        }
    }

    private fun fetchText(urlStr: String): String? {
        var conn: HttpURLConnection? = null
        return try {
            val url = URL(urlStr)
            conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 10_000
            conn.readTimeout = 10_000
            conn.setRequestProperty("User-Agent", "Watcharr/1.0 (Android)")
            if (conn.responseCode in 200..299) {
                val buffer = CharArray(4096)
                val reader = conn.inputStream.bufferedReader()
                val read = reader.read(buffer, 0, buffer.size)
                if (read > 0) String(buffer, 0, read) else null
            } else null
        } catch (e: Exception) {
            null
        } finally {
            conn?.disconnect()
        }
    }

    private fun fetchFullText(urlStr: String): String? {
        var conn: HttpURLConnection? = null
        return try {
            val url = URL(urlStr)
            conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 10_000
            conn.readTimeout = 10_000
            conn.setRequestProperty("User-Agent", "Watcharr/1.0 (Android)")
            if (conn.responseCode in 200..299) {
                conn.inputStream.bufferedReader().use { it.readText() }
            } else null
        } catch (e: Exception) {
            null
        } finally {
            conn?.disconnect()
        }
    }

    private fun fetchBinary(urlStr: String): ByteArray? {
        var conn: HttpURLConnection? = null
        return try {
            val url = URL(urlStr)
            conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 10_000
            conn.readTimeout = 30_000
            conn.setRequestProperty("User-Agent", "Watcharr/1.0 (Android)")
            if (conn.responseCode in 200..299) {
                conn.inputStream.use { it.readBytes() }
            } else null
        } catch (e: Exception) {
            Log.w(TAG, "fetchBinary failed for $urlStr: ${e.message}")
            null
        } finally {
            conn?.disconnect()
        }
    }

    private fun parseMasterVariantUrl(baseUrl: String, content: String): String? {
        val lines = content.lines()
        for (i in lines.indices) {
            if (lines[i].startsWith("#EXT-X-STREAM-INF")) {
                for (j in i + 1 until lines.size) {
                    val line = lines[j].trim()
                    if (line.isNotEmpty() && !line.startsWith("#")) {
                        return try { URL(URL(baseUrl), line).toString() } catch (e: Exception) { line }
                    }
                }
            }
        }
        return null
    }
}
