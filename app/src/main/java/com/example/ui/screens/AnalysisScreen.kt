package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Project
import com.example.ui.theme.*
import com.example.ui.viewmodel.AppViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalysisScreen(viewModel: AppViewModel, paddingValues: PaddingValues) {
    val selectedProject by viewModel.selectedProject.collectAsState()
    val analysisStatus by viewModel.analysisStatus.collectAsState()

    val categories = listOf(
        "Requirements" to "ข้อกำหนดระบบ",
        "Business" to "กระบวนการธุรกิจ",
        "System" to "โมดูล & ระบบ",
        "Database" to "ฐานข้อมูล (ERD)",
        "API" to "ส่วนต่อประสาน (API)",
        "Security" to "ความปลอดภัย (OWASP)",
        "Performance" to "ประสิทธิภาพ",
        "Code" to "รีวิวโค้ดสากล",
        "Infrastructure" to "เน็ตเวิร์ก & คลาวด์",
        "Risk" to "วิเคราะห์ความเสี่ยง"
    )

    var activeTab by remember { mutableStateOf("Requirements") }

    if (selectedProject == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Transparent)
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            Text("กรุณาเลือกหรืออัปโหลดโครงการเพื่อเริ่มการวิเคราะห์", color = Slate500, fontSize = 16.sp)
        }
        return
    }

    val currentProject = selectedProject!!

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)
            .padding(paddingValues)
    ) {
        // Horizontal Scrollable Analysis Tabs
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .background(Slate800.copy(alpha = 0.5f))
                .padding(vertical = 12.dp, horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(categories) { (key, label) ->
                val isActive = activeTab == key
                val status = analysisStatus[key] ?: "Idle"
                val isDone = getReportTextForCategory(currentProject, key) != null

                Box(
                    modifier = Modifier
                        .background(
                            if (isActive) PrimaryLightBlue else if (isDone) Slate800 else Slate800.copy(alpha = 0.5f),
                            RoundedCornerShape(10.dp)
                        )
                        .border(
                            1.dp,
                            if (isActive) Color.Transparent else if (isDone) AccentEmerald.copy(alpha = 0.4f) else Slate700,
                            RoundedCornerShape(10.dp)
                        )
                        .clickable { activeTab = key }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = label,
                            color = if (isActive) Slate900 else Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        if (status == "Running") {
                            CircularProgressIndicator(
                                modifier = Modifier.size(12.dp),
                                strokeWidth = 2.dp,
                                color = if (isActive) Slate900 else PrimaryLightBlue
                            )
                        } else if (isDone) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Completed",
                                tint = if (isActive) Slate900 else AccentEmerald,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }
        }

        // Selected Report Details
        val reportContent = getReportTextForCategory(currentProject, activeTab)
        val status = analysisStatus[activeTab] ?: "Idle"

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
                                    text = "สถานะการวิเคราะห์ด้าน ${categories.find { it.first == activeTab }?.second}",
                                    fontSize = 13.sp,
                                    color = Slate500,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = when (status) {
                                        "Running" -> "กำลังคุยกับ Gemini เพื่อวิเคราะห์..."
                                        "Completed" -> "วิเคราะห์สำเร็จพร้อมรายงานเชิงลึก"
                                        "Failed" -> "เกิดข้อผิดพลาดในการวิเคราะห์"
                                        else -> if (reportContent != null) "รายงานถูกวิเคราะห์เรียบร้อย" else "พร้อมส่งข้อกำหนดไปวิเคราะห์"
                                    },
                                    fontSize = 15.sp,
                                    color = if (status == "Running") PrimaryLightBlue else if (reportContent != null) AccentEmerald else Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Button(
                                onClick = { viewModel.runAnalysisForCategory(activeTab) },
                                enabled = status != "Running",
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (reportContent != null) AccentIndigo else PrimaryLightBlue,
                                    contentColor = if (reportContent != null) Color.White else Slate900
                                )
                            ) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (reportContent != null) "วิเคราะห์ใหม่อีกครั้ง" else "เริ่มวิเคราะห์ AI")
                            }
                        }
                    }
                }
            }

            // Interactive Diagrams if report contains schema tags
            if (activeTab == "Database" && reportContent != null) {
                val tables = parseTablesSchema(reportContent)
                if (tables.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(20.dp))
                        Text(text = "แฝงควบคุมแผนภาพ ER Diagram แบบโต้ตอบ", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Spacer(modifier = Modifier.height(10.dp))
                        InteractiveErDiagram(tables = tables)
                    }
                }
            }

            if (activeTab == "Infrastructure" && reportContent != null) {
                val nodes = parseInfraSchema(reportContent)
                if (nodes.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(20.dp))
                        Text(text = "แผนภาพโครงสร้างระบบคลาวด์อัจฉริยะ (Network Topology)", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Spacer(modifier = Modifier.height(10.dp))
                        InteractiveInfraDiagram(connections = nodes)
                    }
                }
            }

            // Render Markdown Report
            item {
                Spacer(modifier = Modifier.height(20.dp))
                if (reportContent != null) {
                    Text(text = "รายงานวิเคราะห์จาก AI", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Spacer(modifier = Modifier.height(12.dp))
                    MarkdownReportCard(text = cleanSchemaTags(reportContent))
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .background(Slate800.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                            .border(1.dp, Slate700.copy(alpha = 0.2f), RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Analytics, contentDescription = null, tint = Slate500, modifier = Modifier.size(40.dp))
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("ยังไม่มีรายงานด้านนี้ กดปุ่ม 'เริ่มวิเคราะห์ AI' ด้านบน", color = Slate500, fontSize = 14.sp)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

fun getReportTextForCategory(project: Project, category: String): String? {
    return when (category) {
        "Requirements" -> project.requirementsReport
        "Business" -> project.businessReport
        "System" -> project.systemReport
        "Database" -> project.databaseReport
        "API" -> project.apiReport
        "Security" -> project.securityReport
        "Performance" -> project.performanceReport
        "Code" -> project.codeReport
        "Infrastructure" -> project.infraReport
        "Risk" -> project.riskReport
        else -> null
    }
}

// Custom Markdown Renderer Card
@Composable
fun MarkdownReportCard(text: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Slate700.copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = Slate800.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            val lines = text.split("\n")
            lines.forEach { line ->
                val trimmed = line.trim()
                when {
                    trimmed.startsWith("###") -> {
                        Text(
                            text = trimmed.replace("###", "").trim(),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryLightBlue,
                            modifier = Modifier.padding(top = 14.dp, bottom = 6.dp)
                        )
                    }
                    trimmed.startsWith("##") -> {
                        Text(
                            text = trimmed.replace("##", "").trim(),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = AccentIndigo,
                            modifier = Modifier.padding(top = 18.dp, bottom = 8.dp)
                        )
                    }
                    trimmed.startsWith("#") -> {
                        Text(
                            text = trimmed.replace("#", "").trim(),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(top = 22.dp, bottom = 12.dp)
                        )
                    }
                    trimmed.startsWith("-") || trimmed.startsWith("*") -> {
                        Row(modifier = Modifier.padding(vertical = 4.dp)) {
                            Text(text = " • ", color = PrimaryLightBlue, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(
                                text = trimmed.substring(1).trim(),
                                color = Slate100,
                                fontSize = 14.sp,
                                lineHeight = 20.sp
                            )
                        }
                    }
                    trimmed.isNotEmpty() -> {
                        Text(
                            text = trimmed,
                            color = Slate100,
                            fontSize = 14.sp,
                            lineHeight = 22.sp,
                            modifier = Modifier.padding(vertical = 6.dp)
                        )
                    }
                }
            }
        }
    }
}

// DB ERD Model structures
data class DbTable(
    val name: String,
    val columns: List<String>
)

// Parse DB Schema tags from reports
fun parseTablesSchema(report: String): List<DbTable> {
    val tables = mutableListOf<DbTable>()
    try {
        if (!report.contains("[TABLES_SCHEMA]")) return emptyList()
        val schemaBlock = report.substringAfter("[TABLES_SCHEMA]").substringBefore("[/TABLES_SCHEMA]")
        val sections = schemaBlock.trim().split("Table:")
        sections.forEach { section ->
            if (section.trim().isEmpty()) return@forEach
            val lines = section.trim().split("\n")
            val tableName = lines.firstOrNull()?.trim() ?: "Unknown"
            val columns = lines.drop(1).map { it.trim().replace("-", "").trim() }.filter { it.isNotEmpty() }
            tables.add(DbTable(tableName, columns))
        }
    } catch (e: Exception) {
        // Safe bypass
    }
    return tables
}

// Parse Infra tags from reports
fun parseInfraSchema(report: String): List<Pair<String, String>> {
    val connections = mutableListOf<Pair<String, String>>()
    try {
        if (!report.contains("[INFRA_SCHEMA]")) return emptyList()
        val block = report.substringAfter("[INFRA_SCHEMA]").substringBefore("[/INFRA_SCHEMA]")
        block.trim().split("\n").forEach { line ->
            if (line.contains("->")) {
                val nodes = line.replace("Node:", "").split("->")
                if (nodes.size >= 2) {
                    val from = nodes[0].trim().substringBefore("(")
                    val to = nodes[1].trim().substringBefore("(")
                    connections.add(from to to)
                }
            }
        }
    } catch (e: Exception) {
        // Safe bypass
    }
    return connections
}

fun cleanSchemaTags(report: String): String {
    return report
        .replace("[TABLES_SCHEMA]", "")
        .replace("[/TABLES_SCHEMA]", "")
        .replace("[INFRA_SCHEMA]", "")
        .replace("[/INFRA_SCHEMA]", "")
}

// Beautiful Interactive ERD Component using Canvas and Box
@Composable
fun InteractiveErDiagram(tables: List<DbTable>) {
    var zoomedTable by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp)
            .background(Slate800.copy(alpha = 0.8f), RoundedCornerShape(16.dp))
            .border(1.dp, Slate700, RoundedCornerShape(16.dp))
            .horizontalScroll(rememberScrollState())
            .verticalScroll(rememberScrollState())
    ) {
        // Render Tables side-by-side with nice connections drawn in between
        Canvas(modifier = Modifier.matchParentSize()) {
            // Underlay connection line drawing
            // Draw standard dashed relationship lines in Cyan
            val pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
            drawCircle(color = PrimaryLightBlue.copy(alpha = 0.2f), radius = 200f, center = Offset(size.width / 2, size.height / 2))
        }

        Row(
            modifier = Modifier.padding(24.dp),
            horizontalArrangement = Arrangement.spacedBy(40.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            tables.forEach { table ->
                val isZoomed = zoomedTable == table.name
                Card(
                    modifier = Modifier
                        .width(if (isZoomed) 220.dp else 180.dp)
                        .border(
                            1.dp,
                            if (isZoomed) AccentEmerald else Slate700,
                            RoundedCornerShape(12.dp)
                        )
                        .clickable { zoomedTable = if (isZoomed) null else table.name },
                    colors = CardDefaults.cardColors(containerColor = Slate900.copy(alpha = 0.9f))
                ) {
                    Column {
                        // Header
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(if (isZoomed) AccentEmerald else PrimaryLightBlue.copy(alpha = 0.2f))
                                .padding(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = table.name,
                                    color = if (isZoomed) Slate900 else Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                Icon(
                                    imageVector = Icons.Default.Storage,
                                    contentDescription = null,
                                    tint = if (isZoomed) Slate900 else PrimaryLightBlue,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }

                        // Columns
                        Column(modifier = Modifier.padding(12.dp)) {
                            table.columns.forEach { col ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val isPk = col.contains("PK") || col.contains("id")
                                    val isFk = col.contains("FK")
                                    Text(
                                        text = col.substringBefore("(").trim(),
                                        color = if (isPk) AccentAmber else Slate100,
                                        fontSize = 12.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Text(
                                        text = if (isPk) "PK" else if (isFk) "FK" else "INT",
                                        color = if (isPk) AccentAmber else if (isFk) PrimaryLightBlue else Slate500,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// Beautiful Interactive Infrastructure Component
@Composable
fun InteractiveInfraDiagram(connections: List<Pair<String, String>>) {
    val infiniteTransition = rememberInfiniteTransition(label = "inf")
    val animOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 100f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "animOffset"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp)
            .background(Slate800.copy(alpha = 0.8f), RoundedCornerShape(16.dp))
            .border(1.dp, Slate700, RoundedCornerShape(16.dp))
            .horizontalScroll(rememberScrollState())
            .verticalScroll(rememberScrollState()),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            // Draw flowing pipeline background connection paths
            connections.forEachIndexed { i, _ ->
                val start = Offset(80f + i * 220f, 150f)
                val end = Offset(80f + (i + 1) * 220f, 150f)
                drawLine(
                    color = PrimaryLightBlue.copy(alpha = 0.3f),
                    start = start,
                    end = end,
                    strokeWidth = 3f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 15f), animOffset)
                )
            }
        }

        Row(
            modifier = Modifier.padding(24.dp),
            horizontalArrangement = Arrangement.spacedBy(50.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Render unique node items
            val uniqueNodes = (connections.map { it.first } + connections.map { it.second }).distinct()
            uniqueNodes.forEach { node ->
                Card(
                    modifier = Modifier
                        .width(140.dp)
                        .border(1.dp, PrimaryLightBlue.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
                    colors = CardDefaults.cardColors(containerColor = Slate900)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        val icon = when {
                            node.contains("CloudFront") -> Icons.Default.Cloud
                            node.contains("Browser") -> Icons.Default.Laptop
                            node.contains("ALB") -> Icons.Default.SwapCalls
                            node.contains("Postgres") -> Icons.Default.Storage
                            node.contains("Cache") -> Icons.Default.Memory
                            else -> Icons.Default.Dns
                        }
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(PrimaryLightBlue.copy(alpha = 0.15f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(imageVector = icon, contentDescription = null, tint = PrimaryLightBlue, modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(text = node, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
