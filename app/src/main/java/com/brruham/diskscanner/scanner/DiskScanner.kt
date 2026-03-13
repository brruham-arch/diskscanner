package com.brruham.diskscanner.scanner

import com.brruham.diskscanner.model.FileItem
import com.brruham.diskscanner.shizuku.ShizukuHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object DiskScanner {

    // Phase 1: list cepat, size = 0 dulu
    suspend fun scanPath(path: String): List<FileItem> = withContext(Dispatchers.IO) {
        val items = mutableListOf<FileItem>()
        val dir = File(path)
        val files = try { dir.listFiles() } catch (e: Exception) { null }

        if (!files.isNullOrEmpty()) {
            files.forEach { file ->
                items.add(FileItem(
                    path = file.absolutePath,
                    name = file.name,
                    size = if (file.isFile) file.length() else 0L,
                    isDirectory = file.isDirectory
                ))
            }
        } else if (ShizukuHelper.isGranted()) {
            val output = ShizukuHelper.execAsShell("ls \"$path\" 2>/dev/null") ?: ""
            output.lines().filter { it.isNotBlank() }.forEach { name ->
                items.add(FileItem(
                    path = "$path/$name",
                    name = name,
                    size = 0L,
                    isDirectory = true
                ))
            }
        }
        items
    }

    // Phase 2: hitung size 1 folder, dipanggil lazy per item
    suspend fun getDirSize(path: String): Long = withContext(Dispatchers.IO) {
        try {
            if (ShizukuHelper.isGranted()) {
                val out = ShizukuHelper.execAsShell("du -sk \"$path\" 2>/dev/null | cut -f1") ?: return@withContext 0L
                out.trim().toLongOrNull()?.times(1024) ?: 0L
            } else {
                var total = 0L
                File(path).walkTopDown()
                    .onEnter { it.canRead() }
                    .filter { it.isFile }
                    .forEach { total += it.length() }
                total
            }
        } catch (e: Exception) { 0L }
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
