# Touch Grass IRL

A playful Android app that gamifies spending time **outside** — your real-world outdoor
time becomes XP, levels, streaks, and a living garden. Compete with friends, complete
challenges, and earn badges for touching grass in different weather.

## Features

- **Automatic outdoor tracking** — once location permission is granted, the app samples
  your location every 10 minutes and counts time spent away from home as outdoor time.
  No manual setup required (home is set automatically from your first location fix).
- **Manual sessions** — start a session to track steps, distance, and explore nature
  spots with on-screen motion and map tracking.
- **Gamification** — XP, levels, daily streaks, achievements, and a garden that grows
  the more you go outside.
- **Friends & social** — add friends by profile ID, send gifts, view a friend activity
  feed, and climb the weekly leaderboard.
- **Challenges** — create or join time-based challenges with friends.
- **Weather badges** — earn badges for being outdoors in different weather conditions.
- **Stats** — daily, weekly, and monthly breakdowns with charts.
- **Achievements** — unlock milestones across outdoor time, streaks, friends, and exploration.
- **Data export** — export your outdoor history as CSV.
- **Onboarding** — a short intro for first-time users.
- **Dark theme** — follows system or toggles manually.

## Installation

Download the latest signed APK from the
[Releases](https://github.com/Tenthvase9/Touch_grass_rl/releases) page
(`TouchGrass-vX.Y.Z.apk`) and install it on your device. You will need to grant
**location** (including **background location**) and **notifications** permissions for
automatic tracking to work.

## How tracking works

The app runs a lightweight foreground service that periodically reads your location.
If you are more than ~500 m from your recorded home location, that interval is counted
as outdoor time. Time is accumulated per sample, so a full day away from home is credited
even if you never open the app. Outdoor minutes feed your XP, streak, level, and the
social leaderboard.

> Tip: grant **All the time** background location access so tracking continues while the
> app is in the background.

## Tech stack

- **Kotlin** + **Jetpack Compose**
- **Room** for local persistence (sessions, daily logs, friends, achievements)
- **Firebase Firestore** for the social layer (friends, gifts, leaderboard, activity)
- **Google Play Services** location (Fused Location Provider)
- **OSMDroid** for in-session maps
- **Foreground service** for background outdoor detection

## Building from source

```bash
./gradlew assembleRelease
```

A `keystore.properties` (gitignored) is required for a signed release build. The debug
build works without it.

## Privacy

Outdoor time and location are used only to compute your stats and, with your consent,
to share aggregate stats (outdoor minutes, streak, level) with friends via Firebase.
Location is sampled periodically for detection and is not continuously streamed or sold.

## License

MIT — see repository for details.
