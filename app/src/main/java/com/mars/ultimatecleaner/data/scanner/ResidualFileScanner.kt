package com.mars.ultimatecleaner.data.scanner

import android.content.Context
import android.content.pm.PackageManager
import com.mars.ultimatecleaner.domain.model.ResidualFile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ResidualFileScanner @Inject constructor(
    private val context: Context
) {

    fun scanResidualFiles(): Flow<List<ResidualFile>> = flow {
        val residualFiles = mutableListOf<ResidualFile>()

        // Get list of installed packages
        val installedPackages = context.packageManager.getInstalledPackages(0)
            .map { it.packageName }.toSet()

        // Scan common app data directories
        val dataDirs = listOf(
            File("/sdcard/Android/data"),
            File("/sdcard/Android/obb"),
            context.getExternalFilesDir(null)?.parentFile
        )

        dataDirs.forEach { dir ->
            if (dir?.exists() == true && dir.canRead()) {
                scanForResidualFiles(dir, residualFiles, installedPackages)
            }
        }

        emit(residualFiles)
    }

    private fun scanForResidualFiles(
        directory: File,
        residualFiles: MutableList<ResidualFile>,
        installedPackages: Set<String>
    ) {
        directory.listFiles()?.forEach { file ->
            if (file.isDirectory) {
                // Check if this directory belongs to an uninstalled app
                val dirName = file.name
                val isResidual = dirName.contains(".") &&
                    !installedPackages.contains(dirName) &&
                    isLikelyPackageName(dirName)

                if (isResidual) {
                    val totalSize = calculateDirectorySize(file)
                    residualFiles.add(
                        ResidualFile(
                            path = file.absolutePath,
                            name = file.name,
                            size = totalSize,
                            packageName = dirName,
                            lastModified = file.lastModified()
                        )
                    )
                }
            }
        }
    }

    private fun isLikelyPackageName(name: String): Boolean {
        // Basic heuristic to identify package names
        return name.matches(Regex("^[a-z][a-z0-9_]*(\\.[a-z][a-z0-9_]*)+$"))
    }

    private fun calculateDirectorySize(directory: File): Long {
        var size = 0L
        directory.walkTopDown().forEach { file ->
            if (file.isFile) {
                size += file.length()
            }
        }
        return size
    }
}
