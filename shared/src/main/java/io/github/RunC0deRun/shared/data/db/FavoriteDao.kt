package io.github.RunC0deRun.shared.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteDao {
    @Query("SELECT channelUrl FROM favorites")
    fun getFavoriteUrlsFlow(): Flow<List<String>>

    @Query("SELECT channelUrl FROM favorites")
    suspend fun getFavoriteUrls(): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(favorite: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE channelUrl = :channelUrl")
    suspend fun delete(channelUrl: String)
}
