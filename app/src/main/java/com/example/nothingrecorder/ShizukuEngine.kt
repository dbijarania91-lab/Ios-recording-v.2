package com.example.nothingrecorder

import android.util.Log
import rikka.shizuku.Shizuku
import java.io.File

class ShizukuEngine {
    
    // We isolate the video to a flawless, temporary HEVC file
    val tempVideoPath = "/sdcard/Movies/HEX_TEMP_VIDEO.mp4" 

    fun isReady(): Boolean {
        return Shizuku.pingBinder() && 
               Shizuku.checkSelfPermission() == android.content.pm.PackageManager.PERMISSION_GRANTED
    }

    fun startRecording() {
        if (!isReady()) {
            Log.e("ShizukuEngine", "CRITICAL ERROR: Shizuku permission denied!")
            return
        }

        val file = File(tempVideoPath)
        if (file.exists()) file.delete()

        // --- THE NATIVE SHELL HOOK ---
        // Forces hardware HEVC silicon natively without MediaProjection lag
        val command = "screenrecord --codec video/hevc --bit-rate 40000000 --size 2400x1080 $tempVideoPath"
        
        try {
            Shizuku.newProcess(arrayOf("sh", "-c", command), null, null)
            Log.d("ShizukuEngine", "Zero-Lag Video Recording Started.")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun stopRecording() {
        try {
            // --- GRACEFUL SAVE (CRITICAL) ---
            // 'killall -2' prevents MP4 corruption. It tells the binary to safely pack the header.
            val killCommand = "killall -2 screenrecord"
            Shizuku.newProcess(arrayOf("sh", "-c", killCommand), null, null)
            Log.d("ShizukuEngine", "Video gracefully saved to $tempVideoPath")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
