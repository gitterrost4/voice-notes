# Voice Notes

A privacy-focused Android app for organizing audio recordings and text notes with categories, completion tracking, and optional speech transcription.

## 🎯 Core Features

### 📱 **Easy Recording & Notes**
- **One-tap audio recording** with easily accessible button
- **Quick text notes** for situations where audio isn't practical  
- **Mixed timeline** displaying audio and text notes chronologically together
- **Pause/resume playback** for audio notes

### 🗂️ **Organization & Management**
- **Custom categories** - fully customizable via drag-and-drop settings
- **Completion tracking** - mark notes as done
- **Edit functionality** - modify text notes after creation

### 🔒 **Privacy-First Design**
- **Local storage only** - all data stays on your device
- **No analytics or tracking** - zero data collection
- **No cloud dependencies** - works completely offline (except if you choose to use audio transcription)
- **Optional transcription** - only if you choose to configure it

## 🌟 Advanced Features

### 🎤 **Speech Transcription** (Optional)
- **Multi-language support** - 14 languages including German, English, French, Spanish
- **Live transcription** - see text previews of your audio recordings
- **Batch processing** - transcribe existing recordings at once
- **Google Cloud integration** - uses professional-grade Speech-to-Text API

### ⚙️ **Customization**
- **Dark theme** with Material Design
- **Flexible organization** - create categories that match your workflow
- **Drag-and-drop categories** - reorder to your preference

## 📱 Default Categories

- **ToDos** - Task reminders and action items
- **Reminders** - Personal notes and memory aids
- **Town Meeting** - Meeting notes and community discussions

*All categories can be renamed, reordered, or replaced through the Settings menu.*

## 🚀 Getting Started

### Requirements
- **Android 7.0+** (API level 24)
- **Audio recording permission** - for voice notes
- **Storage permission** - for saving recordings to external storage

### Installation
1. Download the APK from releases
2. Enable "Install from unknown sources" in Android settings
3. Install and grant required permissions
4. Start recording your first voice note!

## 🔧 Optional: Setting Up Speech Transcription

The app works perfectly without transcription, but if you want text previews of your audio recordings:

### Step-by-Step Setup

1. **Create Google Cloud Account**
   - Go to [Google Cloud Console](https://console.cloud.google.com)
   - Create a new project (or use existing)

2. **Enable Speech-to-Text API**
   - In the Console, go to "APIs & Services" → "Library"
   - Search for "Cloud Speech-to-Text API"
   - Click "Enable"
   - *You will need to enable billing for this step. If you stay below the 60 minutes of transcription per month, you will not be billed anything. Note that we do not have anything to do with this service. This is completely on Google's side. We just provide a way to use the API within the app.*

3. **Create Service Account**
   - Go to "IAM & Admin" → "Service Accounts"
   - Click "Create Service Account"
   - Name: `voice-notes-service`
   - Role: `Cloud Speech Client`

4. **Generate Credentials**
   - Click on your new service account
   - Go to "Keys" tab → "Add Key" → "Create new key"
   - Choose "JSON" format
   - Download the JSON file

5. **Configure in App**
   - Open Voice Notes → Settings
   - Select your preferred transcription language
   - Paste the JSON content into the credentials field
   - Tap "Save Credentials"

### 💰 Pricing (as of writing this readme. Changes are always possible)
- **Free Tier**: 60 minutes of transcription per month
- **Paid**: $0.024 per minute after free tier
- **Your Control**: Only active when you configure credentials

## 🏗️ Technical Details

### Built With
- **Android SDK 34** (targeting latest Android)
- **Minimum SDK 24** (Android 7.0+ support)
- **Java** with modern features (streams, Optional, LocalDateTime)
- **Gradle 8.4** with Android Gradle Plugin 8.1.4

### Dependencies
- **Gson** - JSON serialization for data persistence
- **AndroidX** - Modern Android development libraries
- **OkHttp** - HTTP client for Google Cloud API calls
- **Google Auth** - OAuth2 authentication for Cloud services

### Architecture
- **Clean separation** - adapters, data models, storage layers
- **JSON persistence** - lightweight local data storage
- **MediaRecorder/MediaPlayer** - native Android audio handling
- **SharedPreferences** - settings and user preferences

### Data Storage
- **Audio files**: `.3gp` format in `getExternalFilesDir()/recordings/`
- **Notes metadata**: JSON in app internal storage
- **Settings**: Android SharedPreferences
- **No cloud storage** - everything stays local

## 🛡️ Privacy & Security

- ✅ **Zero data collection** - we don't track anything
- ✅ **Local-first** - all data stays on your device  
- ✅ **No analytics** - no crash reporting or usage metrics
- ✅ **Optional cloud** - transcription only if you choose
- ✅ **Transparent** - open source code available for review

## 📥 Download

[![Download APK](https://img.shields.io/badge/Download-APK-green?style=for-the-badge)](https://github.com/gitterrost4/voice-notes/releases/latest/download/voice-notes-1.0.0.apk)
    
**Latest Version**: v1.0.0 | [View All Releases](https://github.com/gitterrost4/voice-notes/releases)

## 📄 Legal

- **Privacy Policy**: [View Privacy Policy](privacy_policy.md)
- **Package**: `de.gitterrost4.voicenotes`
- **Developer**: Paul Kramer
- **Contact**: gitterrost4.apps@gmail.com

## 🔄 Version History

- **v0.1** - Initial release with core recording and organization features
- **v0.2** - Added text notes and completion tracking
- **v0.3** - Speech transcription with multi-language support
- **v0.4** - UI polish and settings improvements

---

*Voice Notes - Simple, private, powerful voice organization for Android.*
