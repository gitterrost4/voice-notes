# Voice Notes App

A personal Android app to organize audio recordings with categories and completion tracking.

## Features

- **Audio Recording**: One-tap circular recording button with visual feedback
- **Custom Categories**: Fully customizable recording categories via settings
- **Completion Tracking**: Mark recordings as done with visual indicators
- **Dark Theme**: Modern dark UI optimized for readability
- **Collapsible Sections**: Organized active vs completed recordings
- **Duration Display**: Shows recording length for each item
- **Persistent Storage**: All recordings and settings saved locally

## Technical Details

- **Language**: Java with modern features (streams, Optional, LocalDateTime)
- **Architecture**: Clean separation with adapters and data models
- **Storage**: JSON-based persistence with Gson
- **Audio**: MediaRecorder/MediaPlayer for recording and playback
- **UI**: Material Design with RecyclerView and custom drawables

## Default Categories

- ToDos
- Reminders  
- Town Meeting

Categories can be fully customized through the Settings menu.

## Requirements

- Android 7.0+ (API 24)
- Audio recording permission
- Storage permission for saving recordings

## Built With

- Android SDK 34
- Gradle 8.4
- Gson for JSON serialization
- AndroidX libraries