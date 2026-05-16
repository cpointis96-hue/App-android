package com.tubeextract.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tubeextract.ui.theme.RedYT
import com.tubeextract.ui.theme.SurfaceVariant

@Composable
fun HomeScreen(
    onUrlSubmit: (String) -> Unit,
    sharedUrl: String? = null
) {
    var url by remember { mutableStateOf(sharedUrl ?: "") }
    val keyboard = LocalSoftwareKeyboardController.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(48.dp))

        // Logo / Header
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(RedYT),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Filled.Download,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(48.dp)
            )
        }

        Spacer(Modifier.height(20.dp))

        Text(
            "TubeExtract",
            style = MaterialTheme.typography.headlineLarge.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 32.sp
            ),
            color = MaterialTheme.colorScheme.onBackground
        )

        Text(
            "Télécharge • Extrait • Découpe",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(48.dp))

        // URL input card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "Lien YouTube",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    placeholder = { Text("https://youtube.com/watch?v=...") },
                    leadingIcon = {
                        Icon(Icons.Filled.Link, null, tint = RedYT)
                    },
                    trailingIcon = {
                        if (url.isNotEmpty()) {
                            IconButton(onClick = { url = "" }) {
                                Icon(Icons.Filled.Clear, null)
                            }
                        }
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                    keyboardActions = KeyboardActions(onGo = {
                        if (url.isNotBlank()) {
                            keyboard?.hide()
                            onUrlSubmit(url.trim())
                        }
                    }),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = RedYT,
                        focusedLabelColor = RedYT,
                        unfocusedContainerColor = SurfaceVariant
                    )
                )

                Button(
                    onClick = {
                        if (url.isNotBlank()) {
                            keyboard?.hide()
                            onUrlSubmit(url.trim())
                        }
                    },
                    enabled = url.isNotBlank(),
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = RedYT)
                ) {
                    Icon(Icons.Filled.Search, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Analyser la vidéo", fontWeight = FontWeight.SemiBold)
                }
            }
        }

        Spacer(Modifier.height(32.dp))

        // Features list
        FeatureRow(Icons.Filled.VideoFile, "Toutes qualités", "MP4 jusqu'à 4K")
        FeatureRow(Icons.Filled.AudioFile, "Audio uniquement", "MP3, M4A, OPUS, WAV")
        FeatureRow(Icons.Filled.ContentCut, "Découpe précise", "Extrait une séquence à la seconde")
        FeatureRow(Icons.Filled.Subtitles, "Sous-titres", "Télécharge et extrait les sous-titres")
    }
}

@Composable
private fun FeatureRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(RedYT.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = RedYT, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
