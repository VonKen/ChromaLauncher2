package com.chromalauncher.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.chromalauncher.app.ui.theme.Cyan
import com.chromalauncher.app.ui.theme.Magenta
import com.chromalauncher.app.ui.theme.Violet

@Composable
fun ChromaDivider(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(2.dp)
            .clip(RoundedCornerShape(1.dp))
            .background(
                brush = Brush.horizontalGradient(
                    colors = listOf(Violet, Cyan, Magenta)
                )
            )
    )
}
