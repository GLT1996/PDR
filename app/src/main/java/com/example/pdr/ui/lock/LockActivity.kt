package com.example.pdr.ui.lock

import android.content.Intent
import android.os.Bundle
import android.util.Log
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

    companion object {
        private const val TAG = "LockActivity"
    }

    private lateinit var executor: Executor
    private lateinit var biometricPrompt: BiometricPrompt      // 指纹验证
    private lateinit var credentialPrompt: BiometricPrompt    // 密码验证（独立对象）
    private lateinit var biometricPromptInfo: BiometricPrompt.PromptInfo
    private lateinit var credentialPromptInfo: BiometricPrompt.PromptInfo

    private lateinit var authButton: MaterialButton
    private lateinit var credentialButton: MaterialButton
    private lateinit var statusText: TextView

    // 验证状态标志
    private var isAuthenticating = false
    private var isAuthenticated = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lock)
        Log.i(TAG, "onCreate: LockActivity 创建")

        // 初始化视图
        authButton = findViewById(R.id.authButton)
        credentialButton = findViewById(R.id.credentialButton)
        statusText = findViewById(R.id.statusText)

        // 初始化生物识别
        initBiometric()

        // 指纹验证按钮
        authButton.setOnClickListener {
            Log.i(TAG, "点击指纹验证按钮")
            showBiometricPrompt()
        }

        // 使用密码按钮
        credentialButton.setOnClickListener {
            Log.i(TAG, "点击使用密码按钮")
            showCredentialPrompt()
        }

        // 首次启动时自动触发指纹验证
        showBiometricPrompt()
    }

    override fun onResume() {
        super.onResume()
        Log.i(TAG, "onResume: isAuthenticated=$isAuthenticated, isAuthenticating=$isAuthenticating")
        if (!isAuthenticated && !isAuthenticating) {
            authButton.visibility = View.VISIBLE
            credentialButton.visibility = View.VISIBLE
        }
    }

    /**
     * 初始化生物识别组件
     */
    private fun initBiometric() {
        executor = ContextCompat.getMainExecutor(this)

        // ========== 指纹验证的 BiometricPrompt ==========
        biometricPrompt = BiometricPrompt(this, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    Log.e(TAG, "指纹验证错误: errorCode=$errorCode, msg=$errString")
                    isAuthenticating = false

                    when (errorCode) {
                        BiometricPrompt.ERROR_NEGATIVE_BUTTON -> {
                            Log.i(TAG, "用户点击使用密码，切换到密码验证")
                            showCredentialPrompt()
                        }
                        BiometricPrompt.ERROR_LOCKOUT,
                        BiometricPrompt.ERROR_LOCKOUT_PERMANENT -> {
                            Log.w(TAG, "指纹锁定，切换到密码验证")
                            statusText.visibility = View.VISIBLE
                            statusText.text = "指纹已锁定，请使用密码"
                            showCredentialPrompt()
                        }
                        BiometricPrompt.ERROR_USER_CANCELED,
                        BiometricPrompt.ERROR_CANCELED -> {
                            Log.i(TAG, "用户取消指纹验证")
                            authButton.visibility = View.VISIBLE
                            credentialButton.visibility = View.VISIBLE
                        }
                        else -> {
                            statusText.visibility = View.VISIBLE
                            statusText.text = "验证错误: $errString"
                            authButton.visibility = View.VISIBLE
                            credentialButton.visibility = View.VISIBLE
                        }
                    }
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    Log.w(TAG, "指纹验证失败")
                    statusText.visibility = View.VISIBLE
                    statusText.text = "指纹不匹配，请重试"
                }

                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    Log.i(TAG, "指纹验证成功！")
                    isAuthenticating = false
                    isAuthenticated = true
                    navigateToMain()
                }
            })

        // ========== 密码验证的 BiometricPrompt（独立对象）==========
        credentialPrompt = BiometricPrompt(this, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    Log.e(TAG, "密码验证错误: errorCode=$errorCode, msg=$errString")
                    isAuthenticating = false

                    statusText.visibility = View.VISIBLE
                    statusText.text = "验证错误: $errString"
                    authButton.visibility = View.VISIBLE
                    credentialButton.visibility = View.VISIBLE
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    Log.w(TAG, "密码验证失败")
                    statusText.visibility = View.VISIBLE
                    statusText.text = "密码错误，请重试"
                }

                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    Log.e(TAG, "=== 密码验证成功！准备跳转 ===")
                    android.widget.Toast.makeText(this@LockActivity, "验证成功！", android.widget.Toast.LENGTH_SHORT).show()
                    isAuthenticating = false
                    isAuthenticated = true
                    navigateToMain()
                }
            })

        // 指纹验证配置
        biometricPromptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("身份验证")
            .setSubtitle("请使用指纹验证")
            .setDescription("验证成功后才能使用应用功能")
            .setNegativeButtonText("使用密码")
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_WEAK)
            .build()

        // 密码验证配置
        credentialPromptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("密码验证")
            .setSubtitle("请输入设备锁屏密码")
            .setDescription("验证成功后才能使用应用功能")
            .setAllowedAuthenticators(BiometricManager.Authenticators.DEVICE_CREDENTIAL)
            .build()
    }

    /**
     * 显示指纹验证对话框
     */
    private fun showBiometricPrompt() {
        if (isAuthenticating || isAuthenticated) {
            Log.i(TAG, "showBiometricPrompt: 跳过")
            return
        }

        Log.i(TAG, "showBiometricPrompt: 开始")
        val biometricManager = BiometricManager.from(this)
        val canAuthenticate = biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK)
        Log.i(TAG, "showBiometricPrompt: canAuthenticate=$canAuthenticate")

        when (canAuthenticate) {
            BiometricManager.BIOMETRIC_SUCCESS -> {
                try {
                    isAuthenticating = true
                    authButton.visibility = View.INVISIBLE
                    credentialButton.visibility = View.INVISIBLE
                    statusText.visibility = View.INVISIBLE
                    biometricPrompt.authenticate(biometricPromptInfo)
                    Log.i(TAG, "showBiometricPrompt: 已调用")
                } catch (e: Exception) {
                    Log.e(TAG, "showBiometricPrompt: 异常 $e")
                    isAuthenticating = false
                    showCredentialPrompt()
                }
            }
            else -> {
                Log.i(TAG, "showBiometricPrompt: 指纹不可用，切换密码")
                showCredentialPrompt()
            }
        }
    }

    /**
     * 显示密码验证对话框 - 使用独立的 BiometricPrompt
     */
    private fun showCredentialPrompt() {
        // 点击按钮时重置状态，允许启动密码验证
        isAuthenticating = false

        if (isAuthenticated) {
            Log.i(TAG, "showCredentialPrompt: 已验证，跳过")
            return
        }

        Log.i(TAG, "showCredentialPrompt: 开始密码验证")

        // 取消指纹验证（如果正在进行）
        try {
            biometricPrompt.cancelAuthentication()
            Log.i(TAG, "showCredentialPrompt: 已取消指纹验证")
        } catch (e: Exception) {
            Log.w(TAG, "showCredentialPrompt: 取消指纹失败: $e")
        }

        val biometricManager = BiometricManager.from(this)
        val canAuthenticate = biometricManager.canAuthenticate(BiometricManager.Authenticators.DEVICE_CREDENTIAL)
        Log.i(TAG, "showCredentialPrompt: canAuthenticate=$canAuthenticate")

        when (canAuthenticate) {
            BiometricManager.BIOMETRIC_SUCCESS -> {
                try {
                    isAuthenticating = true
                    authButton.visibility = View.INVISIBLE
                    credentialButton.visibility = View.INVISIBLE
                    statusText.visibility = View.INVISIBLE
                    // 使用独立的 credentialPrompt 对象
                    credentialPrompt.authenticate(credentialPromptInfo)
                    Log.e(TAG, "=== showCredentialPrompt: 密码验证对话框已弹出 ===")
                    android.widget.Toast.makeText(this@LockActivity, "密码对话框已弹出", android.widget.Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Log.e(TAG, "showCredentialPrompt: 异常 $e")
                    isAuthenticating = false
                    showNoLockDialog()
                }
            }
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                Log.w(TAG, "showCredentialPrompt: 未设置锁屏密码")
                showNoLockDialog()
            }
            else -> {
                Log.w(TAG, "showCredentialPrompt: 其他错误")
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
                startActivity(Intent(android.provider.Settings.ACTION_SECURITY_SETTINGS))
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
        Log.e(TAG, "=== navigateToMain: 开始跳转 MainActivity ===")
        android.widget.Toast.makeText(this, "正在跳转主界面...", android.widget.Toast.LENGTH_SHORT).show()
        startActivity(Intent(this, MainActivity::class.java))
        Log.e(TAG, "=== navigateToMain: startActivity 已调用，准备 finish ===")
        finish()
        Log.e(TAG, "=== navigateToMain: finish 已调用 ===")
    }
}