# ClearTV EPL Fixtures (Android TV)

A small companion app for the ClearTV launcher that shows upcoming English Premier League fixtures in **Malaysia Time (MYT, UTC+8)**. It declares a ClearTV module card labelled **EPL Fixtures**.

## Features

- Fetches the next 14 days of English Premier League fixtures from ESPN's public scoreboard endpoint.
- Converts kickoff times to the `Asia/Kuala_Lumpur` time zone.
- Displays match status and scores when ESPN provides them.
- Manual **Refresh** button and remote-control navigation.
- No account, API key, ads, analytics, streaming, or background service.

## Build

Open this folder in Android Studio, or build from a machine with Android SDK 35 and JDK 17:

```bash
gradle assembleDebug
```

APK output: `app/build/outputs/apk/debug/app-debug.apk`.

Install on the TV with ADB:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

The first time the ClearTV launcher discovers the card, it asks whether to show it. Choose **Show card**. If you previously hid it, hold OK on the launcher's Settings pill and open **Module cards** to change the choice.

## Data and privacy

The app requests the Android `INTERNET` permission only to retrieve fixture data from:

`https://site.api.espn.com/apis/site/v2/sports/soccer/eng.1/scoreboard`

It sends no personal data. Fixture availability and accuracy depend on the upstream data provider and network access. Kickoff times are displayed in Malaysia time; TV broadcast times may differ. The app does not stream or link to match broadcasts.

## Limitations

This is a standalone companion app and does not modify the ClearTV launcher itself. Fixtures are loaded when the app opens or when Refresh is selected; there is no automatic background refresh.
