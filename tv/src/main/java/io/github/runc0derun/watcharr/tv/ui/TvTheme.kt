package io.github.runc0derun.watcharr.tv.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.darkColorScheme

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TvAppTheme(content: @Composable () -> Unit) {
    val darkColors = darkColorScheme(
        primary = Color(0xFFDF9A28), // Mustard Yellow
        secondary = Color(0xFFF5C453), // Light Mustard
        background = Color(0xFF062A1F), // Dark Forest Green
        surface = Color(0xFF0F3E30), // Medium Forest Green
        onBackground = Color(0xFFFAF8F5),
        onSurface = Color(0xFFFAF8F5),
        tertiary = Color(0xFF49A752),
        tertiaryContainer = Color(0xFF0F6633),
        error = Color(0xFFE91E63)
    )
    MaterialTheme(
        colorScheme = darkColors,
        content = content
    )
}
