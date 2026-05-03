package com.nku.helloworld

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.nku.helloworld.auth.LoginActivity
import com.nku.helloworld.auth.RegisterActivity
import com.nku.helloworld.ui.dashboard.DashboardScreen
import com.nku.helloworld.ui.home.HomeScreen
import com.nku.helloworld.ui.profile.ProfileScreen
import com.nku.helloworld.ui.stats.StatsScreen

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MainApp()
        }
    }
}

@Composable
fun MainApp() {
    var selectedTab by remember { mutableStateOf("home") }
    val context = LocalContext.current

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = Color.White,
                tonalElevation = 8.dp
            ) {
                val iconColors = NavigationBarItemDefaults.colors(
                    selectedIconColor = colorResource(R.color.brand_primary),
                    selectedTextColor = colorResource(R.color.brand_primary),
                    indicatorColor = colorResource(R.color.brand_primary_soft),
                    unselectedIconColor = colorResource(R.color.text_secondary),
                    unselectedTextColor = colorResource(R.color.text_secondary)
                )

                NavigationBarItem(
                    icon = { Icon(painterResource(R.drawable.ic_home_black_24dp), contentDescription = null) },
                    label = { Text(stringResource(R.string.title_home)) },
                    selected = selectedTab == "home",
                    onClick = { selectedTab = "home" },
                    colors = iconColors
                )
                NavigationBarItem(
                    icon = { Icon(painterResource(R.drawable.ic_dashboard_black_24dp), contentDescription = null) },
                    label = { Text(stringResource(R.string.title_dashboard)) },
                    selected = selectedTab == "dashboard",
                    onClick = { selectedTab = "dashboard" },
                    colors = iconColors
                )
                NavigationBarItem(
                    icon = { Icon(painterResource(R.drawable.ic_dashboard_black_24dp), contentDescription = null) },
                    label = { Text(stringResource(R.string.title_stats)) },
                    selected = selectedTab == "stats",
                    onClick = { selectedTab = "stats" },
                    colors = iconColors
                )
                NavigationBarItem(
                    icon = { Icon(painterResource(R.drawable.ic_notifications_black_24dp), contentDescription = null) },
                    label = { Text(stringResource(R.string.title_profile)) },
                    selected = selectedTab == "profile",
                    onClick = { selectedTab = "profile" },
                    colors = iconColors
                )
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            when (selectedTab) {
                "home" -> HomeScreen()
                "dashboard" -> DashboardScreen()
                "stats" -> StatsScreen()
                "profile" -> ProfileScreen(
                    onOpenLogin = {
                        context.startActivity(Intent(context, LoginActivity::class.java))
                    },
                    onOpenRegister = {
                        context.startActivity(Intent(context, RegisterActivity::class.java))
                    }
                )
            }
        }
    }
}