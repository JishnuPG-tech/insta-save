package com.instasave.app.presentation.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.instasave.app.domain.model.MediaInfo
import com.instasave.app.presentation.theme.HairlineBorder
import com.instasave.app.presentation.theme.PitchBlack
import com.instasave.app.presentation.theme.SurfaceDark
import com.instasave.app.presentation.theme.TextMuted
import com.instasave.app.presentation.theme.TextPrimary
import com.instasave.app.presentation.theme.Typography

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormatBottomSheet(
    mediaInfo: MediaInfo,
    onDismiss: () -> Unit,
    onConfirm: (selectedIndices: List<Int>, formatId: String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val selectedIndices = remember {
        mutableStateListOf<Int>().apply {
            addAll(mediaInfo.items.indices)
        }
    }

    val availableFormats = mediaInfo.items.firstOrNull()?.formats ?: emptyList()
    var selectedFormatId by remember {
        mutableStateOf(availableFormats.firstOrNull()?.id ?: "")
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = PitchBlack,
        scrimColor = PitchBlack.copy(alpha = 0.8f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Text(
                text = "Download Options",
                style = Typography.headlineMedium
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Carousel Item Selection (if carousel)
            if (mediaInfo.items.size > 1) {
                Text(
                    text = "Select Items (${selectedIndices.size}/${mediaInfo.items.size}):",
                    style = Typography.titleMedium
                )
                Spacer(modifier = Modifier.height(8.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    itemsIndexed(mediaInfo.items) { index, item ->
                        val isChecked = selectedIndices.contains(index)
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(SurfaceDark)
                                .border(0.5.dp, HairlineBorder, RoundedCornerShape(8.dp))
                                .clickable {
                                    if (isChecked) selectedIndices.remove(index) else selectedIndices.add(index)
                                }
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isChecked,
                                onCheckedChange = { checked ->
                                    if (checked) selectedIndices.add(index) else selectedIndices.remove(index)
                                },
                                colors = CheckboxDefaults.colors(checkedColor = TextPrimary)
                            )
                            AsyncImage(
                                model = item.thumbnailUrl,
                                contentDescription = "Item $index",
                                modifier = Modifier
                                    .width(48.dp)
                                    .height(48.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "#${index + 1}", style = Typography.bodyMedium)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }

            // Format Selection
            Text(text = "Choose Quality & Format:", style = Typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))

            availableFormats.forEach { format ->
                val isSelected = selectedFormatId == format.id
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) SurfaceDark else PitchBlack)
                        .border(0.5.dp, if (isSelected) TextPrimary else HairlineBorder, RoundedCornerShape(8.dp))
                        .clickable { selectedFormatId = format.id }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = isSelected,
                        onClick = { selectedFormatId = format.id },
                        colors = RadioButtonDefaults.colors(selectedColor = TextPrimary)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        val label = when {
                            format.container == "mp3" -> "Audio Extract (MP3)"
                            format.height != null -> "${format.height}p (${format.container.uppercase()})"
                            else -> "Original (${format.container.uppercase()})"
                        }
                        Text(text = label, style = Typography.bodyLarge)
                        Text(text = "Container: ${format.container}", style = Typography.labelMedium)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Action CTA Button
            Button(
                onClick = {
                    if (selectedIndices.isNotEmpty() && selectedFormatId.isNotEmpty()) {
                        onConfirm(selectedIndices.toList(), selectedFormatId)
                    }
                },
                enabled = selectedIndices.isNotEmpty() && selectedFormatId.isNotEmpty(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = TextPrimary,
                    contentColor = PitchBlack
                )
            ) {
                Icon(imageVector = Icons.Default.Download, contentDescription = "Download")
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "Start Download", style = Typography.titleMedium)
            }
        }
    }
}
