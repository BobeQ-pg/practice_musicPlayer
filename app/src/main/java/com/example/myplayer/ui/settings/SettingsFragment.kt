package com.example.myplayer.ui.settings

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.myplayer.SharedViewModel
import com.example.myplayer.databinding.FragmentSettingsBinding

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    private val sharedViewModel: SharedViewModel by activityViewModels()

    private val openDocumentTreeLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.also { uri ->
                val contentResolver = requireActivity().contentResolver
                val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION
                contentResolver.takePersistableUriPermission(uri, takeFlags)
                sharedViewModel.addMusicFolder(uri)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val folderAdapter = FolderAdapter { uri ->
            showDeleteConfirmationDialog(uri)
        }

        binding.folderRecyclerView.apply {
            adapter = folderAdapter
            layoutManager = LinearLayoutManager(context)
        }

        sharedViewModel.musicFolders.observe(viewLifecycleOwner) { folders ->
            folderAdapter.submitList(folders.toList())
        }

        binding.addFolderButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
            openDocumentTreeLauncher.launch(intent)
        }
    }

    private fun showDeleteConfirmationDialog(uri: Uri) {
        AlertDialog.Builder(requireContext())
            .setTitle("Remove Folder")
            .setMessage("Are you sure you want to remove this folder from the library?\n(The folder itself will not be deleted)")
            .setPositiveButton("Remove") { _, _ ->
                sharedViewModel.removeMusicFolder(uri)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
