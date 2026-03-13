package com.brruham.diskscanner.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.brruham.diskscanner.model.FileItem
import com.brruham.diskscanner.scanner.DiskScanner
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

    init { scan("/sdcard") }

    fun scan(path: String = _state.value.currentPath) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, currentPath = path, error = null)
            try {
                val items = DiskScanner.scanPath(path)
                val (total, free) = DiskScanner.getTotalAndFree()
                _state.value = _state.value.copy(
                    items = items,
                    isLoading = false,
                    totalSpace = total,
                    freeSpace = free
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(isLoading = false, error = e.message)
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
