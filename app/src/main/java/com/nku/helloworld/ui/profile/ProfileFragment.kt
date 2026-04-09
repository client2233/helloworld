package com.nku.helloworld.ui.profile

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.nku.helloworld.R
import com.nku.helloworld.auth.LoginActivity
import com.nku.helloworld.auth.RegisterActivity

class ProfileFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val root = inflater.inflate(R.layout.fragment_profile, container, false)

        // 打开登录页面
        root.findViewById<View>(R.id.buttonOpenLogin).setOnClickListener {
            startActivity(Intent(requireContext(), LoginActivity::class.java))
        }

        // 打开注册页面
        root.findViewById<View>(R.id.buttonOpenRegister).setOnClickListener {
            startActivity(Intent(requireContext(), RegisterActivity::class.java))
        }

        return root
    }
}

