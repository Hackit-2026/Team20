package com.example.myapplication.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.ui.theme.MyApplicationTheme

@Composable
fun StartScreen(onStartClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
    ) {
        Text(
            text = "ヤニモグラ",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = 48.dp),
        )
        Box(
            modifier = Modifier
                .size(120.dp)
                .background(MaterialTheme.colorScheme.primary, CircleShape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(bounded = false, radius = 80.dp),
                    onClick = onStartClick,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "+",
                style = MaterialTheme.typography.displayLarge,
                color = MaterialTheme.colorScheme.onPrimary,
                fontSize = 56.sp,
                textAlign = TextAlign.Center,
            )
        }
        Text(
            text = "タップしてはじめる",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 24.dp),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun StartScreenPreview() {
    MyApplicationTheme {
        StartScreen(onStartClick = {})
    }
}
