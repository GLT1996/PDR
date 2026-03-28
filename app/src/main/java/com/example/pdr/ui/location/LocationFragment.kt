package com.example.pdr.ui.location

import android.Manifest
import android.content.pm.PackageManager
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
import com.example.pdr.databinding.FragmentLocationBinding

class LocationFragment : Fragment() {

    private var _binding: FragmentLocationBinding? = null
    private val binding get() = _binding!!

    private val viewModel: LocationViewModel by viewModels()

    // 权限请求
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseLocationGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true

        if (fineLocationGranted || coarseLocationGranted) {
            // 提示用户权限级别
            if (!fineLocationGranted && coarseLocationGranted) {
                Toast.makeText(requireContext(), "注意：仅授予粗略位置权限，定位精度较低", Toast.LENGTH_LONG).show()
            }
            viewModel.startLocationUpdates()
        } else {
            Toast.makeText(requireContext(), "需要定位权限才能获取位置", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLocationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupWindowInsets()
        setupViews()
        observeViewModel()
    }

    private fun setupWindowInsets() {
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
            viewModel.stopLocationUpdates()
        }
    }

    private fun observeViewModel() {
        viewModel.isLocating.observe(viewLifecycleOwner) { isLocating ->
            binding.btnStart.isEnabled = !isLocating
            binding.btnStop.isEnabled = isLocating

            if (isLocating) {
                binding.btnStart.text = "定位中..."
            } else {
                binding.btnStart.text = "开始定位"
            }
        }

        viewModel.locationState.observe(viewLifecycleOwner) { state ->
            val stateText = when (state) {
                LocationViewModel.LocationState.IDLE -> "状态: 未开始"
                LocationViewModel.LocationState.SEARCHING_GPS -> "状态: 搜索GPS信号..."
                LocationViewModel.LocationState.SEARCHING_NETWORK -> "状态: 网络定位"
                LocationViewModel.LocationState.LOCATED -> "状态: 已定位 ✓"
                LocationViewModel.LocationState.NO_SIGNAL -> "状态: 无信号 ⚠"
                LocationViewModel.LocationState.PERMISSION_DENIED -> "状态: 权限被拒绝 ✗"
            }
            binding.textStatus.text = stateText
        }

        viewModel.statusMessage.observe(viewLifecycleOwner) { message ->
            binding.textStatus.text = message
        }

        viewModel.updateCountText.observe(viewLifecycleOwner) { count ->
            binding.textUpdateCount.text = count
        }

        viewModel.diagnosticInfo.observe(viewLifecycleOwner) { info ->
            binding.textDiagnostic.text = info
        }

        viewModel.longitude.observe(viewLifecycleOwner) { longitude ->
            binding.textLongitude.text = longitude
        }

        viewModel.latitude.observe(viewLifecycleOwner) { latitude ->
            binding.textLatitude.text = latitude
        }

        viewModel.accuracy.observe(viewLifecycleOwner) { accuracy ->
            binding.textAccuracy.text = accuracy
        }

        viewModel.speed.observe(viewLifecycleOwner) { speed ->
            binding.textSpeed.text = speed
        }

        viewModel.bearing.observe(viewLifecycleOwner) { bearing ->
            binding.textBearing.text = bearing
        }

        viewModel.provider.observe(viewLifecycleOwner) { provider ->
            binding.textProvider.text = provider
        }

        viewModel.updateTime.observe(viewLifecycleOwner) { time ->
            binding.textUpdateTime.text = time
        }
    }

    private fun checkPermissionsAndStart() {
        val permissions = mutableListOf<String>()

        // 检查精确位置权限
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        // 检查粗略位置权限
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION)
        }

        if (permissions.isEmpty()) {
            viewModel.startLocationUpdates()
        } else {
            permissionLauncher.launch(permissions.toTypedArray())
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}