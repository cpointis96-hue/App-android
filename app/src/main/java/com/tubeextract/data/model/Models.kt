package com.tubeextract.data.model

data class VideoInfo(
    val id: String,
    val title: String,
    val duration: Long,
    val thumbnail: String?,
    val channel: String?,
    val formats: List<VideoFormat>,
    val subtitleLanguages: List<String>
)

data class VideoFormat(
    val formatId: String,
    val ext: String,
    val resolution: String?,
    val fps: Int?,
    val filesize: Long?,
    val isAudioOnly: Boolean,
    val label: String
)

data class DownloadRequest(
    val url: String,
    val outputType: OutputType,
    val formatId: String = "bestvideo+bestaudio/best",
    val audioFormat: AudioFormat = AudioFormat.MP3,
    val startTime: Long? = null,
    val endTime: Long? = null,
    val subtitleLanguage: String? = null,
    val burnSubtitles: Boolean = false
)

enum class OutputType(val label: String, val icon: String) {
    VIDEO("Vidéo", "video"),
    AUDIO("Audio seulement", "audio"),
    VIDEO_SUBS("Vidéo + Sous-titres", "video_subs"),
    AUDIO_SUBS("Audio + Texte des sous-titres", "audio_subs")
}

enum class AudioFormat(val ext: String, val label: String) {
    MP3("mp3", "MP3"),
    M4A("m4a", "M4A"),
    OPUS("opus", "OPUS"),
    WAV("wav", "WAV")
}

data class DownloadedFile(
    val id: Long = System.currentTimeMillis(),
    val title: String,
    val filePath: String,
    val outputType: OutputType,
    val fileSize: Long,
    val duration: Long?,
    val startTime: Long?,
    val endTime: Long?,
    val createdAt: Long = System.currentTimeMillis()
)

sealed class DownloadState {
    data object Idle : DownloadState()
    data object FetchingInfo : DownloadState()
    data class InfoReady(val info: VideoInfo) : DownloadState()
    data class Downloading(val progress: Float, val message: String) : DownloadState()
    data class Processing(val message: String) : DownloadState()
    data class Success(val file: DownloadedFile) : DownloadState()
    data class Error(val message: String) : DownloadState()
}
