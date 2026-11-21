# duchamp — Game Mode for POCO X6 Pro 5G

**duchamp** is a root-requiring Android launcher that prepares a **POCO X6 Pro 5G** (codename
`duchamp`, Dimensity 8300 Ultra, Android 14 / API 34, KernelSU root) for mobile gaming — currently
optimized for **PUBG Mobile**.

One tap on the big **«Игровой режим»** button, and in 3–5 seconds duchamp:

- freezes / closes the apps you marked in the **Task Manager** (Диспетчер задач),
- frees RAM and reclaims swap,
- switches Wi-Fi to low-latency scanning and disables animations,
- optionally bypasses charging so the battery does not heat the cell,
- records real telemetry and produces a post-game report.

Everything is **fully reversible**. Every number in the report is **measured from the device** —
there are no fake "47 % faster" claims, no cache-clear buttons, no ads, no analytics, no online
accounts. The app works **fully offline** (no `INTERNET` permission).

> **Target device only.** duchamp reads device-specific sysfs/thermal/battery paths that exist on
> `duchamp`. On any other device it reports "monitoring only" and refuses to apply tweaks.

---

## Features

| Screen | What it does |
| --- | --- |
| **Игровой режим** | The one-button hero. Verifies root, applies the profile, freezes apps, reaches `ACTIVE`, and reverts everything on finish. |
| **Панель** | Live instrument panel: CPU / GPU frequency, temperature and free memory, sampled at 1 Hz and drawn with Vico charts. |
| **Сеансы** | History of completed sessions with a per-session post-game report (duration, average CPU, peak temperature, throttle point). |
| **Батарея** | Battery health (`charge_full`/`charge_full_design`, cycle count), charge state, thermal zones, power drain. |
| **Система** | Every function as a row: real current state (read from the system), with per-tweak **Применить / Сброс**. |
| **Сеть** | Wi-Fi status, gateway, latency to the game server, VPN presence. |

On first launch a one-time **onboarding** wizard verifies the device and builds your profile.

---

## Requirements

- A **POCO X6 Pro 5G** (`duchamp`) with **KernelSU** root.
- The app is launched **through the KernelSU manager app** so the root grant is available.
- Android 14 (API 34) or later.

No PC, no Android Studio, no ADB required to *use* the app — only to build it (see below).

---

## Building

duchamp is built **only via GitHub Actions** — there is no local build setup. Push to `main` (or a
`v*` tag) and the workflow `.github/workflows/android.yml` runs:

```
./gradlew lintDebug testDebugUnitTest assembleRelease
```

The workflow:

- sets up **JDK 17 (Temurin)** and **Gradle 9.6.1**,
- runs lint, **unit tests** (parsers, journal/revert, game-mode state machine, boot-script
  generator), and builds the **release APK**,
- uploads `app/build/outputs/apk/release/duchamp-<versionName>.apk` as an artifact,
- on `v*` tags, publishes a **GitHub Release** with the APK.

### Getting the APK

1. Push your change (or merge a PR) to `main`.
2. Open the **Actions** tab, select the latest workflow run, and download the **duchamp-apk**
   artifact, **or**
3. For a tagged release (`v0.1.0`, …), open the **Releases** page and download the attached APK.

`versionCode` is the GitHub run number and `versionName` is `0.<minor>.<run>`, so every build is
newer than the last and **installs over the previous one without losing data**.

---

## Signing (stable across builds)

Signing **must** be stable, otherwise an update install fails and you lose app data. Two options:

### Option A — CI secrets (preferred for a public repo)

Set these repository secrets in **Settings → Secrets and variables → Actions**:

| Secret | Meaning |
| --- | --- |
| `RELEASE_STORE_FILE` | Path to the keystore file (e.g. `keystore.p12`). |
| `RELEASE_STORE_PASSWORD` | Keystore password. |
| `RELEASE_KEY_ALIAS` | Key alias. |
| `RELEASE_KEY_PASSWORD` | Key password. |

The workflow passes them through to Gradle. If any is missing, it falls back to the committed dev
keystore.

### Option B — committed dev keystore (fallback)

A dev keystore is committed at `signing/dev.keystore` (alias `duchamp`, passwords in
`gradle.properties` as `duchamp.dev.*`). This is fine for personal builds; **rotate it before any
public release**.

### Generating a keystore

```bash
keytool -genkeypair -v \
  -keystore signing/release.keystore -storetype PKCS12 \
  -alias duchamp -keyalg RSA -keysize 2048 -validity 10000
```

Then either commit it (Option B) or upload it as a secret (Option A).

---

## Installing

1. Install the APK (allow "install unknown apps" for your download source).
2. Open **KernelSU**, grant root to `com.duchamp`, and launch duchamp **from KernelSU**.
3. Complete onboarding, mark apps in the Task Manager, and press **Игровой режим**.

## The boot script (KernelSU `service.d`)

duchamp ships a boot script generator (`BootScriptGenerator`). It writes
`/data/adb/service.d/duchamp.sh`, which KernelSU runs on **every boot**. The script:

- waits for boot to complete and the system to settle (~20 s),
- **forces the battery bypass OFF** (it must never survive a reboot),
- re-applies the "resettable but chosen permanent" settings (swappiness, Wi-Fi/BLE scan,
  animation scales),
- re-checks the permanent settings and re-applies them on divergence,
- writes a `/data/adb/duchamp/boot.log` with the outcome of every step.

The script is **idempotent** and always `exit 0`. The exact output is covered by a snapshot unit
test (`BootScriptGeneratorTest`).

---

## What duchamp never touches
<!-- ci-nudge -->

The following are hard-excluded and never modified, to keep the system stable and respect the user:

- `com.xiaomi.mipicks` (Game Turbo), `com.miui.securitycenter`, `com.miui.powerkeeper`
- `com.android.phone`, `com.android.systemui`
- `com.google.android.gms` (user 0)
- `com.mediatek.mt6897.gamedriver`, `com.xiaomi.glgm`

Also prohibited: `input_suspend`, `pm clear` / `remove-user`, thermal tweaks, cache-clear /
"RAM booster" buttons, `build.prop` boosts, TCP tweaks, "CPU cooling", fake counters, ads,
analytics, online accounts. The battery bypass is **always** reverted at session end and at boot —
it is never left on outside game mode.

## Root command safety

Every root command goes through a single `CommandRunner` abstraction (libsu):

- a **5 s timeout** by default (longer only for `ping`),
- **exit-code check** and error parsing,
- **never on the main thread** (all `suspend`, dispatched to `Dispatchers.IO`),
- failures are captured as a `CommandResult`, never thrown,
- commands are **read to memory** (stdout captured in-process). Redirecting to a file breaks
  `cmd` / `pm` / `settings` with `Failed transaction (2147483646)` under SELinux, so that is
  avoided.

Every action is written to a **write-ahead journal** before it runs, so the log survives crashes
and reboots and drives revert.

---

## Project layout

```
app/              application, Hilt, navigation, MainActivity
core/root         CommandRunner, LibsuCommandRunner, RootConfig, RootAvailability
core/device       DeviceReader + pure parsers (meminfo, cpufreq, gpu, thermal, battery, network, processes, packages)
core/data        Room DB, GameProfile/ProfileDataStore, repos, Tweak abstraction, GameModeEngine, BootScriptGenerator
designsystem      Onest + JetBrains Mono fonts, Color/Type/Shape/Theme, shared components
feature/gamemode  the one-button hero + state machine wiring
feature/monitor   live Vico charts
feature/sessions  history + post-game report
feature/health    battery & thermal
feature/system    tweak list with per-tweak apply/revert
feature/network   Wi-Fi, ping, VPN
feature/onboarding one-time setup wizard
```

## Tests

Unit tests run in CI (`testDebugUnitTest`) and cover:

- **parsers** — section-9 fixtures for meminfo, swaps, cpufreq, gpu, thermal, battery, network,
  package and process parsers,
- **journal / revert** — write-ahead journal transitions and revert via `FakeShell`,
- **game-mode state machine** — start/finish/fail sequences, freeze, battery-bypass revert,
- **boot-script generator** — snapshot test of the generated `service.d` script.

## License

Provided as-is for the `duchamp` device. Use responsibly — root access can damage your device.

<!-- ci-nudge: re-trigger CI after branch restore -->

<!-- ci-nudge: re-trigger CI after merge -->
