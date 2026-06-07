package com.example.ui

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File

class EcoScanViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = EcoScanRepository(application)

    // --- SESSION CONTEXT ---
    private val _currentUserEmail = MutableStateFlow("citizen@ecoscan.ai")
    val currentUserEmail: StateFlow<String> = _currentUserEmail.asStateFlow()

    val currentUser: StateFlow<UserEntity?> = _currentUserEmail
        .flatMapLatest { email -> repository.getUser(email) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // --- REPOSITORY FLOWS ---
    val allReports: StateFlow<List<ReportEntity>> = repository.allReports
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val leaderboard: StateFlow<List<UserEntity>> = repository.leaderboard
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val challenges: StateFlow<List<ChallengeEntity>> = repository.allChallenges
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val events: StateFlow<List<EventEntity>> = repository.allEvents
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- FILTER STATES ---
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedTypeFilter = MutableStateFlow<String?>(null)
    val selectedTypeFilter: StateFlow<String?> = _selectedTypeFilter.asStateFlow()

    private val _selectedSeverityFilter = MutableStateFlow<String?>(null)
    val selectedSeverityFilter: StateFlow<String?> = _selectedSeverityFilter.asStateFlow()

    // Filtered reports computed reactively
    val filteredReports: StateFlow<List<ReportEntity>> = combine(
        allReports, _searchQuery, _selectedTypeFilter, _selectedSeverityFilter
    ) { list, search, type, severity ->
        list.filter { report ->
            val matchesSearch = search.isEmpty() ||
                    report.address.contains(search, ignoreCase = true) ||
                    report.description.contains(search, ignoreCase = true)

            val matchesType = type == null || report.wasteType.equals(type, ignoreCase = true)
            val matchesSeverity = severity == null || report.severityLevel.equals(severity, ignoreCase = true)

            matchesSearch && matchesType && matchesSeverity
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- SCANNER / AI DETECTOR STATES ---
    private val _scannedBitmap = MutableStateFlow<Bitmap?>(null)
    val scannedBitmap: StateFlow<Bitmap?> = _scannedBitmap.asStateFlow()

    private val _scannedImageName = MutableStateFlow<String?>(null)
    val scannedImageName: StateFlow<String?> = _scannedImageName.asStateFlow()

    private val _isAnalyzing = MutableStateFlow(false)
    val isAnalyzing: StateFlow<Boolean> = _isAnalyzing.asStateFlow()

    private val _analysisResult = MutableStateFlow<WasteAnalysisResult?>(null)
    val analysisResult: StateFlow<WasteAnalysisResult?> = _analysisResult.asStateFlow()

    private val _analysisError = MutableStateFlow<String?>(null)
    val analysisError: StateFlow<String?> = _analysisError.asStateFlow()

    // Fine/manual coordinates for scanner submit
    private val _gpsLatitude = MutableStateFlow(40.7128) // Default NY center
    val gpsLatitude: StateFlow<Double> = _gpsLatitude.asStateFlow()

    private val _gpsLongitude = MutableStateFlow(-74.0060)
    val gpsLongitude: StateFlow<Double> = _gpsLongitude.asStateFlow()

    private val _detectedAddress = MutableStateFlow("New York City Hall Park Trail, NY 10007")
    val detectedAddress: StateFlow<String> = _detectedAddress.asStateFlow()

    private val _submitDescription = MutableStateFlow("")
    val submitDescription: StateFlow<String> = _submitDescription.asStateFlow()

    private val _submitStatus = MutableStateFlow<String>("Idle") // "Idle", "Submitting", "Success", "Error"
    val submitStatus: StateFlow<String> = _submitStatus.asStateFlow()

    // --- BEFORE & AFTER CLEANUP STATE ---
    private val _cleanupTargetReportId = MutableStateFlow<Long?>(null)
    val cleanupTargetReportId: StateFlow<Long?> = _cleanupTargetReportId.asStateFlow()

    private val _cleanupAfterBitmap = MutableStateFlow<Bitmap?>(null)
    val cleanupAfterBitmap: StateFlow<Bitmap?> = _cleanupAfterBitmap.asStateFlow()

    private val _isSubmittingCleanup = MutableStateFlow(false)
    val isSubmittingCleanup: StateFlow<Boolean> = _isSubmittingCleanup.asStateFlow()

    init {
        // Pre-populate mock database with seed files if launching for the first time
        viewModelScope.launch {
            repository.preseedDatabaseIfEmpty()
        }
    }

    // --- ACTIONS ---

    // Toggle user session
    fun switchUser(email: String) {
        viewModelScope.launch {
            if (repository.getUserSync(email) == null) {
                // Register temporary new citizen if not registered in Database
                val parts = email.split("@")
                val seedName = parts.getOrNull(0)?.replaceFirstChar { it.uppercase() } ?: "New Citizen"
                repository.insertUser(UserEntity(
                    userId = email,
                    username = seedName,
                    email = email,
                    passwordHash = if (email == "admin@ecoscan.ai") "admin" else "password",
                    avatarUrl = "avatar_${(2..5).random()}",
                    rewardPoints = 0,
                    isAdmin = email.contains("admin")
                ))
            }
            _currentUserEmail.value = email
        }
    }

    fun signIn(email: String, password: String, onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        viewModelScope.launch {
            if (email.isBlank() || password.isBlank()) {
                onFailure("Please enter both email and password")
                return@launch
            }
            val user = repository.getUserSync(email)
            if (user != null) {
                if (user.passwordHash == password) {
                    _currentUserEmail.value = email
                    onSuccess()
                } else {
                    onFailure("Incorrect password. Please try again.")
                }
            } else {
                // Auto create seed credentials for citizen and admin if database is queryable but empty
                if (email == "citizen@ecoscan.ai" && password == "password") {
                    switchUser(email)
                    onSuccess()
                } else if (email == "admin@ecoscan.ai" && password == "admin") {
                    switchUser(email)
                    onSuccess()
                } else {
                    onFailure("Account not found. Please sign up above!")
                }
            }
        }
    }

    fun signUp(username: String, email: String, passwordHash: String, isAdmin: Boolean, onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        viewModelScope.launch {
            if (username.isBlank() || email.isBlank() || passwordHash.isBlank()) {
                onFailure("Please fill out all fields.")
                return@launch
            }
            if (!email.contains("@")) {
                onFailure("Please enter a valid email address.")
                return@launch
            }
            try {
                val existing = repository.getUserSync(email)
                if (existing != null) {
                    onFailure("Account with this email already exists.")
                    return@launch
                }
                val newUser = UserEntity(
                    userId = email,
                    username = username,
                    email = email,
                    passwordHash = passwordHash,
                    avatarUrl = "avatar_${(1..5).random()}",
                    rewardPoints = 150, // Welcome points boost!
                    reportsSubmittedCount = 0,
                    cleanupsCompletedCount = 0,
                    isAdmin = isAdmin,
                    currentStreak = 1
                )
                repository.insertUser(newUser)
                _currentUserEmail.value = email
                onSuccess()
            } catch (e: Exception) {
                onFailure(e.message ?: "Sign up failed.")
            }
        }
    }

    fun logout() {
        _currentUserEmail.value = ""
    }

    // Update filters
    fun updateQuery(query: String) {
        _searchQuery.value = query
    }

    fun updateTypeFilter(type: String?) {
        _selectedTypeFilter.value = type
    }

    fun updateSeverityFilter(severity: String?) {
        _selectedSeverityFilter.value = severity
    }

    // Capture bitmap
    fun setScannedImage(bitmap: Bitmap, label: String? = null) {
        _scannedBitmap.value = bitmap
        _scannedImageName.value = label
        _analysisResult.value = null
        _analysisError.value = null
        _submitStatus.value = "Idle"
    }

    // Clear Scanner setup
    fun clearScanner() {
        _scannedBitmap.value = null
        _scannedImageName.value = null
        _analysisResult.value = null
        _analysisError.value = null
        _submitDescription.value = ""
        _submitStatus.value = "Idle"
    }

    // Run actual Gemini core trigger
    fun requestAiWasteAnalysis() {
        val bitmap = _scannedBitmap.value ?: return
        _isAnalyzing.value = true
        _analysisResult.value = null
        _analysisError.value = null

        viewModelScope.launch {
            try {
                val resultsObj = repository.analyzeWasteImage(bitmap, _scannedImageName.value)
                _analysisResult.value = resultsObj
            } catch (e: Exception) {
                _analysisError.value = e.localizedMessage ?: "Failed analyzing eco scans."
            } finally {
                _isAnalyzing.value = false
            }
        }
    }

    // Update GPS coordinates based on manual movements or randomized NY grid sweeps
    fun setLocation(lat: Double, lng: Double, addressName: String) {
        _gpsLatitude.value = lat
        _gpsLongitude.value = lng
        _detectedAddress.value = addressName
    }

    fun captureRealGPSLocation(context: android.content.Context) {
        if (androidx.core.content.PermissionChecker.checkSelfPermission(
                context,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) != androidx.core.content.PermissionChecker.PERMISSION_GRANTED
        ) {
            android.util.Log.w("EcoScanRealGPS", "GPS Permission not granted, falling back to drift sandbox.")
            randomizeLocation()
            return
        }

        try {
            val fusedLocationClient = com.google.android.gms.location.LocationServices.getFusedLocationProviderClient(context)
            fusedLocationClient.lastLocation.addOnSuccessListener { location: android.location.Location? ->
                if (location != null) {
                    viewModelScope.launch {
                        val lat = location.latitude
                        val lng = location.longitude
                        
                        val addressName = try {
                            val geocoder = android.location.Geocoder(context, java.util.Locale.getDefault())
                            val addresses = geocoder.getFromLocation(lat, lng, 1)
                            if (addresses != null && addresses.isNotEmpty()) {
                                addresses[0].getAddressLine(0) ?: "Lat: $lat, Lng: $lng"
                            } else {
                                "Lat: $lat, Lng: $lng"
                            }
                        } catch (e: Exception) {
                            "Lat: $lat, Lng: $lng"
                        }
                        setLocation(lat, lng, addressName)
                    }
                } else {
                    android.util.Log.w("EcoScanRealGPS", "Real location returned null. Generating randomized zone coordinates.")
                    randomizeLocation()
                }
            }.addOnFailureListener {
                android.util.Log.e("EcoScanRealGPS", "Failed securing physical device location: ${it.message}")
                randomizeLocation()
            }
        } catch (e: Exception) {
            android.util.Log.e("EcoScanRealGPS", "Error polling location client: ${e.message}")
            randomizeLocation()
        }
    }

    fun randomizeLocation() {
        // Random drift near New York Central coordinates
        val dLat = ((-500..500).random()) / 100000.0
        val dLng = ((-500..500).random()) / 100000.0
        val finalLat = 40.7128 + dLat
        val finalLng = -74.0060 + dLng

        // Simple mock reverse address resolver based on quadrants
        val adText = when {
            dLat > 0 && dLng > 0 -> "Amster Avenue Corner, Upper Manhattan Circle, NY 10025"
            dLat > 0 && dLng <= 0 -> "Central Park Nature Trail Wood, NY 10019"
            dLat <= 0 && dLng > 0 -> "FDR Highway East river Bankside, NY 10002"
            else -> "Cortlandt St Subway Exit, Financial Alleyway, NY 10007"
        }
        setLocation(finalLat, finalLng, adText)
    }

    // Edit fields
    fun setSubmitDescription(desc: String) {
        _submitDescription.value = desc
    }

    // Perform final Report submit
    fun submitReport() {
        val bitmap = _scannedBitmap.value ?: return
        val analysis = _analysisResult.value ?: return
        _submitStatus.value = "Submitting"

        viewModelScope.launch {
            try {
                // Pin image to local private internal space
                val localPath = repository.saveImageToInternalStorage(bitmap)

                val user = currentUser.value
                val newReport = ReportEntity(
                    imageUrl = localPath,
                    description = _submitDescription.value.ifBlank { "Scan reported via EcoScan AI: ${analysis.wasteType} accumulation." },
                    status = "Submitted",
                    latitude = _gpsLatitude.value,
                    longitude = _gpsLongitude.value,
                    address = _detectedAddress.value,
                    wasteType = analysis.wasteType,
                    wasteConfidence = analysis.confidenceScore,
                    severityLevel = analysis.severityLevel,
                    cleanlinessScore = analysis.cleanlinessScore,
                    environmentalImpact = analysis.environmentalImpact,
                    reporterName = user?.username ?: "Citizen_Scanner"
                )

                repository.submitReport(newReport, _currentUserEmail.value)
                _submitStatus.value = "Success"
            } catch (e: Exception) {
                _submitStatus.value = "Error"
            }
        }
    }

    // --- BEFORE & AFTER CLEANUP ---

    fun setCleanupTarget(reportId: Long) {
        _cleanupTargetReportId.value = reportId
        _cleanupAfterBitmap.value = null
        _isSubmittingCleanup.value = false
    }

    fun clearCleanupTarget() {
        _cleanupTargetReportId.value = null
        _cleanupAfterBitmap.value = null
    }

    fun setCleanupAfterImage(bitmap: Bitmap) {
        _cleanupAfterBitmap.value = bitmap
    }

    fun submitCleanupResult() {
        val reportId = _cleanupTargetReportId.value ?: return
        val bitmap = _cleanupAfterBitmap.value ?: return
        _isSubmittingCleanup.value = true

        viewModelScope.launch {
            try {
                val localPath = repository.saveImageToInternalStorage(bitmap)
                // Calculate simulated improvement percent
                val improvement = (80..99).random().toDouble()

                repository.completeCleanup(reportId, localPath, _currentUserEmail.value, improvement)
                clearCleanupTarget()
            } catch (e: Exception) {
                // Silently log or retry
            } finally {
                _isSubmittingCleanup.value = false
            }
        }
    }

    // --- ADMIN CONTROLS ---

    fun adminVerifyReport(reportId: Long, isVerified: Boolean, notes: String) {
        viewModelScope.launch {
            repository.verifyReport(reportId, isVerified, notes, _currentUserEmail.value)
        }
    }

    fun adminDeleteReport(reportId: Long) {
        viewModelScope.launch {
            repository.deleteReport(reportId)
        }
    }

    // --- EVENTS ENGAGEMENT ---
    fun volunteerToEvent(eventId: String) {
        viewModelScope.launch {
            repository.joinEvent(eventId, _currentUserEmail.value)
        }
    }
}
