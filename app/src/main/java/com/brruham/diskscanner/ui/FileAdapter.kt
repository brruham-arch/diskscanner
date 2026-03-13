package com.brruham.diskscanner.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.brruham.diskscanner.databinding.ItemFileBinding
import com.brruham.diskscanner.model.FileItem

class FileAdapter(
    private val onItemClick: (FileItem) -> Unit,
    private val onDeleteClick: (FileItem) -> Unit
) : ListAdapter<FileItem, FileAdapter.VH>(DIFF) {

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<FileItem>() {
            override fun areItemsTheSame(a: FileItem, b: FileItem) = a.path == b.path
            override fun areContentsTheSame(a: FileItem, b: FileItem) = a == b
        }
    }

    // Max size untuk bar visual
    private var maxSize = 1L

    override fun submitList(list: List<FileItem>?) {
        maxSize = list?.maxOfOrNull { it.size } ?: 1L
        super.submitList(list)
    }

    inner class VH(val binding: ItemFileBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemFileBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = getItem(position)
        with(holder.binding) {
            tvName.text = item.name
            tvSize.text = item.formattedSize()
            tvPath.text = item.path

            // Icon folder vs file
            ivIcon.text = if (item.isDirectory) "📁" else "📄"

            // Progress bar visual ukuran relatif
            val progress = if (maxSize > 0) ((item.size.toFloat() / maxSize) * 100).toInt() else 0
            progressBar.progress = progress

            // Warna bar berdasarkan ukuran
            val color = when {
                progress >= 70 -> 0xFFE53935.toInt() // merah
                progress >= 40 -> 0xFFFF9800.toInt() // orange
                else           -> 0xFF4CAF50.toInt() // hijau
            }
            progressBar.progressTintList = android.content.res.ColorStateList.valueOf(color)

            root.setOnClickListener { onItemClick(item) }
            btnDelete.setOnClickListener { onDeleteClick(item) }
        }
    }
}
