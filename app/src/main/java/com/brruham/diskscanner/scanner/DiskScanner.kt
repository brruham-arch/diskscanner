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
        val files = try { dir.listFiles() } catch (e: Exception) { null }

        if (!files.isNullOrEmpty()) {
            files.forEach { file ->
                // Langsung hitung ukuran saat scan
                val size = if (file.isFile) {
                    file.length()
                } else {
                    calcDirSize(file)
                }
                items.add(FileItem(
                    path = file.absolutePath,
                    name = file.name,
                    size = size,
                    isDirectory = file.isDirectory
                ))
            }
        } else if (ShizukuHelper.isGranted()) {
            // Android/data — pakai Shizuku
            val output = ShizukuHelper.execAsShell("ls \"$path\" 2>/dev/null") ?: ""
            output.lines().filter { it.isNotBlank() }.forEach { name ->
                val fullPath = "$path/$name"
                val size = ShizukuHelper.execAsShell(
                    "du -sk \"$fullPath\" 2>/dev/null | cut -f1"
                )?.trim()?.toLongOrNull()?.times(1024) ?: 0L
                items.add(FileItem(
                    path = fullPath,
                    name = name,
                    size = size,
                    isDirectory = true
                ))
            }
        }

        // Urutkan terbesar dulu
        items.sortedByDescending { it.size }
    }

    private fun calcDirSize(dir: File): Long {
        var total = 0L
        try {
            dir.walkTopDown()
                .onEnter { it.canRead() }
                .forEach { if (it.isFile) total += it.length() }
        } catch (e: Exception) {}
        return total
    }

    suspend fun deleteItem(path: String): Boolean = withContext(Dispatchers.IO) {
        val f = File(path)
        if (f.exists()) return@withContext f.deleteRecursively()
        ShizukuHelper.execAsShell("rm -rf \"$path\"") != null
    }

    fun getTotalAndFree(): Pair<Long, Long> {
        val f = File("/sdcard")
        return Pair(f.totalSpace, f.freeSpace)
    }
}
