package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.ActiveTripDisplayMode
import com.example.data.model.ExtraChargeItem
import com.example.data.model.TripStatus
import com.example.service.LocationTrackingService
import com.example.ui.components.BatteryOptimizationCard
import com.example.ui.components.CreatorSignatureBanner
import com.example.ui.components.FloatingSpeedPermissionCard
import com.example.ui.components.LiveSpeedBubble
import com.example.ui.components.LogoStyleCustomizerDialog
import com.example.ui.components.QuickFareCalculatorDialog
import com.example.ui.components.SangamModifiersAndRatePreviewCard
import com.example.ui.components.SangamTariffChartDialog
import com.example.ui.components.SangamTripTypeSelectorCard
import com.example.ui.components.TripMapView
import com.example.ui.components.VehicleCategorySelectorCard
import com.example.ui.components.VendorLogoBadge
import com.example.util.SangamTariffConfig
import com.example.util.SangamTariffConfig.SangamTripType
import com.example.util.appStrings
import com.example.viewmodel.MeterViewModel
import java.util.Locale

private val HudBlack = Color(0xFF090A0C)
private val HudCharcoal = Color(0xFF14171C)
private val HudCard = Color(0xFF1E222A)
private val HudBorder = Color(0xFF2D333E)
private val HudYellow = Color(0xFFFFC107)
private val HudWhite = Color(0xFFFFFFFF)
private val HudMuted = Color(0xFF9CA3AF)
private val HudGreen = Color(0xFF22C55E)
private val HudAmber = Color(0xFFF59E0B)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeMeterScreen(
    viewModel: MeterViewModel,
    onNavigateToSettings: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToAbout: () -> Unit = {},
    onNavigateToSettlement: () -> Unit
) {
    val context = LocalContext.current
    val strings = appStrings()

    val tripState by viewModel.tripState.collectAsStateWithLifecycle()
    val selectedVehicle by viewModel.selectedVehicleCategory.collectAsStateWithLifecycle()
    val selectedTripType by viewModel.selectedSangamTripType.collectAsStateWithLifecycle()
    val isInnovaCrysta by viewModel.isInnovaCrysta.collectAsStateWithLifecycle()
    val isOutsideCity by viewModel.isOutsideCity.collectAsStateWithLifecycle()
    val isNightTrip by viewModel.isNightTrip.collectAsStateWithLifecycle()
    val packageHours by viewModel.packageHours.collectAsStateWithLifecycle()
    val over12Hours by viewModel.over12Hours.collectAsStateWithLifecycle()
    val activeTripDisplayMode by viewModel.activeTripDisplayMode.collectAsStateWithLifecycle()
    val hasBackup by viewModel.hasActiveSessionBackup.collectAsStateWithLifecycle()
    val validationMessage by viewModel.tariffValidationMessage.collectAsStateWithLifecycle()

    var showQuickCalculatorDialog by remember { mutableStateOf(false) }
    var showTariffChartDialog by remember { mutableStateOf(false) }
    var showExtraChargesDialog by remember { mutableStateOf(false) }
    var showLogoStyleDialog by remember { mutableStateOf(false) }
    var isMapExpanded by remember { mutableStateOf(false) }
    var showTopMenu by remember { mutableStateOf(false) }

    val isTripActive = tripState.status == TripStatus.RUNNING || tripState.status == TripStatus.PAUSED

    val notificationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> }

    val backgroundLocationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ ->
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    val locationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (fineGranted || coarseGranted) {
            LocationTrackingService.refreshOverlay(context)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED
            ) {
                backgroundLocationLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
            ) {
                notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.checkForActiveBackup()
        viewModel.loadDriverProfile()

        val hasFine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (hasFine || hasCoarse) {
            LocationTrackingService.refreshOverlay(context)
        } else {
            locationLauncher.launch(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clickable { showLogoStyleDialog = true }
                            .testTag("brand_header_title")
                    ) {
                        VendorLogoBadge(
                            vendorId = SangamTariffConfig.VENDOR_ID,
                            vendorName = SangamTariffConfig.VENDOR_NAME,
                            height = 40.dp,
                            testTag = "header_vendor_logo_badge"
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showTariffChartDialog = true },
                        modifier = Modifier
                            .testTag("tariff_chart_button")
                            .minimumInteractiveComponentSize()
                    ) {
                        Icon(
                            imageVector = Icons.Default.TableChart,
                            contentDescription = "Sangam Tariff Chart",
                            tint = HudYellow
                        )
                    }

                    IconButton(
                        onClick = onNavigateToProfile,
                        modifier = Modifier
                            .testTag("driver_profile_button")
                            .minimumInteractiveComponentSize()
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = strings.profile,
                            tint = HudWhite
                        )
                    }

                    IconButton(
                        onClick = onNavigateToSettings,
                        modifier = Modifier
                            .testTag("settings_button")
                            .minimumInteractiveComponentSize()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = strings.settings,
                            tint = HudMuted
                        )
                    }

                    Box {
                        IconButton(
                            onClick = { showTopMenu = true },
                            modifier = Modifier
                                .testTag("top_overflow_menu_button")
                                .minimumInteractiveComponentSize()
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "More Options",
                                tint = HudWhite
                            )
                        }
                        DropdownMenu(
                            expanded = showTopMenu,
                            onDismissRequest = { showTopMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Sangam Tariff Chart") },
                                onClick = {
                                    showTopMenu = false
                                    showTariffChartDialog = true
                                },
                                leadingIcon = { Icon(Icons.Default.TableChart, contentDescription = null, tint = HudYellow) }
                            )
                            DropdownMenuItem(
                                text = { Text("Logo Color & Font") },
                                onClick = {
                                    showTopMenu = false
                                    showLogoStyleDialog = true
                                },
                                leadingIcon = { Icon(Icons.Default.Palette, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { Text(strings.aboutUs) },
                                onClick = {
                                    showTopMenu = false
                                    onNavigateToAbout()
                                },
                                leadingIcon = { Icon(Icons.Default.Info, contentDescription = null) }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = HudBlack)
            )
        },
        containerColor = HudCharcoal
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            if (!isTripActive) {
                BatteryOptimizationCard()
                FloatingSpeedPermissionCard()

                if (hasBackup && tripState.status == TripStatus.IDLE) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF2B2110)),
                        shape = RoundedCornerShape(18.dp),
                        border = BorderStroke(1.dp, HudYellow),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = strings.unfinishedRideDetected,
                                color = HudYellow,
                                fontWeight = FontWeight.Black,
                                fontSize = 15.sp
                            )
                            Text(
                                text = strings.unfinishedRideDesc,
                                color = HudWhite,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                OutlinedButton(
                                    onClick = { viewModel.discardBackup() },
                                    modifier = Modifier.weight(1f).height(48.dp)
                                ) {
                                    Text(strings.discard, color = HudMuted, fontWeight = FontWeight.Bold)
                                }
                                Button(
                                    onClick = { viewModel.recoverTrip() },
                                    colors = ButtonDefaults.buttonColors(containerColor = HudYellow, contentColor = HudBlack),
                                    modifier = Modifier.weight(1.4f).height(48.dp)
                                ) {
                                    Text(strings.resumeRide, fontWeight = FontWeight.Black)
                                }
                            }
                        }
                    }
                }

                if (validationMessage != null) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF2B2110)),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, HudAmber),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(validationMessage!!, color = HudWhite, fontSize = 12.sp, modifier = Modifier.weight(1f))
                            IconButton(onClick = { viewModel.clearValidationMessage() }) {
                                Icon(Icons.Default.Close, contentDescription = strings.close, tint = HudAmber)
                            }
                        }
                    }
                }
            }

            // GPS Status & Taxi Meter Vacant/Occupied Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(
                                color = if (tripState.latitude != null) HudGreen else HudAmber,
                                shape = CircleShape
                            )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = if (tripState.latitude != null) strings.gpsSignalStrong else strings.acquiringGps,
                            color = HudWhite,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "OFFLINE SANGAM METER • GET TAXI",
                            color = HudMuted,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Surface(
                    color = when (tripState.status) {
                        TripStatus.RUNNING -> HudYellow
                        TripStatus.PAUSED -> Color(0xFF451A03)
                        else -> HudCard
                    },
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(
                        1.dp,
                        when (tripState.status) {
                            TripStatus.RUNNING -> HudYellow
                            TripStatus.PAUSED -> HudAmber
                            else -> HudBorder
                        }
                    ),
                    modifier = Modifier.testTag("trip_status_badge")
                ) {
                    Text(
                        text = when (tripState.status) {
                            TripStatus.RUNNING -> strings.occupied.uppercase(Locale.US)
                            TripStatus.PAUSED -> strings.paused.uppercase(Locale.US)
                            else -> strings.vacant.uppercase(Locale.US)
                        },
                        color = when (tripState.status) {
                            TripStatus.RUNNING -> HudBlack
                            TripStatus.PAUSED -> HudAmber
                            else -> HudWhite
                        },
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }
            }

            if (isTripActive && activeTripDisplayMode == ActiveTripDisplayMode.MAP_FIRST) {
                TripMapView(
                    tripState = tripState,
                    isNavigationConsoleMode = true,
                    isExpanded = false,
                    onToggleExpand = { isMapExpanded = true },
                    onOpenMeterDetails = { viewModel.showMeterDetails() },
                    onPauseTrip = { viewModel.pauseTrip() },
                    onResumeTrip = { viewModel.resumeTrip() },
                    onEndTrip = {
                        viewModel.completeTripForSettlement { onNavigateToSettlement() }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(520.dp)
                        .testTag("active_trip_enlarged_map")
                )
            } else {
                if (isTripActive && activeTripDisplayMode == ActiveTripDisplayMode.METER_DETAILS) {
                    Button(
                        onClick = { viewModel.showMapDisplay() },
                        colors = ButtonDefaults.buttonColors(containerColor = HudBlack, contentColor = HudYellow),
                        border = BorderStroke(1.dp, HudYellow),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .height(52.dp)
                            .testTag("return_to_map_button")
                    ) {
                        Icon(Icons.Default.Map, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(strings.returnToMapButton, fontSize = 14.sp, fontWeight = FontWeight.Black)
                    }
                } else {
                    TripMapView(
                        tripState = tripState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(210.dp)
                            .padding(vertical = 4.dp)
                    )
                }

                // COMMERCIAL METER FARE GAUGE HUD
                CommercialMeterGaugeCard(
                    tripState = tripState,
                    activeTripType = if (isTripActive) SangamTripType.fromId(tripState.sangamTripTypeId) else selectedTripType,
                    onOpenExtras = { showExtraChargesDialog = true }
                )

                // VEHICLE CATEGORY & TRIP MODE SELECTORS
                VehicleCategorySelectorCard(
                    selectedCategory = selectedVehicle,
                    isTripActive = isTripActive,
                    onSelectCategory = { viewModel.selectVehicleCategory(it) }
                )

                SangamTripTypeSelectorCard(
                    selectedCategory = selectedVehicle,
                    selectedTripType = selectedTripType,
                    isInnovaCrysta = isInnovaCrysta,
                    isTripActive = isTripActive,
                    onSelectTripType = { viewModel.setSangamTripType(it) },
                    onToggleInnovaCrysta = { viewModel.setInnovaCrysta(it) }
                )

                SangamModifiersAndRatePreviewCard(
                    category = selectedVehicle,
                    tripType = selectedTripType,
                    isInnovaCrysta = isInnovaCrysta,
                    isOutsideCity = if (isTripActive) tripState.isOutOfCity else isOutsideCity,
                    isNightTrip = if (isTripActive) tripState.isNightTrip else isNightTrip,
                    packageHours = if (isTripActive) tripState.packageHours else packageHours,
                    over12Hours = if (isTripActive) tripState.over12Hours else over12Hours,
                    onToggleOutsideCity = { viewModel.setOutsideCity(it) },
                    onToggleNightTrip = { viewModel.setNightTrip(it) },
                    onSelectPackageHours = { viewModel.setPackageHours(it) },
                    onToggleOver12Hours = { viewModel.setOver12Hours(it) }
                )

                Spacer(modifier = Modifier.height(6.dp))

                // PRIMARY START / PAUSE / END METER ACTIONS
                MeterActionControls(
                    status = tripState.status,
                    onStartTrip = { viewModel.startTrip() },
                    onOpenParkedCalculator = { showQuickCalculatorDialog = true },
                    onPauseTrip = { viewModel.pauseTrip() },
                    onResumeTrip = { viewModel.resumeTrip() },
                    onEndTrip = { viewModel.completeTripForSettlement { onNavigateToSettlement() } }
                )
            }

            Spacer(modifier = Modifier.height(14.dp))
            CreatorSignatureBanner(testTag = "home_creator_credit_banner")
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (showLogoStyleDialog) {
        LogoStyleCustomizerDialog(
            vendorId = SangamTariffConfig.VENDOR_ID,
            vendorName = SangamTariffConfig.VENDOR_NAME,
            onDismiss = { showLogoStyleDialog = false }
        )
    }

    if (showTariffChartDialog) {
        SangamTariffChartDialog(onDismiss = { showTariffChartDialog = false })
    }

    if (showQuickCalculatorDialog) {
        QuickFareCalculatorDialog(
            initialCategory = selectedVehicle,
            initialTripType = selectedTripType,
            initialInnovaCrysta = isInnovaCrysta,
            initialOutsideCity = isOutsideCity,
            initialNightTrip = isNightTrip,
            initialPackageHours = packageHours,
            initialOver12Hours = over12Hours,
            onDismiss = { showQuickCalculatorDialog = false },
            onCalculateAndSettle = { input, pickup, drop, durMin ->
                showQuickCalculatorDialog = false
                viewModel.calculateQuickSettlement(
                    input = input,
                    pickupAddress = pickup,
                    dropAddress = drop,
                    durationMinutes = durMin
                ) {
                    onNavigateToSettlement()
                }
            }
        )
    }

    if (showExtraChargesDialog) {
        ActiveExtraChargesDialog(
            currency = tripState.currency,
            currentItems = tripState.extraChargeItems,
            onAddCharge = { label, amount -> viewModel.addExtraCharge(label, amount) },
            onRemoveCharge = { id -> viewModel.removeExtraCharge(id) },
            onDismiss = { showExtraChargesDialog = false }
        )
    }

    if (isMapExpanded && isTripActive) {
        Dialog(
            onDismissRequest = { isMapExpanded = false },
            properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(HudBlack)
                    .statusBarsPadding()
            ) {
                TripMapView(
                    tripState = tripState,
                    isNavigationConsoleMode = true,
                    isExpanded = true,
                    onToggleExpand = { isMapExpanded = false },
                    onOpenMeterDetails = {
                        isMapExpanded = false
                        viewModel.showMeterDetails()
                    },
                    onPauseTrip = { viewModel.pauseTrip() },
                    onResumeTrip = { viewModel.resumeTrip() },
                    onEndTrip = {
                        isMapExpanded = false
                        viewModel.completeTripForSettlement { onNavigateToSettlement() }
                    },
                    modifier = Modifier.fillMaxSize().testTag("expanded_active_trip_map")
                )
            }
        }
    }
}

@Composable
private fun CommercialMeterGaugeCard(
    tripState: com.example.data.model.TripState,
    activeTripType: SangamTripType,
    onOpenExtras: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = HudBlack),
        shape = RoundedCornerShape(26.dp),
        border = BorderStroke(1.5.dp, if (tripState.hasUnresolvedBoundaryFlag) HudAmber else HudYellow),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .testTag("meter_details_fare_card")
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
            ) {
                Text(
                    text = "LIVE METER FARE",
                    color = HudMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.6.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.Top) {
                    Text(
                        text = tripState.currency,
                        color = HudYellow,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = String.format(Locale.US, "%.2f", tripState.currentFare),
                        color = HudYellow,
                        fontSize = 54.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.testTag("fare_text")
                    )
                }

                if (tripState.hasUnresolvedBoundaryFlag) {
                    Surface(
                        color = Color(0xFF451A03),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, HudAmber),
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Text(
                            text = "5 KM Boundary Overlap — Verify on Settlement",
                            color = HudAmber,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                if (tripState.extraChargeItems.isNotEmpty()) {
                    Surface(
                        color = HudCard,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, HudBorder),
                        modifier = Modifier
                            .padding(top = 6.dp)
                            .clickable { onOpenExtras() }
                            .testTag("live_extra_charges_summary_pill")
                    ) {
                        Text(
                            text = "Extras +${tripState.currency}${String.format(Locale.US, "%.0f", tripState.liveExtraChargesTotal)}",
                            color = HudYellow,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MetricTile(
                        label = "DISTANCE",
                        value = "${String.format(Locale.US, "%.2f", tripState.distanceKm)} km",
                        modifier = Modifier.weight(1f)
                    )
                    val middleLabel = if (activeTripType == SangamTripType.HOURLY_RENTAL) "PACKAGE" else "WAIT"
                    val middleVal = if (activeTripType == SangamTripType.HOURLY_RENTAL) {
                        "${tripState.packageHours}h"
                    } else {
                        "${tripState.waitingSeconds / 60}m"
                    }
                    MetricTile(
                        label = middleLabel,
                        value = middleVal,
                        modifier = Modifier.weight(1f)
                    )
                    MetricTile(
                        label = "DURATION",
                        value = "${tripState.durationSeconds / 60}m",
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            LiveSpeedBubble(
                speedKmH = tripState.speedKmH,
                visible = tripState.status == TripStatus.RUNNING || tripState.status == TripStatus.PAUSED,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(12.dp)
            )

            IconButton(
                onClick = onOpenExtras,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(10.dp)
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(HudCard)
                    .border(1.dp, HudBorder, CircleShape)
                    .testTag("active_meter_extra_charges_menu_button")
            ) {
                Icon(
                    imageVector = Icons.Default.AddRoad,
                    contentDescription = "Extra Charges",
                    tint = HudYellow
                )
            }
        }
    }
}

@Composable
private fun MetricTile(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .background(HudCard, RoundedCornerShape(14.dp))
            .border(1.dp, HudBorder, RoundedCornerShape(14.dp))
            .padding(vertical = 10.dp, horizontal = 6.dp)
    ) {
        Text(text = label, color = HudMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = value, color = HudWhite, fontSize = 16.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
private fun MeterActionControls(
    status: TripStatus,
    onStartTrip: () -> Unit,
    onOpenParkedCalculator: () -> Unit,
    onPauseTrip: () -> Unit,
    onResumeTrip: () -> Unit,
    onEndTrip: () -> Unit
) {
    AnimatedContent(targetState = status, label = "MeterActionControls") { target ->
        when (target) {
            TripStatus.IDLE, TripStatus.FINISHED -> {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = onStartTrip,
                        colors = ButtonDefaults.buttonColors(containerColor = HudYellow, contentColor = HudBlack),
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .testTag("start_trip_button")
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("START METER", fontSize = 16.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                    }

                    OutlinedButton(
                        onClick = onOpenParkedCalculator,
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, HudBorder),
                        colors = ButtonDefaults.outlinedButtonColors(containerColor = HudBlack, contentColor = HudWhite),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("parked_calculator_button")
                    ) {
                        Icon(Icons.Default.Calculate, contentDescription = null, tint = HudYellow, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("CALCULATE BILL (PARKED)", fontSize = 13.sp, fontWeight = FontWeight.Black)
                    }
                }
            }

            TripStatus.RUNNING -> {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = onPauseTrip,
                        shape = RoundedCornerShape(18.dp),
                        border = BorderStroke(1.5.dp, HudYellow),
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp)
                            .testTag("pause_trip_button")
                    ) {
                        Icon(Icons.Default.Pause, contentDescription = null, tint = HudYellow)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("PAUSE", color = HudYellow, fontWeight = FontWeight.Black)
                    }

                    Button(
                        onClick = onEndTrip,
                        colors = ButtonDefaults.buttonColors(containerColor = HudYellow, contentColor = HudBlack),
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier
                            .weight(1.3f)
                            .height(56.dp)
                            .testTag("stop_trip_button")
                    ) {
                        Icon(Icons.Default.Stop, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("END & SETTLE", fontWeight = FontWeight.Black)
                    }
                }
            }

            TripStatus.PAUSED -> {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = onResumeTrip,
                        colors = ButtonDefaults.buttonColors(containerColor = HudGreen, contentColor = HudBlack),
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier
                            .weight(1.1f)
                            .height(56.dp)
                            .testTag("resume_trip_button")
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("RESUME", fontWeight = FontWeight.Black)
                    }

                    Button(
                        onClick = onEndTrip,
                        colors = ButtonDefaults.buttonColors(containerColor = HudYellow, contentColor = HudBlack),
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier
                            .weight(1.2f)
                            .height(56.dp)
                            .testTag("stop_trip_button")
                    ) {
                        Icon(Icons.Default.Stop, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("END & SETTLE", fontWeight = FontWeight.Black)
                    }
                }
            }
        }
    }
}

@Composable
private fun ActiveExtraChargesDialog(
    currency: String,
    currentItems: List<ExtraChargeItem>,
    onAddCharge: (String, Double) -> Unit,
    onRemoveCharge: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var labelInput by remember { mutableStateOf("Toll") }
    var amountInput by remember { mutableStateOf("") }
    val presets = listOf("Toll", "Permit", "Parking", "Hill / Border")

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = HudCharcoal),
            shape = RoundedCornerShape(22.dp),
            border = BorderStroke(1.dp, HudYellow),
            modifier = Modifier.fillMaxWidth().testTag("extra_charges_dialog")
        ) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("EXTRA CHARGES", color = HudYellow, fontWeight = FontWeight.Black, fontSize = 15.sp)
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = HudMuted)
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                    presets.forEach { preset ->
                        FilterChip(
                            selected = labelInput == preset,
                            onClick = { labelInput = preset },
                            label = { Text(preset, fontSize = 11.sp) }
                        )
                    }
                }

                OutlinedTextField(
                    value = labelInput,
                    onValueChange = { labelInput = it },
                    label = { Text("Charge Label") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = amountInput,
                    onValueChange = { amountInput = it },
                    label = { Text("Amount ($currency)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = {
                        val amt = amountInput.trim().toDoubleOrNull() ?: 0.0
                        if (amt > 0.0) {
                            onAddCharge(labelInput.ifBlank { "Extra" }, amt)
                            amountInput = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = HudYellow, contentColor = HudBlack),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Text("ADD CHARGE", fontWeight = FontWeight.Black)
                }

                if (currentItems.isNotEmpty()) {
                    HorizontalDivider(color = HudBorder)
                    currentItems.forEach { item ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("${item.label}: $currency${String.format(Locale.US, "%.2f", item.amount)}", color = HudWhite, fontSize = 12.sp)
                            IconButton(onClick = { onRemoveCharge(item.id) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Remove", tint = HudAmber)
                            }
                        }
                    }
                }
            }
        }
    }
}
