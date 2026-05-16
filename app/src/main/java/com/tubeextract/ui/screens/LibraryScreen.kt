package com.tubeextract.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tubeextract.data.model.DownloadedFile
import com.tubeextract.data.model.OutputType
import com.tubeextract.ui.components.formatSeconds
import com.tubeextract.ui.theme.RedYT
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun LibraryScreen(
    files: List<DownloadedFile>,
    onDelete: (DownloadedFile) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Ma bibliothèque",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.weight(1f))
            Text(
                "${files.size} fichier${if (files.size > 1) "s" else ""}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (files.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(
                        Icons.Filled.FolderOpen,
                        null,
                        modifier = Modifier.size(72.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                    Text(
                        "Aucun fichier téléchargé",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        "Tes téléchargements apparaîtront ici",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(files, key = { it.id }) { file ->
                    FileItem(file = file, onDelete = { onDelete(file) })
                }
            }
        }
    }
}

@Composable
private fun FileItem(file: DownloadedFile, onDelete: () -> Unit) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Supprimer ?") },
            text = { Text("Supprimer \"${file.title}\" ?") },
            confirmButton = {
                TextButton(onClick = { onDelete(); showDeleteDialog = false }, colors = ButtonDefaults.textButtonColors(contentColor = RedYT)) {
                    Text("Supprimer")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Annuler") }
            }
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Icon based on type
            val icon = when (file.outputType) {
                OutputType.VIDEO, OutputType.VIDEO_SUBS -> Icons.Filled.VideoFile
                OutputType.AUDIO, OutputType.AUDIO_SUBS -> Icons.Filled.AudioFile
            }
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(RedYT.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = RedYT)
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    file.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1
                )
                Spacer(Modifier.height(2.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val sizeKb = file.fileSize / 1024
                    val sizeStr = if (sizeKb > 1024) "${sizeKb / 1024} MB" else "$sizeKb KB"
                    Text(sizeStr, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    file.duration?.let {
                        Text("•", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(formatSeconds(it), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                if (file.startTime != null && file.endTime != null) {
                    Text(
                        "${formatSeconds(file.startTime)} → ${formatSeconds(file.endTime)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = RedYT
                    )
                }
                val dateStr = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(file.createdAt))
                Text(dateStr, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
            }

            IconButton(onClick = { showDeleteDialog = true }) {
                Icon(Icons.Filled.DeleteOutline, "Supprimer", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
