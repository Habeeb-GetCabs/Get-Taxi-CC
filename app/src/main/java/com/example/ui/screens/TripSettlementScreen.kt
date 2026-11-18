package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.ui.components.VendorLogoBadge
import com.example.util.PdfGenerator
import com.example.util.SangamTariffCalculator
import com.example.util.SangamTariffConfig
import com.example.util.ShareUtils
import com.example.util.UpiQrCodeGenerator
import com.example.viewmodel.MeterViewModel
import java.io.File
import java.util.Locale

private val SettleBlack = Color(0xFF090A0C)
private val SettleCharcoal = Color(0xFF14171C)
private val SettleCard = Color(0xFF1E222A)
private val SettleBorder = Color(0xFF2D333E)
private val SettleYellow = Color(0xFFFFC107)
private val SettleWhite = Color(0xFFFFFFFF)
private val SettleMuted = Color(0xFF9CA3AF)
private val SettleAmber = Color(0xFFF59E0B)
private val SettleGreen = Color(0xFF22C55E)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripSettlementScreen(
    viewModel: MeterViewModel,
    onDoneClearAndReturnHome: () -> Unit
) {
    val context = LocalContext.current
    val settlement by viewModel.currentSettlement.collectAsStateWithLifecycle()
    val driverProfile by viewModel.driverProfile.collectAsStateWithLifecycle()

    var showCustomerEditor by remember { mutableStateOf(false) }
    var custName by remember(settlement?.tripEntity?.tripNumber) {
        mutableStateOf(settlement?.tripEntity?.customerName ?: "")
    }
    var custMobile by remember(settlement?.tripEntity?.tripNumber) {
        mutableStateOf(settlement?.tripEntity?.customerMobile ?: "")
    }
    var custAddress by remember(settlement?.tripEntity?.tripNumber) {
        mutableStateOf(settlement?.tripEntity?.customerAddress ?: "")
    }

    BackHandler {
        viewModel.endTripAndClearSettlement()
        onDoneClearAndReturnHome()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "TRIP SETTLEMENT",
                        color = SettleWhite,
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp,
                        letterSpacing = 0.8.sp
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            viewModel.endTripAndClearSettlement()
                            onDoneClearAndReturnHome()
                        },
                        modifier = Modifier.testTag("settlement_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back and Clear",
                            tint = SettleYellow
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SettleCharcoal)
            )
        },
        containerColor = SettleCharcoal,
        bottomBar = {
            Surface(
                color = SettleBlack,
                border = BorderStroke(1.dp, SettleBorder)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(16.dp)
                ) {
                    Button(
                        onClick = {
                            viewModel.endTripAndClearSettlement()
                            onDoneClearAndReturnHome()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SettleYellow,
                            contentColor = SettleBlack
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("end_trip_clear_button")
                    ) {
                        Icon(Icons.Default.DoneAll, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "END TRIP & CLEAR",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.8.sp
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        val active = settlement
        if (active == null) {
            Box(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No active settlement session.",
                    color = SettleMuted,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            return@Scaffold
        }

        val trip = active.tripEntity
        val breakdown = active.breakdown
        val categoryLabel = if (breakdown.isInnovaCrysta) "INNOVA CRYSTA" else breakdown.vehicleCategory.displayName

        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. TOTAL CUSTOMER BILL CARD
            Card(
                colors = CardDefaults.cardColors(containerColor = SettleBlack),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.5.dp, SettleYellow),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("settlement_total_card")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "CUSTOMER BILL TOTAL",
                        color = SettleMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.5.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = SangamTariffCalculator.formatRupees(breakdown.customerBillTotal),
                        color = SettleYellow,
                        fontSize = 46.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.testTag("settlement_total_amount")
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        StatusTag(text = categoryLabel, bg = SettleCard, fg = SettleWhite)
                        StatusTag(text = breakdown.tripType.displayName, bg = SettleCard, fg = SettleYellow)
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Bill ${trip.tripNumber} • ${String.format(Locale.US, "%.2f", breakdown.distanceKm)} km • ${trip.durationSeconds / 60} min",
                        color = SettleMuted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // 2. EXPLICIT UNRESOLVED BOUNDARY / CONFIRMATION ALERT
            if (breakdown.hasUnresolvedAmbiguity || breakdown.warnings.isNotEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF2B2110)),
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, SettleAmber),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("settlement_ambiguity_banner")
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = SettleAmber, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "CONFIRMATION REQUIRED (TARIFF RULE)",
                                color = SettleAmber,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.8.sp
                            )
                        }
                        breakdown.ambiguities.forEach { amb ->
                            Text(
                                text = "• ${amb.title}: ${amb.description}",
                                color = SettleWhite,
                                fontSize = 12.sp,
                                lineHeight = 16.sp
                            )
                            if (amb.candidateOptions.isNotEmpty()) {
                                Text(
                                    text = "  Options: ${amb.candidateOptions.joinToString(" OR ")}",
                                    color = SettleYellow,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        breakdown.warnings.forEach { w ->
                            Text(text = "• $w", color = SettleWhite, fontSize = 11.sp, lineHeight = 16.sp)
                        }
                    }
                }
            }

            // 3. ITEMIZED SANGAM FARE BREAKDOWN
            Card(
                colors = CardDefaults.cardColors(containerColor = SettleCard),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, SettleBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("settlement_line_items_card")
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "APPROVED FARE BREAKDOWN",
                        color = SettleYellow,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                    HorizontalDivider(color = SettleBorder)
                    breakdown.lineItems.forEach { item ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.label,
                                    color = SettleWhite,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = item.detail,
                                    color = SettleMuted,
                                    fontSize = 11.sp
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = SangamTariffCalculator.formatRupees(item.amount),
                                color = if (item.isPassThrough) SettleMuted else SettleWhite,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }
            }

            // 4. VENDOR COMMISSION & DRIVER BALANCE
            Card(
                colors = CardDefaults.cardColors(containerColor = SettleCard),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, SettleBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("settlement_commission_card")
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "COMMISSION & DRIVER SETTLEMENT",
                        color = SettleYellow,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )

                    when (val comm = breakdown.commissionResult) {
                        is SangamTariffCalculator.CommissionResult.Confirmed -> {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Approved CC", color = SettleWhite, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    Text(comm.ruleLabel, color = SettleMuted, fontSize = 11.sp)
                                }
                                Text(
                                    text = SangamTariffCalculator.formatRupees(comm.amount),
                                    color = SettleYellow,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Black,
                                    modifier = Modifier.testTag("confirmed_commission_amount")
                                )
                            }
                        }

                        is SangamTariffCalculator.CommissionResult.NeedsConfirmation -> {
                            StatusTag(text = "NEEDS CONFIRMATION", bg = Color(0xFF451A03), fg = SettleAmber)
                            Text(text = comm.reason, color = SettleWhite, fontSize = 12.sp, lineHeight = 16.sp)
                            if (comm.minPossibleAmount != null && comm.maxPossibleAmount != null) {
                                Text(
                                    text = "Chart Range: ${SangamTariffCalculator.formatRupees(comm.minPossibleAmount)} – ${SangamTariffCalculator.formatRupees(comm.maxPossibleAmount)}",
                                    color = SettleYellow,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                            comm.candidateLabels.forEach { opt ->
                                Text(text = "• $opt", color = SettleMuted, fontSize = 11.sp)
                            }
                        }

                        is SangamTariffCalculator.CommissionResult.NotSpecified -> {
                            StatusTag(text = "NOT SPECIFIED IN CHART", bg = Color(0xFF451A03), fg = SettleAmber)
                            Text(text = comm.reason, color = SettleWhite, fontSize = 12.sp, lineHeight = 16.sp)
                        }
                    }

                    HorizontalDivider(color = SettleBorder)

                    when (val bal = breakdown.driverBalanceStatus) {
                        is SangamTariffCalculator.DriverBalanceStatus.Confirmed -> {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Driver Net Balance", color = SettleWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                Text(
                                    text = SangamTariffCalculator.formatRupees(bal.amount),
                                    color = SettleGreen,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Black,
                                    modifier = Modifier.testTag("confirmed_driver_balance")
                                )
                            }
                        }

                        is SangamTariffCalculator.DriverBalanceStatus.RangePendingConfirmation -> {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Driver Net Balance", color = SettleWhite, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    val rangeStr = if (bal.minDriverBalance != null && bal.maxDriverBalance != null) {
                                        "Est. ${SangamTariffCalculator.formatRupees(bal.minDriverBalance)} – ${SangamTariffCalculator.formatRupees(bal.maxDriverBalance)}"
                                    } else {
                                        "Pending commission confirmation"
                                    }
                                    Text(rangeStr, color = SettleAmber, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                                StatusTag(text = "PENDING", bg = SettleBlack, fg = SettleAmber)
                            }
                        }
                    }
                }
            }

            // 5. GET TAXI VENDOR GPAY SETTLEMENT CARD
            Card(
                colors = CardDefaults.cardColors(containerColor = SettleBlack),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, SettleYellow),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("get_taxi_gpay_card")
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        VendorLogoBadge(
                            vendorId = SangamTariffConfig.VENDOR_ID,
                            vendorName = SangamTariffConfig.VENDOR_NAME,
                            height = 34.dp
                        )
                        StatusTag(text = "SETTLEMENT PENDING", bg = SettleCard, fg = SettleAmber)
                    }

                    Text(
                        text = "Pay GET TAXI CC via GPay",
                        color = SettleWhite,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(SettleCard, RoundedCornerShape(14.dp))
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("GET TAXI GPAY NUMBER", color = SettleMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Text(
                                text = SangamTariffConfig.VENDOR_GPAY_NUMBER,
                                color = SettleYellow,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp,
                                modifier = Modifier.testTag("gpay_number_text")
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("GET TAXI GPay", SangamTariffConfig.VENDOR_GPAY_NUMBER))
                                    Toast.makeText(context, "Copied ${SangamTariffConfig.VENDOR_GPAY_NUMBER}", Toast.LENGTH_SHORT).show()
                                },
                                border = BorderStroke(1.dp, SettleYellow),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp),
                                modifier = Modifier
                                    .heightIn(min = 48.dp)
                                    .testTag("copy_gpay_button")
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy GPay Number", tint = SettleYellow, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("COPY", color = SettleYellow, fontWeight = FontWeight.Black, fontSize = 11.sp)
                            }
                            Button(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("GET TAXI GPay", SangamTariffConfig.VENDOR_GPAY_NUMBER))
                                    val gpayIntent = context.packageManager.getLaunchIntentForPackage("com.google.android.apps.nbu.paisa.user")
                                    if (gpayIntent != null) {
                                        gpayIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        context.startActivity(gpayIntent)
                                        Toast.makeText(context, "Copied ${SangamTariffConfig.VENDOR_GPAY_NUMBER} — paste in GPay", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "Copied ${SangamTariffConfig.VENDOR_GPAY_NUMBER} — open GPay/UPI to Pay CC", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = SettleYellow, contentColor = SettleBlack),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp),
                                modifier = Modifier
                                    .heightIn(min = 48.dp)
                                    .testTag("pay_cc_button")
                            ) {
                                Text("Pay CC", fontWeight = FontWeight.Black, fontSize = 12.sp)
                            }
                        }
                    }

                    val commInstruction = when (val comm = breakdown.commissionResult) {
                        is SangamTariffCalculator.CommissionResult.Confirmed ->
                            "Send ${SangamTariffCalculator.formatRupees(comm.amount)} to ${SangamTariffConfig.VENDOR_GPAY_NUMBER} via Google Pay and confirm manually."
                        else ->
                            "Confirm CC amount per approved rule before transferring via Google Pay (${SangamTariffConfig.VENDOR_GPAY_NUMBER})."
                    }
                    Text(text = commInstruction, color = SettleMuted, fontSize = 11.sp, lineHeight = 16.sp)

                    HorizontalDivider(color = SettleBorder)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                val file = PdfGenerator.generateInternalCcReportPdf(context, trip, breakdown, driverProfile)
                                ShareUtils.sharePdf(context, file, "GET TAXI CC Report ${trip.tripNumber}")
                            },
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, SettleYellow),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("share_cc_report_pdf_button")
                        ) {
                            Icon(Icons.Default.Assessment, contentDescription = null, tint = SettleYellow, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("CC REPORT PDF", color = SettleYellow, fontWeight = FontWeight.Black, fontSize = 11.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                val text = ShareUtils.buildInternalCcReportText(trip, breakdown, driverProfile)
                                ShareUtils.shareText(context, text, "GET TAXI Internal CC Report")
                            },
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, SettleBorder),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("share_cc_report_text_button")
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, tint = SettleWhite, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("SHARE CC REPORT", color = SettleWhite, fontWeight = FontWeight.Black, fontSize = 11.sp)
                        }
                    }
                }
            }

            // 6. CUSTOMER INFO & SHARE / BILL PDF
            Card(
                colors = CardDefaults.cardColors(containerColor = SettleCard),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, SettleBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "CUSTOMER BILL & RECEIPT",
                            color = SettleYellow,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        )
                        TextButton(onClick = { showCustomerEditor = !showCustomerEditor }) {
                            Text(
                                text = if (showCustomerEditor) "Hide Details" else "Add Customer Info",
                                color = SettleYellow,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    if (showCustomerEditor) {
                        OutlinedTextField(
                            value = custName,
                            onValueChange = {
                                custName = it
                                viewModel.updateSettlementCustomerInfo(it, custMobile, custAddress)
                            },
                            label = { Text("Customer Name") },
                            colors = settleFieldColors(),
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = custMobile,
                            onValueChange = {
                                custMobile = it
                                viewModel.updateSettlementCustomerInfo(custName, it, custAddress)
                            },
                            label = { Text("Customer Mobile") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            colors = settleFieldColors(),
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = custAddress,
                            onValueChange = {
                                custAddress = it
                                viewModel.updateSettlementCustomerInfo(custName, custMobile, it)
                            },
                            label = { Text("Address / Notes") },
                            colors = settleFieldColors(),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // Customer Payment QR (Driver Profile QR / UPI ID ONLY - never office CC number)
                    val savedQrPath = driverProfile?.qrCodeUri?.takeIf { it.isNotBlank() }
                    val savedUpiId = driverProfile?.upiId?.takeIf { it.isNotBlank() }
                    val driverDisplayName = driverProfile?.driverName?.takeIf { it.isNotBlank() } ?: trip.driverName
                    Surface(
                        color = SettleBlack,
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, SettleBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settlement_customer_upi_qr_card")
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "CUSTOMER SCAN & PAY (DRIVER QR / UPI)",
                                color = SettleYellow,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.8.sp
                            )
                            if (savedQrPath != null) {
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(if (savedQrPath.startsWith("/")) File(savedQrPath) else savedQrPath)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = "Driver Customer Payment QR",
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier
                                        .size(160.dp)
                                        .background(SettleWhite, RoundedCornerShape(12.dp))
                                        .padding(6.dp)
                                )
                            } else if (savedUpiId != null) {
                                val upiQrBitmap: Bitmap? = remember(savedUpiId, driverDisplayName, breakdown.customerBillTotal, trip.tripNumber) {
                                    val uri = UpiQrCodeGenerator.buildUpiUri(
                                        upiId = savedUpiId,
                                        driverName = driverDisplayName,
                                        fareAmount = breakdown.customerBillTotal.toDouble(),
                                        transactionNote = "GET TAXI ${trip.tripNumber}"
                                    )
                                    UpiQrCodeGenerator.generateQrCodeBitmap(uri, sizePx = 440)
                                }
                                if (upiQrBitmap != null) {
                                    Image(
                                        bitmap = upiQrBitmap.asImageBitmap(),
                                        contentDescription = "Customer UPI Payment QR",
                                        contentScale = ContentScale.Fit,
                                        modifier = Modifier
                                            .size(160.dp)
                                            .background(SettleWhite, RoundedCornerShape(12.dp))
                                            .padding(6.dp)
                                    )
                                    Text(
                                        text = "UPI: $savedUpiId",
                                        color = SettleMuted,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            } else {
                                Text(
                                    text = "Add driver UPI ID or Payment QR in Profile for customer scan & pay",
                                    color = SettleMuted,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                val file = PdfGenerator.generateSangamBillPdf(context, trip, breakdown)
                                ShareUtils.sharePdf(context, file, "GET TAXI Bill ${trip.tripNumber}")
                            },
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, SettleYellow),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("share_bill_pdf_button")
                        ) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = SettleYellow, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("BILL PDF", color = SettleYellow, fontWeight = FontWeight.Black, fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                val text = ShareUtils.buildSangamReceiptText(trip, breakdown, includeSettlementNote = false)
                                ShareUtils.shareText(context, text, "GET TAXI Trip Bill")
                            },
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, SettleBorder),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("share_bill_text_button")
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, tint = SettleWhite, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("SHARE TEXT", color = SettleWhite, fontWeight = FontWeight.Black, fontSize = 12.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun StatusTag(text: String, bg: Color, fg: Color) {
    Surface(
        color = bg,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, fg.copy(alpha = 0.4f))
    ) {
        Text(
            text = text,
            color = fg,
            fontSize = 10.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun settleFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = SettleYellow,
    unfocusedBorderColor = SettleBorder,
    focusedLabelColor = SettleYellow,
    unfocusedLabelColor = SettleMuted,
    focusedTextColor = SettleWhite,
    unfocusedTextColor = SettleWhite,
    cursorColor = SettleYellow
)
