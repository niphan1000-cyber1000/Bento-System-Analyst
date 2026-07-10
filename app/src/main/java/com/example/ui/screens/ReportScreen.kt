package com.aistudio.aisystemanalyst.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aistudio.aisystemanalyst.ui.theme.*
import com.aistudio.aisystemanalyst.ui.viewmodel.AppViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportScreen(viewModel: AppViewModel, paddingValues: PaddingValues) {
    val selectedProject by viewModel.selectedProject.collectAsState()
    val scope = rememberCoroutineScope()

    var selectedDocType by remember { mutableStateOf("SRS") } // "SRS", "BRD", "API", "Arch"
    var isCompiling by remember { mutableStateOf(false) }
    var compileProgress by remember { mutableStateOf(0f) }
    var showSuccessToast by remember { mutableStateOf(false) }

    if (selectedProject == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Transparent)
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            Text("กรุณาเลือกหรืออัปโหลดโครงการเพื่อใช้ตัวสร้างรายงาน", color = Slate500, fontSize = 16.sp)
        }
        return
    }

    val project = selectedProject!!

    // Simulated Compiler function
    fun compileDocument() {
        scope.launch {
            isCompiling = true
            compileProgress = 0f
            viewModel.logActivity("Report Compilation", "เริ่มรวบรวมและสร้างเอกสาร $selectedDocType สำหรับ ${project.name}")
            while (compileProgress < 1f) {
                delay(150)
                compileProgress += 0.1f
            }
            isCompiling = false
            showSuccessToast = true
            viewModel.logActivity("Report Export", "นำออกรายงาน $selectedDocType สำเร็จเป็นรูปแบบ PDF/Word")
            viewModel.addNotification("คอมไพล์เอกสาร $selectedDocType สำเร็จแล้ว!")
            delay(3000)
            showSuccessToast = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)
            .padding(paddingValues)
    ) {
        // Document Selector Tabs
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Slate800.copy(alpha = 0.5f))
                .padding(vertical = 12.dp, horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            listOf("SRS", "BRD", "API Docs", "Technical Design").forEach { tab ->
                val active = selectedDocType == tab
                Box(
                    modifier = Modifier
                        .background(if (active) PrimaryLightBlue else Slate800, RoundedCornerShape(8.dp))
                        .clickable { selectedDocType = tab }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = tab,
                        color = if (active) Slate900 else Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
                .padding(horizontal = 20.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(16.dp))

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Slate700.copy(alpha = 0.3f), RoundedCornerShape(16.dp)),
                    colors = CardDefaults.cardColors(containerColor = Slate800),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "ตัวจัดการรวบรวมเอกสารอัตโนมัติ (Report Generator)",
                                    fontSize = 14.sp,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "รวบรวมข้อมูลจากการวิเคราะห์ AI เป็นเล่มเอกสารสากล",
                                    fontSize = 12.sp,
                                    color = Slate500,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }

                            Button(
                                onClick = { compileDocument() },
                                enabled = !isCompiling,
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryLightBlue, contentColor = Slate900)
                            ) {
                                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("คอมไพล์ & ส่งออก", fontWeight = FontWeight.Bold)
                            }
                        }

                        // Compilation Progress Bar
                        if (isCompiling) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(text = "กำลังจัดโครงสร้างและประกอบไฟล์...", fontSize = 12.sp, color = Slate100)
                                    Text(text = "${(compileProgress * 100).toInt()}%", fontSize = 12.sp, color = PrimaryLightBlue, fontWeight = FontWeight.Bold)
                                }
                                LinearProgressIndicator(
                                    progress = compileProgress,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp)),
                                    color = PrimaryLightBlue,
                                    trackColor = Slate700
                                )
                            }
                        }
                    }
                }
            }

            // Document Preview Paper Card
            item {
                Spacer(modifier = Modifier.height(20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "พรีวิวเล่มรายงาน $selectedDocType (Preview)", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Box(
                        modifier = Modifier
                            .background(Slate800, RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(text = "PDF / Word Format", color = Slate100, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Slate700, RoundedCornerShape(16.dp)),
                    colors = CardDefaults.cardColors(containerColor = Slate800.copy(alpha = 0.3f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(24.dp)
                            .fillMaxWidth()
                    ) {
                        // Title Header
                        Text(
                            text = when (selectedDocType) {
                                "SRS" -> "SOFTWARE REQUIREMENT SPECIFICATION"
                                "BRD" -> "BUSINESS REQUIREMENT DOCUMENT"
                                "API Docs" -> "ENTERPRISE API ENDPOINT BLUEPRINT"
                                else -> "TECHNICAL SYSTEM ARCHITECTURE & INTEGRATION DESIGN"
                            },
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 18.sp
                        )

                        Text(
                            text = "โครงการ: ${project.name} (${project.industry})",
                            fontSize = 13.sp,
                            color = PrimaryLightBlue,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                        )

                        Divider(color = Slate700, modifier = Modifier.padding(bottom = 16.dp))

                        // Render compiled mock sections based on document type
                        when (selectedDocType) {
                            "SRS" -> {
                                Text(text = "1. Introduction", fontWeight = FontWeight.Bold, color = AccentIndigo, fontSize = 14.sp)
                                Text(
                                    text = "เอกสารฉบับนี้กำหนดกรอบของโครงการ ${project.name} เพื่ออธิบายลักษณะความต้องการเชิงลึก โมดูลระบบ และข้อจำกัดเชิงเทคนิค ทั้งนี้เพื่อใช้เป็นต้นแบบในการพัฒนาแอปพลิเคชันให้ตรงสเปค",
                                    color = Slate100, fontSize = 13.sp, modifier = Modifier.padding(top = 6.dp, bottom = 14.dp)
                                )

                                Text(text = "2. Functional Requirements", fontWeight = FontWeight.Bold, color = AccentIndigo, fontSize = 14.sp)
                                Text(
                                    text = project.requirementsReport ?: "กรุณาสั่ง 'วิเคราะห์ AI' ในเมนูหลักเพื่อสร้างข้อกำหนดเชิงลึก",
                                    color = Slate100, fontSize = 13.sp, modifier = Modifier.padding(top = 6.dp, bottom = 14.dp)
                                )
                            }
                            "BRD" -> {
                                Text(text = "1. Executive Summary", fontWeight = FontWeight.Bold, color = AccentAmber, fontSize = 14.sp)
                                Text(
                                    text = "โครงการ ${project.name} ออกแบบมาเพื่อเพิ่มมูลค่าธุรกิจในด้าน ${project.industry} มุ่งเน้นการปฏิรูประบบงานผ่านการประมวลผลอัจฉริยะแบบ Real-time และลด Human Error ของกระบวนการทำงาน",
                                    color = Slate100, fontSize = 13.sp, modifier = Modifier.padding(top = 6.dp, bottom = 14.dp)
                                )

                                Text(text = "2. Stakeholders & Business Flows", fontWeight = FontWeight.Bold, color = AccentAmber, fontSize = 14.sp)
                                Text(
                                    text = project.businessReport ?: "กรุณาสั่ง 'วิเคราะห์ AI' ในหน้าแรกเพื่อระบุขั้นตอนกระบวนการทางธุรกิจ",
                                    color = Slate100, fontSize = 13.sp, modifier = Modifier.padding(top = 6.dp, bottom = 14.dp)
                                )
                            }
                            "API Docs" -> {
                                Text(text = "1. API Gateway Configuration & Access Rules", fontWeight = FontWeight.Bold, color = AccentEmerald, fontSize = 14.sp)
                                Text(
                                    text = "บังคับใช้โปรโตคอล HTTPS 100% ร่วมกับ JSON Web Token (JWT) และกำหนดให้เรียกผ่าน API Gateway เพื่อความมั่นคงปลอดภัย",
                                    color = Slate100, fontSize = 13.sp, modifier = Modifier.padding(top = 6.dp, bottom = 14.dp)
                                )

                                Text(text = "2. REST / GraphQL Endpoints Summary", fontWeight = FontWeight.Bold, color = AccentEmerald, fontSize = 14.sp)
                                Text(
                                    text = project.apiReport ?: "กรุณาสั่ง 'วิเคราะห์ AI' ในส่วนวิเคราะห์ APIs เพื่อประมวลผล endpoints และความคุ้มครองสิทธิ์",
                                    color = Slate100, fontSize = 13.sp, modifier = Modifier.padding(top = 6.dp, bottom = 14.dp)
                                )
                            }
                            else -> {
                                Text(text = "1. Cloud Architecture Specifications", fontWeight = FontWeight.Bold, color = PrimaryLightBlue, fontSize = 14.sp)
                                Text(
                                    text = project.infraReport ?: "กรุณาสั่ง 'วิเคราะห์ AI' ในส่วนคลาวด์เพื่อออกแบบโครงสร้าง AWS / GCP Topology",
                                    color = Slate100, fontSize = 13.sp, modifier = Modifier.padding(top = 6.dp, bottom = 14.dp)
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    // Export Success Toast Message overlay
    AnimatedVisibility(
        visible = showSuccessToast,
        enter = fadeIn() + slideInVertically(),
        exit = fadeOut() + slideOutVertically()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = AccentEmerald),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(text = "ส่งออกเอกสารสำเร็จ! บันทึกที่โฟลเดอร์ Downloads เรียบร้อย", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
    }
}
