package com.example.ui.screens

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ProjectTask
import com.example.data.model.RiskItem
import com.example.ui.theme.*
import com.example.ui.viewmodel.AppViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectMgmtScreen(viewModel: AppViewModel, paddingValues: PaddingValues) {
    val selectedProject by viewModel.selectedProject.collectAsState()
    val tasks by viewModel.projectTasks.collectAsState()
    val risks by viewModel.projectRisks.collectAsState()
    val userRole by viewModel.userRole.collectAsState()

    var showAddTaskDialog by remember { mutableStateOf(false) }
    var taskTitle by remember { mutableStateOf("") }
    var taskDesc by remember { mutableStateOf("") }
    var taskPriority by remember { mutableStateOf("Medium") }
    var taskAssignee by remember { mutableStateOf("Developer") }
    var taskSprint by remember { mutableStateOf("Sprint 1") }

    var showAddRiskDialog by remember { mutableStateOf(false) }
    var riskTitle by remember { mutableStateOf("") }
    var riskSeverity by remember { mutableStateOf("Medium") }
    var riskCategory by remember { mutableStateOf("Security") }
    var riskMitigation by remember { mutableStateOf("") }

    var currentViewMode by remember { mutableStateOf("Kanban") } // "Kanban", "Risks", "Timeline"

    if (selectedProject == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Transparent)
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            Text("กรุณาเลือกหรืออัปโหลดโครงการเพื่อใช้โมดูลจัดการ", color = Slate500, fontSize = 16.sp)
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
        // Toggle header for sub-views
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Slate800.copy(alpha = 0.5f))
                .padding(vertical = 12.dp, horizontal = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Kanban", "Risks", "Timeline").forEach { mode ->
                    val active = currentViewMode == mode
                    Box(
                        modifier = Modifier
                            .background(
                                if (active) PrimaryLightBlue else Slate800,
                                RoundedCornerShape(8.dp)
                            )
                            .clickable { currentViewMode = mode }
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = when (mode) {
                                "Kanban" -> "คัมบังบอร์ด (Kanban)"
                                "Risks" -> "ความเสี่ยง (Risks)"
                                else -> "ไทม์ไลน์ (Timeline)"
                            },
                            color = if (active) Slate900 else Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Quick add buttons based on view mode
            if (currentViewMode == "Kanban") {
                IconButton(
                    onClick = { showAddTaskDialog = true },
                    modifier = Modifier
                        .background(PrimaryLightBlue, CircleShape)
                        .size(36.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Task", tint = Slate900)
                }
            } else if (currentViewMode == "Risks") {
                IconButton(
                    onClick = { showAddRiskDialog = true },
                    modifier = Modifier
                        .background(AccentRose, CircleShape)
                        .size(36.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Risk", tint = Color.White)
                }
            }
        }

        when (currentViewMode) {
            "Kanban" -> {
                // Horizontal Kanban Columns
                val statuses = listOf("Todo", "In Progress", "Review", "Done")
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .horizontalScroll(rememberScrollState())
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    statuses.forEach { status ->
                        val colTasks = tasks.filter { it.status == status }
                        KanbanColumn(
                            status = status,
                            tasks = colTasks,
                            onMoveTask = { task ->
                                val nextStatus = when (task.status) {
                                    "Todo" -> "In Progress"
                                    "In Progress" -> "Review"
                                    "Review" -> "Done"
                                    else -> "Todo"
                                }
                                viewModel.updateTaskStatus(task, nextStatus)
                            },
                            onDeleteTask = { viewModel.deleteTask(it) }
                        )
                    }
                }
            }

            "Risks" -> {
                // Risks view
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Text("สรุปการบริหารจัดการความเสี่ยง (Risk Registry)", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    if (risks.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(120.dp)
                                    .background(Slate800.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("ยังไม่ตรวจพบความเสี่ยงเพิ่มในระบบคลาวด์", color = Slate500)
                            }
                        }
                    } else {
                        items(risks) { risk ->
                            RiskRegistryItem(risk = risk, onDelete = { viewModel.deleteRisk(risk) })
                        }
                    }
                }
            }

            "Timeline" -> {
                // Milestone timelines
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Text("แผนงานและไมล์สโตนโครงการ (Gantt Milestone Roadmap)", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    val milestones = listOf(
                        Triple("เฟส 1: การรวบรวม & วิเคราะห์ Requirements", "รวบรวมข้อจำกัด สรุปผลรายงาน คุย AI และลงข้อมูล", "Completed"),
                        Triple("เฟส 2: ออกแบบ APIs สเปคสถาปัตยกรรม & ฐานข้อมูล ERD", "สร้างตารางฐานข้อมูล สรุปโมดูลหลัก", "In Progress"),
                        Triple("เฟส 3: เริ่มต้นติดตั้ง Boilerplate โค้ดต้นฉบับ & ด็อคเกอร์", "เซ็ตอัพ Environment และ Containerizing", "Upcoming"),
                        Triple("เฟส 4: ทำการรีวิวความปลอดภัยและช่องโหว่ OWASP Top 10", "สแกนโค้ดสากล ความเสี่ยงฐานข้อมูล", "Upcoming"),
                        Triple("เฟส 5: บันทึกรายงานฉบับสมบูรณ์ (BRD / SRS) ส่งมอบโครงการ", "เซ็ตรายงานสรุป เอกสารสเปค พร้อมส่งงาน", "Upcoming")
                    )

                    items(milestones) { (title, desc, state) ->
                        MilestoneTimelineRow(title = title, desc = desc, state = state)
                    }
                }
            }
        }
    }

    // Add Task Dialog
    if (showAddTaskDialog) {
        AlertDialog(
            onDismissRequest = { showAddTaskDialog = false },
            title = { Text("สร้างงานใหม่", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = taskTitle,
                        onValueChange = { taskTitle = it },
                        label = { Text("หัวข้อการทำงาน") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White)
                    )

                    OutlinedTextField(
                        value = taskDesc,
                        onValueChange = { taskDesc = it },
                        label = { Text("รายละเอียดงาน") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White)
                    )

                    OutlinedTextField(
                        value = taskAssignee,
                        onValueChange = { taskAssignee = it },
                        label = { Text("ผู้รับผิดชอบ") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White)
                    )

                    Text("ระดับความสำคัญ (Priority)", fontSize = 12.sp, color = Slate500)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Low", "Medium", "High").forEach { pr ->
                            val active = taskPriority == pr
                            Box(
                                modifier = Modifier
                                    .background(if (active) PrimaryLightBlue else Slate800, RoundedCornerShape(8.dp))
                                    .clickable { taskPriority = pr }
                                    .padding(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Text(pr, color = if (active) Slate900 else Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (taskTitle.isNotEmpty()) {
                            viewModel.addTask(taskTitle, taskDesc, taskPriority, taskAssignee, taskSprint)
                            showAddTaskDialog = false
                            // Reset
                            taskTitle = ""
                            taskDesc = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryLightBlue, contentColor = Slate900)
                ) {
                    Text("บันทึกงาน")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddTaskDialog = false }) {
                    Text("ยกเลิก", color = Slate500)
                }
            },
            containerColor = Slate800
        )
    }

    // Add Risk Dialog
    if (showAddRiskDialog) {
        AlertDialog(
            onDismissRequest = { showAddRiskDialog = false },
            title = { Text("บันทึกความเสี่ยงใหม่", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = riskTitle,
                        onValueChange = { riskTitle = it },
                        label = { Text("ชื่อความเสี่ยงที่พบ") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White)
                    )

                    OutlinedTextField(
                        value = riskMitigation,
                        onValueChange = { riskMitigation = it },
                        label = { Text("แผนผ่อนปรนเพื่อลดความเสี่ยง (Mitigation Plan)") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White)
                    )

                    Text("ระดับความรุนแรง (Severity)", fontSize = 12.sp, color = Slate500)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Low", "Medium", "High").forEach { sev ->
                            val active = riskSeverity == sev
                            Box(
                                modifier = Modifier
                                    .background(if (active) AccentRose else Slate800, RoundedCornerShape(8.dp))
                                    .clickable { riskSeverity = sev }
                                    .padding(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Text(sev, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (riskTitle.isNotEmpty()) {
                            viewModel.addRisk(riskTitle, riskSeverity, riskCategory, "Identified", riskMitigation)
                            showAddRiskDialog = false
                            riskTitle = ""
                            riskMitigation = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentRose, contentColor = Color.White)
                ) {
                    Text("บันทึกความเสี่ยง")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddRiskDialog = false }) {
                    Text("ยกเลิก", color = Slate500)
                }
            },
            containerColor = Slate800
        )
    }
}

// Kanban Column UI
@Composable
fun KanbanColumn(
    status: String,
    tasks: List<ProjectTask>,
    onMoveTask: (ProjectTask) -> Unit,
    onDeleteTask: (ProjectTask) -> Unit
) {
    Card(
        modifier = Modifier
            .width(280.dp)
            .fillMaxHeight()
            .border(1.dp, Slate700.copy(alpha = 0.3f), RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = Slate800.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Column Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = when (status) {
                        "Todo" -> "รอดำเนินการ (Todo)"
                        "In Progress" -> "กำลังทำ (In Progress)"
                        "Review" -> "รอตรวจ (Review)"
                        else -> "เสร็จสิ้น (Done)"
                    },
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 14.sp
                )
                Box(
                    modifier = Modifier
                        .background(Slate700, CircleShape)
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(text = tasks.size.toString(), color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxHeight()
            ) {
                if (tasks.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(80.dp)
                                .border(1.dp, Slate700.copy(alpha = 0.1f), RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("ไม่มีงานในคอลัมน์นี้", color = Slate500, fontSize = 12.sp)
                        }
                    }
                } else {
                    items(tasks) { task ->
                        KanbanTaskCard(task = task, onMove = { onMoveTask(task) }, onDelete = { onDeleteTask(task) })
                    }
                }
            }
        }
    }
}

// Kanban individual card with action trigger
@Composable
fun KanbanTaskCard(
    task: ProjectTask,
    onMove: () -> Unit,
    onDelete: () -> Unit
) {
    val priColor = when (task.priority) {
        "High" -> AccentRose
        "Medium" -> AccentAmber
        else -> AccentEmerald
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Slate700.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            .clickable { onMove() },
        colors = CardDefaults.cardColors(containerColor = Slate900)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .background(priColor.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(text = task.priority, color = priColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }

                IconButton(onClick = onDelete, modifier = Modifier.size(20.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Slate500, modifier = Modifier.size(14.dp))
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(text = task.title, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
            if (task.description.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = task.description, color = Slate500, fontSize = 12.sp, maxLines = 2)
            }

            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Person, contentDescription = null, tint = PrimaryLightBlue, modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = task.assignee, color = Slate100, fontSize = 11.sp)
                }

                Text(text = "คลิกเพื่อย้าย ➔", color = PrimaryLightBlue, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// Risk UI Registry Item
@Composable
fun RiskRegistryItem(risk: RiskItem, onDelete: () -> Unit) {
    val badgeColor = when (risk.severity) {
        "High" -> AccentRose
        "Medium" -> AccentAmber
        else -> AccentEmerald
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Slate700.copy(alpha = 0.4f), RoundedCornerShape(14.dp)),
        colors = CardDefaults.cardColors(containerColor = Slate800)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .background(badgeColor.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(text = "ความรุนแรง: ${risk.severity}", color = badgeColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Box(
                        modifier = Modifier
                            .background(PrimaryLightBlue.copy(alpha = 0.1f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(text = risk.category, color = PrimaryLightBlue, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = null, tint = Slate500, modifier = Modifier.size(16.dp))
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text(text = risk.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "แผนการรับมือ (Mitigation Plan):", fontSize = 12.sp, color = Slate500, fontWeight = FontWeight.Bold)
            Text(text = risk.mitigationPlan, fontSize = 13.sp, color = Slate100, lineHeight = 18.sp)
        }
    }
}

// Timeline Row Item
@Composable
fun MilestoneTimelineRow(
    title: String,
    desc: String,
    state: String
) {
    val circleColor = when (state) {
        "Completed" -> AccentEmerald
        "In Progress" -> PrimaryLightBlue
        else -> Slate700
    }

    Row(modifier = Modifier.fillMaxWidth()) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(end = 16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .background(circleColor, CircleShape)
            )
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .height(60.dp)
                    .background(Slate700)
            )
        }

        Column {
            Text(text = title, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
            Text(text = desc, color = Slate100, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
            Spacer(modifier = Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .background(circleColor.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(
                    text = when (state) {
                        "Completed" -> "ดำเนินการเสร็จสิ้น"
                        "In Progress" -> "กำลังดำเนินการ (Active)"
                        else -> "เฟสถัดไป (Upcoming)"
                    },
                    color = circleColor,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
