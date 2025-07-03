package com.mars.ultimatecleaner.data.manager

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import com.mars.ultimatecleaner.domain.model.FileItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ScopedStorageManager @Inject constructor(
    private val context: Context
) {

    private val contentResolver: ContentResolver = context.contentResolver

    @RequiresApi(Build.VERSION_CODES.Q)
    suspend fun getMediaFiles(mimeTypeFilter: String? = null): List<FileItem> = withContext(Dispatchers.IO) {
        val files = mutableListOf<FileItem>()

        val uri = MediaStore.Files.getContentUri("external")
        val projection = arrayOf(
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.DISPLAY_NAME,
            MediaStore.Files.FileColumns.SIZE,
            MediaStore.Files.FileColumns.DATE_MODIFIED,
            MediaStore.Files.FileColumns.MIME_TYPE,
            MediaStore.Files.FileColumns.DATA
        )

        val selection = if (mimeTypeFilter != null) {
            "${MediaStore.Files.FileColumns.MIME_TYPE} LIKE ?"
        } else {
            null
        }

        val selectionArgs = if (mimeTypeFilter != null) {
            arrayOf("$mimeTypeFilter%")
        } else {
            null
        }

        contentResolver.query(
            uri,
            projection,
            selection,
            selectionArgs,
            "${MediaStore.Files.FileColumns.DATE_MODIFIED} DESC"
        )?.use { cursor ->
            while (cursor.moveToNext()) {
                try {
                    val file = createFileItemFromCursor(cursor)
                    if (file != null) {
                        files.add(file)
                    }
                } catch (e: Exception) {
                    // Continue with next file
                }
            }
        }

        files
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    suspend fun getImageFiles(): List<FileItem> = withContext(Dispatchers.IO) {
        getMediaFilesFromCollection(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            arrayOf(
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DISPLAY_NAME,
                MediaStore.Images.Media.SIZE,
                MediaStore.Images.Media.DATE_MODIFIED,
                MediaStore.Images.Media.MIME_TYPE,
                MediaStore.Images.Media.DATA,
                MediaStore.Images.Media.WIDTH,
                MediaStore.Images.Media.HEIGHT
            )
        )
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    suspend fun getVideoFiles(): List<FileItem> = withContext(Dispatchers.IO) {
        getMediaFilesFromCollection(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            arrayOf(
                MediaStore.Video.Media._ID,
                MediaStore.Video.Media.DISPLAY_NAME,
                MediaStore.Video.Media.SIZE,
                MediaStore.Video.Media.DATE_MODIFIED,
                MediaStore.Video.Media.MIME_TYPE,
                MediaStore.Video.Media.DATA,
                MediaStore.Video.Media.DURATION
            )
        )
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    suspend fun getAudioFiles(): List<FileItem> = withContext(Dispatchers.IO) {
        getMediaFilesFromCollection(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            arrayOf(
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.DISPLAY_NAME,
                MediaStore.Audio.Media.SIZE,
                MediaStore.Audio.Media.DATE_MODIFIED,
                MediaStore.Audio.Media.MIME_TYPE,
                MediaStore.Audio.Media.DATA,
                MediaStore.Audio.Media.DURATION
            )
        )
    }

    private suspend fun getMediaFilesFromCollection(
        uri: Uri,
        projection: Array<String>
    ): List<FileItem> = withContext(Dispatchers.IO) {
        val files = mutableListOf<FileItem>()

        contentResolver.query(
            uri,
            projection,
            null,
            null,
            "${MediaStore.MediaColumns.DATE_MODIFIED} DESC"
        )?.use { cursor ->
            while (cursor.moveToNext()) {
                try {
                    val file = createFileItemFromCursor(cursor)
                    if (file != null) {
                        files.add(file)
                    }
                } catch (e: Exception) {
                    // Continue with next file
                }
            }
        }

        files
    }

    private fun createFileItemFromCursor(cursor: Cursor): FileItem? {
        return try {
            val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID))
            val name = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME))
            val size = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE))
            val dateModified = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_MODIFIED)) * 1000
            val mimeType = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.MIME_TYPE)) ?: "unknown"
            val data = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA))

            FileItem(
                path = data ?: "",
                name = name ?: "Unknown",
                size = size,
                lastModified = dateModified,
                mimeType = mimeType,
                isDirectory = false,
                thumbnailPath = null
            )
        } catch (e: Exception) {
            null
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    suspend fun deleteMediaFile(fileId: Long, uri: Uri): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            val fileUri = ContentUris.withAppendedId(uri, fileId)
            val deletedRows = contentResolver.delete(fileUri, null, null)
            deletedRows > 0
        } catch (e: Exception) {
            false
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    suspend fun requestDeletePermission(fileIds: List<Long>, uri: Uri): Boolean {
        return try {
            val urisToDelete = fileIds.map { id ->
                ContentUris.withAppendedId(uri, id)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val pendingIntent = MediaStore.createDeleteRequest(contentResolver, urisToDelete)
                // This would need to be handled in the activity
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    fun canAccessWithoutPermission(path: String): Boolean {
        val appSpecificDirs = listOf(
            context.filesDir.absolutePath,
            context.cacheDir.absolutePath,
            context.getExternalFilesDir(null)?.absolutePath,
            context.externalCacheDir?.absolutePath
        ).filterNotNull()

        return appSpecificDirs.any { path.startsWith(it) }
    }

    fun isMediaFile(mimeType: String): Boolean {
        return mimeType.startsWith("image/") ||
                mimeType.startsWith("video/") ||
                mimeType.startsWith("audio/")
    }

    fun getMediaStoreUri(mimeType: String): Uri {
        return when {
            mimeType.startsWith("image/") -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            mimeType.startsWith("video/") -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            mimeType.startsWith("audio/") -> MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            else -> MediaStore.Files.getContentUri("external")
        }
    }
}