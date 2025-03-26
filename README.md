# PLN SmartView Android App

A modern Android application that provides voice recording and transcription capabilities using OpenAI's API. The app allows users to record audio, transcribe it, and process the text using advanced language models.

## Features

- Voice recording with real-time status updates
- Audio transcription using OpenAI's API
- Modern Material Design UI
- Progress tracking and status indicators
- Permission handling for audio recording and storage
- Secure API key management

## Technical Stack

- **Language**: Kotlin
- **Minimum SDK**: 26 (Android 8.0)
- **Target SDK**: 34
- **Architecture**: MVVM (Model-View-ViewModel)
- **Key Libraries**:
  - Retrofit for network calls
  - Coroutines for asynchronous operations
  - ViewModel and LiveData for UI state management
  - OkHttp for network communication
  - Material Design components

## Project Setup

1. Clone the repository:
   ```bash
   git clone https://github.com/ekkirinaldi/pln_smartview.git
   ```

2. Open the project in Android Studio

3. Build and run the project

## Configuration

### Setting up the OpenAI API Key

The app requires an OpenAI API key to function. Here's how to set it up:

1. When you first run the app, it will create a `config.properties` file in the app's external storage directory
2. Navigate to the app's external storage directory:
   - On Android: `/storage/emulated/0/Android/data/com.pln_llm/files/`
   - You can access this using a file manager app or by connecting your device to a computer
3. Open the `config.properties` file
4. Replace `your_api_key_here` with your actual OpenAI API key:
   ```
   OPENAI_API_KEY=your_actual_api_key_here
   ```

### Required Permissions

The app requires the following permissions:
- `RECORD_AUDIO`: For voice recording
- `WRITE_EXTERNAL_STORAGE`: For saving configuration files

These permissions will be requested when you first try to record audio.

## Project Structure

```
app/
├── src/
│   └── main/
│       ├── java/com/pln_llm/
│       │   ├── MainActivity.kt          # Main UI and user interaction
│       │   ├── api/
│       │   │   ├── LocalLLMApi.kt      # Local LLM API implementation
│       │   │   └── OpenAIApi.kt        # OpenAI API implementation
│       │   ├── config/
│       │   │   └── Config.kt           # Configuration management
│       │   └── viewmodel/
│       │       └── MainViewModel.kt     # UI state management
│       └── res/
│           └── layout/
│               └── activity_main.xml    # Main UI layout
```

## Usage

1. Launch the app
2. Grant the required permissions when prompted
3. Tap the "Record" button to start recording
4. Speak into your device's microphone
5. Tap "Stop" to end recording
6. Wait for the transcription to complete
7. The transcribed text will appear in the text view

## Security Notes

- The API key is stored in a separate configuration file outside the app's main directory
- The configuration file is created only when needed and is not included in the git repository
- Make sure to keep your API key secure and never share it publicly

## Contributing

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## License

This project is licensed under the MIT License - see the LICENSE file for details. 