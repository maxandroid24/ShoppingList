package com.example.shoppinglist.presentation.ui.group

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.shoppinglist.databinding.FragmentGroupListBinding
import com.example.shoppinglist.domain.utils.Resource
import com.example.shoppinglist.presentation.ui.auth.AuthViewModel
import com.example.shoppinglist.presentation.ui.main.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class GroupListFragment : Fragment() {

    private var _binding: FragmentGroupListBinding? = null
    private val binding get() = _binding!!

    private val viewModel: GroupViewModel by viewModels()
    private val authViewModel: AuthViewModel by viewModels()
    private lateinit var adapter: GroupListAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentGroupListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (activity as? MainActivity)?.supportActionBar?.title = "My Groups"
        setupRecyclerView()
        setupListeners()
        observeState()
    }

    private fun setupRecyclerView() {
        adapter = GroupListAdapter(
            onGroupClick = { group ->
                Log.d("GroupListFragment", "Group clicked: ${group.name} (${group.id})")
                (activity as? MainActivity)?.showShoppingListFragment(group.id)
            },
            onLeaveClick = { group ->
                showLeaveDialog(group)
            }
        )
        binding.rvGroups.layoutManager = LinearLayoutManager(context)
        binding.rvGroups.adapter = adapter
    }

    private fun setupListeners() {
        binding.btnCreateNew.setOnClickListener { showCreateGroupDialog() }
        binding.btnJoinByCode.setOnClickListener { showJoinByCodeDialog() }
    }

    private fun showCreateGroupDialog() {
        val context = requireContext()
        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 40, 50, 10)
        }
        
        val etGroupName = EditText(context).apply { hint = "Group Name" }
        val etYourName = EditText(context).apply { hint = "Your Display Name" }
        
        layout.addView(etGroupName)
        layout.addView(etYourName)

        val dialog = AlertDialog.Builder(context)
            .setTitle("Create Group")
            .setView(layout)
            .setPositiveButton("Create") { _, _ ->
                val gName = etGroupName.text.toString().trim()
                val uName = etYourName.text.toString().trim()
                if (gName.isNotEmpty() && uName.isNotEmpty()) {
                    val user = authViewModel.currentUser.value
                    if (user != null) viewModel.createGroup(gName, user.id, uName)
                } else {
                    Toast.makeText(context, "Fill all fields", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .create()
            
        dialog.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))
        dialog.window?.setDimAmount(0.6f)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            dialog.window?.addFlags(android.view.WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
            dialog.window?.attributes?.blurBehindRadius = 40
        }
        dialog.show()
    }

    private fun showJoinByCodeDialog() {
        val context = requireContext()
        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 40, 50, 10)
        }
        
        val etInviteCode = EditText(context).apply { 
            hint = "Invite Code" 
            setSingleLine(true)
        }
        val etYourName = EditText(context).apply { hint = "Your Display Name" }
        
        layout.addView(etInviteCode)
        layout.addView(etYourName)

        val dialog = AlertDialog.Builder(context)
            .setTitle("Join Group")
            .setView(layout)
            .setPositiveButton("Join") { _, _ ->
                val code = etInviteCode.text.toString().trim()
                val uName = etYourName.text.toString().trim()
                if (code.isNotEmpty() && uName.isNotEmpty()) {
                    val user = authViewModel.currentUser.value
                    if (user != null) {
                        Log.d("GroupListFragment", "Joining with code: $code for user: ${user.id}")
                        viewModel.joinGroup(code, user.id, uName)
                    }
                } else {
                    Toast.makeText(context, "Fill all fields", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .create()
            
        dialog.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))
        dialog.window?.setDimAmount(0.6f)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            dialog.window?.addFlags(android.view.WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
            dialog.window?.attributes?.blurBehindRadius = 40
        }
        dialog.show()
    }

    private fun showLeaveDialog(group: com.example.shoppinglist.domain.models.ShoppingGroup) {
        AlertDialog.Builder(requireContext())
            .setTitle("Leave Group")
            .setMessage("Are you sure you want to leave ${group.name}?")
            .setPositiveButton("Leave") { _, _ ->
                viewModel.leaveGroup(group.id)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    authViewModel.currentUser.collect { user ->
                        Log.d("GroupListFragment", "Current user collected: ${user?.id}, Groups: ${user?.groupIds}")
                        if (user != null) {
                            viewModel.loadUserGroups(user.groupIds)
                        }
                    }
                }
                
                launch {
                    viewModel.userGroups.collect { state ->
                        Log.d("GroupListFragment", "User groups state: $state")
                        when (state) {
                            is Resource.Loading -> {
                                binding.loadingLayout.loadingOverlay.visibility = View.VISIBLE
                                binding.tvEmpty.visibility = View.GONE
                            }
                            is Resource.Success -> {
                                binding.loadingLayout.loadingOverlay.visibility = View.GONE
                                Log.d("GroupListFragment", "Submitting ${state.data.size} groups to adapter")
                                adapter.submitList(state.data)
                                binding.tvEmpty.visibility = if (state.data.isEmpty()) View.VISIBLE else View.GONE
                            }
                            is Resource.Error -> {
                                binding.loadingLayout.loadingOverlay.visibility = View.GONE
                                Toast.makeText(context, "Failed to load groups: ${state.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }

                launch {
                    viewModel.groupState.collect { state ->
                        when (state) {
                            is Resource.Loading -> {
                                binding.loadingLayout.loadingOverlay.visibility = View.VISIBLE
                            }
                            is Resource.Success -> {
                                binding.loadingLayout.loadingOverlay.visibility = View.GONE
                                Toast.makeText(context, "Group Created/Joined: ${state.data.name}", Toast.LENGTH_SHORT).show()
                            }
                            is Resource.Error -> {
                                binding.loadingLayout.loadingOverlay.visibility = View.GONE
                                Toast.makeText(context, "Error: ${state.message}", Toast.LENGTH_LONG).show()
                            }
                            null -> {}
                        }
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
