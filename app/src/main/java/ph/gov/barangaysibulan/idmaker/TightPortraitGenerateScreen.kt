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
    val layoutStore = remember(prefs) { IdLayoutStore(prefs) }
    val layoutSaved = layoutStore.isSaved()
    val layoutLocked = layoutStore.isLocked()

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
                Text(
                    "Overlay placement: ${if (layoutSaved) "Saved${if (layoutLocked) " and locked" else ""}" else "Professional default"}"
                )
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
                Text("Saved Layout Studio placement is applied uniformly to both people.", style = MaterialTheme.typography.bodySmall)
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
    val layout = IdLayoutStore(prefs).loadSide(IdLayoutSide.FRONT)
    fun item(element: IdLayoutElement): IdElementPlacement = layout.getValue(element)

    drawLayoutImage(context, canvas, r, prefs.getString("logo1_uri", null), item(IdLayoutElement.FRONT_LOGO_1))
    drawLayoutImage(context, canvas, r, prefs.getString("logo2_uri", null), item(IdLayoutElement.FRONT_LOGO_2))

    drawLayoutSingleLine(
        canvas, r, prefs,
        IdLayoutElement.FRONT_BARANGAY, item(IdLayoutElement.FRONT_BARANGAY),
        prefs.getString("barangay", "BARANGAY SIBULAN") ?: "BARANGAY SIBULAN", bold = true
    )
    drawLayoutSingleLine(
        canvas, r, prefs,
        IdLayoutElement.FRONT_LOCATION, item(IdLayoutElement.FRONT_LOCATION),
        "STA. CRUZ, DAVAO DEL SUR", bold = false
    )
    drawLayoutSingleLine(
        canvas, r, prefs,
        IdLayoutElement.FRONT_ID_TITLE, item(IdLayoutElement.FRONT_ID_TITLE),
        prefs.getString("id_heading", "BARANGAY EMPLOYEE ID") ?: "BARANGAY EMPLOYEE ID", bold = true
    )

    val photoPlacement = item(IdLayoutElement.FRONT_PHOTO)
    val photoRect = layoutRect(r, photoPlacement)
    if (photoPlacement.visible) {
        tightLoadBitmap(context, e.photoUri)?.let { bitmap ->
            tightCenterCrop(canvas, bitmap, photoRect)
            bitmap.recycleSafely()
        }
        if (prefs.getBoolean("outline_photo", false)) drawOptionalRectOutline(canvas, photoRect, prefs)
    }

    drawLayoutSingleLine(canvas, r, prefs, IdLayoutElement.FRONT_NAME_LABEL, item(IdLayoutElement.FRONT_NAME_LABEL), "NAME", bold = true)
    drawLayoutTextBlock(
        canvas, r, prefs, IdLayoutElement.FRONT_NAME_VALUE, item(IdLayoutElement.FRONT_NAME_VALUE),
        e.fullName.uppercase(Locale.ENGLISH), bold = true, maxLines = 2
    )
    drawLayoutSingleLine(canvas, r, prefs, IdLayoutElement.FRONT_DESIGNATION_LABEL, item(IdLayoutElement.FRONT_DESIGNATION_LABEL), "DESIGNATION", bold = true)
    drawLayoutTextBlock(
        canvas, r, prefs, IdLayoutElement.FRONT_DESIGNATION_VALUE, item(IdLayoutElement.FRONT_DESIGNATION_VALUE),
        e.position.uppercase(Locale.ENGLISH), bold = true, maxLines = 2
    )
    drawLayoutSingleLine(canvas, r, prefs, IdLayoutElement.FRONT_EMPLOYEE_NO_LABEL, item(IdLayoutElement.FRONT_EMPLOYEE_NO_LABEL), "EMPLOYEE NO.", bold = true)
    drawLayoutSingleLine(
        canvas, r, prefs, IdLayoutElement.FRONT_EMPLOYEE_NO_VALUE, item(IdLayoutElement.FRONT_EMPLOYEE_NO_VALUE),
        e.controlNumber, bold = true
    )

    if (prefs.getBoolean("outline_info_dividers", false)) {
        val divider = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK; strokeWidth = outlineWidth(prefs) }
        listOf(
            IdLayoutElement.FRONT_NAME_VALUE,
            IdLayoutElement.FRONT_DESIGNATION_VALUE,
            IdLayoutElement.FRONT_EMPLOYEE_NO_VALUE
        ).forEach { element ->
            val placement = item(element)
            if (placement.visible) {
                val field = layoutRect(r, placement)
                canvas.drawLine(field.left, field.bottom, field.right, field.bottom, divider)
            }
        }
    }

    val signaturePlacement = item(IdLayoutElement.FRONT_SIGNATURE)
    val signatureRect = layoutRect(r, signaturePlacement)
    if (signaturePlacement.visible) {
        tightLoadSignatureBitmap(context, e.signatureUri)?.let { bitmap ->
            tightDrawBitmapFit(canvas, bitmap, signatureRect)
            bitmap.recycleSafely()
        }
        if (prefs.getBoolean("outline_signature_line", false)) {
            canvas.drawLine(
                signatureRect.left, signatureRect.bottom + mm(1f), signatureRect.right, signatureRect.bottom + mm(1f),
                Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK; strokeWidth = outlineWidth(prefs) }
            )
        }
    }
    drawLayoutSingleLine(
        canvas, r, prefs, IdLayoutElement.FRONT_SIGNATURE_LABEL, item(IdLayoutElement.FRONT_SIGNATURE_LABEL),
        "SIGNATURE OF HOLDER", bold = true
    )

    drawLayoutSingleLine(
        canvas, r, prefs, IdLayoutElement.FRONT_QR_LABEL, item(IdLayoutElement.FRONT_QR_LABEL),
        "SCAN TO VERIFY", bold = true
    )
    val qrPlacement = item(IdLayoutElement.FRONT_QR)
    val qrRect = layoutRect(r, qrPlacement)
    if (qrPlacement.visible) {
        tightLoadBitmap(context, e.qrImageUri)?.let { bitmap ->
            tightDrawBitmapFit(canvas, bitmap, qrRect)
            bitmap.recycleSafely()
        }
        if (prefs.getBoolean("outline_qr", false)) drawOptionalRectOutline(canvas, qrRect, prefs)
    }
}

private fun tightDrawBack(context: Context, canvas: Canvas, r: RectF, e: Employee, prefs: SharedPreferences) {
    tightDrawTemplate(context, canvas, r, prefs.getString("back_template_uri", null), false)
    val layout = IdLayoutStore(prefs).loadSide(IdLayoutSide.BACK)
    fun item(element: IdLayoutElement): IdElementPlacement = layout.getValue(element)

    drawLayoutSingleLine(canvas, r, prefs, IdLayoutElement.BACK_DOB_LABEL, item(IdLayoutElement.BACK_DOB_LABEL), "DATE OF BIRTH:", bold = true)
    drawLayoutSingleLine(canvas, r, prefs, IdLayoutElement.BACK_DOB_VALUE, item(IdLayoutElement.BACK_DOB_VALUE), formatBirthdateForPdf(e.birthdate), bold = false)
    drawLayoutSingleLine(canvas, r, prefs, IdLayoutElement.BACK_SEX_LABEL, item(IdLayoutElement.BACK_SEX_LABEL), "SEX:", bold = true)
    drawLayoutSingleLine(canvas, r, prefs, IdLayoutElement.BACK_SEX_VALUE, item(IdLayoutElement.BACK_SEX_VALUE), e.sex.ifBlank { "—" }, bold = false)
    drawLayoutSingleLine(canvas, r, prefs, IdLayoutElement.BACK_CIVIL_LABEL, item(IdLayoutElement.BACK_CIVIL_LABEL), "CIVIL STATUS:", bold = true)
    drawLayoutSingleLine(canvas, r, prefs, IdLayoutElement.BACK_CIVIL_VALUE, item(IdLayoutElement.BACK_CIVIL_VALUE), e.civilStatus.ifBlank { "—" }, bold = false)
    drawLayoutSingleLine(canvas, r, prefs, IdLayoutElement.BACK_ADDRESS_LABEL, item(IdLayoutElement.BACK_ADDRESS_LABEL), "ADDRESS:", bold = true)
    drawLayoutWrappedText(
        canvas, r, prefs, IdLayoutElement.BACK_ADDRESS_VALUE, item(IdLayoutElement.BACK_ADDRESS_VALUE),
        e.address.ifBlank { "—" }, bold = false
    )

    val identification = "This identification card is issued to the bearer whose photograph appears herein and who is a bona fide employee of the Barangay Local Government Unit of Sibulan."
    drawLayoutSingleLine(
        canvas, r, prefs, IdLayoutElement.BACK_IDENTIFICATION_HEADING, item(IdLayoutElement.BACK_IDENTIFICATION_HEADING),
        "IDENTIFICATION", bold = true
    )
    drawLayoutWrappedText(
        canvas, r, prefs, IdLayoutElement.BACK_IDENTIFICATION_BODY, item(IdLayoutElement.BACK_IDENTIFICATION_BODY),
        identification, bold = false
    )

    drawLayoutSingleLine(canvas, r, prefs, IdLayoutElement.BACK_ISSUED_LABEL, item(IdLayoutElement.BACK_ISSUED_LABEL), "ISSUED BY:", bold = true)
    drawLayoutSingleLine(
        canvas, r, prefs, IdLayoutElement.BACK_ISSUER_VALUE, item(IdLayoutElement.BACK_ISSUER_VALUE),
        prefs.getString("issuer_name", "BLGU - SIBULAN") ?: "BLGU - SIBULAN", bold = true
    )
    drawLayoutSingleLine(canvas, r, prefs, IdLayoutElement.BACK_APPROVED_LABEL, item(IdLayoutElement.BACK_APPROVED_LABEL), "APPROVED BY:", bold = true)
    drawLayoutImage(
        context, canvas, r, prefs.getString("captain_signature_uri", null),
        item(IdLayoutElement.BACK_CAPTAIN_SIGNATURE), signature = true
    )
    val captainName = prefs.getString("captain_name", "ROWENA A. TABO")?.ifBlank { "ROWENA A. TABO" } ?: "ROWENA A. TABO"
    drawLayoutSingleLine(
        canvas, r, prefs, IdLayoutElement.BACK_CAPTAIN_NAME, item(IdLayoutElement.BACK_CAPTAIN_NAME),
        captainName.uppercase(Locale.ENGLISH), bold = true
    )
    drawLayoutSingleLine(
        canvas, r, prefs, IdLayoutElement.BACK_CAPTAIN_TITLE, item(IdLayoutElement.BACK_CAPTAIN_TITLE),
        prefs.getString("captain_title", "Punong Barangay") ?: "Punong Barangay", bold = false
    )

    drawLayoutSingleLine(
        canvas, r, prefs, IdLayoutElement.BACK_NOTICE_HEADING, item(IdLayoutElement.BACK_NOTICE_HEADING),
        "IMPORTANT NOTICE", bold = true
    )
    val notices = listOf(
        "– This ID is non-transferable.",
        "– This ID remains the property of the Barangay Local Government Unit of Sibulan.",
        "– If lost, report immediately to the Barangay Office.",
        "– Unauthorized use, alteration, or reproduction of this ID is prohibited."
    ).joinToString("\n")
    drawLayoutWrappedText(
        canvas, r, prefs, IdLayoutElement.BACK_NOTICE_BODY, item(IdLayoutElement.BACK_NOTICE_BODY),
        notices, bold = false, preserveParagraphs = true
    )

    val officeEmail = prefs.getString("office_email", "brgysibulan8001@gmail.com") ?: "brgysibulan8001@gmail.com"
    val officePhone = prefs.getString("office_phone", "0970 972 3363") ?: "0970 972 3363"
    drawLayoutSingleLine(
        canvas, r, prefs, IdLayoutElement.BACK_FOOTER_ADDRESS, item(IdLayoutElement.BACK_FOOTER_ADDRESS),
        "Barangay Hall, Sitio Centro, Barangay Sibulan, Sta. Cruz, Davao del Sur", bold = false
    )
    drawLayoutSingleLine(
        canvas, r, prefs, IdLayoutElement.BACK_FOOTER_CONTACT, item(IdLayoutElement.BACK_FOOTER_CONTACT),
        "$officeEmail  |  $officePhone", bold = false
    )

    if (prefs.getBoolean("outline_back_dividers", false)) {
        val sectionLine = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK; strokeWidth = outlineWidth(prefs) }
        val dividerElements = listOf(
            IdLayoutElement.BACK_ADDRESS_VALUE,
            IdLayoutElement.BACK_IDENTIFICATION_BODY,
            IdLayoutElement.BACK_CAPTAIN_TITLE
        )
        dividerElements.forEach { element ->
            val placement = item(element)
            if (placement.visible) {
                val section = layoutRect(r, placement)
                canvas.drawLine(cardX(r, 7f), section.bottom + mm(1f), cardX(r, 78f), section.bottom + mm(1f), sectionLine)
            }
        }
    }
}

private fun layoutRect(card: RectF, placement: IdElementPlacement): RectF = cardRect(
    card,
    placement.xMm,
    placement.yMm,
    placement.widthMm,
    placement.heightMm
)

private fun layoutTextSize(
    prefs: SharedPreferences,
    element: IdLayoutElement,
    placement: IdElementPlacement
): Float = idFontSize(prefs, element.defaultFontPt) * placement.fontScale

private fun layoutPaint(
    prefs: SharedPreferences,
    placement: IdElementPlacement,
    bold: Boolean
): Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
    color = placement.textColor
    textAlign = when (placement.alignment) {
        IdTextAlignment.LEFT -> Paint.Align.LEFT
        IdTextAlignment.CENTER -> Paint.Align.CENTER
        IdTextAlignment.RIGHT -> Paint.Align.RIGHT
    }
    typeface = idTypeface(prefs, bold)
    isUnderlineText = placement.underlineEnabled
}

private fun layoutTextX(rect: RectF, alignment: IdTextAlignment): Float = when (alignment) {
    IdTextAlignment.LEFT -> rect.left
    IdTextAlignment.CENTER -> rect.centerX()
    IdTextAlignment.RIGHT -> rect.right
}

private fun drawLayoutSingleLine(
    canvas: Canvas,
    card: RectF,
    prefs: SharedPreferences,
    element: IdLayoutElement,
    placement: IdElementPlacement,
    value: String,
    bold: Boolean
) {
    if (!placement.visible) return
    val rect = layoutRect(card, placement)
    val paint = layoutPaint(prefs, placement, bold)
    val text = value.trim().ifBlank { "—" }
    val maxSize = layoutTextSize(prefs, element, placement)
    val minSize = maxSize * 0.62f
    paint.textSize = maxSize
    while (
        paint.textSize > minSize &&
        (paint.measureText(text) > rect.width() || paint.fontMetrics.descent - paint.fontMetrics.ascent > rect.height())
    ) {
        paint.textSize -= 0.25f
    }
    val baseline = rect.top - paint.fontMetrics.ascent
    drawLayoutText(canvas, text, layoutTextX(rect, placement.alignment), baseline, paint, placement)
}

private fun drawLayoutTextBlock(
    canvas: Canvas,
    card: RectF,
    prefs: SharedPreferences,
    element: IdLayoutElement,
    placement: IdElementPlacement,
    value: String,
    bold: Boolean,
    maxLines: Int
) {
    if (!placement.visible || maxLines <= 0) return
    val rect = layoutRect(card, placement)
    val paint = layoutPaint(prefs, placement, bold)
    val text = value.trim().ifBlank { "—" }
    val maxSize = layoutTextSize(prefs, element, placement)
    val minSize = maxSize * 0.65f
    var size = maxSize
    var lines = emptyList<String>()
    while (size >= minSize) {
        paint.textSize = size
        lines = tightBreakLines(text, paint, rect.width())
        val lineHeight = paint.textSize * 1.18f
        if (lines.size <= maxLines && lines.size * lineHeight <= rect.height() + 0.5f) break
        size -= 0.25f
    }
    paint.textSize = size.coerceAtLeast(minSize)
    lines = tightBreakLines(text, paint, rect.width())
    val shown = ellipsizeLines(lines, maxLines, rect.width(), paint)
    val lineHeight = paint.textSize * 1.18f
    val firstBaseline = rect.top - paint.fontMetrics.ascent
    shown.forEachIndexed { index, line ->
        drawLayoutText(
            canvas,
            line,
            layoutTextX(rect, placement.alignment),
            firstBaseline + index * lineHeight,
            paint,
            placement
        )
    }
}

private fun drawLayoutWrappedText(
    canvas: Canvas,
    card: RectF,
    prefs: SharedPreferences,
    element: IdLayoutElement,
    placement: IdElementPlacement,
    value: String,
    bold: Boolean,
    preserveParagraphs: Boolean = false
) {
    if (!placement.visible) return
    val rect = layoutRect(card, placement)
    val paint = layoutPaint(prefs, placement, bold)
    val maxSize = layoutTextSize(prefs, element, placement)
    val minSize = maxSize * 0.70f
    var size = maxSize
    var lines = emptyList<String>()
    var maxLines = 1
    while (size >= minSize) {
        paint.textSize = size
        val lineHeight = paint.textSize * 1.22f
        maxLines = (rect.height() / lineHeight).toInt().coerceAtLeast(1)
        lines = if (preserveParagraphs) {
            breakExplicitParagraphs(value, paint, rect.width())
        } else {
            tightBreakLines(value.replace('\n', ' '), paint, rect.width())
        }
        if (lines.size <= maxLines) break
        size -= 0.20f
    }
    paint.textSize = size.coerceAtLeast(minSize)
    val lineHeight = paint.textSize * 1.22f
    maxLines = (rect.height() / lineHeight).toInt().coerceAtLeast(1)
    lines = if (preserveParagraphs) {
        breakExplicitParagraphs(value, paint, rect.width())
    } else {
        tightBreakLines(value.replace('\n', ' '), paint, rect.width())
    }
    val shown = ellipsizeLines(lines, maxLines, rect.width(), paint)
    val firstBaseline = rect.top - paint.fontMetrics.ascent
    shown.forEachIndexed { index, line ->
        drawLayoutText(
            canvas,
            line,
            layoutTextX(rect, placement.alignment),
            firstBaseline + index * lineHeight,
            paint,
            placement
        )
    }
}

private fun breakExplicitParagraphs(value: String, paint: Paint, maxWidth: Float): List<String> =
    value.lines().flatMap { paragraph ->
        if (paragraph.isBlank()) listOf("") else tightBreakLines(paragraph.trim(), paint, maxWidth)
    }

private fun ellipsizeLines(lines: List<String>, maxLines: Int, maxWidth: Float, paint: Paint): List<String> {
    if (lines.size <= maxLines) return lines
    val shown = lines.take(maxLines).toMutableList()
    var last = shown.last().trimEnd()
    while (last.isNotEmpty() && paint.measureText("$last…") > maxWidth) last = last.dropLast(1)
    shown[shown.lastIndex] = "$last…"
    return shown
}

private fun drawLayoutText(
    canvas: Canvas,
    value: String,
    x: Float,
    baseline: Float,
    paint: Paint,
    placement: IdElementPlacement
) {
    if (placement.textOutlineEnabled) {
        val fillColor = paint.color
        val fillStyle = paint.style
        val fillWidth = paint.strokeWidth
        paint.style = Paint.Style.STROKE
        paint.strokeJoin = Paint.Join.ROUND
        paint.strokeWidth = placement.textOutlineWidthPt
        paint.color = if (isLightPdfColor(fillColor)) Color.BLACK else Color.WHITE
        canvas.drawText(value, x, baseline, paint)
        paint.color = fillColor
        paint.style = fillStyle
        paint.strokeWidth = fillWidth
    }
    canvas.drawText(value, x, baseline, paint)
}

private fun isLightPdfColor(color: Int): Boolean {
    val luminance = 0.2126f * Color.red(color) + 0.7152f * Color.green(color) + 0.0722f * Color.blue(color)
    return luminance >= 150f
}

private fun drawLayoutImage(
    context: Context,
    canvas: Canvas,
    card: RectF,
    uri: String?,
    placement: IdElementPlacement,
    signature: Boolean = false
) {
    if (!placement.visible) return
    val bitmap = if (signature) tightLoadSignatureBitmap(context, uri) else tightLoadBitmap(context, uri)
    bitmap?.let {
        tightDrawBitmapFit(canvas, it, layoutRect(card, placement))
        it.recycleSafely()
    }
}

private fun drawOptionalRectOutline(canvas: Canvas, rect: RectF, prefs: SharedPreferences) {
    canvas.drawRect(rect, Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        style = Paint.Style.STROKE
        strokeWidth = outlineWidth(prefs)
    })
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
