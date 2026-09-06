package com.mohamedaminelouati.calllimiterreminder.UI

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textview.MaterialTextView
import com.mohamedaminelouati.calllimiterreminder.R

enum class FilterMode {
    ALL,
    WHITELIST,
    ALPHABETICAL
}

data class SavedLimitItem(
    val phoneNumber: String,
    val contactName: String,
    val remainingTime: Int,
    val totalLimit: Int,
    val isWhitelisted: Boolean
)

class SavedLimitsAdapter(
    private val onItemClick: (SavedLimitItem) -> Unit,
    private val onEditClick: (SavedLimitItem) -> Unit,
    private val onDeleteClick: (SavedLimitItem) -> Unit
) : RecyclerView.Adapter<SavedLimitsAdapter.ViewHolder>() {

    private val allItems = mutableListOf<SavedLimitItem>()
    val displayedItems = mutableListOf<SavedLimitItem>()

    var currentFilterMode = FilterMode.ALL
        private set
    var currentSearchQuery = ""
        private set

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val cardView: MaterialCardView = itemView.findViewById(R.id.card_saved_limit)
        val nameText: MaterialTextView = itemView.findViewById(R.id.contact_name_text)
        val numberText: MaterialTextView = itemView.findViewById(R.id.contact_number_text)
        val remainingText: MaterialTextView = itemView.findViewById(R.id.remaining_time_text)
        val editButton: MaterialButton = itemView.findViewById(R.id.btn_edit_contact)
        val deleteButton: MaterialButton = itemView.findViewById(R.id.btn_delete_contact)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_saved_limit, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = displayedItems.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = displayedItems[position]

        val displayName = item.contactName.ifEmpty { item.phoneNumber }
        holder.nameText.text = displayName

        val showSubtitle = item.contactName.isNotEmpty() && item.contactName != item.phoneNumber
        holder.numberText.visibility = if (showSubtitle) View.VISIBLE else View.GONE
        holder.numberText.text = item.phoneNumber

        holder.remainingText.text = formatDuration(item.remainingTime)

        holder.cardView.setOnClickListener { onItemClick(item) }
        holder.editButton.setOnClickListener { onEditClick(item) }
        holder.deleteButton.setOnClickListener { onDeleteClick(item) }
    }

    fun submitList(items: List<SavedLimitItem>) {
        allItems.clear()
        allItems.addAll(items)
        applyFilters()
    }

    fun setSearchQuery(query: String) {
        currentSearchQuery = query.trim()
        applyFilters()
    }

    fun setFilterMode(mode: FilterMode) {
        currentFilterMode = mode
        applyFilters()
    }

    fun getItem(position: Int): SavedLimitItem? {
        return if (position in displayedItems.indices) displayedItems[position] else null
    }

    private fun applyFilters() {
        val query = currentSearchQuery.lowercase()
        val filtered = allItems.filter { item ->
            val matchesQuery = query.isEmpty() ||
                item.phoneNumber.lowercase().contains(query) ||
                item.contactName.lowercase().contains(query)

            val matchesFilter = when (currentFilterMode) {
                FilterMode.ALL -> true
                FilterMode.WHITELIST -> item.isWhitelisted
                FilterMode.ALPHABETICAL -> true
            }

            matchesQuery && matchesFilter
        }

        val sorted = if (currentFilterMode == FilterMode.ALPHABETICAL) {
            filtered.sortedBy { (it.contactName.ifEmpty { it.phoneNumber }).lowercase() }
        } else {
            filtered
        }

        displayedItems.clear()
        displayedItems.addAll(sorted)
        notifyDataSetChanged()
    }

    private fun formatDuration(seconds: Int): String {
        val h = seconds / 3600
        val m = (seconds % 3600) / 60
        val s = seconds % 60
        return "$h hrs $m mins $s seconds"
    }
}
