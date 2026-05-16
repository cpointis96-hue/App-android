package com.tubeextract.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import com.tubeextract.data.model.DownloadState
import com.tubeextract.ui.theme.RedYT
import java.io.File

@Composable
fun ProgressScreen(
    state: DownloadState,
    onNewDownload: () -> Unit,
    onViewLibrary: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        when (state) {
            is DownloadState.FetchingInfo -> {
                LoadingState("Analyse de la vidéo...", "Récupération des informations")
            }
            is DownloadState.Downloading -> {
                DownloadingState(state.progress, state.message)
            }
            is DownloadState.Processing -> {
                LoadingState("Traitement en cours...", state.message)
            }
            is DownloadState.Success -> {
                SuccessState(state.file, onNewDownload, onViewLibrary)
            }
            is DownloadState.Error -> {
                ErrorState(state.message, onNewDownload)
            }
            else -> {}
        }
    }
}

@Composable
private fun LoadingState(title: String, subtitle: String) {
    val infiniteTransition = rememberInfiniteTransition(label = "loading")
    val angle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(1200, easing = LinearEasing)),
        label = "spin"
    )

    CircularProgressIndicator(
        modifier = Modifier.size(80.dp),
        color = RedYT,
        strokeWidth = 6.dp
    )
    Spacer(Modifier.height(24.dp))
    Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(8.dp))
    Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
}

@Composable
private fun DownloadingState(progress: Float, message: String) {
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(120.dp)) {
        CircularProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxSize(),
            color = RedYT,
            strokeWidth = 8.dp,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
        Text(
            "${(progress * 100).toInt()}%",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = RedYT
        )
    }
    Spacer(Modifier.height(24.dp))
    Text("Téléchargement...", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(8.dp))
    Text(
        message,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        maxLines = 2
    )
}

@Composable
private fun SuccessState(file: DownloadedFile, onNewDownload: () -> Unit, onViewLibrary: () -> Unit) {
    Box(
        modifier = Modifier
            .size(100.dp)
            .clip(RoundedCornerShape(50.dp))
            .background(RedYT.copy(alpha = 0.15f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(Icons.Filled.CheckCircle, null, tint = RedYT, modifier = Modifier.size(60.dp))
    }
    Spacer(Modifier.height(24.dp))
    Text("Téléchargement terminé !", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(8.dp))
    Text(
        file.title,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        maxLines = 2
    )
    Spacer(Modifier.height(4.dp))

    val sizeKb = file.fileSize / 1024
    val sizeStr = if (sizeKb > 1024) "${sizeKb / 1024} MB" else "$sizeKb KB"
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(sizeStr, style = MaterialTheme.typography.labelMedium, color = RedYT)
        file.duration?.let {
            Text("Durée: ${formatSeconds(it)}", style = MaterialTheme.typography.labelMedium, color = RedYT)
        }
    }

    Spacer(Modifier.height(32.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedButton(
            onClick = onViewLibrary,
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedButtonDefaults.outlinedButtonColors(contentColor = RedYT),
            border = ButtonDefaults.outlinedButtonBorder.copy()
        ) {
            Icon(Icons.Filled.Folder, null)
            Spacer(Modifier.width(6.dp))
            Text("Bibliothèque")
        }
        Button(
            onClick = onNewDownload,
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = RedYT)
        ) {
            Icon(Icons.Filled.Add, null)
            Spacer(Modifier.width(6.dp))
            Text("Nouveau")
        }
    }
}

@Composable
private fun ErrorState(message: String, onRetry: () -> Unit) {
    Box(
        modifier = Modifier
            .size(100.dp)
            .clip(RoundedCornerShape(50.dp))
            .background(MaterialTheme.colorScheme.error.copy(alpha = 0.15f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(Icons.Filled.Error, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(60.dp))
    }
    Spacer(Modifier.height(24.dp))
    Text("Une erreur s'est produite", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(8.dp))
    Text(message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
    Spacer(Modifier.height(32.dp))
    Button(
        onClick = onRetry,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = RedYT)
    ) {
        Icon(Icons.Filled.Refresh, null)
        Spacer(Modifier.width(8.dp))
        Text("Réessayer")
    }
}
