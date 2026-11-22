# Food Tracker

An offline-first Android food diary, seeded with 166 Indian foods. Log meals,
water and weight; see macros, minerals and vitamins against targets derived from
your profile.

Your diary never leaves the device. The app has no account, no analytics and no
ads, and makes network calls from exactly three places, each one you trigger:
online food search and barcode lookup (OpenFoodFacts, USDA), and the optional AI
nutrition autofill (your own API key, your chosen provider). See the in-app
Privacy screen under Settings.

## Build

```sh
./gradlew assembleDebug      # APK at app/build/outputs/apk/debug/
./gradlew installDebug       # build + install on a connected device
```

Android SDK 37 and a Java 21 toolchain. The Gradle daemon pins Java 21 in
`gradle/gradle-daemon-jvm.properties` and downloads it if you do not have it,
so the wrapper is all you need. `minSdk` is 31.

## Optional configuration

Both are optional — the app works without either.

**USDA food search.** Falls back to the public `DEMO_KEY`, which works but is
rate-limited. For your own key ([get one here](https://api.data.gov/signup/)),
add to `local.properties` (git-ignored):

```properties
USDA_API_KEY=your_key_here
```

**AI nutrition autofill.** Off by default and configured in-app, not here:
Settings → AI nutrition autofill, or the last onboarding page. You supply a key
for Gemini, Claude or ChatGPT; only the food name is sent. The key is stored
apart from everything else and is deliberately excluded from Android's cloud
backup.

## Stack

Kotlin · Jetpack Compose (Material 3, dark-only) · Room · DataStore ·
WorkManager · Glance widgets · CameraX + ML Kit barcode scanning

Single module, MVVM. `data/` holds the Room database, stores and API clients,
`domain/` the goal calculations, `ui/screens/` one package per screen, and
`widget/` the three home-screen widgets.
