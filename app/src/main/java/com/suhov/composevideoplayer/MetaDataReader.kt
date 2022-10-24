package com.suhov.composevideoplayer

import android.app.Application
import android.net.Uri
import android.provider.MediaStore

data class MetaData(
    val filename: String
)

interface MetaDataReader {
    fun getMetaDataFromUri(contentUri: Uri): MetaData?
}

class MetaDataReaderImpl(
    private val app: Application
): MetaDataReader{
    private val schemeContext = "content"

    override fun getMetaDataFromUri(contentUri: Uri): MetaData? {
        if(contentUri.scheme != schemeContext) return null

        val fileName = app.contentResolver
            .query(
                contentUri,
                arrayOf(MediaStore.Video.VideoColumns.DISPLAY_NAME),
                null,
                null
            )
            ?.use { cursor ->
                val index = cursor.getColumnIndex(MediaStore.Video.VideoColumns.DISPLAY_NAME)
                cursor.moveToFirst()
                cursor.getString(index)
            }
        return fileName?.let { fullFileName ->
            MetaData(
                filename = Uri.parse(fullFileName).lastPathSegment ?: return null
            )
        }
    }

}