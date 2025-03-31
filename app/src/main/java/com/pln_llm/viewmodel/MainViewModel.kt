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
import com.pln_llm.api.*
import com.pln_llm.config.Config
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import java.io.FileOutputStream
import com.google.gson.GsonBuilder
import okhttp3.ResponseBody

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

    private val openAIApi: OpenAIApi by lazy {
        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .addHeader("Authorization", "Bearer ${Config.openAiApiKey}")
                    .build()
                chain.proceed(request)
            }
            .build()

        val gson = GsonBuilder()
            .setLenient()
            .create()

        Retrofit.Builder()
            .baseUrl("https://api.openai.com/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(OpenAIApi::class.java)
    }

    private val localLLMApi: LocalLLMApi by lazy {
        val client = OkHttpClient.Builder()
            .build()

        val gson = GsonBuilder()
            .setLenient()
            .create()

        Retrofit.Builder()
            .baseUrl("http://192.168.1.11:8000/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(LocalLLMApi::class.java)
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
                "audio_record_${System.currentTimeMillis()}.m4a"
            )

            mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                MediaRecorder()
            }.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(audioFile?.absolutePath)
                prepare()
                start()
            }

            isRecording = true
            _status.value = "Recording..."
            _transcribedText.value = ""
            _isProcessing.value = false
            showToast("Recording started")
        } catch (e: Exception) {
            _status.value = "Error starting recording: ${e.message}"
            showToast("Error starting recording: ${e.message}")
        }
    }

    private fun stopRecording() {
        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
            mediaRecorder = null
            isRecording = false
            _status.value = "Processing..."
            _isProcessing.value = true
            showToast("Recording stopped, processing audio...")
            processAudio()
        } catch (e: Exception) {
            _status.value = "Error stopping recording: ${e.message}"
            _isProcessing.value = false
            showToast("Error stopping recording: ${e.message}")
        }
    }

    private fun processAudio() {
        viewModelScope.launch {
            try {
                audioFile?.let { file ->
                    _status.value = "Converting speech to text..."
                    
                    // Log file details only if it's very small or large
                    val fileSizeKB = file.length() / 1024
                    if (fileSizeKB < 1 || fileSizeKB > 1000) {
                        showToast("Warning: Audio file size: $fileSizeKB KB")
                    }
                    
                    // Create request parts
                    val requestFile = file.asRequestBody("audio/mpeg".toMediaType())
                    val filePart = MultipartBody.Part.createFormData("file", file.name, requestFile)
                    val modelPart = "whisper-1".toRequestBody("text/plain".toMediaType())
                    
                    val transcriptionResponse = withContext(Dispatchers.IO) {
                        try {
                            openAIApi.transcribeAudio(
                                file = filePart,
                                model = modelPart
                            )
                        } catch (e: Exception) {
                            withContext(Dispatchers.Main) {
                                _status.value = "Network error: ${e.message}"
                            }
                            throw e
                        }
                    }

                    if (transcriptionResponse.isSuccessful) {
                        val transcribedText = transcriptionResponse.body()?.text ?: ""
                        if (transcribedText.isBlank()) {
                            withContext(Dispatchers.Main) {
                                _status.value = "Error: Empty transcription"
                            }
                            return@let
                        }
                        withContext(Dispatchers.Main) {
                            _transcribedText.value = transcribedText
                            _status.value = "Transcribed: $transcribedText"
                        }

                        val localLLMRequest = LocalLLMRequest(content = transcribedText)

                        withContext(Dispatchers.Main) {
                            _status.value = "Getting response from Local LLM..."
                        }
                        
                        val llmResponse = withContext(Dispatchers.IO) {
                            try {
                                localLLMApi.getCompletion(localLLMRequest)
                            } catch (e: Exception) {
                                withContext(Dispatchers.Main) {
                                    _status.value = "Local LLM network error: ${e.message}"
                                }
                                throw e
                            }
                        }

                        if (llmResponse.isSuccessful) {
                            val responseBody = llmResponse.body()
                            if (responseBody == null) {
                                withContext(Dispatchers.Main) {
                                    _status.value = "Error: Empty response from Local LLM"
                                }
                                return@let
                            }
                            
                            val answer = responseBody.answer
                            val contexts = responseBody.relevant_contexts
                            
                            // Format the complete response with answer and contexts
                            val formattedResponse = buildString {
                                appendLine("Answer:")
                                appendLine(answer)
                                appendLine()
                                appendLine("Relevant Contexts:")
                                contexts.forEachIndexed { index, context ->
                                    appendLine("${index + 1}. $context")
                                }
                            }
                            
                            withContext(Dispatchers.Main) {
                                _status.value = "Local LLM Response:"
                                _transcribedText.value = formattedResponse
                            }

                            // Add a delay to let user read the response
                            delay(3000) // 3 seconds delay
                            
                            withContext(Dispatchers.Main) {
                                _status.value = "Converting to speech..."
                            }

                            // Use only the answer for text-to-speech, not the contexts
                            val ttsRequest = TextToSpeechRequest(input = answer)
                            val audioResponse = withContext(Dispatchers.IO) {
                                try {
                                    openAIApi.textToSpeech(ttsRequest)
                                } catch (e: Exception) {
                                    withContext(Dispatchers.Main) {
                                        _status.value = "TTS network error: ${e.message}"
                                    }
                                    throw e
                                }
                            }

                            if (audioResponse.isSuccessful) {
                                val responseBody = audioResponse.body()
                                if (responseBody == null) {
                                    withContext(Dispatchers.Main) {
                                        _status.value = "Error: Empty TTS response"
                                    }
                                    return@let
                                }
                                withContext(Dispatchers.Main) {
                                    _status.value = "Playing response..."
                                }
                                playAudio(responseBody.bytes())
                            } else {
                                val errorBody = audioResponse.errorBody()?.string()
                                withContext(Dispatchers.Main) {
                                    _status.value = "Text-to-speech failed: ${audioResponse.code()} - $errorBody"
                                }
                            }
                        } else {
                            val errorBody = llmResponse.errorBody()?.string()
                            withContext(Dispatchers.Main) {
                                _status.value = "Local LLM error: ${llmResponse.code()} - $errorBody"
                            }
                        }
                    } else {
                        val errorBody = transcriptionResponse.errorBody()?.string()
                        withContext(Dispatchers.Main) {
                            _status.value = "Transcription error: ${transcriptionResponse.code()} - $errorBody"
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _status.value = "Error: ${e.message}"
                }
            
                withContext(Dispatchers.Main) {
                    _isProcessing.value = false
                }
            }
        }
    }

    private fun playAudio(audioData: ByteArray) {
        try {
            val tempFile = File.createTempFile("tts_audio", ".mp3")
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
} 