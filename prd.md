# Smart Glass Audio Processing Application - Requirements

## Overview
The application will be simplified to handle a single API endpoint that processes audio input and returns both audio and text responses. This streamlines the current multi-step pipeline into a single API call.

## Functional Requirements

### 1. Audio Recording
- Users can start/stop audio recording using a button
- Recorded audio is saved in WAV format
- Visual feedback is provided during recording (recording indicator)
- Maximum recording duration: 30 seconds

### 2. Audio Processing
- Recorded audio is sent to the API endpoint: `https://dev-search.air.id/api/smart/process-audio`
- API request format:
  - Method: POST
  - Content-Type: multipart/form-data
  - Parameters:
    - `audio_file`: WAV audio file
    - `model_type`: "pln" (fixed value)

### 3. Response Handling
- API returns:
  - `audio`: WAV file containing the response
  - `llm_result`: String containing the text response
- The application should:
  - Display the text response (`llm_result`)
  - Automatically play the audio response
  - Show appropriate loading states during processing

### 4. Error Handling
- Handle network errors gracefully
- Show appropriate error messages to users
- Allow retry on failure
- Validate audio file size and format before sending

### 5. UI Requirements
- Keep existing recording button and status display
- Add loading indicator during processing
- Display text response in a scrollable view
- Show error messages when needed
- Maintain current visual design

## Technical Requirements

### 1. API Integration
- Use Retrofit for API calls
- Implement proper error handling
- Handle multipart form data for file upload
- Process binary audio response

### 2. Audio Handling
- Record audio in WAV format
- Play received WAV response
- Handle audio permissions
- Manage audio resources properly

### 3. Performance
- Optimize file size before upload
- Handle large audio responses efficiently
- Implement proper cleanup of resources

### 4. Security
- Handle API authentication if required
- Secure storage of API credentials
- Validate input/output data

## Success Criteria
1. Successful recording and processing of audio
2. Accurate display of text response
3. Proper playback of audio response
4. Smooth user experience with appropriate feedback
5. Proper error handling and recovery
