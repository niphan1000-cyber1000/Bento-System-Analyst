package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Project
import com.example.ui.theme.*
import com.example.ui.viewmodel.AppViewModel
import com.example.ui.viewmodel.Screen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(viewModel: AppViewModel, paddingValues: PaddingValues) {
    val projects by viewModel.projects.collectAsState()
    val selectedProject by viewModel.selectedProject.collectAsState()
    val tasks by viewModel.projectTasks.collectAsState()
    val risks by viewModel.projectRisks.collectAsState()
    val role by viewModel.userRole.collectAsState()
    val notifications by viewModel.notifications.collectAsState()

    var showUploadDialog by remember { mutableStateOf(false) }
    var newProjName by remember { mutableStateOf("") }
    var newProjIndustry by remember { mutableStateOf("FinTech") }
    var newProjDesc by remember { mutableStateOf("") }
    var newProjContent by remember { mutableStateOf("") }

    val industries = listOf("FinTech", "E-Commerce", "HealthCare", "Logistics", "SaaS / Cloud")

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)
            .padding(paddingValues)
            .padding(horizontal = 20.dp)
    ) {
        // Welcoming Hero Header Card
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BentoCardBorder, RoundedCornerShape(24.dp)),
                colors = CardDefaults.cardColors(containerColor = BentoCardBg),
                shape = RoundedCornerShape(24.dp)
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    // Decorative radial blur background
                    Box(
                        modifier = Modifier
                            .size(250.dp)
                            .align(Alignment.TopEnd)
                            .background(Brush.radialGradient(listOf(PrimaryLightBlue.copy(alpha = 0.15f), Color.Transparent)))
                    )

                    Column(modifier = Modifier.padding(24.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(PrimaryLightBlue.copy(alpha = 0.2f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Analytics, contentDescription = null, tint = PrimaryLightBlue, modifier = Modifier.size(26.dp))
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(
                                    text = "สวัสดีตอนบ่าย, บทบาท: $role",
                                    fontSize = 14.sp,
                                    color = Slate500,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "AI System Analyst Platform",
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "ยินดีต้อนรับสู่แผงควบคุมหลักระดับองค์กร อัปโหลดข้อกำหนดและวิเคราะห์สเปคโมเดลและโครงสร้างสถาปัตยกรรมด้วยพลังของ Gemini LLM",
                            fontSize = 14.sp,
                            color = Slate100,
                            lineHeight = 20.sp
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        Row(modifier = Modifier.fillMaxWidth()) {
                            Button(
                                onClick = { showUploadDialog = true },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryLightBlue, contentColor = Slate900)
                            ) {
                                Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("อัปโหลดเอกสารใหม่", fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            OutlinedButton(
                                onClick = { viewModel.navigateTo(Screen.Chat) },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Slate100),
                                border = BorderStroke(1.dp, Slate700)
                            ) {
                                Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(18.dp), tint = PrimaryLightBlue)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("คุยกับ AI Assistant")
                            }
                        }
                    }
                }
            }
        }

        // Project Selection Dropdown / List
        item {
            Spacer(modifier = Modifier.height(24.dp))
            Text(text = "โครงการที่กำลังดำเนินการ (Active Projects)", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(modifier = Modifier.height(12.dp))

            if (projects.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                        .background(Slate800, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("ไม่มีโครงการในระบบ อัปโหลดโครงการด้านบน", color = Slate500)
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    projects.forEach { project ->
                        val isSelected = selectedProject?.id == project.id
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { viewModel.selectProject(project) }
                                .border(
                                    1.dp,
                                    if (isSelected) BentoIndigo else BentoCardBorder,
                                    RoundedCornerShape(20.dp)
                                ),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) BentoCardBg.copy(alpha = 0.25f) else BentoCardBg
                            )
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = project.industry,
                                    color = if (isSelected) PrimaryLightBlue else Slate500,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = project.name,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = project.description,
                                    color = Slate100,
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }

        // Enterprise AI Security & Gateway Routing Configurator Card
        item {
            Spacer(modifier = Modifier.height(24.dp))
            val useEnterpriseGateway by viewModel.useEnterpriseGateway.collectAsState()
            val enterpriseGatewayUrl by viewModel.enterpriseGatewayUrl.collectAsState()
            var gatewayInput by remember(enterpriseGatewayUrl) { mutableStateOf(enterpriseGatewayUrl) }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, if (useEnterpriseGateway) BentoIndigo else BentoCardBorder, RoundedCornerShape(20.dp)),
                colors = CardDefaults.cardColors(containerColor = BentoCardBg),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (useEnterpriseGateway) Icons.Default.Shield else Icons.Default.CloudQueue,
                                contentDescription = null,
                                tint = if (useEnterpriseGateway) AccentEmerald else PrimaryLightBlue,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "เส้นทางการเรียก AI (Enterprise Routing)",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = if (useEnterpriseGateway) "เกตเวย์องค์กร (Secure Backend Proxy)" else "เชื่อมตรง (Client-to-Gemini SDK Prototyping)",
                                    fontSize = 12.sp,
                                    color = if (useEnterpriseGateway) AccentEmerald else Slate500
                                )
                            }
                        }
                        
                        Switch(
                            checked = useEnterpriseGateway,
                            onCheckedChange = { viewModel.setUseEnterpriseGateway(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = AccentIndigo,
                                uncheckedThumbColor = Slate500,
                                uncheckedTrackColor = Slate800
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (useEnterpriseGateway) {
                        Text(
                            text = "ความปลอดภัยระดับสูง: คีย์ API จะถูกเก็บรักษาไว้อย่างปลอดภัยบนฝั่งเซิร์ฟเวอร์หลังบ้าน (FastAPI/Spring) ตัวแอปจะเรียก AI ผ่าน Gateway เท่านั้น",
                            fontSize = 11.sp,
                            color = Slate100,
                            lineHeight = 16.sp
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = gatewayInput,
                                onValueChange = {
                                    gatewayInput = it
                                    viewModel.setEnterpriseGatewayUrl(it)
                                },
                                label = { Text("ที่อยู่ API Gateway (Backend URL)", fontSize = 11.sp) },
                                singleLine = true,
                                textStyle = LocalTextStyle.current.copy(fontSize = 12.sp, color = Color.White),
                                modifier = Modifier.weight(1f),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = PrimaryLightBlue,
                                    unfocusedBorderColor = Slate700,
                                    focusedLabelColor = PrimaryLightBlue,
                                    unfocusedLabelColor = Slate500
                                )
                            )
                        }
                    } else {
                        Text(
                            text = "โหมดแซนด์บอกซ์ (Sandbox Prototyping): แอปพลิเคชันส่งคำขอไปยัง Gemini API โดยตรงผ่าน Client SDK (คีย์ API ถูกเก็บในแอสเซทจำลอง / ค่ากำหนดท้องถิ่น)",
                            fontSize = 11.sp,
                            color = Slate500,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }

        // Dynamic KPI Card Grid for Selected Project
        item {
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "ดัชนีชี้วัดสเปคระบบ: ${selectedProject?.name ?: "ยังไม่ได้เลือกโครงการ"}",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(12.dp))

            // Simulated counts that represent robust specs
            val hasRequirements = selectedProject?.requirementsReport != null
            val hasDatabase = selectedProject?.databaseReport != null
            val hasApi = selectedProject?.apiReport != null

            val requirementsCount = if (hasRequirements) 18 else 6
            val userStoriesCount = if (hasRequirements) 24 else 8
            val modulesCount = if (selectedProject?.systemReport != null) 7 else 3
            val apiCount = if (hasApi) 16 else 4
            val tablesCount = if (hasDatabase) 11 else 4
            val externalServices = if (hasApi) 4 else 1
            val risksCount = risks.size.coerceAtLeast(if (selectedProject?.riskReport != null) 5 else 2)
            val bugsCount = if (selectedProject?.codeReport != null) 1 else 3
            val testCases = if (selectedProject?.codeReport != null) 32 else 8

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    KpiCard(title = "ความต้องการ (Requirement)", value = requirementsCount.toString(), icon = Icons.Default.FormatListBulleted, tint = PrimaryLightBlue, modifier = Modifier.weight(1f))
                    KpiCard(title = "ผู้ใช้จริง (User Story)", value = userStoriesCount.toString(), icon = Icons.Default.Group, tint = AccentIndigo, modifier = Modifier.weight(1f))
                    KpiCard(title = "โมดูลระบบ (Module)", value = modulesCount.toString(), icon = Icons.Default.Layers, tint = AccentAmber, modifier = Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    KpiCard(title = "ส่วนเชื่อมต่อ (API)", value = apiCount.toString(), icon = Icons.Default.Http, tint = AccentEmerald, modifier = Modifier.weight(1f))
                    KpiCard(title = "ตาราง (Database Table)", value = tablesCount.toString(), icon = Icons.Default.Storage, tint = PrimaryLightBlue, modifier = Modifier.weight(1f))
                    KpiCard(title = "บริการภายนอก (Service)", value = externalServices.toString(), icon = Icons.Default.CloudSync, tint = AccentIndigo, modifier = Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    KpiCard(title = "ความเสี่ยง (Risk)", value = risksCount.toString(), icon = Icons.Default.Warning, tint = AccentRose, modifier = Modifier.weight(1f))
                    KpiCard(title = "บั๊ก / ข้อบกพร่อง (Bug)", value = bugsCount.toString(), icon = Icons.Default.BugReport, tint = AccentRose, modifier = Modifier.weight(1f))
                    KpiCard(title = "เคสทดสอบ (Test Case)", value = testCases.toString(), icon = Icons.Default.FactCheck, tint = AccentEmerald, modifier = Modifier.weight(1f))
                }
            }
        }

        // Custom Metric Bar Chart / Interactive Dashboard Analytics
        item {
            Spacer(modifier = Modifier.height(28.dp))
            Text(text = "สรุปปริมาณสเปคระบบเชิงเปรียบเทียบ", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(modifier = Modifier.height(12.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BentoCardBorder, RoundedCornerShape(24.dp)),
                colors = CardDefaults.cardColors(containerColor = BentoCardBg),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(text = "สัดส่วนปริมาณงานแยกตามหมวดหมู่", fontSize = 13.sp, color = Slate500)
                    Spacer(modifier = Modifier.height(16.dp))

                    val metricList = listOf(
                        Triple("Reqs", 0.7f, PrimaryLightBlue),
                        Triple("Stories", 0.95f, AccentIndigo),
                        Triple("APIs", 0.55f, AccentEmerald),
                        Triple("Tables", 0.45f, AccentAmber),
                        Triple("Risks", 0.25f, AccentRose)
                    )

                    metricList.forEach { (label, progress, color) ->
                        Column(modifier = Modifier.padding(vertical = 8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = label, color = Slate100, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Text(text = "${(progress * 30).toInt()} Items", color = color, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            LinearProgressIndicator(
                                progress = progress,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = color,
                                trackColor = Slate700
                            )
                        }
                    }
                }
            }
        }

        // Real-time Activity and Notification list
        item {
            Spacer(modifier = Modifier.height(28.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "การแจ้งเตือนและกิจกรรมในระบบ (Notifications)", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text(
                    text = "ประวัติกิจกรรม",
                    color = PrimaryLightBlue,
                    fontSize = 13.sp,
                    modifier = Modifier.clickable { viewModel.navigateTo(Screen.AuditLogView) }
                )
            }
            Spacer(modifier = Modifier.height(12.dp))

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                notifications.take(3).forEach { notification ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(BentoCardBg, RoundedCornerShape(16.dp))
                            .border(1.dp, BentoCardBorder, RoundedCornerShape(16.dp))
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(PrimaryLightBlue, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = notification,
                            color = Slate100,
                            fontSize = 13.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // Upload / Input Dialog
    if (showUploadDialog) {
        AlertDialog(
            onDismissRequest = { showUploadDialog = false },
            title = {
                Text(text = "อัปโหลดเอกสาร / ข้อกำหนดโครงการ", fontWeight = FontWeight.Bold, color = Color.White)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = newProjName,
                        onValueChange = { newProjName = it },
                        label = { Text("ชื่อโครงการ (Project Name)") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White)
                    )

                    // Industry Selector
                    Text(text = "ประเภทอุตสาหกรรม (Industry)", fontSize = 12.sp, color = Slate500)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        industries.take(3).forEach { ind ->
                            val isSel = newProjIndustry == ind
                            Box(
                                modifier = Modifier
                                    .background(if (isSel) PrimaryLightBlue else Slate800, RoundedCornerShape(8.dp))
                                    .clickable { newProjIndustry = ind }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(text = ind, color = if (isSel) Slate900 else Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    OutlinedTextField(
                        value = newProjDesc,
                        onValueChange = { newProjDesc = it },
                        label = { Text("คำอธิบายสั้นๆ") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White)
                    )

                    OutlinedTextField(
                        value = newProjContent,
                        onValueChange = { newProjContent = it },
                        label = { Text("สเปค/โค้ด หรือ OpenAPI สคีมาแบบละเอียด") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White),
                        maxLines = 8
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newProjName.isNotEmpty()) {
                            viewModel.uploadNewProject(
                                name = newProjName,
                                industry = newProjIndustry,
                                description = newProjDesc,
                                rawContent = newProjContent
                            )
                            showUploadDialog = false
                            // Reset
                            newProjName = ""
                            newProjDesc = ""
                            newProjContent = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryLightBlue, contentColor = Slate900)
                ) {
                    Text("เริ่มวิเคราะห์ทันที")
                }
            },
            dismissButton = {
                TextButton(onClick = { showUploadDialog = false }) {
                    Text("ยกเลิก", color = Slate500)
                }
            },
            containerColor = Slate800,
            shape = RoundedCornerShape(18.dp)
        )
    }
}

@Composable
fun KpiCard(
    title: String,
    value: String,
    icon: ImageVector,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .border(1.dp, BentoCardBorder, RoundedCornerShape(20.dp)),
        colors = CardDefaults.cardColors(containerColor = BentoCardBg),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = title, fontSize = 11.sp, color = Slate500, fontWeight = FontWeight.SemiBold)
                Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(16.dp))
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = value, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
    }
}
