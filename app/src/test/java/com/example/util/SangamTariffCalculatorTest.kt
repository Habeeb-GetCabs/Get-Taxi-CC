package com.example.util

import com.example.util.SangamTariffCalculator.CommissionResult
import com.example.util.SangamTariffCalculator.DriverBalanceStatus
import com.example.util.SangamTariffCalculator.TripCalculationInput
import com.example.util.SangamTariffConfig.Above25KmAnd800Rule
import com.example.util.SangamTariffConfig.CommissionBoundaryPolicy
import com.example.util.SangamTariffConfig.LocalCommissionConfig
import com.example.util.SangamTariffConfig.LocalFiveKmBoundaryPolicy
import com.example.util.SangamTariffConfig.SangamTripType
import com.example.util.SangamTariffConfig.VehicleCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class SangamTariffCalculatorTest {

    @Test
    fun `local normal rates compute exact amounts for MINI SEDAN SUV SUV_PLUS INNOVA`() {
        // MINI: 80 + 10*28 = 360; 10 km is an explicit boundary overlap on commission so NeedsConfirmation
        val mini = SangamTariffCalculator.calculateLocalNormal(VehicleCategory.MINI, distanceKm = 4.0)
        assertEquals(BigDecimal("192.00"), mini.customerBillTotal) // 80 + 4*28 = 192
        assertTrue(mini.commissionResult is CommissionResult.Confirmed)
        assertEquals(BigDecimal("30.00"), (mini.commissionResult as CommissionResult.Confirmed).amount)
        assertEquals(BigDecimal("162.00"), (mini.driverBalanceStatus as DriverBalanceStatus.Confirmed).amount)

        val sedan = SangamTariffCalculator.calculateLocalNormal(VehicleCategory.SEDAN, distanceKm = 12.0, isOutsideCity = true, isNightTrip = true)
        // 80 + 12*28 + 80 + 50 = 80 + 336 + 130 = 546
        assertEquals(BigDecimal("546.00"), sedan.customerBillTotal)
        assertEquals(BigDecimal("80.00"), (sedan.commissionResult as CommissionResult.Confirmed).amount)

        val suv = SangamTariffCalculator.calculateLocalNormal(VehicleCategory.SUV, distanceKm = 4.0)
        // 200 + 4*35 = 340
        assertEquals(BigDecimal("340.00"), suv.customerBillTotal)

        val suvPlus = SangamTariffCalculator.calculateLocalNormal(VehicleCategory.SUV_PLUS, distanceKm = 4.0)
        // 250 + 4*45 = 430
        assertEquals(BigDecimal("430.00"), suvPlus.customerBillTotal)

        val innova = SangamTariffCalculator.calculateLocalNormal(VehicleCategory.INNOVA, distanceKm = 4.0)
        // 250 + 4*50 = 450
        assertEquals(BigDecimal("450.00"), innova.customerBillTotal)
    }

    @Test
    fun `local minimum bill slabs and exact 5 KM boundary flag`() {
        val under5 = SangamTariffCalculator.calculateLocalMinimumBill(VehicleCategory.MINI, distanceKm = 3.5)
        assertEquals(BigDecimal("200.00"), under5.customerBillTotal)
        assertTrue(under5.ambiguities.isEmpty())

        // Exact 5.0 KM must flag unresolved overlap instead of silently inventing
        val exact5 = SangamTariffCalculator.calculateLocalMinimumBill(VehicleCategory.SEDAN, distanceKm = 5.0)
        assertTrue(exact5.hasUnresolvedAmbiguity)
        assertEquals("MIN_BILL_5KM_OVERLAP", exact5.ambiguities.first().code)

        // Configurable isolated policy test
        val exact5As250 = SangamTariffCalculator.calculateLocalMinimumBill(
            category = VehicleCategory.SEDAN,
            distanceKm = 5.0,
            fiveKmPolicy = LocalFiveKmBoundaryPolicy.INCLUDE_IN_5_TO_6_KM_250
        )
        assertEquals(BigDecimal("250.00"), exact5As250.customerBillTotal)
        assertTrue(exact5As250.ambiguities.isEmpty())

        val between5And6 = SangamTariffCalculator.calculateLocalMinimumBill(VehicleCategory.MINI, distanceKm = 5.6)
        assertEquals(BigDecimal("250.00"), between5And6.customerBillTotal)

        val above6 = SangamTariffCalculator.calculateLocalMinimumBill(VehicleCategory.MINI, distanceKm = 7.0)
        // 80 + 7*28 = 276
        assertEquals(BigDecimal("276.00"), above6.customerBillTotal)

        // SUV 1-7 KM = 450, >7 KM = 200 + 35/km
        assertEquals(BigDecimal("450.00"), SangamTariffCalculator.calculateLocalMinimumBill(VehicleCategory.SUV, 7.0).customerBillTotal)
        assertEquals(BigDecimal("480.00"), SangamTariffCalculator.calculateLocalMinimumBill(VehicleCategory.SUV, 8.0).customerBillTotal)

        // SUV+ 1-10 KM = 700, >10 KM = 250 + 45/km
        assertEquals(BigDecimal("700.00"), SangamTariffCalculator.calculateLocalMinimumBill(VehicleCategory.SUV_PLUS, 10.0).customerBillTotal)
        assertEquals(BigDecimal("745.00"), SangamTariffCalculator.calculateLocalMinimumBill(VehicleCategory.SUV_PLUS, 11.0).customerBillTotal)

        // INNOVA 1-10 KM = 800, >10 KM = 250 + 50/km
        assertEquals(BigDecimal("800.00"), SangamTariffCalculator.calculateLocalMinimumBill(VehicleCategory.INNOVA, 10.0).customerBillTotal)
        assertEquals(BigDecimal("800.00"), SangamTariffCalculator.calculateLocalMinimumBill(VehicleCategory.INNOVA, 11.0).customerBillTotal)
        assertEquals(BigDecimal("850.00"), SangamTariffCalculator.calculateLocalMinimumBill(VehicleCategory.INNOVA, 12.0).customerBillTotal)
    }

    @Test
    fun `local commission isolates 6-10 km range, 5-6 km gap, 10 km overlap, and above-25-km vs above-800-bill ambiguity`() {
        // 6-10 km (e.g. 8 km, bill 304) must return NeedsConfirmation for ₹60-₹75
        val eightKm = SangamTariffCalculator.calculateLocalCommission(8.0, BigDecimal("304.00"))
        assertTrue(eightKm is CommissionResult.NeedsConfirmation)
        eightKm as CommissionResult.NeedsConfirmation
        assertEquals(BigDecimal("60.00"), eightKm.minPossibleAmount)
        assertEquals(BigDecimal("75.00"), eightKm.maxPossibleAmount)

        // 5-6 km gap (e.g. 5.5 km) must return NeedsConfirmation
        val gap = SangamTariffCalculator.calculateLocalCommission(5.5, BigDecimal("250.00"))
        assertTrue(gap is CommissionResult.NeedsConfirmation)

        // 10 km boundary overlap must return NeedsConfirmation
        val tenKm = SangamTariffCalculator.calculateLocalCommission(10.0, BigDecimal("360.00"))
        assertTrue(tenKm is CommissionResult.NeedsConfirmation)

        // Above 25 km AND Above ₹800 bill must NOT silently invent cumulative vs alternative
        val both = SangamTariffCalculator.calculateLocalCommission(30.0, BigDecimal("1000.00"))
        assertTrue(both is CommissionResult.NeedsConfirmation)

        // Configured explicit rule resolves cleanly
        val altOnly = SangamTariffCalculator.calculateLocalCommission(
            30.0,
            BigDecimal("1000.00"),
            LocalCommissionConfig(
                above25KmAnd800Rule = Above25KmAnd800Rule.ALTERNATIVE_SINGLE_10_PERCENT,
                fiveToSixKmPolicy = CommissionBoundaryPolicy.USE_UPPER_SLAB,
                tenKmBoundaryPolicy = CommissionBoundaryPolicy.USE_UPPER_SLAB
            )
        )
        assertTrue(altOnly is CommissionResult.Confirmed)
        assertEquals(BigDecimal("100.00"), (altOnly as CommissionResult.Confirmed).amount)
    }

    @Test
    fun `hourly rental exact package lookup tables 1h to 12h for all 5 car categories`() {
        val miniExpected = mapOf(
            1 to "350.00", 2 to "700.00", 3 to "1050.00", 4 to "1300.00",
            5 to "1625.00", 6 to "1950.00", 7 to "2275.00", 8 to "2600.00",
            9 to "2925.00", 10 to "3250.00", 11 to "3575.00", 12 to "3900.00"
        )
        miniExpected.forEach { (hrs, amt) ->
            val res = SangamTariffCalculator.calculateHourlyRental(VehicleCategory.MINI, hrs, actualDistanceKm = 5.0)
            assertEquals("MINI ${hrs}h", BigDecimal(amt), res.customerBillTotal)
        }

        val sedanExpected = mapOf(
            1 to "375.00", 2 to "750.00", 3 to "1125.00", 4 to "1400.00",
            5 to "1750.00", 6 to "2100.00", 7 to "2450.00", 8 to "2800.00",
            9 to "3150.00", 10 to "3500.00", 11 to "3850.00", 12 to "4200.00"
        )
        sedanExpected.forEach { (hrs, amt) ->
            val res = SangamTariffCalculator.calculateHourlyRental(VehicleCategory.SEDAN, hrs, actualDistanceKm = 5.0)
            assertEquals("SEDAN ${hrs}h", BigDecimal(amt), res.customerBillTotal)
        }

        // Verify SUV 1h & 2h both 1000, 3h 1275, 12h 5100
        assertEquals(BigDecimal("1000.00"), SangamTariffCalculator.calculateHourlyRental(VehicleCategory.SUV, 1, 10.0).customerBillTotal)
        assertEquals(BigDecimal("1000.00"), SangamTariffCalculator.calculateHourlyRental(VehicleCategory.SUV, 2, 20.0).customerBillTotal)
        assertEquals(BigDecimal("1275.00"), SangamTariffCalculator.calculateHourlyRental(VehicleCategory.SUV, 3, 30.0).customerBillTotal)
        assertEquals(BigDecimal("5100.00"), SangamTariffCalculator.calculateHourlyRental(VehicleCategory.SUV, 12, 120.0).customerBillTotal)

        // Verify SUV+ & INNOVA exact tables
        assertEquals(BigDecimal("1100.00"), SangamTariffCalculator.calculateHourlyRental(VehicleCategory.SUV_PLUS, 2, 20.0).customerBillTotal)
        assertEquals(BigDecimal("5400.00"), SangamTariffCalculator.calculateHourlyRental(VehicleCategory.SUV_PLUS, 12, 120.0).customerBillTotal)
        assertEquals(BigDecimal("1200.00"), SangamTariffCalculator.calculateHourlyRental(VehicleCategory.INNOVA, 2, 20.0).customerBillTotal)
        assertEquals(BigDecimal("5700.00"), SangamTariffCalculator.calculateHourlyRental(VehicleCategory.INNOVA, 12, 120.0).customerBillTotal)

        // MINI extra km + waiting + 10% commission
        val miniWithExtra = SangamTariffCalculator.calculateHourlyRental(
            category = VehicleCategory.MINI,
            packageHours = 2,
            actualDistanceKm = 25.0, // 5 extra km * 18 = 90
            waitingMinutes = 10.0    // 10 * 2.08 = 20.80
        )
        // 700 + 90 + 20.80 = 810.80
        assertEquals(BigDecimal("810.80"), miniWithExtra.customerBillTotal)
        assertEquals(BigDecimal("81.08"), (miniWithExtra.commissionResult as CommissionResult.Confirmed).amount)
        assertEquals(BigDecimal("729.72"), (miniWithExtra.driverBalanceStatus as DriverBalanceStatus.Confirmed).amount)
    }

    @Test
    fun `outstation excludes pass-through toll permit parking from 10 percent commission`() {
        val out = SangamTariffCalculator.calculateOutstation(
            category = VehicleCategory.SEDAN,
            isRoundTrip = true,
            distanceKm = 200.0,
            over12Hours = true,
            tollCharge = 250.0,
            permitCharge = 150.0,
            parkingCharge = 100.0
        )
        // Tariff: 300 + (200 * 15) + 400 = 3700.00
        // Pass-through: 500.00 -> Customer total: 4200.00
        // Commission: 10% of 3700.00 = 370.00
        // Driver balance: 4200 - 370 = 3830.00
        assertEquals(BigDecimal("3700.00"), out.tariffFareBeforePassThrough)
        assertEquals(BigDecimal("500.00"), out.passThroughExtrasTotal)
        assertEquals(BigDecimal("4200.00"), out.customerBillTotal)
        assertEquals(BigDecimal("370.00"), (out.commissionResult as CommissionResult.Confirmed).amount)
        assertEquals(BigDecimal("3830.00"), (out.driverBalanceStatus as DriverBalanceStatus.Confirmed).amount)
    }

    @Test
    fun `day rental, innova crysta, and tempo or urbania rates and missing commission flags`() {
        val innova = SangamTariffCalculator.calculateDayRental(VehicleCategory.INNOVA, distanceKm = 100.0, isInnovaCrysta = false)
        assertEquals(BigDecimal("4000.00"), innova.customerBillTotal) // 2500 + 1500
        assertTrue(innova.commissionResult is CommissionResult.NotSpecified)

        val crysta = SangamTariffCalculator.calculateDayRental(VehicleCategory.INNOVA, distanceKm = 100.0, isInnovaCrysta = true)
        assertEquals(BigDecimal("4600.00"), crysta.customerBillTotal) // 3000 + 1600
        assertTrue(crysta.isInnovaCrysta)
        assertTrue(crysta.commissionResult is CommissionResult.NotSpecified)

        val tt12 = SangamTariffCalculator.calculateDayRental(VehicleCategory.TEMPO_TRAVELLER_12_14, distanceKm = 100.0, after12Hours = true)
        assertEquals(BigDecimal("5800.00"), tt12.customerBillTotal) // 3500 + 1800 + 500
        assertTrue(tt12.commissionResult is CommissionResult.NotSpecified)

        val urbania16 = SangamTariffCalculator.calculateDayRental(VehicleCategory.FORCE_URBANIA_16_18, distanceKm = 100.0, after12Hours = true)
        assertEquals(BigDecimal("9500.00"), urbania16.customerBillTotal) // 6000 + 3000 + 500
        assertTrue(urbania16.commissionResult is CommissionResult.NotSpecified)

        // Tempo/Urbania only support Day Rental
        assertEquals(listOf(SangamTripType.DAY_RENTAL), VehicleCategory.TEMPO_TRAVELLER_12_14.supportedTripTypes())
        assertFalse(SangamTariffCalculator.calculateTripFareAndSettlement(
            TripCalculationInput(VehicleCategory.FORCE_URBANIA_12_14, SangamTripType.LOCAL_NORMAL, distanceKm = 5.0)
        ).warnings.isEmpty())
    }
}
