package com.suhov.composevideoplayer

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.media3.ui.PlayerView
import com.suhov.composevideoplayer.ui.theme.ComposeVideoPlayerTheme
import com.suhov.composevideoplayer.ui.theme.ratioOfVideo
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val videoExtension = "video/mp4"
    private val selectButtonDescription = "Select video"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent()
    }

    private fun setContent() {
        setContent {
            ComposeVideoPlayerTheme {
                val viewModel = hiltViewModel<MainViewModel>()
                val videoItems by viewModel.videoItems.collectAsState()
                val selectVideoLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.GetContent(),
                    onResult = { uri ->
                        uri?.let(viewModel::addVideoUri)
                    }
                )
                var lifecycle by remember {
                    mutableStateOf(Lifecycle.Event.ON_CREATE)
                }
                val lifecycleOwner = LocalLifecycleOwner.current
                DisposableEffect(lifecycleOwner) {
                    val observer = LifecycleEventObserver { _, event -> lifecycle = event }
                    lifecycleOwner.lifecycle.addObserver(observer)
                    onDispose {
                        lifecycleOwner.lifecycle.removeObserver(observer)
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    VideoPlayer(viewModel, lifecycle)
                    Spacer(modifier = Modifier.height(8.dp))
                    GetFileButton(selectVideoLauncher)
                    Spacer(modifier = Modifier.height(16.dp))
                    ListOfFiles(videoItems, viewModel)
                }
            }
        }
    }

    @Composable
    private fun GetFileButton(selectVideoLauncher: ManagedActivityResultLauncher<String, Uri>) {
        IconButton(onClick = {
            selectVideoLauncher.launch(videoExtension)
        }) {
            Icon(
                imageVector = Icons.Default.FileOpen,
                contentDescription = selectButtonDescription
            )
        }
    }

    @Composable
    private fun ListOfFiles(
        videoItems: List<VideoItem>,
        viewModel: MainViewModel
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth()
        ) {
            items(videoItems) { item ->
                Text(
                    text = item.name,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            viewModel.playVideo(item.contentUri)
                        }
                        .padding(16.dp)
                )
            }
        }
    }

    @Composable
    private fun VideoPlayer(
        viewModel: MainViewModel,
        lifecycle: Lifecycle.Event
    ) {
        AndroidView(
            factory = { context ->
                PlayerView(context).apply {
                    player = viewModel.player
                }
            },
            update = { playerView ->
                when (lifecycle) {
                    Lifecycle.Event.ON_PAUSE -> {
                        playerView.onPause()
                        playerView.player?.pause()
                    }
                    Lifecycle.Event.ON_RESUME -> {
                        playerView.onResume()
                    }
                    else -> Unit
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(ratioOfVideo)
        )
    }
}