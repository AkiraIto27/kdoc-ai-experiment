package com.example.kdoctest.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = CatalogTeal,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDCEDE3),
    onPrimaryContainer = Color(0xFF164A3F),
    secondary = Color(0xFF71613F),
    secondaryContainer = Color(0xFFF2EAD8),
    onSecondaryContainer = Color(0xFF554625),
    background = CatalogCream,
    onBackground = CatalogInk,
    surface = Color(0xFFFFFEFA),
    onSurface = CatalogInk,
    onSurfaceVariant = CatalogMuted,
    surfaceVariant = Color(0xFFEBEEE6),
    surfaceContainer = Color(0xFFEFEFE8),
    surfaceContainerLow = Color(0xFFFFFEFA),
    surfaceContainerHigh = Color(0xFFE7EBE2),
    outline = Color(0xFF7C8980),
    outlineVariant = CatalogLine,
)

private val DarkColorScheme = darkColorScheme(
    primary = CatalogTealLight,
    onPrimary = Color(0xFF093B30),
    primaryContainer = Color(0xFF284D42),
    onPrimaryContainer = Color(0xFFD1EDE0),
    secondary = Color(0xFFD9C79F),
    secondaryContainer = Color(0xFF4F4531),
    onSecondaryContainer = Color(0xFFF2E5C8),
    background = Color(0xFF141C18),
    onBackground = Color(0xFFE3EAE2),
    surface = Color(0xFF1B2520),
    onSurface = Color(0xFFE3EAE2),
    onSurfaceVariant = Color(0xFFB5C2B7),
    surfaceVariant = Color(0xFF354239),
    surfaceContainer = Color(0xFF222D26),
    surfaceContainerLow = Color(0xFF1B2520),
    surfaceContainerHigh = Color(0xFF2D3931),
    outline = Color(0xFF839187),
    outlineVariant = Color(0xFF3D4D42),
)

@Composable
fun KDocTestTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = Typography,
        content = content,
    )
}
