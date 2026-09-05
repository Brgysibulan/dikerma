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
import kotlin.math.abs

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
        if (secondId == firstId) secondId = null
    }

    val first = employees.firstOrNull { it.id == firstId }
    val second = employees.firstOrNull { it.id == secondId }
    val frontReady = !prefs.getString("front_template_uri", null).isNullOrBlank()
    val backReady = !prefs.getString("back_template_uri", null).isNullOrBlank()
    val layoutStore = remember(prefs) { IdLayoutStore(prefs) }
    val firstPhotoValid = remember(first?.photoUri) { first?.photoUri?.let { OfflineImageProcessor.isLikelyIdPhoto(context, it) } ?: false }
    val secondPhotoValid = remember(second?.photoUri) { second?.photoUri?.let { OfflineImageProcessor.isLikelyIdPhoto(context, it) } ?: false }
    val photosReady = first != null && firstPhotoValid && (second == null || secondPhotoValid)

    val createPdf = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
        if (uri != null && first != null) {
            message = if (writeTightPortraitPdf(context, uri, first, second, prefs)) {
                "PDF created. Print at Actual Size / 100%."
            } else "Could not create PDF."
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Generate ID", style = MaterialTheme.typography.headlineSmall)
        Text("A4 portrait • 85 × 115 mm ID front/back • saved Layout Studio style is used")

        if (employees.isEmpty()) {
            Card(Modifier.fillMaxWidth()) { Text("No employee record found. Add one in Records first.", Modifier.padding(16.dp)) }
            return@Column
        }

        Text("Person 1", style = MaterialTheme.typography.titleMedium)
        Box {
            OutlinedButton(onClick = { firstMenu = true }, modifier = Modifier.fillMaxWidth()) {
                Text(first?.let { "${it.fullName} • ${it.controlNumber}" } ?: "Choose employee")
            }
            DropdownMenu(firstMenu, { firstMenu = false }) {
                employees.forEach { employee ->
                    DropdownMenuItem(text = { Text("${employee.fullName} • ${employee.controlNumber}") }, onClick = {
                        firstId = employee.id
                        if (secondId == employee.id) secondId = null
                        firstMenu = false
                        message = ""
                    })
                }
            }
        }

        Text("Person 2 (optional)", style = MaterialTheme.typography.titleMedium)
        Box {
            OutlinedButton(onClick = { secondMenu = true }, modifier = Modifier.fillMaxWidth()) {
                Text(second?.let { "${it.fullName} • ${it.controlNumber}" } ?: "Leave bottom row blank")
            }
            DropdownMenu(secondMenu, { secondMenu = false }) {
                DropdownMenuItem(text = { Text("No second person") }, onClick = { secondId = null; secondMenu = false })
                employees.filter { it.id != firstId }.forEach { employee ->
                    DropdownMenuItem(text = { Text("${employee.fullName} • ${employee.controlNumber}") }, onClick = {
                        secondId = employee.id; secondMenu = false; message = ""
                    })
                }
            }
        }

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Ready Check", fontWeight = FontWeight.Bold)
                Text("Front design: ${if (frontReady) "Ready" else "Missing — fallback will be used"}")
                Text("Back design: ${if (backReady) "Ready" else "Missing — fallback will be used"}")
                Text("Layout Studio: ${if (layoutStore.isSaved()) "Saved${if (layoutStore.isLocked()) " and locked" else ""}" else "Professional default"}")
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
        Text("Print using Actual Size / 100%. Huwag Fit to Page.", style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(12.dp))
    }
}

private const val PT_PER_MM = 72f / 25.4f
private const val ID_W_MM = 85f
private const val ID_H_MM = 115f
private const val SLOT_LEFT_1_MM = 19.97f
private const val SLOT_LEFT_2_MM = 105.02f
private const val SLOT_TOP_1_MM = 14.77f
private const val SLOT_TOP_2_MM = 168.62f
private const val MAX_BITMAP_SIDE = 2200
private val BRAND_GREEN = Color.rgb(0, 82, 45)

private fun mm(value: Float): Float = value * PT_PER_MM
private fun cardX(r: RectF, valueMm: Float): Float = r.left + mm(valueMm)
private fun cardY(r: RectF, valueMm: Float): Float = r.top + mm(valueMm)
private fun cardRect(r: RectF, leftMm: Float, topMm: Float, widthMm: Float, heightMm: Float): RectF =
    RectF(cardX(r, leftMm), cardY(r, topMm), cardX(r, leftMm + widthMm), cardY(r, topMm + heightMm))

private fun idSlot(leftMm: Float, topMm: Float): RectF = RectF(mm(leftMm), mm(topMm), mm(leftMm + ID_W_MM), mm(topMm + ID_H_MM))

private fun writeTightPortraitPdf(context: Context, uri: Uri, first: Employee, second: Employee?, prefs: SharedPreferences): Boolean {
    val pdf = PdfDocument()
    return try {
        val page = pdf.startPage(PdfDocument.PageInfo.Builder(595, 842, 1).create())
        val canvas = page.canvas
        canvas.drawColor(Color.WHITE)
        val card1 = idSlot(SLOT_LEFT_1_MM, SLOT_TOP_1_MM)
        val card2 = idSlot(SLOT_LEFT_2_MM, SLOT_TOP_1_MM)
        val card3 = idSlot(SLOT_LEFT_1_MM, SLOT_TOP_2_MM)
        val card4 = idSlot(SLOT_LEFT_2_MM, SLOT_TOP_2_MM)

        drawFront(context, canvas, card1, first, prefs)
        drawBack(context, canvas, card2, first, prefs)
        if (second != null) {
            drawFront(context, canvas, card3, second, prefs)
            drawBack(context, canvas, card4, second, prefs)
        }

        if (prefs.getBoolean("outline_cut_guide", true)) {
            drawCutGuide(canvas, card1, prefs); drawCutGuide(canvas, card2, prefs)
            if (second != null) { drawCutGuide(canvas, card3, prefs); drawCutGuide(canvas, card4, prefs) }
        }

        pdf.finishPage(page)
        context.contentResolver.openOutputStream(uri)?.use { pdf.writeTo(it); true } ?: false
    } catch (_: Exception) {
        false
    } finally { pdf.close() }
}

private fun drawCutGuide(canvas: Canvas, card: RectF, prefs: SharedPreferences) {
    canvas.drawRect(card, Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK; style = Paint.Style.STROKE; strokeWidth = prefs.getFloat("outline_thickness", 0.65f).coerceIn(0.3f, 1.5f)
    })
}

private fun drawFront(context: Context, canvas: Canvas, r: RectF, e: Employee, prefs: SharedPreferences) {
    drawTemplate(context, canvas, r, prefs.getString("front_template_uri", null), true)
    val layout = IdLayoutStore(prefs).loadSide(IdLayoutSide.FRONT)
    fun p(element: IdLayoutElement) = layout.getValue(element)

    drawImage(context, canvas, r, prefs.getString("logo1_uri", null), p(IdLayoutElement.FRONT_LOGO_1))
    drawImage(context, canvas, r, prefs.getString("logo2_uri", null), p(IdLayoutElement.FRONT_LOGO_2))
    drawSingle(canvas, r, prefs, IdLayoutElement.FRONT_BARANGAY, p(IdLayoutElement.FRONT_BARANGAY), prefs.getString("barangay", "BARANGAY SIBULAN") ?: "BARANGAY SIBULAN")
    drawSingle(canvas, r, prefs, IdLayoutElement.FRONT_MUNICIPALITY, p(IdLayoutElement.FRONT_MUNICIPALITY), cleanPlaceLabel(prefs.getString("municipality", "Municipality of Sta. Cruz"), "Municipality of", "STA. CRUZ"))
    drawSingle(canvas, r, prefs, IdLayoutElement.FRONT_PROVINCE, p(IdLayoutElement.FRONT_PROVINCE), cleanPlaceLabel(prefs.getString("province", "Province of Davao del Sur"), "Province of", "DAVAO DEL SUR"))
    drawSingle(canvas, r, prefs, IdLayoutElement.FRONT_ID_TITLE, p(IdLayoutElement.FRONT_ID_TITLE), prefs.getString("id_heading", "BARANGAY EMPLOYEE ID") ?: "BARANGAY EMPLOYEE ID")

    val photo = p(IdLayoutElement.FRONT_PHOTO)
    if (photo.visible) {
        val rect = layoutRect(r, photo)
        loadBitmap(context, e.photoUri)?.let { centerCrop(canvas, it, rect); it.recycleSafely() }
        if (prefs.getBoolean("outline_photo", false)) drawOptionalOutline(canvas, rect, prefs)
    }

    drawSingle(canvas, r, prefs, IdLayoutElement.FRONT_NAME_LABEL, p(IdLayoutElement.FRONT_NAME_LABEL), "NAME")
    drawBlock(canvas, r, prefs, IdLayoutElement.FRONT_NAME_VALUE, p(IdLayoutElement.FRONT_NAME_VALUE), e.fullName.uppercase(Locale.ENGLISH), 2)
    drawSingle(canvas, r, prefs, IdLayoutElement.FRONT_DESIGNATION_LABEL, p(IdLayoutElement.FRONT_DESIGNATION_LABEL), "DESIGNATION")
    drawBlock(canvas, r, prefs, IdLayoutElement.FRONT_DESIGNATION_VALUE, p(IdLayoutElement.FRONT_DESIGNATION_VALUE), e.position.uppercase(Locale.ENGLISH), 2)
    drawSingle(canvas, r, prefs, IdLayoutElement.FRONT_EMPLOYEE_NO_LABEL, p(IdLayoutElement.FRONT_EMPLOYEE_NO_LABEL), "EMPLOYEE NO.")
    drawSingle(canvas, r, prefs, IdLayoutElement.FRONT_EMPLOYEE_NO_VALUE, p(IdLayoutElement.FRONT_EMPLOYEE_NO_VALUE), e.controlNumber)

    if (prefs.getBoolean("outline_info_dividers", false)) {
        listOf(IdLayoutElement.FRONT_NAME_VALUE, IdLayoutElement.FRONT_DESIGNATION_VALUE, IdLayoutElement.FRONT_EMPLOYEE_NO_VALUE).forEach { element ->
            val rect = layoutRect(r, p(element))
            canvas.drawLine(rect.left, rect.bottom, rect.right, rect.bottom, linePaint(prefs))
        }
    }

    val signature = p(IdLayoutElement.FRONT_SIGNATURE)
    if (signature.visible) {
        val rect = layoutRect(r, signature)
        loadSignatureBitmap(context, e.signatureUri)?.let { fitBitmap(canvas, it, rect); it.recycleSafely() }
        if (prefs.getBoolean("outline_signature_line", false)) canvas.drawLine(rect.left, rect.bottom + mm(1f), rect.right, rect.bottom + mm(1f), linePaint(prefs))
    }
    drawSingle(canvas, r, prefs, IdLayoutElement.FRONT_SIGNATURE_LABEL, p(IdLayoutElement.FRONT_SIGNATURE_LABEL), "SIGNATURE OF HOLDER")
    drawSingle(canvas, r, prefs, IdLayoutElement.FRONT_QR_LABEL, p(IdLayoutElement.FRONT_QR_LABEL), "SCAN TO VERIFY")

    val qr = p(IdLayoutElement.FRONT_QR)
    if (qr.visible) {
        val rect = layoutRect(r, qr)
        loadBitmap(context, e.qrImageUri)?.let { fitBitmap(canvas, it, rect); it.recycleSafely() }
        if (prefs.getBoolean("outline_qr", false)) drawOptionalOutline(canvas, rect, prefs)
    }
}

private fun drawBack(context: Context, canvas: Canvas, r: RectF, e: Employee, prefs: SharedPreferences) {
    drawTemplate(context, canvas, r, prefs.getString("back_template_uri", null), false)
    val layout = IdLayoutStore(prefs).loadSide(IdLayoutSide.BACK)
    fun p(element: IdLayoutElement) = layout.getValue(element)

    drawSingle(canvas, r, prefs, IdLayoutElement.BACK_DOB_LABEL, p(IdLayoutElement.BACK_DOB_LABEL), "DATE OF BIRTH:")
    drawSingle(canvas, r, prefs, IdLayoutElement.BACK_DOB_VALUE, p(IdLayoutElement.BACK_DOB_VALUE), formatBirthdateForPdf(e.birthdate))
    drawSingle(canvas, r, prefs, IdLayoutElement.BACK_SEX_LABEL, p(IdLayoutElement.BACK_SEX_LABEL), "SEX:")
    drawSingle(canvas, r, prefs, IdLayoutElement.BACK_SEX_VALUE, p(IdLayoutElement.BACK_SEX_VALUE), e.sex.ifBlank { "—" })
    drawSingle(canvas, r, prefs, IdLayoutElement.BACK_CIVIL_LABEL, p(IdLayoutElement.BACK_CIVIL_LABEL), "CIVIL STATUS:")
    drawSingle(canvas, r, prefs, IdLayoutElement.BACK_CIVIL_VALUE, p(IdLayoutElement.BACK_CIVIL_VALUE), e.civilStatus.ifBlank { "—" })
    drawSingle(canvas, r, prefs, IdLayoutElement.BACK_ADDRESS_LABEL, p(IdLayoutElement.BACK_ADDRESS_LABEL), "ADDRESS:")
    drawWrapped(canvas, r, prefs, IdLayoutElement.BACK_ADDRESS_VALUE, p(IdLayoutElement.BACK_ADDRESS_VALUE), e.address.ifBlank { "—" })

    drawSingle(canvas, r, prefs, IdLayoutElement.BACK_IDENTIFICATION_HEADING, p(IdLayoutElement.BACK_IDENTIFICATION_HEADING), "IDENTIFICATION")
    drawWrapped(canvas, r, prefs, IdLayoutElement.BACK_IDENTIFICATION_BODY, p(IdLayoutElement.BACK_IDENTIFICATION_BODY), "This identification card is issued to the bearer whose photograph appears herein and who is a bona fide employee of the Barangay Local Government Unit of Sibulan.")

    drawSingle(canvas, r, prefs, IdLayoutElement.BACK_ISSUED_LABEL, p(IdLayoutElement.BACK_ISSUED_LABEL), "ISSUED BY:")
    drawSingle(canvas, r, prefs, IdLayoutElement.BACK_ISSUER_VALUE, p(IdLayoutElement.BACK_ISSUER_VALUE), prefs.getString("issuer_name", "BLGU - SIBULAN") ?: "BLGU - SIBULAN")
    drawSingle(canvas, r, prefs, IdLayoutElement.BACK_APPROVED_LABEL, p(IdLayoutElement.BACK_APPROVED_LABEL), "APPROVED BY:")
    drawImage(context, canvas, r, prefs.getString("captain_signature_uri", null), p(IdLayoutElement.BACK_CAPTAIN_SIGNATURE), true)
    val captainName = prefs.getString("captain_name", "ROWENA A. TABO")?.ifBlank { "ROWENA A. TABO" } ?: "ROWENA A. TABO"
    drawSingle(canvas, r, prefs, IdLayoutElement.BACK_CAPTAIN_NAME, p(IdLayoutElement.BACK_CAPTAIN_NAME), captainName.uppercase(Locale.ENGLISH))
    drawSingle(canvas, r, prefs, IdLayoutElement.BACK_CAPTAIN_TITLE, p(IdLayoutElement.BACK_CAPTAIN_TITLE), prefs.getString("captain_title", "Punong Barangay") ?: "Punong Barangay")

    drawSingle(canvas, r, prefs, IdLayoutElement.BACK_NOTICE_HEADING, p(IdLayoutElement.BACK_NOTICE_HEADING), "IMPORTANT NOTICE")
    val notices = listOf(
        "– This ID is non-transferable.",
        "– This ID remains the property of the Barangay Local Government Unit of Sibulan.",
        "– If lost, report immediately to the Barangay Office.",
        "– Unauthorized use, alteration, or reproduction of this ID is prohibited."
    ).joinToString("\n")
    drawWrapped(canvas, r, prefs, IdLayoutElement.BACK_NOTICE_BODY, p(IdLayoutElement.BACK_NOTICE_BODY), notices, preserveParagraphs = true)

    val email = prefs.getString("office_email", "brgysibulan8001@gmail.com") ?: "brgysibulan8001@gmail.com"
    val phone = prefs.getString("office_phone", "0970 972 3363") ?: "0970 972 3363"
    drawSingle(canvas, r, prefs, IdLayoutElement.BACK_FOOTER_ADDRESS, p(IdLayoutElement.BACK_FOOTER_ADDRESS), "Barangay Hall, Sitio Centro, Barangay Sibulan, Sta. Cruz, Davao del Sur")
    drawSingle(canvas, r, prefs, IdLayoutElement.BACK_FOOTER_CONTACT, p(IdLayoutElement.BACK_FOOTER_CONTACT), "$email  |  $phone")

    if (prefs.getBoolean("outline_back_dividers", false)) {
        listOf(IdLayoutElement.BACK_ADDRESS_VALUE, IdLayoutElement.BACK_IDENTIFICATION_BODY, IdLayoutElement.BACK_CAPTAIN_TITLE).forEach { element ->
            val rect = layoutRect(r, p(element))
            canvas.drawLine(cardX(r, 7f), rect.bottom + mm(1f), cardX(r, 78f), rect.bottom + mm(1f), linePaint(prefs))
        }
    }
}

private fun layoutRect(card: RectF, p: IdElementPlacement): RectF = cardRect(card, p.xMm, p.yMm, p.widthMm, p.heightMm)

private fun globalFontScale(prefs: SharedPreferences): Float = prefs.getFloat("font_scale", 1f).coerceIn(0.85f, 1.2f)
private fun textSize(prefs: SharedPreferences, element: IdLayoutElement, p: IdElementPlacement): Float = element.defaultFontPt * p.fontScale * globalFontScale(prefs)

private fun typeface(p: IdElementPlacement): Typeface {
    val family = when (p.fontFamilyKey) { "serif" -> Typeface.SERIF; "monospace" -> Typeface.MONOSPACE; else -> Typeface.SANS_SERIF }
    return Typeface.create(family, if (p.bold) Typeface.BOLD else Typeface.NORMAL)
}

private fun textPaint(p: IdElementPlacement): Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
    color = p.textColor
    typeface = typeface(p)
    textAlign = when (p.alignment) { IdTextAlignment.LEFT -> Paint.Align.LEFT; IdTextAlignment.CENTER -> Paint.Align.CENTER; IdTextAlignment.RIGHT -> Paint.Align.RIGHT }
}

private fun textX(rect: RectF, alignment: IdTextAlignment): Float = when (alignment) {
    IdTextAlignment.LEFT -> rect.left
    IdTextAlignment.CENTER -> rect.centerX()
    IdTextAlignment.RIGHT -> rect.right
}

private fun drawSingle(canvas: Canvas, card: RectF, prefs: SharedPreferences, element: IdLayoutElement, placement: IdElementPlacement, raw: String) {
    if (!placement.visible) return
    val rect = layoutRect(card, placement)
    val paint = textPaint(placement)
    val value = raw.trim().ifBlank { "—" }
    val maxSize = textSize(prefs, element, placement)
    val minSize = maxSize * 0.58f
    paint.textSize = maxSize
    while (paint.textSize > minSize && (paint.measureText(value) > rect.width() || paint.fontMetrics.descent - paint.fontMetrics.ascent > rect.height())) paint.textSize -= 0.25f
    val baseline = rect.top - paint.fontMetrics.ascent
    drawStyledText(canvas, value, textX(rect, placement.alignment), baseline, rect, paint, placement)
}

private fun drawBlock(canvas: Canvas, card: RectF, prefs: SharedPreferences, element: IdLayoutElement, placement: IdElementPlacement, raw: String, maxLines: Int) {
    if (!placement.visible) return
    val rect = layoutRect(card, placement)
    val paint = textPaint(placement)
    val value = raw.trim().ifBlank { "—" }
    val maxSize = textSize(prefs, element, placement)
    val minSize = maxSize * 0.62f
    var size = maxSize
    var lines = emptyList<String>()
    while (size >= minSize) {
        paint.textSize = size
        lines = breakLines(value, paint, rect.width())
        val h = paint.textSize * 1.18f
        if (lines.size <= maxLines && lines.size * h <= rect.height() + 0.5f) break
        size -= 0.25f
    }
    paint.textSize = size.coerceAtLeast(minSize)
    lines = ellipsizeLines(breakLines(value, paint, rect.width()), maxLines, rect.width(), paint)
    val lineHeight = paint.textSize * 1.18f
    val firstBaseline = rect.top - paint.fontMetrics.ascent
    lines.forEachIndexed { index, line -> drawStyledText(canvas, line, textX(rect, placement.alignment), firstBaseline + index * lineHeight, rect, paint, placement) }
}

private fun drawWrapped(canvas: Canvas, card: RectF, prefs: SharedPreferences, element: IdLayoutElement, placement: IdElementPlacement, value: String, preserveParagraphs: Boolean = false) {
    if (!placement.visible) return
    val rect = layoutRect(card, placement)
    val paint = textPaint(placement)
    val maxSize = textSize(prefs, element, placement)
    val minSize = maxSize * 0.66f
    var size = maxSize
    var lines: List<String>
    while (true) {
        paint.textSize = size
        lines = if (preserveParagraphs) value.lines().flatMap { if (it.isBlank()) listOf("") else breakLines(it.trim(), paint, rect.width()) } else breakLines(value.replace('\n', ' '), paint, rect.width())
        val lineHeight = paint.textSize * 1.25f
        if (lines.size * lineHeight <= rect.height() || size <= minSize) break
        size -= 0.25f
    }
    paint.textSize = size.coerceAtLeast(minSize)
    val lineHeight = paint.textSize * 1.25f
    val maxLines = (rect.height() / lineHeight).toInt().coerceAtLeast(1)
    lines = ellipsizeLines(lines, maxLines, rect.width(), paint)
    val firstBaseline = rect.top - paint.fontMetrics.ascent
    lines.forEachIndexed { index, line -> drawStyledText(canvas, line, textX(rect, placement.alignment), firstBaseline + index * lineHeight, rect, paint, placement) }
}

private fun drawStyledText(canvas: Canvas, value: String, x: Float, baseline: Float, rect: RectF, paint: Paint, p: IdElementPlacement) {
    if (p.shadowEnabled && p.shadowOpacity > 0f) {
        val shadow = Paint(paint).apply {
            val a = (Color.alpha(p.shadowColor) * p.shadowOpacity).toInt().coerceIn(0, 255)
            color = Color.argb(a, Color.red(p.shadowColor), Color.green(p.shadowColor), Color.blue(p.shadowColor))
            style = Paint.Style.FILL
            if (p.shadowRadiusPt > 0f) setShadowLayer(p.shadowRadiusPt, 0f, 0f, color)
        }
        canvas.drawText(value, x + mm(p.shadowDxMm), baseline + mm(p.shadowDyMm), shadow)
        shadow.clearShadowLayer()
    }

    if (p.textOutlineEnabled) {
        val outline = Paint(paint).apply {
            color = p.textOutlineColor
            style = Paint.Style.STROKE
            strokeJoin = Paint.Join.ROUND
            strokeWidth = p.textOutlineWidthPt
        }
        canvas.drawText(value, x, baseline, outline)
    }
    canvas.drawText(value, x, baseline, paint)

    if (p.underlineEnabled) {
        val textWidth = paint.measureText(value).coerceAtMost(rect.width())
        val (start, end) = if (p.underlineWidthMode == IdUnderlineWidthMode.ELEMENT) {
            rect.left to rect.right
        } else when (p.alignment) {
            IdTextAlignment.LEFT -> x to (x + textWidth)
            IdTextAlignment.CENTER -> (x - textWidth / 2f) to (x + textWidth / 2f)
            IdTextAlignment.RIGHT -> (x - textWidth) to x
        }
        val y = baseline + paint.fontMetrics.descent.coerceAtLeast(0f) + mm(p.underlineOffsetMm)
        canvas.drawLine(start, y, end, y, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = p.underlineColor; strokeWidth = p.underlineThicknessPt })
    }
}

private fun drawImage(context: Context, canvas: Canvas, card: RectF, uri: String?, p: IdElementPlacement, signature: Boolean = false) {
    if (!p.visible) return
    val bitmap = if (signature) loadSignatureBitmap(context, uri) else loadBitmap(context, uri)
    bitmap?.let { fitBitmap(canvas, it, layoutRect(card, p)); it.recycleSafely() }
}

private fun linePaint(prefs: SharedPreferences) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
    color = Color.BLACK; strokeWidth = prefs.getFloat("outline_thickness", 0.65f).coerceIn(0.3f, 1.5f)
}

private fun drawOptionalOutline(canvas: Canvas, rect: RectF, prefs: SharedPreferences) {
    canvas.drawRect(rect, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK; style = Paint.Style.STROKE; strokeWidth = prefs.getFloat("outline_thickness", 0.65f).coerceIn(0.3f, 1.5f) })
}

private fun drawTemplate(context: Context, canvas: Canvas, r: RectF, uri: String?, front: Boolean) {
    val bitmap = loadBitmap(context, uri)
    if (bitmap != null) {
        drawBitmapFullFrame(canvas, bitmap, r)
        bitmap.recycleSafely()
        return
    }
    val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
    canvas.drawRect(r, p)
    if (front) {
        p.color = BRAND_GREEN
        canvas.drawRect(r.left, r.top, r.right, r.top + mm(24f), p)
    }
}

private fun centerCrop(canvas: Canvas, bitmap: Bitmap, target: RectF) {
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

private fun fitBitmap(canvas: Canvas, bitmap: Bitmap, target: RectF) {
    if (bitmap.width <= 0 || bitmap.height <= 0) return
    val scale = minOf(target.width() / bitmap.width, target.height() / bitmap.height)
    val w = bitmap.width * scale; val h = bitmap.height * scale
    val left = target.left + (target.width() - w) / 2f; val top = target.top + (target.height() - h) / 2f
    canvas.drawBitmap(bitmap, null, RectF(left, top, left + w, top + h), Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
}

private fun drawBitmapFullFrame(canvas: Canvas, bitmap: Bitmap, target: RectF) {
    canvas.drawBitmap(bitmap, Rect(0, 0, bitmap.width, bitmap.height), target, Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
}

private fun breakLines(value: String, paint: Paint, maxWidth: Float): List<String> {
    val words = value.split(Regex("\\s+")).filter { it.isNotBlank() }
    if (words.isEmpty()) return emptyList()
    val lines = mutableListOf<String>(); var current = ""
    words.forEach { word ->
        val candidate = if (current.isBlank()) word else "$current $word"
        if (current.isBlank() || paint.measureText(candidate) <= maxWidth) current = candidate else { lines += current; current = word }
    }
    if (current.isNotBlank()) lines += current
    return lines
}

private fun ellipsizeLines(lines: List<String>, maxLines: Int, maxWidth: Float, paint: Paint): List<String> {
    if (lines.size <= maxLines) return lines
    val shown = lines.take(maxLines).toMutableList(); var last = shown.last().trimEnd()
    while (last.isNotEmpty() && paint.measureText("$last…") > maxWidth) last = last.dropLast(1)
    shown[shown.lastIndex] = "$last…"
    return shown
}

private val DATE_OUTPUT = DateTimeFormatter.ofPattern("MMMM d, uuuu", Locale.ENGLISH)
private val DATE_INPUTS = listOf(
    DateTimeFormatter.ISO_LOCAL_DATE,
    DateTimeFormatter.ofPattern("M/d/uuuu", Locale.ENGLISH),
    DateTimeFormatter.ofPattern("M-d-uuuu", Locale.ENGLISH),
    DateTimeFormatter.ofPattern("uuuu/M/d", Locale.ENGLISH),
    DateTimeFormatter.ofPattern("MMMM d, uuuu", Locale.ENGLISH),
    DateTimeFormatter.ofPattern("MMM d, uuuu", Locale.ENGLISH),
    DateTimeFormatterBuilder().appendPattern("M/d/").appendValueReduced(ChronoField.YEAR, 2, 2, 1950).toFormatter(Locale.ENGLISH)
)

internal fun formatBirthdateForPdf(raw: String): String {
    val value = raw.trim(); if (value.isEmpty()) return "—"
    DATE_INPUTS.forEach { formatter ->
        try { return LocalDate.parse(value, formatter).format(DATE_OUTPUT) } catch (_: DateTimeParseException) { }
    }
    return "—"
}

private fun loadBitmap(context: Context, uriString: String?): Bitmap? {
    if (uriString.isNullOrBlank()) return null
    return runCatching {
        val uri = Uri.parse(uriString)
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@runCatching null
        var sample = 1
        while (maxOf(bounds.outWidth / sample, bounds.outHeight / sample) > MAX_BITMAP_SIDE * 2) sample *= 2
        val options = BitmapFactory.Options().apply { inSampleSize = sample; inPreferredConfig = Bitmap.Config.ARGB_8888 }
        val decoded = context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) } ?: return@runCatching null
        decoded.setHasAlpha(true)
        val maxSide = maxOf(decoded.width, decoded.height)
        if (maxSide <= MAX_BITMAP_SIDE) return@runCatching decoded
        val scale = MAX_BITMAP_SIDE.toFloat() / maxSide
        val scaled = Bitmap.createScaledBitmap(decoded, maxOf(1, (decoded.width * scale).toInt()), maxOf(1, (decoded.height * scale).toInt()), true)
        if (scaled !== decoded) decoded.recycleSafely()
        scaled
    }.getOrNull()
}

private fun loadSignatureBitmap(context: Context, uriString: String?): Bitmap? {
    val bitmap = loadBitmap(context, uriString) ?: return null
    if (!isAppProcessedSignature(context, uriString)) return bitmap
    return removeUniformSignatureBackground(bitmap).also { cleaned -> if (cleaned !== bitmap) bitmap.recycleSafely() }
}

private fun isAppProcessedSignature(context: Context, uriString: String?): Boolean {
    if (uriString.isNullOrBlank()) return false
    return runCatching {
        val uri = Uri.parse(uriString)
        uri.scheme == "content" && uri.authority == "${context.packageName}.fileprovider" && uri.lastPathSegment?.startsWith("signature_") == true
    }.getOrDefault(false)
}

private fun removeUniformSignatureBackground(source: Bitmap): Bitmap {
    if (source.width < 2 || source.height < 2) return source
    val corners = intArrayOf(source.getPixel(0, 0), source.getPixel(source.width - 1, 0), source.getPixel(0, source.height - 1), source.getPixel(source.width - 1, source.height - 1))
    if (corners.any { Color.alpha(it) < 245 }) return source
    val bgR = corners.map { Color.red(it) }.sorted()[2]
    val bgG = corners.map { Color.green(it) }.sorted()[2]
    val bgB = corners.map { Color.blue(it) }.sorted()[2]
    val pixels = IntArray(source.width * source.height)
    source.getPixels(pixels, 0, source.width, 0, 0, source.width, source.height)
    var transparent = 0
    pixels.indices.forEach { i ->
        val px = pixels[i]
        val difference = maxOf(abs(Color.red(px) - bgR), abs(Color.green(px) - bgG), abs(Color.blue(px) - bgB))
        val alpha = ((difference - 12) * 4.5f).toInt().coerceIn(0, 255)
        if (alpha == 0) transparent++
        pixels[i] = Color.argb(alpha, 0, 0, 0)
    }
    if (transparent < pixels.size / 5) return source
    return Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888).apply { setPixels(pixels, 0, source.width, 0, 0, source.width, source.height) }
}

private fun Bitmap.recycleSafely() { if (!isRecycled) recycle() }
