package com.dangersense.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CrisisDao {
    @Insert
    suspend fun insert(event: CrisisEvent): Long

    @Query("UPDATE crisis_events SET crisisType = :type WHERE id = :id")
    suspend fun assignType(id: Long, type: String)

    @Query("SELECT * FROM crisis_events ORDER BY timestamp DESC LIMIT :limit")
    fun recent(limit: Int = 200): Flow<List<CrisisEvent>>

    @Query("SELECT * FROM crisis_events ORDER BY timestamp DESC LIMIT :limit")
    suspend fun recentOnce(limit: Int = 200): List<CrisisEvent>

    @Query("SELECT COUNT(*) FROM crisis_events WHERE timestamp >= :since")
    suspend fun countSince(since: Long): Int

    @Query("SELECT crisisType FROM crisis_events ORDER BY timestamp DESC LIMIT 1")
    suspend fun lastCrisisType(): String?

    @Query("SELECT COUNT(*) FROM crisis_events")
    fun count(): Flow<Int>
}
