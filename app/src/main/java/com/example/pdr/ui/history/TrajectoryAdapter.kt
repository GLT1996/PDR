package com.example.pdr.ui.history

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.pdr.R
import com.example.pdr.data.model.Trajectory

/**
 * 轨迹列表适配器
 * 支持删除和批量选择
 */
class TrajectoryAdapter(
    private val onItemClick: (Trajectory) -> Unit,
    private val onDeleteClick: (Trajectory) -> Unit,
    private val onSelectionModeChanged: (Boolean) -> Unit,
    private val onSelectionCountChanged: ((Int) -> Unit)? = null
) : RecyclerView.Adapter<TrajectoryAdapter.TrajectoryViewHolder>() {

    private val items = mutableListOf<Trajectory>()
    private val selectedItems = mutableSetOf<Long>()
    private var isSelectionMode = false

    inner class TrajectoryViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textName: TextView = view.findViewById(R.id.text_trajectory_name)
        val textInfo: TextView = view.findViewById(R.id.text_trajectory_info)
        val textDate: TextView = view.findViewById(R.id.text_trajectory_date)
        val checkbox: CheckBox = view.findViewById(R.id.checkbox_select)
        val btnDelete: ImageButton = view.findViewById(R.id.btn_delete)

        fun bind(trajectory: Trajectory) {
            textName.text = trajectory.name
            textInfo.text = "步数: ${trajectory.totalSteps} | 距离: ${String.format("%.1f", trajectory.totalDistance)}m"
            textDate.text = formatDateTime(trajectory.startTime)

            // 选择模式
            if (isSelectionMode) {
                checkbox.visibility = View.VISIBLE
                btnDelete.visibility = View.GONE
                checkbox.isChecked = selectedItems.contains(trajectory.id)
            } else {
                checkbox.visibility = View.GONE
                btnDelete.visibility = View.VISIBLE
            }

            itemView.setOnClickListener {
                if (isSelectionMode) {
                    toggleSelection(trajectory)
                } else {
                    onItemClick(trajectory)
                }
            }

            itemView.setOnLongClickListener {
                if (!isSelectionMode) {
                    enterSelectionMode()
                    toggleSelection(trajectory)
                }
                true
            }

            checkbox.setOnClickListener {
                toggleSelection(trajectory)
            }

            btnDelete.setOnClickListener {
                onDeleteClick(trajectory)
            }
        }

        private fun formatDateTime(timestamp: Long): String {
            if (timestamp <= 0) {
                return "未知时间"
            }
            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault())
            return sdf.format(java.util.Date(timestamp))
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TrajectoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_trajectory, parent, false)
        return TrajectoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: TrajectoryViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    fun submitList(newItems: List<Trajectory>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    private fun toggleSelection(trajectory: Trajectory) {
        if (selectedItems.contains(trajectory.id)) {
            selectedItems.remove(trajectory.id)
        } else {
            selectedItems.add(trajectory.id)
        }
        notifyItemChanged(items.indexOf(trajectory))
        onSelectionCountChanged?.invoke(selectedItems.size)

        // 如果取消所有选择，退出选择模式
        if (selectedItems.isEmpty()) {
            exitSelectionMode()
        }
    }

    fun enterSelectionMode() {
        isSelectionMode = true
        onSelectionModeChanged(true)
        notifyDataSetChanged()
    }

    fun exitSelectionMode() {
        isSelectionMode = false
        selectedItems.clear()
        onSelectionModeChanged(false)
        notifyDataSetChanged()
    }

    fun selectAll() {
        selectedItems.clear()
        selectedItems.addAll(items.map { it.id })
        notifyDataSetChanged()
        onSelectionCountChanged?.invoke(selectedItems.size)
    }

    fun getSelectedIds(): List<Long> = selectedItems.toList()

    fun isInSelectionMode(): Boolean = isSelectionMode
}