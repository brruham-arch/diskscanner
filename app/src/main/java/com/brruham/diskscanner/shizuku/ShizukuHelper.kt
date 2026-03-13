package com.brruham.diskscanner.shizuku

import android.content.pm.PackageManager
import rikka.shizuku.Shizuku
import rikka.shizuku.ShizukuRemoteProcess

object ShizukuHelper {

    fun isAvailable(): Boolean = try { Shizuku.pingBinder() } catch (e: Exception) { false }

    fun isGranted(): Boolean = try {
        Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
    } catch (e: Exception) { false }

    fun requestPermission(code: Int) = try { Shizuku.requestPermission(code) } catch (e: Exception) {}

    /**
     * Jalankan command via Shizuku (benar-benar ADB shell level)
     */
    fun execAsShell(command: String): String? {
        return try {
            val process: ShizukuRemoteProcess = Shizuku.newProcess(
                arrayOf("sh", "-c", command), null, null
            )
            val output = process.inputStream.bufferedReader().readText()
            process.waitFor()
            output.ifBlank { null }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun deleteFile(path: String): Boolean = execAsShell("rm -rf \"$path\"") != null
}
