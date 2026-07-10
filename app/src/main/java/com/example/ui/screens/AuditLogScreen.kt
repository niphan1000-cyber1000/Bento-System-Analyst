package com.aistudio.aisystemanalyst.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aistudio.aisystemanalyst.ui.theme.AccentIndigo
import com.aistudio.aisystemanalyst.ui.theme.AccentRose
import com.aistudio.aisystemanalyst.ui.theme.BentoCardBg
import com.aistudio.aisystemanalyst.ui.theme.BentoCardBorder
import com.aistudio.aisystemanalyst.ui.theme.BentoIndigo
import com.aistudio.aisystemanalyst.ui.theme.PrimaryLightBlue
import com.aistudio.aisystemanalyst.ui.theme.Slate100
import com.aistudio.aisystemanalyst.ui.theme.Slate500
import com.aistudio.aisystemanalyst.ui.theme.Slate700
import com.aistudio.aisystemanalyst.ui.theme.Slate800
import com.aistudio.aisystemanalyst.ui.theme.Slate900
import com.aistudio.aisystemanalyst.ui.viewmodel.AppViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AuditLogScreen(viewModel: AppViewModel, paddingValues: PaddingValues) {
    val auditLogs by viewModel.auditLogs.collectAsState()
    val crashes by viewModel.systemCrashes.collectAsState()
    val metrics by viewModel.performanceMetrics.collectAsState()

    var selectedTab by remember { mutableStateOf(0) }
    val tabTitles = listOf("บันทึกการทำงาน (Audit Trails)", "มอนิเตอร์ & บันทึกข้อผิดพลาด (Telemetry & Crashes)")

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)
            .padding(paddingValues)
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(imageVector = Icons.Default.Shield, contentDescription = null, tint = PrimaryLightBlue, modifier = Modifier.size(24.dp))
                Text(text = "การตรวจสอบความปลอดภัยของระบบ (Security & Telemetry)", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
            Text(
                text = "ประวัติการเรียกวิเคราะห์สถาปัตยกรรม การเข้าถึงสิทธิ์ (RBAC) และระบบรายงานปัญหาแอปพลิเคชันแบบเรียลไทม์",
                fontSize = 12.sp,
                color = Slate500,
                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
            )

            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Slate900,
                contentColor = PrimaryLightBlue,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = PrimaryLightBlue
                    )
                },
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                tabTitles.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(text = title, fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                    )
                }
            }
        }

        if (selectedTab == 0) {
            // Audit Log View
            if (auditLogs.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .background(Slate800, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "ยังไม่มีประวัติการทำงานบันทึกไว้ในสคีมาฐานข้อมูล", color = Slate500)
                    }
                }
            } else {
                items(auditLogs) { log ->
                    val date = Date(log.timestamp)
                    val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm:ss", Locale.getDefault())
                    val formattedDate = sdf.format(date)

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Slate700.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
                        colors = CardDefaults.cardColors(containerColor = Slate800.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "กิจกรรม: " + log.action,
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryLightBlue,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = log.userRole,
                                    color = Slate500,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            Text(text = log.details, color = Slate100, fontSize = 13.sp)
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Default.History, contentDescription = null, tint = Slate500, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(text = formattedDate, color = Slate500, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        } else {
            // Telemetry & Crash Monitor View
            item {
                Text(
                    text = "สถานะทรัพยากรระบบของแอปพลิเคชัน (System Telemetry)",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(8.dp))

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, BentoCardBorder, RoundedCornerShape(16.dp)),
                    colors = CardDefaults.cardColors(containerColor = BentoCardBg),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                                Icon(Icons.Default.Memory, contentDescription = null, tint = PrimaryLightBlue)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("หน่วยความจำ (RAM)", fontSize = 11.sp, color = Slate500)
                                Text("${metrics.usedMemoryMb}/${metrics.totalMemoryMb} MB", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                Text("Max: ${metrics.maxMemoryMb} MB", fontSize = 10.sp, color = Slate500)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                                Icon(Icons.Default.BugReport, contentDescription = null, tint = AccentIndigo)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("เธรดที่ทำงาน", fontSize = 11.sp, color = Slate500)
                                Text("${metrics.activeThreads} Threads", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                                Icon(Icons.Default.Router, contentDescription = null, tint = if (metrics.networkLatencyMs != -1L) Color(0xFF34D399) else AccentRose)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("เกตเวย์ปิง (Ping)", fontSize = 11.sp, color = Slate500)
                                Text(
                                    text = if (metrics.networkLatencyMs != -1L) "${metrics.networkLatencyMs} ms" else "Timeout",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (metrics.networkLatencyMs != -1L) Color(0xFF34D399) else AccentRose
                                )
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "บันทึกและรวบรวมข้อผิดพลาด (Crash Reports: ${crashes.size})",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { viewModel.clearCrashReports() },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentRose),
                            border = androidx.compose.foundation.BorderStroke(1.dp, AccentRose.copy(alpha = 0.5f)),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("ล้างประวัติ", fontSize = 11.sp)
                        }

                        Button(
                            onClick = { viewModel.triggerSimulatedCrash("Enterprise Sandbox exception triggered by user.") },
                            colors = ButtonDefaults.buttonColors(containerColor = AccentRose, contentColor = Color.White),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("จำลองแครช", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            if (crashes.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .background(Slate800, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "แอปพลิเคชันทำงานได้สมบูรณ์แบบ 100%: ไม่มีบันทึกการแครช", color = Slate500, fontSize = 13.sp)
                    }
                }
            } else {
                items(crashes) { crash ->
                    val crashDate = Date(crash.timestamp)
                    val sdf = SimpleDateFormat("dd MMM, HH:mm:ss", Locale.getDefault())
                    val formattedTime = sdf.format(crashDate)

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, AccentRose.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
                        colors = CardDefaults.cardColors(containerColor = Slate800.copy(alpha = 0.6f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = crash.exceptionName,
                                    color = AccentRose,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = formattedTime,
                                    color = Slate500,
                                    fontSize = 11.sp
                                )
                            }
                            Text(
                                text = "ข้อความ: ${crash.errorMessage}",
                                fontSize = 12.sp,
                                color = Color.White,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "อุปกรณ์: ${crash.deviceModel} (${crash.androidVersion})",
                                fontSize = 10.sp,
                                color = Slate500
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = crash.stackTrace,
                                fontSize = 10.sp,
                                color = Slate100,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Slate900, RoundedCornerShape(6.dp))
                                    .padding(8.dp),
                                maxLines = 10
                            )
                        }
                    }
                }
            }
        }
    }
}
