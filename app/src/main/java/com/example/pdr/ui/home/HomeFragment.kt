package com.example.pdr.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.pdr.R

class HomeFragment : Fragment() {

    private lateinit var rootView: View

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        rootView = inflater.inflate(R.layout.fragment_home, container, false)
        return rootView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupWindowInsets()
        setupClickListeners()
    }

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(rootView) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun setupClickListeners() {
        // PDR 功能卡片点击
        rootView.findViewById<View>(R.id.cardPdr).setOnClickListener {
            findNavController().navigate(R.id.action_home_to_main)
        }

        // 3D 模型查看卡片点击
        rootView.findViewById<View>(R.id.cardViewer).setOnClickListener {
            findNavController().navigate(R.id.action_home_to_viewer)
        }

        // GPS 定位卡片点击
        rootView.findViewById<View>(R.id.cardLocation).setOnClickListener {
            findNavController().navigate(R.id.action_home_to_location)
        }

        // 私密照片卡片点击
        rootView.findViewById<View>(R.id.cardPrivatePhoto).setOnClickListener {
            findNavController().navigate(R.id.action_home_to_private_photo)
        }
    }
}