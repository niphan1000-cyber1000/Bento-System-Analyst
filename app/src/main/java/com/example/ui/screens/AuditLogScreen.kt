package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
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
import com.example.ui.theme.PrimaryLightBlue
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import com.example.ui.viewmodel.AppViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AuditLogScreen(viewModel: AppViewModel, paddingValues: PaddingValues) {
    val auditLogs by viewModel.auditLogs.collectAsState()

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
                Text(text = "ประวัติและระบบการบันทึกการทำงาน (Audit Trails)", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
            Text(
                text = "ประวัติการเรียกวิเคราะห์ AI การเข้าถึงสิทธิ์ และการสร้าง/ลบข้อมูลระบบแบบละเอียดระดับ Enterprise",
                fontSize = 12.sp,
                color = Slate500,
                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
            )
        }

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
    }
}
