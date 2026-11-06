package com.dangersense.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface IngestaoDao {
    @Insert
    suspend fun insert(item: IngestaoEntity): Long

    @Query("SELECT * FROM ingestao ORDER BY timestamp DESC LIMIT 50")
    fun recent(): Flow<List<IngestaoEntity>>

    @Query("SELECT * FROM ingestao WHERE timestamp >= :since ORDER BY timestamp DESC")
    suspend fun recentSince(since: Long): List<IngestaoEntity>
}
