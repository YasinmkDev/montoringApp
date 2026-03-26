package com.example.myapplication.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [AppEntity::class], version = 1, exportSchema = false)
abstract class GuardianHubDatabase : RoomDatabase() {
    abstract fun appDao(): AppDao

    companion object {
        @Volatile
        private var instance: GuardianHubDatabase? = null

        fun getInstance(context: Context): GuardianHubDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    GuardianHubDatabase::class.java,
                    "guardian_hub_db"
                ).build().also { instance = it }
            }
        }
    }
}
