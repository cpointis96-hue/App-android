package com.tubeextract.viewmodel

import android.app.Application
import android.os.Environment
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tubeextract.data.model.*
import com.yausername.ffmpeg.FFmpeg
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File

class DownloadViewModel(application: Application) : AndroidViewModel(application) {

    private val _downloadState = MutableStateFlow<DownloadState>(DownloadState.Idle)
    val downloadState: StateFlow<DownloadState> = _downloadState

    private val _downloadedFiles = MutableStateFlow<List<DownloadedFile>>(emptyList())
    val downloadedFiles: StateFlow<List<DownloadedFile>> = _downloadedFiles

    private val outputDir: File
        get() = File(
            getApplication<Application>().getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
            "TubeExtract"
        ).also { it.mkdirs() }

    fun fetchVideoInfo(url: String) {
        viewModelScope.launch {
            _downloadState.value = DownloadState.FetchingInfo
            withContext(Dispatchers.IO) {
                try {
                    val request = YoutubeDLRequest(url)
                    request.addOption("--dump-json")
                    request.addOption("--no-playlist")

                    val app = getApplication<Application>()
                    YoutubeDL.getInstance().init(app)
                    FFmpeg.getInstance().init(app)
                    val rawInfo = YoutubeDL.getInstance().execute(request).out
                    val metadata = JSONObject(rawInfo)

                    val subtitleLanguages = mutableListOf<String>()
                    try {
                        metadata.optJSONObject("subtitles")?.keys()?.let { keys ->
                            keys.forEach { subtitleLanguages.add(it) }
                        }
                    } catch (_: Exception) {}
                    try {
                        metadata.optJSONObject("automatic_captions")?.keys()?.let { keys ->
                            keys.forEach { if (!subtitleLanguages.contains(it)) subtitleLanguages.add(it) }
                        }
                    } catch (_: Exception) {}

                    val formats = mutableListOf<VideoFormat>()
                    formats.add(VideoFormat("bestvideo[ext=mp4]+bestaudio[ext=m4a]/best[ext=mp4]/best", "mp4", "Auto", null, null, false, "Meilleure qualité"))
                    formats.add(VideoFormat("bestvideo[height<=1080]+bestaudio/best[height<=1080]", "mp4", "1080p", null, null, false, "1080p Full HD"))
                    formats.add(VideoFormat("bestvideo[height<=720]+bestaudio/best[height<=720]", "mp4", "720p", null, null, false, "720p HD"))
                    formats.add(VideoFormat("bestvideo[height<=480]+bestaudio/best[height<=480]", "mp4", "480p", null, null, false, "480p"))
                    formats.add(VideoFormat("bestaudio/best", "m4a", null, null, null, true, "Audio uniquement"))

                    val duration = metadata.optLong("duration", 0L)

                    val info = VideoInfo(
                        id = metadata.optString("id", ""),
                        title = metadata.optString("title", "Vidéo sans titre"),
                        duration = duration,
                        thumbnail = if (metadata.isNull("thumbnail")) null else metadata.optString("thumbnail"),
                        channel = if (metadata.isNull("uploader")) null else metadata.optString("uploader"),
                        formats = formats,
                        subtitleLanguages = subtitleLanguages
                    )
                    _downloadState.value = DownloadState.InfoReady(info)
                } catch (e: Exception) {
                    _downloadState.value = DownloadState.Error("Impossible de récupérer les infos:\n${e.message}")
                }
            }
        }
    }

    fun startDownload(request: DownloadRequest) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    _downloadState.value = DownloadState.Downloading(0f, "Préparation...")
                    val app = getApplication<Application>()
                    YoutubeDL.getInstance().init(app)
                    FFmpeg.getInstance().init(app)

                    val tempDir = File(outputDir, "temp_${System.currentTimeMillis()}")
                    tempDir.mkdirs()

                    val dlRequest = YoutubeDLRequest(request.url)
                    dlRequest.addOption("--no-playlist")
                    dlRequest.addOption("-o", "${tempDir.absolutePath}/%(title)s.%(ext)s")

                    when (request.outputType) {
                        OutputType.AUDIO, OutputType.AUDIO_SUBS -> {
                            dlRequest.addOption("-x")
                            dlRequest.addOption("--audio-format", request.audioFormat.ext)
                            dlRequest.addOption("--audio-quality", "0")
                        }
                        OutputType.VIDEO, OutputType.VIDEO_SUBS -> {
                            dlRequest.addOption("-f", request.formatId)
                            dlRequest.addOption("--merge-output-format", "mp4")
                        }
                    }

                    if (request.subtitleLanguage != null &&
                        (request.outputType == OutputType.VIDEO_SUBS || request.outputType == OutputType.AUDIO_SUBS)) {
                        dlRequest.addOption("--write-subs")
                        dlRequest.addOption("--write-auto-subs")
                        dlRequest.addOption("--sub-langs", request.subtitleLanguage)
                        dlRequest.addOption("--sub-format", "srt/vtt/best")
                        if (request.outputType == OutputType.VIDEO_SUBS && request.burnSubtitles) {
                            dlRequest.addOption("--embed-subs")
                        }
                    }

                    YoutubeDL.getInstance().execute(dlRequest) { progress, _, line ->
                        _downloadState.value = DownloadState.Downloading(
                            progress / 100f,
                            line ?: "Téléchargement... ${progress.toInt()}%"
                        )
                    }

                    val downloadedFile = tempDir.listFiles()
                        ?.filter { it.isFile && !it.name.endsWith(".srt") && !it.name.endsWith(".vtt") && !it.name.endsWith(".part") }
                        ?.maxByOrNull { it.length() }

                    if (downloadedFile == null) {
                        _downloadState.value = DownloadState.Error("Fichier introuvable après téléchargement")
                        tempDir.deleteRecursively()
                        return@withContext
                    }

                    val finalFile = if (request.startTime != null && request.endTime != null) {
                        _downloadState.value = DownloadState.Processing("Découpage du segment...")
                        cutSegment(downloadedFile, request.startTime, request.endTime, request.outputType, request.audioFormat)
                    } else {
                        val dest = File(outputDir, downloadedFile.name)
                        downloadedFile.copyTo(dest, overwrite = true)
                        dest
                    }

                    if (request.outputType == OutputType.AUDIO_SUBS) {
                        tempDir.listFiles()
                            ?.firstOrNull { f -> f.name.endsWith(".srt") || f.name.endsWith(".vtt") }
                            ?.let { srt -> srt.copyTo(File(outputDir, srt.name), overwrite = true) }
                    }

                    tempDir.deleteRecursively()

                    val result = DownloadedFile(
                        title = finalFile.nameWithoutExtension,
                        filePath = finalFile.absolutePath,
                        outputType = request.outputType,
                        fileSize = finalFile.length(),
                        duration = if (request.startTime != null && request.endTime != null)
                            request.endTime - request.startTime else null,
                        startTime = request.startTime,
                        endTime = request.endTime
                    )
                    _downloadedFiles.value = _downloadedFiles.value + result
                    _downloadState.value = DownloadState.Success(result)

                } catch (e: Exception) {
                    _downloadState.value = DownloadState.Error("Erreur: ${e.message}")
                }
            }
        }
    }

    private fun cutSegment(
        input: File,
        startTime: Long,
        endTime: Long,
        outputType: OutputType,
        audioFormat: AudioFormat
    ): File {
        val ext = when (outputType) {
            OutputType.AUDIO, OutputType.AUDIO_SUBS -> audioFormat.ext
            else -> "mp4"
        }
        val name = "${input.nameWithoutExtension}_${startTime}s-${endTime}s.$ext"
        val output = File(outputDir, name)

        val args = if (outputType == OutputType.AUDIO || outputType == OutputType.AUDIO_SUBS) {
            arrayOf("-y", "-i", input.absolutePath, "-ss", startTime.toString(), "-to", endTime.toString(), "-vn", "-acodec", "copy", output.absolutePath)
        } else {
            arrayOf("-y", "-i", input.absolutePath, "-ss", startTime.toString(), "-to", endTime.toString(), "-c", "copy", output.absolutePath)
        }

        val app = getApplication<Application>()
        val packages = File(app.noBackupFilesDir, "youtubedl-android/packages")
        val libraryPath = listOf("python", "ffmpeg", "aria2c")
            .joinToString(":") { File(packages, "$it/usr/lib").absolutePath }
        return runSegmentProcess(
            File(app.applicationInfo.nativeLibraryDir, "libffmpeg.so"),
            libraryPath, args.toList(), output
        )
    }

    fun resetState() {
        _downloadState.value = DownloadState.Idle
    }

    fun deleteFile(file: DownloadedFile) {
        File(file.filePath).delete()
        _downloadedFiles.value = _downloadedFiles.value.filter { it.id != file.id }
    }
}
