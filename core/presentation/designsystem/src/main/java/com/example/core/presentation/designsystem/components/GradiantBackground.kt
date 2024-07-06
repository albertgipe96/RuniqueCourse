package com.example.core.presentation.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

@Composable
fun GradientBackground(
    modifier: Modifier = Modifier,
    hasToolbar: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current

    val screenWidthPx = with(density) {
        configuration.screenWidthDp.dp.roundToPx()
    }
    val smallDimension = minOf(configuration.screenWidthDp.dp, configuration.screenHeightDp.dp)
    val smallDimensionPx = with(density) {
        smallDimension.roundToPx()
    }

    val primaryColor = MaterialTheme.colorScheme.primary
    val backgroundColor = MaterialTheme.colorScheme.background

    Box(
        modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .blur(smallDimension / 3F)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(primaryColor, backgroundColor),
                        center = Offset(x = screenWidthPx / 2F, y = -100F),
                        radius = smallDimensionPx / 2F
                    )
                )
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (hasToolbar) Modifier
                    else Modifier.systemBarsPadding()
                )
        ) {
            content()
        }
    }
}