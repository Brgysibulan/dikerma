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
        Text("A4 portrait • 4 fixed paper slots • CR80 card • 30 × 35 mm employee photo")

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
                    "Person 1 has an invalid ID photo. Open Records, edit ${first.fullName}, and replace the ID Photo before generating.",
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
                Text("The ID itself remains CR80 portrait at 53.98 × 85.60 mm.", style = MaterialTheme.typography.bodySmall)
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
private fun cardX(r: RectF, valueMm: Float): Float = r.left + mm(valueMm)
private fun cardY(r: RectF, valueMm: Float): Float = r.top + mm(valueMm)
private fun cardRect(r: RectF, leftMm: Float, topMm: Float, widthMm: Float, heightMm: Float): RectF = RectF(
    cardX(r, leftMm),
    cardY(r, topMm),
    cardX(r, leftMm + widthMm),
    cardY(r, topMm + heightMm)
)

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
        val page = pdf.startPage(PdfDocument.PageInfo.Builder(595, 842, 1).create())
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
    val imagePaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    tightDrawTemplate(context, canvas, r, prefs.getString("front_template_uri", null), true)

    val logoSizeMm = 7.0f
    tightDrawLogoOrB(context, canvas, cardRect(r, 2.2f, 1.6f, logoSizeMm, logoSizeMm), prefs.getString("logo1_uri", null))
    tightDrawLogoOrB(context, canvas, cardRect(r, CARD_W_MM - 2.2f - logoSizeMm, 1.6f, logoSizeMm, logoSizeMm), prefs.getString("logo2_uri", null))

    val header = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
    }
    val headerWidth = mm(35f)
    tightTextFit(canvas, prefs.getString("republic", "REPUBLIC OF THE PHILIPPINES") ?: "REPUBLIC OF THE PHILIPPINES", r.centerX(), cardY(r, 3.4f), headerWidth, header, 4.3f, 3.2f)
    tightTextFit(canvas, prefs.getString("province", "Province of Davao del Sur") ?: "Province of Davao del Sur", r.centerX(), cardY(r, 5.7f), headerWidth, header, 4.2f, 3.1f)
    tightTextFit(canvas, prefs.getString("municipality", "Municipality of Sta. Cruz") ?: "Municipality of Sta. Cruz", r.centerX(), cardY(r, 8.0f), headerWidth, header, 4.2f, 3.1f)
    header.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    tightTextFit(canvas, prefs.getString("barangay", "BARANGAY SIBULAN") ?: "BARANGAY SIBULAN", r.centerX(), cardY(r, 10.6f), headerWidth, header, 5.0f, 3.7f)
    tightTextFit(canvas, prefs.getString("id_heading", "BARANGAY EMPLOYEE ID") ?: "BARANGAY EMPLOYEE ID", r.centerX(), cardY(r, 15.1f), mm(48f), header, 7.4f, 5.2f)

    // Exact requested employee photo size: 30 × 35 mm.
    val photoRect = cardRect(r, 2.5f, 20.5f, 30f, 35f)
    val photo = tightLoadBitmap(context, e.photoUri)
    if (photo != null) {
        canvas.drawBitmap(photo, null, photoRect, imagePaint)
        photo.recycleSafely()
    } else {
        tightLabeledBox(canvas, photoRect, "ID PHOTO")
    }
    val photoBorder = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.DKGRAY
        style = Paint.Style.STROKE
        strokeWidth = 0.45f
    }
    canvas.drawRect(photoRect, photoBorder)

    val fieldX = cardX(r, 34.2f)
    val fieldWidth = mm(17.2f)
    val label = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.DKGRAY
        textAlign = Paint.Align.LEFT
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textSize = 4.1f
    }
    val value = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        textAlign = Paint.Align.LEFT
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }

    canvas.drawText("NAME", fieldX, cardY(r, 25.2f), label)
    tightTextFit(canvas, e.fullName.uppercase(), fieldX, cardY(r, 28.3f), fieldWidth, value, 5.6f, 3.5f)
    canvas.drawText("DESIGNATION", fieldX, cardY(r, 34.0f), label)
    tightTextFit(canvas, e.position, fieldX, cardY(r, 37.2f), fieldWidth, value, 5.3f, 3.4f)
    canvas.drawText("ID NO.", fieldX, cardY(r, 42.9f), label)
    tightTextFit(canvas, e.controlNumber, fieldX, cardY(r, 46.1f), fieldWidth, value, 5.6f, 3.6f)

    val signatureRect = cardRect(r, 2.5f, 59.0f, 30f, 7.8f)
    tightLoadBitmap(context, e.signatureUri)?.let { bitmap ->
        canvas.drawBitmap(bitmap, null, signatureRect, imagePaint)
        bitmap.recycleSafely()
    }
    val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        strokeWidth = 0.6f
    }
    canvas.drawLine(cardX(r, 2.5f), cardY(r, 68.0f), cardX(r, 32.5f), cardY(r, 68.0f), linePaint)
    val smallCenter = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    tightTextFit(canvas, "CARDHOLDER'S SIGNATURE", cardX(r, 17.5f), cardY(r, 70.5f), mm(30f), smallCenter, 4.4f, 3.1f)

    val qrRect = cardRect(r, 39.0f, 58.6f, 12.0f, 12.0f)
    val qr = tightLoadBitmap(context, e.qrImageUri)
    if (qr != null) {
        canvas.drawBitmap(qr, null, qrRect, imagePaint)
        qr.recycleSafely()
    } else {
        tightLabeledBox(canvas, qrRect, "QR")
    }
    tightTextFit(canvas, "SCAN TO VERIFY", cardX(r, 45.0f), cardY(r, 57.2f), mm(16f), smallCenter, 4.2f, 3.0f)
    tightTextFit(canvas, "VERIFY ID VALIDITY", cardX(r, 45.0f), cardY(r, 73.1f), mm(17f), smallCenter, 3.9f, 2.8f)
}

private fun tightDrawBack(context: Context, canvas: Canvas, r: RectF, e: Employee, prefs: SharedPreferences) {
    val imagePaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    tightDrawTemplate(context, canvas, r, prefs.getString("back_template_uri", null), false)

    val left = cardX(r, 2.7f)
    val rightWidth = mm(48.6f)
    val normal = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        textAlign = Paint.Align.LEFT
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
    }
    val bold = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        textAlign = Paint.Align.LEFT
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }

    tightTextFit(canvas, "DATE OF BIRTH: ${e.birthdate.ifBlank { "—" }}", left, cardY(r, 5.2f), rightWidth, normal, 5.4f, 4.0f)
    tightTextFit(canvas, "ADDRESS: ${e.address.ifBlank { "—" }}", left, cardY(r, 9.3f), rightWidth, normal, 5.2f, 3.6f)

    val sexText = "SEX: ${e.sex.ifBlank { "—" }}"
    val civilText = "CIVIL STATUS: ${e.civilStatus.ifBlank { "—" }}"
    tightTextFit(canvas, sexText, left, cardY(r, 13.4f), mm(20f), normal, 5.2f, 3.8f)
    tightTextFit(canvas, civilText, cardX(r, 25.0f), cardY(r, 13.4f), mm(26.3f), normal, 5.2f, 3.6f)

    bold.textAlign = Paint.Align.CENTER
    tightTextFit(canvas, "IDENTIFICATION", r.centerX(), cardY(r, 18.0f), mm(45f), bold, 6.1f, 4.5f)
    normal.textAlign = Paint.Align.LEFT
    normal.textSize = 4.7f
    val identification = "This identification card is issued to the bearer whose photograph appears herein and who is a bona fide employee of the Barangay Local Government Unit of Sibulan."
    tightDrawWrappedText(canvas, identification, left, cardY(r, 21.0f), rightWidth, normal, 4.7f, 5.5f, 4)

    bold.textAlign = Paint.Align.LEFT
    tightTextFit(canvas, "ISSUED BY:", left, cardY(r, 34.8f), rightWidth, bold, 5.2f, 4.0f)
    tightTextFit(canvas, prefs.getString("issuer_name", "BLGU - SIBULAN") ?: "BLGU - SIBULAN", left, cardY(r, 38.0f), rightWidth, bold, 5.8f, 4.3f)

    tightTextFit(canvas, "APPROVED BY:", left, cardY(r, 42.5f), rightWidth, bold, 5.2f, 4.0f)
    val captainSigRect = cardRect(r, 2.7f, 43.5f, 27f, 7.0f)
    tightLoadBitmap(context, prefs.getString("captain_signature_uri", null))?.let { bitmap ->
        canvas.drawBitmap(bitmap, null, captainSigRect, imagePaint)
        bitmap.recycleSafely()
    }

    val captainName = prefs.getString("captain_name", "ROWENA A. TABO")?.ifBlank { "ROWENA A. TABO" } ?: "ROWENA A. TABO"
    tightTextFit(canvas, captainName.uppercase(), left, cardY(r, 53.2f), mm(30f), bold, 5.8f, 4.2f)
    canvas.drawLine(left, cardY(r, 54.2f), cardX(r, 31.0f), cardY(r, 54.2f), Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK; strokeWidth = 0.55f })
    normal.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
    tightTextFit(canvas, prefs.getString("captain_title", "Punong Barangay") ?: "Punong Barangay", left, cardY(r, 57.0f), mm(30f), normal, 5.0f, 3.7f)

    bold.textAlign = Paint.Align.CENTER
    tightTextFit(canvas, "IMPORTANT NOTICE", r.centerX(), cardY(r, 61.5f), mm(45f), bold, 5.4f, 4.0f)
    normal.textAlign = Paint.Align.LEFT
    val notices = listOf(
        "• This ID is non-transferable.",
        "• This ID remains the property of BLGU-Sibulan.",
        "• If lost, report immediately to the Barangay Office.",
        "• Unauthorized use, alteration, or reproduction of this ID is prohibited."
    )
    val noticeWidth = mm(48f)
    val noticeYs = listOf(64.6f, 67.6f, 70.6f, 73.6f)
    notices.forEachIndexed { index, line ->
        tightTextFit(canvas, line, left, cardY(r, noticeYs[index]), noticeWidth, normal, 4.5f, 3.0f)
    }

    val footer = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.DKGRAY
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
    }
    val officeAddress = prefs.getString(
        "office_address",
        "Barangay Hall, Sitio Centro, Barangay Sibulan, Sta. Cruz, Davao del Sur"
    ) ?: "Barangay Hall, Sitio Centro, Barangay Sibulan, Sta. Cruz, Davao del Sur"
    val officeEmail = prefs.getString("office_email", "brgysibulan8001@gmail.com") ?: "brgysibulan8001@gmail.com"
    val officePhone = prefs.getString("office_phone", "0970 972 3363") ?: "0970 972 3363"
    tightTextFit(canvas, officeAddress, r.centerX(), cardY(r, 79.0f), mm(49f), footer, 4.1f, 2.8f)
    tightTextFit(canvas, officeEmail, r.centerX(), cardY(r, 81.8f), mm(42f), footer, 4.0f, 2.9f)
    tightTextFit(canvas, officePhone, r.centerX(), cardY(r, 84.3f), mm(30f), footer, 4.0f, 3.0f)
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
        canvas.drawRect(r.left, r.top, r.right, r.top + mm(17f), p)
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

private fun tightTextFit(
    canvas: Canvas,
    value: String,
    x: Float,
    y: Float,
    maxWidth: Float,
    paint: Paint,
    maxSize: Float,
    minSize: Float
) {
    val text = value.ifBlank { "—" }
    paint.textSize = maxSize
    while (paint.textSize > minSize && paint.measureText(text) > maxWidth) paint.textSize -= 0.3f
    canvas.drawText(text, x, y, paint)
}

private fun tightDrawWrappedText(
    canvas: Canvas,
    value: String,
    x: Float,
    startY: Float,
    maxWidth: Float,
    paint: Paint,
    textSize: Float,
    lineHeight: Float,
    maxLines: Int
) {
    paint.textSize = textSize
    val words = value.trim().split(Regex("\\s+"))
    if (words.isEmpty()) return
    val lines = mutableListOf<String>()
    var current = ""
    for (word in words) {
        val candidate = if (current.isBlank()) word else "$current $word"
        if (paint.measureText(candidate) <= maxWidth || current.isBlank()) {
            current = candidate
        } else {
            lines += current
            current = word
            if (lines.size >= maxLines - 1) break
        }
    }
    if (current.isNotBlank() && lines.size < maxLines) lines += current
    lines.take(maxLines).forEachIndexed { index, line ->
        canvas.drawText(line, x, startY + index * lineHeight, paint)
    }
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
