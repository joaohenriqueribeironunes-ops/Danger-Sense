package com.dangersense.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "neuro_habits_log")
data class NeuroHabitsLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val dateString: String,
    val sleepDurationMinutes: Int,
    val dietSummary: String,
    val dietNeuroImpactScore: Float,
    val activitiesSummary: String,
    val maximalDetectedDb: Float,
    val maximalDbTimestamp: Long,
    val registeredTemperature: Float,
    val predictedCrisisProbability: Float,
    val actualCrisisOccurred: Boolean,
    val assignedCrisisType: String?
)
