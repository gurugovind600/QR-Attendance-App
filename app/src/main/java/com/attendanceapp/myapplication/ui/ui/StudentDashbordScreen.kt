package com.attendanceapp.myapplication.ui.ui

import android.annotation.SuppressLint
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.attendanceapp.myapplication.R
import com.attendanceapp.myapplication.model.Student
import com.attendanceapp.myapplication.utils.QRCodeHelper
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector
import com.attendanceapp.myapplication.viewmodel.StudentViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@RequiresApi(Build.VERSION_CODES.N)
@OptIn(ExperimentalMaterialApi::class)
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun StudentDashboardScreen(
    student: Student,
    email: String,
    viewModel: StudentViewModel, // <--- ADDED THIS PARAMETER
    onLogoutClick: () -> Unit,
    onBiometricToggle: (Boolean) -> Unit,
    onMarkAttendanceClick: () -> Unit,
    onRefreshTriggered: () -> Unit
) {
    // UI States
    var showQrDialog by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableIntStateOf(0) } // 0=Home, 1=Profile, 2=Settings

    // Month Dropdown State
    var showMonthMenu by remember { mutableStateOf(false) }
    // Observe the currently selected month from ViewModel
    val currentSelectedMonth by viewModel.selectedMonth.collectAsState()

    // Pull to Refresh State
    var isRefreshing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val pullRefreshState = rememberPullRefreshState(
        refreshing = isRefreshing,
        onRefresh = {
            isRefreshing = true
            onRefreshTriggered()
            scope.launch {
                delay(1000)
                isRefreshing = false
            }
        }
    )

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = Color.White,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                    label = { Text("Home") },
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Person, contentDescription = "Profile") },
                    label = { Text("Profile") },
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                    label = { Text("Settings") },
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 }
                )
            }
        }
    ) { innerPadding ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = innerPadding.calculateBottomPadding())
                .pullRefresh(pullRefreshState)
        ) {

            // CONTENT AREA
            if (selectedTab == 0) {
                // *** HOME TAB ***
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFFF5F5F5))
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    // 1. Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(text = "Hello,", fontSize = 18.sp, color = Color.Gray)
                            Text(
                                text = if (student.name.isNotEmpty()) "${student.name} 👋" else "Loading...",
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF3D5AFE)
                            )
                        }
                        IconButton(onClick = onLogoutClick) {
                            Icon(Icons.AutoMirrored.Filled.ExitToApp, "Logout", tint = Color.Red)
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // 2. Auto-Rotating Banners
                    val bannerImages = listOf(R.drawable.banner1, R.drawable.banner2, R.drawable.banner3)
                    val pagerState = rememberPagerState(pageCount = { bannerImages.size })

                    LaunchedEffect(Unit) {
                        while (true) {
                            delay(3000)
                            try {
                                val nextPage = (pagerState.currentPage + 1) % bannerImages.size
                                pagerState.animateScrollToPage(nextPage)
                            } catch (_: Exception) { }
                        }
                    }

                    Card(
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth().height(180.dp),
                        elevation = CardDefaults.cardElevation(4.dp)
                    ) {
                        HorizontalPager(state = pagerState) { page ->
                            Image(
                                painter = painterResource(id = bannerImages[page]),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // 3. Quick Actions
                    Text("Quick Actions", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        QuickActionCard(
                            "Attendance", "GPS Check", Icons.Default.LocationOn, Color(0xFFE0F7FA), Color(0xFF006064), Modifier.weight(1f), onMarkAttendanceClick
                        )
                        QuickActionCard(
                            "Digital ID", "Show QR", Icons.Default.Person, Color(0xFFF3E5F5), Color(0xFF4A148C), Modifier.weight(1f), { showQrDialog = true }
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // --- 3.5 Stats & Month Filter ---
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Overview", fontSize = 18.sp, fontWeight = FontWeight.Bold)

                        // Simple Month Button
                        Box {
                            Button(
                                onClick = { showMonthMenu = true },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                                elevation = ButtonDefaults.buttonElevation(2.dp)
                            ) {
                                Text("$currentSelectedMonth ▾", color = Color.Black)
                            }

                            DropdownMenu(
                                expanded = showMonthMenu,
                                onDismissRequest = { showMonthMenu = false },
                                modifier = Modifier.background(Color.White)
                            ) {
                                val availableMonths = viewModel.getAvailableMonths() // Function we added earlier
                                availableMonths.forEach { monthStr ->
                                    DropdownMenuItem(
                                        text = { Text(monthStr) },
                                        onClick = {
                                            viewModel.changeMonthFilter(monthStr)
                                            viewModel.fetchUserData()
                                            showMonthMenu = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // RE-ADDED: Percent & Count Cards
                    Row(modifier = Modifier.fillMaxWidth()) {
                        // Percent Card
                        Card(
                            modifier = Modifier.weight(1f).height(100.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD))
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text("${student.attendancePercentage.toInt()}%", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1565C0))
                                Text("Attendance Rate", fontSize = 12.sp)
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        // Count Card
                        Card(
                            modifier = Modifier.weight(1f).height(100.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9))
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text("${student.presentCount}", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                                Text("Days Present", fontSize = 12.sp)
                            }
                        }
                    }

                    // 4. Donut Chart (Pie Chart)
                    if (student.presentCount > 0 || student.absentCount > 0) {
                        DonutChartSection(student)
                    } else {
                        // Empty State
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                        ) {
                            Text("No attendance data yet.", modifier = Modifier.padding(16.dp), color = Color.Gray)
                        }
                    }

                    Spacer(modifier = Modifier.height(50.dp))
                }
            } else if (selectedTab == 1) {
                // *** PROFILE TAB ***
                // FIXED: Now we pass the ViewModel correctly here
                ProfileScreen(
                    student = student,
                    email = email.ifEmpty { com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.email ?: "No Email" },
                    onSaveProfile = { newAddress, newCategory ->
                        viewModel.updateStudentProfile(newAddress, newCategory)
                    }
                )
            } else {
                // *** SETTINGS TAB ***
                Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                    Text("Settings", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(16.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Biometric Login", fontWeight = FontWeight.Bold)
                                Text("Use fingerprint next time", fontSize = 12.sp, color = Color.Gray)
                            }
                            Switch(
                                checked = student.bioMetricEnabled,
                                onCheckedChange = { onBiometricToggle(it) }
                            )
                        }
                    }
                }
            }

            // Pull Refresh Indicator
            PullRefreshIndicator(
                refreshing = isRefreshing,
                state = pullRefreshState,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }
    }

    // --- QR DIALOG ---
    if (showQrDialog) {
        Dialog(onDismissRequest = { showQrDialog = false }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                modifier = Modifier.padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Digital ID", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(16.dp))
                    val qrContent = if (student.rollNo.isNotEmpty()) student.rollNo else "Unknown"
                    val qrBitmap = remember(qrContent) { QRCodeHelper.generateQRCode(qrContent) }

                    if (qrBitmap != null) {
                        Image(bitmap = qrBitmap.asImageBitmap(), contentDescription = "QR", modifier = Modifier.size(200.dp))
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(student.name, fontWeight = FontWeight.Bold)
                    Text("Roll: ${student.rollNo}", color = Color.Gray)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { showQrDialog = false }) { Text("Close") }
                }
            }
        }
    }
}

// Helper Composable for Cards
@Composable
fun QuickActionCard(
    title: String, subtitle: String, icon: ImageVector,
    bgColor: Color, iconColor: Color, modifier: Modifier, onClick: () -> Unit
) {
    Card(
        modifier = modifier.height(110.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = iconColor)
            Spacer(modifier = Modifier.height(8.dp))
            Text(title, fontWeight = FontWeight.Bold, color = iconColor)
            Text(subtitle, fontSize = 10.sp, color = Color.Gray)
        }
    }
}

// Helper Composable for Donut Chart
@Composable
fun DonutChartSection(student: Student) {
    Text("Participation Analytics", fontSize = 18.sp, fontWeight = FontWeight.Bold)
    Spacer(modifier = Modifier.height(8.dp))

    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier.padding(24.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(120.dp)) {
                val total = (student.presentCount + student.absentCount).toFloat()
                val presentAngle = if (total > 0) (student.presentCount / total) * 360f else 0f

                androidx.compose.foundation.Canvas(modifier = Modifier.size(120.dp)) {
                    val stroke = 30f
                    // Absent (Red Background)
                    drawArc(Color(0xFFEF5350), 0f, 360f, false, style = androidx.compose.ui.graphics.drawscope.Stroke(stroke))
                    // Present (Green Overlay)
                    drawArc(Color(0xFF4CAF50), -90f, presentAngle, false, style = androidx.compose.ui.graphics.drawscope.Stroke(stroke, cap = androidx.compose.ui.graphics.StrokeCap.Round))
                }
                Text("${student.attendancePercentage.toInt()}%", fontWeight = FontWeight.Bold, fontSize = 20.sp)
            }

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                LegendItem(Color(0xFF4CAF50), "Present: ${student.presentCount}")
                LegendItem(Color(0xFFEF5350), "Absent: ${student.absentCount}")
            }
        }
    }
}

@Composable
fun LegendItem(color: Color, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(12.dp).background(color, RoundedCornerShape(2.dp)))
        Spacer(modifier = Modifier.width(8.dp))
        Text(text, fontSize = 14.sp, color = Color.Gray)
    }
}
