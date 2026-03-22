package com.example.pdr.ui.main

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.pdr.R
import com.example.pdr.databinding.FragmentMainBinding

class MainFragment : Fragment() {

    private var _binding: FragmentMainBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MainViewModel by viewModels()

    // 权限请求
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.entries.all { it.value }
        if (allGranted) {
            viewModel.startRecording()
        } else {
            Toast.makeText(requireContext(), "需要传感器权限才能记录轨迹", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMainBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupWindowInsets()
        setupViews()
        observeViewModel()
        handleNavigationArgs()
    }

    private fun setupWindowInsets() {
        // 处理刘海屏和状态栏
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun setupViews() {
        binding.btnStart.setOnClickListener {
            checkPermissionsAndStart()
        }

        binding.btnStop.setOnClickListener {
            viewModel.stopRecording()
        }

        binding.btnCalibrate.setOnClickListener {
            viewModel.calibrateHeading()
            Toast.makeText(requireContext(), "方向已校准", Toast.LENGTH_SHORT).show()
        }

        binding.btnHistory.setOnClickListener {
            findNavController().navigate(R.id.action_main_to_history)
        }
    }

    private fun observeViewModel() {
        viewModel.isRecording.observe(viewLifecycleOwner) { isRecording ->
            binding.btnStart.isEnabled = !isRecording
            binding.btnStop.isEnabled = isRecording
            binding.btnCalibrate.isEnabled = isRecording

            if (isRecording) {
                binding.btnStart.text = "记录中..."
            } else {
                binding.btnStart.text = "开始记录"
            }
        }

        viewModel.totalSteps.observe(viewLifecycleOwner) { steps ->
            binding.textSteps.text = steps.toString()
        }

        viewModel.totalDistance.observe(viewLifecycleOwner) { distance ->
            binding.textDistance.text = String.format("%.1f m", distance)
        }

        viewModel.duration.observe(viewLifecycleOwner) { duration ->
            binding.textDuration.text = viewModel.formatDuration(duration)
        }

        viewModel.trajectoryPoints.observe(viewLifecycleOwner) { points ->
            binding.trajectoryView.setPoints(points)
        }

        viewModel.statusMessage.observe(viewLifecycleOwner) { message ->
            binding.textStatus.text = message
        }

        viewModel.sensorStatus.observe(viewLifecycleOwner) { status ->
            val missingSensors = status.filter { !it.value }.keys
            if (missingSensors.isNotEmpty()) {
                binding.textStatus.text = "缺少传感器: ${missingSensors.joinToString(", ")}"
            }
        }
    }

    private fun handleNavigationArgs() {
        // 处理从历史记录页面返回时加载轨迹
        val trajectoryId = arguments?.getLong("trajectoryId", 0L) ?: 0L
        if (trajectoryId > 0) {
            viewModel.loadTrajectory(trajectoryId)
            // 清除参数避免重复加载
            arguments?.clear()
        }
    }

    private fun checkPermissionsAndStart() {
        val permissions = mutableListOf<String>()

        // Android 10+ 需要活动识别权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.ACTIVITY_RECOGNITION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                permissions.add(Manifest.permission.ACTIVITY_RECOGNITION)
            }
        }

        // Android 13+ 需要通知权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                permissions.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        if (permissions.isEmpty()) {
            viewModel.startRecording()
        } else {
            permissionLauncher.launch(permissions.toTypedArray())
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}