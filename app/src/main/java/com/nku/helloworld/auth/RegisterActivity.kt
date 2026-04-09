package com.nku.helloworld.auth

import android.os.Bundle
import android.widget.EditText
import android.widget.Toast
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.nku.helloworld.R

class RegisterActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        val inputPhone = findViewById<EditText>(R.id.inputRegisterPhone)
        val inputPassword = findViewById<EditText>(R.id.inputRegisterPassword)

        // 返回按钮
        findViewById<View>(R.id.registerBack).setOnClickListener {
            finish()
        }

        // 注册按钮
        findViewById<View>(R.id.buttonRegister).setOnClickListener {
            val phone = inputPhone.text.toString().trim()
            val password = inputPassword.text.toString()

            when {
                phone.isEmpty() -> {
                    Toast.makeText(this, "请输入手机号", Toast.LENGTH_SHORT).show()
                }
                !phone.matches(Regex("^1\\d{10}$")) -> {
                    Toast.makeText(this, "请输入有效的手机号（11位，以1开头）", Toast.LENGTH_SHORT).show()
                }
                password.isEmpty() -> {
                    Toast.makeText(this, "请输入密码", Toast.LENGTH_SHORT).show()
                }
                password.length < 6 -> {
                    Toast.makeText(this, "密码至少6位", Toast.LENGTH_SHORT).show()
                }
                else -> {
                    Toast.makeText(this, "注册成功，请登录", Toast.LENGTH_SHORT).show()
                    // 关闭Activity，返回到前一个页面
                    finish()
                }
            }
        }

        // 去登录 - 简单关闭，不跳转
        findViewById<View>(R.id.textToLogin).setOnClickListener {
            finish()
        }
    }
}
