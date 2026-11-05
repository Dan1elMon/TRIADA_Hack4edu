package com.example.triada

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.example.triada.feature.learning.LearningViewModel
import dagger.hilt.android.AndroidEntryPoint
import com.example.triada.core.navigation.AulaNexoNavHost
import com.example.triada.ui.theme.AulaNexoTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val viewModel: LearningViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AulaNexoTheme { AulaNexoNavHost(viewModel) }
        }
    }
}
