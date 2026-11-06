package com.dangersense.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CrisisDao {
    @Insert
    suspend fun insert(event: CrisisEvent): Long

    @Query("SELECT * FROM crisis_events ORDER BY timestamp DESC LIMIT :limit")
    fun recent(limit: Int = 200): Flow<List<CrisisEvent>>

    @Query("SELECT * FROM crisis_events ORDER BY timestamp DESC LIMIT :limit")
    suspend fun recentOnce(limit: Int = 200): List<CrisisEvent>

    @Query("SELECT COUNT(*) FROM crisis_events")
    fun count(): Flow<Int>
}
