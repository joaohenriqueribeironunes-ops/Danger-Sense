package com.dangersense.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface NeuroHabitsDao {
    @Insert
    suspend fun insert(log: NeuroHabitsLog): Long

    @Query("SELECT * FROM neuro_habits_log ORDER BY timestamp DESC LIMIT 30")
    fun recent(): Flow<List<NeuroHabitsLog>>

    @Query("SELECT * FROM neuro_habits_log ORDER BY timestamp DESC LIMIT 1")
    suspend fun latest(): NeuroHabitsLog?
}
