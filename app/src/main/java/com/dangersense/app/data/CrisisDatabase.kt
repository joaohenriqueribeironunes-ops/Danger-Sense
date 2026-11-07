package com.dangersense.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [CrisisEvent::class, NeuroHabitsLog::class, IngestaoEntity::class, RotinaEntity::class],
    version = 4,
    exportSchema = false
)
abstract class CrisisDatabase : RoomDatabase() {
    abstract fun crisisDao(): CrisisDao
    abstract fun neuroHabitsDao(): NeuroHabitsDao
    abstract fun ingestaoDao(): IngestaoDao
    abstract fun rotinaDao(): RotinaDao

    companion object {
        @Volatile private var instance: CrisisDatabase? = null

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS ingestao (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "timestamp INTEGER NOT NULL, " +
                        "nome TEXT NOT NULL, " +
                        "categoria TEXT NOT NULL, " +
                        "impacto_tea REAL NOT NULL, " +
                        "impacto_tdah REAL NOT NULL, " +
                        "mecanismo_cientifico TEXT NOT NULL, " +
                        "risco_gatilho REAL NOT NULL, " +
                        "fontes_cientificas TEXT NOT NULL)"
                )
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS rotina (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "timestamp INTEGER NOT NULL, " +
                        "categoria TEXT NOT NULL, " +
                        "rotulo TEXT NOT NULL, " +
                        "metrica REAL NOT NULL, " +
                        "duracaoMinutos INTEGER NOT NULL, " +
                        "interacoesSociais INTEGER NOT NULL, " +
                        "impactoTea REAL NOT NULL, " +
                        "impactoTdah REAL NOT NULL, " +
                        "riscoGatilho REAL NOT NULL, " +
                        "detalhe TEXT NOT NULL, " +
                        "validada INTEGER NOT NULL)"
                )
            }
        }

        fun get(context: Context): CrisisDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    CrisisDatabase::class.java,
                    "sense_calm.db"
                )
                    .addMigrations(MIGRATION_2_3, MIGRATION_3_4)
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { instance = it }
            }
    }
}
