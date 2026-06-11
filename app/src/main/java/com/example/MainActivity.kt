package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.GalleryApp
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.GalleryViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: GalleryViewModel = viewModel()
            val isDarkThemeState = viewModel.isDarkTheme.collectAsState()

            MyApplicationTheme(darkTheme = isDarkThemeState.value) {
                GalleryApp(viewModel = viewModel)
            }
        }
    }
}
