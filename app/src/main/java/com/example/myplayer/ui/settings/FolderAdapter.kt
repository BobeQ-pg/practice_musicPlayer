package com.example.myplayer.ui.settings

import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.myplayer.databinding.FolderItemBinding

class FolderAdapter(private val onDeleteClicked: (Uri) -> Unit) :
    ListAdapter<String, FolderAdapter.FolderViewHolder>(FolderDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FolderViewHolder {
        val binding = FolderItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return FolderViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FolderViewHolder, position: Int) {
        val folderUriString = getItem(position)
        holder.bind(folderUriString, onDeleteClicked)
    }

    class FolderViewHolder(private val binding: FolderItemBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(folderUriString: String, onDeleteClicked: (Uri) -> Unit) {
            val uri = Uri.parse(folderUriString)
            binding.folderPathTextView.text = uri.path // Or a more readable name
            binding.deleteFolderButton.setOnClickListener { onDeleteClicked(uri) }
        }
    }
}

class FolderDiffCallback : DiffUtil.ItemCallback<String>() {
    override fun areItemsTheSame(oldItem: String, newItem: String): Boolean = oldItem == newItem
    override fun areContentsTheSame(oldItem: String, newItem: String): Boolean = oldItem == newItem
}
