# WordMate-Android-Vocabulary-App

WordMate is an Android application designed to help users learn and review English vocabulary through topic-based learning, flashcards, vocabulary games, and AI-assisted learning.

The project was developed as part of the Mobile Application Development course at the University of Information Technology – VNU-HCM.

## Overview

WordMate provides an interactive environment for learning English vocabulary.

The application supports:

- Topic-based vocabulary learning
- Vocabulary lookup through external APIs
- Flashcard-based review
- Vocabulary practice games
- Favorite words
- Learning progress tracking
- Learning reminders
- Google and Facebook authentication
- AI-assisted vocabulary explanations
- Light/Dark Mode
- Multilingual interface

## Key Features

### Authentication

- Register and log in using email and password
- Google Login
- Facebook Login
- User-specific learning data

### Vocabulary Learning

- Learn vocabulary by topic
- Display word meaning, pronunciation, examples, and part of speech
- Save words to favorites
- Set learning goals and reminders

### Vocabulary Search

- Search for English words using external dictionary APIs
- Retrieve pronunciation, meaning, and example sentences
- Provide word suggestions while searching

### Flashcards and Practice

- Review vocabulary using flashcards
- Practice vocabulary through interactive games
- Track learning progress

### AI Vocabulary Assistant

The application integrates Gemini API to support vocabulary learning by providing:

- Word explanations
- Contextual examples
- Personalized memory tips

### Personalization

- Light/Dark Mode
- Language settings
- Learning reminders
- Personal account settings

## Technologies

| Technology | Purpose |
|---|---|
| Java | Application development |
| Android Studio | Android development |
| Firebase Authentication | User authentication |
| Firebase Realtime Database | User data synchronization |
| Retrofit | REST API integration |
| Gemini API | AI-powered vocabulary assistance |
| SharedPreferences | Local data and user preferences |
| RecyclerView | Vocabulary list management |
| Fragments | Screen and navigation management |
| XML | User interface development |

## Application Workflow

```text
User
 |
 +-- Register / Login
 |
 v
Home
 |
 +-- Learn by Topic
 |     +-- Vocabulary
 |
 +-- Search
 |     +-- Dictionary API
 |
 +-- Flashcards
 |     +-- Review Vocabulary
 |
 +-- Practice Games
 |
 +-- Dashboard
 |     +-- Learning Progress
 |
 +-- Favorites
 |
 +-- Settings
       +-- Language
       +-- Dark Mode
       +-- Learning Reminder
```
## UI/UX
The application follows a simple and user-friendly interface designed for English learners.

Main screens include:

- Home
- Search
- Dashboard
- Favorite
- Settings
- Flashcards
- Vocabulary Games
The application supports both Light Mode and Dark Mode.

## Testing and Deployment

The main application workflows were tested, including:

- User authentication
- Vocabulary search
- API requests
- Firebase synchronization
- Flashcards
- Learning progress
- Application navigation
- 
The application was built as a Debug APK and tested on Android devices.

## Future Development

Potential improvements include:

- Expanding the vocabulary database to cover more specialized topics
- Developing an advanced AI chatbot
- Adding more diverse exercises and practice activities
- Providing more detailed learning statistics
- Improving data synchronization
- Developing an iOS version
## Project Structure

WordMate/
├── app/
│   ├── src/
│   │   └── main/
│   │       ├── java/
│   │       ├── res/
│   │       └── AndroidManifest.xml
│   └── build.gradle
├── gradle/
├── build.gradle
└── README.md


## Project Information

Course: Mobile Application Development

Project: WordMate – English Vocabulary Learning Application

Platform: Android

Language: Java
