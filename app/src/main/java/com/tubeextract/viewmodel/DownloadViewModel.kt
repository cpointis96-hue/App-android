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

                    val response = YoutubeDL.getInstance().getInfo(request)

                    val formats = buildFormats(response)
                    val subtitles = response.subtitles?.keys?.toList() ?: emptyList()
                    val autoSubs = response.automaticCaptions?.keys?.toList() ?: emptyList()

                    val info = VideoInfo(
                        id = response.id ?: "",
                        title = response.title ?: "Vidéo sans titre",
                        duration = response.duration?.toLong() ?: 0L,
                        thumbnail = response.thumbnail,
                        channel = response.uploader,
                        formats = formats,
                        subtitleLanguages = (subtitles + autoSubs).distinct()
                    )
                    _downloadState.value = DownloadState.InfoReady(info)
                } catch (e: Exception) {
                    _downloadState.value = DownloadState.Error("Impossible de récupérer les infos: ${e.message}")
                }
            }
        }
    }

    fun startDownload(request: DownloadRequest) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    _downloadState.value = DownloadState.Downloading(0f, "Préparation...")

                    val dlRequest = YoutubeDLRequest(request.url)
                    dlRequest.addOption("--no-playlist")

                    val tempDir = File(outputDir, "temp_${System.currentTimeMillis()}")
                    tempDir.mkdirs()

                    val outputTemplate = "${tempDir.absolutePath}/%(title)s.%(ext)s"
                    dlRequest.addOption("-o", outputTemplate)

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

                    if (request.outputType == OutputType.AUDIO_SUBS || request.outputType == OutputType.VIDEO_SUBS) {
                        request.subtitleLanguage?.let { lang ->
                            dlRequest.addOption("--write-subs")
                            dlRequest.addOption("--write-auto-subs")
                            dlRequest.addOption("--sub-langs", lang)
                            dlRequest.addOption("--sub-format", "srt/vtt/best")
                            if (request.outputType == OutputType.VIDEO_SUBS && request.burnSubtitles) {
                                dlRequest.addOption("--embed-subs")
                            }
                        }
                    }

                    var downloadedFile: File? = null

                    YoutubeDL.getInstance().execute(dlRequest) { progress, _, line ->
                        _downloadState.value = DownloadState.Downloading(
                            progress / 100f,
                            line ?: "Téléchargement... ${progress.toInt()}%"
                        )
                    }

                    downloadedFile = tempDir.listFiles()?.firstOrNull { file ->
                        file.isFile && !file.name.endsWith(".srt") && !file.name.endsWith(".vtt")
                    }

                    if (downloadedFile == null) {
                        _downloadState.value = DownloadState.Error("Fichier téléchargé introuvable")
                        tempDir.deleteRecursively()
                        return@withContext
                    }

                    val finalFile = if (request.startTime != null && request.endTime != null) {
                        _downloadState.value = DownloadState.Processing("Découpage du segment...")
                        cutSegment(
                            input = downloadedFile,
                            startTime = request.startTime,
                            endTime = request.endTime,
                            outputType = request.outputType,
                            audioFormat = request.audioFormat
                        )
                    } else {
                        val dest = File(outputDir, downloadedFile.name)
                        downloadedFile.copyTo(dest, overwrite = true)
                        dest
                    }

                    if (request.outputType == OutputType.AUDIO_SUBS) {
                        val srtFile = tempDir.listFiles()?.firstOrNull { it.name.endsWith(".srt") || it.name.endsWith(".vtt") }
                        srtFile?.copyTo(File(outputDir, srtFile.name), overwrite = true)
                    }

                    tempDir.deleteRecursively()

                    val downloaded = DownloadedFile(
                        title = finalFile.nameWithoutExtension,
                        filePath = finalFile.absolutePath,
                        outputType = request.outputType,
                        fileSize = finalFile.length(),
                        duration = if (request.startTime != null && request.endTime != null)
                            request.endTime - request.startTime else null,
                        startTime = request.startTime,
                        endTime = request.endTime
                    )

                    _downloadedFiles.value = _downloadedFiles.value + downloaded
                    _downloadState.value = DownloadState.Success(downloaded)

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
        val name = "${input.nameWithoutExtension}_${formatTime(startTime)}-${formatTime(endTime)}.$ext"
        val output = File(outputDir, name)

        val ffmpegArgs = mutableListOf(
            "-i", input.absolutePath,
            "-ss", startTime.toString(),
            "-to", endTime.toString()
        )

        if (outputType == OutputType.AUDIO || outputType == OutputType.AUDIO_SUBS) {
            ffmpegArgs.addAll(listOf("-vn", "-acodec", "copy"))
        } else {
            ffmpegArgs.addAll(listOf("-c", "copy"))
        }

        ffmpegArgs.addAll(listOf("-avoid_negative_ts", "make_zero", output.absolutePath))

        FFmpeg.getInstance().execute(ffmpegArgs.toTypedArray(), null)
        return output
    }

    private fun formatTime(seconds: Long): String {
        val h = seconds / 3600
        val m = (seconds % 3600) / 60
        val s = seconds % 60
        return if (h > 0) "%02d%02d%02d".format(h, m, s) else "%02d%02d".format(m, s)
    }

    private fun buildFormats(response: com.yausername.youtubedl_android.mapper.VideoInfo): List<VideoFormat> {
        val formats = mutableListOf<VideoFormat>()

        formats.add(VideoFormat("bestvideo[ext=mp4]+bestaudio[ext=m4a]/best[ext=mp4]/best", "mp4", "Meilleure qualité", null, null, false, "Meilleure qualité (MP4)"))
        formats.add(VideoFormat("bestvideo[height<=1080]+bestaudio/best[height<=1080]", "mp4", "1080p", null, null, false, "1080p Full HD"))
        formats.add(VideoFormat("bestvideo[height<=720]+bestaudio/best[height<=720]", "mp4", "720p", null, null, false, "720p HD"))
        formats.add(VideoFormat("bestvideo[height<=480]+bestaudio/best[height<=480]", "mp4", "480p", null, null, false, "480p"))
        formats.add(VideoFormat("bestaudio/best", "m4a", null, null, null, true, "Audio uniquement"))

        response.formats?.forEach { f ->
            if (f.vcodec != null && f.vcodec != "none" && f.acodec != null && f.acodec != "none") {
                val res = f.resolution ?: "${f.width}x${f.height}"
                formats.add(VideoFormat(
                    formatId = f.formatId ?: "",
                    ext = f.ext ?: "mp4",
                    resolution = res,
                    fps = f.fps?.toInt(),
                    filesize = f.filesize,
                    isAudioOnly = false,
                    label = "$res ${f.ext?.uppercase() ?: ""}"
                ))
            }
        }

        return formats
    }

    fun resetState() {
        _downloadState.value = DownloadState.Idle
    }

    fun deleteFile(file: DownloadedFile) {
        File(file.filePath).delete()
        _downloadedFiles.value = _downloadedFiles.value.filter { it.id != file.id }
    }
}
