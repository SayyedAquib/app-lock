package com.pixelguard.applock.ui.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.foundation.selection.selectable
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.MotionPhotosOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pixelguard.applock.ui.main.MainViewModel
import com.pixelguard.applock.ui.theme.semanticColors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit
) {
    val prefs by viewModel.preferences.collectAsState()
    var showChangePinSheet by remember { mutableStateOf(false) }
    var showGracePeriodSheet by remember { mutableStateOf(false) }
    var showScheduleSheet by remember { mutableStateOf(false) }
    var showQuickTilePauseSheet by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "Protection Settings", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        val activeShields = remember(prefs) {
            listOf(
                prefs.isBiometricsEnabled,
                prefs.isRecentsMaskingEnabled,
                prefs.isNotificationShieldEnabled,
                prefs.isIntruderSelfieEnabled,
                prefs.isSettingsProtected,
                prefs.isPlayStoreProtected,
                prefs.isScheduleEnabled || prefs.isScramblePinEnabled
            ).count { it }
        }
        val hasSecurityVulnerability = remember(prefs) {
            !prefs.isSettingsProtected || !prefs.isRecentsMaskingEnabled
        }

        val scrollState = rememberScrollState()
        val coroutineScope = rememberCoroutineScope()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Dynamic Hero Security Status Banner (Interactive shortcut on vulnerability)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (hasSecurityVulnerability) {
                            Modifier.clickable {
                                coroutineScope.launch {
                                    scrollState.animateScrollTo(450)
                                }
                            }
                        } else Modifier
                    ),
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(
                    1.dp,
                    if (hasSecurityVulnerability) MaterialTheme.semanticColors.amber.copy(alpha = 0.35f)
                    else MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                ),
                colors = CardDefaults.cardColors(
                    containerColor = if (hasSecurityVulnerability)
                        MaterialTheme.semanticColors.amberContainer.copy(alpha = 0.28f)
                    else
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.40f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(
                                if (hasSecurityVulnerability) MaterialTheme.semanticColors.amber else MaterialTheme.colorScheme.primary
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (hasSecurityVulnerability) Icons.Default.WarningAmber else Icons.Default.Shield,
                            contentDescription = null,
                            tint = if (hasSecurityVulnerability) MaterialTheme.semanticColors.onAmber else MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (hasSecurityVulnerability) "Attention Needed" else "Hardware Armor Active",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "Titan M2",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        Text(
                            text = if (hasSecurityVulnerability)
                                "Enable Settings Protection & Recents Masking to prevent bypass"
                            else
                                "$activeShields of 7 Protections Armed • Pixel 9 Biometrics Enforced",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        if (hasSecurityVulnerability) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Button(
                                onClick = {
                                    viewModel.armRecommendedShields()
                                },
                                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.semanticColors.amber,
                                    contentColor = MaterialTheme.semanticColors.onAmber
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(38.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Shield,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Arm Recommended Shields",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }

            // 2. Card 1: Authentication & Biometrics
            SettingsCard {
                SettingsCategoryHeader(
                    title = "Authentication & Biometrics",
                    icon = Icons.Default.Fingerprint,
                    badgeText = "Hardware-Backed"
                )

                SettingsSwitchRow(
                    title = "Pixel 9 Biometrics",
                    subtitle = "Use ultrasonic fingerprint & Face Unlock to unlock apps instantly",
                    icon = Icons.Default.Fingerprint,
                    checked = prefs.isBiometricsEnabled,
                    onCheckedChange = { viewModel.updatePreferences(prefs.copy(isBiometricsEnabled = it)) }
                )

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 12.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                )

                SettingsActionRow(
                    title = "Change Master PIN",
                    subtitle = "Update the 4-digit master PIN backup (PBKDF2 200K iterations)",
                    icon = Icons.Default.Key,
                    onClick = { showChangePinSheet = true }
                )

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 12.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                )

                SettingsSwitchRow(
                    title = "Scramble PIN Keypad",
                    subtitle = "Randomize numeric button positions on every unlock to defeat shoulder surfers",
                    icon = Icons.Default.Shield,
                    checked = prefs.isScramblePinEnabled,
                    onCheckedChange = { viewModel.updatePreferences(prefs.copy(isScramblePinEnabled = it)) }
                )
            }

            // 3. Card 2: Privacy & Anti-Tamper
            SettingsCard {
                SettingsCategoryHeader(
                    title = "Privacy & Anti-Tamper",
                    icon = Icons.Default.Shield,
                    badgeText = "Defense-in-Depth"
                )

                SettingsSwitchRow(
                    title = "Recents Privacy Masking",
                    subtitle = "Hide app preview cards & block screenshots in Android overview",
                    icon = Icons.Default.VisibilityOff,
                    checked = prefs.isRecentsMaskingEnabled,
                    onCheckedChange = { viewModel.updatePreferences(prefs.copy(isRecentsMaskingEnabled = it)) }
                )

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 12.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                )

                SettingsSwitchRow(
                    title = "Notification Privacy Shield",
                    subtitle = "Hide sensitive message previews from locked apps in status bar notifications",
                    icon = Icons.Default.Notifications,
                    checked = prefs.isNotificationShieldEnabled,
                    onCheckedChange = { viewModel.updatePreferences(prefs.copy(isNotificationShieldEnabled = it)) }
                )

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 12.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                )

                SettingsSwitchRow(
                    title = "Intruder Selfie",
                    subtitle = "Silently snap front camera photo after failed unlock attempts",
                    icon = Icons.Default.CameraAlt,
                    checked = prefs.isIntruderSelfieEnabled,
                    onCheckedChange = { viewModel.updatePreferences(prefs.copy(isIntruderSelfieEnabled = it)) }
                )

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 12.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                )

                SettingsSwitchRow(
                    title = "Settings & Package Protection",
                    subtitle = "Require PIN to access Android Settings, App Info, or package uninstallers (Pixel & AOSP)",
                    icon = Icons.Default.Security,
                    checked = prefs.isSettingsProtected,
                    onCheckedChange = { viewModel.updatePreferences(prefs.copy(isSettingsProtected = it)) }
                )

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 12.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                )

                SettingsSwitchRow(
                    title = "Prevent Play Store Tampering",
                    subtitle = "Require authentication before opening Google Play Store to stop unauthorized uninstallation",
                    icon = Icons.Default.Shield,
                    checked = prefs.isPlayStoreProtected,
                    onCheckedChange = { viewModel.updatePreferences(prefs.copy(isPlayStoreProtected = it)) }
                )

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 12.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                )

                SettingsSwitchRow(
                    title = "Fake Crash Decoy",
                    subtitle = "Show fake 'App has stopped' dialog before unlocking (long press to reveal PIN)",
                    icon = Icons.Default.VisibilityOff,
                    checked = prefs.isDecoyCrashEnabled,
                    onCheckedChange = { viewModel.updatePreferences(prefs.copy(isDecoyCrashEnabled = it)) }
                )
            }

            // 4. Card 3: Tactile & Display Polish
            SettingsCard {
                SettingsCategoryHeader(
                    title = "Tactile & Display",
                    icon = Icons.Default.Palette,
                    badgeText = "Pixel 9"
                )

                SettingsSwitchRow(
                    title = "App-Themed Material You",
                    subtitle = "Ambient radial brand glow & accents dynamically tailored to the locked app",
                    icon = Icons.Default.Palette,
                    checked = prefs.isDynamicAppThemingEnabled,
                    onCheckedChange = { viewModel.updatePreferences(prefs.copy(isDynamicAppThemingEnabled = it)) }
                )

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 12.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                )

                SettingsSwitchRow(
                    title = "Pixel Haptic Feedback",
                    subtitle = "Tactile linear actuator micro-ticks on keypad & auth feedback (respects system haptics)",
                    icon = Icons.Default.Vibration,
                    checked = prefs.isHapticsEnabled,
                    onCheckedChange = { viewModel.updatePreferences(prefs.copy(isHapticsEnabled = it)) }
                )

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 12.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                )

                SettingsSwitchRow(
                    title = "Fluid Micro-Animations",
                    subtitle = "Spring-loaded physics, lock rotation & pulse (honors Android 'Remove animations')",
                    icon = Icons.Default.MotionPhotosOn,
                    checked = prefs.isAnimationsEnabled,
                    onCheckedChange = { viewModel.updatePreferences(prefs.copy(isAnimationsEnabled = it)) }
                )
            }

            // 5. Card 4: Automation & Schedules
            SettingsCard {
                SettingsCategoryHeader(
                    title = "Automation & Schedules",
                    icon = Icons.Default.Timer,
                    badgeText = "Smart Rules"
                )

                val graceLabel = when (prefs.gracePeriodSeconds) {
                    0 -> "Immediately upon leaving app or screen off"
                    30 -> "30 seconds grace period"
                    60 -> "1 minute grace period"
                    300 -> "5 minutes grace period"
                    else -> "${prefs.gracePeriodSeconds} seconds"
                }

                SettingsActionRow(
                    title = "Default Lock Frequency",
                    subtitle = graceLabel,
                    icon = Icons.Default.Timer,
                    onClick = { showGracePeriodSheet = true }
                )

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 12.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                )

                SettingsSwitchRow(
                    title = "Night Lockdown Schedule",
                    subtitle = "Enforce strict 0-second re-lock schedule automatically during nighttime hours",
                    icon = Icons.Default.Bedtime,
                    checked = prefs.isScheduleEnabled,
                    onCheckedChange = { viewModel.updatePreferences(prefs.copy(isScheduleEnabled = it)) }
                )

                if (prefs.isScheduleEnabled) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showScheduleSheet = true }
                            .padding(start = 52.dp, top = 4.dp, bottom = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Active hours: ${formatHour(prefs.scheduleStartHour)} – ${formatHour(prefs.scheduleEndHour)} (Tap to configure)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 52.dp, end = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Require Master PIN Only at Night",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Disables Fingerprint & Face Unlock while scheduled to protect while asleep",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = prefs.isNightLockdownPinOnly,
                            onCheckedChange = { viewModel.updatePreferences(prefs.copy(isNightLockdownPinOnly = it)) }
                        )
                    }
                }

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 12.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                )

                SettingsSwitchRow(
                    title = "Quick Settings Tile",
                    subtitle = "Status bar tile to pause protection (requires authentication before pausing)",
                    icon = Icons.Default.Dashboard,
                    checked = prefs.isQuickTileEnabled,
                    onCheckedChange = { viewModel.updatePreferences(prefs.copy(isQuickTileEnabled = it)) }
                )

                if (prefs.isQuickTileEnabled) {
                    val durationLabel = when (prefs.quickTilePauseMinutes) {
                        -1 -> "Until manually resumed"
                        5 -> "5 minutes"
                        15 -> "15 minutes (Default)"
                        30 -> "30 minutes"
                        60 -> "1 hour"
                        else -> "${prefs.quickTilePauseMinutes} minutes"
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showQuickTilePauseSheet = true }
                            .padding(start = 52.dp, top = 2.dp, bottom = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Pause Duration: $durationLabel (Tap to configure)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 12.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                )

                SettingsSwitchRow(
                    title = "Per-App Custom Grace Periods",
                    subtitle = "Show individual timeout badges ('0s', '1m', '5m') for each app on the main screen",
                    icon = Icons.Default.HourglassTop,
                    checked = prefs.isPerAppGraceEnabled,
                    onCheckedChange = { viewModel.updatePreferences(prefs.copy(isPerAppGraceEnabled = it)) }
                )

                if (prefs.isPerAppGraceEnabled) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.updatePreferences(prefs.copy(appGracePeriodsJson = "{}"))
                            }
                            .padding(start = 52.dp, top = 4.dp, bottom = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Reset all custom app timeouts to default",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // 6. Card 5: Security & Privacy Info (Collapsible FAQ)
            SecurityPrivacyFaqCard()
        }
    }

    // Material 3 Modal Bottom Sheet: Change PIN
    if (showChangePinSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showChangePinSheet = false },
            sheetState = sheetState,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            ChangePinSheetContent(
                onDismiss = { showChangePinSheet = false },
                onSavePin = { pin ->
                    viewModel.setMasterPin(pin)
                    showChangePinSheet = false
                },
                onVerifyCurrentPin = { pin ->
                    viewModel.verifyMasterPinForChange(pin)
                }
            )
        }
    }

    // Material 3 Modal Bottom Sheet: Default Lock Frequency
    if (showGracePeriodSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showGracePeriodSheet = false },
            sheetState = sheetState,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            GlobalGracePeriodSheetContent(
                currentGracePeriodSeconds = prefs.gracePeriodSeconds,
                onSelectGracePeriod = { seconds ->
                    viewModel.updatePreferences(prefs.copy(gracePeriodSeconds = seconds))
                    showGracePeriodSheet = false
                }
            )
        }
    }

    // Material 3 Modal Bottom Sheet: Night Lockdown Schedule
    if (showScheduleSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showScheduleSheet = false },
            sheetState = sheetState,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            ScheduleSheetContent(
                initialStartHour = prefs.scheduleStartHour,
                initialEndHour = prefs.scheduleEndHour,
                onDismiss = { showScheduleSheet = false },
                onSaveSchedule = { start, end ->
                    viewModel.updatePreferences(
                        prefs.copy(
                            scheduleStartHour = start,
                            scheduleEndHour = end
                        )
                    )
                    showScheduleSheet = false
                }
            )
        }
    }

    // Material 3 Modal Bottom Sheet: Quick Settings Tile Pause Duration
    if (showQuickTilePauseSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showQuickTilePauseSheet = false },
            sheetState = sheetState,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            QuickTilePauseSheetContent(
                currentPauseMinutes = prefs.quickTilePauseMinutes,
                onSelectDuration = { mins ->
                    viewModel.updatePreferences(prefs.copy(quickTilePauseMinutes = mins))
                    showQuickTilePauseSheet = false
                }
            )
        }
    }
}

@Composable
fun ChangePinSheetContent(
    onDismiss: () -> Unit,
    onSavePin: (String) -> Unit,
    onVerifyCurrentPin: (suspend (String) -> Boolean)? = null
) {
    var currentPin by remember { mutableStateOf("") }
    var newPin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var isCurrentPinVerified by remember { mutableStateOf(onVerifyCurrentPin == null) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Key,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(text = "Change Master PIN", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    text = if (!isCurrentPinVerified) "Enter current PIN to confirm identity" else "Enter a new 4-digit master security PIN",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (!isCurrentPinVerified) {
            OutlinedTextField(
                value = currentPin,
                onValueChange = { if (it.length <= 4 && it.all { ch -> ch.isDigit() }) currentPin = it },
                label = { Text("Current 4-Digit PIN") },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            OutlinedTextField(
                value = newPin,
                onValueChange = { if (it.length <= 4 && it.all { ch -> ch.isDigit() }) newPin = it },
                label = { Text("New 4-Digit PIN") },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = confirmPin,
                onValueChange = { if (it.length <= 4 && it.all { ch -> ch.isDigit() }) confirmPin = it },
                label = { Text("Confirm PIN") },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }

        error?.let {
            Text(text = it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
            Spacer(modifier = Modifier.width(8.dp))
            if (!isCurrentPinVerified) {
                Button(
                    onClick = {
                        if (currentPin.length != 4) {
                            error = "Current PIN must be 4 digits"
                            return@Button
                        }
                        scope.launch {
                            val isValid = onVerifyCurrentPin?.invoke(currentPin) ?: true
                            if (isValid) {
                                isCurrentPinVerified = true
                                error = null
                            } else {
                                val remainingLockout = com.pixelguard.applock.domain.usecase.SessionManager.getRemainingLockoutSeconds(com.pixelguard.applock.AppLockApplication.instance.packageName)
                                error = if (remainingLockout > 0) {
                                    "Too many failed attempts. Locked out for ${remainingLockout}s"
                                } else {
                                    "Current PIN is incorrect"
                                }
                            }
                        }
                    },
                    enabled = currentPin.length == 4
                ) {
                    Text("Verify & Continue")
                }
            } else {
                Button(
                    onClick = {
                        if (newPin.length != 4) {
                            error = "PIN must be exactly 4 digits"
                        } else if (newPin != confirmPin) {
                            error = "PINs do not match"
                        } else {
                            onSavePin(newPin)
                        }
                    }
                ) {
                    Text("Save PIN")
                }
            }
        }
    }
}

@Composable
fun GlobalGracePeriodSheetContent(
    currentGracePeriodSeconds: Int,
    onSelectGracePeriod: (Int) -> Unit
) {
    val options = listOf(
        0 to Pair("Immediately", "Lock upon leaving app or turning off screen"),
        30 to Pair("30 seconds", "Short multitasking window before re-locking"),
        60 to Pair("1 minute", "Standard grace period for rapid app switching"),
        300 to Pair("5 minutes", "Extended window for prolonged active sessions")
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Timer,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(text = "Global Relock Frequency", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(text = "Applies to all locked apps unless customized", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        options.forEach { (seconds, details) ->
            val (title, description) = details
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectable(
                        selected = currentGracePeriodSeconds == seconds,
                        onClick = { onSelectGracePeriod(seconds) },
                        role = Role.RadioButton
                    ),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (currentGracePeriodSeconds == seconds)
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                    else
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = currentGracePeriodSeconds == seconds,
                        onClick = null
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = title, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                        Text(text = description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
fun ScheduleSheetContent(
    initialStartHour: Int,
    initialEndHour: Int,
    onDismiss: () -> Unit,
    onSaveSchedule: (startHour: Int, endHour: Int) -> Unit
) {
    var startHour by remember { mutableStateOf(initialStartHour) }
    var endHour by remember { mutableStateOf(initialEndHour) }

    val startHourOptions = listOf(21 to "9:00 PM", 22 to "10:00 PM", 23 to "11:00 PM", 0 to "12:00 AM")
    val endHourOptions = listOf(5 to "5:00 AM", 6 to "6:00 AM", 7 to "7:00 AM", 8 to "8:00 AM")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Bedtime,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(text = "Night Lockdown Schedule", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(text = "Forces instant 0-second relocking during sleep hours", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        // 24-Hour Circular Day/Night Arc Visualizer
        val durationHours = remember(startHour, endHour) {
            if (endHour >= startHour) endHour - startHour
            else (24 - startHour) + endHour
        }
        val trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        val nightTint = MaterialTheme.colorScheme.tertiary
        val sweepAngleTarget = (durationHours / 24f) * 360f
        val animatedSweep by animateFloatAsState(
            targetValue = sweepAngleTarget,
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
            label = "schedule_sweep"
        )
        val startAngle = -90f + (startHour / 24f) * 360f

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(72.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.size(72.dp)) {
                        val stroke = 7.dp.toPx()
                        drawArc(
                            color = trackColor,
                            startAngle = -90f,
                            sweepAngle = 360f,
                            useCenter = false,
                            style = Stroke(width = stroke, cap = StrokeCap.Round)
                        )
                        drawArc(
                            color = nightTint,
                            startAngle = startAngle,
                            sweepAngle = animatedSweep,
                            useCenter = false,
                            style = Stroke(width = stroke, cap = StrokeCap.Round)
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.Bedtime,
                        contentDescription = null,
                        tint = nightTint,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "$durationHours Hours Lockdown",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${formatHour(startHour)} → ${formatHour(endHour)}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = nightTint
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Instant 0s relock enforced every night",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Text(text = "Lockdown Starts At:", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            startHourOptions.forEach { (h, label) ->
                FilterChip(
                    selected = startHour == h,
                    onClick = { startHour = h },
                    label = { Text(label, fontSize = 12.sp) }
                )
            }
        }

        Text(text = "Lockdown Ends At:", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            endHourOptions.forEach { (h, label) ->
                FilterChip(
                    selected = endHour == h,
                    onClick = { endHour = h },
                    label = { Text(label, fontSize = 12.sp) }
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = { onSaveSchedule(startHour, endHour) }
            ) {
                Text("Save Schedule")
            }
        }
    }
}

@Composable
fun QuickTilePauseSheetContent(
    currentPauseMinutes: Int,
    onSelectDuration: (Int) -> Unit
) {
    val pauseOptions = listOf(
        5 to Pair("5 minutes", "Quick temporary pause"),
        15 to Pair("15 minutes (Default)", "Standard balanced break"),
        30 to Pair("30 minutes", "Extended working session"),
        60 to Pair("1 hour", "Prolonged pause"),
        -1 to Pair("Until manually resumed", "Protection remains paused until you tap the tile again")
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Dashboard,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(
                    text = "Quick Tile Pause Duration",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Choose how long protection pauses when tapped in shade",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            pauseOptions.forEach { (mins, details) ->
                val (title, description) = details
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = currentPauseMinutes == mins,
                            onClick = { onSelectDuration(mins) },
                            role = Role.RadioButton
                        ),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (currentPauseMinutes == mins)
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                        else
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = currentPauseMinutes == mins,
                            onClick = null
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun formatHour(hour: Int): String {
    val h = if (hour == 0) 12 else if (hour > 12) hour - 12 else hour
    val amPm = if (hour < 12) "AM" else "PM"
    return "$h:00 $amPm"
}

@Composable
fun SecurityPrivacyFaqCard() {
    // Accordion state: index of currently expanded item, -1 means all collapsed
    var expandedIndex by remember { mutableStateOf(-1) }

    data class FaqItem(val question: String, val answer: String)

    val faqItems = remember {
        listOf(
            FaqItem(
                question = "Why does Android show a \"Review app with full device access\" notification?",
                answer = "This is a standard Android security reminder that appears periodically for ALL apps that use Accessibility Service — including password managers, screen readers, and automation tools. It does NOT mean Pixel Guard is unsafe. Android simply reminds you to review which apps have this permission. You can safely dismiss it."
            ),
            FaqItem(
                question = "Does Pixel Guard read my screen content or personal data?",
                answer = "No. Pixel Guard explicitly sets canRetrieveWindowContent to false in its Accessibility configuration. It only detects which app is in the foreground — it cannot read text, passwords, messages, or any on-screen content whatsoever."
            ),
            FaqItem(
                question = "Why does Pixel Guard need Accessibility Service?",
                answer = "Accessibility Service is the only Android API that provides instant, zero-latency detection of app launches. Without it, Pixel Guard would have to use slow polling methods that drain battery and introduce delays — leaving a window where locked apps could be briefly visible before the lock screen appears."
            ),
            FaqItem(
                question = "Is my data sent anywhere?",
                answer = "Absolutely not. Pixel Guard has zero network permissions, zero analytics SDKs, and zero cloud endpoints. All data — your PIN hash, locked app list, intruder photos, and settings — stays exclusively on your device, protected by Titan M2 hardware encryption."
            )
        )
    }

    SettingsCard {
        SettingsCategoryHeader(
            title = "Security & Privacy Info",
            icon = Icons.Default.Info,
            badgeText = "FAQ"
        )

        Text(
            text = "Common questions about permissions and privacy",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        faqItems.forEachIndexed { index, item ->
            if (index > 0) {
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 4.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                )
            }
            FaqAccordionItem(
                question = item.question,
                answer = item.answer,
                isExpanded = expandedIndex == index,
                onToggle = {
                    expandedIndex = if (expandedIndex == index) -1 else index
                }
            )
        }
    }
}

@Composable
fun FaqAccordionItem(
    question: String,
    answer: String,
    isExpanded: Boolean,
    onToggle: () -> Unit
) {
    val chevronRotation by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "faq_chevron"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() }
            .padding(vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = question,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = Icons.Default.ExpandMore,
                contentDescription = if (isExpanded) "Collapse" else "Expand",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .size(22.dp)
                    .graphicsLayer { rotationZ = chevronRotation }
            )
        }

        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            ),
            exit = shrinkVertically(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            )
        ) {
            Text(
                text = answer,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp, end = 30.dp),
                lineHeight = 18.sp
            )
        }
    }
}

@Composable
fun SettingsCategoryHeader(
    title: String,
    icon: ImageVector,
    badgeText: String? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (badgeText != null) {
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = badgeText,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }
    }
}

@Composable
fun SettingsCard(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            content()
        }
    }
}

@Composable
fun SettingsSwitchRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(19.dp)
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
fun SettingsActionRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(19.dp)
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
