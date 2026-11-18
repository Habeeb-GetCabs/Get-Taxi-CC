package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.service.LocationTrackingService
import com.example.util.AppLanguage
import com.example.util.SangamTariffConfig
import com.example.util.appStrings
import com.example.viewmodel.SettingsViewModel
import java.util.Locale

private val SetBlack = Color(0xFF090A0C)
private val SetCharcoal = Color(0xFF14171C)
private val SetCard = Color(0xFF1E222A)
private val SetBorder = Color(0xFF2D333E)
private val SetYellow = Color(0xFFFFC107)
private val SetWhite = Color(0xFFFFFFFF)
private val SetMuted = Color(0xFF9CA3AF)
private val SetAmber = Color(0xFFF59E0B)

private fun formatDoubleInput(value: Double?): String {
    if (value == null) return ""
    return if (value % 1.0 == 0.0) value.toLong().toString() else value.toString()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToDriverProfile: () -> Unit = {},
    onNavigateToAbout: () -> Unit = {}
) {
    val context = LocalContext.current
    val strings = appStrings()

    val appLanguageCode by viewModel.appLanguage.collectAsStateWithLifecycle()
    val voiceLanguageCode by viewModel.voiceLanguage.collectAsStateWithLifecycle()
    val speedThresholdState by viewModel.speedThreshold.collectAsStateWithLifecycle()
    val audioEnabledState by viewModel.audioEnabled.collectAsStateWithLifecycle()
    val floatingBubbleEnabledState by viewModel.floatingBubbleEnabled.collectAsStateWithLifecycle()
    val currencyState by viewModel.currency.collectAsStateWithLifecycle()
    val distanceCalibrationFactorState by viewModel.distanceCalibrationFactor.collectAsStateWithLifecycle()

    var speedThresholdInput by remember { mutableStateOf("") }
    var distanceCalibrationFactorInput by remember { mutableStateOf("1.000") }
    var audioEnabled by remember { mutableStateOf(true) }
    var floatingBubbleEnabled by remember { mutableStateOf(true) }
    var showSavedMessage by remember { mutableStateOf(false) }

    LaunchedEffect(
        speedThresholdState,
        audioEnabledState,
        floatingBubbleEnabledState,
        distanceCalibrationFactorState
    ) {
        speedThresholdInput = formatDoubleInput(speedThresholdState)
        audioEnabled = audioEnabledState
        floatingBubbleEnabled = floatingBubbleEnabledState
        distanceCalibrationFactorInput = String.format(Locale.US, "%.3f", distanceCalibrationFactorState)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = strings.settingsScreenTitle,
                        color = SetWhite,
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = strings.back,
                            tint = SetYellow
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SetBlack)
            )
        },
        containerColor = SetCharcoal,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    val st = speedThresholdInput.trim().toDoubleOrNull() ?: 5.0
                    val calFactor = distanceCalibrationFactorInput.trim().toDoubleOrNull() ?: 1.0
                    viewModel.updateSpeedThreshold(st)
                    viewModel.updateAudioEnabled(audioEnabled)
                    viewModel.updateFloatingBubbleEnabled(floatingBubbleEnabled)
                    viewModel.updateDistanceCalibrationFactor(calFactor)

                    LocationTrackingService.startMonitoring(
                        context = context,
                        baseFare = 80.0,
                        farePerKm = 28.0,
                        waitFarePerMin = 0.0,
                        currency = currencyState.ifBlank { SangamTariffConfig.CURRENCY_SYMBOL },
                        speedThreshold = st,
                        calibrationFactor = calFactor
                    )
                    showSavedMessage = true
                },
                icon = { Icon(Icons.Default.Save, contentDescription = null, tint = SetBlack) },
                text = { Text(strings.saveSettingsButton, fontWeight = FontWeight.Black, color = SetBlack) },
                containerColor = SetYellow,
                contentColor = SetBlack,
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.testTag("save_settings_fab")
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (showSavedMessage) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF064E3B)),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color(0xFF10B981)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF34D399))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(strings.settingsSavedSuccess, color = SetWhite, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }

            // LANGUAGE SELECTOR
            val isTamilSelected = AppLanguage.fromCode(appLanguageCode).isTamil || strings.isTamil
            Card(
                colors = CardDefaults.cardColors(containerColor = SetCard),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, SetBorder),
                modifier = Modifier.fillMaxWidth().testTag("language_selector_card")
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier.size(38.dp).background(SetYellow, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Language, contentDescription = null, tint = SetBlack)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(strings.languageSectionTitle, color = SetWhite, fontWeight = FontWeight.Black, fontSize = 13.sp)
                            Text(strings.languageSectionSubtitle, color = SetMuted, fontSize = 11.sp)
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        val isEnglishActive = !isTamilSelected
                        Button(
                            onClick = { viewModel.updateAppLanguage("en") },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isEnglishActive) SetYellow else SetBlack,
                                contentColor = if (isEnglishActive) SetBlack else SetWhite
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f).height(48.dp).testTag("language_option_en")
                        ) {
                            Text("English", fontWeight = FontWeight.Black)
                        }
                        Button(
                            onClick = { viewModel.updateAppLanguage("ta") },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isTamilSelected) SetYellow else SetBlack,
                                contentColor = if (isTamilSelected) SetBlack else SetWhite
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f).height(48.dp).testTag("language_option_ta")
                        ) {
                            Text("தமிழ்", fontWeight = FontWeight.Black)
                        }
                    }
                }
            }

            // VOICE ANNOUNCEMENT LANGUAGE SELECTOR
            val isVoiceTamil = AppLanguage.fromCode(voiceLanguageCode).isTamil
            Card(
                colors = CardDefaults.cardColors(containerColor = SetCard),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, SetBorder),
                modifier = Modifier.fillMaxWidth().testTag("voice_language_selector_card")
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier.size(38.dp).background(SetYellow, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.RecordVoiceOver, contentDescription = null, tint = SetBlack)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("VOICE ANNOUNCEMENT LANGUAGE", color = SetWhite, fontWeight = FontWeight.Black, fontSize = 13.sp)
                            Text("Trip start & end voice prompts (English / தமிழ்)", color = SetMuted, fontSize = 11.sp)
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        val isVoiceEnglish = !isVoiceTamil
                        Button(
                            onClick = { viewModel.updateVoiceLanguage("en") },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isVoiceEnglish) SetYellow else SetBlack,
                                contentColor = if (isVoiceEnglish) SetBlack else SetWhite
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f).height(48.dp).testTag("voice_language_option_en")
                        ) {
                            Text("English", fontWeight = FontWeight.Black)
                        }
                        Button(
                            onClick = { viewModel.updateVoiceLanguage("ta") },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isVoiceTamil) SetYellow else SetBlack,
                                contentColor = if (isVoiceTamil) SetBlack else SetWhite
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f).height(48.dp).testTag("voice_language_option_ta")
                        ) {
                            Text("தமிழ்", fontWeight = FontWeight.Black)
                        }
                    }
                }
            }

            // OFFLINE TARIFF ENGINE & AMBIGUITIES CARD
            Card(
                colors = CardDefaults.cardColors(containerColor = SetBlack),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, SetYellow),
                modifier = Modifier.fillMaxWidth().testTag("sangam_tariff_notice_card")
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("GET TAXI • OFFLINE TARIFF ENGINE", color = SetYellow, fontWeight = FontWeight.Black, fontSize = 12.sp)
                    Text("100% Offline • Zero History • CC GPay ${SangamTariffConfig.VENDOR_GPAY_NUMBER}", color = SetMuted, fontSize = 11.sp)
                    Text(
                        text = "Flagged rules awaiting confirmation: 5 KM Min-Bill overlap, ₹60–₹75 local CC range, >25 KM vs >₹800 10% interaction, and Day Rental CC.",
                        color = SetAmber,
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )
                }
            }

            // DRIVER & VEHICLE PROFILE
            Card(
                colors = CardDefaults.cardColors(containerColor = SetCard),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, SetBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Box(
                            modifier = Modifier.size(40.dp).background(SetYellow, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.AccountCircle, contentDescription = null, tint = SetBlack)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(strings.driverVehicleProfileTitle, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = SetWhite)
                            Text(strings.driverVehicleProfileSubtitle, fontSize = 11.sp, color = SetMuted)
                        }
                    }
                    Button(
                        onClick = onNavigateToDriverProfile,
                        colors = ButtonDefaults.buttonColors(containerColor = SetYellow, contentColor = SetBlack),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.heightIn(min = 48.dp)
                    ) {
                        Text(strings.editProfileButton, fontSize = 12.sp, fontWeight = FontWeight.Black)
                    }
                }
            }

            // ABOUT US
            Card(
                colors = CardDefaults.cardColors(containerColor = SetCard),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, SetBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Box(
                            modifier = Modifier.size(40.dp).background(SetBlack, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = SetYellow)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(strings.aboutUsCardTitle, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = SetWhite)
                            Text(strings.aboutUsCardSubtitle, fontSize = 11.sp, color = SetMuted)
                        }
                    }
                    OutlinedButton(
                        onClick = onNavigateToAbout,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, SetBorder),
                        modifier = Modifier.heightIn(min = 48.dp).testTag("about_us_settings_button")
                    ) {
                        Text(strings.viewAboutButton, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SetWhite)
                    }
                }
            }

            // FLOATING SPEEDOMETER BUBBLE
            val hasOverlayPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Settings.canDrawOverlays(context)
            } else true

            Card(
                colors = CardDefaults.cardColors(containerColor = SetBlack),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, SetBorder),
                modifier = Modifier.fillMaxWidth().testTag("floating_bubble_settings_card")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Box(
                                modifier = Modifier.size(40.dp).background(SetYellow, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Speed, contentDescription = null, tint = SetBlack)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(strings.floatingBubbleCardTitle, fontWeight = FontWeight.Black, fontSize = 13.sp, color = SetWhite)
                                Text(strings.floatingBubbleCardSubtitle, fontSize = 11.sp, color = SetMuted)
                            }
                        }
                        Switch(
                            checked = floatingBubbleEnabled,
                            onCheckedChange = { checked ->
                                floatingBubbleEnabled = checked
                                viewModel.updateFloatingBubbleEnabled(checked)
                            },
                            colors = SwitchDefaults.colors(checkedThumbColor = SetBlack, checkedTrackColor = SetYellow),
                            modifier = Modifier.testTag("toggle_floating_bubble")
                        )
                    }

                    if (!hasOverlayPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = {
                                try {
                                    context.startActivity(
                                        Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}"))
                                    )
                                } catch (_: Exception) {
                                    context.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION))
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SetYellow, contentColor = SetBlack),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().height(48.dp)
                        ) {
                            Text(strings.grantOverlayPermissionButton, fontSize = 11.sp, fontWeight = FontWeight.Black)
                        }
                    }
                }
            }

            // GPS CALIBRATION & AUDIO SETTINGS
            Card(
                colors = CardDefaults.cardColors(containerColor = SetCard),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, SetBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("METER & GPS CALIBRATION", color = SetYellow, fontSize = 11.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)

                    OutlinedTextField(
                        value = speedThresholdInput,
                        onValueChange = { speedThresholdInput = it },
                        label = { Text(strings.waitSpeedThresholdLabel) },
                        supportingText = { Text("Waiting triggers at 5 KM/h and below (<= threshold)", color = SetMuted, fontSize = 11.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = darkSettingsFieldColors(),
                        modifier = Modifier.fillMaxWidth().testTag("input_speed_threshold")
                    )

                    OutlinedTextField(
                        value = distanceCalibrationFactorInput,
                        onValueChange = { distanceCalibrationFactorInput = it },
                        label = { Text(strings.gpsCalibrationFactorLabel) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        colors = darkSettingsFieldColors(),
                        modifier = Modifier.fillMaxWidth().testTag("input_calibration_factor")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(strings.soundVoiceTitle, color = SetWhite, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text(strings.soundVoiceSubtitle, color = SetMuted, fontSize = 11.sp)
                        }
                        Switch(
                            checked = audioEnabled,
                            onCheckedChange = { audioEnabled = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = SetBlack, checkedTrackColor = SetYellow),
                            modifier = Modifier.testTag("toggle_audio")
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(84.dp))
        }
    }
}

@Composable
private fun darkSettingsFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = SetYellow,
    unfocusedBorderColor = SetBorder,
    focusedLabelColor = SetYellow,
    unfocusedLabelColor = SetMuted,
    focusedTextColor = SetWhite,
    unfocusedTextColor = SetWhite,
    cursorColor = SetYellow
)
