package com.tubeextract.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.tubeextract.ui.theme.RedYT
import com.tubeextract.ui.theme.SurfaceVariant

@Composable
fun TimeRangePicker(
    videoDuration: Long,
    startTime: Long?,
    endTime: Long?,
    onStartChange: (Long?) -> Unit,
    onEndChange: (Long?) -> Unit,
    modifier: Modifier = Modifier
) {
    var startText by remember(startTime) { mutableStateOf(startTime?.let { formatSeconds(it) } ?: "") }
    var endText by remember(endTime) { mutableStateOf(endTime?.let { formatSeconds(it) } ?: "") }
    var useRange by remember { mutableStateOf(startTime != null || endTime != null) }

    Column(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = useRange,
                onCheckedChange = { checked ->
                    useRange = checked
                    if (!checked) {
                        startText = ""
                        endText = ""
                        onStartChange(null)
                        onEndChange(null)
                    }
                },
                colors = CheckboxDefaults.colors(checkedColor = RedYT)
            )
            Text(
                "Extraire un segment",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.weight(1f))
            Text(
                "Durée totale: ${formatSeconds(videoDuration)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (useRange) {
            Spacer(Modifier.height(12.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                TimeField(
                    label = "Début",
                    value = startText,
                    onValueChange = { text ->
                        startText = text
                        parseTime(text)?.let { onStartChange(it) }
                    },
                    modifier = Modifier.weight(1f)
                )
                Text(
                    "→",
                    modifier = Modifier.align(Alignment.CenterVertically),
                    style = MaterialTheme.typography.titleMedium,
                    color = RedYT
                )
                TimeField(
                    label = "Fin",
                    value = endText,
                    onValueChange = { text ->
                        endText = text
                        parseTime(text)?.let { onEndChange(it) }
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            val start = parseTime(startText)
            val end = parseTime(endText)
            if (start != null && end != null) {
                val duration = end - start
                if (duration > 0) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Segment de ${formatSeconds(duration)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = RedYT
                    )
                } else if (duration <= 0) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "La fin doit être après le début",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
            Text(
                "Format: mm:ss ou hh:mm:ss (ex: 01:30 ou 1:20:00)",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun TimeField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = { Text("00:00") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = RedYT,
            focusedLabelColor = RedYT,
            unfocusedContainerColor = SurfaceVariant
        ),
        modifier = modifier
    )
}

fun formatSeconds(totalSeconds: Long): String {
    val h = totalSeconds / 3600
    val m = (totalSeconds % 3600) / 60
    val s = totalSeconds % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
}

fun parseTime(input: String): Long? {
    val clean = input.trim()
    return try {
        val parts = clean.split(":").map { it.toLong() }
        when (parts.size) {
            2 -> parts[0] * 60 + parts[1]
            3 -> parts[0] * 3600 + parts[1] * 60 + parts[2]
            1 -> parts[0]
            else -> null
        }
    } catch (e: Exception) {
        null
    }
}
