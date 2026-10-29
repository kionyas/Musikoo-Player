<div align="center">

# 🎧 Musikoo

**A local music player for Android with Material 3 Expressive design, word-synced lyrics and a fully customizable home screen.**

![Platform](https://img.shields.io/badge/platform-Android-3DDC84?logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-Jetpack%20Compose-7F52FF?logo=kotlin&logoColor=white)
![Media3](https://img.shields.io/badge/audio-Media3%20ExoPlayer-orange)
![License](https://img.shields.io/badge/license-MIT-blue)
[![Latest release](https://img.shields.io/github/v/release/kionyas/Musikoo-Player?label=release)](https://github.com/kionyas/Musikoo-Player/releases/latest)

[Download](https://github.com/kionyas/Musikoo-Player/releases/latest) · [Features](#-features) · [Screenshots](#-screenshots) · [Build](#-build-from-source)

</div>

---

## ✨ About

Musikoo plays the music that is already on your phone: no streaming accounts, no ads. It is built with Jetpack Compose and leans on expressive Material 3 shapes, wavy progress sliders, album-art-driven colors and smooth motion everywhere.

## 🚀 Features

### 🎵 Library
- Browse by **Albums, Artists, Genres, Songs, Playlists** and **Pins**
- Playlists, Pins, Swipes, sooooooooo many gestures and smoothness 

### ▶️ Player
- Media3 / ExoPlayer playback with a system media notification and lock-screen controls
- Mini player with **configurable swipe gestures**: favorite, next, previous, play/pause, or hide
- Full player with a **M3E** and some more features 
- Album-art backdrop and colors extracted from the cover

### 📝 Lyrics
- **Word-synced lyrics** from resources with fallback to [LRCLIB](https://lrclib.net) and [lrcmux](https://lrcmux.dev/)
- Blured recognizable style :D

### 🏷️ Tag editor
- Edit tags right in the app: title, artist, album, album artist, composer, lyricist, genre, year, track, disc, comment

### 📊 Statistics & Last.fm
- Local listening stats for **7 days / 30 days / 1 year / all time**: time listened, plays, top artists
- **Last.fm** login, scrobbling and Last.fm stats

### 🎛️ Customizable bottom bar
Make the home screen yours: **Settings → Interface → Bottom Bar**.
- Show or hide the **search bar**
- Add, remove and **drag to reorder** items: Search, Artists, Songs, Albums, Genres, Random, Pins, Playlists
- Add a **shortcut straight to a genre**
- The bar scrolls sideways when it has more items than fit
- *Reset to Default* whenever you want

### 🎨 Appearance
- UI styles: **Classic** and **Expensive**
- Shape families: **Material 3** and **Material 3 Expressive**
- **Monet** dynamic colors, light / dark / system theme
- Fonts: system default or **Lora**
- Switchable **app icons**: black & white, white & black, Monet

### 🧩 More
- Home screen **widgets** (built with Glance), two sizes
- Share tracks, delete files from the device
- **In-app updates** from GitHub Releases

## 📸 Screenshots

<div align="center">

| Home | Player & lyrics | Genres |
<img width="1703" height="1100" alt="collage-2" src="https://github.com/user-attachments/assets/1cff744a-5e9b-4f09-8efc-0bfa0451d97a" /> 

| Statistics | Bottom bar setup | Settings |
<img width="1703" height="1100" alt="collage2" src="https://github.com/user-attachments/assets/256b07ef-1924-4bab-9a61-e22d11ab40f1" /> 

</div>

## 📥 Installation

1. Open the [latest release](https://github.com/kionyas/Musikoo-Player/releases/latest).
2. Download the `.apk` and install it. Android may ask you to allow installs from your browser or file manager.
3. On first launch grant access to audio files (and notifications on Android 13+).

Later updates can be installed from inside the app (*Settings → Download Manager*).

## 🔐 Permissions

| Permission | Why |
|---|---|
| `READ_MEDIA_AUDIO` / `READ_EXTERNAL_STORAGE` | Scan your music library |
| `WRITE_EXTERNAL_STORAGE` (Android 9 and below) | Tag editing on old devices |
| `INTERNET`, `ACCESS_NETWORK_STATE` | Lyrics, Last.fm, update check |
| `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_MEDIA_PLAYBACK`, `WAKE_LOCK` | Background playback |
| `POST_NOTIFICATIONS` | Playback notification |
| `REQUEST_INSTALL_PACKAGES` | Hand a downloaded update to the system installer |

## 🛠 Tech stack

- **Kotlin** + **Jetpack Compose**
- **AndroidX Media3** (ExoPlayer, MediaSession)
- **JAudioTagger** for tag reading and writing
- **LRCLIB**, **lrcmux** and **Last.fm** APIs

## 🏗 Build from source

```bash
git clone https://github.com/kionyas/Musikoo-Player.git
cd Musikoo-Player
```

Open the project in **Android Studio**, let Gradle sync, then run the app on a device or emulator.

## 🗂 Project structure

```
com.reikoonya.musikoo
├── data/        scanner, database, playlists, art cache
├── library/     library view model
├── lyrics/      LRCLIB / KuGou clients and repository
├── scrobble/    Last.fm auth, scrobbler, stats
├── service/     media playback service
├── navigation/  screens and navigator
├── ui/          screens, bottom bar, player, settings
│   ├── components/  reusable pieces (wavy slider, glass, pills…)
│   └── theme/       colors, Monet, typography, UI styles
├── util/        player manager, tags, prefs, updater
└── widget/      home screen widgets
```

## 🤝 Contributing

Issues and pull requests are welcome. If you found a bug, please attach your Android version and device model.

## 📄 License

Released under the [MIT License](LICENSE).
