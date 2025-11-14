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
import com.example.franjofit.reminders.SmpReminderManager
import com.example.franjofit.ui.theme.FranJoTheme

class MainActivity : ComponentActivity() {

    private val requestActivityRecognition =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {}

    private val requestNotificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ACTIVITY_RECOGNITION
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val granted = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACTIVITY_RECOGNITION
            ) == PackageManager.PERMISSION_GRANTED

            if (!granted) requestActivityRecognition.launch(Manifest.permission.ACTIVITY_RECOGNITION)
        }

        // POST_NOTIFICATIONS
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

            if (!granted) requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        // PROGRAMAR RECORDATORIOS SMP
        SmpReminderManager.scheduleRepeating(this)

        setContent {
            FranJoTheme {
                AppNav()
            }
        }
    }
}
