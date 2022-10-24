package com.suhov.composevideoplayer

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val metaDataReader: MetaDataReader,
    val player:Player
):ViewModel() {
    private val videoUriPath = "videoUris"
    private val defaultFileName = "No name"
    private val videoUris = savedStateHandle.getStateFlow(videoUriPath, emptyList<Uri>())
    private val stopTimeoutState = 5000L

    val videoItems = videoUris.map { uris ->
        uris.map { uri ->
            VideoItem(
                contentUri = uri,
                mediaItem = MediaItem.fromUri(uri),
                name = metaDataReader.getMetaDataFromUri(uri)?.filename ?: defaultFileName
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(stopTimeoutState), emptyList())

    init {
        player.prepare()
    }

    fun addVideoUri(uri: Uri) {
        savedStateHandle[videoUriPath] = videoUris.value + uri
        player.addMediaItem(MediaItem.fromUri(uri))
    }

    fun playVideo(uri: Uri) {
        player.setMediaItem(
            videoItems.value.find { it.contentUri == uri }?.mediaItem ?: return
        )
    }

    override fun onCleared() {
        super.onCleared()
        player.release()
    }
}