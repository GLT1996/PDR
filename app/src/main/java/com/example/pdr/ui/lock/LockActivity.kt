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
    private lateinit var biometricPrompt: BiometricPrompt
    private lateinit var biometricPromptInfo: BiometricPrompt.PromptInfo
    private lateinit var credentialPromptInfo: BiometricPrompt.PromptInfo

    private lateinit var authButton: MaterialButton
    private lateinit var credentialButton: MaterialButton
    private lateinit var statusText: TextView

    // 验证状态标志：防止重复触发验证
    private var isAuthenticating = false
    // 是否已经验证通过
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
        // 只有在未验证且未正在验证时才触发
        if (!isAuthenticated && !isAuthenticating) {
            // 不在这里自动触发，让用户点击按钮
            authButton.visibility = View.VISIBLE
            credentialButton.visibility = View.VISIBLE
        }
    }

    /**
     * 初始化生物识别组件 - 使用单个 BiometricPrompt
     */
    private fun initBiometric() {
        executor = ContextCompat.getMainExecutor(this)

        // 创建一个统一的 BiometricPrompt
        biometricPrompt = BiometricPrompt(this, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    Log.e(TAG, "onAuthenticationError: errorCode=$errorCode, msg=$errString")
                    isAuthenticating = false

                    statusText.visibility = View.VISIBLE
                    statusText.text = "验证错误: $errString"

                    when (errorCode) {
                        BiometricPrompt.ERROR_NEGATIVE_BUTTON -> {
                            // 用户点击"使用密码"按钮
                            Log.i(TAG, "用户点击使用密码，切换到密码验证")
                            statusText.text = "请输入密码"
                            showCredentialPrompt()
                        }
                        BiometricPrompt.ERROR_LOCKOUT,
                        BiometricPrompt.ERROR_LOCKOUT_PERMANENT -> {
                            Log.w(TAG, "指纹锁定，切换到密码验证")
                            statusText.text = "指纹验证已锁定，请使用密码"
                            showCredentialPrompt()
                        }
                        BiometricPrompt.ERROR_USER_CANCELED,
                        BiometricPrompt.ERROR_CANCELED -> {
                            Log.i(TAG, "用户取消验证")
                            authButton.visibility = View.VISIBLE
                            credentialButton.visibility = View.VISIBLE
                        }
                        else -> {
                            authButton.visibility = View.VISIBLE
                            credentialButton.visibility = View.VISIBLE
                        }
                    }
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    Log.w(TAG, "onAuthenticationFailed: 验证失败")
                    statusText.visibility = View.VISIBLE
                    statusText.text = "验证失败，请重试"
                }

                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    Log.i(TAG, "onAuthenticationSucceeded: 验证成功！authenticatorType=${result.authenticationType}")
                    isAuthenticating = false
                    isAuthenticated = true
                    statusText.visibility = View.INVISIBLE
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

        // 密码验证配置 - 只使用设备密码
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
            Log.i(TAG, "showBiometricPrompt: 跳过，isAuthenticating=$isAuthenticating, isAuthenticated=$isAuthenticated")
            return
        }

        Log.i(TAG, "showBiometricPrompt: 开始指纹验证")
        val biometricManager = BiometricManager.from(this)
        val canAuthenticate = biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK)
        Log.i(TAG, "showBiometricPrompt: BIOMETRIC_WEAK canAuthenticate=$canAuthenticate")

        when (canAuthenticate) {
            BiometricManager.BIOMETRIC_SUCCESS -> {
                try {
                    isAuthenticating = true
                    authButton.visibility = View.INVISIBLE
                    credentialButton.visibility = View.INVISIBLE
                    statusText.visibility = View.INVISIBLE
                    biometricPrompt.authenticate(biometricPromptInfo)
                    Log.i(TAG, "showBiometricPrompt: authenticate 已调用")
                } catch (e: Exception) {
                    Log.e(TAG, "showBiometricPrompt: 异常 $e")
                    isAuthenticating = false
                    showCredentialPrompt()
                }
            }
            else -> {
                Log.i(TAG, "showBiometricPrompt: 指纹不可用，切换密码验证")
                showCredentialPrompt()
            }
        }
    }

    /**
     * 显示密码验证对话框
     */
    private fun showCredentialPrompt() {
        if (isAuthenticating || isAuthenticated) {
            Log.i(TAG, "showCredentialPrompt: 跳过，isAuthenticating=$isAuthenticating, isAuthenticated=$isAuthenticated")
            return
        }

        Log.i(TAG, "showCredentialPrompt: 开始密码验证")
        val biometricManager = BiometricManager.from(this)
        val canAuthenticate = biometricManager.canAuthenticate(BiometricManager.Authenticators.DEVICE_CREDENTIAL)
        Log.i(TAG, "showCredentialPrompt: DEVICE_CREDENTIAL canAuthenticate=$canAuthenticate")

        when (canAuthenticate) {
            BiometricManager.BIOMETRIC_SUCCESS -> {
                try {
                    isAuthenticating = true
                    authButton.visibility = View.INVISIBLE
                    credentialButton.visibility = View.INVISIBLE
                    statusText.visibility = View.INVISIBLE
                    biometricPrompt.authenticate(credentialPromptInfo)
                    Log.i(TAG, "showCredentialPrompt: authenticate 已调用")
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
                Log.w(TAG, "showCredentialPrompt: 其他错误 $canAuthenticate")
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
        Log.i(TAG, "navigateToMain: 开始跳转到 MainActivity")
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
}