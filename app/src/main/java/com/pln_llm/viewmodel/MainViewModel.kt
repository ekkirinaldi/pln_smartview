package com.pln_llm.viewmodel

import android.content.Context
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import android.os.Environment
import android.widget.Toast
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pln_llm.api.SmartGlassApi
import com.pln_llm.api.SmartGlassResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import java.io.FileOutputStream
import com.google.gson.GsonBuilder
import com.pln_llm.util.WavAudioRecorder
import android.util.Log
import org.json.JSONObject
import android.util.Base64

class MainViewModel : ViewModel() {
    private val _status = MutableLiveData<String>()
    val status: LiveData<String> = _status

    private val _transcribedText = MutableLiveData<String>()
    val transcribedText: LiveData<String> = _transcribedText

    private val _isProcessing = MutableLiveData<Boolean>()
    val isProcessing: LiveData<Boolean> = _isProcessing

    private var mediaRecorder: MediaRecorder? = null
    private var mediaPlayer: MediaPlayer? = null
    private var audioFile: File? = null
    private var isRecording = false
    private var context: Context? = null
    private var wavAudioRecorder: WavAudioRecorder? = null

    private val smartGlassApi: SmartGlassApi by lazy {
        val client = OkHttpClient.Builder()
            .build()

        val gson = GsonBuilder()
            .setLenient()
            .create()

        Retrofit.Builder()
            .baseUrl("https://dev-search.air.id/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(SmartGlassApi::class.java)
    }

    fun toggleRecording(context: Context) {
        this.context = context
        if (!isRecording) {
            startRecording(context)
        } else {
            stopRecording()
        }
    }

    private fun showToast(message: String) {
        context?.let {
            Toast.makeText(it, message, Toast.LENGTH_SHORT).show()
        }
    }

    private fun startRecording(context: Context) {
        try {
            audioFile = File(
                context.getExternalFilesDir(Environment.DIRECTORY_MUSIC),
                "audio_record_${System.currentTimeMillis()}.wav"
            )
            Log.d(TAG, "Recording to file: ${audioFile?.absolutePath}")
            wavAudioRecorder = WavAudioRecorder(audioFile!!)
            wavAudioRecorder?.startRecording()
            isRecording = true
            _status.value = "Recording..."
            _transcribedText.value = ""
            _isProcessing.value = false
            showToast("Recording started")
        } catch (e: Exception) {
            Log.e(TAG, "Error starting recording", e)
            _status.value = "Error starting recording: ${e.message}"
            showToast("Error starting recording: ${e.message}")
        }
    }

    private fun stopRecording() {
        try {
            wavAudioRecorder?.stopRecording()
            Log.d(TAG, "Stopped recording. File: ${audioFile?.absolutePath}")
            wavAudioRecorder = null
            isRecording = false
            _status.value = "Processing..."
            _isProcessing.value = true
            showToast("Recording stopped, processing audio...")
            processAudio()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping recording", e)
            _status.value = "Error stopping recording: ${e.message}"
            _isProcessing.value = false
            showToast("Error stopping recording: ${e.message}")
        }
    }

    private fun processAudio() {
        viewModelScope.launch {
            try {
                audioFile?.let { file ->
                    Log.d(TAG, "Preparing to upload file: ${file.absolutePath}")
                    val fileSizeKB = file.length() / 1024
                    Log.d(TAG, "WAV file size: $fileSizeKB KB")
                    if (fileSizeKB < 1 || fileSizeKB > 10000) {
                        showToast("Warning: Audio file size: $fileSizeKB KB")
                    }
                    // Create request parts using PartMap for text fields
                    val params = mutableMapOf<String, okhttp3.RequestBody>()
                    params["model_type"] = "pln".toRequestBody("text/plain".toMediaType())
                    val requestFile = file.asRequestBody("audio/wav".toMediaType())
                    val filePart = MultipartBody.Part.createFormData("audio_file", file.name, requestFile)
                    Log.d(TAG, "Uploading audio to API with PartMap...")
                    val response = withContext(Dispatchers.IO) {
                        try {
                            smartGlassApi.processAudio(
                                params = params,
                                audio_file = filePart
                            )
                        } catch (e: Exception) {
                            Log.e(TAG, "Network/API error", e)
                            withContext(Dispatchers.Main) {
                                _status.value = "Network error: ${e.message}"
                            }
                            throw e
                        }
                    }
                    Log.d(TAG, "API response code: ${response.code()}")
                    if (response.isSuccessful) {
                        val responseBody = response.body()
                        if (responseBody == null) {
                            Log.e(TAG, "API returned empty response body")
                            withContext(Dispatchers.Main) {
                                _status.value = "Error: Empty response"
                            }
                            return@let
                        }
                        val responseString = responseBody.string()
                        Log.d(TAG, "API response string: $responseString")
                        val json = JSONObject(responseString)
                        val llmResult = json.optString("llm_result", "")
                        val audioBase64 = json.optString("audio", "")
                        if (llmResult.isNotEmpty()) {
                            withContext(Dispatchers.Main) {
                                _transcribedText.value = llmResult
                                _status.value = "Response received, playing audio..."
                            }
                        }
                        if (audioBase64.isNotEmpty()) {
                            val audioBytes = Base64.decode(audioBase64, Base64.DEFAULT)
                            playAudio(audioBytes)
                        } else {
                            Log.e(TAG, "No audio data in response")
                        }
                    } else {
                        val errorBody = response.errorBody()?.string()
                        Log.e(TAG, "API error: ${response.code()} - $errorBody")
                        withContext(Dispatchers.Main) {
                            _status.value = "Error: ${response.code()} - $errorBody"
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in processAudio", e)
                withContext(Dispatchers.Main) {
                    _status.value = "Error: ${e.message}"
                    _isProcessing.value = false
                }
            }
        }
    }

    private fun playAudio(audioData: ByteArray) {
        try {
            val tempFile = File.createTempFile("response_audio", ".wav")
            FileOutputStream(tempFile).use { it.write(audioData) }

            mediaPlayer?.release()
            mediaPlayer = MediaPlayer().apply {
                setDataSource(tempFile.absolutePath)
                prepare()
                start()
                setOnCompletionListener {
                    release()
                    tempFile.delete()
                    _status.value = "Ready to record"
                    _isProcessing.value = false
                    showToast("Response playback completed")
                }
            }
        } catch (e: Exception) {
            _status.value = "Error playing audio: ${e.message}"
            _isProcessing.value = false
            showToast("Error playing audio: ${e.message}")
        }
    }

    override fun onCleared() {
        super.onCleared()
        mediaRecorder?.release()
        mediaPlayer?.release()
    }

    companion object {
        private const val TAG = "MainViewModel"
    }
} 