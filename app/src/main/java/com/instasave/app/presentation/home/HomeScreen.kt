package com.instasave.app.presentation.home

import android.content.ClipboardManager
import android.content.Context
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.instasave.app.domain.model.ExtractionError
import com.instasave.app.presentation.theme.AccentError
import com.instasave.app.presentation.theme.HairlineBorder
import com.instasave.app.presentation.theme.PitchBlack
import com.instasave.app.presentation.theme.SurfaceDark
import com.instasave.app.presentation.theme.TextMuted
import com.instasave.app.presentation.theme.TextPrimary
import com.instasave.app.presentation.theme.TextSecondary
import com.instasave.app.presentation.theme.Typography

@Composable
fun HomeScreen(
    initialSharedUrl: String? = null,
    onNavigateToDownloads: () -> Unit,
    onNavigateToPreview: (String) -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Handle initial shared URL from Intent Quick Share
    LaunchedEffect(initialSharedUrl) {
        if (!initialSharedUrl.isNullOrEmpty()) {
            viewModel.onEvent(HomeEvent.UrlInputChanged(initialSharedUrl))
            viewModel.onEvent(HomeEvent.ExtractClicked)
        }
    }

    // Inspect clipboard upon explicit user action or launch
    LaunchedEffect(Unit) {
        try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clipData = clipboard.primaryClip
            if (clipData != null && clipData.itemCount > 0) {
                val clipText = clipData.getItemAt(0).text?.toString() ?: ""
                if (clipText.contains("instagram.com")) {
                    viewModel.setClipboardSuggestion(clipText)
                }
            }
        } catch (_: Exception) {}
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PitchBlack)
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Header
            Text(text = "Insta-Save", style = Typography.headlineLarge)
            Text(
                text = "Paste link to download Reels, Posts & Stories",
                style = Typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Clipboard Suggestion Chip (Tap-to-read)
            if (uiState.clipboardSuggestion != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(SurfaceDark)
                        .border(0.5.dp, HairlineBorder, RoundedCornerShape(8.dp))
                        .clickable { viewModel.onEvent(HomeEvent.ClipboardChipTapped) }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Default.ContentPaste, contentDescription = null, tint = TextPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Paste from Clipboard",
                        style = Typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { viewModel.onEvent(HomeEvent.DismissClipboardChip) }) {
                        Icon(imageVector = Icons.Default.Clear, contentDescription = "Dismiss", tint = TextMuted)
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Input Field
            OutlinedTextField(
                value = uiState.urlInput,
                onValueChange = { viewModel.onEvent(HomeEvent.UrlInputChanged(it)) },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("https://www.instagram.com/reel/...", color = TextMuted) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = TextPrimary,
                    unfocusedBorderColor = HairlineBorder,
                    focusedContainerColor = SurfaceDark,
                    unfocusedContainerColor = SurfaceDark,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                trailingIcon = {
                    if (uiState.urlInput.isNotEmpty()) {
                        IconButton(onClick = { viewModel.onEvent(HomeEvent.UrlInputChanged("")) }) {
                            Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear", tint = TextMuted)
                        }
                    }
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Extract Button
            Button(
                onClick = { viewModel.onEvent(HomeEvent.ExtractClicked) },
                enabled = uiState.urlValid && uiState.phase == ExtractPhase.IDLE,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = TextPrimary,
                    contentColor = PitchBlack
                )
            ) {
                if (uiState.phase != ExtractPhase.IDLE) {
                    CircularProgressIndicator(
                        modifier = Modifier.height(24.dp).width(24.dp),
                        color = PitchBlack,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    val phaseText = if (uiState.phase == ExtractPhase.EXTRACTING_NATIVE) "Extracting (Native)..." else "Extracting (Fallback)..."
                    Text(text = phaseText, style = Typography.titleMedium)
                } else {
                    Icon(imageVector = Icons.Default.Download, contentDescription = "Fetch")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Fetch Media", style = Typography.titleMedium)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Error Display Card
            if (uiState.error != null) {
                val errorMsg = when (uiState.error) {
                    is ExtractionError.InvalidUrl -> "Invalid Instagram URL format. Please check the link."
                    is ExtractionError.LoginRequired -> "This content requires Instagram login. Use Session Sync."
                    is ExtractionError.PrivateAccount -> "Private Account post. Sign in via Session Sync tab."
                    else -> "Media extraction failed. Please try again."
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(SurfaceDark)
                        .border(0.5.dp, AccentError, RoundedCornerShape(8.dp))
                        .padding(16.dp)
                ) {
                    Text(text = errorMsg, color = AccentError, style = Typography.bodyMedium)
                }
            }

            // Extracted Media Info Card
            if (uiState.mediaInfo != null) {
                val info = uiState.mediaInfo!!
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(SurfaceDark)
                        .border(0.5.dp, HairlineBorder, RoundedCornerShape(8.dp))
                        .clickable { onNavigateToPreview(info.shortcode) }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Default.PlayArrow, contentDescription = "Preview", tint = TextPrimary)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "@${info.author.username}", style = Typography.titleMedium)
                        Text(
                            text = info.caption ?: "No caption",
                            style = Typography.bodyMedium,
                            maxLines = 1
                        )
                        Text(
                            text = "${info.items.size} media item(s) • Engine: ${info.extractedBy.name}",
                            style = Typography.labelMedium,
                            color = TextSecondary
                        )
                    }
                }
            }
        }

        // Format Selection Bottom Sheet Modal
        if (uiState.isFormatSheetOpen && uiState.mediaInfo != null) {
            FormatBottomSheet(
                mediaInfo = uiState.mediaInfo!!,
                onDismiss = { viewModel.onEvent(HomeEvent.DismissFormatSheet) },
                onConfirm = { selectedIndices, formatId ->
                    viewModel.onEvent(HomeEvent.ConfirmDownload(selectedIndices, formatId))
                    onNavigateToDownloads()
                }
            )
        }
    }
}

private fun String?.isNullEmpty(): Boolean = this == null || this.trim().isEmpty()
