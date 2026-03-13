package com.brruham.diskscanner.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.brruham.diskscanner.model.FileItem
import com.brruham.diskscanner.scanner.DiskScanner
import com.brruham.diskscanner.shizuku.ShizukuHelper
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class ScanState(
    val items: List<FileItem> = emptyList(),
    val isLoading: Boolean = false,
    val currentPath: String = "/sdcard",
    val totalSpace: Long = 0L,
    val freeSpace: Long = 0L,
    val error: String? = null
)

class ScanViewModel : ViewModel() {

    private val _state = MutableStateFlow(ScanState())
    val state: StateFlow<ScanState> = _state
    private var sizeJob: Job? = null

    init { scan("/sdcard") }

    fun scan(path: String = _state.value.currentPath) {
        sizeJob?.cancel()
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, currentPath = path, error = null, items = emptyList())
            try {
                // 1. Tampilkan list dulu (cepat)
                val items = DiskScanner.scanPath(path).toMutableList()
                val (total, free) = DiskScanner.getTotalAndFree()
                _state.value = _state.value.copy(
                    items = items.toList(),
                    isLoading = false,
                    totalSpace = total,
                    freeSpace = free
                )
                // 2. Hitung ukuran di background
                loadSizesLazy(items, path)
            } catch (e: Exception) {
                _state.value = _state.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    private fun loadSizesLazy(items: MutableList<FileItem>, path: String) {
        sizeJob = viewModelScope.launch {
            val useShizuku = ShizukuHelper.isGranted()
            items.forEachIndexed { i, item ->
                val size = DiskScanner.getSizeOf(item.path, useShizuku)
                items[i] = item.copy(size = size)
                // Update UI setiap item selesai dihitung
                val sorted = items.toList().sortedByDescending { it.size }
                _state.value = _state.value.copy(items = sorted)
            }
        }
    }

    fun delete(item: FileItem) {
        viewModelScope.launch {
            val success = DiskScanner.deleteItem(item.path)
            if (success) {
                val newItems = _state.value.items.filter { it.path != item.path }
                _state.value = _state.value.copy(items = newItems)
            }
        }
    }

    fun navigateTo(path: String) = scan(path)
}
