# StopReels

StopReels is a native Android application written in Kotlin and Jetpack Compose. It locally detects Reel signals inside Instagram to help reduce compulsive scrolling. Videos shared in private conversations remain visible.

## Project status

The project includes:

- Compose Material 3 UI with light/dark themes, English as the default language, and a multilingual selector.
- A simple MVVM architecture with `StopReelsViewModel`.
- Local preferences and statistics using Preferences DataStore.
- An `AccessibilityService` restricted to the `com.instagram.android` package.
- An opaque overlay using `TYPE_ACCESSIBILITY_OVERLAY`, with a confirmable temporary unlock.
- Pure, testable detection for the feed, the Reels tab, Direct messages, regular posts, stories, and screen changes.
- No Internet permission, accounts, ads, analytics, or screenshots.
- Copyright and the current proprietary licence documented in [COPYRIGHT.md](COPYRIGHT.md) and [LICENSE](LICENSE).

## Build

Requirements: JDK 17 and the Android SDK platform currently required by Google Play.

With Android Studio, open this folder, let Gradle sync, and run `app` on an emulator or a device running Android 8.0 (API 26) or later.

The project can also be built without Android Studio:

```bash
./gradlew test
./gradlew assembleDebug
```

The debug APK is generated at `app/build/outputs/apk/debug/app-debug.apk`.

## Test on a physical device

1. Install the debug variant from Android Studio or with `adb install -r app/build/outputs/apk/debug/app-debug.apk`.
2. Open StopReels and read the welcome screen.
3. Open **Accessibility settings** and enable StopReels.
4. Return to the app and verify that the status says **Service active**.
5. Open Instagram and test the Reels tab, a Reel inserted in the feed, and a video shared through Direct.
6. Confirm that Reels outside Direct are covered and that a video in a private conversation is not covered.
7. Tap **View for … seconds**, if enabled, and verify that the overlay returns when the timer ends.
8. Disable the service from Settings > Accessibility to verify the shutdown flow.

The service and overlay can only be tested on a real Android device. The detection logic is covered by JVM tests.

## Background operation and Xiaomi/POCO/Redmi

StopReels does not keep its own process alive and does not need a `ForegroundService`: Android manages the `AccessibilityService` while the user uses Instagram. The StopReels screen can be closed.

On Xiaomi, POCO, or Redmi devices running MIUI/HyperOS, the system may restrict enabled services to save battery. After installing the application:

1. Open StopReels and enable the service in **Settings > Accessibility**.
2. In **Settings > Apps > Manage apps > StopReels**, open **Battery** and select **No restrictions** when available.
3. In **Settings > Apps > Permissions > Background autostart**, enable StopReels if that screen exists on your version.
4. Avoid swiping StopReels away from Recents if your HyperOS version uses that action to clean processes.

The exact names vary between MIUI/HyperOS versions. The application includes buttons that open the StopReels settings and battery screen. It does not request `INTERNET`, start an additional service, or automatically request permission to ignore battery optimisation.

## How Reel detection differs from Direct

`ReelDetectionEngine` works on a small copy of the accessibility hierarchy from the active window. It looks for explicit Reel signals in text, content descriptions, or `viewIdResourceName`, and uses the lower position of an element to identify shortcuts.

Before blocking, it calculates private-context signals: Direct, messages, chat, conversation, the message composer, and their equivalents in other supported languages. If there are two conversation signals, or one conversation signal together with an editable message field, the context is treated as private and no overlay is created, even if the shared video mentions a Reel.

A generic video without a Reel signal is not blocked. This reduces false positives on regular posts, but it means Instagram may show a Reel that exposes no recognisable accessibility signal.

## Publish on Google Play

See [GOOGLE_PLAY_CHECKLIST.md](GOOGLE_PLAY_CHECKLIST.md). Google Play requires a signed Android App Bundle, a public privacy policy, the Data safety form, and a specific declaration because the application uses `AccessibilityService`.

## Privacy

See the [privacy policy](PRIVACY_POLICY.md). The service does not request Internet access or capture the screen. It only processes Instagram's visible accessibility hierarchy temporarily and stores preferences/counters locally in DataStore.

## Known limitations

- Instagram may change text, resource IDs, hierarchy, or windows without notice.
- Android and different Instagram versions may expose different information to accessibility services.
- No heuristic can guarantee perfect blocking. The design prioritises not covering Direct, disabled stories, or regular posts.
- An accessibility overlay may behave differently depending on the manufacturer, split-screen mode, rotation, or accessibility permissions.
- On Xiaomi/POCO/Redmi devices, battery saving or autostart restrictions may stop or delay the service until the user selects **No restrictions**.
- Temporary unlocking is based on process time and may restart if Android kills the service.
- The counter represents local overlay detections, not an exact measurement of videos watched.

## Contributing

The `main` branch is protected. Contributions must be submitted through forks and Pull Requests. See [CONTRIBUTING.md](CONTRIBUTING.md).

## Project structure

```text
app/src/main/java/com/stopreels/
├── data/PreferencesRepository.kt
├── domain/ReelDetection.kt
├── service/StopReelsAccessibilityService.kt
├── ui/StopReelsApp.kt
├── ui/theme/Theme.kt
├── MainActivity.kt
├── StopReelsApplication.kt
└── StopReelsViewModel.kt
```
