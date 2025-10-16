package com.example.franjofit.data

import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.max

class StepCounterManager(
    private val context: Context,
    private val emulatorMode: Boolean = false  // <- pon true si quieres simular
) {
    private val sm = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val prefs = context.getSharedPreferences("steps_prefs", Context.MODE_PRIVATE)
    private val fmt = DateTimeFormatter.ISO_LOCAL_DATE

    fun stepsTodayFlow(): Flow<Int> = callbackFlow {
        if (!hasPermission()) {
            Log.w("Steps", "Sin permiso ACTIVITY_RECOGNITION")
            trySend(0); close(); return@callbackFlow
        }

        if (emulatorMode) {
            // ----- MODO SIMULADOR: sube pasos cada 2s -----
            val t = object : Thread() {
                var s = 0
                override fun run() {
                    try {
                        while (!isInterrupted) {
                            sleep(2000)
                            s += (10..40).random()
                            Log.d("Steps","SIM hoy=$s")
                            trySend(s)
                        }
                    } catch (_: InterruptedException) {}
                }
            }
            t.start()
            awaitClose { t.interrupt() }
            return@callbackFlow
        }

        val counter = sm.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
        val detector = sm.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)
        Log.d("Steps", "Tiene COUNTER=${counter!=null}, DETECTOR=${detector!=null}")

        if (counter==null && detector==null) {
            Log.e("Steps","El dispositivo no trae sensores de pasos")
            trySend(0); close(); return@callbackFlow
        }

        var detectorAccum = 0L

        val listener = object : SensorEventListener {
            override fun onSensorChanged(e: SensorEvent) {
                val day = LocalDate.now().format(fmt)
                val keyBase = "baseline_$day"

                when (e.sensor.type) {
                    Sensor.TYPE_STEP_COUNTER -> {
                        val total = e.values[0].toLong()
                        if (!prefs.contains(keyBase)) {
                            prefs.edit().putLong(keyBase, total).apply()
                            Log.d("Steps","Set baseline=$total para $day")
                        }
                        val base = prefs.getLong(keyBase, total)
                        val today = max(0, (total - base).toInt())
                        Log.d("Steps","COUNTER total=$total base=$base hoy=$today")
                        trySend(today)
                    }
                    Sensor.TYPE_STEP_DETECTOR -> {
                        detectorAccum += 1
                        Log.d("Steps","DETECTOR hoy=$detectorAccum")
                        trySend(detectorAccum.toInt())
                    }
                }
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        counter?.let { sm.registerListener(listener, it, SensorManager.SENSOR_DELAY_UI) }
        detector?.let { sm.registerListener(listener, it, SensorManager.SENSOR_DELAY_UI) }

        awaitClose { sm.unregisterListener(listener) }
    }

    private fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(
            context, android.Manifest.permission.ACTIVITY_RECOGNITION
        ) == PackageManager.PERMISSION_GRANTED
}
