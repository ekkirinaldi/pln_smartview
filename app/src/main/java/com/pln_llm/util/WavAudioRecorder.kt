package com.pln_llm.util

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder

class WavAudioRecorder(
    private val outputFile: File,
    private val sampleRate: Int = 16000,
    private val channelConfig: Int = AudioFormat.CHANNEL_IN_MONO,
    private val audioFormat: Int = AudioFormat.ENCODING_PCM_16BIT
) {
    private var audioRecord: AudioRecord? = null
    private var isRecording = false
    private var recordingThread: Thread? = null
    private var bufferSize: Int = 0

    fun startRecording() {
        bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate,
            channelConfig,
            audioFormat,
            bufferSize
        )
        audioRecord?.startRecording()
        isRecording = true
        recordingThread = Thread({ writeAudioDataToFile() }, "AudioRecorder Thread")
        recordingThread?.start()
    }

    fun stopRecording() {
        isRecording = false
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
        recordingThread = null
        // After recording, write the WAV header
        writeWavHeader()
    }

    private fun writeAudioDataToFile() {
        val pcmFile = File(outputFile.absolutePath + ".pcm")
        FileOutputStream(pcmFile).use { os ->
            val buffer = ByteArray(bufferSize)
            while (isRecording) {
                val read = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                if (read > 0) {
                    os.write(buffer, 0, read)
                }
            }
        }
    }

    private fun writeWavHeader() {
        val pcmFile = File(outputFile.absolutePath + ".pcm")
        val pcmData = pcmFile.readBytes()
        val totalAudioLen = pcmData.size
        val totalDataLen = totalAudioLen + 36
        val channels = if (channelConfig == AudioFormat.CHANNEL_IN_MONO) 1 else 2
        val byteRate = 16 * sampleRate * channels / 8

        FileOutputStream(outputFile).use { out ->
            // Write WAV header
            out.write("RIFF".toByteArray())
            out.write(intToByteArray(totalDataLen), 0, 4)
            out.write("WAVE".toByteArray())
            out.write("fmt ".toByteArray())
            out.write(intToByteArray(16), 0, 4) // Subchunk1Size
            out.write(shortToByteArray(1), 0, 2) // AudioFormat (1 = PCM)
            out.write(shortToByteArray(channels.toShort()), 0, 2)
            out.write(intToByteArray(sampleRate), 0, 4)
            out.write(intToByteArray(byteRate), 0, 4)
            out.write(shortToByteArray((channels * 16 / 8).toShort()), 0, 2) // BlockAlign
            out.write(shortToByteArray(16), 0, 2) // BitsPerSample
            out.write("data".toByteArray())
            out.write(intToByteArray(totalAudioLen), 0, 4)
            // Write PCM data
            out.write(pcmData)
        }
        pcmFile.delete()
    }

    private fun intToByteArray(value: Int): ByteArray {
        return ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(value).array()
    }

    private fun shortToByteArray(value: Short): ByteArray {
        return ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN).putShort(value).array()
    }
} 