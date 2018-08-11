package com.boardgamegeek.ui.adapter

import android.graphics.Typeface
import android.support.v7.widget.RecyclerView
import android.util.SparseBooleanArray
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.boardgamegeek.R
import com.boardgamegeek.model.SearchResult
import com.boardgamegeek.ui.GameActivity
import com.boardgamegeek.ui.adapter.SearchResultsAdapter.SearchResultViewHolder
import com.boardgamegeek.util.PresentationUtils
import kotlinx.android.synthetic.main.row_search.view.*
import java.util.*
import kotlin.properties.Delegates

interface Callback {
    fun onItemClick(position: Int): Boolean

    fun onItemLongClick(position: Int): Boolean
}

class SearchResultsAdapter(private val callback: Callback?) : RecyclerView.Adapter<SearchResultViewHolder>(), AutoUpdatableAdapter {
    init {
        setHasStableIds(true)
    }

    var results: List<SearchResult> by Delegates.observable(emptyList()) { _, old, new ->
        autoNotify(old, new) { o, n ->
            o.id == n.id
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SearchResultViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.row_search, parent, false)
        return SearchResultViewHolder(view)
    }

    override fun onBindViewHolder(holder: SearchResultViewHolder, position: Int) {
        holder.bind(results.getOrNull(position), position)
    }

    override fun getItemCount() = results.size

    override fun getItemId(position: Int) = position.toLong()

    fun getItem(position: Int) = results.getOrNull(position)

    fun clear() {
        results = emptyList()
    }

    inner class SearchResultViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(game: SearchResult?, position: Int) {
            game?.let {
                itemView.nameView.text = it.name
                val style = when (it.nameType) {
                    SearchResult.NAME_TYPE_ALTERNATE -> Typeface.ITALIC
                    SearchResult.NAME_TYPE_PRIMARY, SearchResult.NAME_TYPE_UNKNOWN -> Typeface.NORMAL
                    else -> Typeface.NORMAL
                }
                itemView.nameView.setTypeface(itemView.nameView.typeface, style)
                itemView.yearView.text = PresentationUtils.describeYear(itemView.context, it.yearPublished)
                itemView.gameIdView.text = itemView.gameIdView.context.getString(R.string.id_list_text, it.id.toString())

                itemView.isActivated = selectedItems.get(position, false)

                itemView.setOnClickListener { _ ->
                    if (callback?.onItemClick(position) != true) {
                        GameActivity.start(itemView.context, it.id, it.name)
                    }
                }

                itemView.setOnLongClickListener { _ ->
                    callback?.onItemLongClick(position) ?: false
                }
            }
        }
    }

    private val selectedItems: SparseBooleanArray = SparseBooleanArray()

    val selectedItemCount: Int
        get() = selectedItems.size()

    fun toggleSelection(position: Int) {
        if (selectedItems.get(position, false)) {
            selectedItems.delete(position)
        } else {
            selectedItems.put(position, true)
        }
        notifyItemChanged(position)
    }

    fun clearSelections() {
        selectedItems.clear()
        notifyDataSetChanged()
    }

    fun getSelectedItems(): List<Int> {
        val items = ArrayList<Int>(selectedItems.size())
        for (i in 0 until selectedItems.size()) {
            items.add(selectedItems.keyAt(i))
        }
        return items
    }
}

