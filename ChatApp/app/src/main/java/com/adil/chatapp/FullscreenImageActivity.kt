package com.adil.chatapp

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import androidx.appcompat.app.AppCompatActivity
import com.adil.chatapp.databinding.ActivityFullscreenImageBinding

class FullscreenImageActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_IMAGE_BASE64 = "extra_image_base64"
    }

    private lateinit var binding: ActivityFullscreenImageBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFullscreenImageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val imageBase64 = intent.getStringExtra(EXTRA_IMAGE_BASE64)
        if (imageBase64 != null) {
            val bitmap = decodeBase64ToBitmap(imageBase64)
            binding.ivFullscreenImage.setImageBitmap(bitmap)
        }

        binding.ivFullscreenImage.setOnClickListener {
            finish()
        }
    }

    private fun decodeBase64ToBitmap(base64: String): Bitmap? {
        return try {
            val decodedBytes = Base64.decode(base64, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
        } catch (e: Exception) {
            null
        }
    }
}
