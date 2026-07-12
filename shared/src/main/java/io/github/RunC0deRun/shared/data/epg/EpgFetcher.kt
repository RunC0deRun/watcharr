package io.github.runc0derun.shared.data.epg

import android.content.Context
import io.github.runc0derun.shared.data.db.AppDatabase
import io.github.runc0derun.shared.data.db.ProgramEntity
import io.github.runc0derun.shared.data.parser.EpgParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL

class EpgFetcher(private val context: Context) {

    private val db = AppDatabase.getDatabase(context)
    private val programDao = db.programDao()

    suspend fun fetchAndSyncEpg(epgUrl: String): Result<Unit> = withContext(Dispatchers.IO) {
        val tempFile = java.io.File.createTempFile("epg", ".xml", context.cacheDir)
        var connection: java.net.URLConnection? = null
        try {
            val url = URL(epgUrl)
            connection = url.openConnection()
            connection.connectTimeout = 30000
            connection.readTimeout = 30000
            connection.connect()

            val inputStream = connection.getInputStream()
            inputStream.use { input ->
                tempFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }

            val isGzip = epgUrl.endsWith(".gz", ignoreCase = true) || epgUrl.contains(".xml.gz", ignoreCase = true)

            // Deleting all old programs is now safe since the file is successfully downloaded and on disk.
            programDao.deleteAll()

            val batch = mutableListOf<ProgramEntity>()
            val batchSize = 1000

            java.io.FileInputStream(tempFile).use { fileInputStream ->
                EpgParser.parse(fileInputStream, isGzip).collect { program ->
                    batch.add(program)
                    if (batch.size >= batchSize) {
                        programDao.insertAll(batch)
                        batch.clear()
                    }
                }
            }

            if (batch.isNotEmpty()) {
                programDao.insertAll(batch)
            }

            pruneCache()

            Result.success(Unit)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        } finally {
            (connection as? java.net.HttpURLConnection)?.disconnect()
            if (tempFile.exists()) {
                tempFile.delete()
            }
        }
    }

    suspend fun pruneCache() = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val pastLimit = now - 12 * 60 * 60 * 1000L
        val futureLimit = now + 36 * 60 * 60 * 1000L
        programDao.pruneOldPrograms(pastLimit, futureLimit)
    }
}
