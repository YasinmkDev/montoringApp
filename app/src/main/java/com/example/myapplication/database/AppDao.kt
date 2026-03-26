package com.example.myapplication.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {
    @Query("SELECT * FROM apps WHERE isImported = 1 ORDER BY importedAt DESC")
    fun getImportedApps(): Flow<List<AppEntity>>

    @Query("SELECT * FROM apps WHERE isImported = 1")
    suspend fun getImportedAppsList(): List<AppEntity>

    @Insert
    suspend fun insertApp(app: AppEntity)

    @Update
    suspend fun updateApp(app: AppEntity)

    @Delete
    suspend fun deleteApp(app: AppEntity)

    @Query("DELETE FROM apps WHERE packageName = :packageName")
    suspend fun deleteAppByPackage(packageName: String)

    @Query("SELECT * FROM apps WHERE packageName = :packageName")
    suspend fun getApp(packageName: String): AppEntity?
}
