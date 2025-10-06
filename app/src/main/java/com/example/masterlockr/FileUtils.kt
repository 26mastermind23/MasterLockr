package com.example.masterlockr

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.MediaStore
import android.util.Log

object FileUtils {

    fun getPath(context: Context, uri: Uri): String? {
        if ("content" == uri.scheme) {
            val projection = arrayOf(MediaStore.Images.Media.DATA)
            var cursor: Cursor? = null

            try {
                cursor = context.contentResolver.query(uri, projection, null, null, null)
                val columnIndex = cursor?.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
                cursor?.moveToFirst()

                return columnIndex?.let {
                    cursor?.getString(it)
                }
            } catch (e: Exception) {
                Log.e("FileUtils", "Error getting path from content URI", e)
            } finally {
                cursor?.close()
            }
        } else if ("file" == uri.scheme) {
            return uri.path
        }

        return null
    }
}
