package com.brruham.diskscanner.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.brruham.diskscanner.model.FileItem
import com.brruham.diskscanner.scanner.DiskScanner
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class ScanState(
    val items: List<FileItem> = emptyList(),
    val isLoading: Boolean = false,
    val isCalculating: Boolean = false,
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
            _state.value = _state.value.copy(isLoading = true, currentPath = path, items = emptyList())
            try {
                // Phase 1: tampilkan list cepat
                val items = DiskScanner.scanPath(path)
                val (total, free) = DiskScanner.getTotalAndFree()
                _state.value = _state.value.copy(
                    items = items,
                    isLoading = false,
                    isCalculating = true,
                    totalSpace = total,
                    freeSpace = free
                )
                // Phase 2: hitung ukuran folder di background
                calcSizes(items.toMutableList())
            } catch (e: Exception) {
                _state.value = _state.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    private fun calcSizes(items: MutableList<FileItem>) {
        sizeJob = viewModelScope.launch(Dispatchers.IO) {
            items.forEachIndexed { i, item ->
                if (!item.isDirectory) return@forEachIndexed
                val size = DiskScanner.getDirSize(item.path)
                items[i] = item.copy(size = size)
                // Update list di UI thread, sort by size
                withContext(Dispatchers.Main) {
                    _state.value = _state.value.copy(
                        items = items.toList().sortedByDescending { it.size }
                    )
                }
            }
            withContext(Dispatchers.Main) {
                _state.value = _state.value.copy(isCalculating = false)
            }
        }
    }

    fun delete(item: FileItem) {
        viewModelScope.launch {
            val success = DiskScanner.deleteItem(item.path)
            if (success) {
                _state.value = _state.value.copy(
                    items = _state.value.items.filter { it.path != item.path }
                )
            }
        }
    }

    fun navigateTo(path: String) = scan(path)
}
