package com.brruham.diskscanner.ui

import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.brruham.diskscanner.databinding.ActivityMainBinding
import com.brruham.diskscanner.model.FileItem
import com.brruham.diskscanner.shizuku.ShizukuHelper
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import rikka.shizuku.Shizuku

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val vm: ScanViewModel by viewModels()
    private lateinit var adapter: FileAdapter

    private val SHIZUKU_CODE = 1001

    private val shizukuListener = Shizuku.OnRequestPermissionResultListener { _, grantResult ->
        if (grantResult == PackageManager.PERMISSION_GRANTED) {
            toast("✓ Shizuku granted — akses penuh aktif")
            vm.scan()
        } else {
            toast("Shizuku ditolak — mode terbatas")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        setupShizuku()
        observeState()

        binding.swipeRefresh.setOnRefreshListener { vm.scan() }

        binding.chipSdcard.setOnClickListener { vm.navigateTo("/sdcard") }
        binding.chipAndroidData.setOnClickListener { vm.navigateTo("/sdcard/Android/data") }
        binding.chipObb.setOnClickListener { vm.navigateTo("/sdcard/Android/obb") }
        binding.chipDownload.setOnClickListener { vm.navigateTo("/sdcard/Download") }
    }

    private fun setupRecyclerView() {
        adapter = FileAdapter(
            onItemClick = { item ->
                if (item.isDirectory) vm.navigateTo(item.path)
            },
            onDeleteClick = { item -> confirmDelete(item) }
        )
        binding.recyclerView.adapter = adapter
    }

    private fun setupShizuku() {
        Shizuku.addRequestPermissionResultListener(shizukuListener)
        if (ShizukuHelper.isAvailable()) {
            if (!ShizukuHelper.isGranted()) {
                ShizukuHelper.requestPermission(SHIZUKU_CODE)
            } else {
                binding.tvShizukuStatus.text = "🟢 Shizuku aktif — akses penuh"
                binding.tvShizukuStatus.setTextColor(0xFF4CAF50.toInt())
            }
        } else {
            binding.tvShizukuStatus.text = "🔴 Shizuku tidak aktif — akses terbatas"
        }
    }

    private fun observeState() {
        lifecycleScope.launch {
            vm.state.collectLatest { state ->
                binding.swipeRefresh.isRefreshing = state.isLoading
                binding.tvCurrentPath.text = state.currentPath

                if (!state.isLoading) {
                    adapter.submitList(state.items)
                    updateStorageInfo(state.totalSpace, state.freeSpace)
                }

                state.error?.let { toast("Error: $it") }

                // Shizuku status
                if (ShizukuHelper.isGranted()) {
                    binding.tvShizukuStatus.text = "🟢 Shizuku aktif — akses penuh"
                }
            }
        }
    }

    private fun updateStorageInfo(total: Long, free: Long) {
        val used = total - free
        val usedPct = if (total > 0) (used.toFloat() / total * 100).toInt() else 0
        binding.tvStorageInfo.text = "${formatSize(used)} / ${formatSize(total)} dipakai ($usedPct%)"
        binding.storageBar.progress = usedPct
    }

    private fun confirmDelete(item: FileItem) {
        AlertDialog.Builder(this)
            .setTitle("Hapus?")
            .setMessage("${item.name}\n${item.formattedSize()}\n\nTidak bisa di-undo!")
            .setPositiveButton("Hapus") { _, _ ->
                vm.delete(item)
                toast("Menghapus ${item.name}...")
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun formatSize(bytes: Long): String = when {
        bytes >= 1_073_741_824 -> "%.1f GB".format(bytes / 1_073_741_824.0)
        bytes >= 1_048_576     -> "%.1f MB".format(bytes / 1_048_576.0)
        else                   -> "%.1f KB".format(bytes / 1_024.0)
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()

    override fun onDestroy() {
        super.onDestroy()
        Shizuku.removeRequestPermissionResultListener(shizukuListener)
    }
}
