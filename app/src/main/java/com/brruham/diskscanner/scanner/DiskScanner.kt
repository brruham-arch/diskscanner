package com.brruham.diskscanner.scanner

import com.brruham.diskscanner.model.FileItem
import com.brruham.diskscanner.shizuku.ShizukuHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object DiskScanner {

    suspend fun scanPath(path: String): List<FileItem> = withContext(Dispatchers.IO) {
        val items = mutableListOf<FileItem>()
        val useShizuku = ShizukuHelper.isGranted()

        if (useShizuku) {
            // List via shell — cepat, bisa baca Android/data
            val output = ShizukuHelper.exec("ls \"$path\" 2>/dev/null") ?: ""
            output.lines().filter { it.isNotBlank() }.forEach { name ->
                val fullPath = "$path/$name"
                // Ukuran awal 0, akan di-update lazy
                val isDir = ShizukuHelper.exec("[ -d \"$fullPath\" ] && echo dir || echo file")
                    ?.trim() == "dir"
                items.add(FileItem(
                    path = fullPath,
                    name = name,
                    size = 0L,
                    isDirectory = isDir
                ))
            }
        } else {
            // Java File API — cepat tapi tidak bisa Android/data
            File(path).listFiles()?.forEach { file ->
                items.add(FileItem(
                    path = file.absolutePath,
                    name = file.name,
                    size = 0L,
                    isDirectory = file.isDirectory
                ))
            }
        }

        items
    }

    // Hitung ukuran 1 item saja — dipanggil lazy per item
    suspend fun getSizeOf(path: String, useShizuku: Boolean): Long = withContext(Dispatchers.IO) {
        try {
            if (useShizuku) {
                val out = ShizukuHelper.exec("du -sk \"$path\" 2>/dev/null") ?: return@withContext 0L
                out.trim().split(Regex("\\s+")).firstOrNull()?.toLongOrNull()?.times(1024) ?: 0L
            } else {
                val f = File(path)
                if (f.isFile) f.length()
                else f.walkTopDown().filter { it.isFile }.sumOf { it.length() }
            }
        } catch (e: Exception) { 0L }
    }

    suspend fun deleteItem(path: String): Boolean = withContext(Dispatchers.IO) {
        if (ShizukuHelper.isGranted()) {
            ShizukuHelper.deleteFile(path)
        } else {
            File(path).deleteRecursively()
        }
    }

    fun getTotalAndFree(): Pair<Long, Long> {
        val f = File("/sdcard")
        return Pair(f.totalSpace, f.freeSpace)
    }
}
