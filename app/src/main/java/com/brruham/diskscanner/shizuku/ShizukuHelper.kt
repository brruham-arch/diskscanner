package com.brruham.diskscanner.shizuku

import android.content.pm.PackageManager
import rikka.shizuku.Shizuku

object ShizukuHelper {

    fun isAvailable(): Boolean = try { Shizuku.pingBinder() } catch (e: Exception) { false }

    fun isGranted(): Boolean = try {
        Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
    } catch (e: Exception) { false }

    fun requestPermission(code: Int) = try { Shizuku.requestPermission(code) } catch (e: Exception) {}

    /**
     * Jalankan shell command via Shizuku
     * Shizuku.newProcess() ada di versi API 13+ sebagai public method
     */
    fun execAsShell(command: String): String? {
        return try {
            // Shizuku.newProcess tersedia via reflection pada beberapa versi
            val method = Shizuku::class.java.getMethod(
                "newProcess", Array<String>::class.java,
                Array<String>::class.java, String::class.java
            )
            val process = method.invoke(null, arrayOf("sh", "-c", command), null, null) as Process
            val output = process.inputStream.bufferedReader().readText()
            process.waitFor()
            output.ifBlank { null }
        } catch (e: Exception) {
            // Fallback: Runtime.exec biasa (tidak elevated, tapi cukup untuk /sdcard)
            try {
                val process = Runtime.getRuntime().exec(arrayOf("sh", "-c", command))
                val output = process.inputStream.bufferedReader().readText()
                process.waitFor()
                output.ifBlank { null }
            } catch (e2: Exception) { null }
        }
    }

    fun deleteFile(path: String): Boolean = execAsShell("rm -rf \"$path\"") != null
}
