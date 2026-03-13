package com.brruham.diskscanner.shizuku

import android.content.pm.PackageManager
import rikka.shizuku.Shizuku

object ShizukuHelper {

    fun isAvailable(): Boolean = try {
        Shizuku.pingBinder()
    } catch (e: Exception) { false }

    fun isGranted(): Boolean = try {
        if (Shizuku.isPreV11() || Shizuku.getVersion() < 11) {
            false
        } else {
            Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        }
    } catch (e: Exception) { false }

    fun requestPermission(requestCode: Int) {
        try {
            Shizuku.requestPermission(requestCode)
        } catch (e: Exception) { e.printStackTrace() }
    }

    /**
     * Jalankan shell command via Shizuku (setara ADB shell)
     * Return: output string, null jika gagal
     */
    fun exec(command: String): String? {
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("sh", "-c", command))
            val output = process.inputStream.bufferedReader().readText()
            process.waitFor()
            output
        } catch (e: Exception) {
            null
        }
    }

    /**
     * List semua file + ukuran di path tertentu via du command
     * Ini yang membuat kita bisa baca Android/data
     */
    fun listWithSize(path: String): String? = exec("du -s $path/* 2>/dev/null")

    fun deleteFile(path: String): Boolean {
        val result = exec("rm -rf \"$path\"")
        return result != null
    }

    fun getDirectorySize(path: String): Long {
        val output = exec("du -sk \"$path\" 2>/dev/null") ?: return 0L
        return output.trim().split("\t").firstOrNull()?.toLongOrNull()?.times(1024) ?: 0L
    }
}
