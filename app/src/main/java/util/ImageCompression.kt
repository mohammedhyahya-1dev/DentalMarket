package com.dentalmarket.app.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.ByteArrayOutputStream
import java.io.InputStream

// Downscales and re-encodes a picked image before it ever reaches Storage —
// a raw phone-camera photo is routinely 3-8MB, and identityVerifications
// submissions upload 4 of them; Blaze billing is real now, and a capped
// upload is faster on a poor connection regardless of cost. storage.rules'
// own size check is the server-side backstop for this, same "client check +
// rules backstop" split as everywhere else in this app.
object ImageCompression {
    private const val MAX_DIMENSION = 1600
    private const val JPEG_QUALITY = 80

    // Two-pass decode: first read just the bounds (no full bitmap allocated)
    // to compute a downsample factor, then decode for real at that size —
    // avoids ever holding a full-resolution bitmap in memory for a photo
    // that's going to be shrunk anyway.
    fun compressToJpeg(openStream: () -> InputStream): ByteArray {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        openStream().use { BitmapFactory.decodeStream(it, null, bounds) }

        val sampleSize = calculateSampleSize(bounds.outWidth, bounds.outHeight, MAX_DIMENSION)
        val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        val decoded = openStream().use { BitmapFactory.decodeStream(it, null, decodeOptions) }
            ?: throw IllegalArgumentException("Could not decode image")

        val scaled = scaleToMaxDimension(decoded, MAX_DIMENSION)
        if (scaled !== decoded) decoded.recycle()

        val output = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, output)
        scaled.recycle()
        return output.toByteArray()
    }

    private fun calculateSampleSize(width: Int, height: Int, maxDimension: Int): Int {
        var sampleSize = 1
        var currentWidth = width
        var currentHeight = height
        while (currentWidth / 2 >= maxDimension || currentHeight / 2 >= maxDimension) {
            currentWidth /= 2
            currentHeight /= 2
            sampleSize *= 2
        }
        return sampleSize
    }

    private fun scaleToMaxDimension(bitmap: Bitmap, maxDimension: Int): Bitmap {
        val longestEdge = maxOf(bitmap.width, bitmap.height)
        if (longestEdge <= maxDimension) return bitmap
        val scale = maxDimension.toFloat() / longestEdge
        val newWidth = (bitmap.width * scale).toInt().coerceAtLeast(1)
        val newHeight = (bitmap.height * scale).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }
}
