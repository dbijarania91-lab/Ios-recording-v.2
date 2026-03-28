
package com.example.nothingrecorder

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.material.button.MaterialButton
import rikka.shizuku.Shizuku

class MainActivity : AppCompatActivity() {

    private val MEDIA_PROJECTION_REQUEST_CODE = 1001
    private lateinit var mediaProjectionManager: MediaProjectionManager
    private var isRecording = false
    private lateinit var recordButton: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 1)
        }

        mediaProjectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        recordButton = findViewById(R.id.btn_record)

        recordButton.setOnClickListener {
            if (!isRecording) {
                if (checkShizukuPermission()) {
                    startHybridSetup()
                } else {
                    Shizuku.requestPermission(0)
                }
            } else {
                stopRecording()
            }
        }
    }

    private fun checkShizukuPermission(): Boolean {
        if (!Shizuku.pingBinder()) {
            Toast.makeText(this, "Shizuku Engine offline! Open Shizuku app.", Toast.LENGTH_LONG).show()
            return false
        }
        return Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
    }

    private fun startHybridSetup() {
        val intent = mediaProjectionManager.createScreenCaptureIntent()
        startActivityForResult(intent, MEDIA_PROJECTION_REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == MEDIA_PROJECTION_REQUEST_CODE && resultCode == Activity.RESULT_OK && data != null) {
            isRecording = true
            recordButton.text = "STOP"
            recordButton.setBackgroundColor(android.graphics.Color.DKGRAY)

            val serviceIntent = Intent(this, HybridRecordService::class.java).apply {
                action = "START_RECORDING"
                putExtra("RESULT_CODE", resultCode)
                putExtra("DATA", data)
            }
            startForegroundService(serviceIntent)
            Toast.makeText(this, "HΞX Engine Active", Toast.LENGTH_SHORT).show()
            moveTaskToBack(true) 
        }
    }

    private fun stopRecording() {
        isRecording = false
        recordButton.text = "REC"
        recordButton.setBackgroundColor(android.graphics.Color.parseColor("#FF3B30"))

        val serviceIntent = Intent(this, HybridRecordService::class.java).apply {
            action = "STOP_RECORDING"
        }
        startForegroundService(serviceIntent)
        Toast.makeText(this, "Compiling HΞX Clip...", Toast.LENGTH_LONG).show()
    }
}
