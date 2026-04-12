package com.example.pdr.ui.lock

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
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
    private lateinit var credentialPrompt: BiometricPrompt
    private lateinit var biometricPromptInfo: BiometricPrompt.PromptInfo
    private lateinit var credentialPromptInfo: BiometricPrompt.PromptInfo

    private lateinit var authButton: MaterialButton
    private lateinit var credentialButton: MaterialButton
    private lateinit var statusText: TextView

    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lock)

        // 初始化视图
        authButton = findViewById(R.id.authButton)
        credentialButton = findViewById(R.id.credentialButton)
        statusText = findViewById(R.id.statusText)

        // 初始化生物识别
        initBiometric()

        // 指纹验证按钮
        authButton.setOnClickListener {
            showBiometricPrompt()
        }

        // 使用密码按钮 - 直接调用密码验证
        credentialButton.setOnClickListener {
            showCredentialPrompt()
        }
    }

    override fun onResume() {
        super.onResume()
        // 每次恢复时自动触发指纹验证（包括首次启动）
        showBiometricPrompt()
    }

    /**
     * 初始化生物识别组件
     */
    private fun initBiometric() {
        executor = ContextCompat.getMainExecutor(this)

        // 指纹验证的 BiometricPrompt
        biometricPrompt = BiometricPrompt(this, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    statusText.visibility = View.VISIBLE
                    statusText.text = "验证错误: $errString"

                    when (errorCode) {
                        BiometricPrompt.ERROR_NEGATIVE_BUTTON -> {
                            // 用户点击"使用密码"按钮，切换到密码验证
                            statusText.text = "请输入密码"
                            handler.postDelayed({ showCredentialPrompt() }, 100)
                        }
                        BiometricPrompt.ERROR_LOCKOUT,
                        BiometricPrompt.ERROR_LOCKOUT_PERMANENT -> {
                            // 指纹失败次数过多被锁定，自动切换到密码验证
                            statusText.text = "指纹验证已锁定，请使用密码"
                            handler.postDelayed({ showCredentialPrompt() }, 100)
                        }
                        BiometricPrompt.ERROR_USER_CANCELED,
                        BiometricPrompt.ERROR_CANCELED -> {
                            // 用户取消，显示按钮
                            authButton.visibility = View.VISIBLE
                            credentialButton.visibility = View.VISIBLE
                        }
                        else -> {
                            // 其他错误，显示按钮
                            authButton.visibility = View.VISIBLE
                            credentialButton.visibility = View.VISIBLE
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

        // 密码验证的 BiometricPrompt（单独创建）
        credentialPrompt = BiometricPrompt(this, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    Log.e(TAG, "密码验证错误: errorCode=$errorCode, msg=$errString")
                    statusText.visibility = View.VISIBLE
                    statusText.text = "密码验证错误: $errString"
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
                    Log.i(TAG, "密码验证成功！准备进入主界面")
                    statusText.visibility = View.INVISIBLE
                    navigateToMain()
                }
            })

        // 指纹验证配置（带"使用密码"负按钮）
        biometricPromptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("身份验证")
            .setSubtitle("请使用指纹验证")
            .setDescription("验证成功后才能使用应用功能")
            .setNegativeButtonText("使用密码")
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_WEAK)
            .build()

        // 密码/PIN验证配置 - 只使用设备密码，不包含生物识别
        // 注意：使用 DEVICE_CREDENTIAL 时不能设置 NegativeButtonText
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
        // 先检查设备是否支持指纹
        val biometricManager = BiometricManager.from(this)
        val canAuthenticate = biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK)

        when (canAuthenticate) {
            BiometricManager.BIOMETRIC_SUCCESS -> {
                try {
                    authButton.visibility = View.INVISIBLE
                    credentialButton.visibility = View.INVISIBLE
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
     * 显示密码/PIN验证对话框 - 直接弹出密码输入界面
     */
    private fun showCredentialPrompt() {
        Log.i(TAG, "showCredentialPrompt: 开始密码验证")
        // 检查是否设置了设备锁屏
        val biometricManager = BiometricManager.from(this)
        val canAuthenticate = biometricManager.canAuthenticate(BiometricManager.Authenticators.DEVICE_CREDENTIAL)
        Log.i(TAG, "showCredentialPrompt: canAuthenticate=$canAuthenticate")

        when (canAuthenticate) {
            BiometricManager.BIOMETRIC_SUCCESS -> {
                authButton.visibility = View.INVISIBLE
                credentialButton.visibility = View.INVISIBLE
                statusText.visibility = View.INVISIBLE
                try {
                    // 使用 AUTHENTICATOR_DEVICE_CREDENTIAL 会直接显示密码输入界面
                    Log.i(TAG, "showCredentialPrompt: 调用 credentialPrompt.authenticate")
                    credentialPrompt.authenticate(credentialPromptInfo)
                } catch (e: Exception) {
                    Log.e(TAG, "showCredentialPrompt: 异常 $e")
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
        Log.i(TAG, "navigateToMain: MainActivity 已启动，准备 finish")
        finish()
    }
}