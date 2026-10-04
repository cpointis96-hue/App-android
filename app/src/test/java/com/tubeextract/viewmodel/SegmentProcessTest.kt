package com.tubeextract.viewmodel

import java.io.File
import java.nio.file.Files
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SegmentProcessTest {
    @Test
    fun passesArgumentsAndLibraryPathAndRequiresARealOutput() {
        val directory = Files.createTempDirectory("segment-test-").toFile()
        try {
            val output = File(directory, "segment.mp4")
            val executable = File(directory, "fake-ffmpeg")
            executable.writeText("#!/bin/sh\n[ \"\$LD_LIBRARY_PATH\" = \"synthetic-libs\" ] || exit 8\n[ \"\$1\" = \"-i\" ] || exit 9\nprintf synthetic > \"\$3\"\n")
            assertTrue(executable.setExecutable(true))
            assertEquals(output, runSegmentProcess(executable, "synthetic-libs",
                listOf("-i", "synthetic-input", output.absolutePath), output))
            assertEquals("synthetic", output.readText())
        } finally {
            directory.deleteRecursively()
        }
    }

    @Test
    fun rejectsFailedOrEmptyOutputWithoutTouchingTheInput() {
        val directory = Files.createTempDirectory("segment-test-").toFile()
        try {
            val input = File(directory, "input.mp4").apply { writeText("original") }
            val output = File(directory, "segment.mp4")
            for (script in listOf("printf partial > \"\$1\"; exit 1", "touch \"\$1\"; exit 0")) {
                output.delete()
                val executable = File(directory, "fake-ffmpeg")
                executable.writeText("#!/bin/sh\n$script\n")
                assertTrue(executable.setExecutable(true))
                val result = runCatching {
                    runSegmentProcess(executable, "", listOf(output.absolutePath), output)
                }
                assertTrue(result.isFailure)
                assertEquals("original", input.readText())
            }
        } finally {
            directory.deleteRecursively()
        }
    }
}
