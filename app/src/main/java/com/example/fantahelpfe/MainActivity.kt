package com.example.fantahelpfe

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.fantahelpfe.ui.navigation.AppNavigation
import com.example.fantahelpfe.ui.theme.FantaHelpFETheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint // This enables Hilt for this Activity
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FantaHelpFETheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // For now, we are just showing one screen.
                    // Later, this will be replaced by our NavHost.
                    AppNavigation()
                }
            }
        }
    }
}