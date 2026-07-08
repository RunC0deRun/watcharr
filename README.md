# Watcharr

Watcharr is a modern, lightweight, and performant native IPTV player and EPG mapping client tailored for Android (Mobile), Android TV, and Android Auto / Android Automotive OS. It features a unified Kotlin Multiplatform-ready architecture with separate optimized UIs for each form factor.

---

## Generative AI Usage

Human writer here. This application has almost solely been written by Gemini Flash 3.5. It is a personal experiment in producing an app using generate AI. Decide for yourself if you are okay with this development pattern when deciding whether you want to use this app.

---

## 🚀 Features

### 📺 Customized Native Interfaces
*   **Android TV Dashboard**: Custom-designed TV UI with featured Hero Carousel, interactive "Now Live" program grid, expandable sidebar navigation, and focus-aware overlays.
*   **Mobile Experience**: Immersive widescreen and portrait layouts featuring full-screen gestural player controls, rapid list traversal, and quick channel configuration.

### ⚡ Headless Playback Engine
*   **Media3 ExoPlayer Integration**: Reliable HLS/DASH streaming capabilities.
*   **DASH Encryption**: Out-of-the-box support for encrypted streams.
*   **Wake Prevention**: Automatic screen-sleep prevention during live media playback.

### 📅 Advanced EPG & Parsing Engine
*   **M3U Playlist Parser**: High-performance download-first staging loader to parse local or remote playlist files safely.
*   **48-Hour Rolling EPG Cache**: Room-backed database caching EPG data with scheduled background updates (`WorkManager`).
*   **Fuzzy EPG Mapping**: Automated matching algorithm connecting channel lists with imported XMLTV/EPG program guides.

### 🚗 Automotive Integration
*   **Android Auto & AAOS Support**: Integration with car restriction managers and template-driven layouts for safe in-vehicle playback.

---

## 🛠 Project Architecture

The codebase follows a modular design split into separate logical layers:

```
watcharr/
├── shared/          # Core model logic, parsers, DB/caching, and media engines
├── mobile/          # Jetpack Compose mobile application and phone/tablet layouts
└── tv/              # Android TV leanback-optimized Compose UI and setup wizard
```

---

## 📖 Documentation

*   [Stream Metadata Guidelines](docs/STREAM_METADATA.md): Detailed information on structuring HLS/DASH manifest tags and media elementary stream properties so that Watcharr can display correct and complete stream statistics.

---

## 📦 Building and Running

### Prerequisites
*   **JDK 21** or later (recommend the runtime bundled inside Android Studio).
*   **Android SDK 35+** (Targets Android 17 / SDK 37).

### Build APK
To build the debug APKs for both TV and Mobile modules, run:
```bash
./gradlew assembleDebug
```
The resulting APKs will be located at:
*   **Mobile**: `mobile/build/outputs/apk/debug/mobile-debug.apk`
*   **TV**: `tv/build/outputs/apk/debug/tv-debug.apk`

### Clean Build Directory
```bash
./gradlew clean
```

---

## 🧪 Testing

To run the unit tests across the shared module:
```bash
./gradlew test
```

A GitHub Action is configured in this repository to automatically run the test suite upon opening Pull Requests.

---

## 🤖 Contributing

This project is co-developed with **Gemini**. When committing changes, please ensure that you add the appropriate co-author attribution to your commit messages:

```
Co-authored-by: Gemini <gemini@google.com>
```

---

## 📄 License

This project is licensed under the terms of the LICENSE file.

