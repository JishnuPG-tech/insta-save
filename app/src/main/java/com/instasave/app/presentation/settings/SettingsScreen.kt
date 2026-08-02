package com.instasave.app.presentation.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.instasave.app.presentation.theme.HairlineBorder
import com.instasave.app.presentation.theme.PitchBlack
import com.instasave.app.presentation.theme.SurfaceDark
import com.instasave.app.presentation.theme.TextMuted
import com.instasave.app.presentation.theme.TextPrimary
import com.instasave.app.presentation.theme.TextSecondary
import com.instasave.app.presentation.theme.Typography

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PitchBlack)
            .padding(16.dp)
            .verticalScroll(scrollState)
    ) {
        Text(text = "Settings", style = Typography.headlineLarge)
        Spacer(modifier = Modifier.height(20.dp))

        // Save Caption Sidecars Toggle
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(SurfaceDark)
                .border(0.5.dp, HairlineBorder, RoundedCornerShape(8.dp))
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Save Caption Sidecars (.txt)", style = Typography.titleMedium)
                    Text(
                        text = "Automatically write post text and hashtags to <basename>.txt alongside downloaded media",
                        style = Typography.bodyMedium,
                        color = TextSecondary
                    )
                }
                Switch(
                    checked = settings.saveCaptionSidecars,
                    onCheckedChange = { viewModel.onSaveCaptionSidecarsToggled(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = PitchBlack,
                        checkedTrackColor = TextPrimary,
                        uncheckedThumbColor = TextMuted,
                        uncheckedTrackColor = SurfaceDark
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Filename Template Editor
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(SurfaceDark)
                .border(0.5.dp, HairlineBorder, RoundedCornerShape(8.dp))
                .padding(16.dp)
        ) {
            Text(text = "Filename Format Template", style = Typography.titleMedium)
            Text(
                text = "Available tokens: {author}, {shortcode}, {index}",
                style = Typography.bodyMedium,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = settings.filenameTemplate,
                onValueChange = { viewModel.onFilenameTemplateChanged(it) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = TextPrimary,
                    unfocusedBorderColor = HairlineBorder,
                    focusedContainerColor = PitchBlack,
                    unfocusedContainerColor = PitchBlack,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                )
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Engine Selector
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(SurfaceDark)
                .border(0.5.dp, HairlineBorder, RoundedCornerShape(8.dp))
                .padding(16.dp)
        ) {
            Text(text = "Extraction Engine Mode", style = Typography.titleMedium)
            Text(
                text = "Current: ${settings.preferredEngine} (Auto-fallback enabled)",
                style = Typography.bodyMedium,
                color = TextSecondary
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // FOSS Legal Disclaimer Panel
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(SurfaceDark)
                .border(0.5.dp, HairlineBorder, RoundedCornerShape(8.dp))
                .padding(16.dp)
        ) {
            Text(text = "About Insta-Save", style = Typography.titleMedium)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = "Version 1.0.0 • Open Source GPL-3.0", style = Typography.labelMedium, color = TextMuted)
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Insta-Save is a personal archival tool inspired by Seal. 100% client-side direct connection to Instagram servers. Zero telemetry or third-party proxy tracking.",
                style = Typography.bodyMedium,
                color = TextSecondary
            )
        }
    }
}
