package com.example.nothingrecorder

import android.annotation.SuppressLint
import android.media.*
import android.media.projection.MediaProjection
import android.os.Build

class InternalAudioEngine {
    val tempAudioPath = "/sdcard/Movies/HEX_TEMP_AUDIO.m4a"
    private var audioRecord: AudioRecord? = null
    private var codec: MediaCodec? = null
    private var muxer: MediaMuxer? = null
    @Volatile var isRecording = false

    @SuppressLint("MissingPermission")
    fun startRecording(mediaProjection: MediaProjection) {
        isRecording = true
        val sampleRate = 44100
        val bufferSize = AudioRecord.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_IN_STEREO, AudioFormat.ENCODING_PCM_16BIT) * 2

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val config = AudioPlaybackCaptureConfiguration.Builder(mediaProjection)
                .addMatchingUsage(AudioAttributes.USAGE_MEDIA)
                .addMatchingUsage(AudioAttributes.USAGE_GAME)
                .addMatchingUsage(AudioAttributes.USAGE_UNKNOWN).build()

            val format = AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setSampleRate(sampleRate).setChannelMask(AudioFormat.CHANNEL_IN_STEREO).build()

            audioRecord = AudioRecord.Builder().setAudioFormat(format)
                .setBufferSizeInBytes(bufferSize).setAudioPlaybackCaptureConfig(config).build()
        }

        val mediaFormat = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, sampleRate, 2).apply {
            setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
            setInteger(MediaFormat.KEY_BIT_RATE, 320000)
        }

        codec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC)
        codec!!.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        codec!!.start()

        muxer = MediaMuxer(tempAudioPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        audioRecord?.startRecording()
        Thread { encodeAudioLoop() }.start()
    }

    private fun encodeAudioLoop() {
        val bufferInfo = MediaCodec.BufferInfo()
        var audioTrackIndex = -1
        var muxerStarted = false
        var startTimeUs = -1L

        while (isRecording) {
            val inputIndex = codec!!.dequeueInputBuffer(10000)
            if (inputIndex >= 0) {
                val inputBuffer = codec!!.getInputBuffer(inputIndex)!!
                val read = audioRecord!!.read(inputBuffer, inputBuffer.capacity())
                if (read > 0) {
                    // --- THE NANOSECOND SYNC (CRITICAL) ---
                    // This forces the audio PTS to match the Shizuku video PTS clock perfectly.
                    val currentUs = System.nanoTime() / 1000
                    if (startTimeUs == -1L) startTimeUs = currentUs
                    codec!!.queueInputBuffer(inputIndex, 0, read, currentUs - startTimeUs, 0)
                }
            }

            var outputIndex = codec!!.dequeueOutputBuffer(bufferInfo, 10000)
            while (outputIndex >= 0) {
                if (outputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    audioTrackIndex = muxer!!.addTrack(codec!!.outputFormat)
                    muxer!!.start()
                    muxerStarted = true
                } else if (outputIndex >= 0) {
                    val encodedData = codec!!.getOutputBuffer(outputIndex)!!
                    if (muxerStarted && bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG == 0) {
                        muxer!!.writeSampleData(audioTrackIndex, encodedData, bufferInfo)
                    }
                    codec!!.releaseOutputBuffer(outputIndex, false)
                }
                outputIndex = codec!!.dequeueOutputBuffer(bufferInfo, 0)
            }
        }
        
        try {
            audioRecord?.stop(); audioRecord?.release()
            codec?.stop(); codec?.release()
            if (muxerStarted) muxer?.stop(); muxer?.release()
        } catch (e: Exception) { e.printStackTrace() }
    }

    fun stopRecording() { isRecording = false }
}
