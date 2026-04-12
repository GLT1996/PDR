package com.example.pdr.ui.lock

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
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
    private lateinit var biometricPromptInfo: BiometricPrompt.PromptInfo
    private lateinit var credentialPromptInfo: BiometricPrompt.PromptInfo

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
    }

    override fun onResume() {
        super.onResume()
        // 每次恢复时自动触发验证（包括首次启动）
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
                    statusText.visibility = View.VISIBLE
                    statusText.text = "验证错误: $errString"

                    when (errorCode) {
                        BiometricPrompt.ERROR_NEGATIVE_BUTTON -> {
                            // 用户点击"使用密码"按钮，切换到密码验证
                            showCredentialPrompt()
                        }
                        BiometricPrompt.ERROR_LOCKOUT,
                        BiometricPrompt.ERROR_LOCKOUT_PERMANENT -> {
                            // 指纹失败次数过多被锁定，自动切换到密码验证
                            statusText.text = "指纹验证已锁定，请使用密码"
                            showCredentialPrompt()
                        }
                        BiometricPrompt.ERROR_USER_CANCELED,
                        BiometricPrompt.ERROR_CANCELED -> {
                            // 用户取消，显示重试按钮
                            authButton.visibility = View.VISIBLE
                            authButton.text = "点击重试"
                        }
                        else -> {
                            // 其他错误，显示重试按钮
                            authButton.visibility = View.VISIBLE
                            authButton.text = "点击重试"
                        }
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

        // 指纹验证配置（带"使用密码"按钮）
        biometricPromptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("身份验证")
            .setSubtitle("请使用指纹验证")
            .setDescription("验证成功后才能使用应用功能")
            .setNegativeButtonText("使用密码")
            .build()

        // 密码/PIN验证配置
        credentialPromptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("密码验证")
            .setSubtitle("请输入设备锁屏密码")
            .setDescription("验证成功后才能使用应用功能")
            .setDeviceCredentialAllowed(true)
            .build()
    }

    /**
     * 显示指纹验证对话框
     */
    private fun showBiometricPrompt() {
        // 先检查设备是否支持指纹
        val biometricManager = BiometricManager.from(this)
        val canAuthenticate = biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK)

        when (canAuthenticate) {
            BiometricManager.BIOMETRIC_SUCCESS -> {
                try {
                    authButton.visibility = View.INVISIBLE
                    statusText.visibility = View.INVISIBLE
                    biometricPrompt.authenticate(biometricPromptInfo)
                } catch (e: Exception) {
                    // 指纹不可用，直接用密码验证
                    showCredentialPrompt()
                }
            }
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                // 未录入指纹，直接用密码验证
                showCredentialPrompt()
            }
            else -> {
                // 其他错误，直接用密码验证
                showCredentialPrompt()
            }
        }
    }

    /**
     * 显示密码/PIN验证对话框
     */
    private fun showCredentialPrompt() {
        // 检查是否设置了设备锁屏
        val biometricManager = BiometricManager.from(this)
        val canAuthenticate = biometricManager.canAuthenticate(BiometricManager.Authenticators.DEVICE_CREDENTIAL)

        when (canAuthenticate) {
            BiometricManager.BIOMETRIC_SUCCESS -> {
                authButton.visibility = View.INVISIBLE
                statusText.visibility = View.INVISIBLE
                try {
                    biometricPrompt.authenticate(credentialPromptInfo)
                } catch (e: Exception) {
                    showNoLockDialog()
                }
            }
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                showNoLockDialog()
            }
            else -> {
                showNoLockDialog()
            }
        }
    }

    /**
     * 未设置锁屏时显示提示
     */
    private fun showNoLockDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("无法验证")
            .setMessage("您的设备未设置指纹或锁屏密码。\n请在系统设置中先设置设备锁屏方式后再使用本应用。")
            .setPositiveButton("去设置") { _, _ ->
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