package io.github.RunC0deRun.shared.data.epg

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import java.util.concurrent.TimeUnit

class EpgSyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val epgUrl = inputData.getString(KEY_EPG_URL) ?: return Result.failure()
        
        val fetcher = EpgFetcher(applicationContext)
        val result = fetcher.fetchAndSyncEpg(epgUrl)
        
        return if (result.isSuccess) {
            Result.success()
        } else {
            Result.retry()
        }
    }

    companion object {
        const val KEY_EPG_URL = "epg_url"
        const val WORK_NAME = "epg_sync_work"

        fun schedule(context: Context, epgUrl: String) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.UNMETERED)
                .build()

            val workRequest = PeriodicWorkRequestBuilder<EpgSyncWorker>(24, TimeUnit.HOURS)
                .setConstraints(constraints)
                .setInputData(workDataOf(KEY_EPG_URL to epgUrl))
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                workRequest
            )
        }
    }
}
