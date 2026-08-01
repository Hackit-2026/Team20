package com.example.myapplication.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun TimelineScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    CommunityFeedScreen(
        viewModel = viewModel,
        onBack = onBack,
        modifier = modifier
    )
}
