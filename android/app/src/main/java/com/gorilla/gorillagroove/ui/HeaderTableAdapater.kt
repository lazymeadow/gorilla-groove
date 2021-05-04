package com.gorilla.gorillagroove.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.gorilla.gorillagroove.R
import kotlinx.android.synthetic.main.simple_text_info_item.view.*

private const val TYPE_HEADER = 0
private const val TYPE_ITEM = 1

/**
 * Android's RecyclerView has no easy way to have a table with headers without doing all the BS yourself. I needed to have a table with headers in two places,
 * so I did the BS myself and made a reusable adapter for it. It's not very sophisticated, but works for tables with simple headers
 */
abstract class HeaderTableAdapter<T: RecyclerView.ViewHolder> : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == TYPE_ITEM) {
            onCreateBodyViewHolder(parent)
        } else {
            val itemView = inflater.inflate(R.layout.simple_text_header_item, parent, false)
            HeaderViewHolder(itemView)
        }
    }

    override fun getItemCount(): Int {
        val sections = getSectionCount()

        // Only sections that contain items should be counted towards the total, as we don't show headers with no content
        var sectionsContainingItems = 0

        val bodyItems = (0 until sections).fold(0) { acc, sectionIndex ->
            val count = getCountForSection(sectionIndex)
            if (count > 0) {
                sectionsContainingItems++
            }
            acc + getCountForSection(sectionIndex)
        }

        return bodyItems + sectionsContainingItems
    }

    override fun getItemViewType(position: Int): Int {
        val headerPositions = getHeaderPositions()

        return when {
            headerPositions.contains(position) -> TYPE_HEADER
            else -> TYPE_ITEM
        }
    }

    @Suppress("UNCHECKED_CAST")
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val (section, positionInSection) = getSectionAndPositionInSection(position)

        if (holder is HeaderViewHolder) {
            holder.itemView.textItem.text = getTitleForHeader(section)
        } else {
            onBindBodyViewHolder(holder as T, section, positionInSection)
        }
    }

    private fun getHeaderPositions(): List<Int> {
        var currentCount = 0

        val positions = mutableListOf<Int>()

        (0 until getSectionCount()).forEach { section ->
            val position = currentCount
            val count = getCountForSection(section)
            currentCount += count

            // If a header has no content, then we don't want to show it / deal with it
            if (count > 0) {
                positions.add(position)
                currentCount++
            }
        }

        return positions
    }

    private fun getSectionAndPositionInSection(position: Int): Pair<Int, Int> {
        if (position == 0) {
            return 0 to 0
        }

        val headerPositions = getHeaderPositions()
        val sectionIndex = (headerPositions.indexOfFirst { it > position }.takeIf { it > -1 } ?: headerPositions.size) - 1

        val positionInSection = (position - headerPositions[sectionIndex] - 1)

        return sectionIndex to positionInSection
    }

    abstract fun onCreateBodyViewHolder(parent: ViewGroup): T

    abstract fun getSectionCount(): Int

    abstract fun getCountForSection(sectionIndex: Int): Int

    abstract fun onBindBodyViewHolder(holder: T, sectionIndex: Int, positionInSection: Int)

    abstract fun getTitleForHeader(sectionIndex: Int): String
}

private class HeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
