# Boxy

**English** · [Русский](README.ru.md)

Open-source Android app for managing the [Box for Root](https://github.com/taamarin/box_for_magisk) module
(Magisk / KernelSU / APatch). The design follows BFR by boxproxy; the code is written from scratch in Kotlin and Jetpack Compose.

Languages: English, Русский, Українська, Беларуская, Қазақша, Deutsch, Español, Français, Italiano, Türkçe, 中文, 日本語.

## Features

**Home**
- Service status with Start / Stop / Restart, uptime, service details (PID, core version, memory, CPU, affinity) and config reload
- Switch core, network mode and IPv6 while the service is stopped
- Panel and Logs shortcuts, SubStore shortcut when the `sub_store` module is installed
- Latency to three configurable targets
- IP card (LAN or public WAN with country flag), public IPv4/IPv6 details
- Net speed with a live graph (network counters or Clash API, optional chain filter)
- Subscription usage (subscription URLs, proxy-providers or core API)
- System card with CPU and RAM of the core, system environment (kernel, memory, IPSet)
- Home layout editor: hide sections, reorder and hide metric cards

**Apps**
- Blacklist / whitelist proxy mode (`package.list.cfg`)
- All Android users: main, second space, work profile, app clones
- Search, sort, filters (system / user, network permission, users), select all, invert, smart select

**Tools**
- Config: choose the active config of the current core, file manager for `/data/adb/box`
  (search, create, rename, delete, download by URL) and a code editor with search
- Network control: Wi‑Fi rules, SSID / BSSID lists with nearby Wi‑Fi scan, proxy on/off for hotspot clients, hotspot client MAC filter
- Update: cores (Mihomo, Sing-box, Xray, V2Ray, Hysteria), subscription, web UI with live output
- Subscription settings: URLs, provider files, interval, crontab
- Logs: module log files with auto refresh and colour highlighting

**Panel**
- Core web UI (Zashboard or MetaCubeXD, switchable), custom panels, SubStore, cache cleaning

**Settings**
- Theme (light, dark, system, Material You), true black, language picker with flags
- Liquid glass navigation bar with backdrop blur and lens, sheet blur, UI scale, system bars
- Navigation: Apps or Logs as a fourth tab
- GitHub mirror for all downloads (module scripts included)
- Status notification with actions, Quick Settings tile, open panel on launch
- Backup and restore of module files and app preferences
- App updates (GitHub releases) and module updates (`updateJson`)

## Module

The `module/` folder holds the Box for Root module used with Boxy (upstream v1.10.2 plus fixes, Russian installer,
BSSID matching and hotspot MAC filter). See [module/README.md](module/README.md).

## Build

Requirements: Android Studio (JDK 17+), Android SDK 37.

```
./gradlew assembleDebug
./gradlew assembleRelease
```

Release signing reads `keystore.properties` in the project root (not committed):

```
storeFile=keystore/boxy-release.jks
storePassword=...
keyAlias=boxy
keyPassword=...
```

Without it the release build is signed with the debug key.

## Credits

- Design: BFR by boxproxy
- Module: Box for Root by taamarin
- Libraries: Jetpack Compose, [libsu](https://github.com/topjohnwu/libsu), [Sora Editor](https://github.com/Rosemoe/sora-editor)
