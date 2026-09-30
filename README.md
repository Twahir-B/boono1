# Aether — Android 10+ ready

Notch · Dynamic Island · Quick Cursor · Multi-provider AI · Screen-aware Agent · Voice

## Android version
**minSdk 26 (Android 8) · tested target for Android 10 (API 29)**

Live Updates (Android 16) are not required; Island uses a classic overlay.

## Build a ready-to-install APK (Android Studio — ~5 min)

1. Install [Android Studio](https://developer.android.com/studio) (any recent version).
2. **File → Open** the `Aether` folder.
3. Wait for Gradle sync (first time downloads dependencies).
4. Connect your phone (USB debugging on) **or** use an emulator API 29+.
5. **Build → Build Bundle(s) / APK(s) → Build APK(s)**.
6. When finished, click **locate** — install `app-debug.apk` on your phone  
   (enable *Install from unknown sources* if needed).

Or from terminal (with SDK installed):

```bash
cd Aether
./gradlew assembleDebug
# APK: app/build/outputs/apk/debug/app-debug.apk
```

## First run on your phone (Android 10)

1. Open **Aether**.
2. Home → **Enable Accessibility** (find Aether in the list).
3. Grant **Display over other apps** (Appear on top).
4. **API Keys** → paste OpenAI / Gemini / etc.
5. **Settings** → pick provider + model (e.g. `gpt-4o`).
6. **Start Island** — notch zone at top.
7. **Start Cursor** — swipe from bottom left/right edge for one-hand cursor.
8. Chat → optional **Agent** mode → approve taps in the dialog.
9. Mic button = voice (STT → your model → TTS). Eye icon = enable screen capture for vision.

## Features included

| Feature | Status |
|--------|--------|
| Notch gestures | ✅ |
| Dynamic Island overlay | ✅ |
| Quick Cursor | ✅ |
| BYOK multi-provider AI | ✅ (Settings → Chat wired) |
| Agent + approval dialog | ✅ |
| Screen vision (MediaProjection) | ✅ |
| Voice (STT + TTS cascade) | ✅ |
| Richer Island / Live Updates | Overlay ready; media polish next |

## Privacy
API keys encrypted on device. Accessibility only for gestures/agent. No keylogging.
