package com.example.nothingrecorder

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat

class HybridRecordService : Service() {

    private var mediaProjection: MediaProjection? = null
    private val shizukuEngine = ShizukuEngine()
    private val audioEngine = InternalAudioEngine()
    private lateinit var stitcher: Stitcher
    private lateinit var thermalGovernor: ThermalGovernor

    companion object {
        const val CHANNEL_ID = "HexRecorderChannel"
        const val NOTIFICATION_ID = 999
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        stitcher = Stitcher(this)
        
        // If the phone hits critical heat, automatically trigger the STOP sequence
        thermalGovernor = ThermalGovernor(this) {
            stopRecordingSequence()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "START_RECORDING") {
            val notification = NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("HΞX Engine Active")
                .setContentText("Zero-Lag Recording in Progress...")
                .setSmallIcon(android.R.drawable.ic_menu_camera)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .build()

            startForeground(NOTIFICATION_ID, notification)

            val resultCode = intent.getIntExtra("RESULT_CODE", -1)
            val data: Intent? = intent.getParcelableExtra("DATA")
            
            if (resultCode == android.app.Activity.RESULT_OK && data != null) {
                val projectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
                mediaProjection = projectionManager.getMediaProjection(resultCode, data)

                // Triple Ignition
                shizukuEngine.startRecording()
                audioEngine.startRecording(mediaProjection!!)
                thermalGovernor.startMonitoring()
            }
        } else if (intent?.action == "STOP_RECORDING") {
            stopRecordingSequence()
        }
        return START_NOT_STICKY
    }

    private fun stopRecordingSequence() {
        thermalGovernor.stopMonitoring()
        shizukuEngine.stopRecording()
        audioEngine.stopRecording()
        mediaProjection?.stop()
        
        stopForeground(STOP_FOREGROUND_REMOVE)
        
        // Mux files
        stitcher.muxVideoAndAudio(shizukuEngine.tempVideoPath, audioEngine.tempAudioPath)
        
        stopSelf()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "God-Tier Recorder", NotificationManager.IMPORTANCE_HIGH)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
