package ph.gov.barangaysibulan.idmaker

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.media.ExifInterface
import android.net.Uri
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

internal data class ProcessedImage(
    val uri: String,
    val note: String
)

internal data class CameraTarget(
    val uri: Uri,
    val file: File
)

internal object OfflineImageProcessor {
    private const val MAX_PHOTO_INPUT = 1400
    private const val MAX_SIGNATURE_INPUT = 1800
    private const val MAX_PHOTO_OUTPUT = 1000
    private const val MAX_SIGNATURE_OUTPUT = 1400
    private const val VALIDATION_SIDE = 256
    private const val MIN_PHOTO_FOREGROUND_FRACTION = 0.035f
    private const val MIN_PHOTO_BBOX_DENSITY = 0.16f
    private const val MIN_PROCESSED_PHOTO_CONTENT = 0.12f
    private const val MIN_PROCESSED_BBOX_DENSITY = 0.18f
    private val OWNED_IMAGE_NAME = Regex("^(id_photo|signature)_\\d+\\.(jpg|png)$")

    fun createCameraTarget(context: Context, prefix: String): CameraTarget {
        val dir = File(context.cacheDir, "camera_capture").apply { mkdirs() }
        val file = File.createTempFile(prefix, ".jpg", dir)
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        return CameraTarget(uri, file)
    }

    /** Deletes only app-generated processed images. Gallery/Documents originals are never deleted. */
    fun deleteProcessed(context: Context, uriString: String?): Boolean {
        if (uriString.isNullOrBlank()) return false
        return runCatching {
            val uri = Uri.parse(uriString)
            if (uri.scheme != "content" || uri.authority != "${context.packageName}.fileprovider") {
                return@runCatching false
            }
            if (uri.pathSegments.firstOrNull() != "processed_images") return@runCatching false
            val fileName = uri.pathSegments.lastOrNull()?.takeIf { OWNED_IMAGE_NAME.matches(it) }
                ?: return@runCatching false
            val dir = File(context.filesDir, "processed_images")
            val file = File(dir, fileName)
            file.exists() && file.delete()
        }.getOrDefault(false)
    }

    /**
     * Lightweight guard for existing records. Processed ID photos should contain a substantial,
     * connected person-shaped foreground. Sparse ink-like content (for example a signature) fails.
     */
    fun isLikelyIdPhoto(context: Context, uriString: String?): Boolean {
        if (uriString.isNullOrBlank()) return false
        val bitmap = decodeValidationBitmap(context, Uri.parse(uriString)) ?: return false
        return try {
            val width = bitmap.width
            val height = bitmap.height
            if (width < 24 || height < 24) return false

            val pixels = IntArray(width * height)
            bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

            var count = 0
            var minX = width
            var minY = height
            var maxX = -1
            var maxY = -1

            for (i in pixels.indices) {
                val c = pixels[i]
                val alpha = Color.alpha(c)
                val distanceFromWhite = max(
                    255 - Color.red(c),
                    max(255 - Color.green(c), 255 - Color.blue(c))
                )
                if (alpha >= 48 && distanceFromWhite >= 32) {
                    val x = i % width
                    val y = i / width
                    count++
                    if (x < minX) minX = x
                    if (x > maxX) maxX = x
                    if (y < minY) minY = y
                    if (y > maxY) maxY = y
                }
            }

            if (count <= 0 || maxX <= minX || maxY <= minY) return false
            val boxW = maxX - minX + 1
            val boxH = maxY - minY + 1
            val total = width * height
            val boxArea = boxW * boxH
            val contentFraction = count.toFloat() / total.toFloat()
            val boxDensity = count.toFloat() / boxArea.toFloat()
            val widthFraction = boxW.toFloat() / width.toFloat()
            val heightFraction = boxH.toFloat() / height.toFloat()

            contentFraction >= MIN_PROCESSED_PHOTO_CONTENT &&
                boxDensity >= MIN_PROCESSED_BBOX_DENSITY &&
                widthFraction >= 0.22f &&
                heightFraction >= 0.22f
        } finally {
            if (!bitmap.isRecycled) bitmap.recycle()
        }
    }

    suspend fun processIdPhoto(context: Context, source: Uri): ProcessedImage? = withContext(Dispatchers.IO) {
        val input = decodeBounded(context, source, MAX_PHOTO_INPUT) ?: return@withContext null
        try {
            val width = input.width
            val height = input.height
            if (width < 80 || height < 80) return@withContext null

            val pixels = IntArray(width * height)
            input.getPixels(pixels, 0, width, 0, 0, width, height)

            val borderSamples = collectBorderSamples(pixels, width, height)
            if (borderSamples.isEmpty()) return@withContext null
            val bg = medianColor(borderSamples)
            val spread = borderColorSpread(borderSamples, bg)
            val threshold = (spread + 34).coerceIn(38, 92)

            val background = edgeConnectedBackground(pixels, width, height, bg, threshold)
            var minX = width
            var minY = height
            var maxX = -1
            var maxY = -1
            var foregroundCount = 0

            val cleanedPixels = IntArray(pixels.size)
            for (i in pixels.indices) {
                if (background[i]) {
                    cleanedPixels[i] = Color.WHITE
                } else {
                    val x = i % width
                    val y = i / width
                    foregroundCount++
                    if (x < minX) minX = x
                    if (x > maxX) maxX = x
                    if (y < minY) minY = y
                    if (y > maxY) maxY = y
                    cleanedPixels[i] = pixels[i] or (0xFF shl 24)
                }
            }

            if (foregroundCount < pixels.size / 50 || maxX <= minX || maxY <= minY) {
                return@withContext null
            }

            val subjectW = maxX - minX + 1
            val subjectH = maxY - minY + 1
            val bboxArea = subjectW * subjectH
            val foregroundFraction = foregroundCount.toFloat() / pixels.size.toFloat()
            val bboxDensity = foregroundCount.toFloat() / bboxArea.toFloat()
            val widthFraction = subjectW.toFloat() / width.toFloat()
            val heightFraction = subjectH.toFloat() / height.toFloat()

            // Reject sparse line-art/signature-like images before they can become an ID photo.
            if (
                foregroundFraction < MIN_PHOTO_FOREGROUND_FRACTION ||
                bboxDensity < MIN_PHOTO_BBOX_DENSITY ||
                widthFraction < 0.18f ||
                heightFraction < 0.18f
            ) {
                return@withContext null
            }

            val cleaned = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            cleaned.setPixels(cleanedPixels, 0, width, 0, 0, width, height)

            val pad = (max(subjectW, subjectH) * 0.09f).toInt().coerceAtLeast(12)
            val cropLeft = (minX - pad).coerceAtLeast(0)
            val cropTop = (minY - pad).coerceAtLeast(0)
            val cropRight = (maxX + pad + 1).coerceAtMost(width)
            val cropBottom = (maxY + pad + 1).coerceAtMost(height)
            val cropW = cropRight - cropLeft
            val cropH = cropBottom - cropTop
            val cropped = Bitmap.createBitmap(cleaned, cropLeft, cropTop, cropW, cropH)
            if (!cleaned.isRecycled) cleaned.recycle()

            val squareSide = max(cropW, cropH)
            val square = Bitmap.createBitmap(squareSide, squareSide, Bitmap.Config.ARGB_8888)
            Canvas(square).apply {
                drawColor(Color.WHITE)
                drawBitmap(
                    cropped,
                    ((squareSide - cropW) / 2f),
                    ((squareSide - cropH) / 2f),
                    Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
                )
            }
            if (!cropped.isRecycled) cropped.recycle()

            val output = scaleDown(square, MAX_PHOTO_OUTPUT)
            if (output !== square && !square.isRecycled) square.recycle()
            val uri = saveProcessed(context, output, "id_photo", Bitmap.CompressFormat.JPEG, 95)
            if (!output.isRecycled) output.recycle()
            uri ?: return@withContext null

            ProcessedImage(
                uri = uri,
                note = if (spread > 48) {
                    "Processed offline. Background had some variation; check the preview and choose another portrait if edges look rough."
                } else {
                    "Processed offline: portrait validated, plain background replaced with pure white, and photo auto-cropped."
                }
            )
        } finally {
            if (!input.isRecycled) input.recycle()
        }
    }

    suspend fun processSignature(context: Context, source: Uri): ProcessedImage? = withContext(Dispatchers.IO) {
        val input = decodeBounded(context, source, MAX_SIGNATURE_INPUT) ?: return@withContext null
        try {
            val width = input.width
            val height = input.height
            if (width < 80 || height < 40) return@withContext null

            val pixels = IntArray(width * height)
            input.getPixels(pixels, 0, width, 0, 0, width, height)
            val borderSamples = collectBorderSamples(pixels, width, height)
            if (borderSamples.isEmpty()) return@withContext null
            val background = medianColor(borderSamples)
            val spread = borderColorSpread(borderSamples, background)
            val threshold = (spread + 18).coerceIn(20, 72)

            val out = IntArray(pixels.size)
            var minX = width
            var minY = height
            var maxX = -1
            var maxY = -1
            var inkCount = 0

            for (i in pixels.indices) {
                val c = pixels[i]
                // Use distance from the plain edge color instead of assuming white paper.
                // This also cleans a light signature photographed on a dark solid surface.
                val difference = maxChannelDistance(c, background)
                val alpha = ((difference - threshold) * 4.2f).toInt().coerceIn(0, 255)
                out[i] = Color.argb(alpha, 0, 0, 0)
                if (alpha >= 46) {
                    val x = i % width
                    val y = i / width
                    inkCount++
                    if (x < minX) minX = x
                    if (x > maxX) maxX = x
                    if (y < minY) minY = y
                    if (y > maxY) maxY = y
                }
            }

            if (inkCount < max(30, pixels.size / 2500) || maxX <= minX || maxY <= minY) {
                return@withContext null
            }

            val signature = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            signature.setPixels(out, 0, width, 0, 0, width, height)

            val inkW = maxX - minX + 1
            val inkH = maxY - minY + 1
            val pad = (max(inkW, inkH) * 0.05f).toInt().coerceAtLeast(8)
            val left = (minX - pad).coerceAtLeast(0)
            val top = (minY - pad).coerceAtLeast(0)
            val right = (maxX + pad + 1).coerceAtMost(width)
            val bottom = (maxY + pad + 1).coerceAtMost(height)
            val cropped = Bitmap.createBitmap(signature, left, top, right - left, bottom - top)
            if (!signature.isRecycled) signature.recycle()
            val output = scaleDown(cropped, MAX_SIGNATURE_OUTPUT)
            if (output !== cropped && !cropped.isRecycled) cropped.recycle()
            val uri = saveProcessed(context, output, "signature", Bitmap.CompressFormat.PNG, 100)
            if (!output.isRecycled) output.recycle()
            uri ?: return@withContext null

            ProcessedImage(
                uri = uri,
                note = "Processed offline: plain light/dark background removed, signature auto-cropped, and saved as transparent PNG."
            )
        } finally {
            if (!input.isRecycled) input.recycle()
        }
    }

    fun loadPreview(context: Context, uriString: String?): Bitmap? {
        if (uriString.isNullOrBlank()) return null
        return runCatching {
            context.contentResolver.openInputStream(Uri.parse(uriString))?.use { stream ->
                BitmapFactory.decodeStream(stream)
            }
        }.getOrNull()
    }

    private fun decodeValidationBitmap(context: Context, uri: Uri): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        var sample = 1
        while (max(bounds.outWidth / sample, bounds.outHeight / sample) > VALIDATION_SIDE * 2) {
            sample *= 2
        }
        val options = BitmapFactory.Options().apply {
            inSampleSize = sample
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val decoded = context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, options)
        } ?: return null
        val maxSide = max(decoded.width, decoded.height)
        if (maxSide <= VALIDATION_SIDE) return decoded
        val scale = VALIDATION_SIDE.toFloat() / maxSide.toFloat()
        val scaled = Bitmap.createScaledBitmap(
            decoded,
            max(1, (decoded.width * scale).toInt()),
            max(1, (decoded.height * scale).toInt()),
            true
        )
        if (scaled !== decoded && !decoded.isRecycled) decoded.recycle()
        return scaled
    }

    private fun decodeBounded(context: Context, uri: Uri, maxDimension: Int): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        var sample = 1
        while (max(bounds.outWidth / sample, bounds.outHeight / sample) > maxDimension * 2) {
            sample *= 2
        }
        val options = BitmapFactory.Options().apply {
            inSampleSize = sample
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val decoded = context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, options)
        } ?: return null

        val oriented = applyExifOrientation(context, uri, decoded)
        val maxSide = max(oriented.width, oriented.height)
        if (maxSide <= maxDimension) return oriented
        val scale = maxDimension.toFloat() / maxSide.toFloat()
        val scaled = Bitmap.createScaledBitmap(
            oriented,
            max(1, (oriented.width * scale).toInt()),
            max(1, (oriented.height * scale).toInt()),
            true
        )
        if (scaled !== oriented) oriented.recycle()
        return scaled
    }

    private fun applyExifOrientation(context: Context, uri: Uri, bitmap: Bitmap): Bitmap {
        val orientation = runCatching {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                ExifInterface(stream).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )
            } ?: ExifInterface.ORIENTATION_NORMAL
        }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)

        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.setScale(-1f, 1f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.setRotate(180f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> {
                matrix.setRotate(180f)
                matrix.postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_TRANSPOSE -> {
                matrix.setRotate(90f)
                matrix.postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.setRotate(90f)
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                matrix.setRotate(-90f)
                matrix.postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.setRotate(-90f)
            else -> return bitmap
        }

        return runCatching {
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true).also { transformed ->
                if (transformed !== bitmap && !bitmap.isRecycled) bitmap.recycle()
            }
        }.getOrElse { bitmap }
    }

    private fun collectBorderSamples(pixels: IntArray, width: Int, height: Int): IntArray {
        val step = max(1, min(width, height) / 160)
        val list = ArrayList<Int>()
        var x = 0
        while (x < width) {
            list.add(pixels[x])
            list.add(pixels[(height - 1) * width + x])
            x += step
        }
        var y = step
        while (y < height - 1) {
            list.add(pixels[y * width])
            list.add(pixels[y * width + (width - 1)])
            y += step
        }
        return list.toIntArray()
    }

    private fun medianColor(samples: IntArray): Int {
        val rs = IntArray(samples.size)
        val gs = IntArray(samples.size)
        val bs = IntArray(samples.size)
        for (i in samples.indices) {
            rs[i] = Color.red(samples[i])
            gs[i] = Color.green(samples[i])
            bs[i] = Color.blue(samples[i])
        }
        rs.sort(); gs.sort(); bs.sort()
        val mid = samples.size / 2
        return Color.rgb(rs[mid], gs[mid], bs[mid])
    }

    private fun borderColorSpread(samples: IntArray, reference: Int): Int {
        val distances = IntArray(samples.size)
        for (i in samples.indices) distances[i] = maxChannelDistance(samples[i], reference)
        distances.sort()
        return distances[(distances.size * 9 / 10).coerceIn(0, distances.lastIndex)]
    }

    private fun edgeConnectedBackground(
        pixels: IntArray,
        width: Int,
        height: Int,
        reference: Int,
        threshold: Int
    ): BooleanArray {
        val total = pixels.size
        val background = BooleanArray(total)
        val visited = BooleanArray(total)
        val queue = IntArray(total)
        var head = 0
        var tail = 0

        fun seed(index: Int) {
            if (!visited[index] && maxChannelDistance(pixels[index], reference) <= threshold) {
                visited[index] = true
                background[index] = true
                queue[tail++] = index
            }
        }

        for (x in 0 until width) {
            seed(x)
            seed((height - 1) * width + x)
        }
        for (y in 1 until height - 1) {
            seed(y * width)
            seed(y * width + width - 1)
        }

        while (head < tail) {
            val index = queue[head++]
            val x = index % width
            val y = index / width

            fun visit(next: Int) {
                if (visited[next]) return
                visited[next] = true
                if (maxChannelDistance(pixels[next], reference) <= threshold) {
                    background[next] = true
                    queue[tail++] = next
                }
            }

            if (x > 0) visit(index - 1)
            if (x + 1 < width) visit(index + 1)
            if (y > 0) visit(index - width)
            if (y + 1 < height) visit(index + width)
        }

        return background
    }

    private fun maxChannelDistance(a: Int, b: Int): Int = max(
        abs(Color.red(a) - Color.red(b)),
        max(abs(Color.green(a) - Color.green(b)), abs(Color.blue(a) - Color.blue(b)))
    )

    private fun scaleDown(bitmap: Bitmap, maxDimension: Int): Bitmap {
        val maxSide = max(bitmap.width, bitmap.height)
        if (maxSide <= maxDimension) return bitmap
        val scale = maxDimension.toFloat() / maxSide.toFloat()
        return Bitmap.createScaledBitmap(
            bitmap,
            max(1, (bitmap.width * scale).toInt()),
            max(1, (bitmap.height * scale).toInt()),
            true
        )
    }

    private fun saveProcessed(
        context: Context,
        bitmap: Bitmap,
        prefix: String,
        format: Bitmap.CompressFormat,
        quality: Int
    ): String? {
        val dir = File(context.filesDir, "processed_images").apply { mkdirs() }
        val extension = if (format == Bitmap.CompressFormat.PNG) "png" else "jpg"
        val file = File(dir, "${prefix}_${System.currentTimeMillis()}.$extension")
        val ok = runCatching {
            FileOutputStream(file).use { output -> bitmap.compress(format, quality, output) }
        }.getOrDefault(false)
        if (!ok) {
            file.delete()
            return null
        }
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        ).toString()
    }
}
