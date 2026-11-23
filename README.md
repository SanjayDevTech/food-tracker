# Food Tracker

An offline-first Android food diary, seeded with 167 Indian foods. Log meals, water and
weight; see macros, minerals and vitamins against targets worked out from your profile.

Your diary never leaves the device. There is no account, no analytics and no ads, and the
app makes network calls from exactly three places, each one you trigger: online food search,
barcode lookup, and the optional AI nutrition autofill. See the in-app Privacy screen.

## What it does

- **Log a day** into breakfast, lunch, snack and dinner, plus water and weight. Back-fill
  any earlier day from the date strip.
- **Indian food out of the box:** 167 seeded dishes with real serving sizes — *1 dosa*,
  *1 katori*, *1 roti* — not grams you have to guess at.
- **Cooking fat is a first-class variable.** A dish carries the oil it was cooked in, and
  swapping groundnut for ghee or coconut moves its fat, saturated fat, vitamin A and
  cholesterol. Most trackers pretend this does not exist.
- **30 nutrients**, not four: calories and the macros, then fibre, sugar, omega-3,
  cholesterol, nine minerals and thirteen vitamins.
  - A **status strip** answers "is today broadly fine?" in one 40dp band, 28 cells, no
    reading. Only the exceptions get named below it.
  - Limits (sugar, saturated fat, sodium) are drawn as limits — 40% of a cap is a good
    day, not a shortfall.
  - **Dietary cholesterol is tracked without a target**, because no current guideline sets
    one. The reasoning, with sources, is in `NutrientTarget.Untargeted`.
- **Trends** over 7, 30, 90 or 365 days: calories against goal, macro split, weight, and a
  consistency heatmap showing which nutrient you are chronically short of.
- **Find foods rich in** any nutrient you are behind on, ranked across the whole catalogue.
- **Add food** three ways: by hand, by **barcode** (on-device scan, then OpenFoodFacts or
  USDA), or by **AI autofill** — your own key for Gemini, Claude or ChatGPT, two-pass with
  a verify step, and only the food name is ever sent.
- **Targets from your body:** BMR and TDEE from age, sex, height, weight and activity, then
  a calorie goal from whether you want to lose, hold or gain. Change any of it and all 30
  targets move.
- **Reminders** for each meal, water and the weigh-in, on Doze-aware alarms.
- **Two home-screen widgets:** calories left, and the macro split.
- **Backup and restore** to a folder you choose, as plain JSON you can read. Your AI key is
  deliberately left out of Android's cloud backup.

## Install

Grab the APK from [Releases](../../releases). It is signed with the public key committed in
this repo, so each release installs over the last as an update.

That key is public on purpose, which means anyone can sign an APK with it — it is a
convenience for updating, never proof of who built a build.

## Build

```sh
./gradlew assembleDebug      # APK at app/build/outputs/apk/debug/
./gradlew installDebug       # build + install on a connected device
./gradlew assembleRelease    # signed, R8-shrunk release APK
```

Android SDK 37 and a Java 21 toolchain. The Gradle daemon pins Java 21 in
`gradle/gradle-daemon-jvm.properties` and downloads it if you do not have it, so the wrapper
is all you need. `minSdk` is 31, English only.

CI mirrors this: **Actions → Build Release APK** builds any branch and attaches the APK to
the run, and **Actions → Trigger a Release** bumps the version, builds, then tags and
publishes a release only if the build passed.

## Optional configuration

Both are optional — the app works without either.

**USDA food search.** Falls back to the public `DEMO_KEY`, which works but is rate-limited.
For your own key ([get one here](https://api.data.gov/signup/)), add to `local.properties`
(git-ignored):

```properties
USDA_API_KEY=your_key_here
```

**AI nutrition autofill.** Off by default and configured in-app, not here: Settings → AI
nutrition autofill, or the last onboarding page. The key is stored apart from everything
else and is excluded from cloud backup.

## Stack

Kotlin · Jetpack Compose (Material 3, dark-only) · Room · DataStore · WorkManager ·
Glance widgets · CameraX + ML Kit barcode scanning

Single module, MVVM:
- `data/`: the Room database, the seeded catalogue, stores and API clients.
- `domain/`: goal calculation — BMR, TDEE, the 30 targets.
- `ui/`: the design system in `components/`, one package per screen under `screens/`.
- `widget/`: the two home-screen widgets.
- `reminder/`: alarm scheduling and the notification.

The interface follows a short written rule set — a standing head with a rule under it rather
than cards, the number as the loudest thing on screen, the accent reserved for data. It is in
[docs/ui-house-rules.md](docs/ui-house-rules.md), and it is worth reading before changing any
screen.

## How it was made

Built with AI assistance (Claude), directed, designed and tested by its maintainer. The commit
trailers say so too.

## License

[MIT](LICENSE) © 2026 Food Tracker contributors.

Nutrient reference values follow ICMR-NIN and the Dietary Guidelines for Americans; food data
can come from OpenFoodFacts (ODbL) and USDA FoodData Central (public domain). Ships the
AndroidX, Kotlin and ML Kit libraries (Apache 2.0).
