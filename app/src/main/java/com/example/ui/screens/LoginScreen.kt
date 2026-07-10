package com.aistudio.aisystemanalyst.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aistudio.aisystemanalyst.ui.theme.*
import com.aistudio.aisystemanalyst.ui.viewmodel.AppViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(viewModel: AppViewModel) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isRegisterMode by remember { mutableStateOf(false) }
    var isForgotPasswordMode by remember { mutableStateOf(false) }
    var passwordVisible by remember { mutableStateOf(false) }

    // Observe real authentication states
    val authError by viewModel.authError.collectAsState()
    val registerSuccess by viewModel.registerSuccess.collectAsState()

    // Available roles to choose from when registering
    val roles = listOf("System Analyst", "Admin", "Project Manager", "Developer", "QA", "Viewer")
    var selectedRole by remember { mutableStateOf("System Analyst") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(Color(0xFF13151E), BentoBg), // Premium indigo-tinted dark bento slate
                    radius = 2000f
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        // Glowing background accents
        Box(
            modifier = Modifier
                .size(450.dp)
                .offset(x = (-120).dp, y = (-160).dp)
                .background(Brush.radialGradient(listOf(BentoIndigo.copy(alpha = 0.2f), Color.Transparent)))
        )
        Box(
            modifier = Modifier
                .size(500.dp)
                .offset(x = 140.dp, y = 200.dp)
                .background(Brush.radialGradient(listOf(BentoBlue.copy(alpha = 0.15f), Color.Transparent)))
        )

        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .widthIn(max = 440.dp)
                .border(1.dp, BentoCardBorder, RoundedCornerShape(28.dp)),
            colors = CardDefaults.cardColors(containerColor = BentoCardBg),
            shape = RoundedCornerShape(28.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Enterprise Header Brand
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Analytics,
                        contentDescription = "Platform Logo",
                        tint = PrimaryLightBlue,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "AI System Analyst",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontFamily = FontFamily.SansSerif
                    )
                }

                Text(
                    text = "Enterprise AI-Driven Architecture & Analysis",
                    fontSize = 12.sp,
                    color = Slate500,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(bottom = 20.dp),
                    textAlign = TextAlign.Center
                )

                // Error Banner
                if (authError != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                            .background(Color(0xFF3E1F1F), RoundedCornerShape(12.dp))
                            .border(1.dp, Color(0xFFC62828), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Text(
                            text = authError ?: "",
                            color = Color(0xFFFFCDD2),
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // Success Banner
                if (registerSuccess && isRegisterMode) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                            .background(Color(0xFF1B3B2B), RoundedCornerShape(12.dp))
                            .border(1.dp, Color(0xFF2E7D32), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Text(
                            text = "ลงทะเบียนบัญชีสำเร็จแล้ว! กรุณาสลับหน้าจอเพื่อเข้าสู่ระบบ",
                            color = Color(0xFFA5D6A7),
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                AnimatedContent(
                    targetState = when {
                        isForgotPasswordMode -> 2
                        isRegisterMode -> 1
                        else -> 0
                    },
                    transitionSpec = {
                        slideInHorizontally(animationSpec = tween(300)) { if (targetState > initialState) it else -it } togetherWith
                                slideOutHorizontally(animationSpec = tween(300)) { if (targetState > initialState) -it else it }
                    },
                    label = "AuthFormAnimation"
                ) { mode ->
                    when (mode) {
                        0 -> { // Login Screen
                            Column {
                                OutlinedTextField(
                                    value = email,
                                    onValueChange = { email = it },
                                    label = { Text("อีเมลผู้ใช้งาน (Email)") },
                                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = PrimaryLightBlue) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("username_input")
                                        .padding(bottom = 16.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Slate100,
                                        focusedBorderColor = PrimaryLightBlue,
                                        unfocusedBorderColor = Slate700
                                    )
                                )

                                OutlinedTextField(
                                    value = password,
                                    onValueChange = { password = it },
                                    label = { Text("รหัสผ่าน (Password)") },
                                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = PrimaryLightBlue) },
                                    trailingIcon = {
                                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                            Icon(
                                                imageVector = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                                contentDescription = "Toggle password visibility",
                                                tint = Slate500
                                            )
                                        }
                                    },
                                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("password_input")
                                        .padding(bottom = 8.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Slate100,
                                        focusedBorderColor = PrimaryLightBlue,
                                        unfocusedBorderColor = Slate700
                                    )
                                )

                                Box(
                                    modifier = Modifier.fillMaxWidth(),
                                    contentAlignment = Alignment.CenterEnd
                                ) {
                                    Text(
                                        text = "ลืมรหัสผ่าน?",
                                        color = PrimaryLightBlue,
                                        fontSize = 13.sp,
                                        modifier = Modifier
                                            .clickable { isForgotPasswordMode = true }
                                            .padding(vertical = 4.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(24.dp))

                                Button(
                                    onClick = { viewModel.login(email.ifEmpty { "demo@enterprise.com" }, password.ifEmpty { "password123" }) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(50.dp)
                                        .testTag("login_button"),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = PrimaryLightBlue,
                                        contentColor = Slate900
                                    )
                                ) {
                                    Text("เข้าสู่ระบบ (Login)", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                }
                            }
                        }
                        1 -> { // Register Screen
                            Column {
                                OutlinedTextField(
                                    value = email,
                                    onValueChange = { email = it },
                                    label = { Text("ระบุอีเมลสมัครใช้งาน") },
                                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = AccentIndigo) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 16.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        focusedBorderColor = AccentIndigo
                                    )
                                )

                                OutlinedTextField(
                                    value = password,
                                    onValueChange = { password = it },
                                    label = { Text("รหัสผ่านใหม่") },
                                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = AccentIndigo) },
                                    visualTransformation = PasswordVisualTransformation(),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 16.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        focusedBorderColor = AccentIndigo
                                    )
                                )

                                Text(
                                    text = "เลือกบทบาทในการทำงาน (User Role):",
                                    color = Slate500,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )

                                androidx.compose.foundation.lazy.LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 24.dp)
                                ) {
                                    items(roles.size) { index ->
                                        val roleOption = roles[index]
                                        val isSelected = selectedRole == roleOption
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(20.dp))
                                                .background(if (isSelected) AccentIndigo else Slate800)
                                                .clickable { selectedRole = roleOption }
                                                .border(
                                                    1.dp,
                                                    if (isSelected) Color.White else Color.Transparent,
                                                    RoundedCornerShape(20.dp)
                                                )
                                                .padding(horizontal = 14.dp, vertical = 6.dp)
                                        ) {
                                            Text(
                                                text = roleOption,
                                                color = if (isSelected) Color.White else Slate100,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }

                                Button(
                                    onClick = { viewModel.register(email, password, selectedRole) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(50.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = AccentIndigo)
                                ) {
                                    Text("ลงทะเบียนสร้างบัญชี", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        2 -> { // Forgot Password Screen
                            Column {
                                Text(
                                    text = "กรอกอีเมลของคุณเพื่อรับลิงก์สำหรับเปลี่ยนรหัสผ่านใหม่",
                                    color = Slate100,
                                    fontSize = 14.sp,
                                    modifier = Modifier.padding(bottom = 16.dp),
                                    textAlign = TextAlign.Center
                                )

                                OutlinedTextField(
                                    value = email,
                                    onValueChange = { email = it },
                                    label = { Text("อีเมลของคุณ") },
                                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = AccentAmber) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 24.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        focusedBorderColor = AccentAmber
                                    )
                                )

                                Button(
                                    onClick = { isForgotPasswordMode = false },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(50.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = AccentAmber, contentColor = Slate900)
                                ) {
                                    Text("ส่งลิงก์กู้คืนรหัสผ่าน", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Bottom Switch Links
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!isForgotPasswordMode) {
                        Text(
                            text = if (isRegisterMode) "มีบัญชีอยู่แล้ว? " else "ยังไม่มีบัญชีสมาชิกรึเปล่า? ",
                            color = Slate500,
                            fontSize = 13.sp
                        )
                        Text(
                            text = if (isRegisterMode) "เข้าสู่ระบบ" else "สร้างบัญชีใหม่",
                            color = if (isRegisterMode) PrimaryLightBlue else AccentIndigo,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            modifier = Modifier
                                .clickable {
                                    viewModel.clearAuthStates()
                                    isRegisterMode = !isRegisterMode
                                }
                                .padding(vertical = 4.dp)
                        )
                    } else {
                        Text(
                            text = "กลับไปยังหน้าเข้าสู่ระบบ",
                            color = PrimaryLightBlue,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            modifier = Modifier
                                .clickable {
                                    viewModel.clearAuthStates()
                                    isForgotPasswordMode = false
                                }
                                .padding(vertical = 4.dp)
                        )
                    }
                }

                Divider(
                    color = Slate700,
                    modifier = Modifier.padding(vertical = 24.dp),
                    thickness = 0.8.dp
                )

                // Google OAuth Simulated Login
                OutlinedButton(
                    onClick = { viewModel.login("demo@enterprise.com", "password123") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Slate700),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Slate100)
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudQueue,
                        contentDescription = "Google Icon",
                        tint = AccentIndigo,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("เข้าสู่ระบบผ่าน Google Workspace (Demo)", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                }
            }
        }
    }
}
