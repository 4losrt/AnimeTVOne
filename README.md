# AnimeTVOne

An independent, third-party anime browsing and playback app for **Fire TV** and
**Android TV**, built with **Jetpack Compose for TV** and a remote-control-first
(DPad) navigation model.

> **Unofficial client:** This project is not affiliated with, endorsed by, or
> operated by Anime1.me. Anime1.me is a third-party service and retains ownership
> of its own content, names and trademarks. This app only browses and plays media
> that the service itself exposes; no media is bundled or redistributed.

---

## Features

**Browsing**
- Home feed with a hero billboard and horizontal rows (今日更新 / 熱門連載 / 精選推薦).
- Weekly schedule (每週新番表) parsed from the current season's table, with an
  automatic fallback to the newest season page that exists.
- Full catalog (動畫列表) with **season filter chips**, keyword search and paging.
- Poster resolution with a multi-source fallback chain (local cache →
  prepopulated dictionary → Bangumi / AniList / Jikan → Bangumi web search), with
  a text-poster fallback so a card is never blank.

**Playback**
- Media3 / ExoPlayer player with a custom DPad-first control overlay.
- Play/pause, ±10s, next episode, seek bar, playback speed and aspect-ratio modes.
- Resume position is remembered per episode (playback history).

**Library**
- Favorites (追番) and playback history (歷史紀錄) with progress bars.
- Settings for refreshing the catalog and clearing cover / history data.

**Interface**
- Streaming-style dark UI (Disney+/Netflix inspired): near-black canvas, a single
  blue accent, bold typography, rounded cards with a white focus ring and scale.
- Every screen shares the same loading / error / empty state components.

---

## Install

Grab the APK from the [latest release](https://github.com/4losrt/AnimeTVOne/releases/latest).

> The released APK is **self-signed with a debug key** for direct sideloading — it
> is not Play-Store production-signed. It installs over earlier builds because the
> same signing key is reused.

**Sideload over ADB**

```bash
# Fire TV / Android TV: enable Developer Options → ADB debugging first
adb connect <tv-ip>:5555
adb install -r AnimeTVOne-v1.3.7.apk
```

Or use any Fire TV sideloading tool (e.g. Downloader) and open the APK.

**Requirements:** Android TV / Fire OS on **Android 7.1 (API 25)** or newer, with
a DPad remote.

---

## Build

Prerequisites: JDK 17, Android SDK (platform 37, build-tools 37), and
`ANDROID_HOME` / `local.properties` pointing at your SDK.

```bash
# Debug build (debug-signed, installable as-is)
./gradlew :app:assembleDebug

# Release build (R8 minified + resource shrunk)
./gradlew :app:assembleRelease
```

Release builds are minified with R8 (the shipped APK is ~2.9 MB). To sign a
release yourself:

```bash
apksigner sign --ks <your.keystore> --out app-release-signed.apk \
  app/build/outputs/apk/release/app-release-unsigned.apk
```

### Tests

```bash
./gradlew :app:testDebugUnitTest
```

---

## Tech stack

| Area | Used |
| --- | --- |
| Language / UI | Kotlin, Jetpack Compose for TV (`androidx.tv.material3`) |
| Playback | AndroidX Media3 / ExoPlayer |
| Images | Coil (memory + disk cache) |
| Networking | OkHttp |
| Parsing | Moshi `JsonReader` (streaming), Jsoup (HTML) |
| Build | Gradle, AGP 9, R8 |

Architecture: a single app-wide `AnimeRepository` (singleton provider) exposes
cached data through Kotlin `Flow`s consumed by per-screen `ViewModel`s; the
network layer (`Anime1Service` + `Anime1Parser`) scrapes the service's JSON/HTML,
and covers are resolved lazily and cached on disk.

---

## AI-assisted development

This project used AI assistance for requirements, interface design,
implementation, testing and documentation. The project maintainer remains
responsible for reviewing and publishing all code and functionality.

## License

This project is licensed under the [MIT License](LICENSE).
