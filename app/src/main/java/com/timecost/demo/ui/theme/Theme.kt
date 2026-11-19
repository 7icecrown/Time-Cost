package com.timecost.demo.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** 시안 토큰 (PRD 9장 + 시안의 강조색 #2B59C3). 시안은 라이트 단일 테마입니다. */
object TC {
    val Bg = Color(0xFFF7F7F5)
    val Text = Color(0xFF16181D)
    val Sub = Color(0xFF5F646C)
    val Border = Color(0xFFE4E4E0)
    val Primary = Color(0xFF16181D)
    val Highlight = Color(0xFFFFF8E1)
    val Accent = Color(0xFF2B59C3)
    val Card = Color.White
    val Radius = 12.dp
}

@Composable
fun TimeCostTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            background = TC.Bg, surface = TC.Card, onBackground = TC.Text, onSurface = TC.Text,
            primary = TC.Primary, onPrimary = Color.White, outline = TC.Border,
        ),
        shapes = Shapes(medium = RoundedCornerShape(TC.Radius)),
        content = content,
    )
}
