package com.tubeextract

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tubeextract.data.model.*
import com.tubeextract.ui.screens.*
import com.tubeextract.ui.theme.TubeExtractTheme
import com.tubeextract.viewmodel.DownloadViewModel

class MainActivity : ComponentActivity() {

    private var sharedUrl: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        sharedUrl = extractUrlFromIntent(intent)

        setContent {
            TubeExtractTheme {
                TubeExtractApp(initialUrl = sharedUrl)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        sharedUrl = extractUrlFromIntent(intent)
    }

    private fun extractUrlFromIntent(intent: Intent?): String? {
        if (intent?.action == Intent.ACTION_SEND && intent.type == "text/plain") {
            return intent.getStringExtra(Intent.EXTRA_TEXT)
        }
        return null
    }
}

@Composable
fun TubeExtractApp(initialUrl: String? = null) {
    val viewModel: DownloadViewModel = viewModel()
    val downloadState by viewModel.downloadState.collectAsState()
    val downloadedFiles by viewModel.downloadedFiles.collectAsState()

    var currentScreen by remember { mutableStateOf(Screen.Home) }
    var currentUrl by remember { mutableStateOf(initialUrl ?: "") }

    LaunchedEffect(initialUrl) {
        if (!initialUrl.isNullOrBlank()) {
            viewModel.fetchVideoInfo(initialUrl)
            currentScreen = Screen.Download
        }
    }

    LaunchedEffect(downloadState) {
        when (downloadState) {
            is DownloadState.InfoReady -> currentScreen = Screen.Download
            is DownloadState.Downloading,
            is DownloadState.Processing,
            is DownloadState.Success,
            is DownloadState.Error -> currentScreen = Screen.Progress
            else -> {}
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    icon = { Icon(Icons.Filled.Home, null) },
                    label = { Text("Accueil") },
                    selected = currentScreen == Screen.Home,
                    onClick = {
                        viewModel.resetState()
                        currentScreen = Screen.Home
                    }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Filled.Folder, null) },
                    label = { Text("Bibliothèque") },
                    selected = currentScreen == Screen.Library,
                    onClick = { currentScreen = Screen.Library }
                )
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            when (currentScreen) {
                Screen.Home -> HomeScreen(
                    onUrlSubmit = { url ->
                        currentUrl = url
                        viewModel.fetchVideoInfo(url)
                        currentScreen = Screen.Progress
                    },
                    sharedUrl = initialUrl
                )

                Screen.Progress -> {
                    when (val state = downloadState) {
                        is DownloadState.FetchingInfo,
                        is DownloadState.Downloading,
                        is DownloadState.Processing,
                        is DownloadState.Success,
                        is DownloadState.Error -> ProgressScreen(
                            state = state,
                            onNewDownload = {
                                viewModel.resetState()
                                currentScreen = Screen.Home
                            },
                            onViewLibrary = {
                                viewModel.resetState()
                                currentScreen = Screen.Library
                            }
                        )
                        is DownloadState.InfoReady -> {}
                        else -> {}
                    }
                }

                Screen.Download -> {
                    val state = downloadState
                    if (state is DownloadState.InfoReady) {
                        DownloadScreen(
                            videoInfo = state.info,
                            url = currentUrl,
                            onDownload = { request ->
                                viewModel.startDownload(request)
                                currentScreen = Screen.Progress
                            },
                            onBack = {
                                viewModel.resetState()
                                currentScreen = Screen.Home
                            }
                        )
                    }
                }

                Screen.Library -> LibraryScreen(
                    files = downloadedFiles,
                    onDelete = viewModel::deleteFile
                )
            }
        }
    }
}

enum class Screen { Home, Download, Progress, Library }
