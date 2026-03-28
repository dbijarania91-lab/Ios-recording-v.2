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

    // --- THE SHIZUKU REFLECTION BYPASS ---
    // Forces access to the private newProcess API without needing a heavy UserService
    private fun executeShellCommand(command: String) {
        try {
            val clazz = Class.forName("rikka.shizuku.Shizuku")
            val method = clazz.getDeclaredMethod("newProcess", Array<String>::class.java, Array<String>::class.java, String::class.java)
            method.isAccessible = true // Breaks the private lock
            method.invoke(null, arrayOf("sh", "-c", command), null, null)
        } catch (e: Exception) {
            Log.e("ShizukuEngine", "Failed to execute Shizuku command: ${e.message}")
            e.printStackTrace()
        }
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
        
        executeShellCommand(command)
        Log.d("ShizukuEngine", "Zero-Lag Video Recording Started.")
    }

    fun stopRecording() {
        // --- GRACEFUL SAVE (CRITICAL) ---
        // 'killall -2' prevents MP4 corruption. It tells the binary to safely pack the header.
        val killCommand = "killall -2 screenrecord"
        executeShellCommand(killCommand)
        Log.d("ShizukuEngine", "Video gracefully saved to $tempVideoPath")
    }
}
