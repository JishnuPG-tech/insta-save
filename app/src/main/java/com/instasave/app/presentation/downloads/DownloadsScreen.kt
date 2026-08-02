package com.instasave.app.presentation.downloads

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.instasave.app.domain.model.DownloadStatus
import com.instasave.app.domain.model.DownloadTask
import com.instasave.app.presentation.theme.AccentError
import com.instasave.app.presentation.theme.HairlineBorder
import com.instasave.app.presentation.theme.PitchBlack
import com.instasave.app.presentation.theme.SurfaceDark
import com.instasave.app.presentation.theme.TextMuted
import com.instasave.app.presentation.theme.TextPrimary
import com.instasave.app.presentation.theme.TextSecondary
import com.instasave.app.presentation.theme.Typography

@Composable
fun DownloadsScreen(
    onNavigateToPreview: (String) -> Unit,
    viewModel: DownloadsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PitchBlack)
            .padding(16.dp)
    ) {
        Text(text = "Downloads", style = Typography.headlineLarge)
        Spacer(modifier = Modifier.height(16.dp))

        // Tabs (Active Queue vs History)
        TabRow(
            selectedTabIndex = uiState.selectedTab,
            containerColor = PitchBlack,
            contentColor = TextPrimary,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    Modifier.tabIndicatorOffset(tabPositions[uiState.selectedTab]),
                    color = TextPrimary
                )
            }
        ) {
            Tab(
                selected = uiState.selectedTab == 0,
                onClick = { viewModel.onEvent(DownloadsEvent.TabSelected(0)) },
                text = { Text("Active (${uiState.activeQueue.size})", style = Typography.titleMedium) }
            )
            Tab(
                selected = uiState.selectedTab == 1,
                onClick = { viewModel.onEvent(DownloadsEvent.TabSelected(1)) },
                text = { Text("History (${uiState.history.size})", style = Typography.titleMedium) }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (uiState.selectedTab == 0) {
            // Active Queue View
            if (uiState.activeQueue.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "No active downloads", style = Typography.bodyLarge, color = TextMuted)
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(uiState.activeQueue) { task ->
                        DownloadItemRow(
                            task = task,
                            onPause = { viewModel.onEvent(DownloadsEvent.PauseTask(task.id)) },
                            onResume = { viewModel.onEvent(DownloadsEvent.ResumeTask(task.id)) },
                            onCancel = { viewModel.onEvent(DownloadsEvent.CancelTask(task.id)) },
                            onRetry = { viewModel.onEvent(DownloadsEvent.RetryTask(task.id)) }
                        )
                    }
                }
            }
        } else {
            // History View
            if (uiState.history.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "No download history", style = Typography.bodyLarge, color = TextMuted)
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(uiState.history) { task ->
                        HistoryItemRow(
                            task = task,
                            onDelete = { viewModel.onEvent(DownloadsEvent.DeleteHistoryRecord(task.id)) },
                            onClick = { onNavigateToPreview(task.mediaInfo.shortcode) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DownloadItemRow(
    task: DownloadTask,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onCancel: () -> Unit,
    onRetry: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(SurfaceDark)
            .border(0.5.dp, HairlineBorder, RoundedCornerShape(8.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = task.targetFilename, style = Typography.titleMedium, maxLines = 1)
            val speedKb = task.speedBytesPerSec / 1024
            val statusText = when (task.status) {
                DownloadStatus.DOWNLOADING -> "Downloading • $speedKb KB/s"
                DownloadStatus.PAUSED -> "Paused"
                DownloadStatus.QUEUED -> "Queued..."
                DownloadStatus.FAILED -> "Failed: ${task.errorDetail ?: "Error"}"
                else -> task.status.name
            }
            Text(text = statusText, style = Typography.labelMedium, color = if (task.status == DownloadStatus.FAILED) AccentError else TextSecondary)

            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { task.progress },
                modifier = Modifier.fillMaxWidth().height(4.dp),
                color = TextPrimary,
                trackColor = PitchBlack
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        when (task.status) {
            DownloadStatus.DOWNLOADING -> {
                IconButton(onClick = onPause) {
                    Icon(imageVector = Icons.Default.Pause, contentDescription = "Pause", tint = TextPrimary)
                }
            }
            DownloadStatus.PAUSED -> {
                IconButton(onClick = onResume) {
                    Icon(imageVector = Icons.Default.PlayArrow, contentDescription = "Resume", tint = TextPrimary)
                }
            }
            DownloadStatus.FAILED -> {
                IconButton(onClick = onRetry) {
                    Icon(imageVector = Icons.Default.Refresh, contentDescription = "Retry", tint = TextPrimary)
                }
            }
            else -> {}
        }
        IconButton(onClick = onCancel) {
            Icon(imageVector = Icons.Default.Delete, contentDescription = "Cancel", tint = TextMuted)
        }
    }
}

@Composable
fun HistoryItemRow(
    task: DownloadTask,
    onDelete: () -> Unit,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(SurfaceDark)
            .border(0.5.dp, HairlineBorder, RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = task.targetFilename, style = Typography.titleMedium, maxLines = 1)
            Text(text = "@${task.mediaInfo.author.username}", style = Typography.labelMedium, color = TextSecondary)
        }
        IconButton(onClick = onDelete) {
            Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = TextMuted)
        }
    }
}
