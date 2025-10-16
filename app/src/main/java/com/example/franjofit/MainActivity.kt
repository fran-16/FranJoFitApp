package com.example.franjofit

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.example.franjofit.nav.AppNav
import com.example.franjofit.ui.theme.FranJoTheme

class MainActivity : ComponentActivity() {

    private val requestActivityRecognition =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* opcional: mostrar UI */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

// en MainActivity.onCreate
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            val granted = androidx.core.content.ContextCompat.checkSelfPermission(
                this, android.Manifest.permission.ACTIVITY_RECOGNITION
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                registerForActivityResult(
                    androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
                ) { /* opcional log */ }
                    .launch(android.Manifest.permission.ACTIVITY_RECOGNITION)
            }
        }

        setContent {
            FranJoTheme { AppNav() }
        }
    }
}
