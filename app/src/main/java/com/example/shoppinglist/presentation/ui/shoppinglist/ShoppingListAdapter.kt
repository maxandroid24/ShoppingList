package com.example.shoppinglist.presentation.ui.shoppinglist

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.shoppinglist.R
import com.example.shoppinglist.databinding.ItemShoppingBinding
import com.example.shoppinglist.domain.models.ShoppingItem

class ShoppingListAdapter(
    private val onItemClick: (ShoppingItem) -> Unit
) : ListAdapter<ShoppingItem, ShoppingListAdapter.ViewHolder>(ItemDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemShoppingBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding, onItemClick)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(
        private val binding: ItemShoppingBinding,
        private val onItemClick: (ShoppingItem) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: ShoppingItem) {
            binding.tvName.text = item.name
            binding.tvQuantity.text = "${item.quantity}x"
            binding.tvAddedBy.text = itemView.context.getString(R.string.added_by, item.addedByUserName)

            if (item.isBought) {
                binding.tvName.paintFlags = binding.tvName.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                binding.clMain.alpha = 0.5f
            } else {
                binding.tvName.paintFlags = binding.tvName.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
                binding.clMain.alpha = 1.0f
            }

            binding.root.setOnClickListener { onItemClick(item) }
        }
    }

    class ItemDiffCallback : DiffUtil.ItemCallback<ShoppingItem>() {
        override fun areItemsTheSame(oldItem: ShoppingItem, newItem: ShoppingItem): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: ShoppingItem, newItem: ShoppingItem): Boolean {
            return oldItem == newItem
        }
    }
}
