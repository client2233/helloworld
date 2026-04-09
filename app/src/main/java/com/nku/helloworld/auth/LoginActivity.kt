package com.nku.helloworld.auth

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.MotionEvent
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.nku.helloworld.R

class LoginActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val etUsername = findViewById<EditText>(R.id.etUsername)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val tvToRegister = findViewById<TextView>(R.id.tvToRegister)
        val tvForgotPassword = findViewById<TextView>(R.id.tvForgotPassword)
        val tvPhoneLogin = findViewById<TextView>(R.id.tvPhoneLogin)
        val tvWechatLogin = findViewById<TextView>(R.id.tvWechatLogin)

        var isPasswordVisible = false
        etPassword.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                val endDrawable = etPassword.compoundDrawablesRelative[2]
                if (endDrawable != null) {
                    val iconTouchStart = etPassword.width - etPassword.paddingEnd - endDrawable.bounds.width()
                    if (event.x >= iconTouchStart) {
                        isPasswordVisible = !isPasswordVisible
                        etPassword.inputType = if (isPasswordVisible) {
                            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                        } else {
                            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                        }
                        etPassword.setCompoundDrawablesRelativeWithIntrinsicBounds(
                            R.drawable.ic_password,
                            0,
                            if (isPasswordVisible) R.drawable.ic_visibility else R.drawable.ic_visibility_off,
                            0
                        )
                        etPassword.setSelection(etPassword.text.length)
                        return@setOnTouchListener true
                    }
                }
            }
            false
        }

        btnLogin.setOnClickListener {
            val username = etUsername.text.toString()
            val password = etPassword.text.toString()
            if (username.isNotEmpty() && password.isNotEmpty()) {
                Toast.makeText(this, "登录成功", Toast.LENGTH_SHORT).show()
                finish()
            } else {
                Toast.makeText(this, "请输入账号和密码", Toast.LENGTH_SHORT).show()
            }
        }

        tvToRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        tvForgotPassword.setOnClickListener {
            startActivity(Intent(this, ForgotPasswordActivity::class.java))
        }

        tvPhoneLogin.setOnClickListener {
            Toast.makeText(this, getString(R.string.auth_phone_login_tips), Toast.LENGTH_SHORT).show()
        }

        tvWechatLogin.setOnClickListener {
            Toast.makeText(this, getString(R.string.auth_third_party_tips), Toast.LENGTH_SHORT).show()
        }
    }
}