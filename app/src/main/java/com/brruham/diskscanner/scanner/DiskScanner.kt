package com.brruham.diskscanner.scanner

import com.brruham.diskscanner.model.FileItem
import com.brruham.diskscanner.shizuku.ShizukuHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object DiskScanner {

    // Root paths yang akan di-scan
    val SCAN_ROOTS = listOf(
        "/sdcard",
        "/sdcard/Android/data",
        "/sdcard/Android/obb",
        "/sdcard/Download",
        "/sdcard/DCIM",
        "/sdcard/Pictures",
        "/sdcard/Movies",
        "/sdcard/Music"
    )

    suspend fun scanPath(path: String): List<FileItem> = withContext(Dispatchers.IO) {
        val items = mutableListOf<FileItem>()
        val useShizuku = ShizukuHelper.isGranted()

        if (useShizuku) {
            // Pakai Shizuku — bisa baca Android/data
            scanViaShizuku(path, items)
        } else {
            // Fallback biasa
            scanViaJava(File(path), items)
        }

        // Urutkan terbesar → terkecil
        items.sortByDescending { it.size }
        items
    }

    private fun scanViaShizuku(path: String, items: MutableList<FileItem>) {
        val output = ShizukuHelper.listWithSize(path) ?: return
        output.lines().forEach { line ->
            if (line.isBlank()) return@forEach
            val parts = line.trim().split("\t")
            if (parts.size < 2) return@forEach
            val sizeKb = parts[0].toLongOrNull() ?: return@forEach
            val filePath = parts[1].trim()
            val file = File(filePath)
            items.add(FileItem(
                path = filePath,
                name = file.name,
                size = sizeKb * 1024,
                isDirectory = true, // du output always dirs
                childCount = 0
            ))
        }
    }

    private fun scanViaJava(dir: File, items: MutableList<FileItem>) {
        if (!dir.exists() || !dir.canRead()) return
        dir.listFiles()?.forEach { file ->
            val size = if (file.isDirectory) calculateDirSize(file) else file.length()
            items.add(FileItem(
                path = file.absolutePath,
                name = file.name,
                size = size,
                isDirectory = file.isDirectory,
                childCount = if (file.isDirectory) file.listFiles()?.size ?: 0 else 0
            ))
        }
    }

    private fun calculateDirSize(dir: File): Long {
        var size = 0L
        dir.walkTopDown().forEach { file ->
            if (file.isFile) size += file.length()
        }
        return size
    }

    suspend fun deleteItem(path: String): Boolean = withContext(Dispatchers.IO) {
        if (ShizukuHelper.isGranted()) {
            ShizukuHelper.deleteFile(path)
        } else {
            File(path).deleteRecursively()
        }
    }

    fun getTotalAndFree(): Pair<Long, Long> {
        val file = File("/sdcard")
        return Pair(file.totalSpace, file.freeSpace)
    }
}
