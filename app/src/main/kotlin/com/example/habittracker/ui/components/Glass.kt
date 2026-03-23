package com.example.habittracker.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.platform.LocalConfiguration

@Composable
fun AppBackground(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val gradient = Brush.linearGradient(
        listOf(
            colors.surfaceVariant.copy(alpha = 0.65f),
            colors.background,
            colors.surfaceVariant.copy(alpha = 0.55f)
        )
    )
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(gradient)
    ) {
        content()
    }
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.medium,
    borderColor: Color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
    contentPadding: PaddingValues = PaddingValues(16.dp),
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val gradient = Brush.linearGradient(
        listOf(
            colors.surface.copy(alpha = 0.28f),
            colors.surfaceVariant.copy(alpha = 0.32f),
            colors.surface.copy(alpha = 0.28f)
        )
    )

    val baseModifier = modifier
        .clip(shape)
        .background(gradient)
        .let { mod -> if (onClick != null) mod.clickable(onClick = onClick) else mod }

    Card(
        modifier = baseModifier,
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        shape = shape,
        border = BorderStroke(
            1.dp,
            Brush.linearGradient(
                listOf(
                    borderColor,
                    borderColor.copy(alpha = 0.05f)
                )
            )
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp, pressedElevation = 10.dp)
    ) {
        Column(
            modifier = Modifier
                .background(colors.surface.copy(alpha = 0.35f))
                .padding(contentPadding),
            content = content
        )
    }
}

@Composable
fun adaptiveContentPadding(vertical: Dp = 16.dp): PaddingValues {
    val config = LocalConfiguration.current
    val horizontal = when (config.screenWidthDp) {
        in 0..599 -> 16.dp
        in 600..839 -> 20.dp
        in 840..1199 -> 28.dp
        else -> 36.dp
    }
    return remember(config.screenWidthDp, vertical) {
        PaddingValues(horizontal = horizontal, vertical = vertical)
    }
}
