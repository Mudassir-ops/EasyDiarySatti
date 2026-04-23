package com.example.easydiarysatti.utills

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.easydiarysatti.data.local.CustomTagEntity
import com.example.easydiarysatti.databinding.ItemTagChipBinding

/**
 * TagsAdapter — 2-column grid, driven by [isManageMode].
 *
 * ┌─────────────────────────────────────────────────────────────────┐
 * │  isManageMode = false   SELECT mode  (Create Note → Add Tags)  │
 * │  • chip tap             → [onSelectTag]  selects / deselects   │
 * │  • X badge (top-right)  → visible only on ALREADY-SELECTED     │
 * │                           chips; [onDeleteTag] removes globally │
 * │  • selected state       → tag_chip_selector fills teal;        │
 * │                           tag_chip_text_selector goes white     │
 * ├─────────────────────────────────────────────────────────────────┤
 * │  isManageMode = true    MANAGE mode  (Drawer → Edit Tags)      │
 * │  • chip tap             → [onEditTag]  opens rename dialog      │
 * │  • X badge              → ALWAYS visible on every chip          │
 * │  • no selected state    → chips are never highlighted           │
 * └─────────────────────────────────────────────────────────────────┘
 *
 * Layout: item_tag_chip.xml
 *   • tvTagChip      — pill text view; background = tag_chip_selector
 *                      (reacts to view.isSelected for teal fill)
 *   • ivDeleteBadge  — 18dp × badge in top-right corner of FrameLayout
 */
class TagsAdapter(
    private val isManageMode: Boolean = false,
    private val onSelectTag:  (String) -> Unit,
    private val onEditTag:    (CustomTagEntity) -> Unit,
    private val onDeleteTag:  (String) -> Unit
) : ListAdapter<CustomTagEntity, TagsAdapter.TagViewHolder>(TagDiffCallback()) {

    // Full unfiltered list — submitList only contains the visible subset
    private var fullList:    List<CustomTagEntity> = emptyList()
    private var addedTags:   Set<String>           = emptySet()
    private var activeQuery: String                = ""

    // ── Public API ─────────────────────────────────────────────────────────────

    /**
     * Replace both the full backing list and the selected-tags set,
     * then re-apply any active search query so the visible list stays correct.
     */
    fun submitTagList(tags: List<CustomTagEntity>, selectedTags: Set<String>) {
        fullList  = tags
        addedTags = selectedTags
        applyFilter(activeQuery)
    }

    fun updateAddedTags(selected: Set<String>) {
        addedTags = selected
        // Re-apply filter instead of notifyItemRangeChanged so the list
        // is rebuilt with the new selection state in one synchronous pass.
        applyFilter(activeQuery)
    }

    fun filter(query: String) {
        activeQuery = query
        applyFilter(query)
    }

    private fun applyFilter(query: String) {
        val filtered = if (query.isBlank()) fullList
        else fullList.filter {
            it.tagName.orEmpty().contains(query.trim(), ignoreCase = true)
        }
        // Pass a new ArrayList copy every time so DiffCallback always sees
        // a different list instance and dispatches full rebind.
        submitList(null)                        // force clear first
        submitList(ArrayList(filtered))         // then submit fresh copy
    }

    // ── ViewHolder ─────────────────────────────────────────────────────────────

    inner class TagViewHolder(
        private val binding: ItemTagChipBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(tag: CustomTagEntity) {
            val tagName    = tag.tagName.orEmpty()
            val isSelected = !isManageMode && addedTags.contains(tagName)

            // ── Chip label ────────────────────────────────────────────────────
            binding.tvTagChip.text = tagName

            // ── Selected state drives tag_chip_selector + tag_chip_text_selector
            //    true  → teal fill + white text  (defined in the XML drawables)
            //    false → grey border + dark text
            binding.tvTagChip.isSelected = isSelected

            // ── Delete X badge visibility ─────────────────────────────────────
            //
            //  MANAGE MODE → always visible so the user can delete any tag
            //  SELECT MODE → visible when the chip IS selected (the × removes it
            //                from the note AND globally).
            //                Hidden when unselected — the user first selects, then
            //                can delete via the badge if they want to remove it
            //                from the database entirely.
            binding.ivDeleteBadge.visibility = when {
                isManageMode -> View.VISIBLE          // always in manage mode
                isSelected   -> View.VISIBLE          // show on selected chips
                else         -> View.GONE             // hidden on unselected chips
            }

            // ── Click: chip pill ──────────────────────────────────────────────
            binding.tvTagChip.setOnClickListener {
                if (isManageMode) {
                    // Manage: tap → open rename dialog
                    onEditTag(tag)
                } else {
                    // Select: tap → toggle selection
                    onSelectTag(tagName)
                }
            }

            // ── Click: × badge ────────────────────────────────────────────────
            binding.ivDeleteBadge.setOnClickListener {
                onDeleteTag(tagName)
            }
        }
    }

    // ── Adapter overrides ──────────────────────────────────────────────────────

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TagViewHolder {
        val binding = ItemTagChipBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return TagViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TagViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    /**
     * Called by RecyclerView after inflation so we can attach a
     * GridLayoutManager with span = 2.  If the RecyclerView already has a
     * layout manager set via XML (app:layoutManager + app:spanCount), this
     * override is not needed — keep it only if you set the LM in code.
     */
    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        if (recyclerView.layoutManager == null) {
            recyclerView.layoutManager =
                GridLayoutManager(recyclerView.context, 2)
        }
    }

    // ── DiffCallback ───────────────────────────────────────────────────────────

    private class TagDiffCallback : DiffUtil.ItemCallback<CustomTagEntity>() {
        override fun areItemsTheSame(old: CustomTagEntity, new: CustomTagEntity) =
            old.tagName == new.tagName

        // Always return false — isSelected state lives outside the entity in
        // addedTags, so two "equal" entities can have different visual states.
        // Returning false forces onBindViewHolder to run for every item so
        // teal/grey highlight always reflects the current selection correctly.
        override fun areContentsTheSame(old: CustomTagEntity, new: CustomTagEntity) = false
    }
}