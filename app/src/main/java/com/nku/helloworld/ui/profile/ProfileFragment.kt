package com.nku.helloworld.ui.profile

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
        return ComposeView(requireContext()).apply {
            setContent {
                ProfileScreen(
                    onOpenLogin = {
                        startActivity(Intent(requireContext(), LoginActivity::class.java))
                    },
                    onOpenRegister = {
                        startActivity(Intent(requireContext(), RegisterActivity::class.java))
                    }
                )
            }
        }
    }
}

@Composable
fun ProfileScreen(
    onOpenLogin: () -> Unit,
    onOpenRegister: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colorResource(id = R.color.app_bg))
            .padding(top = 52.dp, start = 24.dp, end = 24.dp, bottom = 24.dp)
    ) {
        Text(
            text = "我的",
            color = colorResource(id = R.color.text_primary),
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = stringResource(id = R.string.profile_placeholder),
            color = colorResource(id = R.color.text_secondary),
            fontSize = 14.sp,
            modifier = Modifier.padding(top = 8.dp)
        )

        Button(
            onClick = onOpenLogin,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp)
                .height(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = colorResource(id = R.color.brand_primary))
        ) {
            Text(text = stringResource(id = R.string.auth_login), fontSize = 16.sp)
        }

        OutlinedButton(
            onClick = onOpenRegister,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp)
                .height(52.dp),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, colorResource(id = R.color.brand_primary)),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = colorResource(id = R.color.brand_primary))
        ) {
            Text(text = stringResource(id = R.string.auth_register), fontSize = 16.sp)
        }
    }
}
