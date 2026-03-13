package com.brruham.diskscanner.scanner

import com.brruham.diskscanner.model.FileItem
import com.brruham.diskscanner.shizuku.ShizukuHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object DiskScanner {

    suspend fun scanPath(path: String): List<FileItem> = withContext(Dispatchers.IO) {
        val items = mutableListOf<FileItem>()
        val dir = File(path)

        // Coba baca via Java File API dulu
        val files = try { dir.listFiles() } catch (e: Exception) { null }

        if (!files.isNullOrEmpty()) {
            files.forEach { file ->
                items.add(FileItem(
                    path = file.absolutePath,
                    name = file.name,
                    size = 0L,
                    isDirectory = file.isDirectory
                ))
            }
        } else if (ShizukuHelper.isGranted()) {
            // Fallback Shizuku untuk folder yang tidak bisa dibaca (Android/data)
            val output = ShizukuHelper.execAsShell("ls \"$path\" 2>/dev/null") ?: ""
            output.lines().filter { it.isNotBlank() }.forEach { name ->
                val fullPath = "$path/$name"
                items.add(FileItem(
                    path = fullPath,
                    name = name,
                    size = 0L,
                    isDirectory = true
                ))
            }
        }

        items
    }

    suspend fun getSizeOf(path: String, useShizuku: Boolean): Long = withContext(Dispatchers.IO) {
        try {
            val f = File(path)
            if (f.exists() && f.canRead()) {
                if (f.isFile) return@withContext f.length()
                // Hitung rekursif via Java
                var total = 0L
                f.walkTopDown().forEach { if (it.isFile) total += it.length() }
                return@withContext total
            }
            // Tidak bisa baca — coba Shizuku
            if (useShizuku) {
                val out = ShizukuHelper.execAsShell("du -sk \"$path\" 2>/dev/null") ?: return@withContext 0L
                out.trim().split(Regex("\\s+")).firstOrNull()?.toLongOrNull()?.times(1024) ?: 0L
            } else 0L
        } catch (e: Exception) { 0L }
    }

    suspend fun deleteItem(path: String): Boolean = withContext(Dispatchers.IO) {
        val f = File(path)
        if (f.exists()) return@withContext f.deleteRecursively()
        if (ShizukuHelper.isGranted()) {
            ShizukuHelper.execAsShell("rm -rf \"$path\"") != null
        } else false
    }

    fun getTotalAndFree(): Pair<Long, Long> {
        val f = File("/sdcard")
        return Pair(f.totalSpace, f.freeSpace)
    }
}
