package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.util.SangamTariffCalculator
import com.example.util.SangamTariffConfig
import com.example.util.SangamTariffConfig.SangamTripType
import com.example.util.SangamTariffConfig.VehicleCategory
import java.util.Locale

private val MeterBlack = Color(0xFF090A0C)
private val MeterCharcoal = Color(0xFF14171C)
private val MeterCharcoalCard = Color(0xFF1E222A)
private val MeterBorder = Color(0xFF2D333E)
private val TaxiYellow = Color(0xFFFFC107)
private val TextWhite = Color(0xFFFFFFFF)
private val TextMuted = Color(0xFF9CA3AF)
private val WarningAmber = Color(0xFFF59E0B)

@Composable
fun VehicleCategorySelectorCard(
    selectedCategory: VehicleCategory,
    isTripActive: Boolean,
    onSelectCategory: (VehicleCategory) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MeterCharcoalCard),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, MeterBorder),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .testTag("vehicle_category_selector_card")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "VEHICLE CATEGORY",
                    color = TaxiYellow,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.2.sp
                )
                if (isTripActive) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Locked during active trip",
                            tint = WarningAmber,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "LOCKED",
                            color = WarningAmber,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                VehicleCategory.entries.forEach { category ->
                    val selected = category == selectedCategory
                    Surface(
                        color = if (selected) TaxiYellow else MeterBlack,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, if (selected) TaxiYellow else MeterBorder),
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .clickable(enabled = !isTripActive) { onSelectCategory(category) }
                            .testTag("vehicle_cat_${category.id.lowercase(Locale.US)}")
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            Text(
                                text = category.shortLabel,
                                color = if (selected) MeterBlack else TextWhite,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "${selectedCategory.displayName}: ${selectedCategory.exampleModels}",
                color = TextMuted,
                fontSize = 11.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun SangamTripTypeSelectorCard(
    selectedCategory: VehicleCategory,
    selectedTripType: SangamTripType,
    isInnovaCrysta: Boolean,
    isTripActive: Boolean,
    onSelectTripType: (SangamTripType) -> Unit,
    onToggleInnovaCrysta: (Boolean) -> Unit
) {
    val supportedTypes = selectedCategory.supportedTripTypes()

    Card(
        colors = CardDefaults.cardColors(containerColor = MeterCharcoalCard),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, MeterBorder),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .testTag("sangam_trip_type_selector_card")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "TRIP TARIFF MODE",
                    color = TaxiYellow,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.2.sp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                supportedTypes.forEach { tripType ->
                    val selected = tripType == selectedTripType
                    Surface(
                        color = if (selected) TaxiYellow else MeterBlack,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, if (selected) TaxiYellow else MeterBorder),
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .clickable(enabled = !isTripActive) { onSelectTripType(tripType) }
                            .testTag("trip_type_${tripType.id.lowercase(Locale.US)}")
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            Text(
                                text = tripType.displayName,
                                color = if (selected) MeterBlack else TextWhite,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            if (selectedCategory == VehicleCategory.INNOVA && selectedTripType == SangamTripType.DAY_RENTAL) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    color = MeterBlack,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, if (isInnovaCrysta) TaxiYellow else MeterBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Innova Crysta Day Rental Rate",
                                color = TextWhite,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Standard Innova ₹2,500+₹15/km vs Crysta ₹3,000+₹16/km",
                                color = TextMuted,
                                fontSize = 10.sp
                            )
                        }
                        Switch(
                            checked = isInnovaCrysta,
                            enabled = !isTripActive,
                            onCheckedChange = onToggleInnovaCrysta,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MeterBlack,
                                checkedTrackColor = TaxiYellow,
                                uncheckedThumbColor = TextMuted,
                                uncheckedTrackColor = MeterCharcoalCard
                            ),
                            modifier = Modifier.testTag("toggle_innova_crysta")
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SangamModifiersAndRatePreviewCard(
    category: VehicleCategory,
    tripType: SangamTripType,
    isInnovaCrysta: Boolean,
    isOutsideCity: Boolean,
    isNightTrip: Boolean,
    packageHours: Int,
    over12Hours: Boolean,
    additionalWaitingHours: Double = 0.0,
    outstationWaitRate: Double = 0.0,
    onToggleOutsideCity: (Boolean) -> Unit,
    onToggleNightTrip: (Boolean) -> Unit,
    onSelectPackageHours: (Int) -> Unit,
    onToggleOver12Hours: (Boolean) -> Unit,
    onUpdateAdditionalWaitingHours: (Double) -> Unit = {}
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MeterCharcoalCard),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, MeterBorder),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .testTag("sangam_rate_preview_card")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = "ACTIVE TARIFF & MODIFIERS",
                color = TaxiYellow,
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.1.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            val rateSummary = remember(category, tripType, isInnovaCrysta, packageHours) {
                buildRateSummaryText(category, tripType, isInnovaCrysta, packageHours)
            }
            Text(
                text = rateSummary,
                color = TextWhite,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                lineHeight = 17.sp
            )

            when (tripType) {
                SangamTripType.LOCAL_NORMAL, SangamTripType.LOCAL_MINIMUM_BILL -> {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ModifierToggleChip(
                            label = "Outside City +₹80",
                            checked = isOutsideCity,
                            onToggle = { onToggleOutsideCity(!isOutsideCity) },
                            testTag = "chip_outside_city",
                            modifier = Modifier.weight(1f)
                        )
                        ModifierToggleChip(
                            label = "Night 11–6 +₹50",
                            checked = isNightTrip,
                            onToggle = { onToggleNightTrip(!isNightTrip) },
                            testTag = "chip_night_charge",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                SangamTripType.HOURLY_RENTAL -> {
                    Spacer(modifier = Modifier.height(10.dp))
                    val safeHours = packageHours.coerceAtLeast(1)
                    val pkgAmt = SangamTariffConfig.hourlyBaseRatePerHour.toInt() * safeHours
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Package: ${safeHours}h / ${safeHours * SangamTariffConfig.hourlyIncludedKmPerHour} km",
                                color = TextWhite,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Base Fee: ₹$pkgAmt • 5-min grace • Zero wait",
                                color = TaxiYellow,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = { if (safeHours > 1) onSelectPackageHours(safeHours - 1) },
                                enabled = safeHours > 1,
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, MeterBorder),
                                contentPadding = PaddingValues(0.dp),
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("hourly_hours_minus")
                            ) {
                                Text("−", color = TextWhite, fontSize = 18.sp, fontWeight = FontWeight.Black)
                            }
                            Text(
                                text = "${safeHours}h",
                                color = TaxiYellow,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black
                            )
                            OutlinedButton(
                                onClick = { onSelectPackageHours(safeHours + 1) },
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, TaxiYellow),
                                contentPadding = PaddingValues(0.dp),
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("hourly_hours_plus")
                            ) {
                                Text("+", color = TaxiYellow, fontSize = 18.sp, fontWeight = FontWeight.Black)
                            }
                        }
                    }
                }

                SangamTripType.OUTSTATION_ONE_WAY, SangamTripType.OUTSTATION_ROUND_TRIP -> {
                    Spacer(modifier = Modifier.height(10.dp))
                    ModifierToggleChip(
                        label = "Driver Allowance (>12 Hours) +₹400",
                        checked = over12Hours,
                        onToggle = { onToggleOver12Hours(!over12Hours) },
                        testTag = "chip_over_12_hours",
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (outstationWaitRate > 0.0) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Manual Extra Waiting Hours (Optional)",
                                    color = TextWhite,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Configured Wait Rate: ₹${String.format(Locale.US, "%.0f", outstationWaitRate)}/hr",
                                    color = TextMuted,
                                    fontSize = 10.sp
                                )
                            }
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedButton(
                                    onClick = { onUpdateAdditionalWaitingHours((additionalWaitingHours - 1.0).coerceAtLeast(0.0)) },
                                    enabled = additionalWaitingHours > 0.0,
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, MeterBorder),
                                    contentPadding = PaddingValues(0.dp),
                                    modifier = Modifier.size(48.dp).testTag("outstation_wait_hours_minus")
                                ) {
                                    Text("−", color = TextWhite, fontSize = 18.sp, fontWeight = FontWeight.Black)
                                }
                                Text(
                                    text = "${String.format(Locale.US, "%.1f", additionalWaitingHours)}h",
                                    color = TaxiYellow,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Black
                                )
                                OutlinedButton(
                                    onClick = { onUpdateAdditionalWaitingHours(additionalWaitingHours + 1.0) },
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, TaxiYellow),
                                    contentPadding = PaddingValues(0.dp),
                                    modifier = Modifier.size(48.dp).testTag("outstation_wait_hours_plus")
                                ) {
                                    Text("+", color = TaxiYellow, fontSize = 18.sp, fontWeight = FontWeight.Black)
                                }
                            }
                        }
                    }
                }

                SangamTripType.DAY_RENTAL -> Unit
            }
        }
    }
}

@Composable
private fun ModifierToggleChip(
    label: String,
    checked: Boolean,
    onToggle: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    Surface(
        color = if (checked) TaxiYellow else MeterBlack,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, if (checked) TaxiYellow else MeterBorder),
        modifier = modifier
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable { onToggle() }
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = if (checked) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                contentDescription = null,
                tint = if (checked) MeterBlack else TextMuted,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                color = if (checked) MeterBlack else TextWhite,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private fun buildRateSummaryText(
    category: VehicleCategory,
    tripType: SangamTripType,
    isInnovaCrysta: Boolean,
    packageHours: Int
): String {
    return when (tripType) {
        SangamTripType.LOCAL_NORMAL -> {
            val rate = SangamTariffConfig.localNormalRates[category]
            if (rate != null) {
                "Local Normal: ₹${rate.initialCharge.toInt()} initial + ₹${rate.perKmRate.toInt()}/km"
            } else "Not applicable for ${category.displayName}"
        }
        SangamTripType.LOCAL_MINIMUM_BILL -> {
            when (category) {
                VehicleCategory.MINI, VehicleCategory.SEDAN ->
                    "Min Bill: 1–5 KM ₹200 | 5–6 KM ₹250 | >6 KM ₹80 + ₹28/km (5 KM overlap flagged)"
                VehicleCategory.SUV ->
                    "Min Bill: 1–7 KM ₹450 | >7 KM ₹200 + ₹35/km"
                VehicleCategory.SUV_PLUS ->
                    "Min Bill: 1–10 KM ₹700 | >10 KM ₹250 + ₹45/km"
                VehicleCategory.INNOVA ->
                    "Min Bill: 1–10 KM ₹800 | >10 KM ₹250 + ₹50/km"
            }
        }
        SangamTripType.HOURLY_RENTAL -> {
            val hrs = packageHours.coerceAtLeast(1)
            val amt = SangamTariffConfig.hourlyBaseRatePerHour.toInt() * hrs
            "Hourly ${hrs}h (${hrs * SangamTariffConfig.hourlyIncludedKmPerHour} km): ₹$amt (₹350/hr) • Extra ₹20/km • 5-min grace • Zero wait • CC ₹50/hr"
        }
        SangamTripType.OUTSTATION_ONE_WAY -> {
            val r = SangamTariffConfig.outstationRates[category]
            if (r != null) {
                "One-Way: ₹${r.initialCharge.toInt()} initial + ₹${r.oneWayPerKm.toInt()}/km • >12h ₹400 • CC 10%"
            } else "Not applicable"
        }
        SangamTripType.OUTSTATION_ROUND_TRIP -> {
            val r = SangamTariffConfig.outstationRates[category]
            if (r != null) {
                "Round-Trip: ₹${r.initialCharge.toInt()} initial + ₹${r.roundTripPerKm.toInt()}/km • >12h ₹400 • CC 10%"
            } else "Not applicable"
        }
        SangamTripType.DAY_RENTAL -> {
            val r = if (category == VehicleCategory.INNOVA && isInnovaCrysta) {
                SangamTariffConfig.innovaCrystaDayRentalRate
            } else {
                SangamTariffConfig.dayRentalRates[category]
            }
            if (r != null) {
                "Day Rental: ₹${r.baseAmount.toInt()} + ₹${r.perKmRate.toInt()}/km"
            } else "Not available"
        }
    }
}

@Composable
fun QuickFareCalculatorDialog(
    initialCategory: VehicleCategory,
    initialTripType: SangamTripType,
    initialInnovaCrysta: Boolean,
    initialOutsideCity: Boolean,
    initialNightTrip: Boolean,
    initialPackageHours: Int,
    initialOver12Hours: Boolean,
    outstationWaitRate: Double = 0.0,
    onDismiss: () -> Unit,
    onCalculateAndSettle: (
        input: SangamTariffCalculator.TripCalculationInput,
        pickup: String,
        drop: String,
        durationMin: Long
    ) -> Unit
) {
    var distanceInput by remember { mutableStateOf("") }
    var durationInput by remember { mutableStateOf("") }
    var hourlyExtraMinutesInput by remember { mutableStateOf("0") }
    var outstationWaitHoursInput by remember { mutableStateOf("") }
    var hoursInput by remember { mutableIntStateOf(initialPackageHours.coerceAtLeast(1)) }
    var outsideCity by remember { mutableStateOf(initialOutsideCity) }
    var nightTrip by remember { mutableStateOf(initialNightTrip) }
    var over12Hrs by remember { mutableStateOf(initialOver12Hours) }
    var tollInput by remember { mutableStateOf("") }
    var permitInput by remember { mutableStateOf("") }
    var parkingInput by remember { mutableStateOf("") }
    var pickupInput by remember { mutableStateOf("") }
    var dropInput by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MeterCharcoal),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, TaxiYellow),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 640.dp)
                .testTag("quick_fare_calculator_dialog")
        ) {
            Column(
                modifier = Modifier
                    .padding(18.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "PARKED FARE CALCULATOR",
                            color = TaxiYellow,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black
                        )
                        val catText = if (initialCategory == VehicleCategory.INNOVA && initialInnovaCrysta && initialTripType == SangamTripType.DAY_RENTAL) {
                            "INNOVA CRYSTA"
                        } else initialCategory.displayName
                        Text(
                            text = "$catText • ${initialTripType.displayName}",
                            color = TextMuted,
                            fontSize = 12.sp
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextMuted)
                    }
                }

                if (errorMsg != null) {
                    Text(text = errorMsg!!, color = Color(0xFFF87171), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                OutlinedTextField(
                    value = distanceInput,
                    onValueChange = { distanceInput = it; errorMsg = null },
                    label = { Text("Distance (KM)") },
                    placeholder = { Text("e.g. 12.5") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    colors = darkFieldColors(),
                    modifier = Modifier.fillMaxWidth().testTag("quick_calc_distance_input")
                )

                if (initialTripType == SangamTripType.HOURLY_RENTAL) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Hours: ${hoursInput}h (${hoursInput * SangamTariffConfig.hourlyIncludedKmPerHour} km)",
                                color = TextWhite,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "₹350/hr • 5-min grace • Zero waiting",
                                color = TextMuted,
                                fontSize = 10.sp
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = { if (hoursInput > 1) hoursInput-- },
                                modifier = Modifier.size(48.dp),
                                contentPadding = PaddingValues(0.dp)
                            ) { Text("−", color = TextWhite) }
                            OutlinedButton(
                                onClick = { hoursInput++ },
                                modifier = Modifier.size(48.dp),
                                contentPadding = PaddingValues(0.dp)
                            ) { Text("+", color = TaxiYellow) }
                        }
                    }

                    OutlinedTextField(
                        value = hourlyExtraMinutesInput,
                        onValueChange = { hourlyExtraMinutesInput = it },
                        label = { Text("Extra Minutes (0–59, 5-min grace per slab)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = darkFieldColors(),
                        modifier = Modifier.fillMaxWidth().testTag("quick_calc_hourly_extra_minutes")
                    )
                } else {
                    OutlinedTextField(
                        value = durationInput,
                        onValueChange = { durationInput = it },
                        label = { Text("Duration (Minutes, optional)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = darkFieldColors(),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (initialTripType == SangamTripType.LOCAL_NORMAL || initialTripType == SangamTripType.LOCAL_MINIMUM_BILL) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        ModifierToggleChip(
                            label = "Outside City +₹80",
                            checked = outsideCity,
                            onToggle = { outsideCity = !outsideCity },
                            testTag = "quick_calc_outside_city",
                            modifier = Modifier.weight(1f)
                        )
                        ModifierToggleChip(
                            label = "Night +₹50",
                            checked = nightTrip,
                            onToggle = { nightTrip = !nightTrip },
                            testTag = "quick_calc_night",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                if (initialTripType == SangamTripType.OUTSTATION_ONE_WAY ||
                    initialTripType == SangamTripType.OUTSTATION_ROUND_TRIP
                ) {
                    ModifierToggleChip(
                        label = ">12 Hours Allowance +₹400",
                        checked = over12Hrs,
                        onToggle = { over12Hrs = !over12Hrs },
                        testTag = "quick_calc_over_12",
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = outstationWaitHoursInput,
                        onValueChange = { outstationWaitHoursInput = it },
                        label = { Text("Additional Waiting Hours (Optional)") },
                        supportingText = {
                            Text(
                                text = if (outstationWaitRate > 0.0) {
                                    "Configured wait rate: ₹${String.format(Locale.US, "%.0f", outstationWaitRate)}/hr"
                                } else {
                                    "Outstation hourly wait rate not configured (no auto charge)"
                                },
                                color = TextMuted,
                                fontSize = 10.sp
                            )
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        colors = darkFieldColors(),
                        modifier = Modifier.fillMaxWidth().testTag("quick_calc_outstation_wait_hours")
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = tollInput,
                        onValueChange = { tollInput = it },
                        label = { Text("Toll (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        colors = darkFieldColors(),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = permitInput,
                        onValueChange = { permitInput = it },
                        label = { Text("Permit (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        colors = darkFieldColors(),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = parkingInput,
                        onValueChange = { parkingInput = it },
                        label = { Text("Park (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        colors = darkFieldColors(),
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = pickupInput,
                        onValueChange = { pickupInput = it },
                        label = { Text("From") },
                        colors = darkFieldColors(),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = dropInput,
                        onValueChange = { dropInput = it },
                        label = { Text("To") },
                        colors = darkFieldColors(),
                        modifier = Modifier.weight(1f)
                    )
                }

                Button(
                    onClick = {
                        val dist = distanceInput.trim().toDoubleOrNull()
                        if (dist == null || dist < 0.0) {
                            errorMsg = "Enter valid distance in KM"
                            return@Button
                        }
                        val extraMins = hourlyExtraMinutesInput.trim().toLongOrNull()?.coerceIn(0L, 59L) ?: 0L
                        val computedDurationSec = if (initialTripType == SangamTripType.HOURLY_RENTAL) {
                            (hoursInput.coerceAtLeast(1) * 3600L) + (extraMins * 60L)
                        } else {
                            (durationInput.trim().toLongOrNull() ?: 0L) * 60L
                        }
                        val waitHours = outstationWaitHoursInput.trim().toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
                        val input = SangamTariffCalculator.TripCalculationInput(
                            vehicleCategory = initialCategory,
                            tripType = initialTripType,
                            distanceKm = dist,
                            packageHours = hoursInput.coerceAtLeast(1),
                            waitingMinutes = 0.0,
                            durationSeconds = computedDurationSec,
                            additionalWaitingHours = waitHours,
                            additionalWaitingHourlyRate = outstationWaitRate,
                            isInnovaCrysta = initialInnovaCrysta && initialCategory == VehicleCategory.INNOVA,
                            isOutsideCity = outsideCity,
                            isNightTrip = nightTrip,
                            over12Hours = over12Hrs,
                            tollCharge = tollInput.trim().toDoubleOrNull() ?: 0.0,
                            permitCharge = permitInput.trim().toDoubleOrNull() ?: 0.0,
                            parkingCharge = parkingInput.trim().toDoubleOrNull() ?: 0.0
                        )
                        val durMin = if (initialTripType == SangamTripType.HOURLY_RENTAL) {
                            computedDurationSec / 60L
                        } else {
                            durationInput.trim().toLongOrNull() ?: 0L
                        }
                        onCalculateAndSettle(input, pickupInput.trim(), dropInput.trim(), durMin)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TaxiYellow, contentColor = MeterBlack),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("quick_calc_submit_button")
                ) {
                    Text("CALCULATE & SETTLE", fontWeight = FontWeight.Black, fontSize = 14.sp)
                }
            }
        }
    }
}

@Composable
fun GetTaxiTariffDetailsDialog(onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MeterCharcoal),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, TaxiYellow),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 650.dp)
                .testTag("get_taxi_tariff_details_dialog")
        ) {
            Column(
                modifier = Modifier
                    .padding(18.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "GET TAXI TARIFF DETAILS",
                        color = TaxiYellow,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextMuted)
                    }
                }

                ReferenceBlock(
                    title = "1. Local Normal & Minimum Bill",
                    body = "• MINI / SEDAN: ₹80 + ₹28/km | Min: 1–5km ₹200, 5–6km ₹250, >6km ₹80+₹28/km\n" +
                        "• SUV: ₹200 + ₹35/km | Min: 1–7km ₹450, >7km ₹200+₹35/km\n" +
                        "• SUV+: ₹250 + ₹45/km | Min: 1–10km ₹700, >10km ₹250+₹45/km\n" +
                        "• INNOVA: ₹250 + ₹50/km | Min: 1–10km ₹800, >10km ₹250+₹50/km\n" +
                        "• Extras: Outside city ₹80 | Night (11PM–6AM) ₹50"
                )
                ReferenceBlock(
                    title = "2. Local CC Structure",
                    body = "1–5 KM: ₹30 | 6–10 KM: ₹60–₹75 | 10–25 KM: ₹80 | >25 KM: 10% | >₹800 bill: 10%"
                )
                ReferenceBlock(
                    title = "3. Hourly Rental (GET TAXI Standard)",
                    body = "• Customer Fare: ₹350 per charged hour • 10 km included per hour\n" +
                        "• Additional Distance: ₹20/km beyond included allowance\n" +
                        "• Grace Period: 5 minutes after every hourly slab (zero waiting charges)\n" +
                        "• Driver CC: ₹50 per charged hour (fixed)"
                )
                ReferenceBlock(
                    title = "4. Outstation & Day Rental",
                    body = "• One-Way / Round-Trip: MINI ₹300+30/15 | SEDAN ₹300+30/15 | SUV ₹400+35/17 | SUV+ ₹400+36/18 | INNOVA ₹400+40/20 (>12h +₹400, CC 10%)\n" +
                        "• Day Rental: MINI/SEDAN ₹2000+10/km | SUV ₹2300+13/km | SUV+ ₹2400+14/km | INNOVA ₹2500+15/km | CRYSTA ₹3000+16/km"
                )
                ReferenceBlock(
                    title = "Items Flagged for Confirmation",
                    body = "1) Exact 5.0 KM MINI/SEDAN min-bill overlap (₹200 vs ₹250)\n" +
                        "2) Local CC 6–10 KM range (₹60–₹75) & 5–6 / 10 KM boundaries\n" +
                        "3) >25 KM (10%) vs >₹800 bill (10%) alternative vs cumulative\n" +
                        "4) Missing Day Rental CC rate & optional Outstation waiting-hour ₹ rate",
                    accentColor = WarningAmber
                )
            }
        }
    }
}

@Composable
private fun ReferenceBlock(
    title: String,
    body: String,
    accentColor: Color = TaxiYellow
) {
    Surface(
        color = MeterBlack,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.45f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = title,
                color = accentColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.Black
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = body,
                color = TextWhite,
                fontSize = 11.sp,
                lineHeight = 16.sp
            )
        }
    }
}

@Composable
private fun darkFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = TaxiYellow,
    unfocusedBorderColor = MeterBorder,
    focusedLabelColor = TaxiYellow,
    unfocusedLabelColor = TextMuted,
    focusedTextColor = TextWhite,
    unfocusedTextColor = TextWhite,
    cursorColor = TaxiYellow
)
