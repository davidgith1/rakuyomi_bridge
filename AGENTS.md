# Rakuyomi Bridge — AI Agent Guide

## Project Overview

Kotlin Multiplatform + Android companion app for the RakuYomi KOReader plugin.
Loads `librakuyomi_server.so` (Rust cdylib) via JNI and runs the rakuyomi HTTP
server as a foreground service, exposing it at `http://127.0.0.1:8787`.

Includes a CLI test harness (`bridge/cli/`) for Linux that starts/stops the
server binary and runs API tests.

## Repository Structure

```
bridge/
├── build.gradle.kts                    # Root: plugin declarations (apply false)
├── settings.gradle.kts                 # Multi-module config
├── gradle/libs.versions.toml           # Version catalog
├── shared/                             # KMP shared module (android + jvm)
│   ├── build.gradle.kts
│   └── src/
│       ├── commonMain/.../             # ServerConfig, BridgeClient, BridgeResponse
│       ├── androidMain/.../            # JniBridge, RakuyomiServer
│       └── jvmMain/.../                # Platform.actual (HttpURLConnection)
├── androidApp/                         # Android application (Compose + Service)
│   ├── build.gradle.kts
│   ├── proguard-rules.pro
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── kotlin/.../
│       │   ├── MainActivity.kt
│       │   ├── RakuyomiTheme.kt
│       │   ├── ServerService.kt
│       │   └── BootReceiver.kt
│       └── res/
│           ├── values/strings.xml
│           └── xml/network_security_config.xml
└── cli/                               # JVM CLI test harness (Linux)
    ├── build.gradle.kts
    └── src/main/kotlin/.../BridgeCLI.kt
```

## Build System

- AGP 8.4.0, Kotlin 1.9.22, Gradle 8.6
- JVM target: 1.8, minSdk 21 (androidApp), targetSdk 34
- KMP targets: `androidTarget` (shared) + `jvm` (shared + cli)
- UI: Jetpack Compose (Material3) via BOM 2024.02.00
- CLI: `application` plugin, `java.net.HttpURLConnection` (no extra HTTP lib)
- The `.so` is built separately via `scripts/build-android.sh`

## KMP Targets

| Source Set | Target | Key Files |
|---|---|---|
| `commonMain` | all | ServerConfig, BridgeClient (expect Platform), BridgeResponse |
| `androidMain` | Android | JniBridge, RakuyomiServer |
| `jvmMain` | JVM | Platform.actual (HttpURLConnection) |

The `expect object Platform` / `actual object Platform` pattern provides
HTTP get/post on both JVM and future native targets.

## CLI Usage (Linux)

```sh
./gradlew :cli:run --args="health 8787"
./gradlew :cli:run --args="run ./server /tmp/data"
./gradlew :cli:run --args="exec ./server /tmp/data -- curl http://127.0.0.1:8787/library"
./gradlew :cli:run --args="test ./server /tmp/data"
```

## Key Architecture

```
KOReader (Lua) ──TCP──► Rust server ──► SQLite + WASM sources
                           ▲
Android:  JniBridge ←───╯  (libserver.so via System.loadLibrary)
Linux:    HttpURLConnection  (standalone server binary, systemd)
```

## Permissions (Android)

| API | Permission | UX |
|---|---|---|
| 30+ | `MANAGE_EXTERNAL_STORAGE` | Compose AlertDialog → Settings intent |
| 33+ | `POST_NOTIFICATIONS` | Compose AlertDialog → system dialog |
| 34+ | `FOREGROUND_SERVICE_DATA_SYNC` | Manifest only, maxSdkVersion="34" |

## Important Rules

- **Compose only** — no XML layouts
- Use `ComponentActivity` + `setContent` for Android
- Use `expect`/`actual` for platform-specific code in shared module
- `jvmMain` must not depend on Android APIs
- `androidMain` must not depend on JVM APIs (java.net, etc.)
- All user-facing strings go into `strings.xml`
- No emojis in code or comments

## RakuYomi Integration Points

- Server port: `8787` (must match `listener.rs` `DEFAULT_TCP_PORT`)
- Server host: `127.0.0.1`
- Data directory: `/storage/emulated/0/koreader/rakuyomi` (Android) or `~/.local/share/rakuyomi` (Linux)
- Health check: `GET /health-check`
