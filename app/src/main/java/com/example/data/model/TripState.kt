package com.example.data.model

import com.example.util.SangamTariffConfig

data class TripState(
    val status: TripStatus = TripStatus.IDLE,
    val distanceKm: Double = 0.0,
    val durationSeconds: Long = 0L,
    val waitingSeconds: Long = 0L,
    val currentFare: Double = 0.0,
    val speedKmH: Float = 0f,
    val isMoving: Boolean = false,
    val isWaitingPaused: Boolean = false,
    val baseFare: Double = 80.0,
    val farePerKm: Double = 28.0,
    val waitFarePerMin: Double = 0.0,
    val currency: String = "₹",
    val speedThreshold: Double = 5.0,
    val startTime: Long = 0L,
    val endTime: Long = 0L,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val bearing: Float = 0f,
    val isSimulationEnabled: Boolean = false,
    val isOutOfCity: Boolean = false,
    val outOfCitySurchargePercent: Double = 0.0,
    val outOfCitySurchargeAmount: Double = 0.0,
    val pickupAddress: String = "",
    val dropAddress: String = "",
    val tripNumber: String = "",
    val customerName: String = "",
    val customerMobile: String = "",
    val customerAddress: String = "",
    val serviceType: String = "LOCAL",
    // Sangam Tariff Selection & Modifiers
    val vehicleCategoryId: String = SangamTariffConfig.VehicleCategory.MINI.id,
    val sangamTripTypeId: String = SangamTariffConfig.SangamTripType.LOCAL_NORMAL.id,
    val isInnovaCrysta: Boolean = false,
    val isNightTrip: Boolean = false,
    val packageHours: Int = 1,
    val over12Hours: Boolean = false,
    val tollCharge: Double = 0.0,
    val permitCharge: Double = 0.0,
    val parkingCharge: Double = 0.0,
    val hasUnresolvedBoundaryFlag: Boolean = false,
    // Hourly Rental
    val rentalHourlyRate: Double = 350.0,
    val rentalMinHours: Double = 1.0,
    val rentalIncludedKmPerHour: Double = 10.0,
    val rentalExtraHrRate: Double = 0.0,
    val rentalExtraKmRate: Double = 18.0,
    val billableHours: Long = 1L,
    val rentalTotalIncludedKm: Double = 10.0,
    val rentalExtraDistanceKm: Double = 0.0,
    val rentalHourlyCharge: Double = 0.0,
    val rentalExtraDistanceCharge: Double = 0.0,
    // Outstation
    val outstationRatePerKm: Double = 28.0,
    val outstationMinFare: Double = 0.0,
    val outstationWaitRate: Double = 0.0,
    val outstationDriverBata: Double = 0.0,
    val outstationAdditionalCharges: Double = 0.0,
    val extraChargeItems: List<ExtraChargeItem> = emptyList(),
    val liveExtraChargesTotal: Double = 0.0,
    val routePoints: List<Pair<Double, Double>> = emptyList(),
    // Exclusive GET TAXI Vendor Identity (v1)
    val vendorId: String = SangamTariffConfig.VENDOR_ID,
    val vendorName: String = SangamTariffConfig.VENDOR_NAME,
    val vendorLogoAsset: String = "drawable/get_taxi_header_logo",
    val vendorContactNumber: String = SangamTariffConfig.VENDOR_GPAY_NUMBER,
    val vendorBusinessAddress: String = "No. 286, Diwan Bahadur Road, R.S. Puram, Coimbatore",
    val vendorEmail: String = "",
    val vendorGstin: String = "",
    val vendorInvoiceDetails: String = ""
)

enum class TripStatus {
    IDLE,
    RUNNING,
    PAUSED,
    FINISHED
}

enum class ActiveTripDisplayMode {
    MAP_FIRST,
    METER_DETAILS
}
