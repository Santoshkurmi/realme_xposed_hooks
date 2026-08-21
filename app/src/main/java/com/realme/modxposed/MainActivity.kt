package com.realme.modxposed

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.realme.modxposed.ui.screens.MainAppScreen
import com.realme.modxposed.ui.theme.RealmeModTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RealmeModTheme {
                MainAppScreen()
            }
        }
    }
}
