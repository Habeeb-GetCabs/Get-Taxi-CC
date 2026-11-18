package com.example.util

import com.example.util.SangamTariffConfig.Above25KmAnd800Rule
import com.example.util.SangamTariffConfig.CommissionBoundaryPolicy
import com.example.util.SangamTariffConfig.LocalFiveKmBoundaryPolicy
import com.example.util.SangamTariffConfig.SangamTripType
import com.example.util.SangamTariffConfig.TariffRulesConfig
import com.example.util.SangamTariffConfig.VehicleCategory
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Locale

/**
 * Deterministic, BigDecimal-backed Sangam Tariff & Vendor Settlement engine.
 * Works 100% offline with zero invented rules for unresolved chart boundaries.
 */
object SangamTariffCalculator {

    private val ZERO = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
    private val FIVE = BigDecimal("5")
    private val SIX = BigDecimal("6")
    private val TEN = BigDecimal("10")
    private val TWENTY_FIVE = BigDecimal("25")
    private val EIGHT_HUNDRED = BigDecimal("800")

    fun money(value: BigDecimal): BigDecimal = value.setScale(2, RoundingMode.HALF_UP)
    fun money(value: Double): BigDecimal = BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP)
    fun money(value: String): BigDecimal = BigDecimal(value).setScale(2, RoundingMode.HALF_UP)

    fun formatRupees(value: BigDecimal, currency: String = SangamTariffConfig.CURRENCY_SYMBOL): String {
        val scaled = money(value)
        return "$currency${String.format(Locale.US, "%.2f", scaled.toDouble())}"
    }

    data class FareLineItem(
        val label: String,
        val detail: String,
        val amount: BigDecimal,
        val isPassThrough: Boolean = false
    )

    data class BoundaryAmbiguity(
        val code: String,
        val title: String,
        val description: String,
        val candidateOptions: List<String>
    )

    sealed class CommissionResult {
        data class Confirmed(
            val amount: BigDecimal,
            val ruleLabel: String
        ) : CommissionResult()

        data class NeedsConfirmation(
            val minPossibleAmount: BigDecimal?,
            val maxPossibleAmount: BigDecimal?,
            val candidateLabels: List<String>,
            val reason: String
        ) : CommissionResult()

        data class NotSpecified(
            val reason: String
        ) : CommissionResult()
    }

    sealed class DriverBalanceStatus {
        data class Confirmed(val amount: BigDecimal) : DriverBalanceStatus()
        data class RangePendingConfirmation(
            val minDriverBalance: BigDecimal?,
            val maxDriverBalance: BigDecimal?,
            val note: String
        ) : DriverBalanceStatus()
    }

    data class TripCalculationInput(
        val vehicleCategory: VehicleCategory,
        val tripType: SangamTripType,
        val distanceKm: Double = 0.0,
        val packageHours: Int = 1,
        val waitingMinutes: Double = 0.0,
        val isInnovaCrysta: Boolean = false,
        val isOutsideCity: Boolean = false,
        val isNightTrip: Boolean = false,
        val over12Hours: Boolean = false,
        val tollCharge: Double = 0.0,
        val permitCharge: Double = 0.0,
        val parkingCharge: Double = 0.0,
        val customExtrasTotal: Double = 0.0,
        val rulesConfig: TariffRulesConfig = SangamTariffConfig.defaultRulesConfig
    )

    data class SangamSettlementBreakdown(
        val vehicleCategory: VehicleCategory,
        val tripType: SangamTripType,
        val isInnovaCrysta: Boolean,
        val distanceKm: Double,
        val tariffFareBeforePassThrough: BigDecimal,
        val passThroughExtrasTotal: BigDecimal,
        val customerBillTotal: BigDecimal,
        val lineItems: List<FareLineItem>,
        val commissionResult: CommissionResult,
        val driverBalanceStatus: DriverBalanceStatus,
        val ambiguities: List<BoundaryAmbiguity>,
        val warnings: List<String>
    ) {
        val hasUnresolvedAmbiguity: Boolean
            get() = ambiguities.isNotEmpty() || commissionResult !is CommissionResult.Confirmed
    }

    // =========================================================================
    // TESTABLE INDIVIDUAL RULE FUNCTIONS
    // =========================================================================

    fun calculateLocalNormal(
        category: VehicleCategory,
        distanceKm: Double,
        isOutsideCity: Boolean = false,
        isNightTrip: Boolean = false,
        tollPermitParkingEtc: Double = 0.0
    ): SangamSettlementBreakdown {
        val rate = SangamTariffConfig.localNormalRates[category]
            ?: return unsupportedCombination(category, SangamTripType.LOCAL_NORMAL)

        val distBd = BigDecimal.valueOf(distanceKm.coerceAtLeast(0.0))
        val initial = money(rate.initialCharge)
        val distCharge = money(distBd.multiply(rate.perKmRate))
        val outsideCharge = if (isOutsideCity) money(SangamTariffConfig.localOutsideCityCharge) else ZERO
        val nightCharge = if (isNightTrip) money(SangamTariffConfig.localNightCharge) else ZERO
        val passThrough = money(tollPermitParkingEtc.coerceAtLeast(0.0))

        val lineItems = mutableListOf(
            FareLineItem("Initial Charge (${category.displayName})", "Fixed flag drop", initial),
            FareLineItem(
                "Distance Charge",
                "${String.format(Locale.US, "%.2f", distBd.toDouble())} km × ₹${rate.perKmRate.toInt()}/km",
                distCharge
            )
        )
        if (outsideCharge > ZERO) {
            lineItems.add(FareLineItem("Outside City Limits", "Sangam local extra", outsideCharge))
        }
        if (nightCharge > ZERO) {
            lineItems.add(FareLineItem("Night Charge (11 PM–6 AM)", "Sangam local night charge", nightCharge))
        }
        if (passThrough > ZERO) {
            lineItems.add(FareLineItem("Toll / Parking / Extras", "Additional charges", passThrough, isPassThrough = true))
        }

        val tariffSubtotal = money(initial + distCharge + outsideCharge + nightCharge)
        val customerTotal = money(tariffSubtotal + passThrough)
        val commission = calculateLocalCommission(
            distanceKm = distBd.toDouble(),
            customerBillTotal = customerTotal,
            config = SangamTariffConfig.defaultRulesConfig.localCommissionConfig
        )

        return buildBreakdown(
            category = category,
            tripType = SangamTripType.LOCAL_NORMAL,
            isInnovaCrysta = false,
            distanceKm = distBd.toDouble(),
            tariffSubtotal = tariffSubtotal,
            passThrough = passThrough,
            customerTotal = customerTotal,
            lineItems = lineItems,
            commissionResult = commission,
            ambiguities = emptyList(),
            warnings = emptyList()
        )
    }

    fun calculateLocalMinimumBill(
        category: VehicleCategory,
        distanceKm: Double,
        isOutsideCity: Boolean = false,
        isNightTrip: Boolean = false,
        tollPermitParkingEtc: Double = 0.0,
        fiveKmPolicy: LocalFiveKmBoundaryPolicy = LocalFiveKmBoundaryPolicy.UNRESOLVED_FLAG
    ): SangamSettlementBreakdown {
        val cfg = SangamTariffConfig.localMinimumBillRates[category]
            ?: return unsupportedCombination(category, SangamTripType.LOCAL_MINIMUM_BILL)

        val distBd = BigDecimal.valueOf(distanceKm.coerceAtLeast(0.0))
        val outsideCharge = if (isOutsideCity) money(SangamTariffConfig.localOutsideCityCharge) else ZERO
        val nightCharge = if (isNightTrip) money(SangamTariffConfig.localNightCharge) else ZERO
        val passThrough = money(tollPermitParkingEtc.coerceAtLeast(0.0))

        val ambiguities = mutableListOf<BoundaryAmbiguity>()
        val warnings = mutableListOf<String>()
        val lineItems = mutableListOf<FareLineItem>()

        if (distBd > BigDecimal.ZERO && distBd < BigDecimal.ONE) {
            warnings.add("Distance below 1 KM (${String.format(Locale.US, "%.2f", distBd.toDouble())} km): first minimum-bill slab applied.")
        }

        val basePackageFare: BigDecimal
        if (category == VehicleCategory.MINI || category == VehicleCategory.SEDAN) {
            val isExact5Km = distBd.compareTo(FIVE) == 0
            when {
                distBd < FIVE -> {
                    basePackageFare = money(cfg.firstSlabAmount)
                    lineItems.add(FareLineItem("Minimum Bill (1–5 KM)", "${category.displayName} flat slab", basePackageFare))
                }
                isExact5Km -> {
                    when (fiveKmPolicy) {
                        LocalFiveKmBoundaryPolicy.INCLUDE_IN_1_TO_5_KM_200 -> {
                            basePackageFare = money(cfg.firstSlabAmount)
                            lineItems.add(FareLineItem("Minimum Bill (1–5 KM)", "5 KM boundary policy: ₹200", basePackageFare))
                        }
                        LocalFiveKmBoundaryPolicy.INCLUDE_IN_5_TO_6_KM_250 -> {
                            basePackageFare = money(cfg.secondSlabAmount!!)
                            lineItems.add(FareLineItem("Minimum Bill (5–6 KM)", "5 KM boundary policy: ₹250", basePackageFare))
                        }
                        LocalFiveKmBoundaryPolicy.UNRESOLVED_FLAG -> {
                            basePackageFare = money(cfg.firstSlabAmount)
                            lineItems.add(
                                FareLineItem(
                                    "Minimum Bill (5 KM Boundary Overlap)",
                                    "Chart overlaps 1–5 KM (₹200) & 5–6 KM (₹250) — requires confirmation",
                                    basePackageFare
                                )
                            )
                            ambiguities.add(
                                BoundaryAmbiguity(
                                    code = "MIN_BILL_5KM_OVERLAP",
                                    title = "Unconfirmed 5 KM Boundary (${category.displayName})",
                                    description = "Sangam chart lists both 1–5 KM = ₹200 and 5–6 KM = ₹250. Exact 5.00 KM trip requires manual confirmation.",
                                    candidateOptions = listOf("₹200.00 (1–5 KM slab)", "₹250.00 (5–6 KM slab)")
                                )
                            )
                            warnings.add("UNRESOLVED: 5 KM boundary overlaps ₹200 and ₹250 slabs.")
                        }
                    }
                }
                distBd <= SIX -> {
                    basePackageFare = money(cfg.secondSlabAmount!!)
                    lineItems.add(FareLineItem("Minimum Bill (5–6 KM)", "${category.displayName} flat slab", basePackageFare))
                }
                else -> {
                    val init = money(cfg.aboveLimitInitial)
                    val distCharge = money(distBd.multiply(cfg.aboveLimitPerKm))
                    basePackageFare = money(init + distCharge)
                    lineItems.add(FareLineItem("Initial Charge (>6 KM)", "${category.displayName} minimum-bill rate", init))
                    lineItems.add(
                        FareLineItem(
                            "Distance Charge",
                            "${String.format(Locale.US, "%.2f", distBd.toDouble())} km × ₹${cfg.aboveLimitPerKm.toInt()}/km",
                            distCharge
                        )
                    )
                }
            }
        } else {
            // SUV (1-7 KM = 450, >7 KM = 200 + 35/km), SUV+ (1-10 KM = 700, >10 KM = 250 + 45/km), INNOVA (1-10 KM = 800, >10 KM = 250 + 50/km)
            if (distBd <= cfg.firstSlabMaxKm) {
                basePackageFare = money(cfg.firstSlabAmount)
                lineItems.add(
                    FareLineItem(
                        "Minimum Bill (1–${cfg.firstSlabMaxKm.toInt()} KM)",
                        "${category.displayName} flat slab",
                        basePackageFare
                    )
                )
            } else {
                val init = money(cfg.aboveLimitInitial)
                val distCharge = money(distBd.multiply(cfg.aboveLimitPerKm))
                basePackageFare = money(init + distCharge)
                lineItems.add(
                    FareLineItem(
                        "Initial Charge (>${cfg.firstSlabMaxKm.toInt()} KM)",
                        "${category.displayName} minimum-bill rate",
                        init
                    )
                )
                lineItems.add(
                    FareLineItem(
                        "Distance Charge",
                        "${String.format(Locale.US, "%.2f", distBd.toDouble())} km × ₹${cfg.aboveLimitPerKm.toInt()}/km",
                        distCharge
                    )
                )
            }
        }

        if (outsideCharge > ZERO) {
            lineItems.add(FareLineItem("Outside City Limits", "Sangam local extra", outsideCharge))
        }
        if (nightCharge > ZERO) {
            lineItems.add(FareLineItem("Night Charge (11 PM–6 AM)", "Sangam local night charge", nightCharge))
        }
        if (passThrough > ZERO) {
            lineItems.add(FareLineItem("Toll / Parking / Extras", "Additional charges", passThrough, isPassThrough = true))
        }

        val tariffSubtotal = money(basePackageFare + outsideCharge + nightCharge)
        val customerTotal = money(tariffSubtotal + passThrough)

        return buildBreakdown(
            category = category,
            tripType = SangamTripType.LOCAL_MINIMUM_BILL,
            isInnovaCrysta = false,
            distanceKm = distBd.toDouble(),
            tariffSubtotal = tariffSubtotal,
            passThrough = passThrough,
            customerTotal = customerTotal,
            lineItems = lineItems,
            commissionResult = calculateLocalCommission(distBd.toDouble(), customerTotal),
            ambiguities = ambiguities,
            warnings = warnings
        )
    }

    /**
     * Authoritative Local Commission Evaluator.
     * Preserves all unresolved chart ambiguities without inventing fixed numbers.
     */
    fun calculateLocalCommission(
        distanceKm: Double,
        customerBillTotal: BigDecimal,
        config: SangamTariffConfig.LocalCommissionConfig = SangamTariffConfig.defaultRulesConfig.localCommissionConfig
    ): CommissionResult {
        val distBd = BigDecimal.valueOf(distanceKm.coerceAtLeast(0.0))
        val bill = money(customerBillTotal)
        val tenPercentOfBill = money(bill.multiply(BigDecimal("0.10")))

        val isAbove25Km = distBd > TWENTY_FIVE
        val isAbove800Bill = bill > EIGHT_HUNDRED

        // Base distance-slab candidates
        val distanceSlabResult: CommissionResult = when {
            distBd <= FIVE -> CommissionResult.Confirmed(money("30"), "1–5 KM: ₹30")
            distBd > FIVE && distBd < SIX -> when (config.fiveToSixKmPolicy) {
                CommissionBoundaryPolicy.USE_LOWER_SLAB ->
                    CommissionResult.Confirmed(money("30"), "5–6 KM (Lower Slab): ₹30")
                CommissionBoundaryPolicy.USE_UPPER_SLAB ->
                    evaluateSixToTenCommission(config)
                CommissionBoundaryPolicy.UNRESOLVED_FLAG ->
                    CommissionResult.NeedsConfirmation(
                        minPossibleAmount = money("30"),
                        maxPossibleAmount = money("75"),
                        candidateLabels = listOf("₹30.00 (1–5 KM slab)", "₹60.00–₹75.00 (6–10 KM slab)"),
                        reason = "5–6 KM gap is not explicitly assigned in the Sangam commission chart."
                    )
            }
            distBd >= SIX && distBd < TEN -> evaluateSixToTenCommission(config)
            distBd.compareTo(TEN) == 0 -> when (config.tenKmBoundaryPolicy) {
                CommissionBoundaryPolicy.USE_LOWER_SLAB -> evaluateSixToTenCommission(config)
                CommissionBoundaryPolicy.USE_UPPER_SLAB -> CommissionResult.Confirmed(money("80"), "10–25 KM: ₹80")
                CommissionBoundaryPolicy.UNRESOLVED_FLAG -> {
                    val sixToTenMax = config.sixToTenKmFixedAmount ?: money("75")
                    val sixToTenMin = config.sixToTenKmFixedAmount ?: money("60")
                    CommissionResult.NeedsConfirmation(
                        minPossibleAmount = sixToTenMin,
                        maxPossibleAmount = money("80"),
                        candidateLabels = listOf(
                            if (config.sixToTenKmFixedAmount != null) "₹${sixToTenMax.toPlainString()} (6–10 KM)" else "₹60.00–₹75.00 (6–10 KM)",
                            "₹80.00 (10–25 KM)"
                        ),
                        reason = "10 KM boundary overlaps 6–10 KM (₹60–₹75) and 10–25 KM (₹80)."
                    )
                }
            }
            distBd > TEN && distBd <= TWENTY_FIVE -> CommissionResult.Confirmed(money("80"), "10–25 KM: ₹80")
            else -> CommissionResult.Confirmed(tenPercentOfBill, "Above 25 KM: 10%")
        }

        // Handle Above ₹800 bill and Above 25 KM interaction
        if (isAbove25Km || isAbove800Bill) {
            return when (config.above25KmAnd800Rule) {
                Above25KmAnd800Rule.ALTERNATIVE_SINGLE_10_PERCENT -> {
                    CommissionResult.Confirmed(
                        tenPercentOfBill,
                        if (isAbove25Km && isAbove800Bill) "10% (Alternative rule for >25 KM / >₹800)"
                        else if (isAbove25Km) "Above 25 KM: 10%"
                        else "Above ₹800 bill: 10%"
                    )
                }
                Above25KmAnd800Rule.CUMULATIVE_STACKED -> {
                    if (isAbove25Km && isAbove800Bill) {
                        val combined = money(tenPercentOfBill + tenPercentOfBill)
                        CommissionResult.Confirmed(combined, "Cumulative >25 KM (10%) + >₹800 bill (10%)")
                    } else if (isAbove800Bill && distanceSlabResult is CommissionResult.Confirmed) {
                        val combined = money(distanceSlabResult.amount + tenPercentOfBill)
                        CommissionResult.Confirmed(combined, "Cumulative ${distanceSlabResult.ruleLabel} + >₹800 bill (10%)")
                    } else {
                        distanceSlabResult
                    }
                }
                Above25KmAnd800Rule.UNRESOLVED_NEEDS_CONFIRMATION -> {
                    val candidates = mutableListOf<String>()
                    val possibleAmounts = mutableListOf<BigDecimal>()

                    if (isAbove25Km && isAbove800Bill) {
                        candidates.add("10% once = ${formatRupees(tenPercentOfBill)} (if >25 KM & >₹800 are alternatives)")
                        candidates.add("20% total = ${formatRupees(money(tenPercentOfBill + tenPercentOfBill))} (if cumulative)")
                        possibleAmounts.add(tenPercentOfBill)
                        possibleAmounts.add(money(tenPercentOfBill + tenPercentOfBill))
                    } else if (isAbove800Bill) {
                        when (distanceSlabResult) {
                            is CommissionResult.Confirmed -> {
                                possibleAmounts.add(distanceSlabResult.amount)
                                possibleAmounts.add(tenPercentOfBill)
                                possibleAmounts.add(money(distanceSlabResult.amount + tenPercentOfBill))
                                candidates.add("${distanceSlabResult.ruleLabel} = ${formatRupees(distanceSlabResult.amount)}")
                                candidates.add("10% for >₹800 bill = ${formatRupees(tenPercentOfBill)}")
                                candidates.add("Cumulative = ${formatRupees(money(distanceSlabResult.amount + tenPercentOfBill))}")
                            }
                            is CommissionResult.NeedsConfirmation -> {
                                distanceSlabResult.minPossibleAmount?.let { possibleAmounts.add(it) }
                                possibleAmounts.add(tenPercentOfBill)
                                distanceSlabResult.maxPossibleAmount?.let { possibleAmounts.add(money(it + tenPercentOfBill)) }
                                candidates.addAll(distanceSlabResult.candidateLabels)
                                candidates.add("10% for >₹800 bill = ${formatRupees(tenPercentOfBill)}")
                            }
                            is CommissionResult.NotSpecified -> Unit
                        }
                    } else {
                        // Only isAbove25Km (unlikely unless bill <= 800, still flag 10% vs >800 relationship)
                        possibleAmounts.add(tenPercentOfBill)
                        candidates.add("Above 25 KM: 10% = ${formatRupees(tenPercentOfBill)}")
                    }

                    CommissionResult.NeedsConfirmation(
                        minPossibleAmount = possibleAmounts.minOrNull(),
                        maxPossibleAmount = possibleAmounts.maxOrNull(),
                        candidateLabels = candidates,
                        reason = if (isAbove25Km && isAbove800Bill) {
                            "Chart mentions both 'Above 25 KM: 10%' and 'Above ₹800 bill: 10%' without stating if alternative or cumulative."
                        } else if (isAbove800Bill) {
                            "Bill exceeds ₹800 while distance is ${String.format(Locale.US, "%.2f", distBd.toDouble())} km: chart does not clarify if 'Above ₹800: 10%' replaces or adds to distance slab."
                        } else {
                            "Distance exceeds 25 KM: confirm 10% rule applicability."
                        }
                    )
                }
            }
        }

        return distanceSlabResult
    }

    private fun evaluateSixToTenCommission(config: SangamTariffConfig.LocalCommissionConfig): CommissionResult {
        val fixed = config.sixToTenKmFixedAmount
        return if (fixed != null) {
            CommissionResult.Confirmed(money(fixed), "6–10 KM (Configured): ${formatRupees(fixed)}")
        } else {
            CommissionResult.NeedsConfirmation(
                minPossibleAmount = money("60"),
                maxPossibleAmount = money("75"),
                candidateLabels = listOf("Minimum ₹60.00", "Maximum ₹75.00"),
                reason = "Sangam chart lists a ₹60–₹75 range for 6–10 KM without a fixed selection rule."
            )
        }
    }

    fun calculateHourlyRental(
        category: VehicleCategory,
        packageHours: Int,
        actualDistanceKm: Double,
        waitingMinutes: Double = 0.0,
        tollPermitParkingEtc: Double = 0.0
    ): SangamSettlementBreakdown {
        val table = SangamTariffConfig.hourlyPackageTableFor(category)
            ?: return unsupportedCombination(category, SangamTripType.HOURLY_RENTAL)

        val clampedHours = packageHours.coerceIn(1, 12)
        val packageAmount = money(table.getValue(clampedHours))
        val includedKm = clampedHours * SangamTariffConfig.hourlyIncludedKmPerHour
        val distBd = BigDecimal.valueOf(actualDistanceKm.coerceAtLeast(0.0))
        val includedKmBd = BigDecimal(includedKm)
        val extraKmBd = if (distBd > includedKmBd) distBd.subtract(includedKmBd) else BigDecimal.ZERO

        val warnings = mutableListOf<String>()
        if (packageHours !in 1..12) {
            warnings.add("Sangam Hourly Rental chart covers 1h–12h packages; clamped ${packageHours}h to ${clampedHours}h.")
        }

        val lineItems = mutableListOf(
            FareLineItem(
                "Hourly Package (${clampedHours}h / $includedKm km)",
                "${category.displayName} exact Sangam chart rate",
                packageAmount
            )
        )

        var extraKmCharge = ZERO
        var waitingCharge = ZERO

        if (category == VehicleCategory.MINI || category == VehicleCategory.SEDAN) {
            val extraKmRate = if (category == VehicleCategory.MINI) {
                SangamTariffConfig.hourlyMiniExtraKmRate
            } else {
                SangamTariffConfig.hourlySedanExtraKmRate
            }
            if (extraKmBd > BigDecimal.ZERO) {
                extraKmCharge = money(extraKmBd.multiply(extraKmRate))
                lineItems.add(
                    FareLineItem(
                        "Extra Distance",
                        "${String.format(Locale.US, "%.2f", extraKmBd.toDouble())} km × ₹${extraKmRate.toInt()}/km",
                        extraKmCharge
                    )
                )
            }
            val waitMinsBd = BigDecimal.valueOf(waitingMinutes.coerceAtLeast(0.0))
            if (waitMinsBd > BigDecimal.ZERO) {
                waitingCharge = money(waitMinsBd.multiply(SangamTariffConfig.hourlyMiniSedanWaitingPerMin))
                lineItems.add(
                    FareLineItem(
                        "Waiting Charge",
                        "${String.format(Locale.US, "%.1f", waitMinsBd.toDouble())} min × ₹2.08/min",
                        waitingCharge
                    )
                )
            }
        } else {
            if (extraKmBd > BigDecimal.ZERO) {
                warnings.add("Extra KM (${String.format(Locale.US, "%.2f", extraKmBd.toDouble())} km over $includedKm km): Sangam chart does not define extra KM rate for ${category.displayName}.")
            }
            if (waitingMinutes > 0.0) {
                warnings.add("Waiting minutes (${String.format(Locale.US, "%.1f", waitingMinutes)} min): Sangam chart does not define hourly waiting rate for ${category.displayName}.")
            }
        }

        val passThrough = money(tollPermitParkingEtc.coerceAtLeast(0.0))
        if (passThrough > ZERO) {
            lineItems.add(FareLineItem("Toll / Parking / Extras", "Additional charges", passThrough, isPassThrough = true))
        }

        val tariffSubtotal = money(packageAmount + extraKmCharge + waitingCharge)
        val customerTotal = money(tariffSubtotal + passThrough)
        val commissionAmt = money(tariffSubtotal.multiply(SangamTariffConfig.hourlyCommissionPercent))
        val commission = CommissionResult.Confirmed(commissionAmt, "Hourly Rental: 10% on fare")

        return buildBreakdown(
            category = category,
            tripType = SangamTripType.HOURLY_RENTAL,
            isInnovaCrysta = false,
            distanceKm = distBd.toDouble(),
            tariffSubtotal = tariffSubtotal,
            passThrough = passThrough,
            customerTotal = customerTotal,
            lineItems = lineItems,
            commissionResult = commission,
            ambiguities = emptyList(),
            warnings = warnings
        )
    }

    fun calculateOutstation(
        category: VehicleCategory,
        isRoundTrip: Boolean,
        distanceKm: Double,
        over12Hours: Boolean = false,
        tollCharge: Double = 0.0,
        permitCharge: Double = 0.0,
        parkingCharge: Double = 0.0,
        customExtrasTotal: Double = 0.0
    ): SangamSettlementBreakdown {
        val rate = SangamTariffConfig.outstationRates[category]
            ?: return unsupportedCombination(
                category,
                if (isRoundTrip) SangamTripType.OUTSTATION_ROUND_TRIP else SangamTripType.OUTSTATION_ONE_WAY
            )

        val tripType = if (isRoundTrip) SangamTripType.OUTSTATION_ROUND_TRIP else SangamTripType.OUTSTATION_ONE_WAY
        val distBd = BigDecimal.valueOf(distanceKm.coerceAtLeast(0.0))
        val perKm = if (isRoundTrip) rate.roundTripPerKm else rate.oneWayPerKm

        val initial = money(rate.initialCharge)
        val distCharge = money(distBd.multiply(perKm))
        val allowance = if (over12Hours) money(SangamTariffConfig.outstationOver12HoursDriverAllowance) else ZERO

        val toll = money(tollCharge.coerceAtLeast(0.0))
        val permit = money(permitCharge.coerceAtLeast(0.0))
        val parking = money(parkingCharge.coerceAtLeast(0.0))
        val otherExtras = money(customExtrasTotal.coerceAtLeast(0.0))
        val passThrough = money(toll + permit + parking + otherExtras)

        val lineItems = mutableListOf(
            FareLineItem("Initial Charge (${category.displayName})", "Outstation initial", initial),
            FareLineItem(
                if (isRoundTrip) "Round-Trip Distance" else "One-Way Distance",
                "${String.format(Locale.US, "%.2f", distBd.toDouble())} km × ₹${perKm.toInt()}/km",
                distCharge
            )
        )
        if (allowance > ZERO) {
            lineItems.add(FareLineItem("Driver Allowance (>12 hrs)", "Sangam outstation allowance", allowance))
        }
        if (toll > ZERO) lineItems.add(FareLineItem("Toll Charges", "Pass-through extra", toll, isPassThrough = true))
        if (permit > ZERO) lineItems.add(FareLineItem("Permit Charges", "Pass-through extra", permit, isPassThrough = true))
        if (parking > ZERO) lineItems.add(FareLineItem("Parking Charges", "Pass-through extra", parking, isPassThrough = true))
        if (otherExtras > ZERO) lineItems.add(FareLineItem("Other Extras", "Pass-through extra", otherExtras, isPassThrough = true))

        val tariffSubtotal = money(initial + distCharge + allowance)
        val customerTotal = money(tariffSubtotal + passThrough)
        val commissionAmt = money(tariffSubtotal.multiply(SangamTariffConfig.outstationCommissionPercent))
        val commission = CommissionResult.Confirmed(
            commissionAmt,
            "Outstation: 10% on fare (excluding toll/permit/parking)"
        )

        return buildBreakdown(
            category = category,
            tripType = tripType,
            isInnovaCrysta = false,
            distanceKm = distBd.toDouble(),
            tariffSubtotal = tariffSubtotal,
            passThrough = passThrough,
            customerTotal = customerTotal,
            lineItems = lineItems,
            commissionResult = commission,
            ambiguities = emptyList(),
            warnings = emptyList()
        )
    }

    fun calculateDayRental(
        category: VehicleCategory,
        distanceKm: Double,
        isInnovaCrysta: Boolean = false,
        after12Hours: Boolean = false,
        tollCharge: Double = 0.0,
        permitCharge: Double = 0.0,
        parkingCharge: Double = 0.0,
        customExtrasTotal: Double = 0.0
    ): SangamSettlementBreakdown {
        val effectiveCrysta = (category == VehicleCategory.INNOVA && isInnovaCrysta)
        val rate = if (effectiveCrysta) {
            SangamTariffConfig.innovaCrystaDayRentalRate
        } else {
            SangamTariffConfig.dayRentalRates[category]
                ?: return unsupportedCombination(category, SangamTripType.DAY_RENTAL)
        }

        val labelSuffix = if (effectiveCrysta) "INNOVA CRYSTA" else category.displayName
        val distBd = BigDecimal.valueOf(distanceKm.coerceAtLeast(0.0))
        val base = money(rate.baseAmount)
        val distCharge = money(distBd.multiply(rate.perKmRate))
        val over12Charge = if (after12Hours && rate.after12HoursExtra > BigDecimal.ZERO) {
            money(rate.after12HoursExtra)
        } else {
            ZERO
        }

        val toll = money(tollCharge.coerceAtLeast(0.0))
        val permit = money(permitCharge.coerceAtLeast(0.0))
        val parking = money(parkingCharge.coerceAtLeast(0.0))
        val otherExtras = money(customExtrasTotal.coerceAtLeast(0.0))
        val passThrough = money(toll + permit + parking + otherExtras)

        val lineItems = mutableListOf(
            FareLineItem("Day Rental Base ($labelSuffix)", "Fixed day base", base),
            FareLineItem(
                "Distance Charge",
                "${String.format(Locale.US, "%.2f", distBd.toDouble())} km × ₹${rate.perKmRate.toInt()}/km",
                distCharge
            )
        )
        if (over12Charge > ZERO) {
            lineItems.add(FareLineItem("After 12 Hours Charge", "$labelSuffix >12h extra", over12Charge))
        }
        if (toll > ZERO) lineItems.add(FareLineItem("Toll Charges", "Pass-through extra", toll, isPassThrough = true))
        if (permit > ZERO) lineItems.add(FareLineItem("Permit Charges", "Pass-through extra", permit, isPassThrough = true))
        if (parking > ZERO) lineItems.add(FareLineItem("Parking Charges", "Pass-through extra", parking, isPassThrough = true))
        if (otherExtras > ZERO) lineItems.add(FareLineItem("Other Extras", "Pass-through extra", otherExtras, isPassThrough = true))

        val tariffSubtotal = money(base + distCharge + over12Charge)
        val customerTotal = money(tariffSubtotal + passThrough)

        val reason = if (category.isVanOrTraveller) {
            "Commission rate for ${category.displayName} is not specified in the Sangam chart."
        } else {
            "Commission rate for Day Rental is not specified in the Sangam chart."
        }

        return buildBreakdown(
            category = category,
            tripType = SangamTripType.DAY_RENTAL,
            isInnovaCrysta = effectiveCrysta,
            distanceKm = distBd.toDouble(),
            tariffSubtotal = tariffSubtotal,
            passThrough = passThrough,
            customerTotal = customerTotal,
            lineItems = lineItems,
            commissionResult = CommissionResult.NotSpecified(reason),
            ambiguities = emptyList(),
            warnings = emptyList()
        )
    }

    // =========================================================================
    // UNIFIED DISPATCHER
    // =========================================================================

    fun calculateTripFareAndSettlement(input: TripCalculationInput): SangamSettlementBreakdown {
        val passThroughExtras = input.tollCharge.coerceAtLeast(0.0) +
            input.permitCharge.coerceAtLeast(0.0) +
            input.parkingCharge.coerceAtLeast(0.0) +
            input.customExtrasTotal.coerceAtLeast(0.0)

        if (input.vehicleCategory.isVanOrTraveller && input.tripType != SangamTripType.DAY_RENTAL) {
            return unsupportedCombination(input.vehicleCategory, input.tripType)
        }

        return when (input.tripType) {
            SangamTripType.LOCAL_NORMAL -> calculateLocalNormal(
                category = input.vehicleCategory,
                distanceKm = input.distanceKm,
                isOutsideCity = input.isOutsideCity,
                isNightTrip = input.isNightTrip,
                tollPermitParkingEtc = passThroughExtras
            )
            SangamTripType.LOCAL_MINIMUM_BILL -> calculateLocalMinimumBill(
                category = input.vehicleCategory,
                distanceKm = input.distanceKm,
                isOutsideCity = input.isOutsideCity,
                isNightTrip = input.isNightTrip,
                tollPermitParkingEtc = passThroughExtras,
                fiveKmPolicy = input.rulesConfig.miniSedanFiveKmBoundaryPolicy
            )
            SangamTripType.HOURLY_RENTAL -> calculateHourlyRental(
                category = input.vehicleCategory,
                packageHours = input.packageHours,
                actualDistanceKm = input.distanceKm,
                waitingMinutes = input.waitingMinutes,
                tollPermitParkingEtc = passThroughExtras
            )
            SangamTripType.OUTSTATION_ONE_WAY -> calculateOutstation(
                category = input.vehicleCategory,
                isRoundTrip = false,
                distanceKm = input.distanceKm,
                over12Hours = input.over12Hours,
                tollCharge = input.tollCharge,
                permitCharge = input.permitCharge,
                parkingCharge = input.parkingCharge,
                customExtrasTotal = input.customExtrasTotal
            )
            SangamTripType.OUTSTATION_ROUND_TRIP -> calculateOutstation(
                category = input.vehicleCategory,
                isRoundTrip = true,
                distanceKm = input.distanceKm,
                over12Hours = input.over12Hours,
                tollCharge = input.tollCharge,
                permitCharge = input.permitCharge,
                parkingCharge = input.parkingCharge,
                customExtrasTotal = input.customExtrasTotal
            )
            SangamTripType.DAY_RENTAL -> calculateDayRental(
                category = input.vehicleCategory,
                distanceKm = input.distanceKm,
                isInnovaCrysta = input.isInnovaCrysta,
                after12Hours = input.over12Hours,
                tollCharge = input.tollCharge,
                permitCharge = input.permitCharge,
                parkingCharge = input.parkingCharge,
                customExtrasTotal = input.customExtrasTotal
            )
        }
    }

    private fun buildBreakdown(
        category: VehicleCategory,
        tripType: SangamTripType,
        isInnovaCrysta: Boolean,
        distanceKm: Double,
        tariffSubtotal: BigDecimal,
        passThrough: BigDecimal,
        customerTotal: BigDecimal,
        lineItems: List<FareLineItem>,
        commissionResult: CommissionResult,
        ambiguities: List<BoundaryAmbiguity>,
        warnings: List<String>
    ): SangamSettlementBreakdown {
        val driverBalanceStatus = when (commissionResult) {
            is CommissionResult.Confirmed -> {
                DriverBalanceStatus.Confirmed(money(customerTotal - commissionResult.amount))
            }
            is CommissionResult.NeedsConfirmation -> {
                val minDriver = commissionResult.maxPossibleAmount?.let { money(customerTotal - it) }
                val maxDriver = commissionResult.minPossibleAmount?.let { money(customerTotal - it) }
                DriverBalanceStatus.RangePendingConfirmation(
                    minDriverBalance = minDriver,
                    maxDriverBalance = maxDriver,
                    note = commissionResult.reason
                )
            }
            is CommissionResult.NotSpecified -> {
                DriverBalanceStatus.RangePendingConfirmation(
                    minDriverBalance = null,
                    maxDriverBalance = customerTotal,
                    note = commissionResult.reason
                )
            }
        }

        return SangamSettlementBreakdown(
            vehicleCategory = category,
            tripType = tripType,
            isInnovaCrysta = isInnovaCrysta,
            distanceKm = distanceKm,
            tariffFareBeforePassThrough = tariffSubtotal,
            passThroughExtrasTotal = passThrough,
            customerBillTotal = customerTotal,
            lineItems = lineItems,
            commissionResult = commissionResult,
            driverBalanceStatus = driverBalanceStatus,
            ambiguities = ambiguities,
            warnings = warnings
        )
    }

    private fun unsupportedCombination(
        category: VehicleCategory,
        tripType: SangamTripType
    ): SangamSettlementBreakdown {
        val msg = "${category.displayName} only supports Day Rental in the supplied Sangam chart."
        return SangamSettlementBreakdown(
            vehicleCategory = category,
            tripType = tripType,
            isInnovaCrysta = false,
            distanceKm = 0.0,
            tariffFareBeforePassThrough = ZERO,
            passThroughExtrasTotal = ZERO,
            customerBillTotal = ZERO,
            lineItems = emptyList(),
            commissionResult = CommissionResult.NotSpecified(msg),
            driverBalanceStatus = DriverBalanceStatus.RangePendingConfirmation(null, null, msg),
            ambiguities = emptyList(),
            warnings = listOf(msg)
        )
    }
}
