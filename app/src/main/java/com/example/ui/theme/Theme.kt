package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme =
  darkColorScheme(
    primary = BentoIndigo,
    secondary = BentoPurple,
    tertiary = BentoEmerald,
    background = BentoBg,
    surface = BentoCardBg,
    onPrimary = BentoBg,
    onSecondary = Color.White,
    onBackground = BentoTextSecondary,
    onSurface = BentoTextPrimary,
    error = BentoRose
  )

private val LightColorScheme = DarkColorScheme // Enforce the gorgeous Bento Dark theme as standard to follow Bento specifications

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = true, // Force Dark Bento Theme
  dynamicColor: Boolean = false, // Disable dynamic color to preserve custom glowing colors
  content: @Composable () -> Unit,
) {
  val colorScheme = DarkColorScheme

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
