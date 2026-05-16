package com.tubeextract.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.tubeextract.data.model.*
import com.tubeextract.ui.components.TimeRangePicker
import com.tubeextract.ui.components.formatSeconds
import com.tubeextract.ui.components.parseTime
import com.tubeextract.ui.theme.RedYT
import com.tubeextract.ui.theme.SurfaceVariant

@Composable
fun DownloadScreen(
    videoInfo: VideoInfo,
    onDownload: (DownloadRequest) -> Unit,
    onBack: () -> Unit,
    url: String
) {
    var selectedOutputType by remember { mutableStateOf(OutputType.VIDEO) }
    var selectedFormat by remember { mutableStateOf(videoInfo.formats.firstOrNull()?.formatId ?: "bestvideo+bestaudio/best") }
    var selectedAudioFormat by remember { mutableStateOf(AudioFormat.MP3) }
    var startTime by remember { mutableStateOf<Long?>(null) }
    var endTime by remember { mutableStateOf<Long?>(null) }
    var selectedSubtitle by remember { mutableStateOf(videoInfo.subtitleLanguages.firstOrNull()) }
    var burnSubtitles by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()

    Column(modifier = Modifier.fillMaxSize()) {
        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBack, "Retour", tint = MaterialTheme.colorScheme.onBackground)
            }
            Text(
                "Options de téléchargement",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Video info card
            VideoInfoCard(videoInfo)

            // Output type selector
            SectionCard("Que veux-tu extraire ?") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutputType.entries.forEach { type ->
                        OutputTypeOption(
                            type = type,
                            selected = selectedOutputType == type,
                            onClick = { selectedOutputType = type }
                        )
                    }
                }
            }

            // Video quality (only for video modes)
            if (selectedOutputType == OutputType.VIDEO || selectedOutputType == OutputType.VIDEO_SUBS) {
                SectionCard("Qualité vidéo") {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        videoInfo.formats.filter { !it.isAudioOnly }.forEach { format ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { selectedFormat = format.formatId }
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = selectedFormat == format.formatId,
                                    onClick = { selectedFormat = format.formatId },
                                    colors = RadioButtonDefaults.colors(selectedColor = RedYT)
                                )
                                Spacer(Modifier.width(8.dp))
                                Column {
                                    Text(format.label, style = MaterialTheme.typography.bodyMedium)
                                    format.filesize?.let {
                                        Text(
                                            "~${it / 1024 / 1024} MB",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Audio format (for audio modes)
            if (selectedOutputType == OutputType.AUDIO || selectedOutputType == OutputType.AUDIO_SUBS) {
                SectionCard("Format audio") {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AudioFormat.entries.forEach { fmt ->
                            FilterChip(
                                selected = selectedAudioFormat == fmt,
                                onClick = { selectedAudioFormat = fmt },
                                label = { Text(fmt.label) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = RedYT,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                )
                            )
                        }
                    }
                }
            }

            // Subtitles options
            if (selectedOutputType == OutputType.VIDEO_SUBS || selectedOutputType == OutputType.AUDIO_SUBS) {
                SectionCard("Sous-titres") {
                    if (videoInfo.subtitleLanguages.isEmpty()) {
                        Text(
                            "Aucun sous-titre disponible pour cette vidéo",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Langue:", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Row(
                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                videoInfo.subtitleLanguages.take(10).forEach { lang ->
                                    FilterChip(
                                        selected = selectedSubtitle == lang,
                                        onClick = { selectedSubtitle = lang },
                                        label = { Text(lang.uppercase()) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = RedYT,
                                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                        )
                                    )
                                }
                            }
                            if (selectedOutputType == OutputType.VIDEO_SUBS) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(
                                        checked = burnSubtitles,
                                        onCheckedChange = { burnSubtitles = it },
                                        colors = CheckboxDefaults.colors(checkedColor = RedYT)
                                    )
                                    Text("Incruster dans la vidéo (hardcode)", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }
            }

            // Time range picker
            SectionCard("Découpe temporelle") {
                TimeRangePicker(
                    videoDuration = videoInfo.duration,
                    startTime = startTime,
                    endTime = endTime,
                    onStartChange = { startTime = it },
                    onEndChange = { endTime = it }
                )
            }

            Spacer(Modifier.height(8.dp))
        }

        // Download button
        val isTimeValid = (startTime == null && endTime == null) ||
            (startTime != null && endTime != null && endTime!! > startTime!!)

        Button(
            onClick = {
                onDownload(
                    DownloadRequest(
                        url = url,
                        outputType = selectedOutputType,
                        formatId = selectedFormat,
                        audioFormat = selectedAudioFormat,
                        startTime = startTime,
                        endTime = endTime,
                        subtitleLanguage = selectedSubtitle,
                        burnSubtitles = burnSubtitles
                    )
                )
            },
            enabled = isTimeValid,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .height(56.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = RedYT)
        ) {
            Icon(Icons.Filled.Download, null)
            Spacer(Modifier.width(8.dp))
            Text("Télécharger", fontWeight = FontWeight.Bold)
            startTime?.let { s ->
                endTime?.let { e ->
                    Text("  (${formatSeconds(s)} → ${formatSeconds(e)})", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@Composable
private fun VideoInfoCard(info: VideoInfo) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
            info.thumbnail?.let {
                AsyncImage(
                    model = it,
                    contentDescription = null,
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(10.dp)),
                    contentScale = ContentScale.Crop
                )
                Spacer(Modifier.width(12.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    info.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2
                )
                Spacer(Modifier.height(4.dp))
                info.channel?.let {
                    Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Chip(Icons.Filled.Timer, formatSeconds(info.duration))
                    if (info.subtitleLanguages.isNotEmpty()) {
                        Chip(Icons.Filled.Subtitles, "${info.subtitleLanguages.size} langues")
                    }
                }
            }
        }
    }
}

@Composable
private fun Chip(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String) {
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(icon, null, modifier = Modifier.size(12.dp), tint = RedYT)
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun OutputTypeOption(type: OutputType, selected: Boolean, onClick: () -> Unit) {
    val icon = when (type) {
        OutputType.VIDEO -> Icons.Filled.VideoFile
        OutputType.AUDIO -> Icons.Filled.AudioFile
        OutputType.VIDEO_SUBS -> Icons.Filled.ClosedCaption
        OutputType.AUDIO_SUBS -> Icons.Filled.RecordVoiceOver
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) RedYT.copy(alpha = 0.15f) else SurfaceVariant)
            .border(
                width = if (selected) 1.5.dp else 0.dp,
                color = if (selected) RedYT else androidx.compose.ui.graphics.Color.Transparent,
                shape = RoundedCornerShape(10.dp)
            )
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(icon, null, tint = if (selected) RedYT else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp))
        Text(type.label, style = MaterialTheme.typography.bodyMedium, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
        Spacer(Modifier.weight(1f))
        if (selected) Icon(Icons.Filled.CheckCircle, null, tint = RedYT, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}
