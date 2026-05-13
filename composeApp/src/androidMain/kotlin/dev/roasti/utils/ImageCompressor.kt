package dev.roasti.utils

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.ByteArrayOutputStream

fun compressImage(contentResolver: ContentResolver, uri: Uri, maxSize: Int = 1024, quality: Int = 80): ByteArray {
    val inputStream = contentResolver.openInputStream(uri) ?: return ByteArray(0)
    val original = BitmapFactory.decodeStream(inputStream)
    inputStream.close()
    val scaled = scaleBitmap(original, maxSize)
    val out = ByteArrayOutputStream()
    scaled.compress(Bitmap.CompressFormat.JPEG, quality, out)
    if (scaled !== original) scaled.recycle()
    original.recycle()
    return out.toByteArray()
}

private fun scaleBitmap(bitmap: Bitmap, maxSize: Int): Bitmap {
    val w = bitmap.width
    val h = bitmap.height
    if (w <= maxSize && h <= maxSize) return bitmap
    val ratio = maxSize.toFloat() / maxOf(w, h)
    return Bitmap.createScaledBitmap(bitmap, (w * ratio).toInt(), (h * ratio).toInt(), true)
}
