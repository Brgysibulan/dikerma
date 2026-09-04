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
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.time.format.DateTimeParseException
import java.time.temporal.ChronoField
import java.util.Locale

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
        Text("A4 portrait • 85 × 115 mm ID front/back • uploaded design stays as the background")

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
                    Text("Person 1 photo: ${if (firstPhotoValid) "Ready" else "Invalid / Missing"}")
                    Text("Person 1 QR: ${if (!it.qrImageUri.isNullOrBlank()) "Ready" else "Missing"}")
                }
                second?.let {
                    Text("Person 2 photo: ${if (secondPhotoValid) "Ready" else "Invalid / Missing"}")
                    Text("Person 2 QR: ${if (!it.qrImageUri.isNullOrBlank()) "Ready" else "Missing"}")
                }
            }
        }

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("A4 layout", fontWeight = FontWeight.Bold)
                Text("Top-left: Person 1 Front")
                Text("Top-right: Person 1 Back")
                Text(if (second == null) "Bottom row: unused and no empty cut boxes" else "Bottom-left: Person 2 Front • Bottom-right: Person 2 Back")
                Text("Each front/back ID is exactly 85 × 115 mm.", style = MaterialTheme.typography.bodySmall)
                Text("Outline and font controls are in Settings.", style = MaterialTheme.typography.bodySmall)
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
private const val ID_W_MM = 85.0f
private const val ID_H_MM = 115.0f
private const val SLOT_LEFT_1_MM = 19.97f
private const val SLOT_LEFT_2_MM = 105.02f
private const val SLOT_TOP_1_MM = 14.77f
private const val SLOT_TOP_2_MM = 168.62f
private const val TIGHT_MAX_BITMAP_SIDE = 2200
private val BRAND_GREEN = Color.rgb(0, 82, 45)

private fun mm(value: Float): Float = value * TIGHT_PT_PER_MM
private fun cardX(r: RectF, valueMm: Float): Float = r.left + mm(valueMm)
private fun cardY(r: RectF, valueMm: Float): Float = r.top + mm(valueMm)
private fun cardRect(r: RectF, leftMm: Float, topMm: Float, widthMm: Float, heightMm: Float): RectF = RectF(
    cardX(r, leftMm),
    cardY(r, topMm),
    cardX(r, leftMm + widthMm),
    cardY(r, topMm + heightMm)
)

private fun idSlot(leftMm: Float, topMm: Float): RectF = RectF(
    mm(leftMm),
    mm(topMm),
    mm(leftMm + ID_W_MM),
    mm(topMm + ID_H_MM)
)

private fun idTypeface(prefs: SharedPreferences, bold: Boolean): Typeface {
    val family = when (prefs.getString("font_family", "sans")) {
        "serif" -> Typeface.SERIF
        "monospace" -> Typeface.MONOSPACE
        else -> Typeface.SANS_SERIF
    }
    return Typeface.create(family, if (bold) Typeface.BOLD else Typeface.NORMAL)
}

private fun idFontSize(prefs: SharedPreferences, base: Float): Float {
    return base * prefs.getFloat("font_scale", 1.0f).coerceIn(0.85f, 1.20f)
}

private fun outlineWidth(prefs: SharedPreferences): Float {
    return prefs.getFloat("outline_thickness", 0.65f).coerceIn(0.30f, 1.50f)
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

        val card1 = idSlot(SLOT_LEFT_1_MM, SLOT_TOP_1_MM)
        val card2 = idSlot(SLOT_LEFT_2_MM, SLOT_TOP_1_MM)
        val card3 = idSlot(SLOT_LEFT_1_MM, SLOT_TOP_2_MM)
        val card4 = idSlot(SLOT_LEFT_2_MM, SLOT_TOP_2_MM)

        tightDrawFront(context, canvas, card1, first, prefs)
        tightDrawBack(context, canvas, card2, first, prefs)

        if (second != null) {
            tightDrawFront(context, canvas, card3, second, prefs)
            tightDrawBack(context, canvas, card4, second, prefs)
        }

        // Cut guides belong above the full-bleed artwork so the ON setting is visible.
        if (prefs.getBoolean("outline_cut_guide", true)) {
            tightDrawCardCutGuide(canvas, card1, prefs)
            tightDrawCardCutGuide(canvas, card2, prefs)
            if (second != null) {
                tightDrawCardCutGuide(canvas, card3, prefs)
                tightDrawCardCutGuide(canvas, card4, prefs)
            }
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

private fun tightDrawCardCutGuide(canvas: Canvas, card: RectF, prefs: SharedPreferences) {
    val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        style = Paint.Style.STROKE
        strokeWidth = outlineWidth(prefs)
    }
    canvas.drawRect(card, p)
}

private fun tightDrawFront(context: Context, canvas: Canvas, r: RectF, e: Employee, prefs: SharedPreferences) {
    tightDrawTemplate(context, canvas, r, prefs.getString("front_template_uri", null), true)

    val blackRegular = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        textAlign = Paint.Align.LEFT
        typeface = idTypeface(prefs, false)
    }
    val centeredBold = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        textAlign = Paint.Align.CENTER
        typeface = idTypeface(prefs, true)
    }

    val logo1Uri = prefs.getString("logo1_uri", null)
    val logo2Uri = prefs.getString("logo2_uri", null)
    if (!logo1Uri.isNullOrBlank()) tightDrawLogo(context, canvas, cardRect(r, 5.0f, 4.0f, 14.0f, 14.0f), logo1Uri)
    if (!logo2Uri.isNullOrBlank()) tightDrawLogo(context, canvas, cardRect(r, 67.0f, 4.0f, 12.0f, 12.0f), logo2Uri)

    tightTextFit(
        canvas,
        prefs.getString("barangay", "BARANGAY SIBULAN") ?: "BARANGAY SIBULAN",
        r.centerX(), cardY(r, 9.5f), mm(44f), centeredBold,
        idFontSize(prefs, 12.5f), idFontSize(prefs, 8.0f)
    )
    tightTextFit(
        canvas,
        "STA. CRUZ, DAVAO DEL SUR",
        r.centerX(), cardY(r, 15.0f), mm(44f), Paint(blackRegular).apply { textAlign = Paint.Align.CENTER },
        idFontSize(prefs, 7.2f), idFontSize(prefs, 5.0f)
    )
    tightTextFit(
        canvas,
        prefs.getString("id_heading", "BARANGAY EMPLOYEE ID") ?: "BARANGAY EMPLOYEE ID",
        r.centerX(), cardY(r, 20.3f), mm(45f), centeredBold,
        idFontSize(prefs, 8.6f), idFontSize(prefs, 6.0f)
    )

    val photoRect = cardRect(r, 6.0f, 27.0f, 31.0f, 40.0f)
    tightLoadBitmap(context, e.photoUri)?.let { bitmap ->
        tightCenterCrop(canvas, bitmap, photoRect)
        bitmap.recycleSafely()
    } ?: tightLabeledBox(canvas, photoRect, "ID PHOTO", prefs)

    if (prefs.getBoolean("outline_photo", false)) {
        canvas.drawRect(photoRect, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            style = Paint.Style.STROKE
            strokeWidth = outlineWidth(prefs)
        })
    }

    val fieldX = cardX(r, 41.0f)
    val fieldWidth = mm(38.0f)
    val label = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        textAlign = Paint.Align.LEFT
        typeface = idTypeface(prefs, true)
        textSize = idFontSize(prefs, 6.8f)
    }
    val value = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        textAlign = Paint.Align.LEFT
        typeface = idTypeface(prefs, true)
    }

    canvas.drawText("NAME", fieldX, cardY(r, 31.0f), label)
    tightDrawTextBlockFit(
        canvas, e.fullName.uppercase(Locale.ENGLISH), fieldX, cardY(r, 36.5f), fieldWidth, value,
        idFontSize(prefs, 10.5f), idFontSize(prefs, 7.0f), 2
    )

    canvas.drawText("DESIGNATION", fieldX, cardY(r, 49.0f), label)
    tightDrawTextBlockFit(
        canvas, e.position.uppercase(Locale.ENGLISH), fieldX, cardY(r, 54.5f), fieldWidth, value,
        idFontSize(prefs, 9.4f), idFontSize(prefs, 6.4f), 2
    )

    canvas.drawText("EMPLOYEE NO.", fieldX, cardY(r, 67.0f), label)
    tightTextFit(canvas, e.controlNumber, fieldX, cardY(r, 73.0f), fieldWidth, value, idFontSize(prefs, 10.0f), idFontSize(prefs, 7.0f))

    if (prefs.getBoolean("outline_info_dividers", false)) {
        val divider = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            strokeWidth = outlineWidth(prefs)
        }
        canvas.drawLine(fieldX, cardY(r, 45.5f), cardX(r, 79.0f), cardY(r, 45.5f), divider)
        canvas.drawLine(fieldX, cardY(r, 63.0f), cardX(r, 79.0f), cardY(r, 63.0f), divider)
        canvas.drawLine(fieldX, cardY(r, 76.0f), cardX(r, 79.0f), cardY(r, 76.0f), divider)
    }

    val signatureRect = cardRect(r, 7.0f, 79.0f, 33.0f, 10.0f)
    tightLoadSignatureBitmap(context, e.signatureUri)?.let { bitmap ->
        tightDrawBitmapFit(canvas, bitmap, signatureRect)
        bitmap.recycleSafely()
    }

    if (prefs.getBoolean("outline_signature_line", false)) {
        canvas.drawLine(
            cardX(r, 6.0f), cardY(r, 90.5f), cardX(r, 41.0f), cardY(r, 90.5f),
            Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK; strokeWidth = outlineWidth(prefs) }
        )
    }
    tightTextFit(
        canvas, "SIGNATURE OF HOLDER", cardX(r, 23.5f), cardY(r, 94.0f), mm(34f), centeredBold,
        idFontSize(prefs, 6.6f), idFontSize(prefs, 4.8f)
    )

    val qrRect = cardRect(r, 57.0f, 82.5f, 20.0f, 20.0f)
    tightLoadBitmap(context, e.qrImageUri)?.let { bitmap ->
        tightDrawBitmapFit(canvas, bitmap, qrRect)
        bitmap.recycleSafely()
    }

    if (prefs.getBoolean("outline_qr", false)) {
        canvas.drawRect(qrRect, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            style = Paint.Style.STROKE
            strokeWidth = outlineWidth(prefs)
        })
    }
    tightTextFit(
        canvas, "SCAN TO VERIFY", cardX(r, 67.0f), cardY(r, 80.0f), mm(24f), centeredBold,
        idFontSize(prefs, 6.3f), idFontSize(prefs, 4.6f)
    )

    val captainName = prefs.getString("captain_name", "ROWENA A. TABO")?.ifBlank { "ROWENA A. TABO" } ?: "ROWENA A. TABO"
    val approver = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
        typeface = idTypeface(prefs, true)
    }
    tightTextFit(canvas, "HON. ${captainName.uppercase(Locale.ENGLISH)}", r.centerX(), cardY(r, 108.0f), mm(70f), approver, idFontSize(prefs, 8.0f), idFontSize(prefs, 5.5f))
    approver.typeface = idTypeface(prefs, false)
    tightTextFit(
        canvas,
        prefs.getString("captain_title", "Punong Barangay") ?: "Punong Barangay",
        r.centerX(), cardY(r, 111.8f), mm(54f), approver,
        idFontSize(prefs, 6.2f), idFontSize(prefs, 4.8f)
    )
}

private fun tightDrawBack(context: Context, canvas: Canvas, r: RectF, e: Employee, prefs: SharedPreferences) {
    tightDrawTemplate(context, canvas, r, prefs.getString("back_template_uri", null), false)

    val label = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        textAlign = Paint.Align.LEFT
        typeface = idTypeface(prefs, true)
        textSize = idFontSize(prefs, 7.0f)
    }
    val normal = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        textAlign = Paint.Align.LEFT
        typeface = idTypeface(prefs, false)
    }
    val bold = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        textAlign = Paint.Align.LEFT
        typeface = idTypeface(prefs, true)
    }

    canvas.drawText("DATE OF BIRTH:", cardX(r, 7.0f), cardY(r, 10.0f), label)
    tightTextFit(canvas, formatBirthdateForPdf(e.birthdate), cardX(r, 30.0f), cardY(r, 10.0f), mm(48f), normal, idFontSize(prefs, 7.8f), idFontSize(prefs, 5.5f))

    canvas.drawText("SEX:", cardX(r, 7.0f), cardY(r, 17.0f), label)
    tightTextFit(canvas, e.sex.ifBlank { "—" }, cardX(r, 17.0f), cardY(r, 17.0f), mm(18f), normal, idFontSize(prefs, 7.8f), idFontSize(prefs, 5.5f))
    canvas.drawText("CIVIL STATUS:", cardX(r, 40.0f), cardY(r, 17.0f), label)
    tightTextFit(canvas, e.civilStatus.ifBlank { "—" }, cardX(r, 63.0f), cardY(r, 17.0f), mm(15f), normal, idFontSize(prefs, 7.2f), idFontSize(prefs, 5.0f))

    canvas.drawText("ADDRESS:", cardX(r, 7.0f), cardY(r, 24.0f), label)
    tightDrawWrappedText(
        canvas, e.address.ifBlank { "—" }, cardX(r, 7.0f), cardY(r, 29.0f), mm(71.0f), normal,
        idFontSize(prefs, 7.2f), mm(3.3f), 2
    )

    val sectionLine = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        strokeWidth = outlineWidth(prefs)
    }
    val showBackDividers = prefs.getBoolean("outline_back_dividers", false)
    if (showBackDividers) canvas.drawLine(cardX(r, 7.0f), cardY(r, 36.5f), cardX(r, 78.0f), cardY(r, 36.5f), sectionLine)

    bold.textAlign = Paint.Align.CENTER
    tightTextFit(canvas, "IDENTIFICATION", r.centerX(), cardY(r, 41.5f), mm(66f), bold, idFontSize(prefs, 8.5f), idFontSize(prefs, 6.5f))
    normal.textAlign = Paint.Align.LEFT
    val identification = "This identification card is issued to the bearer whose photograph appears herein and who is a bona fide employee of the Barangay Local Government Unit of Sibulan."
    tightDrawWrappedText(
        canvas, identification, cardX(r, 9.0f), cardY(r, 46.5f), mm(67.0f), normal,
        idFontSize(prefs, 7.0f), mm(3.5f), 5
    )

    if (showBackDividers) canvas.drawLine(cardX(r, 7.0f), cardY(r, 64.0f), cardX(r, 78.0f), cardY(r, 64.0f), sectionLine)

    bold.textAlign = Paint.Align.LEFT
    tightTextFit(canvas, "ISSUED BY:", cardX(r, 8.0f), cardY(r, 67.0f), mm(30f), bold, idFontSize(prefs, 7.5f), idFontSize(prefs, 5.8f))
    tightTextFit(
        canvas,
        prefs.getString("issuer_name", "BLGU - SIBULAN") ?: "BLGU - SIBULAN",
        cardX(r, 8.0f), cardY(r, 73.0f), mm(30f), bold,
        idFontSize(prefs, 8.0f), idFontSize(prefs, 6.0f)
    )

    tightTextFit(canvas, "APPROVED BY:", cardX(r, 45.0f), cardY(r, 67.0f), mm(31f), bold, idFontSize(prefs, 7.5f), idFontSize(prefs, 5.8f))
    val captainSigRect = cardRect(r, 47.0f, 67.5f, 27.0f, 8.0f)
    tightLoadSignatureBitmap(context, prefs.getString("captain_signature_uri", null))?.let { bitmap ->
        tightDrawBitmapFit(canvas, bitmap, captainSigRect)
        bitmap.recycleSafely()
    }
    val captainName = prefs.getString("captain_name", "ROWENA A. TABO")?.ifBlank { "ROWENA A. TABO" } ?: "ROWENA A. TABO"
    tightTextFit(canvas, captainName.uppercase(Locale.ENGLISH), cardX(r, 45.0f), cardY(r, 78.5f), mm(33f), bold, idFontSize(prefs, 7.4f), idFontSize(prefs, 5.4f))
    normal.typeface = idTypeface(prefs, false)
    tightTextFit(
        canvas,
        prefs.getString("captain_title", "Punong Barangay") ?: "Punong Barangay",
        cardX(r, 45.0f), cardY(r, 82.0f), mm(31f), normal,
        idFontSize(prefs, 6.2f), idFontSize(prefs, 4.8f)
    )

    if (showBackDividers) canvas.drawLine(cardX(r, 7.0f), cardY(r, 83.5f), cardX(r, 78.0f), cardY(r, 83.5f), sectionLine)
    bold.textAlign = Paint.Align.CENTER
    tightTextFit(canvas, "IMPORTANT NOTICE", r.centerX(), cardY(r, 87.0f), mm(65f), bold, idFontSize(prefs, 7.8f), idFontSize(prefs, 5.8f))
    normal.textAlign = Paint.Align.LEFT
    val notices = listOf(
        "- This ID is non-transferable.",
        "- This ID remains the property of the Barangay Local Government Unit of Sibulan.",
        "- If lost, report immediately to the Barangay Office.",
        "- Unauthorized use, alteration, or reproduction of this ID is prohibited."
    )
    var noticeY = cardY(r, 90.5f)
    notices.forEach { line ->
        val linesUsed = tightDrawWrappedText(
            canvas, line, cardX(r, 9.0f), noticeY, mm(67f), normal,
            idFontSize(prefs, 5.7f), mm(2.75f), 2
        )
        noticeY += linesUsed * mm(2.75f)
    }

    val footer = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
        typeface = idTypeface(prefs, false)
    }
    val officeEmail = prefs.getString("office_email", "brgysibulan8001@gmail.com") ?: "brgysibulan8001@gmail.com"
    val officePhone = prefs.getString("office_phone", "0970 972 3363") ?: "0970 972 3363"
    tightTextFit(
        canvas, "Barangay Hall, Sitio Centro, Barangay Sibulan, Sta. Cruz, Davao del Sur",
        r.centerX(), cardY(r, 108.0f), mm(75f), footer, idFontSize(prefs, 5.4f), idFontSize(prefs, 4.1f)
    )
    tightTextFit(canvas, "$officeEmail  |  $officePhone", r.centerX(), cardY(r, 111.5f), mm(75f), footer, idFontSize(prefs, 5.3f), idFontSize(prefs, 4.1f))
}

private fun tightDrawTemplate(context: Context, canvas: Canvas, r: RectF, uri: String?, front: Boolean) {
    val bitmap = tightLoadBitmap(context, uri)
    if (bitmap != null) {
        // The uploaded file is complete artwork for the full 85 x 115 mm card.
        // Map the entire source image to the card; never inset or crop its design.
        tightDrawBitmapFullFrame(canvas, bitmap, r)
        bitmap.recycleSafely()
        return
    }

    val p = Paint(Paint.ANTI_ALIAS_FLAG)
    p.color = Color.WHITE
    canvas.drawRect(r, p)
    if (front) {
        p.color = BRAND_GREEN
        canvas.drawRect(r.left, r.top, r.right, r.top + mm(24f), p)
        canvas.drawRect(r.left, r.bottom - mm(12f), r.right, r.bottom, p)
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

private fun tightDrawBitmapFit(canvas: Canvas, bitmap: Bitmap, target: RectF) {
    if (bitmap.width <= 0 || bitmap.height <= 0 || target.width() <= 0f || target.height() <= 0f) return
    val scale = minOf(target.width() / bitmap.width.toFloat(), target.height() / bitmap.height.toFloat())
    val drawW = bitmap.width * scale
    val drawH = bitmap.height * scale
    val left = target.left + (target.width() - drawW) / 2f
    val top = target.top + (target.height() - drawH) / 2f
    val dst = RectF(left, top, left + drawW, top + drawH)
    canvas.drawBitmap(bitmap, null, dst, Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
}

private fun tightDrawLogo(context: Context, canvas: Canvas, r: RectF, uri: String?) {
    val bitmap = tightLoadBitmap(context, uri)
    if (bitmap != null) {
        tightDrawBitmapFit(canvas, bitmap, r)
        bitmap.recycleSafely()
    }
}

private fun tightLabeledBox(canvas: Canvas, r: RectF, label: String, prefs: SharedPreferences) {
    val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.DKGRAY
        textSize = idFontSize(prefs, 8f)
        textAlign = Paint.Align.CENTER
        typeface = idTypeface(prefs, true)
    }
    canvas.drawText(label, r.centerX(), r.centerY() + 3f, p)
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
): Int {
    if (maxLines <= 0) return 0
    paint.textSize = textSize
    val words = value.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
    if (words.isEmpty()) return 0

    val lines = mutableListOf<String>()
    var current = ""
    var wordIndex = 0
    while (wordIndex < words.size && lines.size < maxLines) {
        val word = words[wordIndex]
        val candidate = if (current.isBlank()) word else "$current $word"
        if (paint.measureText(candidate) <= maxWidth || current.isBlank()) {
            current = candidate
            wordIndex++
        } else {
            lines += current
            current = ""
        }
    }
    if (current.isNotBlank() && lines.size < maxLines) lines += current

    lines.take(maxLines).forEachIndexed { index, line ->
        canvas.drawText(line, x, startY + index * lineHeight, paint)
    }
    return lines.size.coerceAtMost(maxLines)
}

private fun tightDrawTextBlockFit(
    canvas: Canvas,
    value: String,
    x: Float,
    startY: Float,
    maxWidth: Float,
    paint: Paint,
    maxSize: Float,
    minSize: Float,
    maxLines: Int
) {
    val text = value.trim().ifBlank { "—" }
    var size = maxSize
    var lines: List<String>
    do {
        paint.textSize = size
        lines = tightBreakLines(text, paint, maxWidth)
        if (lines.size <= maxLines) break
        size -= 0.3f
    } while (size >= minSize)

    paint.textSize = size.coerceAtLeast(minSize)
    lines = tightBreakLines(text, paint, maxWidth).toMutableList().let { fitted ->
        if (fitted.size <= maxLines) fitted else fitted.take(maxLines).toMutableList().also { shown ->
            var last = shown.last().trimEnd()
            while (last.isNotEmpty() && paint.measureText("$last…") > maxWidth) last = last.dropLast(1)
            shown[shown.lastIndex] = "$last…"
        }
    }
    val lineHeight = paint.textSize * 1.18f
    lines.forEachIndexed { index, line -> canvas.drawText(line, x, startY + index * lineHeight, paint) }
}

private fun tightBreakLines(value: String, paint: Paint, maxWidth: Float): List<String> {
    val words = value.split(Regex("\\s+")).filter { it.isNotBlank() }
    if (words.isEmpty()) return emptyList()
    val lines = mutableListOf<String>()
    var current = ""
    words.forEach { word ->
        val candidate = if (current.isEmpty()) word else "$current $word"
        if (current.isEmpty() || paint.measureText(candidate) <= maxWidth) {
            current = candidate
        } else {
            lines += current
            current = word
        }
    }
    if (current.isNotEmpty()) lines += current
    return lines
}

private fun tightDrawBitmapFullFrame(canvas: Canvas, bitmap: Bitmap, target: RectF) {
    if (bitmap.width <= 0 || bitmap.height <= 0) return
    val source = Rect(0, 0, bitmap.width, bitmap.height)
    canvas.drawBitmap(bitmap, source, target, Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
}

private val PDF_DATE_OUTPUT: DateTimeFormatter =
    DateTimeFormatter.ofPattern("MMMM d, uuuu", Locale.ENGLISH)

private val PDF_DATE_INPUTS: List<DateTimeFormatter> = listOf(
    DateTimeFormatter.ISO_LOCAL_DATE,
    DateTimeFormatter.ofPattern("M/d/uuuu", Locale.ENGLISH),
    DateTimeFormatter.ofPattern("M-d-uuuu", Locale.ENGLISH),
    DateTimeFormatter.ofPattern("uuuu/M/d", Locale.ENGLISH),
    DateTimeFormatter.ofPattern("MMMM d, uuuu", Locale.ENGLISH),
    DateTimeFormatter.ofPattern("MMM d, uuuu", Locale.ENGLISH),
    DateTimeFormatterBuilder()
        .appendPattern("M/d/")
        .appendValueReduced(ChronoField.YEAR, 2, 2, 1950)
        .toFormatter(Locale.ENGLISH)
)

internal fun formatBirthdateForPdf(raw: String): String {
    val value = raw.trim()
    if (value.isEmpty()) return "—"
    PDF_DATE_INPUTS.forEach { formatter ->
        try {
            return LocalDate.parse(value, formatter).format(PDF_DATE_OUTPUT)
        } catch (_: DateTimeParseException) {
            // Try the next supported stored/input format.
        }
    }
    return "—"
}

private fun tightLoadBitmap(context: Context, uriString: String?): Bitmap? {
    if (uriString.isNullOrBlank()) return null
    return runCatching {
        val uri = Uri.parse(uriString)
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@runCatching null

        var sample = 1
        while (maxOf(bounds.outWidth / sample, bounds.outHeight / sample) > TIGHT_MAX_BITMAP_SIDE * 2) sample *= 2

        val options = BitmapFactory.Options().apply {
            inSampleSize = sample
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val decoded = context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, options)
        } ?: return@runCatching null
        decoded.setHasAlpha(true)

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

private fun tightLoadSignatureBitmap(context: Context, uriString: String?): Bitmap? {
    val bitmap = tightLoadBitmap(context, uriString) ?: return null
    if (!isAppProcessedSignature(context, uriString)) return bitmap

    // Repair previously auto-cleaned signatures that were saved with an opaque,
    // uniform rectangle. Keep Original uploads are deliberately left untouched.
    return removeUniformSignatureBackground(bitmap).also { cleaned ->
        if (cleaned !== bitmap) bitmap.recycleSafely()
    }
}

private fun isAppProcessedSignature(context: Context, uriString: String?): Boolean {
    if (uriString.isNullOrBlank()) return false
    return runCatching {
        val uri = Uri.parse(uriString)
        uri.scheme == "content" &&
            uri.authority == "${context.packageName}.fileprovider" &&
            uri.lastPathSegment?.startsWith("signature_") == true
    }.getOrDefault(false)
}

private fun removeUniformSignatureBackground(source: Bitmap): Bitmap {
    if (source.width < 2 || source.height < 2) return source
    val corners = intArrayOf(
        source.getPixel(0, 0),
        source.getPixel(source.width - 1, 0),
        source.getPixel(0, source.height - 1),
        source.getPixel(source.width - 1, source.height - 1)
    )
    if (corners.any { Color.alpha(it) < 245 }) return source

    val bgR = corners.map { Color.red(it) }.sorted()[corners.size / 2]
    val bgG = corners.map { Color.green(it) }.sorted()[corners.size / 2]
    val bgB = corners.map { Color.blue(it) }.sorted()[corners.size / 2]
    val pixels = IntArray(source.width * source.height)
    source.getPixels(pixels, 0, source.width, 0, 0, source.width, source.height)
    var transparentCount = 0
    pixels.indices.forEach { index ->
        val pixel = pixels[index]
        val difference = maxOf(
            kotlin.math.abs(Color.red(pixel) - bgR),
            kotlin.math.abs(Color.green(pixel) - bgG),
            kotlin.math.abs(Color.blue(pixel) - bgB)
        )
        val alpha = ((difference - 12) * 4.5f).toInt().coerceIn(0, 255)
        if (alpha == 0) transparentCount++
        pixels[index] = Color.argb(alpha, 0, 0, 0)
    }
    if (transparentCount < pixels.size / 5) return source
    return Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888).apply {
        setPixels(pixels, 0, source.width, 0, 0, source.width, source.height)
    }
}

private fun Bitmap.recycleSafely() {
    if (!isRecycled) recycle()
}
