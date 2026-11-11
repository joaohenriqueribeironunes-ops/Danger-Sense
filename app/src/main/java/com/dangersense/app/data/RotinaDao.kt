package com.dangersense.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface RotinaDao {
    @Insert
    suspend fun insert(item: RotinaEntity): Long

    @Query("SELECT * FROM rotina WHERE timestamp >= :since ORDER BY timestamp DESC")
    suspend fun desde(since: Long): List<RotinaEntity>

    @Query("SELECT * FROM rotina ORDER BY timestamp DESC LIMIT 80")
    fun recente(): Flow<List<RotinaEntity>>

    @Query("SELECT * FROM rotina ORDER BY timestamp DESC LIMIT :limit")
    suspend fun recenteOnce(limit: Int): List<RotinaEntity>

    @Query("UPDATE rotina SET validada = 0 WHERE id = :id")
    suspend fun invalidar(id: Long)

    @Query("SELECT COUNT(*) FROM rotina WHERE categoria = 'CRISE' AND validada = 1 AND timestamp >= :since")
    suspend fun crisesValidadas(since: Long): Int
}
