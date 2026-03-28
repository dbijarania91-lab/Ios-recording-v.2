package com.example.nothingrecorder

import android.content.Context
import android.os.Build
import android.os.PowerManager
import android.util.Log
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ThermalGovernor(private val context: Context, private val onCriticalHeat: () -> Unit) {

    private val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    private var isListening = false

    private val thermalListener = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        PowerManager.OnThermalStatusChangedListener { status ->
            when (status) {
                PowerManager.THERMAL_STATUS_SEVERE -> {
                    Log.w("ThermalGovernor", "WARNING: Snapdragon 778G is overheating. Framerate drop imminent.")
                    CoroutineScope(Dispatchers.Main).launch {
                        Toast.makeText(context, "HΞX WARNING: Device overheating! Saving video to prevent lag.", Toast.LENGTH_LONG).show()
                    }
                    onCriticalHeat.invoke() // Triggers the graceful stop
                }
                PowerManager.THERMAL_STATUS_CRITICAL -> {
                    Log.e("ThermalGovernor", "CRITICAL: Thermal shutdown threshold reached.")
                    onCriticalHeat.invoke()
                }
            }
        }
    } else null

    fun startMonitoring() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && !isListening) {
            powerManager.addThermalStatusListener(thermalListener!!)
            isListening = true
            Log.d("ThermalGovernor", "Monitoring internal Snapdragon temperatures.")
        }
    }

    fun stopMonitoring() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && isListening) {
            powerManager.removeThermalStatusListener(thermalListener!!)
            isListening = false
        }
    }
}
