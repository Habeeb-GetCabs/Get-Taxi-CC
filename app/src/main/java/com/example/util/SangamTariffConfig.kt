package com.example.util

import java.math.BigDecimal

/**
 * Authoritative Sangam Tariff Configuration for GET TAXI.
 *
 * Vehicle classifications, fixed rate cards, exact 1h-12h lookup tables, and
 * unresolved boundary/commission rules are isolated here for verification
 * and testability without silent assumptions.
 */
object SangamTariffConfig {

    const val VENDOR_ID = "get_taxi"
    const val VENDOR_NAME = "GET TAXI"
    const val VENDOR_GPAY_NUMBER = "9043743777"
    const val CURRENCY_SYMBOL = "₹"

    enum class VehicleCategory(
        val id: String,
        val displayName: String,
        val shortLabel: String,
        val exampleModels: String,
        val isVanOrTraveller: Boolean = false
    ) {
        MINI(
            id = "MINI",
            displayName = "MINI",
            shortLabel = "MINI",
            exampleModels = "Alto, WagonR, Santro, Swift"
        ),
        SEDAN(
            id = "SEDAN",
            displayName = "SEDAN",
            shortLabel = "SEDAN",
            exampleModels = "Dzire, Aura, Amaze, Etios"
        ),
        SUV(
            id = "SUV",
            displayName = "SUV",
            shortLabel = "SUV",
            exampleModels = "Ertiga & similar 6–7 seaters"
        ),
        SUV_PLUS(
            id = "SUV_PLUS",
            displayName = "SUV+",
            shortLabel = "SUV+",
            exampleModels = "Ertiga, Rumion, Xylo, Marazzo (Premium)"
        ),
        INNOVA(
            id = "INNOVA",
            displayName = "INNOVA",
            shortLabel = "INNOVA",
            exampleModels = "Innova, Innova Crysta, Innova Hycross"
        ),
        TEMPO_TRAVELLER_12_14(
            id = "TEMPO_TRAVELLER_12_14",
            displayName = "Tempo Traveller 12–14 AC",
            shortLabel = "TT 12–14 AC",
            exampleModels = "Tempo Traveller 12–14 seater AC",
            isVanOrTraveller = true
        ),
        TEMPO_TRAVELLER_16_18(
            id = "TEMPO_TRAVELLER_16_18",
            displayName = "Tempo Traveller 16–18 AC",
            shortLabel = "TT 16–18 AC",
            exampleModels = "Tempo Traveller 16–18 seater AC",
            isVanOrTraveller = true
        ),
        FORCE_URBANIA_12_14(
            id = "FORCE_URBANIA_12_14",
            displayName = "Force Urbania 12–14 AC",
            shortLabel = "Urbania 12–14",
            exampleModels = "Force Urbania 12–14 seater AC",
            isVanOrTraveller = true
        ),
        FORCE_URBANIA_16_18(
            id = "FORCE_URBANIA_16_18",
            displayName = "Force Urbania 16–18 AC",
            shortLabel = "Urbania 16–18",
            exampleModels = "Force Urbania 16–18 seater AC",
            isVanOrTraveller = true
        );

        fun supportedTripTypes(): List<SangamTripType> {
            return if (isVanOrTraveller) {
                listOf(SangamTripType.DAY_RENTAL)
            } else {
                SangamTripType.entries
            }
        }

        companion object {
            fun fromId(id: String?): VehicleCategory {
                return entries.firstOrNull { it.id.equals(id?.trim(), ignoreCase = true) } ?: MINI
            }
        }
    }

    enum class SangamTripType(
        val id: String,
        val displayName: String,
        val shortLabel: String
    ) {
        LOCAL_NORMAL("LOCAL_NORMAL", "Local Normal", "Local"),
        LOCAL_MINIMUM_BILL("LOCAL_MINIMUM_BILL", "Local Min Bill", "Min Bill"),
        HOURLY_RENTAL("HOURLY_RENTAL", "Hourly Rental", "Hourly"),
        OUTSTATION_ONE_WAY("OUTSTATION_ONE_WAY", "Outstation One-Way", "One-Way"),
        OUTSTATION_ROUND_TRIP("OUTSTATION_ROUND_TRIP", "Outstation Round-Trip", "Round-Trip"),
        DAY_RENTAL("DAY_RENTAL", "Day Rental", "Day Rent");

        companion object {
            fun fromId(id: String?): SangamTripType {
                return entries.firstOrNull { it.id.equals(id?.trim(), ignoreCase = true) }
                    ?: LOCAL_NORMAL
            }
        }
    }

    // =========================================================================
    // ISOLATED UNRESOLVED CHART RULES (MUST BE EXPLICITLY FLAGGED FOR VERIFICATION)
    // =========================================================================

    /**
     * MINI / SEDAN Minimum-Bill chart specifies:
     *  - 1–5 KM: ₹200
     *  - 5–6 KM: ₹250
     * Distance == 5.0 KM overlaps both tiers. Do not silently invent a winner.
     */
    enum class LocalFiveKmBoundaryPolicy {
        UNRESOLVED_FLAG,
        INCLUDE_IN_1_TO_5_KM_200,
        INCLUDE_IN_5_TO_6_KM_250
    }

    /**
     * Local commission chart specifies:
     *  - Above 25 KM: 10%
     *  - Above ₹800 bill: 10% is also mentioned.
     * It is unspecified whether these conditions are alternatives (max 10%) or cumulative (10% + 10% or slab + 10%).
     */
    enum class Above25KmAnd800Rule {
        UNRESOLVED_NEEDS_CONFIRMATION,
        ALTERNATIVE_SINGLE_10_PERCENT,
        CUMULATIVE_STACKED
    }

    enum class CommissionBoundaryPolicy {
        UNRESOLVED_FLAG,
        USE_LOWER_SLAB,
        USE_UPPER_SLAB
    }

    data class LocalCommissionConfig(
        val sixToTenKmFixedAmount: BigDecimal? = null,
        val above25KmAnd800Rule: Above25KmAnd800Rule = Above25KmAnd800Rule.UNRESOLVED_NEEDS_CONFIRMATION,
        val fiveToSixKmPolicy: CommissionBoundaryPolicy = CommissionBoundaryPolicy.UNRESOLVED_FLAG,
        val tenKmBoundaryPolicy: CommissionBoundaryPolicy = CommissionBoundaryPolicy.UNRESOLVED_FLAG
    )

    data class TariffRulesConfig(
        val miniSedanFiveKmBoundaryPolicy: LocalFiveKmBoundaryPolicy = LocalFiveKmBoundaryPolicy.UNRESOLVED_FLAG,
        val localCommissionConfig: LocalCommissionConfig = LocalCommissionConfig()
    )

    val defaultRulesConfig = TariffRulesConfig()

    // =========================================================================
    // LOCAL TARIFF TABLES
    // =========================================================================

    data class LocalNormalRate(
        val initialCharge: BigDecimal,
        val perKmRate: BigDecimal
    )

    val localNormalRates: Map<VehicleCategory, LocalNormalRate> = mapOf(
        VehicleCategory.MINI to LocalNormalRate(BigDecimal("80"), BigDecimal("28")),
        VehicleCategory.SEDAN to LocalNormalRate(BigDecimal("80"), BigDecimal("28")),
        VehicleCategory.SUV to LocalNormalRate(BigDecimal("200"), BigDecimal("35")),
        VehicleCategory.SUV_PLUS to LocalNormalRate(BigDecimal("250"), BigDecimal("45")),
        VehicleCategory.INNOVA to LocalNormalRate(BigDecimal("250"), BigDecimal("50"))
    )

    val localOutsideCityCharge: BigDecimal = BigDecimal("80")
    val localNightCharge: BigDecimal = BigDecimal("50") // 11 PM – 6 AM

    data class LocalMinimumBillConfig(
        val firstSlabMaxKm: BigDecimal,
        val firstSlabAmount: BigDecimal,
        val secondSlabMaxKm: BigDecimal? = null,
        val secondSlabAmount: BigDecimal? = null,
        val aboveLimitInitial: BigDecimal,
        val aboveLimitPerKm: BigDecimal
    )

    val localMinimumBillRates: Map<VehicleCategory, LocalMinimumBillConfig> = mapOf(
        VehicleCategory.MINI to LocalMinimumBillConfig(
            firstSlabMaxKm = BigDecimal("5"),
            firstSlabAmount = BigDecimal("200"),
            secondSlabMaxKm = BigDecimal("6"),
            secondSlabAmount = BigDecimal("250"),
            aboveLimitInitial = BigDecimal("80"),
            aboveLimitPerKm = BigDecimal("28")
        ),
        VehicleCategory.SEDAN to LocalMinimumBillConfig(
            firstSlabMaxKm = BigDecimal("5"),
            firstSlabAmount = BigDecimal("200"),
            secondSlabMaxKm = BigDecimal("6"),
            secondSlabAmount = BigDecimal("250"),
            aboveLimitInitial = BigDecimal("80"),
            aboveLimitPerKm = BigDecimal("28")
        ),
        VehicleCategory.SUV to LocalMinimumBillConfig(
            firstSlabMaxKm = BigDecimal("7"),
            firstSlabAmount = BigDecimal("450"),
            aboveLimitInitial = BigDecimal("200"),
            aboveLimitPerKm = BigDecimal("35")
        ),
        VehicleCategory.SUV_PLUS to LocalMinimumBillConfig(
            firstSlabMaxKm = BigDecimal("10"),
            firstSlabAmount = BigDecimal("700"),
            aboveLimitInitial = BigDecimal("250"),
            aboveLimitPerKm = BigDecimal("45")
        ),
        VehicleCategory.INNOVA to LocalMinimumBillConfig(
            firstSlabMaxKm = BigDecimal("10"),
            firstSlabAmount = BigDecimal("800"),
            aboveLimitInitial = BigDecimal("250"),
            aboveLimitPerKm = BigDecimal("50")
        )
    )

    // =========================================================================
    // HOURLY RENTAL EXACT LOOKUP TABLES (1h – 12h) — NEVER REPLACED BY FORMULA
    // =========================================================================

    val hourlyIncludedKmPerHour: Int = 10
    val hourlyMiniExtraKmRate: BigDecimal = BigDecimal("18")
    val hourlySedanExtraKmRate: BigDecimal = BigDecimal("20")
    val hourlyMiniSedanWaitingPerMin: BigDecimal = BigDecimal("2.08")
    val hourlyCommissionPercent: BigDecimal = BigDecimal("0.10")

    val hourlyPackagesMini: Map<Int, BigDecimal> = mapOf(
        1 to BigDecimal("350"),
        2 to BigDecimal("700"),
        3 to BigDecimal("1050"),
        4 to BigDecimal("1300"),
        5 to BigDecimal("1625"),
        6 to BigDecimal("1950"),
        7 to BigDecimal("2275"),
        8 to BigDecimal("2600"),
        9 to BigDecimal("2925"),
        10 to BigDecimal("3250"),
        11 to BigDecimal("3575"),
        12 to BigDecimal("3900")
    )

    val hourlyPackagesSedan: Map<Int, BigDecimal> = mapOf(
        1 to BigDecimal("375"),
        2 to BigDecimal("750"),
        3 to BigDecimal("1125"),
        4 to BigDecimal("1400"),
        5 to BigDecimal("1750"),
        6 to BigDecimal("2100"),
        7 to BigDecimal("2450"),
        8 to BigDecimal("2800"),
        9 to BigDecimal("3150"),
        10 to BigDecimal("3500"),
        11 to BigDecimal("3850"),
        12 to BigDecimal("4200")
    )

    val hourlyPackagesSuv: Map<Int, BigDecimal> = mapOf(
        1 to BigDecimal("1000"),
        2 to BigDecimal("1000"),
        3 to BigDecimal("1275"),
        4 to BigDecimal("1700"),
        5 to BigDecimal("2125"),
        6 to BigDecimal("2550"),
        7 to BigDecimal("2975"),
        8 to BigDecimal("3400"),
        9 to BigDecimal("3825"),
        10 to BigDecimal("4250"),
        11 to BigDecimal("4675"),
        12 to BigDecimal("5100")
    )

    val hourlyPackagesSuvPlus: Map<Int, BigDecimal> = mapOf(
        1 to BigDecimal("1100"),
        2 to BigDecimal("1100"),
        3 to BigDecimal("1350"),
        4 to BigDecimal("1800"),
        5 to BigDecimal("2250"),
        6 to BigDecimal("2700"),
        7 to BigDecimal("3150"),
        8 to BigDecimal("3600"),
        9 to BigDecimal("4050"),
        10 to BigDecimal("4500"),
        11 to BigDecimal("4950"),
        12 to BigDecimal("5400")
    )

    val hourlyPackagesInnova: Map<Int, BigDecimal> = mapOf(
        1 to BigDecimal("1200"),
        2 to BigDecimal("1200"),
        3 to BigDecimal("1425"),
        4 to BigDecimal("1900"),
        5 to BigDecimal("2375"),
        6 to BigDecimal("2850"),
        7 to BigDecimal("3325"),
        8 to BigDecimal("3800"),
        9 to BigDecimal("4275"),
        10 to BigDecimal("4750"),
        11 to BigDecimal("5225"),
        12 to BigDecimal("5700")
    )

    fun hourlyPackageTableFor(category: VehicleCategory): Map<Int, BigDecimal>? {
        return when (category) {
            VehicleCategory.MINI -> hourlyPackagesMini
            VehicleCategory.SEDAN -> hourlyPackagesSedan
            VehicleCategory.SUV -> hourlyPackagesSuv
            VehicleCategory.SUV_PLUS -> hourlyPackagesSuvPlus
            VehicleCategory.INNOVA -> hourlyPackagesInnova
            else -> null
        }
    }

    // =========================================================================
    // OUTSTATION TARIFF TABLES
    // =========================================================================

    data class OutstationRate(
        val initialCharge: BigDecimal,
        val oneWayPerKm: BigDecimal,
        val roundTripPerKm: BigDecimal
    )

    val outstationRates: Map<VehicleCategory, OutstationRate> = mapOf(
        VehicleCategory.MINI to OutstationRate(BigDecimal("300"), BigDecimal("28"), BigDecimal("14")),
        VehicleCategory.SEDAN to OutstationRate(BigDecimal("300"), BigDecimal("28"), BigDecimal("15")),
        VehicleCategory.SUV to OutstationRate(BigDecimal("400"), BigDecimal("35"), BigDecimal("17")),
        VehicleCategory.SUV_PLUS to OutstationRate(BigDecimal("400"), BigDecimal("36"), BigDecimal("18")),
        VehicleCategory.INNOVA to OutstationRate(BigDecimal("400"), BigDecimal("40"), BigDecimal("20"))
    )

    val outstationOver12HoursDriverAllowance: BigDecimal = BigDecimal("400")
    val outstationCommissionPercent: BigDecimal = BigDecimal("0.10")

    // =========================================================================
    // DAY RENTAL & TEMPO TRAVELLER / FORCE URBANIA TABLES
    // =========================================================================

    data class DayRentalRate(
        val baseAmount: BigDecimal,
        val perKmRate: BigDecimal,
        val after12HoursExtra: BigDecimal = BigDecimal.ZERO
    )

    val dayRentalRates: Map<VehicleCategory, DayRentalRate> = mapOf(
        VehicleCategory.MINI to DayRentalRate(BigDecimal("2000"), BigDecimal("10")),
        VehicleCategory.SEDAN to DayRentalRate(BigDecimal("2000"), BigDecimal("10")),
        VehicleCategory.SUV to DayRentalRate(BigDecimal("2300"), BigDecimal("13")),
        VehicleCategory.SUV_PLUS to DayRentalRate(BigDecimal("2400"), BigDecimal("14")),
        VehicleCategory.INNOVA to DayRentalRate(BigDecimal("2500"), BigDecimal("15")),
        VehicleCategory.TEMPO_TRAVELLER_12_14 to DayRentalRate(
            baseAmount = BigDecimal("3500"),
            perKmRate = BigDecimal("18"),
            after12HoursExtra = BigDecimal("500")
        ),
        VehicleCategory.TEMPO_TRAVELLER_16_18 to DayRentalRate(
            baseAmount = BigDecimal("4000"),
            perKmRate = BigDecimal("20"),
            after12HoursExtra = BigDecimal("500")
        ),
        VehicleCategory.FORCE_URBANIA_12_14 to DayRentalRate(
            baseAmount = BigDecimal("5000"),
            perKmRate = BigDecimal("28"),
            after12HoursExtra = BigDecimal("500")
        ),
        VehicleCategory.FORCE_URBANIA_16_18 to DayRentalRate(
            baseAmount = BigDecimal("6000"),
            perKmRate = BigDecimal("30"),
            after12HoursExtra = BigDecimal("500")
        )
    )

    val innovaCrystaDayRentalRate = DayRentalRate(BigDecimal("3000"), BigDecimal("16"))
}
