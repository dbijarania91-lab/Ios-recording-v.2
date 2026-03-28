package com.example.nothingrecorder

import android.content.Context
import android.media.*
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import java.io.File
import java.nio.ByteBuffer

class Stitcher(private val context: Context) {

    fun muxVideoAndAudio(videoPath: String, audioPath: String) {
        Thread {
            try {
                // Wait 2.5s for the native Linux kernel to safely write the MP4 header
                Thread.sleep(2500) 
                
                val finalPath = "/sdcard/Movies/HEX_Record_${System.currentTimeMillis()}.mp4"
                val muxer = MediaMuxer(finalPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)

                val videoExtractor = MediaExtractor().apply { setDataSource(videoPath) }
                var videoTrackIndex = -1
                for (i in 0 until videoExtractor.trackCount) {
                    val format = videoExtractor.getTrackFormat(i)
                    if (format.getString(MediaFormat.KEY_MIME)?.startsWith("video/") == true) {
                        videoExtractor.selectTrack(i)
                        videoTrackIndex = muxer.addTrack(format)
                        break
                    }
                }

                val audioExtractor = MediaExtractor().apply { setDataSource(audioPath) }
                var audioTrackIndex = -1
                for (i in 0 until audioExtractor.trackCount) {
                    val format = audioExtractor.getTrackFormat(i)
                    if (format.getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true) {
                        audioExtractor.selectTrack(i)
                        audioTrackIndex = muxer.addTrack(format)
                        break
                    }
                }

                muxer.start()

                val buffer = ByteBuffer.allocate(1024 * 1024 * 5)
                val bufferInfo = MediaCodec.BufferInfo()

                if (videoTrackIndex != -1) {
                    while (true) {
                        val chunkSize = videoExtractor.readSampleData(buffer, 0)
                        if (chunkSize < 0) break
                        bufferInfo.offset = 0; bufferInfo.size = chunkSize
                        bufferInfo.presentationTimeUs = videoExtractor.sampleTime
                        bufferInfo.flags = videoExtractor.sampleFlags
                        muxer.writeSampleData(videoTrackIndex, buffer, bufferInfo)
                        videoExtractor.advance()
                    }
                }

                if (audioTrackIndex != -1) {
                    while (true) {
                        val chunkSize = audioExtractor.readSampleData(buffer, 0)
                        if (chunkSize < 0) break
                        bufferInfo.offset = 0; bufferInfo.size = chunkSize
                        bufferInfo.presentationTimeUs = audioExtractor.sampleTime
                        bufferInfo.flags = audioExtractor.sampleFlags
                        muxer.writeSampleData(audioTrackIndex, buffer, bufferInfo)
                        audioExtractor.advance()
                    }
                }

                muxer.stop(); muxer.release()
                videoExtractor.release(); audioExtractor.release()

                // Wipe the temporary engine files
                File(videoPath).delete()
                File(audioPath).delete()

                MediaScannerConnection.scanFile(context, arrayOf(finalPath), arrayOf("video/mp4")) { path, _ ->
                    Handler(Looper.getMainLooper()).post {
                        Toast.makeText(context, "HΞX Clip Saved: $path", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }
}
