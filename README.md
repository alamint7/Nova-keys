# Nova Keys — Orbit Board Edition

Offline Android keyboard by **Zayd** — English QWERTY + Avro-style phonetic Bengali.

Features: Orbit Board multicolour theme, Nothing Dark and AMOLED Black themes, popup key bubbles, number row toggle, long-press digits, haptics with adjustable strength, adjustable height, Bengali candidate bar, no INTERNET permission, no analytics.

## Download the APK (v1.1.0)

1. Open the [Actions build page](https://github.com/alamint7/Nova-keys/actions/workflows/build.yml).
2. Open the latest successful **Build Nova Keys APK** run.
3. Under **Artifacts**, download `nova-keys-apk.zip` and extract `app-debug.apk`.

## Build locally

Open this project in Android Studio with JDK 17 and Android SDK 34, or run `gradle assembleDebug --no-daemon`. The APK is generated at `app/build/outputs/apk/debug/app-debug.apk`.

## Install and enable

Install the APK, open Nova Keys, choose **Enable keyboard**, then **Switch to Nova Keys**. Tap **EN** to toggle **বাং**.

Not included yet: glide typing, English word prediction, and a full personal dictionary. Bengali layout switches in Settings are currently UI preferences; full independent Jatiyo/Probhat key maps are not yet implemented.