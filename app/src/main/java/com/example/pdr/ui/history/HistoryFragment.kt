package com.example.pdr.ui.history

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.pdr.R
import com.example.pdr.data.model.Trajectory

class HistoryFragment : Fragment() {

    private lateinit var rootView: View
    private lateinit var recyclerView: RecyclerView
    private lateinit var textEmpty: TextView
    private lateinit var normalHeader: LinearLayout
    private lateinit var selectionHeader: LinearLayout
    private lateinit var textSelectionCount: TextView
    private lateinit var btnClearAll: Button
    private lateinit var btnCancelSelection: Button
    private lateinit var btnSelectAll: Button
    private lateinit var btnDeleteSelected: Button

    private val viewModel: HistoryViewModel by viewModels()
    private lateinit var adapter: TrajectoryAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        rootView = inflater.inflate(R.layout.fragment_history, container, false)
        return rootView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupWindowInsets()
        initViews(view)
        setupRecyclerView()
        setupButtons()
        observeViewModel()
    }

    private fun setupWindowInsets() {
        // 处理刘海屏和状态栏
        ViewCompat.setOnApplyWindowInsetsListener(rootView) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun initViews(view: View) {
        recyclerView = view.findViewById(R.id.recyclerView)
        textEmpty = view.findViewById(R.id.text_empty)
        normalHeader = view.findViewById(R.id.normalHeader)
        selectionHeader = view.findViewById(R.id.selectionHeader)
        textSelectionCount = view.findViewById(R.id.textSelectionCount)
        btnClearAll = view.findViewById(R.id.btnClearAll)
        btnCancelSelection = view.findViewById(R.id.btnCancelSelection)
        btnSelectAll = view.findViewById(R.id.btnSelectAll)
        btnDeleteSelected = view.findViewById(R.id.btnDeleteSelected)
    }

    private fun setupRecyclerView() {
        adapter = TrajectoryAdapter(
            onItemClick = { trajectory -> onTrajectoryClick(trajectory) },
            onDeleteClick = { trajectory -> showDeleteConfirmDialog(trajectory) },
            onSelectionModeChanged = { isInSelectionMode -> updateSelectionModeUI(isInSelectionMode) }
        )

        recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@HistoryFragment.adapter
        }
    }

    private fun setupButtons() {
        btnClearAll.setOnClickListener {
            showClearAllConfirmDialog()
        }

        btnCancelSelection.setOnClickListener {
            adapter.exitSelectionMode()
        }

        btnSelectAll.setOnClickListener {
            adapter.selectAll()
            updateSelectionCount()
        }

        btnDeleteSelected.setOnClickListener {
            val selectedIds = adapter.getSelectedIds()
            if (selectedIds.isNotEmpty()) {
                showDeleteConfirmDialog(selectedIds)
            }
        }
    }

    private fun observeViewModel() {
        viewModel.trajectories.observe(viewLifecycleOwner) { list ->
            adapter.submitList(list)
            textEmpty.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
        }

        viewModel.statusMessage.observe(viewLifecycleOwner) { message ->
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
        }
    }

    private fun onTrajectoryClick(trajectory: Trajectory) {
        if (adapter.isInSelectionMode()) {
            return
        }
        // 跳转到主页面并加载轨迹
        val bundle = Bundle().apply {
            putLong("trajectoryId", trajectory.id)
        }
        findNavController().navigate(R.id.action_history_to_main, bundle)
    }

    private fun showDeleteConfirmDialog(trajectory: Trajectory) {
        AlertDialog.Builder(requireContext())
            .setTitle("删除轨迹")
            .setMessage("确定要删除这条轨迹吗？")
            .setPositiveButton("删除") { _, _ ->
                viewModel.deleteTrajectory(trajectory.id)
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun showDeleteConfirmDialog(ids: List<Long>) {
        AlertDialog.Builder(requireContext())
            .setTitle("删除轨迹")
            .setMessage("确定要删除选中的 ${ids.size} 条轨迹吗？")
            .setPositiveButton("删除") { _, _ ->
                viewModel.deleteTrajectories(ids)
                adapter.exitSelectionMode()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun showClearAllConfirmDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("清空全部")
            .setMessage("确定要删除所有轨迹吗？此操作不可恢复。")
            .setPositiveButton("清空") { _, _ ->
                val allIds = viewModel.trajectories.value?.map { it.id } ?: emptyList()
                if (allIds.isNotEmpty()) {
                    viewModel.deleteTrajectories(allIds)
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun updateSelectionModeUI(isInSelectionMode: Boolean) {
        if (isInSelectionMode) {
            normalHeader.visibility = View.GONE
            selectionHeader.visibility = View.VISIBLE
            updateSelectionCount()
        } else {
            normalHeader.visibility = View.VISIBLE
            selectionHeader.visibility = View.GONE
        }
    }

    private fun updateSelectionCount() {
        val count = adapter.getSelectedIds().size
        textSelectionCount.text = "已选择 $count 项"
    }
}