# Lumen IPTV

An independent GPLv3 fork of OwnTV. Application ID: `tv.lumen.iptv`.
Version: 0.1.0. Android 8.0/API 26 or newer. ARM APK includes arm64-v8a and armeabi-v7a.

## Build

Open this directory in Android Studio. The modified OwnTV_Core source is included in
`owntv-core` as a Gradle composite build; no separate core checkout is required.
Use JDK 21 and Android SDK 37.0. Run `gradlew.bat :app:assembleStandardDebug`.
The installable APK is `app/build/outputs/apk/standard/debug/app-standard-debug.apk`.
The debug signature is for testing; retain a private release signing key for stable future upgrades.
GitHub Actions configuration is `.github/workflows/lumen.yml`.

## Install on Sony Google TV

Copy the ARM APK to a USB drive or transfer it with a trusted file-transfer application.
Open it with a file manager on the TV, allow installation from that file manager when
Google TV prompts, and install Lumen IPTV. It coexists with official OwnTV.
Alternatively, after enabling developer options and network debugging on your TV,
pair/connect Android platform-tools to the address shown by the TV, then run
`adb install -r Lumen-IPTV-0.1.0-arm-debug.apk`.
Add your own authorized M3U/Xtream source in the setup wizard or via its QR setup.

Use Balanced buffering initially. Leave maximum video quality unrestricted and Prefer SDR off.
Enable Auto frame rate in Player settings and permit content frame-rate matching in
Google TV settings where available. Audio Auto uses reported output capabilities with
the upstream recovery safeguards. A soundbar/receiver and its HDMI configuration affect surround output.

## Changes and retained functionality

Lumen retains OwnTV's Compose TV UI, remote focus/navigation, M3U/Xtream/Stalker,
XMLTV guide, programme search and matching, custom categories, favorites/history,
multiple playlists, movies/series/resume, recording/scheduling, catch-up, multiview,
profiles/PIN controls, backup/restore, QR setup, and both playback engines.
Country/language organization uses provider groups or user-created categories.

Changes: separate app ID/name/deep-link scheme/update destination; checked-in core;
unrestricted adaptive-video viewport selection; diagnostic display refresh separate
from source FPS; declared/estimated bitrate distinguished from network throughput;
Dolby Vision format detection in Media3; improved missing-FPS reporting;
SQLCipher database encryption with an Android Keystore-wrapped random key.
No paid locks, advertising, bundled provider credentials, or channel subscriptions added.
Encrypted database storage does not encrypt exported plaintext backups or every cache/settings file.
Use the existing password-protected backup option for exports containing credentials.

## Verification scope

See `VERIFICATION.md` for actual results. Emulator verification cannot establish Sony
hardware decoder throughput, HDR output, Dolby Vision profiles, refresh switching,
HDMI passthrough, or sustained sports performance. The TV exposes the final usable
codec/display modes; no synthetic UHD or frame-rate interpolation is added.
Multiview consumes one decoder and provider connection per tile.

## Provenance and licenses

OwnTV base: bc4cab5d8adb70fd4a476b8d8e007050abb295e5.
OwnTV_Core base: 236cecaabcabd6e71e93cb5364b74b3f75a5b852.
Original repositories: https://github.com/ahXN00/OwnTV and
https://github.com/ahXN00/OwnTV_Core. Their GPLv3 LICENSE files are retained.
Lumen modifications are distributed under GPLv3. Original names in Kotlin namespaces,
credits, assets and upstream documentation identify the original work.
Dependency versions remain in both Gradle version catalogs. libmpv 2026.10.4 source/build
scripts: https://github.com/ahXN00/OwnTV_libmpv. SQLCipher 4.20.0:
https://github.com/sqlcipher/sqlcipher-android (retain its BSD-style license).
Media3 and AndroidX use Apache-2.0; mpv/FFmpeg and their enabled components retain
their respective licenses. Distribute corresponding source/build scripts with binaries.

Test media only: Apple Bip Bop developer samples (https://developer.apple.com/streaming/examples/)
and Big Buck Bunny, copyright 2008 Blender Foundation, CC BY 3.0
(https://peach.blender.org/about/). Test URLs are confined to instrumentation tests.
