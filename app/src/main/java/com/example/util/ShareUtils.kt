package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.data.database.DriverProfileEntity
import com.example.data.database.TripEntity
import com.example.data.model.ExtraChargeItem
import com.example.data.model.ServiceType
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

object ShareUtils {

    fun shareText(context: Context, text: String, title: String = "Share Trip Receipt") {
        try {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, text)
                putExtra(Intent.EXTRA_SUBJECT, title)
            }
            context.startActivity(Intent.createChooser(intent, title))
        } catch (e: Exception) {
            android.util.Log.e("ShareUtils", "Failed sharing text message", e)
        }
    }

    fun shareViaSms(context: Context, phoneNumber: String = "", text: String) {
        try {
            val cleanPhone = phoneNumber.trim()
            val smsUri = if (cleanPhone.isNotBlank()) {
                Uri.parse("smsto:$cleanPhone")
            } else {
                Uri.parse("smsto:")
            }
            val smsIntent = Intent(Intent.ACTION_SENDTO, smsUri).apply {
                putExtra("sms_body", text)
            }
            context.startActivity(smsIntent)
        } catch (e: Exception) {
            // Fallback to standard chooser if no dedicated SMS app handles smsto:
            shareText(context, text, "Send SMS Receipt")
        }
    }

    fun sharePdf(context: Context, file: File, title: String) {
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, title)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Share $title"))
        } catch (e: Exception) {
            android.util.Log.e("ShareUtils", "Failed sharing PDF file", e)
        }
    }

    private fun formatDurationHhMmSs(totalSeconds: Long): String {
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
    }

    fun buildVendorReceiptText(trip: TripEntity): String {
        val strings = AppLanguageManager.currentStrings
        val dateFormatter = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.US)
        val dateStr = dateFormatter.format(Date(trip.startTime))

        val vendorMeta = PdfGenerator.resolveVendorHeaderMetadata(trip)
        val brandName = vendorMeta.vendorName

        val driverNameStr = trip.driverName.ifBlank { "DRIVER" }
        val vehicleRegStr = trip.vehicleNumber.ifBlank { "N/A" }
        val vehicleTypeStr = trip.vehicleType.ifBlank { "Sedan" }
        val vehicleModelStr = trip.vehicleModel.ifBlank { "N/A" }

        val currLabel = if (trip.currency.isNotBlank()) trip.currency.trim() else "₹"
        val mode = ServiceType.fromId(trip.serviceType)
        val serviceTitle = if (strings.isTamil) strings.serviceTypeTitle(mode) else mode.title
        val itemizedExtras = ExtraChargeItem.deserialize(trip.extraChargesJson)
        val itemizedSum = ExtraChargeItem.totalAmount(itemizedExtras)

        return buildString {
            append("$brandName - ${strings.officialTripInvoiceHeader}\n")
            if (vendorMeta.displayedBusinessLines.isNotEmpty()) {
                vendorMeta.displayedBusinessLines.forEach { line ->
                    append("$line\n")
                }
            }
            append("\n")
            append("Invoice No: ${vendorMeta.invoiceNumber}\n")
            append("${strings.tripIdPrefix}: ${vendorMeta.tripNumber}\n")
            append("${strings.servicePrefix}: $serviceTitle\n")
            append("${strings.startTimeLabel}: $dateStr\n")
            append("${strings.driverNameLabel}: $driverNameStr ($vehicleRegStr)\n")
            append("${strings.vehicleTypeLabel}: $vehicleTypeStr ($vehicleModelStr)\n")
            if (trip.customerName.isNotBlank() || trip.customerMobile.isNotBlank()) {
                val custName = trip.customerName.ifBlank { strings.customerPrefix }
                val custPhone = if (trip.customerMobile.isNotBlank()) " (${trip.customerMobile})" else ""
                append("${strings.customerPrefix}: $custName$custPhone\n")
            }
            if (trip.customerAddress.isNotBlank()) {
                append("${strings.passengerAddressLabel}: ${trip.customerAddress}\n")
            }

            append("\n${strings.fareBreakdownHeader}:\n")
            when (mode) {
                ServiceType.LOCAL -> {
                    val distFare = trip.distanceKm * trip.farePerKm
                    val waitMinutes = trip.waitingSeconds / 60.0
                    val waitFare = waitMinutes * trip.waitFarePerMin
                    append("${strings.baseFareLabel}: $currLabel ${String.format(Locale.US, "%.2f", trip.baseFare)}\n")
                    append("${strings.distanceFareLabel}: $currLabel ${String.format(Locale.US, "%.2f", distFare)}\n")
                    append("${strings.waitingFareLabel}: $currLabel ${String.format(Locale.US, "%.2f", waitFare)}\n")
                }
                ServiceType.RENTAL -> {
                    val calc = FareCalculator.calculateHourlyRentalFare(
                        durationSeconds = trip.durationSeconds,
                        distanceKm = trip.distanceKm,
                        hourlyRate = trip.rentalBaseFare,
                        includedKmPerHour = if (trip.rentalBaseKm > 0.0) trip.rentalBaseKm else 10.0,
                        extraKmRate = trip.rentalExtraKmRate
                    )

                    append("${strings.serviceTypeHeader}: ${strings.hourlyRental}\n")
                    append("${strings.billableHoursLabel}: ${calc.billableHours} ${strings.unitHr}\n")
                    append("${strings.hourlyChargeLabel}: $currLabel ${String.format(Locale.US, "%.2f", calc.hourlyCharge)}\n")
                    append("${strings.includedDistanceAllowanceLabel}: ${String.format(Locale.US, "%.2f", calc.includedKmPerHour)} km/hr (${String.format(Locale.US, "%.2f", calc.totalIncludedKm)} km)\n")
                    append("${strings.actualDistanceDrivenLabel}: ${String.format(Locale.US, "%.2f", calc.actualDistanceKm)} KM\n")
                    if (calc.extraDistanceCharge > 0) {
                        append("${strings.extraDistanceChargeLabel}: $currLabel ${String.format(Locale.US, "%.2f", calc.extraDistanceCharge)}\n")
                    }
                }
                ServiceType.OUTSTATION -> {
                    val distFare = trip.distanceKm * trip.outstationRatePerKm
                    val waitHours = trip.waitingSeconds / 3600.0
                    val waitFare = if (trip.outstationWaitRate > 0) waitHours * trip.outstationWaitRate else 0.0

                    append("${strings.outstationDistanceChargeLabel}: $currLabel ${String.format(Locale.US, "%.2f", distFare)}\n")
                    append("${strings.driverAllowanceBataLabel}: $currLabel ${String.format(Locale.US, "%.2f", trip.outstationDriverBata)}\n")
                    if (waitFare > 0) {
                        append("${strings.waitingFareLabel}: $currLabel ${String.format(Locale.US, "%.2f", waitFare)}\n")
                    }
                }
            }

            if (itemizedExtras.isNotEmpty()) {
                itemizedExtras.forEach { item ->
                    append("${strings.extraChargeFallback} (${item.label}): $currLabel ${String.format(Locale.US, "%.2f", item.amount)}\n")
                }
                val rem = trip.outstationAdditionalCharges - itemizedSum
                if (rem > 0.01) {
                    append("${strings.additionalChargesLabel}: $currLabel ${String.format(Locale.US, "%.2f", rem)}\n")
                }
            } else if (trip.outstationAdditionalCharges > 0.0) {
                append("${strings.extraChargesLabel}: $currLabel ${String.format(Locale.US, "%.2f", trip.outstationAdditionalCharges)}\n")
            }

            append("\n${strings.distance}: *${String.format(Locale.US, "%.2f", trip.distanceKm)} km*\n")
            append("${strings.duration}: ${formatDurationHhMmSs(trip.durationSeconds)}\n\n")

            append("${strings.totalPayableLabel}: *$currLabel${String.format(Locale.US, "%.2f", trip.totalFare)}*\n\n")

            append("${strings.pickupPointLabel}: ${trip.pickupAddress.ifBlank { strings.gpsCoordinatesFallback }}\n")
            append("${strings.dropPointLabel}: ${trip.dropAddress.ifBlank { strings.gpsCoordinatesFallback }}\n")

            val startLat = trip.startLatitude
            val startLng = trip.startLongitude
            val endLat = trip.endLatitude
            val endLng = trip.endLongitude

            if (startLat != 0.0 && startLng != 0.0 && endLat != 0.0 && endLng != 0.0) {
                append("Route Map: https://www.google.com/maps/dir/?api=1&origin=$startLat,$startLng&destination=$endLat,$endLng\n\n")
            } else if (startLat != 0.0 && startLng != 0.0) {
                append("Route Map: https://www.google.com/maps/search/?api=1&query=$startLat,$startLng\n\n")
            } else {
                append("\n")
            }

            append("Thank you for riding with $brandName!")
        }
    }

    fun buildCustomerReceiptText(trip: TripEntity): String {
        return buildVendorReceiptText(trip)
    }

    fun buildVendorStatementText(trip: TripEntity): String {
        return buildVendorReceiptText(trip)
    }

    /**
     * Customer-facing bill text ONLY. Strictly excludes Driver CC, commission rules,
     * Driver Net Balance, and internal CC settlement GPay numbers.
     */
    fun buildSangamReceiptText(
        trip: TripEntity,
        breakdown: SangamTariffCalculator.SangamSettlementBreakdown,
        @Suppress("UNUSED_PARAMETER") includeSettlementNote: Boolean = false
    ): String {
        val catLabel = if (breakdown.isInnovaCrysta) "INNOVA CRYSTA" else breakdown.vehicleCategory.displayName
        val dateStr = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.US)
            .format(Date(trip.endTime.takeIf { it > 0L } ?: System.currentTimeMillis()))

        return buildString {
            append("*GET TAXI — Official Trip Bill*\n")
            append("Bill No: ${trip.tripNumber}\n")
            append("Date: $dateStr\n")
            append("Vehicle: $catLabel\n")
            append("Tariff: ${breakdown.tripType.displayName}\n")
            if (trip.customerName.isNotBlank()) {
                append("Customer: ${trip.customerName}")
                if (trip.customerMobile.isNotBlank()) append(" (${trip.customerMobile})")
                append("\n")
            }
            if (trip.driverName.isNotBlank() || trip.vehicleNumber.isNotBlank()) {
                append("Driver: ${trip.driverName.ifBlank { "GET TAXI Driver" }}")
                if (trip.vehicleNumber.isNotBlank()) append(" • ${trip.vehicleNumber}")
                append("\n")
            }
            append("Distance: ${String.format(Locale.US, "%.2f", breakdown.distanceKm)} km\n")
            append("Duration: ${formatDurationHhMmSs(trip.durationSeconds)}\n\n")
            append("*Fare Breakdown:*\n")
            breakdown.lineItems.forEach { item ->
                append("• ${item.label} (${item.detail}): ${SangamTariffCalculator.formatRupees(item.amount)}\n")
            }
            append("\n*Total Customer Bill: ${SangamTariffCalculator.formatRupees(breakdown.customerBillTotal)}*\n")
            append("\nThank you for travelling with GET TAXI!")
        }
    }

    /**
     * Separate Driver / Office Internal CC Report containing Driver ID, Vehicle Details,
     * Trip Category, Approved CC amount & rule, Driver Net Balance, and GET TAXI CC GPay number.
     */
    fun buildInternalCcReportText(
        trip: TripEntity,
        breakdown: SangamTariffCalculator.SangamSettlementBreakdown,
        driverProfile: DriverProfileEntity? = null
    ): String {
        val catLabel = if (breakdown.isInnovaCrysta) "INNOVA CRYSTA" else breakdown.vehicleCategory.displayName
        val dateStr = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.US)
            .format(Date(trip.endTime.takeIf { it > 0L } ?: System.currentTimeMillis()))
        val driverName = driverProfile?.driverName?.ifBlank { null } ?: trip.driverName.ifBlank { "GET TAXI Driver" }
        val driverId = driverProfile?.driverId?.ifBlank { null } ?: "N/A"
        val vehicleReg = driverProfile?.vehicleNumber?.ifBlank { null } ?: trip.vehicleNumber.ifBlank { "N/A" }

        return buildString {
            append("*GET TAXI — Internal CC Report*\n")
            append("Report / Bill ID: ${trip.tripNumber}\n")
            append("Date: $dateStr\n")
            append("Driver: $driverName (ID: $driverId)\n")
            append("Vehicle: $vehicleReg • $catLabel\n")
            append("Trip Type: ${breakdown.tripType.displayName}\n")
            append("Distance: ${String.format(Locale.US, "%.2f", breakdown.distanceKm)} km\n")
            append("Duration: ${formatDurationHhMmSs(trip.durationSeconds)}\n")
            append("Customer Bill Total: ${SangamTariffCalculator.formatRupees(breakdown.customerBillTotal)}\n")

            when (val comm = breakdown.commissionResult) {
                is SangamTariffCalculator.CommissionResult.Confirmed -> {
                    append("Approved CC (${comm.ruleLabel}): ${SangamTariffCalculator.formatRupees(comm.amount)}\n")
                }
                is SangamTariffCalculator.CommissionResult.NeedsConfirmation -> {
                    append("CC Status: NEEDS CONFIRMATION (${comm.reason})\n")
                }
                is SangamTariffCalculator.CommissionResult.NotSpecified -> {
                    append("CC Status: NOT SPECIFIED (${comm.reason})\n")
                }
            }

            when (val bal = breakdown.driverBalanceStatus) {
                is SangamTariffCalculator.DriverBalanceStatus.Confirmed -> {
                    append("Driver Net Balance: ${SangamTariffCalculator.formatRupees(bal.amount)}\n")
                }
                is SangamTariffCalculator.DriverBalanceStatus.RangePendingConfirmation -> {
                    val rangeStr = if (bal.minDriverBalance != null && bal.maxDriverBalance != null) {
                        "${SangamTariffCalculator.formatRupees(bal.minDriverBalance)} – ${SangamTariffCalculator.formatRupees(bal.maxDriverBalance)}"
                    } else {
                        "Pending Confirmation"
                    }
                    append("Driver Net Balance: $rangeStr\n")
                }
            }

            append("GET TAXI CC GPay: ${SangamTariffConfig.VENDOR_GPAY_NUMBER}")
        }
    }
}
