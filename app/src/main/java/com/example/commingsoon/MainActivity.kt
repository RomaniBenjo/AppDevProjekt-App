package com.example.commingsoon

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.example.commingsoon.components.AppLayout
import com.example.commingsoon.ui.theme.AppThemeViewModel
import com.example.commingsoon.ui.theme.CommingSoonTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val navController = rememberNavController()
            val themeViewModel: AppThemeViewModel = viewModel()
            val currentAppTheme = themeViewModel.getThemeDefinition()

            CommingSoonTheme(
                theme = currentAppTheme
            ) {
                AppLayout(
                    navController = navController,
                    themeDefinition = currentAppTheme,
                    title = "Comming Soon"
                )
            }

        }
    }
}

