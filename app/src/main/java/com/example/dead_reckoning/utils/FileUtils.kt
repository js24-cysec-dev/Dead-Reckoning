package com.example.dead_reckoning.utils

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

object FileUtils {

    fun getAppDownloadsDir(): File {
        val downloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val aiDrDir = File(downloads, Constants.DIRECTORY_NAME)
        if (!aiDrDir.exists()) {
            aiDrDir.mkdirs()
        }
        return aiDrDir
    }

    fun createCsvOutputStream(context: Context, fileName: String): Pair<Uri?, OutputStream?> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "text/csv")
                put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/${Constants.DIRECTORY_NAME}")
            }
            val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
            val outputStream = uri?.let { context.contentResolver.openOutputStream(it) }
            Pair(uri, outputStream)
        } else {
            val dir = getAppDownloadsDir()
            val file = File(dir, fileName)
            val outputStream = FileOutputStream(file, true)
            Pair(Uri.fromFile(file), outputStream)
        }
    }

    fun countCsvFiles(context: Context): Int {
        return listCsvFiles(context).size
    }

    fun listCsvFiles(context: Context): List<File> {
        val dir = getAppDownloadsDir()
        return dir.listFiles { _, name -> name.endsWith(".csv") }?.toList()?.sortedByDescending { it.lastModified() } ?: emptyList()
    }
}
