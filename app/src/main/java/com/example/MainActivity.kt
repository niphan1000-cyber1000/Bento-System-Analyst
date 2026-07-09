package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.screens.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.AppViewModel
import com.example.ui.viewmodel.Screen
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                val viewModel: AppViewModel by viewModels()
                val isLoggedIn by viewModel.isLoggedIn.collectAsState()
                val currentScreen by viewModel.currentScreen.collectAsState()

                if (!isLoggedIn) {
                    LoginScreen(viewModel = viewModel)
                } else {
                    AppMainLayout(viewModel = viewModel, currentScreen = currentScreen)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppMainLayout(viewModel: AppViewModel, currentScreen: Screen) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val activeRole by viewModel.userRole.collectAsState()
    val selectedProject by viewModel.selectedProject.collectAsState()

    val navigationItems = listOf(
        AppNavigationItem("แผงควบคุม (Dashboard)", Screen.Dashboard, Icons.Default.Dashboard),
        AppNavigationItem("วิเคราะห์ AI (Analysis)", Screen.Visualization, Icons.Default.AutoAwesome),
        AppNavigationItem("จัดการงาน (Kanban)", Screen.ProjectMgmt, Icons.Default.Task),
        AppNavigationItem("แชทบอท AI (Chat)", Screen.Chat, Icons.Default.Forum),
        AppNavigationItem("เล่มรายงาน (Reports)", Screen.Reports, Icons.Default.ReceiptLong),
        AppNavigationItem("สิทธิ์ผู้ใช้ (RBAC Matrix)", Screen.UserRoles, Icons.Default.Security),
        AppNavigationItem("ประวัติกิจกรรม (Audit)", Screen.AuditLogView, Icons.Default.Shield)
    )

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = Slate800,
                modifier = Modifier.width(300.dp)
            ) {
                // Drawer Header
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Slate900)
                        .padding(24.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Analytics,
                            contentDescription = null,
                            tint = PrimaryLightBlue,
                            modifier = Modifier.size(28.dp)
                        )
                        Text(
                            text = "Nexus Analyst",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Enterprise AI • $activeRole",
                        fontSize = 12.sp,
                        color = PrimaryLightBlue,
                        fontWeight = FontWeight.Bold
                    )

                    if (selectedProject != null) {
                        Text(
                            text = "โครงการ: " + selectedProject!!.name,
                            fontSize = 12.sp,
                            color = Slate500,
                            modifier = Modifier.padding(top = 4.dp),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Navigation Items List
                Column(modifier = Modifier.padding(horizontal = 12.dp)) {
                    navigationItems.forEach { item ->
                        val isSelected = currentScreen::class == item.screen::class
                        NavigationDrawerItem(
                            label = { Text(text = item.label, fontWeight = FontWeight.Bold, fontSize = 13.sp) },
                            selected = isSelected,
                            onClick = {
                                viewModel.navigateTo(item.screen)
                                scope.launch { drawerState.close() }
                            },
                            icon = { Icon(imageVector = item.icon, contentDescription = null, tint = if (isSelected) Slate900 else PrimaryLightBlue) },
                            colors = NavigationDrawerItemDefaults.colors(
                                selectedContainerColor = PrimaryLightBlue,
                                selectedTextColor = Slate900,
                                unselectedTextColor = Slate100,
                                unselectedIconColor = PrimaryLightBlue
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.weight(1f))
                    Divider(color = Slate700, modifier = Modifier.padding(vertical = 12.dp))

                    // Logout Button
                    NavigationDrawerItem(
                        label = { Text("ออกจากระบบ", fontWeight = FontWeight.Bold, fontSize = 13.sp) },
                        selected = false,
                        onClick = {
                            viewModel.logout()
                            scope.launch { drawerState.close() }
                        },
                        icon = { Icon(Icons.Default.ExitToApp, contentDescription = null, tint = AccentRose) },
                        colors = NavigationDrawerItemDefaults.colors(
                            unselectedTextColor = AccentRose,
                            unselectedIconColor = AccentRose
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                }
            }
        }
    ) {
        Box(modifier = Modifier.fillMaxSize().background(BentoBg)) {
            // Ambient Background Accents (Glowing Orbs)
            Box(
                modifier = Modifier
                    .offset(x = (-120).dp, y = (-120).dp)
                    .size(350.dp)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(BentoIndigo.copy(alpha = 0.25f), Color.Transparent)
                        )
                    )
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 120.dp, y = 120.dp)
                    .size(450.dp)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(BentoBlue.copy(alpha = 0.2f), Color.Transparent)
                        )
                    )
            )

            Scaffold(
                containerColor = Color.Transparent,
                topBar = {
                    CenterAlignedTopAppBar(
                        title = {
                            Text(
                                text = when (currentScreen) {
                                    is Screen.Dashboard -> "แดชบอร์ดสรุประบบ"
                                    is Screen.Visualization -> "ศูนย์วิเคราะห์โครงสร้าง AI"
                                    is Screen.ProjectMgmt -> "จัดการงาน & ความเสี่ยง"
                                    is Screen.Chat -> "ห้องปรึกษาผู้ช่วย AI"
                                    is Screen.Reports -> "ระบบรวบรวมรายงาน"
                                    is Screen.UserRoles -> "ตารางสิทธิ์การทำงาน"
                                    is Screen.AuditLogView -> "ระบบบันทึก Audit Trails"
                                    else -> "Nexus Analyst"
                                },
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = Color.White
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(Icons.Default.Menu, contentDescription = "Menu", tint = Color.White)
                            }
                        },
                        actions = {
                            Row(
                                modifier = Modifier.padding(end = 16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // API status indicator badge
                                val isApiAvailable = viewModel.isApiKeyAvailable
                                Box(
                                    modifier = Modifier
                                        .background(
                                            if (isApiAvailable) Color(0xFF103A2B) else Color(0xFF3E2B1F),
                                            RoundedCornerShape(8.dp)
                                        )
                                        .border(
                                            1.dp,
                                            if (isApiAvailable) Color(0xFF2E7D32) else Color(0xFFEF6C00),
                                            RoundedCornerShape(8.dp)
                                        )
                                        .padding(horizontal = 8.dp, vertical = 4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .background(
                                                    if (isApiAvailable) Color(0xFF4CAF50) else Color(0xFFFF9800),
                                                    CircleShape
                                                )
                                        )
                                        Text(
                                            text = if (isApiAvailable) "LIVE AI" else "MOCK AI",
                                            fontSize = 9.sp,
                                            color = if (isApiAvailable) Color(0xFF81C784) else Color(0xFFFFB74D),
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                // Role badge
                                Box(
                                    modifier = Modifier
                                        .border(1.dp, PrimaryLightBlue, CircleShape)
                                        .clickable { viewModel.navigateTo(Screen.UserRoles) }
                                        .padding(horizontal = 10.dp, vertical = 4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = activeRole.take(5) + "..",
                                        fontSize = 10.sp,
                                        color = PrimaryLightBlue,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        },
                        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                            containerColor = Color.Transparent, // Make Top Bar transparent too!
                            titleContentColor = Color.White
                        )
                    )
                }
            ) { innerPadding ->
            // High-grade animated screen switcher
            AnimatedContent(
                targetState = currentScreen,
                transitionSpec = {
                    fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(220))
                },
                label = "ScreenTransition"
            ) { screen ->
                when (screen) {
                    is Screen.Dashboard -> DashboardScreen(viewModel = viewModel, paddingValues = innerPadding)
                    is Screen.Visualization -> AnalysisScreen(viewModel = viewModel, paddingValues = innerPadding)
                    is Screen.ProjectMgmt -> ProjectMgmtScreen(viewModel = viewModel, paddingValues = innerPadding)
                    is Screen.Chat -> ChatScreen(viewModel = viewModel, paddingValues = innerPadding)
                    is Screen.Reports -> ReportScreen(viewModel = viewModel, paddingValues = innerPadding)
                    is Screen.UserRoles -> UserRolesScreen(viewModel = viewModel, paddingValues = innerPadding)
                    is Screen.AuditLogView -> AuditLogScreen(viewModel = viewModel, paddingValues = innerPadding)
                    else -> DashboardScreen(viewModel = viewModel, paddingValues = innerPadding)
                }
            }
        }
    }
}
}

data class AppNavigationItem(
    val label: String,
    val screen: Screen,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)
