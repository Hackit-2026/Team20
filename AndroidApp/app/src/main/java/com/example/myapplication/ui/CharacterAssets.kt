package com.example.myapplication.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.logic.StageLogic

@Composable
fun CharacterView(stageIndex: Int, modifier: Modifier = Modifier) {
    val background by animateColorAsState(
        targetValue = StageLogic.stageBackgrounds[stageIndex],
        label = "stageBackground"
    )
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(background)
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = StageLogic.stageEmojis[stageIndex],
            fontSize = 120.sp, 
            textAlign = TextAlign.Center,
        )
    }
}
