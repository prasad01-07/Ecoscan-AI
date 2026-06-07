package com.example.ui

import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Paint as AndroidPaint
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.*
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*
import android.net.Uri
import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import java.io.File
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.isGranted

enum class EcoTab {
    DASHBOARD, SCANNER, MAP, REPORTS, ALLIANCE
}

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.animation.ExperimentalAnimationApi::class)
@Composable
fun EcoScanApp() {
    val model: EcoScanViewModel = viewModel()
    
    val user by model.currentUser.collectAsState()
    val allReports by model.allReports.collectAsState()
    val filteredReports by model.filteredReports.collectAsState()
    val leaderboard by model.leaderboard.collectAsState()
    val challenges by model.challenges.collectAsState()
    val events by model.events.collectAsState()

    val currentTab = remember { mutableStateOf(EcoTab.DASHBOARD) }
    var showUserMenu by remember { mutableStateOf(false) }

    val currentEmail by model.currentUserEmail.collectAsState()
    if (currentEmail.isEmpty()) {
        AuthBarrierScreen(model)
        return
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Eco,
                            contentDescription = null,
                            tint = EmeraldAccent,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "EcoScan AI",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontFamily = FontFamily.SansSerif,
                            letterSpacing = 0.5.sp
                        )
                    }
                },
                actions = {
                    // Quick profile switch shortcut
                    Box(modifier = Modifier.padding(end = 8.dp)) {
                        IconButton(
                            onClick = { showUserMenu = !showUserMenu }
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .background(
                                        if (user?.isAdmin == true) Color(0xFFE53935) else EmeraldAccent,
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = user?.username?.take(2)?.uppercase() ?: "US",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = showUserMenu,
                            onDismissRequest = { showUserMenu = false },
                            modifier = Modifier.background(SpruceCardSurface)
                        ) {
                            DropdownMenuItem(
                                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = LightSageText) },
                                text = { Text("${user?.username} (${if (user?.isAdmin == true) "Admin" else "Citizen"})", color = Color.White) },
                                onClick = {}
                            )
                            Divider(color = BarkNeutralGrey.copy(alpha = 0.3f))
                            DropdownMenuItem(
                                leadingIcon = { Icon(Icons.Default.SwitchAccount, contentDescription = null, tint = EmeraldAccent) },
                                text = { Text("Switch to User Profile", color = LightSageText) },
                                onClick = {
                                    model.switchUser("citizen@ecoscan.ai")
                                    showUserMenu = false
                                }
                            )
                            DropdownMenuItem(
                                leadingIcon = { Icon(Icons.Default.Security, contentDescription = null, tint = Color.Red) },
                                text = { Text("Switch to Admin Profile", color = LightSageText) },
                                onClick = {
                                    model.switchUser("admin@ecoscan.ai")
                                    showUserMenu = false
                                }
                            )
                            Divider(color = BarkNeutralGrey.copy(alpha = 0.3f))
                            DropdownMenuItem(
                                leadingIcon = { Icon(Icons.Default.ExitToApp, contentDescription = null, tint = Color.Yellow) },
                                text = { Text("Log Out & Lock", color = Color.White) },
                                onClick = {
                                    model.logout()
                                    showUserMenu = false
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = DeepForestBackground
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = SpruceCardSurface,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = currentTab.value == EcoTab.DASHBOARD,
                    onClick = { currentTab.value = EcoTab.DASHBOARD },
                    icon = { Icon(Icons.Default.Dashboard, contentDescription = "Dashboard") },
                    label = { Text("Overview", fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.White,
                        selectedTextColor = Color.White,
                        unselectedIconColor = LightSageText.copy(alpha = 0.6f),
                        unselectedTextColor = LightSageText.copy(alpha = 0.6f),
                        indicatorColor = LeafPrimaryGreen
                    )
                )
                NavigationBarItem(
                    selected = currentTab.value == EcoTab.SCANNER,
                    onClick = { currentTab.value = EcoTab.SCANNER },
                    icon = { Icon(Icons.Default.CameraAlt, contentDescription = "Scan AI") },
                    label = { Text("Scan AI", fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.White,
                        selectedTextColor = Color.White,
                        unselectedIconColor = LightSageText.copy(alpha = 0.6f),
                        unselectedTextColor = LightSageText.copy(alpha = 0.6f),
                        indicatorColor = LeafPrimaryGreen
                    )
                )
                NavigationBarItem(
                    selected = currentTab.value == EcoTab.MAP,
                    onClick = { currentTab.value = EcoTab.MAP },
                    icon = { Icon(Icons.Default.Map, contentDescription = "Eco Map") },
                    label = { Text("Eco-Map", fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.White,
                        selectedTextColor = Color.White,
                        unselectedIconColor = LightSageText.copy(alpha = 0.6f),
                        unselectedTextColor = LightSageText.copy(alpha = 0.6f),
                        indicatorColor = LeafPrimaryGreen
                    )
                )
                NavigationBarItem(
                    selected = currentTab.value == EcoTab.REPORTS,
                    onClick = { currentTab.value = EcoTab.REPORTS },
                    icon = { Icon(Icons.Default.Assignment, contentDescription = "Reports") },
                    label = { Text("Feeds", fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.White,
                        selectedTextColor = Color.White,
                        unselectedIconColor = LightSageText.copy(alpha = 0.6f),
                        unselectedTextColor = LightSageText.copy(alpha = 0.6f),
                        indicatorColor = LeafPrimaryGreen
                    )
                )
                NavigationBarItem(
                    selected = currentTab.value == EcoTab.ALLIANCE,
                    onClick = { currentTab.value = EcoTab.ALLIANCE },
                    icon = { Icon(Icons.Default.Stars, contentDescription = "Alliance") },
                    label = { Text("Challenges", fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.White,
                        selectedTextColor = Color.White,
                        unselectedIconColor = LightSageText.copy(alpha = 0.6f),
                        unselectedTextColor = LightSageText.copy(alpha = 0.6f),
                        indicatorColor = LeafPrimaryGreen
                    )
                )
            }
        },
        containerColor = DeepForestBackground
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            AnimatedContent(
                targetState = currentTab.value,
                transitionSpec = {
                    fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(160))
                },
                label = "tabs"
            ) { tab ->
                when (tab) {
                    EcoTab.DASHBOARD -> DashboardTab(model, user, allReports, onGoToScan = { currentTab.value = EcoTab.SCANNER })
                    EcoTab.SCANNER -> ScannerTab(model)
                    EcoTab.MAP -> MapTab(model, filteredReports)
                    EcoTab.REPORTS -> ReportsFeedTab(model, user, filteredReports)
                    EcoTab.ALLIANCE -> AllianceTab(model, user, challenges, events, leaderboard)
                }
            }
        }
    }
}

@Composable
fun AuthBarrierScreen(model: EcoScanViewModel) {
    var isSignUp by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isAdmin by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepForestBackground)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 420.dp)
                .verticalScroll(rememberScrollState()),
            colors = CardDefaults.cardColors(containerColor = SpruceCardSurface),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, EmeraldAccent.copy(alpha = 0.3f))
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Logo Icon
                Icon(
                    imageVector = Icons.Default.Eco,
                    contentDescription = null,
                    tint = EmeraldAccent,
                    modifier = Modifier.size(54.dp)
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "EcoScan AI",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "Smart Waste Detection & Reporting",
                    fontSize = 11.sp,
                    color = LightSageText,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 2.dp, bottom = 20.dp)
                )

                // Tab selectors
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                        .background(BarkNeutralGrey.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                        .padding(4.dp)
                ) {
                    Button(
                        onClick = { 
                            isSignUp = false
                            errorMessage = null 
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (!isSignUp) EmeraldAccent else Color.Transparent
                        ),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(vertical = 8.dp),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text("Sign In", color = if (!isSignUp) Color.Black else Color.White, fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = { 
                            isSignUp = true
                            errorMessage = null 
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSignUp) EmeraldAccent else Color.Transparent
                        ),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(vertical = 8.dp),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text("Register", color = if (isSignUp) Color.Black else Color.White, fontWeight = FontWeight.Bold)
                    }
                }

                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        color = Color.Red,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                }

                // Username input (Sign up only)
                if (isSignUp) {
                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text("Display Username") },
                        leadingIcon = { Icon(Icons.Default.Person, null, tint = LightSageText) },
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = EmeraldAccent,
                            unfocusedBorderColor = BarkNeutralGrey.copy(alpha = 0.4f),
                            focusedContainerColor = DeepForestBackground,
                            unfocusedContainerColor = DeepForestBackground
                        )
                    )
                }

                // Email input
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email Address") },
                    leadingIcon = { Icon(Icons.Default.Email, null, tint = LightSageText) },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = EmeraldAccent,
                        unfocusedBorderColor = BarkNeutralGrey.copy(alpha = 0.4f),
                        focusedContainerColor = DeepForestBackground,
                        unfocusedContainerColor = DeepForestBackground
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                )

                // Password input
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    leadingIcon = { Icon(Icons.Default.Lock, null, tint = LightSageText) },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = EmeraldAccent,
                        unfocusedBorderColor = BarkNeutralGrey.copy(alpha = 0.4f),
                        focusedContainerColor = DeepForestBackground,
                        unfocusedContainerColor = DeepForestBackground
                    ),
                    visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                )

                // Admin check (Sign up only)
                if (isSignUp) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = isAdmin,
                            onCheckedChange = { isAdmin = it },
                            colors = CheckboxDefaults.colors(checkedColor = EmeraldAccent)
                        )
                        Text("Register as administrative staff", color = LightSageText, fontSize = 12.sp)
                    }
                }

                // Primary submit button
                Button(
                    onClick = {
                        isLoading = true
                        errorMessage = null
                        if (isSignUp) {
                            model.signUp(username, email, password, isAdmin,
                                onSuccess = { isLoading = false },
                                onFailure = { msg ->
                                    isLoading = false
                                    errorMessage = msg
                                }
                            )
                        } else {
                            model.signIn(email, password,
                                onSuccess = { isLoading = false },
                                onFailure = { msg ->
                                    isLoading = false
                                    errorMessage = msg
                                }
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldAccent),
                    shape = RoundedCornerShape(10.dp),
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.Black)
                    } else {
                        Text(
                            text = if (isSignUp) "REGISTER ACCOUNT" else "SECURE SIGN IN",
                            color = Color.Black,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }


            }
        }
    }
}

// ================= TAB 1: DASHBOARD OVERVIEW =================
@Composable
fun DashboardTab(
    model: EcoScanViewModel,
    user: UserEntity?,
    reports: List<ReportEntity>,
    onGoToScan: () -> Unit
) {
    val scrollState = rememberScrollState()

    // Aggregate statistics
    val totalReports = reports.size
    val cleanedCount = reports.count { it.status == "Cleaned" }
    val points = user?.rewardPoints ?: 0
    val averageCleanliness = if (reports.isNotEmpty()) reports.map { it.cleanlinessScore }.average() else 100.0

    // Analytical graphs dummy data points (last 6 months metrics)
    val successTrendData = listOf(0.40f, 0.48f, 0.58f, 0.64f, 0.72f, averageCleanliness.toFloat() / 100f)
    val monthsLabels = listOf("Jan", "Feb", "Mar", "Apr", "May", "Current")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Welcoming Card Hero Space
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            colors = CardDefaults.cardColors(containerColor = SpruceCardSurface),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, BarkNeutralGrey.copy(alpha = 0.3f))
        ) {
            Row(
                modifier = Modifier
                    .background(
                        Brush.radialGradient(
                            colors = listOf(LeafPrimaryGreen.copy(alpha = 0.45f), Color.Transparent),
                            center = Offset(100f, 50f)
                        )
                    )
                    .padding(16.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "WELCOME BACK, HERO!",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldAccent,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = user?.username ?: "Guest Citizen",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "You've earned ${user?.currentStreak} consecutive activity streak days. Help make New York pristine!",
                        fontSize = 12.sp,
                        color = LightSageText.copy(alpha = 0.82f),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Grid of KPIs
        Row(modifier = Modifier.fillMaxWidth()) {
            DashboardStatsCard(
                title = "TOTAL SCANS",
                value = "$totalReports",
                icon = { Icon(Icons.Default.QrCodeScanner, null, tint = EmeraldAccent, modifier = Modifier.size(24.dp)) },
                colorAccent = EmeraldAccent,
                modifier = Modifier.weight(1f)
            )
            DashboardStatsCard(
                title = "CLEANED PLOTS",
                value = "$cleanedCount",
                icon = { Icon(Icons.Default.CleaningServices, null, tint = MintTealTertiary, modifier = Modifier.size(22.dp)) },
                colorAccent = MintTealTertiary,
                modifier = Modifier.weight(1f)
            )
        }
        Row(modifier = Modifier.fillMaxWidth()) {
            DashboardStatsCard(
                title = "REWARD PINTS",
                value = "$points",
                icon = { Icon(Icons.Default.Stars, null, tint = Color(0xFFFFC107), modifier = Modifier.size(24.dp)) },
                colorAccent = Color(0xFFFFC107),
                modifier = Modifier.weight(1f)
            )
            DashboardStatsCard(
                title = "CITY COMPOSTS",
                value = "${reports.count { it.wasteType == "Organic Waste" }}",
                icon = { Icon(Icons.Default.NaturePeople, null, tint = LightSageText, modifier = Modifier.size(24.dp)) },
                colorAccent = Color(0xFF81C784),
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Center Gauges Block
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(SpruceCardSurface, RoundedCornerShape(16.dp))
                .border(1.dp, BarkNeutralGrey.copy(alpha = 0.25f), RoundedCornerShape(16.dp))
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            CircularAnalyticsGauge(
                score = (averageCleanliness / 100f).toFloat(),
                title = "City Cleanliness Meter",
                colorAccent = EmeraldAccent
            )
            CircularAnalyticsGauge(
                score = if (totalReports > 0) cleanedCount.toFloat() / totalReports.toFloat() else 1.0f,
                title = "Success Cleanup Rate",
                colorAccent = MintTealTertiary
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Clean Success Lines Chart
        EcoTrendLineChart(
            dataPoints = successTrendData,
            labels = monthsLabels
        )

        Spacer(modifier = Modifier.height(12.dp))

        // CTA Scanner Button
        Button(
            onClick = onGoToScan,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = EmeraldAccent),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.CameraAlt, contentDescription = null, tint = Color.Black)
            Spacer(modifier = Modifier.width(8.dp))
            Text("LAUNCH SMART AI SCANNER", fontWeight = FontWeight.Bold, color = Color.Black, fontSize = 13.sp)
        }
    }
}

// ================= TAB 2: AI SCANNER =================
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun ScannerTab(model: EcoScanViewModel) {
    val scrollState = rememberScrollState()

    val scannedBitmap by model.scannedBitmap.collectAsState()
    val scannedImageName by model.scannedImageName.collectAsState()
    val isAnalyzing by model.isAnalyzing.collectAsState()
    val analysisResult by model.analysisResult.collectAsState()
    val analysisError by model.analysisError.collectAsState()

    val gpsLat by model.gpsLatitude.collectAsState()
    val gpsLng by model.gpsLongitude.collectAsState()
    val addressText by model.detectedAddress.collectAsState()
    val description by model.submitDescription.collectAsState()
    val submitStatus by model.submitStatus.collectAsState()

    // Procedural Sandbox Mock Image utilities for the emulator
    val ctx = LocalContext.current

    // Launchers for authentic system camera and photo gallery selections
    var tempPhotoFile by remember { mutableStateOf<File?>(null) }
    val cameraPermissionState = rememberPermissionState(android.Manifest.permission.CAMERA)
    val locationPermissionState = rememberPermissionState(android.Manifest.permission.ACCESS_FINE_LOCATION)

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                val inputStream = ctx.contentResolver.openInputStream(it)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                if (bitmap != null) {
                    model.setScannedImage(bitmap, "gallery_${System.currentTimeMillis()}.jpg")
                    model.captureRealGPSLocation(ctx)
                }
            } catch (e: Exception) {
                android.util.Log.e("EcoScanGallery", "Error retrieving gallery photo: ${e.message}")
            }
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success: Boolean ->
        if (success) {
            tempPhotoFile?.let { file ->
                try {
                    val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                    if (bitmap != null) {
                        model.setScannedImage(bitmap, file.name)
                        model.captureRealGPSLocation(ctx)
                    }
                } catch (e: Exception) {
                    android.util.Log.e("EcoScanCamera", "Error decoding photo file: ${e.message}")
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "AI WASTE DETECTOR & CLASSIFIER",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = EmeraldAccent,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 1.sp,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Start
        )
        Text(
            text = "Capture & Report Waste Zones",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            textAlign = TextAlign.Start
        )



        // Image Viewport Frame
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .background(SpruceCardSurface, RoundedCornerShape(16.dp))
                .border(2.dp, BarkNeutralGrey.copy(alpha = 0.3f), RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (scannedBitmap != null) {
                Image(
                    bitmap = scannedBitmap!!.asImageBitmap(),
                    contentDescription = "Scanned spot preview",
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(16.dp)),
                    contentScale = ContentScale.Crop
                )

                // Small badge path indicator
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(8.dp)
                        .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = scannedImageName ?: "Custom Scan",
                        fontSize = 9.sp,
                        color = Color.LightGray,
                        fontFamily = FontFamily.Monospace
                    )
                }
                
                // Reset Floating Button
                IconButton(
                    onClick = { model.clearScanner() },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                ) {
                    Icon(Icons.Default.Close, contentDescription = null, tint = Color.White)
                }
            } else {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudUpload,
                        contentDescription = null,
                        tint = LightSageText,
                        modifier = Modifier.size(44.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "ADD WASTE ZONE PHOTO",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = {
                                if (cameraPermissionState.status.isGranted) {
                                    val file = File(ctx.cacheDir, "camera_${System.currentTimeMillis()}.jpg")
                                    tempPhotoFile = file
                                    val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", file)
                                    cameraLauncher.launch(uri)
                                } else {
                                    cameraPermissionState.launchPermissionRequest()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldAccent),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.CameraAlt, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Camera Capture", fontSize = 11.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                galleryLauncher.launch("image/*")
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = BarkNeutralGrey),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.PhotoLibrary, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Select Gallery", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "(We will request GPS location automatically)",
                        color = LightSageText.copy(alpha = 0.5f),
                        fontSize = 9.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Trigger analysis Button
        if (scannedBitmap != null && analysisResult == null) {
            Button(
                onClick = { model.requestAiWasteAnalysis() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldAccent),
                shape = RoundedCornerShape(12.dp),
                enabled = !isAnalyzing
            ) {
                if (isAnalyzing) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.Black)
                } else {
                    Icon(Icons.Default.Analytics, contentDescription = null, tint = Color.Black)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("ANALYZE WITH AI VISION ENGINE", fontWeight = FontWeight.Bold, color = Color.Black)
                }
            }
        }

        // Output error
        if (analysisError != null) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0x33D32F2F)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Text(
                    text = "API Alert: ${analysisError!!}",
                    color = Color(0xFFFFCDD2),
                    fontSize = 11.sp,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }

        // ================= AI RESULTS ANALYSIS DATA CARDS =================
        if (analysisResult != null) {
            val res = analysisResult!!

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                colors = CardDefaults.cardColors(containerColor = SpruceCardSurface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, BarkNeutralGrey.copy(alpha = 0.25f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = when (res.severityLevel) {
                                "Critical" -> Color.Red
                                "High" -> Color(0xFFFF9800)
                                "Medium" -> Color(0xFFFFEB3B)
                                else -> Color(0xFF4CAF50)
                            },
                            contentColor = Color.Black,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = " SEVERITY: ${res.severityLevel.uppercase()} ",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(2.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "AI WASTE DETECTED",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldAccent,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Primary Class: ${res.wasteType}",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Probability bar charts
                    res.probabilities?.forEach { (key, prob) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = key,
                                fontSize = 11.sp,
                                color = LightSageText,
                                modifier = Modifier.width(90.dp)
                            )
                            LinearProgressIndicator(
                                progress = prob.toFloat(),
                                color = if (key == res.wasteType) EmeraldAccent else BarkNeutralGrey,
                                trackColor = BarkNeutralGrey.copy(alpha = 0.2f),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(6.dp)
                                    .clip(CircleShape)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "${(prob * 100).toInt()}%",
                                fontSize = 11.sp,
                                color = Color.White,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.width(30.dp)
                            )
                        }
                    }

                    Divider(color = BarkNeutralGrey.copy(alpha = 0.3f), modifier = Modifier.padding(vertical = 12.dp))

                    // Severity metrics gauges
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Cleanliness Score", fontSize = 10.sp, color = LightSageText)
                            Text("${res.cleanlinessScore}/100", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White, fontFamily = FontFamily.Monospace)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Coverage Area", fontSize = 10.sp, color = LightSageText)
                            Text("${res.detectedPercentage.toInt()}%", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White, fontFamily = FontFamily.Monospace)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Priority Rank", fontSize = 10.sp, color = LightSageText)
                            Text(res.recomCleanupPriority, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = EmeraldAccent)
                        }
                    }

                    Divider(color = BarkNeutralGrey.copy(alpha = 0.3f), modifier = Modifier.padding(vertical = 12.dp))

                    Text(
                        text = "ENVIRONMENTAL BIOLOGICAL THREAT",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Red,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = res.environmentalImpact,
                        fontSize = 11.sp,
                        color = LightSageText.copy(alpha = 0.85f),
                        modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
                    )

                    Text(
                        text = "AI CLEANUP PROTOCOL",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldAccent,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = res.aiRecommendation,
                        fontSize = 11.sp,
                        color = LightSageText.copy(alpha = 0.85f),
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // ================= REPORT SUBMISSION SETTINGS MODULE =================
            Text(
                text = "Eco-GPS Tracking Coordinates",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White,
                modifier = Modifier.fillMaxWidth()
            )

            // Street coordinates & Address field display
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                colors = CardDefaults.cardColors(containerColor = SpruceCardSurface),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, BarkNeutralGrey.copy(alpha = 0.2f))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LocationOn, contentDescription = null, tint = EmeraldAccent, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = addressText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Latitude: $gpsLat  |  Longitude: $gpsLng",
                        fontSize = 10.sp,
                        color = LightSageText,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Button(
                        onClick = { model.randomizeLocation() },
                        colors = ButtonDefaults.buttonColors(containerColor = BarkNeutralGrey.copy(alpha = 0.4f)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Refresh, null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Randomize NY Spot Coordinates", fontSize = 10.sp, color = Color.White)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Submitter description Input field
            OutlinedTextField(
                value = description,
                onValueChange = { model.setSubmitDescription(it) },
                label = { Text("Report description & comments") },
                placeholder = { Text("e.g., Plastic heap blocking path beside park entrance...") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = EmeraldAccent,
                    unfocusedBorderColor = BarkNeutralGrey.copy(alpha = 0.4f),
                    focusedLabelColor = EmeraldAccent,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedContainerColor = SpruceCardSurface,
                    unfocusedContainerColor = SpruceCardSurface
                ),
                maxLines = 3
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Upload report button
            Button(
                onClick = { model.submitReport() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldAccent),
                shape = RoundedCornerShape(12.dp),
                enabled = submitStatus != "Submitting"
            ) {
                if (submitStatus == "Submitting") {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.Black)
                } else {
                    Icon(Icons.Default.CloudUpload, contentDescription = null, tint = Color.Black)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("FILE CIVIC WASTE REPORT (+50 PTS)", fontWeight = FontWeight.Bold, color = Color.Black)
                }
            }

            if (submitStatus == "Success") {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0x334CAF50)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = Color.Green)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("SUCCESSFULLY REPORTED!", fontWeight = FontWeight.Bold, color = Color.Green)
                        }
                        Text(
                            text = "Your civic report has been logged. Admin verification is pending. Thank you for scanning!",
                            fontSize = 11.sp,
                            color = LightSageText,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

// ================= TAB 3: INTERACTIVE ECO-MAP =================
@Composable
fun MapTab(
    model: EcoScanViewModel,
    filteredReports: List<ReportEntity>
) {
    val query by model.searchQuery.collectAsState()
    val typeFilter by model.selectedTypeFilter.collectAsState()
    val severityFilter by model.selectedSeverityFilter.collectAsState()

    var activePopupReport by remember { mutableStateOf<ReportEntity?>(null) }
    var mapType by remember { mutableStateOf("google") }

    val mapCenterLat = activePopupReport?.latitude ?: filteredReports.firstOrNull()?.latitude ?: 40.7128
    val mapCenterLng = activePopupReport?.longitude ?: filteredReports.firstOrNull()?.longitude ?: -74.0060

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Upper search boundaries
        Text(
            text = "INTELLIGENT INTEGRATED CIVIC MAP",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = EmeraldAccent,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 1.sp
        )
        Text(
            text = "Explore & Track Litter Dumps",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // Filters bar
        OutlinedTextField(
            value = query,
            onValueChange = { model.updateQuery(it) },
            placeholder = { Text("Search location or block address...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = LightSageText) },
            trailingIcon = if (query.isNotEmpty()) {
                { IconButton(onClick = { model.updateQuery("") }) { Icon(Icons.Default.Clear, null, tint = Color.White) } }
            } else null,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = EmeraldAccent,
                unfocusedBorderColor = BarkNeutralGrey.copy(alpha = 0.3f),
                focusedContainerColor = SpruceCardSurface,
                unfocusedContainerColor = SpruceCardSurface,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            )
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Type Filters Selector Chips
            val filterTypes = listOf("Plastic", "E-Waste", "Organic Waste", "Mixed Waste", "Metal", "Paper")
            Box {
                Button(
                    onClick = { model.updateTypeFilter(null) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (typeFilter == null) EmeraldAccent else BarkNeutralGrey.copy(alpha = 0.3f)
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text("All Types", fontSize = 10.sp, color = if (typeFilter == null) Color.Black else Color.White)
                }
            }
            filterTypes.forEach { type ->
                Button(
                    onClick = { model.updateTypeFilter(type) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (typeFilter == type) EmeraldAccent else BarkNeutralGrey.copy(alpha = 0.3f)
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(type, fontSize = 10.sp, color = if (typeFilter == type) Color.Black else Color.White)
                }
            }
        }

        // Map switcher buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { mapType = "google" },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (mapType == "google") EmeraldAccent else SpruceCardSurface
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(vertical = 4.dp, horizontal = 8.dp)
            ) {
                Icon(Icons.Default.Public, contentDescription = null, tint = if (mapType == "google") Color.Black else Color.White, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Live Google Map", color = if (mapType == "google") Color.Black else Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = { mapType = "sandbox" },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (mapType == "sandbox") EmeraldAccent else SpruceCardSurface
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(vertical = 4.dp, horizontal = 8.dp)
            ) {
                Icon(Icons.Default.SportsEsports, contentDescription = null, tint = if (mapType == "sandbox") Color.Black else Color.White, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("AI Tactical Sandbox", color = if (mapType == "sandbox") Color.Black else Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        if (mapType == "google") {
            GoogleMapWebView(
                latitude = mapCenterLat,
                longitude = mapCenterLng,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .border(1.dp, BarkNeutralGrey.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                    .clip(RoundedCornerShape(16.dp))
            )
        } else {
            // Live Tactical Map Canvas
            InteractiveTacticalCanvasMap(
                reports = filteredReports,
                onPinClicked = { report -> activePopupReport = report },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Selected Pin detail Drawer card pops up
        if (activePopupReport != null) {
            val rep = activePopupReport!!
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize(),
                colors = CardDefaults.cardColors(containerColor = SpruceCardSurface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, EmeraldAccent.copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = when (rep.severityLevel) {
                                "Critical" -> Color.Red
                                "High" -> Color(0xFFFF9800)
                                else -> Color(0xFF4CAF50)
                            },
                            contentColor = Color.Black,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = " ${rep.severityLevel.uppercase()} ",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = rep.wasteType.uppercase(),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldAccent,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        IconButton(
                            onClick = { activePopupReport = null },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = null, tint = Color.White)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = rep.address,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = rep.description,
                        fontSize = 11.sp,
                        color = LightSageText.copy(alpha = 0.8f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 2.dp)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, null, tint = if (rep.status == "Cleaned") Color.Green else BarkNeutralGrey, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = rep.status,
                                fontSize = 11.sp,
                                color = if (rep.status == "Cleaned") Color.Green else Color.LightGray
                            )
                        }

                        Text(
                            text = "Confidence: ${(rep.wasteConfidence * 100).toInt()}%",
                            fontSize = 10.sp,
                            color = LightSageText,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        } else {
            // Friendly tips on map touch
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = SpruceCardSurface.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.TouchApp, contentDescription = null, tint = LightSageText)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Touch any glowing coordinates pin on the radar map to inspect analytical severity tags and report feedback.",
                        fontSize = 11.sp,
                        color = LightSageText.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}

// ================= TAB 4: HISTORIC FEEDS & CLEANUP COMPARER =================
@Composable
fun ReportsFeedTab(
    model: EcoScanViewModel,
    user: UserEntity?,
    reportsList: List<ReportEntity>
) {
    val cleanupTargetId by model.cleanupTargetReportId.collectAsState()
    val afterBitmap by model.cleanupAfterBitmap.collectAsState()
    val isCleanupSaving by model.isSubmittingCleanup.collectAsState()

    var activeReportTabFilter by remember { mutableStateOf("All") } // "All", "Pending", "Cleaned"
    val context = LocalContext.current

    // Launchers for authentic system camera and photo gallery selections for cleanup
    var tempCleanupFile by remember { mutableStateOf<File?>(null) }
    val cleanupGalleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                val inputStream = context.contentResolver.openInputStream(it)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                if (bitmap != null) {
                    model.setCleanupAfterImage(bitmap)
                }
            } catch (e: Exception) {
                android.util.Log.e("EcoScanCleanup", "Error retrieving gallery photo: ${e.message}")
            }
        }
    }

    val cleanupCameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success: Boolean ->
        if (success) {
            tempCleanupFile?.let { file ->
                try {
                    val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                    if (bitmap != null) {
                        model.setCleanupAfterImage(bitmap)
                    }
                } catch (e: Exception) {
                    android.util.Log.e("EcoScanCleanup", "Error decoding camera photo file: ${e.message}")
                }
            }
        }
    }

    val displayedReports = when (activeReportTabFilter) {
        "Pending" -> reportsList.filter { it.status != "Cleaned" }
        "Cleaned" -> reportsList.filter { it.status == "Cleaned" }
        else -> reportsList
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Feed titles header
        item {
            Text(
                text = "COMMUNITY CIVIC PROGRESS LOGS",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = EmeraldAccent,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.sp
            )
            Text(
                text = "Report Feed & Verification Dashboard",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // Dynamic segments tabs Selector
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SpruceCardSurface, RoundedCornerShape(8.dp))
                    .border(1.dp, BarkNeutralGrey.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                    .padding(4.dp)
            ) {
                listOf("All", "Pending", "Cleaned").forEach { tabName ->
                    val isSelected = activeReportTabFilter == tabName
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(
                                if (isSelected) LeafPrimaryGreen else Color.Transparent,
                                RoundedCornerShape(6.dp)
                            )
                            .clickable { activeReportTabFilter = tabName }
                            .padding(vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = tabName,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) Color.White else LightSageText
                        )
                    }
                }
            }
        }

        // Active Before-After Cleanup image logger modal overlay inline
        if (cleanupTargetId != null) {
            item {
                val repInstance = reportsList.find { it.id == cleanupTargetId }
                if (repInstance != null) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, EmeraldAccent, RoundedCornerShape(16.dp)),
                        colors = CardDefaults.cardColors(containerColor = SpruceCardSurface)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CleaningServices, contentDescription = null, tint = EmeraldAccent)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "LOG CLEANUP RESULTS",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.weight(1f))
                                IconButton(onClick = { model.clearCleanupTarget() }) {
                                    Icon(Icons.Default.Close, null, tint = Color.White)
                                }
                            }

                            Text(
                                text = "Spot: ${repInstance.address}",
                                fontSize = 11.sp,
                                color = LightSageText
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            // After image slot container click trigger
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(140.dp)
                                    .background(
                                        BarkNeutralGrey.copy(alpha = 0.15f),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .border(
                                        1.dp,
                                        BarkNeutralGrey.copy(alpha = 0.4f),
                                        RoundedCornerShape(8.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (afterBitmap != null) {
                                    Box(modifier = Modifier.fillMaxSize()) {
                                        Image(
                                            bitmap = afterBitmap!!.asImageBitmap(),
                                            contentDescription = "After image preview",
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .clip(RoundedCornerShape(8.dp)),
                                            contentScale = ContentScale.Crop
                                        )
                                        // Reset floating button
                                        IconButton(
                                            onClick = { model.clearCleanupTarget() },
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .padding(6.dp)
                                                .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                                                .size(28.dp)
                                        ) {
                                            Icon(Icons.Default.Close, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                } else {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.padding(12.dp)
                                    ) {
                                        Text(
                                            "ADD WORK COMPLETION PHOTO",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Button(
                                                onClick = {
                                                    val file = File(context.cacheDir, "cleanup_${System.currentTimeMillis()}.jpg")
                                                    tempCleanupFile = file
                                                    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                                                    cleanupCameraLauncher.launch(uri)
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldAccent),
                                                shape = RoundedCornerShape(8.dp),
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                            ) {
                                                Icon(Icons.Default.CameraAlt, contentDescription = null, tint = Color.Black, modifier = Modifier.size(14.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Camera", fontSize = 10.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                                            }

                                            Button(
                                                onClick = {
                                                    cleanupGalleryLauncher.launch("image/*")
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = BarkNeutralGrey),
                                                shape = RoundedCornerShape(8.dp),
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                            ) {
                                                Icon(Icons.Default.PhotoLibrary, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Gallery", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                            }


                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            "Submit a picture of the clean, sorted yard to claim your bounty points",
                                            fontSize = 8.sp,
                                            color = LightSageText.copy(alpha = 0.6f)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Button(
                                onClick = { model.submitCleanupResult() },
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldAccent),
                                modifier = Modifier.fillMaxWidth(),
                                enabled = afterBitmap != null && !isCleanupSaving
                            ) {
                                if (isCleanupSaving) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        color = Color.Black
                                    )
                                } else {
                                    Text(
                                        "SUBMIT PROOF FOR VERIFICATION (+200 PTS)",
                                        fontSize = 11.sp,
                                        color = Color.Black,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if (displayedReports.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Inbox,
                            null,
                            tint = LightSageText,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "No reports match the segment criteria",
                            color = LightSageText,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        } else {
            items(displayedReports) { report ->
                ReportListItem(
                    report = report,
                    currentUser = user,
                    onCleanupRequested = { id -> model.setCleanupTarget(id) },
                    onAdminVerify = { isVerified, notes ->
                        model.adminVerifyReport(
                            report.id,
                            isVerified,
                            notes
                        )
                    },
                    onAdminDelete = { model.adminDeleteReport(report.id) }
                )
            }
        }
    }
}

// Inline Sub-Item representing individual report card
@Composable
fun ReportListItem(
    report: ReportEntity,
    currentUser: UserEntity?,
    onCleanupRequested: (Long) -> Unit,
    onAdminVerify: (Boolean, String) -> Unit,
    onAdminDelete: () -> Unit
) {
    var adminComment by remember { mutableStateOf("") }
    var isVerifiedChecked by remember { mutableStateOf(report.isVerified) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SpruceCardSurface),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, BarkNeutralGrey.copy(alpha = 0.25f))
    ) {
        Column {
            // High fidelity image compare slider
            BeforeAfterSlider(
                beforePath = report.imageUrl,
                afterPath = report.afterImageUrl
            )

            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = when (report.severityLevel) {
                            "Critical" -> Color.Red
                            "High" -> Color(0xFFFF9800)
                            else -> Color(0xFF4CAF50)
                        },
                        contentColor = Color.Black,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = " ${report.severityLevel.uppercase()} ",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = report.wasteType.uppercase(),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldAccent,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    // Verification Status Pill tag
                    Surface(
                        color = when (report.status) {
                            "Cleaned" -> Color(0xFFE8F5E9)
                            "In Progress" -> Color(0xFFFFFDE7)
                            "Verified" -> Color(0xFFE3F2FD)
                            else -> Color(0xFFEEEEEE)
                        },
                        contentColor = Color.Black,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "  ${report.status}  ",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = report.address,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Text(
                    text = report.description,
                    fontSize = 12.sp,
                    color = LightSageText.copy(alpha = 0.9f),
                    modifier = Modifier.padding(vertical = 4.dp)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Reported by: ${report.reporterName}",
                        fontSize = 10.sp,
                        color = LightSageText.copy(alpha = 0.7f)
                    )
                    Text(
                        text = SimpleDateFormat("MMM d, yyyy  HH:mm", Locale.getDefault()).format(
                            Date(report.timestamp)
                        ),
                        fontSize = 10.sp,
                        color = LightSageText.copy(alpha = 0.7f),
                        fontFamily = FontFamily.Monospace
                    )
                }

                // If cleanup is completed, showcase improvement percentage!
                if (report.status == "Cleaned" && report.improvementPercentage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(EmeraldAccent.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Verified, null, tint = EmeraldAccent, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "BEFORE & AFTER SUCCESS INDEX: ${report.improvementPercentage?.toInt()}% CLEAR!",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                if (report.status != "Cleaned") {
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { onCleanupRequested(report.id) },
                        colors = ButtonDefaults.buttonColors(containerColor = FreshGreenPrimary),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.CleaningServices, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("I'VE CLEANED SPLOT (+200 PTS)", fontWeight = FontWeight.SemiBold, color = Color.White)
                    }
                }

                // ================= ADMIN VALIDATION EXTRA CONTROLS =================
                if (currentUser?.isAdmin == true) {
                    Divider(
                        color = BarkNeutralGrey.copy(alpha = 0.3f),
                        modifier = Modifier.padding(vertical = 12.dp)
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Security, null, tint = Color.Red, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("ADMINISTRATOR AUDITING CODES", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White, fontFamily = FontFamily.Monospace)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = isVerifiedChecked,
                            onCheckedChange = { isVerifiedChecked = it },
                            colors = CheckboxDefaults.colors(checkedColor = EmeraldAccent)
                        )
                        Text("Approve / Verify Spot Report", fontSize = 11.sp, color = LightSageText)

                        Spacer(modifier = Modifier.weight(1f))

                        IconButton(onClick = onAdminDelete) {
                            Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red)
                        }
                    }

                    OutlinedTextField(
                        value = adminComment,
                        onValueChange = { adminComment = it },
                        placeholder = { Text("verification notations or notes...") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Red,
                            unfocusedBorderColor = BarkNeutralGrey.copy(alpha = 0.3f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = { onAdminVerify(isVerifiedChecked, adminComment) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("SAVE VERIFICATION AUDITS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}

// ================= TAB 5: ALLIANCE, CHALLENGES & LEADERBOARDS =================
@Composable
fun AllianceTab(
    model: EcoScanViewModel,
    user: UserEntity?,
    challenges: List<ChallengeEntity>,
    events: List<EventEntity>,
    leaderboard: List<UserEntity>
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        Text(
            text = "CITIZEN ECO-ALLIANCE LEAGUE",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = EmeraldAccent,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 1.sp
        )
        Text(
            text = "Gamification, Challenges & Rallies",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // --- SECTION 1: ACTIVE CHALLENGES ---
        Text(
            text = "CITIZEN SWEEP CHALLENGES",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = LightSageText,
            modifier = Modifier.padding(bottom = 6.dp)
        )

        challenges.forEach { ch ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                colors = CardDefaults.cardColors(containerColor = SpruceCardSurface),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, BarkNeutralGrey.copy(alpha = 0.2f))
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                if (ch.isCompleted) EmeraldAccent.copy(alpha = 0.2f) else BarkNeutralGrey.copy(
                                    alpha = 0.2f
                                ), CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (ch.isCompleted) Icons.Default.EmojiEvents else Icons.Default.Lock,
                            contentDescription = null,
                            tint = if (ch.isCompleted) Color(0xFFFFC107) else Color.LightGray
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = ch.title,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Surface(
                                color = BarkNeutralGrey.copy(alpha = 0.4f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = " ${ch.category} ",
                                    fontSize = 8.sp,
                                    color = LightSageText,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Text(
                            text = ch.description,
                            fontSize = 11.sp,
                            color = LightSageText.copy(alpha = 0.8f)
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        // Linear Progress Meter
                        val ratio = ch.currentProgress.toFloat() / ch.targetThreshold.toFloat()
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            LinearProgressIndicator(
                                progress = ratio.coerceAtMost(1.0f),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(4.dp)
                                    .clip(CircleShape),
                                color = EmeraldAccent,
                                trackColor = BarkNeutralGrey.copy(alpha = 0.2f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "${ch.currentProgress}/${ch.targetThreshold}",
                                fontSize = 10.sp,
                                color = Color.White,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Stars, contentDescription = null, tint = Color(0xFFFFC107), modifier = Modifier.size(16.dp))
                        Text(
                            text = "+${ch.pointsReward}",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFFC107),
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- SECTION 2: COMMUNITY EVENTS ---
        Text(
            text = "UPCOMING VOLUNTEER CLEANUP EVENTS",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = LightSageText,
            modifier = Modifier.padding(bottom = 6.dp)
        )

        events.forEach { ev ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                colors = CardDefaults.cardColors(containerColor = SpruceCardSurface),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Group, contentDescription = null, tint = EmeraldAccent, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = ev.title,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    Text(
                        text = ev.description,
                        fontSize = 11.sp,
                        color = LightSageText.copy(alpha = 0.85f),
                        modifier = Modifier.padding(vertical = 4.dp)
                    )

                    Row(
                        modifier = Modifier.padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.AccessTime, null, tint = LightSageText, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = SimpleDateFormat("EEE, MMM d, yyyy  HH:mm", Locale.getDefault()).format(
                                Date(ev.scheduledTime)
                            ),
                            fontSize = 10.sp,
                            color = LightSageText
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Icon(Icons.Default.People, null, tint = LightSageText, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${ev.participantCount} registered",
                            fontSize = 10.sp,
                            color = LightSageText
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "@ ${ev.locationName}",
                            fontSize = 10.sp,
                            color = EmeraldAccent,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Button(
                            onClick = { model.volunteerToEvent(ev.eventId) },
                            enabled = !ev.isJoined,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (ev.isJoined) BarkNeutralGrey else EmeraldAccent
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = if (ev.isJoined) "Registered" else "Join rally (+${ev.pointsGiven}pts)",
                                fontSize = 10.sp,
                                color = if (ev.isJoined) Color.White else Color.Black,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- SECTION 3: COMMUNITY LEADERBOARD ---
        Text(
            text = "TOP ECO-DEFENDERS POINT LEADERBOARD",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = LightSageText,
            modifier = Modifier.padding(bottom = 6.dp)
        )

        leaderboard.forEachIndexed { idx, player ->
            val isLoggedUser = player.userId == user?.userId

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 3.dp)
                    .background(
                        if (isLoggedUser) LeafPrimaryGreen.copy(alpha = 0.35f) else SpruceCardSurface,
                        RoundedCornerShape(8.dp)
                    )
                    .border(
                        1.dp,
                        if (isLoggedUser) EmeraldAccent else Color.Transparent,
                        RoundedCornerShape(8.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Medal slot / Rank index
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(
                            when (idx) {
                                0 -> Color(0xFFFFD700) // Gold
                                1 -> Color(0xFFC0C0C0) // Silver
                                2 -> Color(0xFFCD7F32) // Bronze
                                else -> Color.Transparent
                            },
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${idx + 1}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (idx in 0..2) Color.Black else Color.White,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Text(
                    text = player.username,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.weight(1f)
                )

                // Stats totals
                Text(
                    text = "${player.reportsSubmittedCount} Scans",
                    fontSize = 11.sp,
                    color = LightSageText,
                    modifier = Modifier.padding(end = 12.dp)
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Stars, contentDescription = null, tint = Color(0xFFFFC107), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${player.rewardPoints}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}
