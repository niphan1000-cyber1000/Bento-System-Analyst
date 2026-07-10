package com.aistudio.aisystemanalyst.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.aistudio.aisystemanalyst.data.model.AuditLog
import com.aistudio.aisystemanalyst.data.model.Project
import com.aistudio.aisystemanalyst.data.model.ProjectTask
import com.aistudio.aisystemanalyst.data.model.RiskItem

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [Project::class, ProjectTask::class, RiskItem::class, AuditLog::class, com.aistudio.aisystemanalyst.data.model.User::class],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun projectDao(): ProjectDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `users` (
                        `email` TEXT NOT NULL, 
                        `passwordHash` TEXT NOT NULL, 
                        `role` TEXT NOT NULL, 
                        `createdAt` INTEGER NOT NULL, 
                        PRIMARY KEY(`email`)
                    )
                """.trimIndent())
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `users` ADD COLUMN `salt` TEXT NOT NULL DEFAULT ''")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "ai_analyst_database"
                )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
