package io.github.runc0derun.watcharr.shared.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface DvrRecordingDao {

    @Query("SELECT * FROM dvr_recordings ORDER BY startEpochMs DESC")
    fun getAllRecordingsFlow(): Flow<List<DvrRecordingEntity>>

    @Query("SELECT * FROM dvr_recordings ORDER BY startEpochMs DESC")
    suspend fun getAllRecordings(): List<DvrRecordingEntity>

    @Query("SELECT * FROM dvr_recordings WHERE id = :id LIMIT 1")
    suspend fun getRecordingById(id: String): DvrRecordingEntity?

    @Query("SELECT * FROM dvr_recordings WHERE status = 'SCHEDULED' AND startEpochMs >= :nowMs ORDER BY startEpochMs ASC")
    suspend fun getUpcomingScheduledRecordings(nowMs: Long): List<DvrRecordingEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecording(recording: DvrRecordingEntity)

    @Update
    suspend fun updateRecording(recording: DvrRecordingEntity)

    @Query("UPDATE dvr_recordings SET status = :status, localFilePath = :localFilePath, errorReason = :errorReason WHERE id = :id")
    suspend fun updateStatus(id: String, status: String, localFilePath: String? = null, errorReason: String? = null)

    @Query("DELETE FROM dvr_recordings WHERE id = :id")
    suspend fun deleteRecording(id: String)

    @Query("DELETE FROM dvr_recordings")
    suspend fun clearAll()
}
