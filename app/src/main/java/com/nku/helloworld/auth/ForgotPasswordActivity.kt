package com.nku.helloworld.auth

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.nku.helloworld.R

class ForgotPasswordActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forgot_password)

        findViewById<View>(R.id.forgotBack).setOnClickListener {
            finish()
        }
    }
}

