package ph.gov.barangaysibulan.idmaker

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ph.gov.barangaysibulan.idmaker.data.AppDatabase
import ph.gov.barangaysibulan.idmaker.data.Employee

@Composable
fun TightPortraitGenerateScreen() {
    val context = LocalContext.current
    val dao = remember { AppDatabase.get(context).employeeDao() }
    val employees by dao.observeAll().collectAsState(initial = emptyList())
    val prefs = remember { context.getSharedPreferences("id_maker_settings", Context.MODE_PRIVATE) }

    var firstId by remember { mutableStateOf<Long?>(null) }
    var secondId by remember { mutableStateOf<Long?>(null) }
    var firstMenu by remember { mutableStateOf(false) }
    var secondMenu by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }

    LaunchedEffect(employees, firstId) {
        if (firstId == null && employees.isNotEmpty()) firstId = employees.first().id
        if (firstId != null && employees.none { it.id == firstId }) firstId = employees.firstOrNull()?.id
        if (secondId != null && employees.none { it.id == secondId }) secondId = null
        if (secondId != null && secondId == firstId) secondId = null
    }

    val first = employees.firstOrNull { it.id == firstId }
    val second = employees.firstOrNull { it.id == secondId }
    val frontReady = !prefs.getString("front_template_uri", null).isNullOrBlank()
    val backReady = !prefs.getString("back_template_uri", null).isNullOrBlank()

    val firstPhotoValid = remember(first?.photoUri) {
        first?.photoUri?.let { OfflineImageProcessor.isLikelyIdPhoto(context, it) } ?: false
    }
    val secondPhotoValid = remember(second?.photoUri) {
        second?.photoUri?.let { OfflineImageProcessor.isLikelyIdPhoto(context, it) } ?: false
    }
    val photosReady = first != null && firstPhotoValid && (second == null || secondPhotoValid)

    val createPdf = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
        if (uri != null && first != null) {
            message = if (writeTightPortraitPdf(context, uri, first, second, prefs)) {
                "PDF created. Print at Actual Size / 100%."
            } else {
                "Could not create PDF."
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Generate ID", style = MaterialTheme.typography.headlineSmall)
        Text("A4 portrait • 4 fixed paper slots • Publisher-matched cut lines • CR80 ID centered in each slot")

        if (employees.isEmpty()) {
            Card(Modifier.fillMaxWidth()) {
                Text("No employee record found. Add one in Records first.", Modifier.padding(16.dp))
            }
            return@Column
        }

        Text("Person 1", style = MaterialTheme.typography.titleMedium)
        Box {
            OutlinedButton(onClick = { firstMenu = true }, modifier = Modifier.fillMaxWidth()) {
                Text(first?.let { "${it.fullName} • ${it.controlNumber}" } ?: "Choose employee")
            }
            DropdownMenu(expanded = firstMenu, onDismissRequest = { firstMenu = false }) {
                employees.forEach { employee ->
                    DropdownMenuItem(
                        text = { Text("${employee.fullName} • ${employee.controlNumber}") },
                        onClick = {
                            firstId = employee.id
                            if (secondId == employee.id) secondId = null
                            firstMenu = false
                            message = ""
                        }
                    )
                }
            }
        }

        Text("Person 2 (optional)", style = MaterialTheme.typography.titleMedium)
        Box {
            OutlinedButton(onClick = { secondMenu = true }, modifier = Modifier.fillMaxWidth()) {
                Text(second?.let { "${it.fullName} • ${it.controlNumber}" } ?: "Leave bottom row blank")
            }
            DropdownMenu(expanded = secondMenu, onDismissRequest = { secondMenu = false }) {
                DropdownMenuItem(
                    text = { Text("No second person") },
                    onClick = {
                        secondId = null
                        secondMenu = false
                    }
                )
                employees.filter { it.id != firstId }.forEach { employee ->
                    DropdownMenuItem(
                        text = { Text("${employee.fullName} • ${employee.controlNumber}") },
                        onClick = {
                            secondId = employee.id
                            secondMenu = false
                            message = ""
                        }
                    )
                }
            }
        }

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Ready Check", fontWeight = FontWeight.Bold)
                Text("Front design: ${if (frontReady) "Ready" else "Missing — fallback design will be used"}")
                Text("Back design: ${if (backReady) "Ready" else "Missing — fallback design will be used"}")
                first?.let {
                    Text(
                        "Person 1 photo: " + when {
                            it.photoUri.isNullOrBlank() -> "Missing"
                            firstPhotoValid -> "Ready"
                            else -> "Invalid — replace photo"
                        }
                    )
                    Text("Person 1 QR: ${if (!it.qrImageUri.isNullOrBlank()) "Ready" else "Missing"}")
                }
                second?.let {
                    Text(
                        "Person 2 photo: " + when {
                            it.photoUri.isNullOrBlank() -> "Missing"
                            secondPhotoValid -> "Ready"
                            else -> "Invalid — replace photo"
                        }
                    )
                    Text("Person 2 QR: ${if (!it.qrImageUri.isNullOrBlank()) "Ready" else "Missing"}")
                }
            }
        }

        if (first != null && !firstPhotoValid) {
            Card(Modifier.fillMaxWidth()) {
                Text(
                    "Person 1 has an invalid ID photo. A signature/document-like image may have been saved in the photo field. Open Records, edit ${first.fullName}, and replace the ID Photo before generating.",
                    Modifier.padding(14.dp)
                )
            }
        }
        if (second != null && !secondPhotoValid) {
            Card(Modifier.fillMaxWidth()) {
                Text(
                    "Person 2 has an invalid ID photo. Open Records and replace that employee's ID Photo before generating.",
                    Modifier.padding(14.dp)
                )
            }
        }

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("A4 layout", fontWeight = FontWeight.Bold)
                Text("Slot 1 / top-left: Person 1 Front")
                Text("Slot 2 / top-right: Person 1 Back")
                Text(if (second == null) "Slots 3 & 4 / bottom row: blank" else "Slot 3: Person 2 Front • Slot 4: Person 2 Back")
                Text("Each paper slot is about 85.01 × 115.05 mm and has a visible cut line.", style = MaterialTheme.typography.bodySmall)
                Text("The ID itself stays CR80 portrait at 53.98 × 85.60 mm and is centered inside the paper slot.", style = MaterialTheme.typography.bodySmall)
            }
        }

        first?.let { employee ->
            Button(
                onClick = {
                    if (!photosReady) {
                        message = "Replace the invalid/missing ID photo before generating the PDF."
                        return@Button
                    }
                    val safeName = employee.fullName.replace(Regex("[^A-Za-z0-9_-]+"), "_").take(40)
                    createPdf.launch("Barangay-ID-${safeName.ifBlank { employee.controlNumber }}.pdf")
                },
                modifier = Modifier.fillMaxWidth().height(54.dp),
                enabled = photosReady
            ) { Text("Generate A4 PDF") }
        }

        if (message.isNotBlank()) Text(message)
        Text("Important: sa print dialog piliin ang Actual Size / 100%. Huwag Fit to Page.", style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(12.dp))
    }
}

private const val TIGHT_PT_PER_MM = 72f / 25.4f
private const val SLOT_W_MM = 85.01f
private const val SLOT_H_MM = 115.05f
private const val SLOT_LEFT_1_MM = 19.97f
private const val SLOT_LEFT_2_MM = 105.02f
private const val SLOT_TOP_1_MM = 14.77f
private const val SLOT_TOP_2_MM = 168.62f
private const val CARD_W_MM = 53.98f
private const val CARD_H_MM = 85.60f
private const val TIGHT_MAX_BITMAP_SIDE = 1800

private fun mm(value: Float): Float = value * TIGHT_PT_PER_MM

private fun paperSlot(leftMm: Float, topMm: Float): RectF = RectF(
    mm(leftMm),
    mm(topMm),
    mm(leftMm + SLOT_W_MM),
    mm(topMm + SLOT_H_MM)
)

private fun centeredCard(slot: RectF): RectF {
    val cardW = mm(CARD_W_MM)
    val cardH = mm(CARD_H_MM)
    val left = slot.left + (slot.width() - cardW) / 2f
    val top = slot.top + (slot.height() - cardH) / 2f
    return RectF(left, top, left + cardW, top + cardH)
}

private fun writeTightPortraitPdf(
    context: Context,
    uri: Uri,
    first: Employee,
    second: Employee?,
    prefs: SharedPreferences
): Boolean {
    val pdf = PdfDocument()
    return try {
        val pageW = 595
        val pageH = 842
        val page = pdf.startPage(PdfDocument.PageInfo.Builder(pageW, pageH, 1).create())
        val canvas = page.canvas
        canvas.drawColor(Color.WHITE)

        val slot1 = paperSlot(SLOT_LEFT_1_MM, SLOT_TOP_1_MM)
        val slot2 = paperSlot(SLOT_LEFT_2_MM, SLOT_TOP_1_MM)
        val slot3 = paperSlot(SLOT_LEFT_1_MM, SLOT_TOP_2_MM)
        val slot4 = paperSlot(SLOT_LEFT_2_MM, SLOT_TOP_2_MM)

        listOf(slot1, slot2, slot3, slot4).forEach { tightDrawCutLine(canvas, it) }

        tightDrawFront(context, canvas, centeredCard(slot1), first, prefs)
        tightDrawBack(context, canvas, centeredCard(slot2), first, prefs)

        if (second != null) {
            tightDrawFront(context, canvas, centeredCard(slot3), second, prefs)
            tightDrawBack(context, canvas, centeredCard(slot4), second, prefs)
        }

        pdf.finishPage(page)
        context.contentResolver.openOutputStream(uri)?.use { output ->
            pdf.writeTo(output)
            true
        } ?: false
    } catch (_: Exception) {
        false
    } finally {
        pdf.close()
    }
}

private fun tightDrawCutLine(canvas: Canvas, slot: RectF) {
    val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        style = Paint.Style.STROKE
        strokeWidth = 0.5f
    }
    canvas.drawRect(slot, p)
}

private fun tightDrawFront(context: Context, canvas: Canvas, r: RectF, e: Employee, prefs: SharedPreferences) {
    val p = Paint(Paint.ANTI_ALIAS_FLAG)
    tightDrawTemplate(context, canvas, r, prefs.getString("front_template_uri", null), true)

    val logo = 24f
    tightDrawLogoOrB(context, canvas, RectF(r.left + 8f, r.top + 7f, r.left + 8f + logo, r.top + 7f + logo), prefs.getString("logo1_uri", null))
    tightDrawLogoOrB(context, canvas, RectF(r.right - 8f - logo, r.top + 7f, r.right - 8f, r.top + 7f + logo), prefs.getString("logo2_uri", null))

    val text = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    tightTextFit(canvas, prefs.getString("barangay", "BARANGAY SIBULAN") ?: "BARANGAY SIBULAN", r.centerX(), r.top + 18f, r.width() - 72f, text, 7.2f, 4.8f)
    tightTextFit(canvas, prefs.getString("id_heading", "BARANGAY EMPLOYEE ID") ?: "BARANGAY EMPLOYEE ID", r.centerX(), r.top + 31f, r.width() - 28f, text, 8.5f, 5.2f)

    val photoRect = RectF(r.left + 37f, r.top + 48f, r.right - 37f, r.top + 137f)
    val photo = tightLoadBitmap(context, e.photoUri)
    if (photo != null) {
        canvas.drawBitmap(photo, null, photoRect, p)
        photo.recycleSafely()
    } else {
        tightLabeledBox(canvas, photoRect, "PHOTO")
    }

    tightTextFit(canvas, e.fullName, r.centerX(), r.top + 154f, r.width() - 18f, text, 10.5f, 6f)
    text.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
    tightTextFit(canvas, e.position, r.centerX(), r.top + 169f, r.width() - 18f, text, 8f, 5.5f)
    tightTextFit(canvas, "ID NO. ${e.controlNumber}", r.centerX(), r.top + 184f, r.width() - 18f, text, 7.5f, 5.2f)

    val sigRect = RectF(r.left + 16f, r.bottom - 50f, r.left + 82f, r.bottom - 25f)
    tightLoadBitmap(context, e.signatureUri)?.let { bitmap ->
        canvas.drawBitmap(bitmap, null, sigRect, p)
        bitmap.recycleSafely()
    }

    val qrRect = RectF(r.right - 51f, r.bottom - 56f, r.right - 10f, r.bottom - 15f)
    val qr = tightLoadBitmap(context, e.qrImageUri)
    if (qr != null) {
        canvas.drawBitmap(qr, null, qrRect, p)
        qr.recycleSafely()
    } else {
        tightLabeledBox(canvas, qrRect, "QR")
    }
}

private fun tightDrawBack(context: Context, canvas: Canvas, r: RectF, e: Employee, prefs: SharedPreferences) {
    val p = Paint(Paint.ANTI_ALIAS_FLAG)
    tightDrawTemplate(context, canvas, r, prefs.getString("back_template_uri", null), false)

    val text = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        textAlign = Paint.Align.LEFT
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
    }
    val x = r.left + 12f
    var y = r.top + 34f
    listOf(
        "BIRTHDATE: ${e.birthdate.ifBlank { "—" }}",
        "ADDRESS: ${e.address.ifBlank { "—" }}",
        "SEX: ${e.sex.ifBlank { "—" }}",
        "CIVIL STATUS: ${e.civilStatus.ifBlank { "—" }}"
    ).forEach { line ->
        tightTextFit(canvas, line, x, y, r.width() - 24f, text, 7.4f, 4.7f)
        y += 22f
    }

    text.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    tightTextFit(canvas, "APPROVED BY:", x, r.bottom - 78f, r.width() - 24f, text, 7.2f, 5f)

    val sigRect = RectF(x, r.bottom - 72f, x + 82f, r.bottom - 42f)
    tightLoadBitmap(context, prefs.getString("captain_signature_uri", null))?.let { bitmap ->
        canvas.drawBitmap(bitmap, null, sigRect, p)
        bitmap.recycleSafely()
    }

    val captain = prefs.getString("captain_name", "")?.ifBlank { "PUNONG BARANGAY" } ?: "PUNONG BARANGAY"
    tightTextFit(canvas, captain, x, r.bottom - 28f, r.width() - 24f, text, 8f, 5.2f)
    text.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
    tightTextFit(canvas, prefs.getString("captain_title", "PUNONG BARANGAY") ?: "PUNONG BARANGAY", x, r.bottom - 15f, r.width() - 24f, text, 6.8f, 4.8f)
}

private fun tightDrawTemplate(context: Context, canvas: Canvas, r: RectF, uri: String?, front: Boolean) {
    val bitmap = tightLoadBitmap(context, uri)
    if (bitmap != null) {
        tightCenterCrop(canvas, bitmap, r)
        bitmap.recycleSafely()
        return
    }
    val p = Paint(Paint.ANTI_ALIAS_FLAG)
    p.color = Color.WHITE
    canvas.drawRect(r, p)
    p.style = Paint.Style.STROKE
    p.strokeWidth = 1f
    p.color = Color.rgb(28, 92, 48)
    canvas.drawRect(r, p)
    p.style = Paint.Style.FILL
    if (front) {
        p.color = Color.rgb(28, 92, 48)
        canvas.drawRect(r.left, r.top, r.right, r.top + 42f, p)
    }
}

private fun tightCenterCrop(canvas: Canvas, bitmap: Bitmap, target: RectF) {
    val srcRatio = bitmap.width.toFloat() / bitmap.height.toFloat()
    val dstRatio = target.width() / target.height()
    val src = if (srcRatio > dstRatio) {
        val newW = (bitmap.height * dstRatio).toInt().coerceAtLeast(1)
        val left = ((bitmap.width - newW) / 2).coerceAtLeast(0)
        Rect(left, 0, (left + newW).coerceAtMost(bitmap.width), bitmap.height)
    } else {
        val newH = (bitmap.width / dstRatio).toInt().coerceAtLeast(1)
        val top = ((bitmap.height - newH) / 2).coerceAtLeast(0)
        Rect(0, top, bitmap.width, (top + newH).coerceAtMost(bitmap.height))
    }
    canvas.drawBitmap(bitmap, src, target, Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
}

private fun tightDrawLogoOrB(context: Context, canvas: Canvas, r: RectF, uri: String?) {
    val bitmap = tightLoadBitmap(context, uri)
    if (bitmap != null) {
        canvas.drawBitmap(bitmap, null, r, Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
        bitmap.recycleSafely()
        return
    }
    val p = Paint(Paint.ANTI_ALIAS_FLAG)
    p.color = Color.WHITE
    canvas.drawCircle(r.centerX(), r.centerY(), r.width() / 2f, p)
    p.color = Color.rgb(28, 92, 48)
    p.textAlign = Paint.Align.CENTER
    p.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    p.textSize = 13f
    canvas.drawText("B", r.centerX(), r.centerY() + 4.5f, p)
}

private fun tightLabeledBox(canvas: Canvas, r: RectF, label: String) {
    val p = Paint(Paint.ANTI_ALIAS_FLAG)
    p.color = Color.LTGRAY
    p.style = Paint.Style.STROKE
    p.strokeWidth = 1f
    canvas.drawRect(r, p)
    p.style = Paint.Style.FILL
    p.color = Color.DKGRAY
    p.textSize = 7f
    p.textAlign = Paint.Align.CENTER
    canvas.drawText(label, r.centerX(), r.centerY() + 2.5f, p)
}

private fun tightTextFit(canvas: Canvas, value: String, x: Float, y: Float, maxWidth: Float, paint: Paint, maxSize: Float, minSize: Float) {
    val text = value.ifBlank { "—" }
    paint.textSize = maxSize
    while (paint.textSize > minSize && paint.measureText(text) > maxWidth) paint.textSize -= 0.4f
    canvas.drawText(text, x, y, paint)
}

private fun tightLoadBitmap(context: Context, uriString: String?): Bitmap? {
    if (uriString.isNullOrBlank()) return null
    return runCatching {
        val uri = Uri.parse(uriString)
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@runCatching null

        var sample = 1
        while (maxOf(bounds.outWidth / sample, bounds.outHeight / sample) > TIGHT_MAX_BITMAP_SIDE * 2) {
            sample *= 2
        }

        val options = BitmapFactory.Options().apply {
            inSampleSize = sample
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val decoded = context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, options)
        } ?: return@runCatching null

        val maxSide = maxOf(decoded.width, decoded.height)
        if (maxSide <= TIGHT_MAX_BITMAP_SIDE) return@runCatching decoded

        val scale = TIGHT_MAX_BITMAP_SIDE.toFloat() / maxSide.toFloat()
        val scaled = Bitmap.createScaledBitmap(
            decoded,
            maxOf(1, (decoded.width * scale).toInt()),
            maxOf(1, (decoded.height * scale).toInt()),
            true
        )
        if (scaled !== decoded) decoded.recycleSafely()
        scaled
    }.getOrNull()
}

private fun Bitmap.recycleSafely() {
    if (!isRecycled) recycle()
}
