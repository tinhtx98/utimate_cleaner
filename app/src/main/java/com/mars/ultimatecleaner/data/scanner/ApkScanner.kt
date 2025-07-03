package com.mars.ultimatecleaner.data.scanner

import android.content.Context
import android.content.pm.PackageManager
import com.mars.ultimatecleaner.domain.model.ApkFile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ApkScanner @Inject constructor(
    private val context: Context
) {

    fun scanApkFiles(): Flow<List<ApkFile>> = flow {
        val apkFiles = mutableListOf<ApkFile>()

        // Scan download folder for APK files
        val downloadDir = File(context.getExternalFilesDir(null)?.parent, "Download")
        if (downloadDir.exists()) {
            scanDirectoryForApks(downloadDir, apkFiles)
        }

        // Scan other common directories
        val commonDirs = listOf(
            File("/sdcard/Download"),
            File("/sdcard/Downloads"),
            context.getExternalFilesDir(null)
        )

        commonDirs.forEach { dir ->
            if (dir?.exists() == true) {
                scanDirectoryForApks(dir, apkFiles)
            }
        }

        emit(apkFiles)
    }

    private fun scanDirectoryForApks(directory: File, apkFiles: MutableList<ApkFile>) {
        directory.listFiles()?.forEach { file ->
            when {
                file.isFile && file.extension.equals("apk", ignoreCase = true) -> {
                    val apkInfo = getApkInfo(file)
                    apkFiles.add(
                        ApkFile(
                            path = file.absolutePath,
                            name = file.name,
                            size = file.length(),
                            packageName = apkInfo.first,
                            versionName = apkInfo.second,
                            isInstalled = isPackageInstalled(apkInfo.first),
                            lastModified = file.lastModified()
                        )
                    )
                }
                file.isDirectory -> {
                    scanDirectoryForApks(file, apkFiles)
                }
            }
        }
    }

    private fun getApkInfo(apkFile: File): Pair<String, String> {
        return try {
            val packageInfo = context.packageManager.getPackageArchiveInfo(
                apkFile.absolutePath,
                PackageManager.GET_META_DATA
            )
            val packageName = packageInfo?.packageName ?: "unknown"
            val versionName = packageInfo?.versionName ?: "unknown"
            Pair(packageName, versionName)
        } catch (e: Exception) {
            Pair("unknown", "unknown")
        }
    }

    private fun isPackageInstalled(packageName: String): Boolean {
        return try {
            context.packageManager.getPackageInfo(packageName, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }
}
