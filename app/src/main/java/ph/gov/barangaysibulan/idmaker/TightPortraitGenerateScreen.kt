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
        Text("A4 portrait • 85 × 115 mm ID front/back • 1 or 2 people")

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
                Text("Each front/back ID is exactly 85 × 115 mm.", style = MaterialTheme.typography.bodySmall)
                Text("The visible border is the actual cutting guide for the ID.", style = MaterialTheme.typography.bodySmall)
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
private val BRAND_GREEN_DARK = Color.rgb(0, 67, 37)
private val BRAND_YELLOW = Color.rgb(247, 190, 0)
private val BRAND_RED = Color.rgb(196, 28, 38)

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

        listOf(card1, card2, card3, card4).forEach { tightDrawCardCutGuide(canvas, it) }

        tightDrawFront(context, canvas, card1, first, prefs)
        tightDrawBack(context, canvas, card2, first, prefs)

        if (second != null) {
            tightDrawFront(context, canvas, card3, second, prefs)
            tightDrawBack(context, canvas, card4, second, prefs)
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

private fun tightDrawCardCutGuide(canvas: Canvas, card: RectF) {
    val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        style = Paint.Style.STROKE
        strokeWidth = 0.65f
    }
    canvas.drawRect(card, p)
}

private fun tightDrawFront(context: Context, canvas: Canvas, r: RectF, e: Employee, prefs: SharedPreferences) {
    tightDrawTemplate(context, canvas, r, prefs.getString("front_template_uri", null), true)

    val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(242, 0, 82, 45)
        style = Paint.Style.FILL
    }
    canvas.drawRect(cardRect(r, 0f, 0f, ID_W_MM, 25f), headerPaint)

    val accent = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    accent.color = BRAND_YELLOW
    canvas.drawRect(cardRect(r, 0f, 24.3f, ID_W_MM, 0.9f), accent)
    accent.color = BRAND_RED
    canvas.drawRect(cardRect(r, 0f, 25.2f, ID_W_MM, 0.6f), accent)

    val bodyPanel = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(188, 255, 255, 255)
        style = Paint.Style.FILL
    }
    canvas.drawRect(cardRect(r, 3.0f, 28.0f, 79.0f, 72.5f), bodyPanel)

    val logo1Rect = cardRect(r, 4.5f, 3.7f, 17.0f, 17.0f)
    tightDrawLogoOrB(context, canvas, logo1Rect, prefs.getString("logo1_uri", null))

    val logo2Uri = prefs.getString("logo2_uri", null)
    if (!logo2Uri.isNullOrBlank()) {
        tightDrawLogoOrB(context, canvas, cardRect(r, 70.0f, 5.0f, 10.0f, 10.0f), logo2Uri)
    }

    val headerText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.LEFT
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    tightTextFit(
        canvas,
        prefs.getString("barangay", "BARANGAY SIBULAN") ?: "BARANGAY SIBULAN",
        cardX(r, 25.0f),
        cardY(r, 10.5f),
        mm(42.0f),
        headerText,
        13.5f,
        8.0f
    )
    headerText.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
    tightTextFit(
        canvas,
        "STA. CRUZ, DAVAO DEL SUR",
        cardX(r, 25.1f),
        cardY(r, 16.0f),
        mm(40.0f),
        headerText,
        7.5f,
        5.5f
    )
    headerText.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    tightTextFit(
        canvas,
        prefs.getString("id_heading", "BARANGAY EMPLOYEE ID") ?: "BARANGAY EMPLOYEE ID",
        cardX(r, 25.1f),
        cardY(r, 21.3f),
        mm(42.0f),
        headerText,
        8.4f,
        6.0f
    )

    val photoRect = cardRect(r, 6.0f, 32.0f, 31.0f, 40.0f)
    tightLoadBitmap(context, e.photoUri)?.let { bitmap ->
        tightCenterCrop(canvas, bitmap, photoRect)
        bitmap.recycleSafely()
    } ?: tightLabeledBox(canvas, photoRect, "ID PHOTO")
    canvas.drawRect(
        photoRect,
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = BRAND_GREEN
            style = Paint.Style.STROKE
            strokeWidth = 1.1f
        }
    )

    val fieldX = cardX(r, 42.0f)
    val fieldWidth = mm(36.0f)
    val label = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = BRAND_GREEN_DARK
        textAlign = Paint.Align.LEFT
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textSize = 7.0f
    }
    val value = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        textAlign = Paint.Align.LEFT
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    val divider = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = BRAND_GREEN
        strokeWidth = 1.0f
    }

    canvas.drawText("NAME:", fieldX, cardY(r, 37.0f), label)
    tightTextFit(canvas, e.fullName.uppercase(), fieldX, cardY(r, 44.0f), fieldWidth, value, 11.5f, 7.2f)
    canvas.drawLine(fieldX, cardY(r, 47.0f), cardX(r, 78.0f), cardY(r, 47.0f), divider)

    canvas.drawText("DESIGNATION:", fieldX, cardY(r, 55.0f), label)
    tightTextFit(canvas, e.position.uppercase(), fieldX, cardY(r, 62.0f), fieldWidth, value, 10.0f, 6.5f)
    canvas.drawLine(fieldX, cardY(r, 65.0f), cardX(r, 78.0f), cardY(r, 65.0f), divider)

    canvas.drawText("EMPLOYEE NO.:", fieldX, cardY(r, 73.0f), label)
    tightTextFit(canvas, e.controlNumber, fieldX, cardY(r, 80.0f), fieldWidth, value, 11.0f, 7.0f)
    canvas.drawLine(fieldX, cardY(r, 83.0f), cardX(r, 78.0f), cardY(r, 83.0f), divider)

    val signatureRect = cardRect(r, 7.0f, 79.5f, 31.0f, 9.0f)
    tightLoadBitmap(context, e.signatureUri)?.let { bitmap ->
        tightDrawBitmapFit(canvas, bitmap, signatureRect)
        bitmap.recycleSafely()
    }
    canvas.drawLine(cardX(r, 6.0f), cardY(r, 90.0f), cardX(r, 38.5f), cardY(r, 90.0f), divider)
    val sigLabel = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = BRAND_GREEN_DARK
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    tightTextFit(canvas, "SIGNATURE OF HOLDER", cardX(r, 22.25f), cardY(r, 94.0f), mm(32f), sigLabel, 6.8f, 5.0f)

    val qrRect = cardRect(r, 55.0f, 79.0f, 22.0f, 22.0f)
    val qrPanel = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(230, 255, 255, 255)
        style = Paint.Style.FILL
    }
    canvas.drawRect(cardRect(r, 53.5f, 75.5f, 25.0f, 27.0f), qrPanel)
    tightLoadBitmap(context, e.qrImageUri)?.let { bitmap ->
        tightDrawBitmapFit(canvas, bitmap, qrRect)
        bitmap.recycleSafely()
    } ?: tightLabeledBox(canvas, qrRect, "QR")
    val qrLabel = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = BRAND_GREEN_DARK
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    tightTextFit(canvas, "SCAN TO VERIFY", cardX(r, 66.0f), cardY(r, 78.0f), mm(24f), qrLabel, 6.5f, 4.8f)

    val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(244, 0, 82, 45)
        style = Paint.Style.FILL
    }
    canvas.drawRect(cardRect(r, 0f, 102.0f, ID_W_MM, 13.0f), footerPaint)
    accent.color = BRAND_YELLOW
    canvas.drawRect(cardRect(r, 0f, 101.1f, ID_W_MM, 0.7f), accent)
    accent.color = BRAND_RED
    canvas.drawRect(cardRect(r, 0f, 100.5f, ID_W_MM, 0.6f), accent)

    val approver = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    val captainName = prefs.getString("captain_name", "ROWENA A. TABO")?.ifBlank { "ROWENA A. TABO" } ?: "ROWENA A. TABO"
    tightTextFit(canvas, "HON. ${captainName.uppercase()}", r.centerX(), cardY(r, 108.8f), mm(72f), approver, 10.0f, 6.5f)
    approver.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
    tightTextFit(
        canvas,
        prefs.getString("captain_title", "Punong Barangay") ?: "Punong Barangay",
        r.centerX(),
        cardY(r, 113.0f),
        mm(54f),
        approver,
        7.0f,
        5.0f
    )
}

private fun tightDrawBack(context: Context, canvas: Canvas, r: RectF, e: Employee, prefs: SharedPreferences) {
    tightDrawTemplate(context, canvas, r, prefs.getString("back_template_uri", null), false)

    val panel = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(198, 255, 255, 255)
        style = Paint.Style.FILL
    }
    canvas.drawRect(cardRect(r, 4.0f, 4.0f, 77.0f, 101.0f), panel)

    val sectionLine = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = BRAND_GREEN
        strokeWidth = 1.0f
    }
    val label = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = BRAND_GREEN_DARK
        textAlign = Paint.Align.LEFT
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textSize = 7.2f
    }
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

    canvas.drawText("DATE OF BIRTH:", cardX(r, 7.0f), cardY(r, 11.0f), label)
    tightTextFit(canvas, e.birthdate.ifBlank { "—" }, cardX(r, 29.0f), cardY(r, 11.0f), mm(20f), normal, 7.8f, 5.5f)

    canvas.drawText("SEX:", cardX(r, 7.0f), cardY(r, 18.0f), label)
    tightTextFit(canvas, e.sex.ifBlank { "—" }, cardX(r, 17.0f), cardY(r, 18.0f), mm(18f), normal, 7.8f, 5.5f)
    canvas.drawText("CIVIL STATUS:", cardX(r, 43.0f), cardY(r, 18.0f), label)
    tightTextFit(canvas, e.civilStatus.ifBlank { "—" }, cardX(r, 65.0f), cardY(r, 18.0f), mm(14f), normal, 7.2f, 5.0f)

    canvas.drawText("ADDRESS:", cardX(r, 7.0f), cardY(r, 25.0f), label)
    tightDrawWrappedText(
        canvas,
        e.address.ifBlank { "—" },
        cardX(r, 7.0f),
        cardY(r, 30.0f),
        mm(70.0f),
        normal,
        7.2f,
        mm(3.3f),
        2
    )

    canvas.drawLine(cardX(r, 7.0f), cardY(r, 38.0f), cardX(r, 78.0f), cardY(r, 38.0f), sectionLine)

    bold.textAlign = Paint.Align.CENTER
    tightTextFit(canvas, "IDENTIFICATION", r.centerX(), cardY(r, 44.0f), mm(66f), bold, 10.0f, 7.0f)
    normal.textAlign = Paint.Align.LEFT
    val identification = "This identification card is issued to the bearer whose photograph appears herein and who is a bona fide employee of the Barangay Local Government Unit of Sibulan."
    tightDrawWrappedText(
        canvas,
        identification,
        cardX(r, 9.0f),
        cardY(r, 50.0f),
        mm(67.0f),
        normal,
        7.2f,
        mm(3.45f),
        4
    )

    canvas.drawLine(cardX(r, 7.0f), cardY(r, 64.5f), cardX(r, 78.0f), cardY(r, 64.5f), sectionLine)

    bold.textAlign = Paint.Align.LEFT
    tightTextFit(canvas, "ISSUED BY:", cardX(r, 8.0f), cardY(r, 70.5f), mm(30f), bold, 8.0f, 6.0f)
    tightTextFit(
        canvas,
        prefs.getString("issuer_name", "BLGU - SIBULAN") ?: "BLGU - SIBULAN",
        cardX(r, 8.0f),
        cardY(r, 76.0f),
        mm(30f),
        bold,
        9.0f,
        6.0f
    )

    tightTextFit(canvas, "APPROVED BY:", cardX(r, 45.0f), cardY(r, 70.5f), mm(31f), bold, 8.0f, 6.0f)
    val captainSigRect = cardRect(r, 47.0f, 71.5f, 26.0f, 7.0f)
    tightLoadBitmap(context, prefs.getString("captain_signature_uri", null))?.let { bitmap ->
        tightDrawBitmapFit(canvas, bitmap, captainSigRect)
        bitmap.recycleSafely()
    }
    val captainName = prefs.getString("captain_name", "ROWENA A. TABO")?.ifBlank { "ROWENA A. TABO" } ?: "ROWENA A. TABO"
    tightTextFit(canvas, captainName.uppercase(), cardX(r, 45.0f), cardY(r, 81.0f), mm(33f), bold, 7.8f, 5.5f)
    normal.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
    tightTextFit(
        canvas,
        prefs.getString("captain_title", "Punong Barangay") ?: "Punong Barangay",
        cardX(r, 45.0f),
        cardY(r, 85.0f),
        mm(31f),
        normal,
        6.5f,
        4.8f
    )

    canvas.drawLine(cardX(r, 7.0f), cardY(r, 88.0f), cardX(r, 78.0f), cardY(r, 88.0f), sectionLine)
    bold.textAlign = Paint.Align.CENTER
    tightTextFit(canvas, "IMPORTANT NOTICE", r.centerX(), cardY(r, 93.0f), mm(65f), bold, 8.5f, 6.0f)
    normal.textAlign = Paint.Align.LEFT
    val notices = listOf(
        "• This ID is non-transferable.",
        "• This ID remains the property of BLGU-Sibulan.",
        "• If lost, report immediately to the Barangay Office.",
        "• Unauthorized use or reproduction of this ID is prohibited."
    )
    val noticeY = listOf(97.5f, 101.0f, 104.5f, 108.0f)
    notices.forEachIndexed { index, line ->
        tightTextFit(canvas, line, cardX(r, 9.0f), cardY(r, noticeY[index]), mm(67f), normal, 6.2f, 4.8f)
    }

    val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(244, 0, 82, 45)
        style = Paint.Style.FILL
    }
    canvas.drawRect(cardRect(r, 0f, 109.5f, ID_W_MM, 5.5f), footerPaint)
    val footerText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
    }
    val officeEmail = prefs.getString("office_email", "brgysibulan8001@gmail.com") ?: "brgysibulan8001@gmail.com"
    val officePhone = prefs.getString("office_phone", "0970 972 3363") ?: "0970 972 3363"
    tightTextFit(canvas, "$officeEmail  •  $officePhone", r.centerX(), cardY(r, 113.3f), mm(75f), footerText, 5.4f, 4.2f)
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
    p.color = BRAND_GREEN
    canvas.drawRect(r, p)
    p.style = Paint.Style.FILL
    if (front) {
        p.color = BRAND_GREEN
        canvas.drawRect(r.left, r.top, r.right, r.top + mm(25f), p)
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

private fun tightDrawLogoOrB(context: Context, canvas: Canvas, r: RectF, uri: String?) {
    val bitmap = tightLoadBitmap(context, uri)
    if (bitmap != null) {
        tightDrawBitmapFit(canvas, bitmap, r)
        bitmap.recycleSafely()
        return
    }

    val p = Paint(Paint.ANTI_ALIAS_FLAG)
    p.color = Color.WHITE
    canvas.drawCircle(r.centerX(), r.centerY(), r.width() / 2f, p)
    p.color = BRAND_GREEN
    p.textAlign = Paint.Align.CENTER
    p.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    p.textSize = 18f
    canvas.drawText("B", r.centerX(), r.centerY() + 6f, p)
}

private fun tightLabeledBox(canvas: Canvas, r: RectF, label: String) {
    val p = Paint(Paint.ANTI_ALIAS_FLAG)
    p.color = Color.LTGRAY
    p.style = Paint.Style.STROKE
    p.strokeWidth = 1f
    canvas.drawRect(r, p)
    p.style = Paint.Style.FILL
    p.color = Color.DKGRAY
    p.textSize = 9f
    p.textAlign = Paint.Align.CENTER
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
) {
    if (maxLines <= 0) return
    paint.textSize = textSize
    val words = value.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
    if (words.isEmpty()) return

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
    val overflowed = wordIndex < words.size
    if (overflowed && lines.isNotEmpty()) {
        var last = lines.last().trimEnd()
        while (last.isNotEmpty() && paint.measureText("$last…") > maxWidth) last = last.dropLast(1).trimEnd()
        lines[lines.lastIndex] = if (last.isBlank()) "…" else "$last…"
    }

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
