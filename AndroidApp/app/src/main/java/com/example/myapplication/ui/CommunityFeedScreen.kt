package com.example.myapplication.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.data.MemberComment
import com.example.myapplication.data.TimelinePost

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunityFeedScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    var postText by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("🌐 コミュニティタイムライン", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text("← 戻る")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            // 投稿フォーム
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("つぶやき・我慢ログを投稿", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = postText,
                        onValueChange = { postText = it },
                        placeholder = { Text("例: 今日は1本も吸わずに過ごせた！") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        maxLines = 3
                    )
                    Button(
                        onClick = {
                            if (postText.isNotBlank()) {
                                viewModel.postToTimeline(postText)
                                postText = ""
                            }
                        },
                        modifier = Modifier
                            .align(Alignment.End)
                            .padding(top = 8.dp)
                    ) {
                        Text("投稿する")
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // タイムライン一覧
            if (uiState.feed.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("投稿はまだありません。つぶやいてみましょう！", color = MaterialTheme.colorScheme.secondary)
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(uiState.feed) { post ->
                        PostCard(post = post)
                    }
                }
            }
        }
    }
}

@Composable
fun PostCard(post: TimelinePost) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = post.author,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(text = post.timestamp, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
            }
            Text(
                text = post.text,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 8.dp, bottom = 12.dp)
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            Text(
                text = "💬 専属メンバーからの反応",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
            )

            // メンバーのコメント一覧
            post.memberComments.forEach { comment ->
                MemberCommentRow(comment = comment)
            }
        }
    }
}

@Composable
fun MemberCommentRow(comment: MemberComment) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .background(MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(8.dp))
            .padding(8.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(text = comment.avatar, fontSize = 20.sp, modifier = Modifier.padding(end = 8.dp))
        Column {
            Text(
                text = comment.name,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.secondary
            )
            Text(
                text = comment.comment,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
