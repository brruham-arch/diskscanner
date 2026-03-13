package com.brruham.diskscanner.ui

import androidx.lifecycle.*
import com.brruham.diskscanner.model.FileItem
import com.brruham.diskscanner.model.FilterType
import com.brruham.diskscanner.model.SortOrder
import com.brruham.diskscanner.scanner.DiskScanner
import kotlinx.coroutines.*

class MainViewModel : ViewModel() {

    private val _items = MutableLiveData<List<FileItem>>(emptyList())
    val items: LiveData<List<FileItem>> = _items

    private val _scanning = MutableLiveData(false)
    val scanning: LiveData<Boolean> = _scanning

    private val _progress = MutableLiveData("")
    val progress: LiveData<String> = _progress

    private val _totalSize = MutableLiveData(0L)
    val totalSize: LiveData<Long> = _totalSize

    private val _selectedPaths = MutableLiveData<Set<String>>(emptySet())
    val selectedPaths: LiveData<Set<String>> = _selectedPaths

    var sortOrder = SortOrder.SIZE_DESC
    var filterType = FilterType.ALL

    private val allItems = mutableListOf<FileItem>()
    private var scanJob: Job? = null

    fun startScan() {
        scanJob?.cancel()
        allItems.clear()
        _items.value = emptyList()
        _scanning.value = true
        _totalSize.value = 0L

        scanJob = viewModelScope.launch {
            DiskScanner.scanAll(
                onProgress = { count, name ->
                    _progress.postValue("Scanning #$count: $name")
                },
                onItemFound = { item ->
                    allItems.add(item)
                    _totalSize.postValue(allItems.sumOf { it.size })
                    _items.postValue(applyFilter(applySort(allItems)))
                }
            )
            _scanning.value = false
            _progress.value = "Selesai — ${allItems.size} item ditemukan"
        }
    }

    fun drillDown(path: String) {
        viewModelScope.launch {
            _scanning.value = true
            val children = DiskScanner.scanDir(path)
            _items.value = applyFilter(applySort(children))
            _scanning.value = false
        }
    }

    fun deleteSelected() {
        val paths = _selectedPaths.value ?: return
        viewModelScope.launch {
            paths.forEach { path ->
                DiskScanner.delete(path)
                allItems.removeAll { it.path == path }
            }
            _selectedPaths.value = emptySet()
            _totalSize.value = allItems.sumOf { it.size }
            _items.value = applyFilter(applySort(allItems))
        }
    }

    fun toggleSelect(path: String) {
        val current = _selectedPaths.value?.toMutableSet() ?: mutableSetOf()
        if (current.contains(path)) current.remove(path) else current.add(path)
        _selectedPaths.value = current
    }

    fun clearSelection() { _selectedPaths.value = emptySet() }

    fun setSortOrder(order: SortOrder) {
        sortOrder = order
        _items.value = applyFilter(applySort(allItems))
    }

    fun setFilter(filter: FilterType) {
        filterType = filter
        _items.value = applyFilter(applySort(allItems))
    }

    private fun applySort(list: List<FileItem>): List<FileItem> = when (sortOrder) {
        SortOrder.SIZE_DESC -> list.sortedByDescending { it.size }
        SortOrder.SIZE_ASC  -> list.sortedBy { it.size }
        SortOrder.NAME_ASC  -> list.sortedBy { it.name.lowercase() }
    }

    private fun applyFilter(list: List<FileItem>): List<FileItem> = when (filterType) {
        FilterType.ALL      -> list
        FilterType.APP_DATA -> list.filter { it.path.contains("/Android/data") }
        FilterType.OBB      -> list.filter { it.path.contains("/Android/obb") }
        FilterType.DOWNLOAD -> list.filter { it.path.contains("/Download") }
        FilterType.DCIM     -> list.filter { it.path.contains("/DCIM") }
        FilterType.OTHER    -> list.filter {
            !it.path.contains("/Android/data") &&
            !it.path.contains("/Android/obb") &&
            !it.path.contains("/Download") &&
            !it.path.contains("/DCIM")
        }
    }
}
