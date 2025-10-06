package com.example.masterlockr

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import jp.wasabeef.glide.transformations.BlurTransformation

class DisplayImageActivity : AppCompatActivity() {

    private lateinit var imageViewBlurred: ImageView
    private lateinit var btnShare: Button
    private lateinit var imageUri: Uri

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_display_image)

        imageViewBlurred = findViewById(R.id.imageViewBlurred)
        btnShare = findViewById(R.id.btnShare)

        // Get the image URI from the intent
        imageUri = intent.getParcelableExtra(EXTRA_IMAGE_URI)!!

        // Load the original image into the blurred image view
        Glide.with(this)
            .load(imageUri)
            .transform(BlurTransformation(40, 6))
            .into(imageViewBlurred)

        // Share button click listener
        btnShare.setOnClickListener {
            shareImage(imageUri)
        }
    }

    private fun shareImage(imageUri: Uri) {
        // Create a share intent
        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_STREAM, imageUri)
            type = "image/jpeg"
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        }

        // Start the activity with the share intent
        startActivity(Intent.createChooser(shareIntent, "Share Image"))
    }

    companion object {
        const val EXTRA_IMAGE_URI = "com.example.masterlockr.EXTRA_IMAGE_URI"
    }
}
