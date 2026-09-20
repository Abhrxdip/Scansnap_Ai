package com.smartvendor.ai.ui.theme

import androidx.compose.ui.graphics.Color

// ── Neubrutalism Design System Palette (from newfrontend) ──
val NeuBackground = Color(0xFFF5F0E8)   // Warm Cream canvas
val NeuSurface = Color(0xFFFFFDF7)      // Crisp Card surface
val NeuBlack = Color(0xFF0A0A0A)        // High-contrast ink border & text
val NeuBlue = Color(0xFF1D4ED8)         // Electric Royal Blue (Primary)
val NeuBlueHover = Color(0xFF2563EB)
val NeuYellow = Color(0xFFFBBF24)       // Amber Gold
val NeuGreen = Color(0xFF34D399)        // Emerald Mint (Success / Cash)
val NeuGreenDark = Color(0xFF059669)
val NeuRed = Color(0xFFE11D48)          // Crimson Red (Alert / Danger)
val NeuPurple = Color(0xFFA78BFA)       // Lavender Purple (AI / Insights)
val NeuPink = Color(0xFFF472B6)         // Candy Pink
val NeuCyan = Color(0xFF22D3EE)         // Cyan Accent
val NeuGray = Color(0xFF374151)         // Dark Charcoal Secondary Text
val NeuGrayLight = Color(0xFFE5E7EB)    // Neutral divider / border
val NeuCreamAlt = Color(0xFFEDE8DE)
val NeuWhite = Color(0xFFFFFFFF)       // Pure White for buttons and high-contrast accents

// Backward-compatible tokens mapped to Neubrutalism palette
val BluePrimary = NeuBlue
val BlueDark = NeuBlack
val BlueLight = Color(0xFFD6EEFF)
val AccentGreen = NeuGreen
val WarningYellow = NeuYellow
val DangerRed = NeuRed

val BackgroundLight = NeuBackground
val SurfaceLight = NeuSurface
val TextPrimaryLight = NeuBlack
val TextSecondaryLight = NeuGray

val BackgroundDark = Color(0xFF121212)
val SurfaceDark = Color(0xFF1E1E1E)
val TextPrimaryDark = Color(0xFFF9FAFB)
val TextSecondaryDark = Color(0xFF9CA3AF)
