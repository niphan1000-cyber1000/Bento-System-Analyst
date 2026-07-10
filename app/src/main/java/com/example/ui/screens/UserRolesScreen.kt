package com.aistudio.aisystemanalyst.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aistudio.aisystemanalyst.ui.theme.*
import com.aistudio.aisystemanalyst.ui.viewmodel.AppViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserRolesScreen(viewModel: AppViewModel, paddingValues: PaddingValues) {
    val activeRole by viewModel.userRole.collectAsState()

    val roles = listOf(
        Triple("Admin", "สิทธิ์ผู้ดูแลระบบสูงสุด (Full Control)", Icons.Default.SettingsSuggest),
        Triple("System Analyst", "นักวิเคราะห์ระบบและสถาปัตยกรรม (AI analyst)", Icons.Default.Engineering),
        Triple("Project Manager", "ผู้จัดการโครงการ (Product & Tasks)", Icons.Default.AssignmentInd),
        Triple("Developer", "นักพัฒนาโปรแกรม (Tasks & Code)", Icons.Default.Code),
        Triple("QA", "ผู้ตรวจสอบระบบทดสอบ (Testing & Bugs)", Icons.Default.FactCheck),
        Triple("Viewer", "ผู้สังเกตการณ์โครงการ (Read-Only)", Icons.Default.Visibility)
    )

    // Permission Matrix list
    val permissions = listOf(
        "เข้าถึงแผงควบคุมหลัก & สรุป Dashboard" to mapOf(
            "Admin" to true, "System Analyst" to true, "Project Manager" to true, "Developer" to true, "QA" to true, "Viewer" to true
        ),
        "สั่งวิเคราะห์ระบบเชิงเทคนิคด้วย AI (Gemini Analysis)" to mapOf(
            "Admin" to true, "System Analyst" to true, "Project Manager" to false, "Developer" to false, "QA" to false, "Viewer" to false
        ),
        "สร้าง และแก้ไขข้อมูลโครงการหรืออัปโหลดข้อกำหนด" to mapOf(
            "Admin" to true, "System Analyst" to true, "Project Manager" to true, "Developer" to false, "QA" to false, "Viewer" to false
        ),
        "สร้าง ลบ หรือย้ายขั้นตอนบอร์ดคัมบัง (Kanban Tasks)" to mapOf(
            "Admin" to true, "System Analyst" to true, "Project Manager" to true, "Developer" to true, "QA" to true, "Viewer" to false
        ),
        "เขียนคำถามถามตอบผ่าน AI Chatbot" to mapOf(
            "Admin" to true, "System Analyst" to true, "Project Manager" to true, "Developer" to true, "QA" to true, "Viewer" to true
        ),
        "เข้าตรวจสอบบันทึกความปลอดภัยประวัติการเข้าใช้งาน (Audit Logs)" to mapOf(
            "Admin" to true, "System Analyst" to false, "Project Manager" to false, "Developer" to false, "QA" to false, "Viewer" to false
        )
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)
            .padding(paddingValues)
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(imageVector = Icons.Default.Security, contentDescription = null, tint = PrimaryLightBlue, modifier = Modifier.size(24.dp))
                Text(text = "การจัดการบทบาทและสิทธิ์การใช้งาน (RBAC Matrix)", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
            Text(
                text = "เลือกจำลองบทบาทผู้ใช้งานเพื่อดูผลลัพธ์การอนุญาตและตรวจจับสิทธิ์ (Role-Based Access Control)",
                fontSize = 12.sp,
                color = Slate500,
                modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
            )
        }

        // Roles Selection Slider / Cards
        item {
            Text(text = "คลิกเพื่อสลับบทบาทของคุณ (Select Role):", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(modifier = Modifier.height(10.dp))

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                roles.forEach { (roleName, desc, icon) ->
                    val isActive = activeRole == roleName
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                1.dp,
                                if (isActive) PrimaryLightBlue else Slate700.copy(alpha = 0.5f),
                                RoundedCornerShape(12.dp)
                            )
                            .clickable { viewModel.changeUserRole(roleName) },
                        colors = CardDefaults.cardColors(
                            containerColor = if (isActive) Slate800 else Slate800.copy(alpha = 0.4f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(if (isActive) PrimaryLightBlue.copy(alpha = 0.2f) else Slate700, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(imageVector = icon, contentDescription = null, tint = if (isActive) PrimaryLightBlue else Color.White, modifier = Modifier.size(18.dp))
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = roleName, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                                Text(text = desc, color = Slate500, fontSize = 12.sp)
                            }

                            if (isActive) {
                                Box(
                                    modifier = Modifier
                                        .background(PrimaryLightBlue.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(text = "Active", color = PrimaryLightBlue, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Permissions Matrix Table Card
        item {
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = "ตารางแสดงขอบเขตสิทธิ์ของบทบาทปัจจุบัน (${activeRole})", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(modifier = Modifier.height(10.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Slate700.copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = Slate800),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    permissions.forEach { (perm, rolesMap) ->
                        val isAllowed = rolesMap[activeRole] ?: false
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = perm,
                                fontSize = 13.sp,
                                color = Slate100,
                                modifier = Modifier.weight(1f),
                                lineHeight = 18.sp
                            )

                            Spacer(modifier = Modifier.width(16.dp))

                            Box(
                                modifier = Modifier
                                    .background(
                                        if (isAllowed) AccentEmerald.copy(alpha = 0.15f) else AccentRose.copy(alpha = 0.15f),
                                        RoundedCornerShape(6.dp)
                                    )
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isAllowed) Icons.Default.CheckCircle else Icons.Default.Cancel,
                                        contentDescription = null,
                                        tint = if (isAllowed) AccentEmerald else AccentRose,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text(
                                        text = if (isAllowed) "อนุญาต" else "ไม่อนุญาต",
                                        color = if (isAllowed) AccentEmerald else AccentRose,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                        Divider(color = Slate700.copy(alpha = 0.4f))
                    }
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
