# ClearTV EPL Fixtures + Astro AWANI News (Android TV)

A small companion app for the ClearTV launcher that shows upcoming English Premier League fixtures in **Malaysia Time (MYT, UTC+8)** and a continuously scrolling Astro AWANI news ticker along the bottom. It declares a ClearTV module card labelled **EPL Fixtures**.

## Features

- Fetches the next 14 days of English Premier League fixtures from ESPN's public scoreboard endpoint.
- Converts kickoff times to the `Asia/Kuala_Lumpur` time zone.
- Displays match status and scores when ESPN provides them.
- Scrolls recent Astro AWANI Bahasa Malaysia headlines across the bottom of the screen.
- Press OK on the news bar to open the latest available headline in the TV's web browser.
- **Refresh** reloads fixtures and news; the ticker also loads automatically when the app opens.
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

The app requests the Android `INTERNET` permission to retrieve:

- Fixtures from ESPN's public scoreboard endpoint: `https://site.api.espn.com/apis/site/v2/sports/soccer/eng.1/scoreboard`
- Astro AWANI's official RSS feed: `https://www.astroawani.com/rss/latest/public`

Astro AWANI says its RSS headlines are offered free for personal, non-commercial use. The ticker displays headline text and attributes it to Astro AWANI; selecting the bar opens the first headline's original article when a link is available. News availability depends on the publisher feed and the TV's network connection. See [Astro AWANI RSS terms and available feeds](https://www.astroawani.com/rss).

The app sends no personal data. It does not stream video or run a background service.

## Limitations

This is a standalone companion app and does not modify the ClearTV launcher itself. Fixtures and headlines are fetched when the app opens or when Refresh is selected; there is no automatic background refresh. If the RSS feed is temporarily unavailable, the app shows a fallback message.
