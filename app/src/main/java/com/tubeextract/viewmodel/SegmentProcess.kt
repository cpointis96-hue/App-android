package com.tubeextract.viewmodel

import java.io.File

internal fun runSegmentProcess(
    executable: File,
    libraryPath: String,
    arguments: List<String>,
    output: File
): File {
    val builder = ProcessBuilder(listOf(executable.absolutePath) + arguments)
        .redirectErrorStream(true)
    builder.environment()["LD_LIBRARY_PATH"] = libraryPath
    val process = builder.start()
    val log = process.inputStream.bufferedReader().use { it.readText() }
    check(process.waitFor() == 0 && output.isFile && output.length() > 0) {
        "Échec du découpage FFmpeg : ${log.takeLast(500)}"
    }
    return output
}
