package com.pln_llm

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.pln_llm.config.Config
import com.pln_llm.viewmodel.MainViewModel
import android.widget.ScrollView

class MainActivity : AppCompatActivity() {
    private val viewModel: MainViewModel by viewModels()
    private lateinit var recordButton: Button
    private lateinit var statusText: TextView
    private lateinit var transcribedText: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var scrollView: ScrollView

    companion object {
        private const val PERMISSION_REQUEST_CODE = 123
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize Config
        Config.init(this)

        recordButton = findViewById(R.id.recordButton)
        statusText = findViewById(R.id.statusText)
        transcribedText = findViewById(R.id.transcribedText)
        progressBar = findViewById(R.id.progressBar)
        scrollView = findViewById(R.id.scrollView)
        scrollView.visibility = View.GONE

        setupObservers()
        setupClickListeners()
        checkPermissions()
    }

    private fun setupObservers() {
        viewModel.status.observe(this) { status ->
            statusText.text = status
            when (status) {
                "Recording..." -> {
                    progressBar.visibility = View.GONE
                    recordButton.text = "Stop"
                    scrollView.visibility = View.GONE
                    transcribedText.text = ""
                }
                "Processing..." -> {
                    progressBar.visibility = View.VISIBLE
                    recordButton.text = "Record"
                }
                else -> {
                    progressBar.visibility = View.GONE
                    recordButton.text = "Record"
                }
            }
        }
        
        viewModel.transcribedText.observe(this) { text ->
            transcribedText.text = text
            if (text.isNotEmpty()) {
                scrollView.visibility = View.VISIBLE
                progressBar.visibility = View.GONE
            } else {
                scrollView.visibility = View.GONE
            }
        }

        viewModel.isProcessing.observe(this) { isProcessing ->
            progressBar.visibility = if (isProcessing) View.VISIBLE else View.GONE
            recordButton.isEnabled = !isProcessing
        }
    }

    private fun setupClickListeners() {
        recordButton.setOnClickListener {
            if (checkPermissions()) {
                viewModel.toggleRecording(this)
            } else {
                requestPermissions()
            }
        }
    }

    private fun checkPermissions(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED &&
        ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermissions() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ),
            PERMISSION_REQUEST_CODE
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                viewModel.toggleRecording(this)
            } else {
                Toast.makeText(
                    this,
                    "Permissions are required to record audio",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}