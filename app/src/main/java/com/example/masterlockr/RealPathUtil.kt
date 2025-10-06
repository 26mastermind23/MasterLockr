package com.example.masterlockr

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.MediaStore

object RealPathUtil {

    fun getRealPath(context: Context, uri: Uri): String? {
        val projection = arrayOf(MediaStore.Images.Media.DATA)
        var cursor: Cursor? = null

        try {
            cursor = context.contentResolver.query(uri, projection, null, null, null)
            val columnIndex = cursor?.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
            cursor?.moveToFirst()

            return columnIndex?.let {
                cursor?.getString(it)
            }
        } finally {
            cursor?.close()
        }

        return null
    }
}
