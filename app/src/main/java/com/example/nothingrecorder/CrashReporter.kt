package com.example.nothingrecorder

import android.os.Environment
import android.util.Log
import java.io.File
import java.io.FileWriter
import java.io.PrintWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CrashReporter : Thread.UncaughtExceptionHandler {
    private val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()

    override fun uncaughtException(thread: Thread, exception: Throwable) {
        try {
            val timestamp = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault()).format(Date())
            val fileName = "HEX_CrashLog_$timestamp.txt"
            
            // Saves the crash log to your phone's Documents folder
            val file = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), fileName)
            
            val writer = FileWriter(file, true)
            val printWriter = PrintWriter(writer)
            
            printWriter.println("--- HΞX ENGINE FATAL CRASH LOG ---")
            printWriter.println("Time: $timestamp")
            printWriter.println("Thread: ${thread.name}")
            printWriter.println("Message: ${exception.message}")
            printWriter.println("Stacktrace:")
            exception.printStackTrace(printWriter)
            
            printWriter.flush()
            printWriter.close()
            writer.close()
            Log.e("HexCrash", "Crash saved to ${file.absolutePath}")
        } catch (e: Exception) {
            Log.e("HexCrash", "Failed to save crash log", e)
        }
        
        // Let the app crash normally after saving the log
        defaultHandler?.uncaughtException(thread, exception)
    }

    companion object {
        fun init() {
            Thread.setDefaultUncaughtExceptionHandler(CrashReporter())
        }
    }
}
