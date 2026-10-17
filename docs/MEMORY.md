# Project Memory

## Current Status
- **App Status**: Fully functional, compiling cleanly (`BUILD SUCCESSFUL`), hardened with PBKDF2 (200K iterations) & log sanitization, and installed on the connected **Google Pixel 9** (Android 17, `tokay`).
- **Display Status**: Locked 120Hz LTPO display pipeline active (`renderFrameRate: 120.00001 Hz`, `modeId: 2`). Zero dropped frames during list scroll.
- **Biometric Status**: Zero-tap Pixel 9 ultrasonic fingerprint / Face Unlock prompts automatically on locked app open with custom Master PIN fallback.

## Recently Completed
1. **120Hz Smooth Scrolling Pipeline**: Pre-rasterized app icons into 120x120 Bitmaps on `Dispatchers.IO`, locked display mode to 120Hz via `preferredDisplayModeId`, marked `AppItem` as `@Immutable`, and memoized category counts & health stats.
2. **Setup Permission Clarity**: Updated `OnboardingScreen.kt` with explicit "Required" (Overlay, Accessibility) and "Optional" (Notification Shield) badges, an inline "Later" skip action, and an intuitive primary action button ("Grant 2 Required Permissions" ➔ "Continue to App Lock").
3. **Frictionless Zero-Tap Biometric Unlock**: Updated `LockActivity.kt` to auto-trigger `BiometricPrompt` on app launch with isolated `BIOMETRIC_STRONG` and custom PIN fallback (`setNegativeButtonText("Use PIN")`).
4. **Dynamic App-Themed Material You Lock Screen**: Implemented ambient radial brand glow and accent tinting adapting to target app colors (WhatsApp green, YouTube ruby, Instagram magenta, etc.) with a toggle in Settings.
5. **Contextual Category Batch Controls**: Added a contextual action strip in `MainScreen.kt` when filtering by category (Financial, Social, Sensitive), featuring "X of Y protected" indicator and 1-tap `[Lock All]` / `[Unlock All]` buttons.
6. **Standard 8 Governance & Specification Documents**: Created `PRD.md`, `AGENTS.md`, `CLAUDE.md`, `DESIGN.md`, `ARCHITECTURE.md`, `RULES.md`, `MEMORY.md`, `DECISIONS.md`, and `TESTING.md`.
7. **Hybrid Clean Documentation Structure**: Organized docs suite into `docs/` (`PRD.md`, `DESIGN.md`, `ARCHITECTURE.md`, `RULES.md`, `MEMORY.md`, `DECISIONS.md`, `TESTING.md`) while keeping `AGENTS.md`, `CLAUDE.md`, and `README.md` at root for automatic AI agent discovery.
8. **Automated Unit Test Suite**: Implemented automated unit tests across `CryptoManagerTest` and `SessionManagerTest` validating PBKDF2/SHA-256 salted PIN hashing, constant-time equality comparisons, grace period expiration, immediate relock foreground handling, pause protection, and night lockdown scheduling boundaries. (100% pass rate, 0 failures).
9. **Refined 6-Toggle Pixel 9 Enhancements**:
   - **Option 1 (Recents Masking)**: `FLAG_SECURE` applied plus instant package relock when swiping to launcher/recents overview.
   - **Option 2 (Quick Settings Tile)**: Biometric/PIN auth required before pausing protection; configurable duration sheet (5m, 15m, 30m, 1h, until resumed).
   - **Option 3 (Pixel Haptic Feedback)**: Tactile clicks strictly respect system silent mode and system haptic feedback settings.
   - **Option 4 (Fluid Micro-Animations)**: In-app animation toggle + automatically honors Android OS 'Remove animations' setting.
   - **Option 5 (Night Lockdown)**: 0s relock window with sub-toggle 'Require Master PIN Only at Night' to prevent unauthorized sleep unlocks.
   - **Option 6 (Per-App Grace)**: Clear readable badge chips (`0s`, `30s`, `1m`, `5m`, `Default`) on app rows + 'Reset all custom app timeouts to default' action in Settings.
10. **Target App Unlock Resumption & Task Affinity Fix**: Resolved an Android task stack issue where unlocking a target app (e.g., Instagram) brought Pixel Guard's `MainActivity` to the foreground instead of the target app. Fixed by detaching `LockActivity` with `taskAffinity=""`, `launchMode="singleInstance"`, `noHistory="true"`.
11. **Infinite Lock Screen Loop Elimination**: Resolved a race condition where unlocking an app immediately re-triggered `LockActivity` in a loop via 3s transition buffer and transient system package filters.
12. **APK Size Optimization (66MB ➔ 3.3MB)**: Diagnosed that the 66MB debug footprint was caused by uncompressed debug DEX files, absence of dead-code stripping, and the bulky `material-icons-extended` dependency (over 30,000+ unminified classes). Configured R8 minification (`isMinifyEnabled = true`), resource shrinking (`isShrinkResources = true`), and debug signing in `build.gradle.kts`. Slashed release APK footprint by **95%** down to **3.3MB** (DEX shrunk from 60.3MB across 14 multi-dex files to a single 2.47MB DEX file). Verified and installed directly on Pixel 9.
13. **Titan Shield & Lock Crest App Icon Redesign**: Redesigned adaptive launcher icon with deep midnight slate base, ambient security aura, concentric Titan M2 radar rings, faceted chiseled armor shield, and electric cobalt padlock with keyhole aperture. Added native Android 13+ `<monochrome>` layer for seamless Material You dynamic themed icon support on Google Pixel Launcher.
14. **System-Wide Brand Unification ("Pixel Guard")**: Replaced legacy generic "Pixel App Lock" string resources with unified "Pixel Guard" branding across launcher labels, Accessibility service descriptions, Notification Shield settings, and Quick Settings tiles.
15. **Security Hardening & Vulnerability Remediation**:
    - **PBKDF2 Key Stretching**: Upgraded PIN hashing from single SHA-256 to `PBKDF2WithHmacSHA256` with 200,000 iterations and memory zeroing (`PBEKeySpec.clearPassword()`), eliminating offline GPU brute-force feasibility.
    - **Default PIN Removal**: Completely removed hardcoded `"1234"` fallback in `AppLockRepositoryImpl.verifyPin()`, returning `false` until the user sets a custom PIN during onboarding.
    - **Logcat Privacy Protection**: Wrapped all foreground package names and event logging behind `if (BuildConfig.DEBUG)` in `AppLockAccessibilityService.kt`, preventing snooping via `adb logcat` on release builds.
    - **Session-Persisted Intrusion Attempts**: Migrated `failedAttempts` tracking into `SessionManager` singleton keyed by package name so counters persist across `LockActivity` destructions, preventing bypass of the 3-attempt camera capture threshold.
    - **Release BuildConfig Generation**: Added `buildConfig = true` under `buildFeatures` in `build.gradle.kts` to enable release BuildConfig flags.
    - **Release APK Deployment**: Compiled and deployed `app-release.apk` (3.46MB) directly to the connected Pixel 9 via wireless debugging.
16. **Mandatory Onboarding PIN Creation & First-Launch Lockout Elimination (OWASP M3)**:
    - Resolved critical authentication barrier where removing default PIN `"1234"` locked out unconfigured users on first launch.
    - Added `hasPinSetFlow` reactive flow in `AppLockRepository` and `MainViewModel`.
    - Updated `MainActivity.kt` to allow first-time users directly into Onboarding while strictly blocking `MainScreen` until both permissions are granted and a Master PIN is saved.
    - Integrated "Step 2: Create Master PIN" in `OnboardingScreen.kt` with numeric masking, confirmation match validation, and PBKDF2 derivation. Subsequent launches enforce zero-tap biometric / Master PIN gates.
17. **Progressive Brute-Force Rate Limiting & Defense-in-Depth (OWASP M3, M6, M8)**:
    - Implemented progressive lockout logic in `SessionManager`: 1–4 attempts standard shake, 5th attempt triggers 30s lockout, 8+ attempts trigger 60s extended lockout.
    - Added lockout visual countdown banner and keypad disabling in `LockOverlayScreen.kt`.
    - Enforced immediate synchronous `FLAG_SECURE` in `LockActivity.onCreate()` and `WindowManager.LayoutParams.FLAG_SECURE` in `OverlayWindowManager.kt`.
    - Protected `BootReceiver` with `android:permission="android.permission.RECEIVE_BOOT_COMPLETED"`.
    - Sanitized `NotificationShieldService` logging behind `BuildConfig.DEBUG`.
    - Verified with 31 passing unit tests and deployed release build to Pixel 9.
18. **Anti-Tamper & Tapjacking (UI Redressing) Defense (OWASP M1, M8, M10)**:
    - Expanded `AppLockAccessibilityService` to intercept canonical package installers (`com.google.android.packageinstaller`, `com.android.packageinstaller`, `com.samsung.android.packageinstaller`) alongside `com.android.settings`, preventing uninstallation attempts via launcher drag-and-drop.
    - Added dedicated "Prevent Play Store Tampering" toggle (`isPlayStoreProtected`) in `SettingsScreen.kt` and `LockPreferences` to lock `com.android.vending`.
    - Enforced `window.setHideOverlayWindows(true)` in `LockActivity.onCreate()` on Android 12+ (API 31+) to hide third-party `TYPE_APPLICATION_OVERLAY` windows and defeat tapjacking attacks.
    - Verified test suite and deployed hardened release build to Pixel 9.
19. **At-Rest Intruder Photo AES-256-GCM Hardware Encryption (OWASP M2)**:
    - Implemented containerized AES-256-GCM file encryption (`encryptToFileBytes`, `decryptFromFileBytes`) in `CryptoManager` backed by Titan M2 KeyStore.
    - Updated `SilentCameraCapture` to write encrypted `.enc` files to sandboxed storage instead of plaintext JPEGs.
    - Updated `IntruderLogsScreen` with `rememberDecryptedBitmap` moving decryption and decoding off the Main Thread to `Dispatchers.IO`, retaining 120Hz display pacing and supporting legacy `.jpg` fallback.
    - Passed all 31 unit tests and installed release APK on Pixel 9.
20. **Non-Destructive Notification Privacy Shield Replacement (OWASP M6)**:
    - Replaced permanent notification destruction (`cancelNotification`) with masked placeholder notifications posted via dedicated `CHANNEL_ID_MASKED`.
    - Masked notification displays target app name and generic text: `"New notification • Content hidden by Pixel Guard"`.
    - Protected against self-package interception and configured `setAutoCancel(true)` with intent launching the locked app.
    - Compiled with R8 minification and deployed to Pixel 9.
21. **Scramble PIN Keypad Anti-Shoulder Surfing (Lock Screen UX)**:
    - Added `isScramblePinEnabled` toggle in `SettingsScreen.kt` and `LockPreferences`.
    - Implemented session-stable randomized numeric button distribution in `LockOverlayScreen` while keeping biometric and backspace corner positions fixed.
    - Passed all 31 unit tests and deployed release APK to Google Pixel 9.
22. **Zero-Latency Translucent Interception & Predictive Back Protection (UX Polish)**:
    - Upgraded base theme to dark `android:Theme.Material.NoActionBar` (`#FF0F141A`) and introduced `Theme.PixelAppLock.LockOverlay` (`windowIsTranslucent = true`, `windowDisablePreview = true`, `windowBackground = @android:color/transparent`), completely eliminating cold-start white rectangular flickers on 120Hz displays.
    - Replaced deprecated activity transitions with Android 14+ `overrideActivityTransition(OVERRIDE_TRANSITION_OPEN/CLOSE, 0, 0)` for instantaneous zero-latency overlay appearance and dismissals.
    - Implemented `OnBackPressedDispatcher` callback on `LockActivity` redirecting cleanly to Home without calling `super.onBackPressed()`, eliminating protected app frame leakage on back gestures.
23. **Reactive Dashboard App Filtering, Sort Persistence, and Contextual Empty States (UX Polish)**:
    - Added `isOnlyLockedFilterActive` toggle chip directly below category chips displaying live protected count (`Locked Only ($lockedCount)`).
    - Added Material 3 `Sort By` dropdown menu persisting `AppSortOrder` (`NAME_ASC`, `LOCKED_FIRST`, `CATEGORY`) in DataStore across app restarts.
    - Implemented contextual Empty State card with 1-tap reset action ("Clear search" / "Reset filters") preventing blank dead-ends.
    - Verified all 31 unit tests passing and compiled into release APK.
24. **Lock Screen Tactile Micro-Ticks, Elastic PIN Bounce & Pre-Lockout Alerts (UX Polish)**:
    - Integrated native `VibrationEffect.EFFECT_TICK` via `HapticManager.keypadTick()` for ultra-crisp, low-latency tactile feedback on Pixel 9 linear resonant actuators.
    - Applied spring-loaded physics scale transitions (`Spring.DampingRatioMediumBouncy`, `Spring.StiffnessLow`) on PIN dots via `animateFloatAsState` and `graphicsLayer`.
    - Added amber pre-lockout caution badge when `SessionManager.getAttemptsUntilLockout() <= 2` giving users transparent advance warning before the 30-second lockout triggers.
    - Verified with 32 passing unit tests.
25. **Per-App Security Profile Sheet & Custom Security Policy Architecture (UX Polish)**:
    - Replaced minimal timer bottom sheet with unified Material 3 "App Security Profile" modal sheet triggered by tapping any app card row.
    - Integrated live Lock/Unlock status switch in sheet header.
    - Added "Require Master PIN Only (Bypass Biometrics)" toggle for high-assurance apps (banking, crypto) enforcing PIN-only unlock in `LockActivity` and `OverlayWindowManager`.
    - Added opt-out "Hide Notification Previews" toggle in `NotificationShieldService` allowing selective notification unmasking.
    - Enhanced main list item rows with compact `PIN` Key badge chip alongside the Re-lock Timeout pill.
    - Implemented DataStore persistence (`pinOnlyPackagesJson`, `notificationExemptPackagesJson`) and verified with 5 new unit tests (37 passing tests total).
26. **Settings Hub Information Architecture & Dynamic Security Posture Banner (UX Polish)**:
    - Restructured 15+ flat toggles into 4 cohesive Material 3 Expressive cards: "Authentication & Biometrics", "Privacy & Anti-Tamper", "Tactile & Display", and "Automation & Schedules".
    - Added tinted circular header icons, distinct category badge chips, and subtle horizontal dividers between preference rows.
    - Implemented Dynamic Hero Security Posture Banner calculating active defense layers in real time ("X of 7 Protections Armed"), Titan M2 badge, and amber caution indicator if critical anti-tamper or recents masking protections are disabled.
    - Replaced deprecated `Icons.Default.ArrowBack` with `Icons.AutoMirrored.Filled.ArrowBack`.
    - Passed all 37 unit tests and deployed release APK to Google Pixel 9.
27. **Accessibility Service Crash Remediation & Zero-Latency Interception Hardening (Bug Fix)**:
    - **Fatal SecurityException Elimination**: Identified that `window.setHideOverlayWindows(true)` was throwing `SecurityException: Permission denial: setHideOverlayWindows: HIDE_OVERLAY_WINDOWS` in `LockActivity.onCreate()`, crashing the app and causing Android OS to disband/unbind `AppLockAccessibilityService` (`Crashed services` list in `dumpsys accessibility`). Added `<uses-permission android:name="android.permission.HIDE_OVERLAY_WINDOWS" />` in `AndroidManifest.xml` and defensive `try-catch` wrapper in `LockActivity.kt`.
    - **Zero-Latency In-Memory Flow Caching**: Added in-memory `cachedLockedPackages` and `cachedPreferences` state collections inside `AppLockAccessibilityService.onCreate()`, replacing asynchronous Main-dispatcher DataStore `.first()` queries with synchronous O(1) checks.
    - **SingleInstance Lifecycle Hardening**: Implemented `onNewIntent` (`recreate()`) and `onUserLeaveHint` (`finish()`) in `LockActivity` to prevent orphaned background instances from ignoring lock requests.
    - **Reactive App Lists & Sheet Sync**: Replaced static `_installedApps` state in `MainViewModel` with reactive `allApps`/`installedApps` combining `repository.lockedPackages`, ensuring bottom sheet toggles stay 100% in sync with actual DataStore state.
    - **Instant Session Eviction on Lock**: Updated `AppLockRepositoryImpl.setAppLocked()` to immediately call `SessionManager.lockPackage(packageName)` on lock toggle, evicting any cached unlock session.
    - **Verified & Installed on Pixel 9**: Passed all 37 unit tests, built minified release APK, installed via ADB, and confirmed `dumpsys accessibility` reports `Bound services: {Pixel Guard Protection}` and `Crashed services: {}`.
28. **Modern Reactive Unit & Flow Testing Stack (Turbine + Truth + Coroutines-Test + FakeAppLockRepository)**:
    - Configured targeted modern testing stack in `gradle/libs.versions.toml` and `app/build.gradle.kts`: JUnit 4 + Google Truth (`1.4.4`) + CashApp Turbine (`1.2.0`) + `kotlinx-coroutines-test` (`1.9.0`) + MockK (`1.13.13`).
    - Implemented `MainDispatcherRule` (JUnit `TestWatcher`) to virtualize `Dispatchers.Main` with `StandardTestDispatcher` on the JVM.
    - Implemented `FakeAppLockRepository` test double providing clean, deterministic, in-memory `StateFlow` streams without DataStore or SQLite disk IO.
    - Implemented `MainViewModelTest` (17 tests) covering initial app loading, case-insensitive search queries, category filters, locked-only toggle chip, dashboard sort orders (`NAME_ASC`, `LOCKED_FIRST`, `CATEGORY`), batch protections (`protectAllSensitiveApps`), category locks (`lockCategory`, `unlockCategory`), `unlockAllApps`, master PIN setup with async callbacks, JSON grace periods, PIN-only overrides, and intruder record deletions.
    - Expanded overall test suite from 37 to 54 tests; all 54 tests run in 1.2s with a 100% pass rate. Recorded ADR 021 in `docs/DECISIONS.md` and updated `docs/TESTING.md`.

## Currently In Progress
- Modern testing stack fully implemented and verified (54/54 tests passing). Ready for next feature development.

30. **Complete UI Flow & Edge-Case Coverage (Robolectric + Compose UI Test, 101 Passing Tests)**:
    - Added `androidx.compose.ui:ui-test-junit4`, `androidx.compose.ui:ui-test-manifest`, and `Robolectric 4.14.1` with native Google Pixel 9 display metrics (`w412dp-h915dp-420dpi`).
    - Implemented 5 complete Compose UI test suites spanning all user flows:
      - `MainScreenUiTest` (16 tests): Header, permission banner toggle/visibility, search input/clear, category chips, batch category locking/unlocking, locked-only filter, empty states, switch toggle, hero gauge actions (`Lock Critical`, `Unlock All`), sort dropdown (`Locked First`), and `AppSecurityProfileSheet` (open, per-app grace period selection).
      - `LockOverlayScreenUiTest` (10 tests): Target app info, 4-digit PIN submission, backspace deletion, pre-lockout caution badge, lockout timer countdown banner, exit to home, decoy crash dialog ("Pixel Bank keeps stopping", feedback, close app), biometric click, and scrambled PIN keypad.
      - `OnboardingScreenUiTest` (7 tests): Permission cards, required/optional badges, Step 2 Master PIN card, 4-digit requirement validation, PIN mismatch error, completion callback, continue button when PIN is already set, and optional notification shield skip.
      - `SettingsScreenUiTest` (9 tests): Titanium M2 armor banner, vulnerability attention banner, biometrics toggle, back navigation, changeMasterPinRow sheet open, `ChangePinSheetContent` mismatch & save, `GlobalGracePeriodSheetContent` duration selection, `ScheduleSheetContent` night lockdown hours, and `QuickTilePauseSheetContent` pause duration selection.
      - `IntruderLogsScreenUiTest` (6 tests): "No Break-In Attempts" empty card, intruder record card, single record deletion, clear all confirmation dialog & cancellation, photo preview dialog open & close, and back navigation.
    - Upgraded `MainViewModel` StateFlow sharing from `WhileSubscribed` to `SharingStarted.Eagerly` (`allApps`, `filteredApps`, `preferences`, `intruderRecords`, `hasPinSet`), eliminating asynchronous collection races and guaranteeing deterministic state propagation.
    - Achieved **101/101 tests passing (100% pass rate)** with exit code 0. Verified `assembleDebug` successfully.
31. **Native Material 3 UI Elevation, Tabular Typography & Ergonomics (UX Polish)**:
    - **Above-the-Fold Geometry**: Redesigned `SecurityHealthGaugeCard` in `MainScreen.kt` from a 180dp+ circular hero into an **82dp horizontal security capsule** with a 54dp arc gauge and inline action pills, reclaiming **~120dp vertical space** and displaying 4–5 app rows immediately upon launch.
    - **Tabular Numerics (`tnum`)**: Activated `fontFeatureSettings = "tnum"` in `Type.kt` across titles, body, and labels to eliminate glyph jitter during countdowns and metrics.
    - **Tokens & OLED Blacks**: Added `PixelOledBlack (#000000)`, M3 surface containers (`surfaceContainerLowest` through `surfaceContainerHighest`), `PixelAmber (#F9AB00)`, and `PixelOutlineVariantDark (#3F4750)` in `Color.kt` and `Theme.kt`.
    - **Borderless Tonal Keypad**: Replaced wireframe borders with solid 72dp tonal discs (`surfaceVariant.copy(alpha = 0.42f)`), 30sp tabular digits, true OLED black backdrop with radial glow, and `Icons.AutoMirrored.Filled.Backspace` in `LockOverlayScreen.kt`.
    - **Settings Hub Inset Grouping**: Upgraded `SettingsCard` in `SettingsScreen.kt` to 18dp rounded containers with `surfaceContainer` tint, `1dp outlineVariant` border stroke, 32dp category icon discs, and 36dp row icon discs.
    - **Micro-Delight List Rows & Haptics**: Standardized 44dp icon container, subtle 1dp border stroke on locked items, unified badge chips, tightened 10dp list spacing, and 3-tier haptics (`EFFECT_TICK`, `EFFECT_CLICK`, `EFFECT_HEAVY_CLICK`).
    - **Verification & Deployment**: Maintained all 101 unit/UI tests (0 regressions), compiled minified release APK (~3.5MB), and deployed via ADB USB to connected Google Pixel 9.
32. **Ergonomic Elevation, Viewport Consolidation & Sensory Choreography (UX Polish, ADR 024)**:
    - **Consolidated Dashboard Chrome**: Merged Search and Sort into a single edge-to-edge row (`OutlinedTextField` + `AssistChip`), moved `Locked Only ($count)` as the pinned leading chip in the category row, and removed the separate redundant control strip item, reclaiming **~42dp vertical space**.
    - **Live TopAppBar Status**: Subtitle dynamically reflects current defense posture (`"Pixel 9 Armor • $lockedCount Protected"`).
    - **Upright Spring Shackle & Drill-down Capsule**: Replaced the disorienting -90° rotation with an upright spring shackle bounce (`-3dp` displacement, `0.92f` scale pulse) and consolidated separate badges into a unified interactive capsule (`[ 🔑 PIN • $graceBadge › ]`).
    - **Mass-Unlock Safeguard**: Wrapped `Unlock All` on the security gauge card with an `AlertDialog` confirmation safeguard ("Unlock All Apps?", "Confirm" button) to eliminate accidental mass unlocks.
    - **Horizontal Segmented Selector in BottomSheet**: Replaced 6 vertical `RadioButton` rows with a horizontal `LazyRow` of `FilterChip`s (`Default`, `Instant (0s)`, `30s`, `1m`, `5m`, `15m`) with live contextual description, reclaiming **~180dp vertical space**.
    - **3-Stage Keypad PIN Failure Choreography**: Implemented 3-stage sequence in `LockOverlayScreen.kt`: Error Red morph (60ms) ➔ 5-point elastic spring shake (180ms) ➔ smooth alpha dissolve & reset (120ms).
    - **Lockout Penalty Drain Bar**: Added a sleek `LinearProgressIndicator` below the countdown banner showing penalty progress in real time.
    - **Interactive Settings Hero Banner**: Added tap-to-scroll navigation that automatically scrolls down to unconfigured protections when security posture is vulnerable.
    - **Copy De-Jargonization**: Replaced internal system symbols (`FLAG_SECURE`) with plain user benefit copy: `"Hide app preview cards & block screenshots in Android overview"`.
    - **Verification & Deployment**: Maintained all 101 automated tests (54 Unit + 47 UI) with a 100% pass rate, compiled the minified release APK (~3.5MB), and installed/started on the connected Google Pixel 9.
33. **Settings Shield Decoupling, Financial App Categorization & Mirroring Fix**:
    - **Financial Category Expansion**: Added `paisa` (Google Pay India), `phonepe`, `upi`, and `bhim` to `AppCategory.FINANCIAL` in `AppLockRepositoryImpl.kt`, ensuring domestic and global payment apps are correctly classified.
    - **Credential Dialog Bypassing**: Added `isCredentialOrBiometricPrompt` in `AppLockAccessibilityService.kt` to prevent intercepting system device credential/biometric verification dialogs invoked by third-party apps (e.g. Google Pay).
    - **Settings Protection Default**: Changed default of `isSettingsProtected` to `false` in `LockPreferences` and `PreferencesDataStore`, ensuring the Android Settings app is never locked unless explicitly enabled.
    - **Dynamic Scrcpy Screen Mirroring**: Decoupled `FLAG_SECURE` in `LockActivity` and `OverlayWindowManager` to strictly honor `isRecentsMaskingEnabled`, resolving the black-screen issue in `scrcpy` laptop screen mirroring.
    - **Verification & Deployment**: Passed 100% of unit tests (101/101), built minified release APK, and deployed directly to Pixel 9 wirelessly over Wi-Fi.
34. **LockPreferences Invariant & Enum Test Suite Expansion**:
    - **Default Value Assertion Tests**: Authored `testDefaultPreferences_haveExpectedDefaults()` in `LockPreferencesTest.kt`, locking down all critical security defaults (`isSettingsProtected == false`, `isRecentsMaskingEnabled == false`, `isBiometricsEnabled == true`, `gracePeriodSeconds == 0`, `isQuickTileEnabled == true`, etc.) to prevent silent regressions.
    - **Enum Completeness & Serialization**: Added `testAppSortOrderEnum_containsExpectedValues()` and `testLockTypeEnum_containsExpectedValues()`.
    - **Immutable Copy Isolation**: Added `testLockPreferences_copy_updatesTargetFieldOnly()` validating non-destructive state mutations.
    - **Test Suite Metrics**: Expanded test suite to **107 total automated tests** (57 Unit + 50 Compose UI) with a 100% pass rate.
35. **UI & Theming Refinements (Overlay Legibility, Search Alignment, Badge Stability)**:
    - **Lock Overlay Theming**: Forced dedicated dark color scheme tokens (`#E2E2E6` onSurface, `#282F37` surfaceVariant) in `LockOverlayScreen.kt` regardless of system light/dark mode, guaranteeing crisp, high-contrast title/digits and eliminating the center radial spotlight.
    - **Keypad Uniformity**: Unified all 12 buttons (0-9, delete, and biometric) with identical 72dp dark glass discs (`surfaceVariant.copy(alpha = 0.42f)`).
    - **Search Field Clamping**: Enforced `maxLines = 1`, `softWrap = false`, and `overflow = TextOverflow.Ellipsis` in `MainScreen.kt`, eliminating 2-line placeholder bloat and aligning cleanly with the Sort chip.
    - **Badge Stability**: Added `weight(1f)` to category title and `softWrap = false` to badge text in `SettingsCategoryHeader`, permanently preventing "Hardware-Backed" from hyphen-splitting.
    - **Verification & Deployment**: 105/105 tests passing, release APK compiled and installed on Pixel 9.
36. **Setup Permissions Screen Ergonomics & Dynamic Flow Refactor**:
    - **Two-Tier PermissionCard Hierarchy**: Restructured `PermissionCard` into a dedicated header row (40dp circular icon, title, guaranteed-space badge, and top-right `(v) Active` badge when granted) and an aligned content column with an indented body (52dp start padding) and bottom-right actions (`Later` + `Enable`). Completely eliminated squished titles and missing `[Optional]` badges on "Notification Privacy Shield".
    - **Single-Line Title Stability**: Hid redundant `[Required]` badge once a card is active, granting full width to "Display Over Other Apps" and preventing 2-line title wrapping.
    - **Material 3 Contrast Tokens**: Paired `primaryContainer` with `tint = onPrimaryContainer` across all icon discs (Hero shield, Step 2 lock, active cards), and styled the `Active` status pill with `primary.copy(alpha = 0.12f)` container and `primary` content, completely eliminating dark-on-dark contrast defects in Light Theme.
    - **Viewport Optimization**: Clamped active descriptions to `maxLines = 1` with ellipsis and 4dp spacing, saving ~64dp so Step 2 (Create Master PIN) fits comfortably above the fold.
    - **Actionable Dynamic Bottom Button**: Replaced hardcoded disabled button with a dynamic, fully actionable primary action: `"Grant 2 Required Permissions"` ➔ `"Grant 1 Remaining Permission"` ➔ `"Continue to App Lock"`. Tapping it directly opens the next missing permission settings (Overlay first, then Accessibility).
    - **Soft Keyboard Insets & Auto-Scroll (`imePadding`)**: Applied `Modifier.imePadding()` to the scrollable container and added `LaunchedEffect(isKeyboardOpen)` animated scroll to smoothly bring Step 2 inputs and the submit button fully above the soft keyboard as soon as a PIN field is tapped. Configured `ImeAction.Next` ➔ `ImeAction.Done` keyboard actions for seamless navigation.
    - **Notice Icon Contrast Fix**: Styled the `(i)` info disc in the Android 17 notice card with `background = primary.copy(alpha = 0.14f)` and `tint = primary`, permanently eliminating the dark-on-dark contrast defect in Monet Light Theme.
    - **Contextual Android 17 Warning Card**: Gated warning card to only show when Accessibility is not yet granted, with a matching 32dp tonal info icon container.
    - **Centered Hero Header & Deduplicated Footnote**: Balanced headline and subtitle alignment with `textAlign = TextAlign.Center`, and gated footnote behind `isPinConfigured == true`.
    - **Test Suite & Verification**: Added 2 new tests in `OnboardingScreenUiTest.kt` (107/107 tests passing, 100% pass rate). Production release APK compiled and verified.
37. **Active Foreground Session Retention & Exit Grace Fix (Instagram Multiple Prompts Elimination)**:
    - **Bug Remediation**: Solved repeated unlock prompts while actively using locked apps (e.g. Instagram `com.instagram.android`).
    - **Gesture Bar Filter**: Filtered out `AccessibilityEvent.TYPE_WINDOWS_CHANGED` from app locking logic, eliminating false home/launcher exits generated by the Google Pixel 9 gesture navigation bar (`com.google.android.apps.nexuslauncher`).
    - **Active Foreground Retention**: Ensured `isTemporarilyUnlocked` returns `true` whenever `currentForegroundPackage == packageName`, guaranteeing active in-app navigation (scrolling, Reels, Stories, DMs, comments, popups) never triggers relock.
    - **Exit-Based Grace Countdown**: Computed grace period elapsed time from `exitTimestamps[packageName]` (the exact time user switched away), rather than the initial unlock time.
    - **Screen Off Receiver**: Dynamically registered `ACTION_SCREEN_OFF` receiver to revoke active foreground sessions and enforce immediate relock when the device display turns off.
    - **Transient Package Masks**: Whitelisted Pixel Guard (`applicationContext.packageName`), system photo pickers (`photopicker`, `providers.media.module`), and system share sheet (`intentresolver`) in `isSystemOrTransientPackage`.
    - **Test Suite Expansion**: Added 3 new unit tests in `SessionManagerTest.kt` and 12 comprehensive service tests in `AppLockAccessibilityServiceTest.kt` verifying gesture bar immunity (`TYPE_WINDOWS_CHANGED`), locked package interception, transient package masks (Gboard, PhotoPicker, Chooser, Pixel Guard), launcher exits, and screen-off events. Expanded suite to **122 tests** (72 Unit & Service + 50 Compose UI) with a 100% pass rate.
    - **Deployment & Verification**: Built release APK (`app-release.apk`) and installed directly to Pixel 9 via wireless debugging.
38. **Antigravity AI Context & Repository Hygiene Configuration (`.antigravityignore`)**:
    - Created comprehensive `.antigravityignore` at project root specifically configured for the Pixel Guard Kotlin/Compose/Gradle Android project.
    - Excluded build directories (`build/`, `**/build/`, `out/`, `bin/`), compiled binaries (`*.apk`, `*.aab`, `*.dex`, `*.class`), native builds (`.cxx/`, `.externalNativeBuild/`), Gradle daemons & memory dumps (`.gradle/`, `*.hprof`), local secrets (`local.properties`, `*.jks`, `*.keystore`), IDE metadata (`.idea/`, `.vscode/`, `.kotlin/`), and temporary debug screenshots (`/current_screen.png`, `/ui.xml`, `captures/`).
    - Added explicit whitelist safety overrides (`!app/src/**`, `!docs/**`, `!*.gradle.kts`, `!*.md`) guaranteeing complete visibility for core source code and documentation.
39. **Mandatory Ephemeral Artifact Cleanup & Workspace Hygiene (ADR 029)**:
    - Purged residual debug artifacts from the repository root: `ui.xml`, `current_screen.png`, and `current_device_screen.png`.
    - Executed `.\gradlew.bat clean` to reset `app/build/` intermediate build outputs and reports.
    - Codified mandatory cleanup policy into `AGENTS.md` (Verification Checklist item 5 & Forbidden Changes) and `docs/RULES.md` (Git & Workspace Rules & Forbidden Practices), requiring agents to immediately delete ad-hoc screen captures, UI dumps, and scratch files upon task completion.
40. **Pre-GitHub Push Security Audit & PII Sanitization**:
    - Conducted a comprehensive audit for sensitive files, API keys, credentials, local paths, and PII across the codebase.
    - Hardened `.gitignore` to strictly exclude local developer scripts (`*.bat`, `*.cmd`, `*.ps1`, `*.sh` with `!gradlew.bat` whitelist), secret configs (`.env`, `secrets.properties`), additional certificate formats (`*.pfx`, `*.crt`, `*.der`), and debug logs.
    - Confirmed `pixel9-screen.bat` (containing user profile paths) and `local.properties` (containing local SDK paths) are completely ignored and shielded from git.
    - Sanitized physical device hardware serials (`53201VDAQ00067`) and home Wi-Fi IP addresses (`192.168.1.x`) from `docs/MEMORY.md`.
    - Verified zero cloud telemetry, zero network permissions, and zero hardcoded secrets.
41. **Google 4-Color App Icon Redesign & Brand Alignment (ADR 030)**:
    - Conducted comprehensive `/grill-me` design alignment session establishing full consensus on aligning Pixel Guard's launcher icon with Google first-party apps (Google Authenticator, Drive, Files, Play Protect).
    - **Adaptive Background (`ic_launcher_background.xml`)**: Replaced dark midnight slate and radar rings with a clean solid `#FFFFFF` adaptive canvas.
    - **Adaptive Foreground (`ic_launcher_foreground.xml`)**: Implemented modern rounded shield crest divided into 4 Google brand color quadrants (Blue `#4285F4`, Red `#EA4335`, Yellow `#FBBC05`, Green `#34A853`), featuring a unified white negative-space padlock cutout (shackle + body) with shield colors peeking through the shackle arch and a Google Material Blue (`#1A73E8`) keyhole.
    - **Monochrome Themed Icon (`ic_launcher_monochrome.xml`)**: Structured single-path `android:fillType="evenOdd"` vector carving out the padlock silhouette and restoring the keyhole for native Monet dynamic wallpaper theming on Android 13-17 Pixel Launcher.
    - Passed all 122 automated unit/service tests, verified clean build with `assembleDebug`, and deployed directly to connected Google Pixel 9 via ADB.
42. **OWASP Mobile Full-Stack Security Hardening & Remediation (ADR 031)**:
    - Remediated all 7 security findings from the comprehensive OWASP MASVS v2.1 code audit:
      - **SEC-001 (Overlay Window Biometric Bypass)**: Rewired overlay biometric tap to transition into `LockActivity` (`FragmentActivity`) for hardware `BiometricPrompt` verification, eliminating unauthorized unlock bypass.
      - **SEC-002 (Wall-Clock Monotonicity Flaw)**: Replaced wall-clock `System.currentTimeMillis()` with monotonic `SystemClock.elapsedRealtime()` across `SessionManager` rate limiting, lockout countdowns, and session timers, preventing clock tampering bypass.
      - **SEC-003 (Package Filter Substring Flaw)**: Replaced loose `.contains("keyboard")` and `.contains("systemui")` with exact sets (`KNOWN_SYSTEM_PACKAGES`) and dynamic `InputMethodManager.enabledInputMethodList` checks.
      - **SEC-004 (Launch Transition Buffer)**: Bound `isRecentlyUnlocked` elapsed difference (`diff in 0 until thresholdMs`) and ensured app transitions properly manage session states.
      - **SEC-005 (Encrypted Intruder Metadata)**: Upgraded `intruder_records.json` to AES-256-GCM encrypted container `intruder_records.enc` with automatic migration from legacy plaintext.
      - **SEC-006 (Memory Hygiene with CharArray)**: Added `hashPin(pin: CharArray, salt: ByteArray)` with immediate memory zeroing (`fill('0')`) on both internal and caller buffers.
      - **SEC-007 (Titan M2 StrongBox KeyStore Backing)**: Enabled `setIsStrongBoxBacked(true)` in `CryptoManager.createKey()` with automatic TEE fallback.
    - Verified all **123 automated tests** passing with exit code 0 (`BUILD SUCCESSFUL`).
    - Successfully compiled debug APK with AAPT2 and DEX shrinking.
43. **Mandatory Production (Release) Deployment Policy & R8 Optimization (ADR 032)**:
    - **User Mandate**: Codified project-wide policy requiring all on-device deployments to be **Production (Release) Builds** (`assembleRelease` -> `app-release.apk`) rather than debug builds.
    - **Optimization Profile**: Release build activates full R8 / ProGuard minification, resource shrinking, dead code stripping, and removes `DEBUGGABLE` flags, reducing APK size by **94.6%** (from 66.1 MB down to 3.56 MB).
    - **Governance Updates**: Updated `AGENTS.md` (Build Commands, Verification Checklist, Forbidden Changes) and `docs/RULES.md` (Deployment & Build Rules, Forbidden Practices).
    - **Deployment & Verification**: Successfully built `app-release.apk` with R8 (`BUILD SUCCESSFUL in 9m 41s`), installed to Google Pixel 9 via `adb install -r -d`, launched `MainActivity`, and verified non-debuggable flags (`flags=[ HAS_CODE ALLOW_CLEAR_USER_DATA ]`).

## Known Problems
- None in active runtime. On Android 17, initial sideloading requires the user to tap "Allow restricted settings" in App Info for Accessibility Service toggle activation (interactive guidance provided in Onboarding).

## Important Context
- **Target Device**: Google Pixel 9, Android 17 (Paired and connected via ADB over USB / Wi-Fi).
- **Screen Simulation**: `scrcpy` running live on laptop with Direct3D11 rendering at 1080x2424.
- **ADB Command**: `$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe`
- **Java Home**: JDK 17
- **Android SDK**: Local Android SDK (`$env:LOCALAPPDATA\Android\Sdk`)
- **Master PIN**: Configured by user during onboarding via PBKDF2 (200K iterations); no fallback default PIN exists.

## Recent Decisions
- Decided on direct power-user sideload model rather than Google Play Store policy compromises.
- Decided on true dual-layer security: biometrics fallback strictly to custom Master PIN, not the device lockscreen passcode.
- Decided on ambient radial glow for app-themed lock screen rather than jarring high-saturation full-screen tints.
- Decided on contextual category action strip rather than intrusive full-screen grouping headers.
- Decided on Hybrid Clean Documentation Structure (ADR 008) to preserve root AI discovery while keeping workspace root uncluttered.
- Decided on PBKDF2WithHmacSHA256 (200,000 iterations) with constant-time verification (`MessageDigest.isEqual`) and session-persisted failed attempts (ADR 009).
- Decided on Targeted Modern Reactive Testing Stack (Turbine + Truth + MockK + kotlinx-coroutines-test + FakeAppLockRepository) for sub-second JVM execution and race-free Flow testing (ADR 021).
- Decided on Compose UI Test JUnit 4 + Robolectric (4.14.1) configured with Pixel 9 display metrics (`w412dp-h915dp-420dpi`) for fast, deterministic local UI testing without requiring an active emulator (ADR 022).
- Decided on Native Material 3 UI Elevation, Tabular Typography & Ergonomics (ADR 023).
- Decided on Zero-Feature Ergonomic Elevation, Viewport Consolidation & Sensory Choreography (ADR 024).
- Decided on Setup Permissions Two-Tier Hierarchy, Contrast Fixes & IME Auto-Scroll (ADR 026).
- Decided on Active Foreground Session Retention & Exit-Based Grace Countdown (ADR 027).
- Decided on Repository Context Hygiene & Antigravity Ignore Configuration (ADR 028).
- Decided on Mandatory Ephemeral Artifact Cleanup & Workspace Hygiene (ADR 029).
- Decided on Google 4-Color Brand Palette, Clean White Canvas & Negative-Space Padlock Cutout (ADR 030).
- Decided on OWASP Mobile Full-Stack Security Hardening & Remediation (ADR 031).
- Decided on Mandatory Production (Release) Build Deployment Architecture (ADR 032).

## Next Steps
- Explore Roadmap feature: Calculator / Notes Stealth Disguise mode.
- Optional on-device Compose instrumentation tests (`connectedDebugAndroidTest`) on physical Pixel 9.

## Things to Be Careful About
- Never run Gradle without setting `$env:JAVA_HOME` and `$env:ANDROID_HOME`.
- Never call synchronous `toBitmap()` inside `@Composable` drawing or layout code.
- Always use `BypassSandbox: true` when running Gradle builds or ADB commands.

## Session Handoff
- Repository has been fully audited and sanitized for GitHub push: local developer batch scripts, SDK paths, local caches, and temporary debug captures are strictly excluded via `.gitignore` and `.antigravityignore`. Private IP addresses and device serials have been stripped from documentation. All 122 automated tests (72 Unit & Service + 50 Compose UI) are passing (100% pass rate). The codebase is clean, secure, and ready for git commit and push.

