package com.example.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.DriverProfileEntity
import com.example.data.database.TripDatabase
import com.example.data.database.TripEntity
import com.example.data.database.VendorProfileEntity
import com.example.data.model.ActiveTripDisplayMode
import com.example.data.model.ExtraChargeItem
import com.example.data.model.ServiceType
import com.example.data.model.TripState
import com.example.data.model.TripStatus
import com.example.data.preferences.OtpValidationResult
import com.example.data.preferences.SettingsRepository
import com.example.data.preferences.TripOtpManager
import com.example.data.repository.TripRepository
import com.example.service.LocationTrackingService
import com.example.util.FareCalculator
import com.example.util.SangamTariffCalculator
import com.example.util.SangamTariffConfig
import com.example.util.SangamTariffConfig.SangamTripType
import com.example.util.SangamTariffConfig.VehicleCategory
import com.example.util.VendorLogoRenderer
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MeterViewModel(application: Application) : AndroidViewModel(application) {

    private val tripRepository: TripRepository
    private val settingsRepository: SettingsRepository
    private val tripOtpManager: TripOtpManager

    val tripState: StateFlow<TripState> = LocationTrackingService.tripState

    val allTrips: StateFlow<List<TripEntity>>
    val allVendors: StateFlow<List<VendorProfileEntity>>
    val activeVendors: StateFlow<List<VendorProfileEntity>>

    private val _selectedServiceType = MutableStateFlow(ServiceType.LOCAL)
    val selectedServiceType: StateFlow<ServiceType> = _selectedServiceType.asStateFlow()

    // Sangam Vehicle & Tariff Selection
    private val _selectedVehicleCategory = MutableStateFlow(VehicleCategory.MINI)
    val selectedVehicleCategory: StateFlow<VehicleCategory> = _selectedVehicleCategory.asStateFlow()

    private val _selectedSangamTripType = MutableStateFlow(SangamTripType.LOCAL_NORMAL)
    val selectedSangamTripType: StateFlow<SangamTripType> = _selectedSangamTripType.asStateFlow()

    private val _isInnovaCrysta = MutableStateFlow(false)
    val isInnovaCrysta: StateFlow<Boolean> = _isInnovaCrysta.asStateFlow()

    private val _isOutsideCity = MutableStateFlow(false)
    val isOutsideCity: StateFlow<Boolean> = _isOutsideCity.asStateFlow()

    private val _isNightTrip = MutableStateFlow(false)
    val isNightTrip: StateFlow<Boolean> = _isNightTrip.asStateFlow()

    private val _packageHours = MutableStateFlow(1)
    val packageHours: StateFlow<Int> = _packageHours.asStateFlow()

    private val _over12Hours = MutableStateFlow(false)
    val over12Hours: StateFlow<Boolean> = _over12Hours.asStateFlow()

    private val _additionalWaitingHours = MutableStateFlow(0.0)
    val additionalWaitingHours: StateFlow<Double> = _additionalWaitingHours.asStateFlow()

    val outstationWaitRate: StateFlow<Double>

    // Exclusive GET TAXI vendor (no multi-vendor switching in v1)
    private val _selectedVendor = MutableStateFlow(VendorLogoRenderer.defaultGetTaxiProfile())
    val selectedVendor: StateFlow<VendorProfileEntity> = _selectedVendor.asStateFlow()

    // Transient completed trip / quick calculation settlement (NEVER stored in permanent history)
    private val _manualSettlement = MutableStateFlow<LocationTrackingService.TransientCompletedTrip?>(null)
    val currentSettlement: StateFlow<LocationTrackingService.TransientCompletedTrip?> = combine(
        _manualSettlement,
        LocationTrackingService.lastCompletedTrip
    ) { manual, serviceTrip ->
        manual ?: serviceTrip
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val _activeTripDisplayMode = MutableStateFlow(ActiveTripDisplayMode.MAP_FIRST)
    val activeTripDisplayMode: StateFlow<ActiveTripDisplayMode> = _activeTripDisplayMode.asStateFlow()

    private val _hasActiveSessionBackup = MutableStateFlow(false)
    val hasActiveSessionBackup: StateFlow<Boolean> = _hasActiveSessionBackup.asStateFlow()

    private val _driverProfile = MutableStateFlow<DriverProfileEntity?>(null)
    val driverProfile: StateFlow<DriverProfileEntity?> = _driverProfile.asStateFlow()

    private val _tariffValidationMessage = MutableStateFlow<String?>(null)
    val tariffValidationMessage: StateFlow<String?> = _tariffValidationMessage.asStateFlow()

    init {
        val database = TripDatabase.getDatabase(application)
        tripRepository = TripRepository(database.tripDao())
        settingsRepository = SettingsRepository(application)
        tripOtpManager = TripOtpManager(application)

        outstationWaitRate = settingsRepository.outstationWaitRate
            .stateIn(viewModelScope, SharingStarted.Eagerly, SettingsRepository.DEFAULT_OUTSTATION_WAIT_RATE)

        allTrips = tripRepository.allTrips
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        allVendors = tripRepository.allVendors
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                listOf(VendorLogoRenderer.defaultGetTaxiProfile())
            )

        activeVendors = tripRepository.activeVendors
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                listOf(VendorLogoRenderer.defaultGetTaxiProfile())
            )

        checkForActiveBackup()
        loadDriverProfile()
    }

    fun selectVehicleCategory(category: VehicleCategory) {
        val status = tripState.value.status
        if (status == TripStatus.RUNNING || status == TripStatus.PAUSED) return
        _selectedVehicleCategory.value = category
        val supported = category.supportedTripTypes()
        if (_selectedSangamTripType.value !in supported) {
            setSangamTripType(supported.first())
        }
        if (category != VehicleCategory.INNOVA) {
            _isInnovaCrysta.value = false
        }
        clearValidationMessage()
    }

    fun setSangamTripType(tripType: SangamTripType) {
        val status = tripState.value.status
        if (status == TripStatus.RUNNING || status == TripStatus.PAUSED) return
        val category = _selectedVehicleCategory.value
        val validType = if (tripType in category.supportedTripTypes()) {
            tripType
        } else {
            category.supportedTripTypes().first()
        }
        _selectedSangamTripType.value = validType
        _selectedServiceType.value = when (validType) {
            SangamTripType.LOCAL_NORMAL, SangamTripType.LOCAL_MINIMUM_BILL -> ServiceType.LOCAL
            SangamTripType.HOURLY_RENTAL -> ServiceType.RENTAL
            SangamTripType.OUTSTATION_ONE_WAY, SangamTripType.OUTSTATION_ROUND_TRIP, SangamTripType.DAY_RENTAL -> ServiceType.OUTSTATION
        }
        clearValidationMessage()
    }

    fun setServiceType(type: ServiceType) {
        val status = tripState.value.status
        if (status == TripStatus.RUNNING || status == TripStatus.PAUSED) return
        _selectedServiceType.value = type
        val mapped = when (type) {
            ServiceType.LOCAL -> SangamTripType.LOCAL_NORMAL
            ServiceType.RENTAL -> SangamTripType.HOURLY_RENTAL
            ServiceType.OUTSTATION -> SangamTripType.OUTSTATION_ONE_WAY
        }
        if (mapped in _selectedVehicleCategory.value.supportedTripTypes()) {
            _selectedSangamTripType.value = mapped
        }
        clearValidationMessage()
    }

    fun setInnovaCrysta(enabled: Boolean) {
        if (tripState.value.status == TripStatus.RUNNING || tripState.value.status == TripStatus.PAUSED) return
        _isInnovaCrysta.value = enabled
    }

    fun setOutsideCity(enabled: Boolean) {
        _isOutsideCity.value = enabled
        val status = tripState.value.status
        if (status == TripStatus.RUNNING || status == TripStatus.PAUSED) {
            LocationTrackingService.updateActiveModifiers(isOutsideCity = enabled)
        }
    }

    fun setNightTrip(enabled: Boolean) {
        _isNightTrip.value = enabled
        val status = tripState.value.status
        if (status == TripStatus.RUNNING || status == TripStatus.PAUSED) {
            LocationTrackingService.updateActiveModifiers(isNightTrip = enabled)
        }
    }

    fun setPackageHours(hours: Int) {
        val clamped = hours.coerceAtLeast(1)
        _packageHours.value = clamped
        val status = tripState.value.status
        if (status == TripStatus.RUNNING || status == TripStatus.PAUSED) {
            LocationTrackingService.updateActiveModifiers(packageHours = clamped)
        }
    }

    fun setOver12Hours(enabled: Boolean) {
        _over12Hours.value = enabled
        val status = tripState.value.status
        if (status == TripStatus.RUNNING || status == TripStatus.PAUSED) {
            LocationTrackingService.updateActiveModifiers(over12Hours = enabled)
        }
    }

    fun setAdditionalWaitingHours(hours: Double) {
        val sanitized = hours.coerceAtLeast(0.0)
        _additionalWaitingHours.value = sanitized
        val status = tripState.value.status
        if (status == TripStatus.RUNNING || status == TripStatus.PAUSED) {
            LocationTrackingService.updateActiveModifiers(additionalWaitingHours = sanitized)
        }
    }

    fun showMeterDetails() {
        _activeTripDisplayMode.value = ActiveTripDisplayMode.METER_DETAILS
    }

    fun showMapDisplay() {
        _activeTripDisplayMode.value = ActiveTripDisplayMode.MAP_FIRST
    }

    fun clearValidationMessage() {
        _tariffValidationMessage.value = null
    }

    fun loadDriverProfile() {
        viewModelScope.launch {
            _driverProfile.value = tripRepository.getDriverProfile()
        }
    }

    fun saveDriverProfile(profile: DriverProfileEntity, onSaved: () -> Unit = {}) {
        viewModelScope.launch {
            tripRepository.saveDriverProfile(profile)
            _driverProfile.value = profile
            onSaved()
        }
    }

    fun checkForActiveBackup() {
        viewModelScope.launch {
            val backup = tripRepository.getActiveTrip()
            _hasActiveSessionBackup.value = backup != null
            if (backup != null) {
                _selectedServiceType.value = ServiceType.fromId(backup.serviceType)
            }
            Log.d("MeterViewModel", "Checked active session backup: ${backup != null}")
        }
    }

    /**
     * Direct Hire start: starts immediately without requiring a Trip OTP.
     */
    fun startTrip() {
        _activeTripDisplayMode.value = ActiveTripDisplayMode.MAP_FIRST
        _manualSettlement.value = null
        viewModelScope.launch {
            startTripInternal()
        }
    }

    /**
     * OTP-authorized start: validates the 4-digit OTP via TripOtpManager and starts the meter
     * only if validation succeeds.
     */
    fun startTripWithOtp(enteredOtp: String, onResult: (OtpValidationResult) -> Unit) {
        viewModelScope.launch {
            val result = tripOtpManager.validateAndAuthorizeTrip(enteredOtp)
            if (result is OtpValidationResult.Success) {
                _activeTripDisplayMode.value = ActiveTripDisplayMode.MAP_FIRST
                _manualSettlement.value = null
                startTripInternal()
            }
            onResult(result)
        }
    }

    private suspend fun startTripInternal() {
        val category = _selectedVehicleCategory.value
        val tripType = _selectedSangamTripType.value
        val isCrysta = _isInnovaCrysta.value && category == VehicleCategory.INNOVA
        val outside = _isOutsideCity.value
        val night = _isNightTrip.value
        val pkgHrs = _packageHours.value.coerceAtLeast(1)
        val over12 = _over12Hours.value
        val addWaitHrs = _additionalWaitingHours.value.coerceAtLeast(0.0)

        val currencyVal = settingsRepository.currency.first()
        val thresholdVal = settingsRepository.speedThreshold.first()
        val calFactor = settingsRepository.distanceCalibrationFactor.first()
        val outstationWaitRateVal = settingsRepository.outstationWaitRate.first()

        val localRate = SangamTariffConfig.localNormalRates[category]
        val outRate = SangamTariffConfig.outstationRates[category]
        val dayRate = if (isCrysta) SangamTariffConfig.innovaCrystaDayRentalRate else SangamTariffConfig.dayRentalRates[category]

        val baseVal = when (tripType) {
            SangamTripType.LOCAL_NORMAL, SangamTripType.LOCAL_MINIMUM_BILL -> localRate?.initialCharge?.toDouble() ?: 80.0
            SangamTripType.HOURLY_RENTAL -> SangamTariffConfig.hourlyBaseRatePerHour.toDouble() * pkgHrs
            SangamTripType.OUTSTATION_ONE_WAY, SangamTripType.OUTSTATION_ROUND_TRIP -> outRate?.initialCharge?.toDouble() ?: 300.0
            SangamTripType.DAY_RENTAL -> dayRate?.baseAmount?.toDouble() ?: 2000.0
        }
        val perKmVal = when (tripType) {
            SangamTripType.LOCAL_NORMAL, SangamTripType.LOCAL_MINIMUM_BILL -> localRate?.perKmRate?.toDouble() ?: 28.0
            SangamTripType.HOURLY_RENTAL -> SangamTariffConfig.hourlyExtraKmRate.toDouble()
            SangamTripType.OUTSTATION_ONE_WAY -> outRate?.oneWayPerKm?.toDouble() ?: 30.0
            SangamTripType.OUTSTATION_ROUND_TRIP -> outRate?.roundTripPerKm?.toDouble() ?: 15.0
            SangamTripType.DAY_RENTAL -> dayRate?.perKmRate?.toDouble() ?: 10.0
        }

        val mappedServiceType = when (tripType) {
            SangamTripType.LOCAL_NORMAL, SangamTripType.LOCAL_MINIMUM_BILL -> ServiceType.LOCAL.id
            SangamTripType.HOURLY_RENTAL -> ServiceType.RENTAL.id
            SangamTripType.OUTSTATION_ONE_WAY, SangamTripType.OUTSTATION_ROUND_TRIP, SangamTripType.DAY_RENTAL -> ServiceType.OUTSTATION.id
        }

        _tariffValidationMessage.value = null

        LocationTrackingService.startTrip(
            context = getApplication(),
            baseFare = baseVal,
            farePerKm = perKmVal,
            waitFarePerMin = 0.0,
            currency = currencyVal,
            speedThreshold = thresholdVal,
            serviceType = mappedServiceType,
            vehicleCategoryId = category.id,
            sangamTripTypeId = tripType.id,
            isInnovaCrysta = isCrysta,
            isOutOfCity = outside,
            isNightTrip = night,
            packageHours = pkgHrs,
            over12Hours = over12,
            rentalHourlyRate = SangamTariffConfig.hourlyBaseRatePerHour.toDouble(),
            rentalIncludedKmPerHour = SangamTariffConfig.hourlyIncludedKmPerHour.toDouble(),
            rentalMinHours = pkgHrs.toDouble(),
            rentalExtraHrRate = SangamTariffConfig.hourlyBaseRatePerHour.toDouble(),
            rentalExtraKmRate = SangamTariffConfig.hourlyExtraKmRate.toDouble(),
            outstationRatePerKm = perKmVal,
            outstationMinFare = 0.0,
            outstationWaitRate = outstationWaitRateVal,
            outstationDriverBata = if (over12 && (tripType == SangamTripType.OUTSTATION_ONE_WAY || tripType == SangamTripType.OUTSTATION_ROUND_TRIP)) {
                SangamTariffConfig.outstationOver12HoursDriverAllowance.toDouble()
            } else {
                0.0
            },
            outstationAdditionalCharges = 0.0,
            outstationAdditionalWaitingHours = addWaitHrs,
            calibrationFactor = calFactor,
            vendorId = SangamTariffConfig.VENDOR_ID,
            vendorName = SangamTariffConfig.VENDOR_NAME,
            vendorContactNumber = SangamTariffConfig.VENDOR_GPAY_NUMBER
        )
        _hasActiveSessionBackup.value = false
    }

    fun pauseTrip() {
        LocationTrackingService.pauseTrip(getApplication())
    }

    fun resumeTrip() {
        LocationTrackingService.resumeTrip(getApplication())
    }

    fun addExtraCharge(label: String, amount: Double) {
        LocationTrackingService.addExtraCharge(getApplication(), label, amount)
    }

    fun removeExtraCharge(chargeId: String) {
        LocationTrackingService.removeExtraCharge(getApplication(), chargeId)
    }

    fun clearExtraCharges() {
        LocationTrackingService.clearExtraCharges(getApplication())
    }

    /**
     * Completes an active GPS trip transiently (NO permanent history saved) and transitions to Settlement.
     */
    fun completeTripForSettlement(onReady: () -> Unit) {
        val currentState = tripState.value
        val fallbackInput = LocationTrackingService.buildSangamInput(currentState)
        val fallbackBreakdown = SangamTariffCalculator.calculateTripFareAndSettlement(fallbackInput)

        LocationTrackingService.stopTrip(getApplication())
        _activeTripDisplayMode.value = ActiveTripDisplayMode.MAP_FIRST

        viewModelScope.launch {
            _hasActiveSessionBackup.value = false
            // Wait briefly for service coroutine to publish transient trip
            repeat(8) {
                if (LocationTrackingService.lastCompletedTrip.value != null) {
                    onReady()
                    return@launch
                }
                delay(40)
            }
            // Fallback transient build if service didn't complete in time
            if (LocationTrackingService.lastCompletedTrip.value == null) {
                buildTransientSettlement(
                    input = fallbackInput,
                    breakdown = fallbackBreakdown,
                    distanceKm = currentState.distanceKm,
                    durationSeconds = currentState.durationSeconds,
                    waitingSeconds = currentState.waitingSeconds,
                    pickup = currentState.pickupAddress.ifBlank { "GPS Location" },
                    drop = currentState.dropAddress.ifBlank { "GPS Location" },
                    extraItems = currentState.extraChargeItems
                )
            }
            onReady()
        }
    }

    /**
     * Calculates an instant offline Sangam fare & settlement for parked or manual billing
     * without saving permanent trip history.
     */
    fun calculateQuickSettlement(
        input: SangamTariffCalculator.TripCalculationInput,
        pickupAddress: String = "Local Trip",
        dropAddress: String = "Drop Point",
        durationMinutes: Long = 0L,
        onReady: () -> Unit
    ) {
        viewModelScope.launch {
            val breakdown = SangamTariffCalculator.calculateTripFareAndSettlement(input)
            val extras = mutableListOf<ExtraChargeItem>()
            if (input.isOutsideCity && (input.tripType == SangamTripType.LOCAL_NORMAL || input.tripType == SangamTripType.LOCAL_MINIMUM_BILL)) {
                extras.add(ExtraChargeItem(label = "Outside City Limits", amount = SangamTariffConfig.localOutsideCityCharge.toDouble()))
            }
            if (input.isNightTrip && (input.tripType == SangamTripType.LOCAL_NORMAL || input.tripType == SangamTripType.LOCAL_MINIMUM_BILL)) {
                extras.add(ExtraChargeItem(label = "Night Charge (11 PM–6 AM)", amount = SangamTariffConfig.localNightCharge.toDouble()))
            }
            if (input.tollCharge > 0.0) extras.add(ExtraChargeItem(label = "Toll", amount = input.tollCharge))
            if (input.permitCharge > 0.0) extras.add(ExtraChargeItem(label = "Permit", amount = input.permitCharge))
            if (input.parkingCharge > 0.0) extras.add(ExtraChargeItem(label = "Parking", amount = input.parkingCharge))
            if (input.customExtrasTotal > 0.0) extras.add(ExtraChargeItem(label = "Extra Charges", amount = input.customExtrasTotal))

            val durationSec = when {
                input.durationSeconds > 0L -> input.durationSeconds
                durationMinutes > 0L -> durationMinutes * 60L
                input.tripType == SangamTripType.HOURLY_RENTAL -> input.packageHours.coerceAtLeast(1) * 3600L
                else -> 0L
            }

            buildTransientSettlement(
                input = input,
                breakdown = breakdown,
                distanceKm = input.distanceKm,
                durationSeconds = durationSec,
                waitingSeconds = (input.waitingMinutes * 60.0).toLong(),
                pickup = pickupAddress.ifBlank { "Parked Fare Entry" },
                drop = dropAddress.ifBlank { "Destination" },
                extraItems = extras
            )
            onReady()
        }
    }

    private suspend fun buildTransientSettlement(
        input: SangamTariffCalculator.TripCalculationInput,
        breakdown: SangamTariffCalculator.SangamSettlementBreakdown,
        distanceKm: Double,
        durationSeconds: Long,
        waitingSeconds: Long,
        pickup: String,
        drop: String,
        extraItems: List<ExtraChargeItem>
    ) {
        val now = System.currentTimeMillis()
        val profile = tripRepository.getDriverProfile()
        val tripNumber = LocationTrackingService.generateTripNumber(now)
        val dateText = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(now))
        val timeText = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(now))

        val mappedMode = when (input.tripType) {
            SangamTripType.LOCAL_NORMAL, SangamTripType.LOCAL_MINIMUM_BILL -> ServiceType.LOCAL.id
            SangamTripType.HOURLY_RENTAL -> ServiceType.RENTAL.id
            SangamTripType.OUTSTATION_ONE_WAY, SangamTripType.OUTSTATION_ROUND_TRIP, SangamTripType.DAY_RENTAL -> ServiceType.OUTSTATION.id
        }

        val vehicleDisplay = if (breakdown.isInnovaCrysta) "INNOVA CRYSTA" else breakdown.vehicleCategory.displayName
        val extrasTotal = ExtraChargeItem.totalAmount(extraItems)

        val entity = TripEntity(
            id = 0,
            tripNumber = tripNumber,
            startTime = now - (durationSeconds * 1000L),
            endTime = now,
            distanceKm = distanceKm,
            durationSeconds = durationSeconds,
            waitingSeconds = waitingSeconds,
            totalFare = breakdown.customerBillTotal.toDouble(),
            startLatitude = 0.0,
            startLongitude = 0.0,
            endLatitude = 0.0,
            endLongitude = 0.0,
            driverName = profile?.driverName ?: "",
            driverMobile = profile?.driverMobile ?: "",
            vehicleNumber = profile?.vehicleNumber ?: "",
            vehicleModel = profile?.vehicleModel ?: "",
            vehicleType = vehicleDisplay,
            pickupAddress = pickup,
            dropAddress = drop,
            isOutOfCity = input.isOutsideCity,
            outOfCitySurcharge = if (input.isOutsideCity) SangamTariffConfig.localOutsideCityCharge.toDouble() else 0.0,
            baseFare = breakdown.lineItems.firstOrNull()?.amount?.toDouble() ?: 0.0,
            farePerKm = 0.0,
            waitFarePerMin = 0.0,
            currency = SangamTariffConfig.CURRENCY_SYMBOL,
            serviceType = mappedMode,
            rentalBaseHours = input.packageHours.toDouble(),
            rentalBaseKm = SangamTariffConfig.hourlyIncludedKmPerHour.toDouble(),
            rentalBaseFare = SangamTariffConfig.hourlyBaseRatePerHour.toDouble(),
            rentalExtraKmRate = SangamTariffConfig.hourlyExtraKmRate.toDouble(),
            billableHours = if (input.tripType == SangamTripType.HOURLY_RENTAL) {
                maxOf(input.packageHours.coerceAtLeast(1), FareCalculator.calculateBillableHours(durationSeconds).toInt()).toLong()
            } else {
                input.packageHours.toLong()
            },
            outstationWaitRate = input.additionalWaitingHourlyRate,
            outstationDriverBata = if (input.over12Hours) SangamTariffConfig.outstationOver12HoursDriverAllowance.toDouble() else 0.0,
            outstationAdditionalCharges = extrasTotal,
            extraChargesJson = ExtraChargeItem.serialize(extraItems),
            invoiceTimestamp = now,
            invoiceDateText = dateText,
            invoiceTimeText = timeText,
            vendorId = SangamTariffConfig.VENDOR_ID,
            vendorName = SangamTariffConfig.VENDOR_NAME,
            vendorLogoAsset = "drawable/get_taxi_header_logo",
            vendorContactNumber = SangamTariffConfig.VENDOR_GPAY_NUMBER,
            vendorBusinessAddress = "No. 286, Diwan Bahadur Road, R.S. Puram, Coimbatore",
            vendorInvoiceDetails = "GPay: ${SangamTariffConfig.VENDOR_GPAY_NUMBER}",
            vendorInvoiceNumber = tripNumber
        )

        _manualSettlement.value = LocationTrackingService.TransientCompletedTrip(
            tripEntity = entity,
            calculationInput = input,
            breakdown = breakdown
        )
    }

    fun updateSettlementCustomerInfo(name: String, mobile: String, address: String) {
        val current = currentSettlement.value ?: return
        val updatedEntity = current.tripEntity.copy(
            customerName = name.trim(),
            customerMobile = mobile.trim(),
            customerAddress = address.trim()
        )
        _manualSettlement.value = current.copy(tripEntity = updatedEntity)
    }

    /**
     * Clears transient settlement and resets meter/modifiers for the next trip.
     * Zero permanent trip history is retained.
     */
    fun endTripAndClearSettlement() {
        _manualSettlement.value = null
        LocationTrackingService.clearLastCompletedTrip()
        _isOutsideCity.value = false
        _isNightTrip.value = false
        _over12Hours.value = false
        _packageHours.value = 1
        _additionalWaitingHours.value = 0.0
        _activeTripDisplayMode.value = ActiveTripDisplayMode.MAP_FIRST
        viewModelScope.launch {
            tripRepository.clearActiveTrip()
            tripOtpManager.destroyTripAccessSession()
            _hasActiveSessionBackup.value = false
        }
    }

    fun recoverTrip() {
        _activeTripDisplayMode.value = ActiveTripDisplayMode.MAP_FIRST
        LocationTrackingService.recoveryTrip(getApplication())
        _hasActiveSessionBackup.value = false
    }

    fun discardBackup() {
        viewModelScope.launch {
            tripRepository.clearActiveTrip()
            _hasActiveSessionBackup.value = false
        }
    }
}
