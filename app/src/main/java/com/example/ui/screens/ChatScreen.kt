package com.aistudio.aisystemanalyst.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import com.aistudio.aisystemanalyst.ui.viewmodel.ChatMessage
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aistudio.aisystemanalyst.ui.theme.*
import com.aistudio.aisystemanalyst.ui.viewmodel.AppViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(viewModel: AppViewModel, paddingValues: PaddingValues) {
    val chatHistory by viewModel.chatHistory.collectAsState()
    val isChatLoading by viewModel.isChatLoading.collectAsState()
    val selectedProject by viewModel.selectedProject.collectAsState()

    var userMessage by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current

    val quickPrompts = listOf(
        "อธิบายระบบนี้อย่างย่อ",
        "สรุปความต้องการ (Requirements)",
        "ช่วยร่าง Test Cases ให้หน่อย",
        "ช่วยออกแบบ API Endpoints",
        "ช่วยวิเคราะห์ความเสี่ยง (Risks)",
        "ช่วยเขียนลำดับขั้นตอน (Sequence Diagram)"
    )

    if (selectedProject == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Transparent)
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            Text("กรุณาเลือกหรืออัปโหลดโครงการเพื่อเริ่มแชทกับ AI", color = Slate500, fontSize = 16.sp)
        }
        return
    }

    val project = selectedProject!!

    // Auto scroll to bottom when chat history updates
    LaunchedEffect(chatHistory.size) {
        if (chatHistory.isNotEmpty()) {
            listState.animateScrollToItem(chatHistory.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)
            .padding(paddingValues)
    ) {
        // Chat Header Banner
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .border(1.dp, Slate700.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
            colors = CardDefaults.cardColors(containerColor = Slate800.copy(alpha = 0.5f))
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(PrimaryLightBlue.copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.SupportAgent, contentDescription = null, tint = PrimaryLightBlue, modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "ผู้ช่วยสถาปนิก AI (AI Context Chatbot)", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                    Text(text = "กำลังคุยในบริบทโครงการ: ${project.name}", color = Slate500, fontSize = 12.sp)
                }

                IconButton(onClick = { viewModel.clearChat() }) {
                    Icon(Icons.Default.DeleteSweep, contentDescription = "Clear Chat", tint = Slate500)
                }
            }
        }

        // Chat logs
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            if (chatHistory.isEmpty()) {
                // Empty state view
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Default.QuestionAnswer, contentDescription = null, tint = Slate700, modifier = Modifier.size(64.dp))
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(text = "ยินดีต้อนรับสู่แชทบอทอัจฉริยะ", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp)
                    Text(text = "ถามข้อมูลเชิงลึก สร้างเอกสารสเปค หรือสร้างชุดทดสอบได้ทันที", color = Slate500, fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp))
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(chatHistory) { message ->
                        ChatBubble(message = message)
                    }

                    if (isChatLoading) {
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 10.dp),
                                horizontalArrangement = Arrangement.Start,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .background(Slate800, RoundedCornerShape(12.dp))
                                        .padding(14.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = PrimaryLightBlue)
                                        Text(text = "AI กำลังคิดคำตอบเชิงลึก...", color = Slate100, fontSize = 13.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Horizontal Quick Prompts Selector
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp, horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(quickPrompts) { prompt ->
                Box(
                    modifier = Modifier
                        .background(Slate800, RoundedCornerShape(12.dp))
                        .border(1.dp, Slate700.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        .clickable { viewModel.sendChatMessage(prompt) }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(text = prompt, color = PrimaryLightBlue, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Input Field Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Slate800)
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedTextField(
                value = userMessage,
                onValueChange = { userMessage = it },
                placeholder = { Text("ถามเกี่ยวกับโครงการของคุณที่นี่...", color = Slate500, fontSize = 14.sp) },
                modifier = Modifier.weight(1f),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Slate100,
                    focusedBorderColor = PrimaryLightBlue,
                    unfocusedBorderColor = Slate700
                ),
                shape = RoundedCornerShape(12.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(
                    onSend = {
                        if (userMessage.trim().isNotEmpty()) {
                            viewModel.sendChatMessage(userMessage)
                            userMessage = ""
                            keyboardController?.hide()
                        }
                    }
                ),
                maxLines = 3
            )

            IconButton(
                onClick = {
                    if (userMessage.trim().isNotEmpty()) {
                        viewModel.sendChatMessage(userMessage)
                        userMessage = ""
                        keyboardController?.hide()
                    }
                },
                modifier = Modifier
                    .background(PrimaryLightBlue, CircleShape)
                    .size(48.dp)
            ) {
                Icon(imageVector = Icons.Default.Send, contentDescription = "Send", tint = Slate900)
            }
        }
    }
}

// ChatBubble Drawer
@Composable
fun ChatBubble(message: ChatMessage) {
    val isUser = message.sender == "user"
    val alignment = if (isUser) Alignment.End else Alignment.Start
    val bg = if (isUser) PrimaryLightBlue else Slate800
    val textColor = if (isUser) Slate900 else Slate100

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = alignment
    ) {
        Box(
            modifier = Modifier
                .background(
                    color = bg,
                    shape = RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (isUser) 16.dp else 0.dp,
                        bottomEnd = if (isUser) 0.dp else 16.dp
                    )
                )
                .border(
                    width = 1.dp,
                    color = if (isUser) Color.Transparent else Slate700.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (isUser) 16.dp else 0.dp,
                        bottomEnd = if (isUser) 0.dp else 16.dp
                    )
                )
                .padding(14.dp)
                .widthIn(max = 280.dp)
        ) {
            Column {
                Text(
                    text = if (isUser) "คุณ (User)" else "ผู้ช่วยสถาปนิก AI",
                    fontSize = 11.sp,
                    color = if (isUser) Slate900.copy(alpha = 0.6f) else PrimaryLightBlue,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                // Simple structured line support for assistant replies
                if (isUser) {
                    Text(text = message.content, color = textColor, fontSize = 14.sp)
                } else {
                    val formattedText = message.content
                    // We split into simple styling
                    val lines = formattedText.split("\n")
                    lines.forEach { line ->
                        val trimmed = line.trim()
                        when {
                            trimmed.startsWith("###") -> {
                                Text(text = trimmed.replace("###", "").trim(), fontWeight = FontWeight.Bold, color = PrimaryLightBlue, fontSize = 13.sp, modifier = Modifier.padding(vertical = 4.dp))
                            }
                            trimmed.startsWith("##") -> {
                                Text(text = trimmed.replace("##", "").trim(), fontWeight = FontWeight.Bold, color = AccentIndigo, fontSize = 14.sp, modifier = Modifier.padding(vertical = 6.dp))
                            }
                            trimmed.startsWith("-") || trimmed.startsWith("*") -> {
                                Text(text = " • " + trimmed.substring(1).trim(), color = textColor, fontSize = 13.sp, modifier = Modifier.padding(vertical = 2.dp))
                            }
                            trimmed.isNotEmpty() -> {
                                Text(text = trimmed, color = textColor, fontSize = 13.sp, modifier = Modifier.padding(vertical = 2.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}
