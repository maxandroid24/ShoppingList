package com.example.shoppinglist.presentation.ui.shoppinglist

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.shoppinglist.R
import com.example.shoppinglist.databinding.FragmentShoppingListBinding
import com.example.shoppinglist.domain.models.ShoppingItem
import com.example.shoppinglist.domain.utils.Resource
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ShoppingListFragment : Fragment() {

    private var _binding: FragmentShoppingListBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ShoppingListViewModel by viewModels()
    private lateinit var adapter: ShoppingListAdapter

    companion object {
        fun newInstance(groupId: String, userId: String, userName: String) = ShoppingListFragment().apply {
            arguments = Bundle().apply {
                putString("groupId", groupId)
                putString("userId", userId)
                putString("userName", userName)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentShoppingListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Toast.makeText(context, "Shopping List View Created", Toast.LENGTH_SHORT).show()

        val groupId = arguments?.getString("groupId")
        val userId = arguments?.getString("userId")
        val userName = arguments?.getString("userName")
        
        android.util.Log.d("ShoppingListFragment", "onViewCreated: groupId=$groupId, userId=$userId, userName=$userName")

        if (groupId == null || userId == null || userName == null) {
            android.util.Log.e("ShoppingListFragment", "Missing arguments, returning")
            return
        }

        viewModel.init(groupId, userId, userName)

        setupRecyclerView()
        setupListeners()
        observeState()
    }

    private fun setupRecyclerView() {
        adapter = ShoppingListAdapter { item ->
            showItemDialog(item)
        }
        binding.recyclerView.layoutManager = LinearLayoutManager(context)
        binding.recyclerView.adapter = adapter

        val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val item = adapter.currentList[position]
                if (direction == ItemTouchHelper.LEFT) {
                    viewModel.deleteItem(item)
                } else if (direction == ItemTouchHelper.RIGHT) {
                    viewModel.toggleBoughtStatus(item)
                }
            }

            override fun onChildDraw(c: Canvas, recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, dX: Float, dY: Float, actionState: Int, isCurrentlyActive: Boolean) {
                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
                val itemView = viewHolder.itemView
                val background = ColorDrawable()
                if (dX > 0) { // Swipe Right
                    background.color = Color.parseColor("#4CAF50") // Green
                    background.setBounds(itemView.left, itemView.top, itemView.left + dX.toInt(), itemView.bottom)
                } else if (dX < 0) { // Swipe Left
                    background.color = Color.parseColor("#F44336") // Red
                    background.setBounds(itemView.right + dX.toInt(), itemView.top, itemView.right, itemView.bottom)
                } else {
                    background.setBounds(0, 0, 0, 0)
                }
                background.draw(c)
            }
        })
        itemTouchHelper.attachToRecyclerView(binding.recyclerView)
    }

    private fun setupListeners() {
        binding.fabAdd.setOnClickListener {
            showItemDialog()
        }

        binding.btnLeaveGroup.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Leave Group")
                .setMessage("Are you sure you want to leave this group?")
                .setPositiveButton("Leave") { _, _ ->
                    viewModel.leaveGroup()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        binding.btnShare.setOnClickListener {
            val group = (viewModel.group.value as? Resource.Success)?.data
            if (group != null) {
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, "Join my shopping list group! Code: ${group.inviteCode}")
                }
                startActivity(Intent.createChooser(shareIntent, "Share Invite Code"))
            }
        }
    }

    private fun showItemDialog(item: ShoppingItem? = null) {
        val isEdit = item != null
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_add_item, null)
        
        val tvDialogTitle = dialogView.findViewById<TextView>(R.id.tvDialogTitle)
        val etItemName = dialogView.findViewById<EditText>(R.id.etItemName)
        val etQuantity = dialogView.findViewById<EditText>(R.id.etQuantity)
        val cbBought = dialogView.findViewById<CheckBox>(R.id.cbBought)
        val btnDelete = dialogView.findViewById<Button>(R.id.btnDelete)
        val btnSave = dialogView.findViewById<Button>(R.id.btnSave)
        val btnCancel = dialogView.findViewById<Button>(R.id.btnCancel)

        if (isEdit) {
            tvDialogTitle.text = getString(R.string.edit_item)
            etItemName.setText(item!!.name)
            etQuantity.setText(item.quantity.toString())
            cbBought.visibility = View.VISIBLE
            cbBought.isChecked = item.isBought
            btnDelete.visibility = View.VISIBLE
        }

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()

        dialog.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))
        dialog.window?.setDimAmount(0.6f)
        
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            dialog.window?.addFlags(android.view.WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
            dialog.window?.attributes?.blurBehindRadius = 40
        }

        btnCancel.setOnClickListener { dialog.dismiss() }
        
        btnDelete.setOnClickListener {
            item?.let { viewModel.deleteItem(it) }
            dialog.dismiss()
        }

        btnSave.setOnClickListener {
            val name = etItemName.text.toString().trim()
            val qtyStr = etQuantity.text.toString().trim()
            val quantity = if (qtyStr.isNotEmpty()) qtyStr.toInt() else 1

            if (name.isNotEmpty()) {
                if (isEdit) {
                    val updatedItem = item!!.copy(
                        name = name,
                        quantity = quantity,
                        isBought = cbBought.isChecked
                    )
                    viewModel.updateItem(updatedItem)
                    dialog.dismiss()
                } else {
                    lifecycleScope.launch {
                        val duplicate = viewModel.checkDuplicate(name)
                        if (duplicate != null) {
                            AlertDialog.Builder(requireContext())
                                .setTitle(R.string.duplicate_item_title)
                                .setMessage(R.string.duplicate_item_msg)
                                .setPositiveButton(R.string.increase_quantity) { _, _ ->
                                    viewModel.increaseQuantity(duplicate, quantity)
                                    dialog.dismiss()
                                }
                                .setNegativeButton(R.string.cancel, null)
                                .show()
                        } else {
                            viewModel.addItem(name, quantity)
                            dialog.dismiss()
                        }
                    }
                }
            }
        }
        dialog.show()
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.items.collect { state ->
                        when (state) {
                            is Resource.Loading -> {
                                binding.loadingLayout.loadingOverlay.visibility = View.VISIBLE
                                binding.tvEmpty.visibility = View.GONE
                            }
                            is Resource.Success -> {
                                binding.loadingLayout.loadingOverlay.visibility = View.GONE
                                adapter.submitList(state.data)
                                binding.tvEmpty.visibility = if (state.data.isEmpty()) View.VISIBLE else View.GONE
                            }
                            is Resource.Error -> {
                                binding.loadingLayout.loadingOverlay.visibility = View.GONE
                                Toast.makeText(context, state.message, Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }

                launch {
                    viewModel.group.collect { state ->
                        if (state is Resource.Success) {
                            (activity as? AppCompatActivity)?.supportActionBar?.title = state.data.name
                            binding.tvGroupName.text = state.data.name
                            binding.tvInviteCode.text = "Invite Code: ${state.data.inviteCode}"
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
