package com.example.franjofit

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.franjofit.nav.AppNav
import com.example.franjofit.ui.theme.FranJoTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            FranJoTheme {
                AppNav()
            }
        }
    }
}

