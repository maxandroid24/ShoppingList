package com.example.shoppinglist.presentation.ui.group

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.shoppinglist.databinding.ItemGroupBinding
import com.example.shoppinglist.domain.models.ShoppingGroup

class GroupListAdapter(
    private val onGroupClick: (ShoppingGroup) -> Unit,
    private val onLeaveClick: (ShoppingGroup) -> Unit
) : ListAdapter<ShoppingGroup, GroupListAdapter.ViewHolder>(GroupDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemGroupBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemGroupBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(group: ShoppingGroup) {
            binding.tvGroupName.text = group.name
            binding.tvMemberCount.text = "${group.memberIds.size} members"
            binding.root.setOnClickListener { 
                android.util.Log.d("GroupListAdapter", "Group card clicked: ${group.name}")
                onGroupClick(group) 
            }
            binding.btnLeave.setOnClickListener { onLeaveClick(group) }
        }
    }

    class GroupDiffCallback : DiffUtil.ItemCallback<ShoppingGroup>() {
        override fun areItemsTheSame(oldItem: ShoppingGroup, newItem: ShoppingGroup): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: ShoppingGroup, newItem: ShoppingGroup): Boolean = oldItem == newItem
    }
}
