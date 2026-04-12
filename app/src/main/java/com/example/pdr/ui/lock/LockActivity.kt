package com.example.pdr.ui.lock

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import com.example.pdr.MainActivity
import com.example.pdr.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.util.concurrent.Executor

/**
 * 锁屏验证Activity
 * 应用启动时首先显示此界面，验证通过后才进入主功能
 */
class LockActivity : AppCompatActivity() {

    private lateinit var executor: Executor
    private lateinit var biometricPrompt: BiometricPrompt
    private lateinit var promptInfo: BiometricPrompt.PromptInfo

    private lateinit var authButton: MaterialButton
    private lateinit var statusText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lock)

        // 初始化视图
        authButton = findViewById(R.id.authButton)
        statusText = findViewById(R.id.statusText)

        // 初始化生物识别
        initBiometric()

        // 点击按钮触发验证
        authButton.setOnClickListener {
            showBiometricPrompt()
        }

        // 自动触发验证（启动时立即验证）
        showBiometricPrompt()
    }

    /**
     * 初始化生物识别组件
     */
    private fun initBiometric() {
        executor = ContextCompat.getMainExecutor(this)

        biometricPrompt = BiometricPrompt(this, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    // 验证错误，显示错误信息
                    statusText.visibility = View.VISIBLE
                    statusText.text = "验证错误: $errString"

                    // 如果是用户取消，允许重试
                    if (errorCode == BiometricPrompt.ERROR_USER_CANCELED ||
                        errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
                        authButton.text = "点击重试"
                    }
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    // 验证失败（如指纹不匹配）
                    statusText.visibility = View.VISIBLE
                    statusText.text = "验证失败，请重试"
                }

                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    // 验证成功，进入主界面
                    statusText.visibility = View.INVISIBLE
                    navigateToMain()
                }
            })

        // 配置验证提示信息
        // 使用 Device Credential 允许 fallback 到 PIN/密码
        promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("身份验证")
            .setSubtitle("请使用指纹或设备密码验证")
            .setDescription("验证成功后才能使用应用功能")
            .setDeviceCredentialAllowed(true)  // 允许使用设备PIN/密码
            .build()
    }

    /**
     * 显示生物识别验证对话框
     */
    private fun showBiometricPrompt() {
        try {
            biometricPrompt.authenticate(promptInfo)
        } catch (e: Exception) {
            // 设备不支持生物识别或未设置锁屏
            showNoBiometricDialog()
        }
    }

    /**
     * 设备不支持生物识别时显示提示
     */
    private fun showNoBiometricDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("无法验证")
            .setMessage("您的设备未设置指纹或锁屏密码。\n请在系统设置中先设置设备锁屏方式后再使用本应用。")
            .setPositiveButton("去设置") { _, _ ->
                // 打开系统安全设置
                val intent = Intent(android.provider.Settings.ACTION_SECURITY_SETTINGS)
                startActivity(intent)
            }
            .setNegativeButton("退出") { _, _ ->
                finish()
            }
            .setCancelable(false)
            .show()
    }

    /**
     * 验证成功后导航到主界面
     */
    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
}