package com.suhov.composevideoplayer

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.NotificationCompat
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
    private var clickCount = 1

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
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp)
                    ) {
                        TextButton(
                            modifier = Modifier.defaultMinSize(
                                minHeight = 50.dp,
                                minWidth = 50.dp
                            ).border(BorderStroke(1.dp, Color.Red), shape = RectangleShape).background(color = Color.Cyan),
                            onClick = { createPush() }
                        ) {
                            Text(text = "I will send pushes")
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    ListOfFiles(videoItems, viewModel)
                }
            }
        }
    }

    private fun createPush() {
        val channelID = "myRoom"
        val noticeBuilder = NotificationCompat.Builder(this@MainActivity, channelID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Hi there!")
            .setContentText("I am here! Your click on button $clickCount times")
            .setAutoCancel(true)

        val channel = NotificationChannel(
            /* id = */ channelID,
            /* name = */ "Some chanel which pushing",
            /* importance = */ NotificationManager.IMPORTANCE_DEFAULT
        )

        val intent = Intent(this@MainActivity, MainActivity::class.java)
        val pendingIntent =
            PendingIntent.getActivity(this@MainActivity, 0, intent, PendingIntent.FLAG_IMMUTABLE)
        noticeBuilder.setContentIntent(pendingIntent)

        val notification = noticeBuilder.build()
        val noticeManager = getSystemService(NOTIFICATION_SERVICE) as? NotificationManager
        noticeManager?.createNotificationChannel(channel)
        noticeManager?.notify(1, notification)
        clickCount++
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