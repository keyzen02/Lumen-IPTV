# Verification — 2026-11-02

- ARM debug build: successful; application tv.lumen.iptv, label Lumen IPTV, version 0.1.0/code 1.
- ARM libraries: arm64-v8a and armeabi-v7a. APK v2 signature verified.
- Default enabled launcher activity: tv.own.owntv.MainActivity; Leanback launcher present.
- Existing unit results: 2,089 tests, 276 suites, zero failures/errors.
- Existing Android lint task: BUILD SUCCESSFUL. Its log also contains an internal lint exception; do not interpret this as proof every lint analysis completed without issues.
- Google TV API 36 x86_64 emulator: 3 instrumentation tests passed in 18.213 seconds.
- Real M3U SyncManager import into SQLCipher database passed; credentials round-trip, key stability and non-plaintext database header checked.
- ExoPlayer: Apple developer HLS, decoded H.264 768x432 at 30 fps, actual nonblank surface pixels verified.
- libmpv: official Blender Big Buck Bunny MP4 downloaded from its zipped distribution, local playback 1920x1080 at 30 fps, MediaCodec direct surface; nonblank pixels verified.
- Test harness fixed: explicit Unit return types required by JUnit.
- Network failures: Google sample bucket returned HTTP 403. Apple HLS through libmpv produced repeated HTTP 400 keepalive errors and startup watchdog resets. These remain unresolved; local decoding success is NOT network-playback verification.
- Fullscreen surface rendering tested; actual app fullscreen navigation, channel-switch latency, standby/resume, automatic recovery and long-duration sports tests remain outstanding.
- No physical Sony TV test; UHD, HDR, Dolby Vision, surround output and 100/120 Hz switching NOT physically verified.
- Public fork created at https://github.com/keyzen02/Lumen-IPTV. Tested Lumen source is published on lumen/main; main retains the newer upstream snapshot.

The delivered APK is debug-signed, not a production release. Source and workflow are present.
See LUMEN.md for installation, features, licensing and build instructions.
