# Main Menu & UI Redesign Implementation Plan

Redesign the main menu and app interface to be highly stylish, responsive, and feature-rich with a 3-column album grid, mini-player bar, bottom action pills, and dedicated Settings and About screens.

## User Review Required

> [!IMPORTANT]
> - **New Screens**: Creating [SettingsScreen.kt](file:///C:/AndroidStudioProjects/MyMD3Player/app/src/main/java/com/reikoonya/playeerok/ui/SettingsScreen.kt) and [AboutScreen.kt](file:///C:/AndroidStudioProjects/MyMD3Player/app/src/main/java/com/reikoonya/playeerok/ui/AboutScreen.kt) as separate files.
> - **Layout Structure**: 3-column album grid in the center, mini-player preview bar right above the bottom pill (`Artists` / `Songs` / `Randomize`), and top action buttons for Settings and About.

## Proposed Changes

### UI Components & Screens

#### [NEW] [SettingsScreen.kt](file:///C:/AndroidStudioProjects/MyMD3Player/app/src/main/java/com/reikoonya/playeerok/ui/SettingsScreen.kt)
- Dedicated settings screen with clean Material 3 design and back navigation.

#### [NEW] [AboutScreen.kt](file:///C:/AndroidStudioProjects/MyMD3Player/app/src/main/java/com/reikoonya/playeerok/ui/AboutScreen.kt)
- Dedicated "About App" screen displaying app info, version, and developer details.

#### [MODIFY] [MainActivity.kt](file:///C:/AndroidStudioProjects/MyMD3Player/app/src/main/java/com/reikoonya/playeerok/MainActivity.kt)
- Update `Screen` sealed class to include `Settings`, `About`, `Artists`, `AlbumDetail`.
- Wire up navigation for the new screens and mini-player actions.

#### [MODIFY] [MainMenuScreen.kt](file:///C:/AndroidStudioProjects/MyMD3Player/app/src/main/java/com/reikoonya/playeerok/ui/MainMenuScreen.kt)
- Top bar with Settings and About icon buttons.
- 3-column `LazyVerticalGrid` for albums with rounded square cards and album art thumbnails.
- Mini-player bar positioned above the bottom pill.
- Bottom pill with `Artists`, `Songs`, and `Randomize`.

## Verification Plan

### Automated Tests
- Build verification via `gradle_build` (`app:assembleDebug`).

### Manual Verification
- Deploy to device/emulator and test smooth navigation, mini-player interactions, 3-column album scrolling, and new screens.
