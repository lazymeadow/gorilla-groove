package com.gorilla.gorillagroove.ui.multiselectlist

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.annotation.MainThread
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.gorilla.gorillagroove.R
import com.gorilla.gorillagroove.database.entity.DbEntity
import com.gorilla.gorillagroove.database.entity.DbTrack
import com.gorilla.gorillagroove.ui.MainActivity
import com.gorilla.gorillagroove.ui.createDivider
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.multiselect_list.view.*
import kotlinx.android.synthetic.main.multiselect_list_item.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

abstract class MultiselectList<T: DbEntity>(context: Context, attrs: AttributeSet? = null) : ConstraintLayout(context, attrs) {
    val layout = LayoutInflater.from(context).inflate(R.layout.multiselect_list, this, true) as ConstraintLayout

    var data = listOf<T>()

    private lateinit var multiselectAdapter: MultiselectListAdapter

    val checkedDataIds = mutableSetOf<Long>()

    private lateinit var mainActivity: MainActivity
    protected lateinit var tracksToAdd: List<Long>

    abstract fun loadData(): List<T>

    fun reload() {
        GlobalScope.launch(Dispatchers.IO) {
            data = loadData()

            // We may have hidden a checked row. Remove its checkmark from the list if we do
            val visibleIds = data.map { it.id }.toSet()
            val removedCheckedEntities = checkedDataIds - visibleIds
            checkedDataIds.removeAll(removedCheckedEntities)

            withContext(Dispatchers.Main) {
                multiselectAdapter.notifyDataSetChanged()
            }
        }
    }

    open suspend fun initialize(activity: MainActivity, tracks: List<DbTrack>) = withContext(Dispatchers.Main) {
        withContext(Dispatchers.IO) {
            data = loadData()
        }

        if (!::multiselectAdapter.isInitialized) {
            setupRecyclerView()
        }

        multiselectAdapter.notifyDataSetChanged()

        mainActivity = activity
        tracksToAdd = tracks.map { it.id }

        activity.toolbar.isVisible = false
        rightLoadingIndicator.isVisible = false

        addButton.isVisible = true

        layout.visibility = VISIBLE

        EventBus.getDefault().register(this@MultiselectList)
    }

    private fun setupRecyclerView() = multiselectList.apply {
        multiselectAdapter = MultiselectListAdapter()
        addItemDecoration(createDivider(context))
        adapter = multiselectAdapter
        layoutManager = LinearLayoutManager(context)
    }

    @MainThread
    fun close() {
        data = emptyList()
        checkedDataIds.clear()

        mainActivity.toolbar.isVisible = true
        layout.visibility = View.GONE

        EventBus.getDefault().unregister(this)
    }

    @Subscribe(threadMode = ThreadMode.ASYNC)
    fun onTouchDownEvent(event: MotionEvent) = popoutMenu.handleScreenTap(event, this, mainActivity)

    inner class MultiselectListAdapter : RecyclerView.Adapter<MultiselectItemViewHolder<T>>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MultiselectItemViewHolder<T> {
            val itemView = LayoutInflater.from(parent.context).inflate(R.layout.multiselect_list_item, parent, false)
            return createMyViewHolder(itemView, checkedDataIds)
        }

        override fun getItemCount() = data.size

        override fun onBindViewHolder(holder: MultiselectItemViewHolder<T>, position: Int) {
            val data = data[position]
            holder.setData(data)
        }
    }

    abstract class MultiselectItemViewHolder<T: DbEntity>(itemView: View, private val checkedIds: MutableSet<Long>) : RecyclerView.ViewHolder(itemView) {
        private var _data: T? = null
        private val data get() = _data!!

        init {
            itemView.setOnClickListener {
                if (checkedIds.contains(data.id)) {
                    checkedIds.remove(data.id)
                } else {
                    checkedIds.add(data.id)
                }

                itemView.itemCheckmark.visibility = if (checkedIds.contains(data.id)) View.VISIBLE else View.INVISIBLE
            }
        }

        fun setData(data: T) {
            this._data = data

            itemView.itemTitle.text = getRowName(data)
            itemView.itemCheckmark.visibility = if (checkedIds.contains(data.id)) View.VISIBLE else View.INVISIBLE
        }

        abstract fun getRowName(datum: T): String
    }

    abstract fun createMyViewHolder(itemView: View, checkedIds: MutableSet<Long>): MultiselectItemViewHolder<T>
}
