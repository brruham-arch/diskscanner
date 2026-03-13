package com.brruham.diskscanner.model

data class FileItem(
    val path: String,
    val name: String,
    val size: Long,
    val isDirectory: Boolean,
    val childCount: Int = 0
) {
    fun formattedSize(): String = when {
        size >= 1_073_741_824 -> "%.1f GB".format(size / 1_073_741_824.0)
        size >= 1_048_576     -> "%.1f MB".format(size / 1_048_576.0)
        size >= 1_024         -> "%.1f KB".format(size / 1_024.0)
        else                  -> "$size B"
    }
}
