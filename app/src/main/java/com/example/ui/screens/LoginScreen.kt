package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.*
import com.example.ui.viewmodel.AppViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(viewModel: AppViewModel) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isRegisterMode by remember { mutableStateOf(false) }
    var isForgotPasswordMode by remember { mutableStateOf(false) }
    var passwordVisible by remember { mutableStateOf(false) }

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
                    .padding(32.dp),
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
                    modifier = Modifier.padding(bottom = 28.dp),
                    textAlign = TextAlign.Center
                )

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
                                    onClick = { viewModel.login(email.ifEmpty { "demo@enterprise.com" }) },
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
                                        .padding(bottom = 24.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        focusedBorderColor = AccentIndigo
                                    )
                                )

                                Button(
                                    onClick = { isRegisterMode = false },
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

                // Bottom Switch Links & Social
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
                                .clickable { isRegisterMode = !isRegisterMode }
                                .padding(vertical = 4.dp)
                        )
                    } else {
                        Text(
                            text = "กลับไปยังหน้าเข้าสู่ระบบ",
                            color = PrimaryLightBlue,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            modifier = Modifier
                                .clickable { isForgotPasswordMode = false }
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
                    onClick = { viewModel.login("oauth.google@enterprise.com") },
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
                    Text("เข้าสู่ระบบผ่าน Google Workspace", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                }
            }
        }
    }
}

// Helper for dynamic flow state
fun <T> mutableStateFlowOf(value: T): MutableState<T> {
    return mutableStateOf(value)
}
