package com.example.myapplication.repository

import android.content.Context
import android.content.pm.PackageManager
import com.example.myapplication.database.AppDao
import com.example.myapplication.database.AppEntity
import kotlinx.coroutines.flow.Flow

class AppRepository(private val appDao: AppDao, private val context: Context) {
    fun getImportedApps(): Flow<List<AppEntity>> = appDao.getImportedApps()

    suspend fun importApp(packageName: String) {
        try {
            val appInfo = context.packageManager.getApplicationInfo(packageName, 0)
            val appLabel = context.packageManager.getApplicationLabel(appInfo).toString()

            appDao.insertApp(
                AppEntity(
                    packageName = packageName,
                    appName = appLabel,
                    isImported = true,
                    importedAt = System.currentTimeMillis()
                )
            )
        } catch (e: PackageManager.NameNotFoundException) {
            throw Exception("App not found")
        }
    }

    suspend fun removeApp(packageName: String) {
        appDao.deleteAppByPackage(packageName)
    }

    fun getAllInstalledApps(): List<Pair<String, String>> {
        val apps = mutableListOf<Pair<String, String>>()
        val packageManager = context.packageManager
        val packages = packageManager.getInstalledPackages(0)

        for (packageInfo in packages) {
            try {
                val appName = packageManager.getApplicationLabel(
                    packageManager.getApplicationInfo(packageInfo.packageName, 0)
                ).toString()
                apps.add(Pair(packageInfo.packageName, appName))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        return apps.sortedBy { it.second }
    }
}
