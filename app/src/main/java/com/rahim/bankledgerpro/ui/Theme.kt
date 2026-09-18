package com.rahim.bankledgerpro.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Navy = Color(0xFF11131A)
private val Card = Color(0xFF1A1D26)
private val Accent = Color(0xFF42D3B2)
private val Purple = Color(0xFF8B7CFF)

private val DarkColors = darkColorScheme(
    primary = Accent,
    secondary = Purple,
    background = Navy,
    surface = Card,
    onBackground = Color(0xFFF4F6FA),
    onSurface = Color(0xFFF4F6FA)
)

@Composable
fun BankLedgerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColors,
        typography = Typography(),
        content = content
    )
}
