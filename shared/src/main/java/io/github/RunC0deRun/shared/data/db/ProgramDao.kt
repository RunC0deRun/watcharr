package io.github.runc0derun.shared.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ProgramDao {

    @Query("SELECT * FROM programs WHERE start <= :now AND stop >= :now")
    fun getActiveProgramsFlow(now: Long): Flow<List<ProgramEntity>>

    @Query("SELECT * FROM programs WHERE start > :now ORDER BY start ASC")
    fun getAllUpcomingProgramsFlow(now: Long): Flow<List<ProgramEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(programs: List<ProgramEntity>)

    @Query("DELETE FROM programs WHERE stop < :pastThreshold OR start > :futureThreshold")
    suspend fun pruneOldPrograms(pastThreshold: Long, futureThreshold: Long)

    @Query("DELETE FROM programs")
    suspend fun deleteAll()
}
