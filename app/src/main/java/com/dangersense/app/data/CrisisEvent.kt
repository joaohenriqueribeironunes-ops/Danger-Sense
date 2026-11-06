package com.dangersense.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "crisis_events")
data class CrisisEvent(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val dayOfWeek: Int,
    val hourOfDay: Int,
    val motionPeak: Float,
    val decibels: Float,
    val foregroundPackage: String
)
