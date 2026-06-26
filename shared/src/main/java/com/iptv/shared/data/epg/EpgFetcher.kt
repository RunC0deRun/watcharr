package com.iptv.shared.data.epg

import android.content.Context
import com.iptv.shared.data.db.AppDatabase
import com.iptv.shared.data.db.ProgramEntity
import com.iptv.shared.data.parser.EpgParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL

class EpgFetcher(context: Context) {

    private val db = AppDatabase.getDatabase(context)
    private val programDao = db.programDao()

    suspend fun fetchAndSyncEpg(epgUrl: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val url = URL(epgUrl)
            val connection = url.openConnection()
            connection.connect()
            val inputStream = connection.getInputStream()

            val isGzip = epgUrl.endsWith(".gz", ignoreCase = true) || epgUrl.contains(".xml.gz", ignoreCase = true)

            programDao.deleteAll()

            val batch = mutableListOf<ProgramEntity>()
            val batchSize = 1000

            EpgParser.parse(inputStream, isGzip).collect { program ->
                batch.add(program)
                if (batch.size >= batchSize) {
                    programDao.insertAll(batch)
                    batch.clear()
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
        }
    }

    suspend fun pruneCache() = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val pastLimit = now - 12 * 60 * 60 * 1000L
        val futureLimit = now + 36 * 60 * 60 * 1000L
        programDao.pruneOldPrograms(pastLimit, futureLimit)
    }
}
