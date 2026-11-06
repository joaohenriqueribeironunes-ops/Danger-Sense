package com.dangersense.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [CrisisEvent::class, NeuroHabitsLog::class],
    version = 2,
    exportSchema = false
)
abstract class CrisisDatabase : RoomDatabase() {
    abstract fun crisisDao(): CrisisDao
    abstract fun neuroHabitsDao(): NeuroHabitsDao

    companion object {
        @Volatile private var instance: CrisisDatabase? = null

        fun get(context: Context): CrisisDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    CrisisDatabase::class.java,
                    "sense_calm.db"
                ).fallbackToDestructiveMigration().build().also { instance = it }
            }
    }
}
