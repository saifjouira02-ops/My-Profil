package com.example.data.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Base64
import java.io.ByteArrayOutputStream
import kotlin.math.max
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

enum class ImageValidationError {
    UNSUPPORTED_TYPE,
    TOO_LARGE,
    DECODE_FAILED,
}

sealed interface ImageProcessResult {
    data class Success(val dataUrl: String) : ImageProcessResult
    data class Error(val error: ImageValidationError) : ImageProcessResult
}

object ImagePickerHelper {
    private val ALLOWED_MIME_TYPES = setOf(
        "image/jpeg",
        "image/jpg",
        "image/png",
        "image/webp",
    )
    private const val MAX_SOURCE_BYTES = 8L * 1024L * 1024L // 8 MB source file cap

    suspend fun processPickedImage(
        context: Context,
        uri: Uri,
        maxDimensionPx: Int = 720,
        maxEncodedChars: Int = 300_000,
    ): ImageProcessResult = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        val mimeType = resolver.getType(uri)?.lowercase()
        if (mimeType != null && mimeType !in ALLOWED_MIME_TYPES) {
            return@withContext ImageProcessResult.Error(ImageValidationError.UNSUPPORTED_TYPE)
        }

        val fileSize = resolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val idx = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (idx >= 0 && !cursor.isNull(idx)) cursor.getLong(idx) else null
            } else {
                null
            }
        }
        if (fileSize != null && fileSize > MAX_SOURCE_BYTES) {
            return@withContext ImageProcessResult.Error(ImageValidationError.TOO_LARGE)
        }

        try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            resolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, bounds)
            } ?: return@withContext ImageProcessResult.Error(ImageValidationError.DECODE_FAILED)

            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
                return@withContext ImageProcessResult.Error(ImageValidationError.DECODE_FAILED)
            }

            var sampleSize = 1
            while (bounds.outWidth / sampleSize > maxDimensionPx * 2 ||
                bounds.outHeight / sampleSize > maxDimensionPx * 2
            ) {
                sampleSize *= 2
            }

            val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
            val decoded = resolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, decodeOpts)
            } ?: return@withContext ImageProcessResult.Error(ImageValidationError.DECODE_FAILED)

            val scaled = scaleDownBitmap(decoded, maxDimensionPx)
            if (scaled !== decoded) decoded.recycle()

            val qualities = listOf(80, 68, 54, 40)
            for (quality in qualities) {
                val out = ByteArrayOutputStream()
                scaled.compress(Bitmap.CompressFormat.JPEG, quality, out)
                val bytes = out.toByteArray()
                val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
                val dataUrl = "data:image/jpeg;base64,$base64"
                if (dataUrl.length <= maxEncodedChars) {
                    scaled.recycle()
                    return@withContext ImageProcessResult.Success(dataUrl)
                }
            }
            scaled.recycle()
            ImageProcessResult.Error(ImageValidationError.TOO_LARGE)
        } catch (_: Exception) {
            ImageProcessResult.Error(ImageValidationError.DECODE_FAILED)
        }
    }

    private fun scaleDownBitmap(bitmap: Bitmap, maxDim: Int): Bitmap {
        val largest = max(bitmap.width, bitmap.height)
        if (largest <= maxDim) return bitmap
        val ratio = maxDim.toFloat() / largest.toFloat()
        val targetW = max(1, (bitmap.width * ratio).roundToInt())
        val targetH = max(1, (bitmap.height * ratio).roundToInt())
        return Bitmap.createScaledBitmap(bitmap, targetW, targetH, true)
    }

    fun decodeDataUrlToBytes(dataUrl: String?): ByteArray? {
        if (dataUrl.isNullOrBlank()) return null
        val commaIndex = dataUrl.indexOf(',')
        if (commaIndex < 0) return null
        return try {
            Base64.decode(dataUrl.substring(commaIndex + 1), Base64.DEFAULT)
        } catch (_: Exception) {
            null
        }
    }
}
