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
import androidx.compose.ui.Alignment
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

    LaunchedEffect(employees) {
        if (firstId == null && employees.isNotEmpty()) firstId = employees.first().id
        if (firstId != null && employees.none { it.id == firstId }) firstId = employees.firstOrNull()?.id
        if (secondId != null && employees.none { it.id == secondId }) secondId = null
    }

    val first = employees.firstOrNull { it.id == firstId }
    val second = employees.firstOrNull { it.id == secondId }

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
        Text("Portrait CR80 • exact 53.98 × 85.60 mm • 2 × 2 cutting block • no gap between IDs")

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
                employees.forEach { employee ->
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
                Text("A4 layout", fontWeight = FontWeight.Bold)
                Text("Top-left: Person 1 Front")
                Text("Top-right: Person 1 Back")
                Text(if (second == null) "Bottom row: blank" else "Bottom-left: Person 2 Front • Bottom-right: Person 2 Back")
                Text("All four slots touch edge-to-edge. No internal spacing.", style = MaterialTheme.typography.bodySmall)
            }
        }

        first?.let { employee ->
            Button(
                onClick = {
                    val safeName = employee.fullName.replace(Regex("[^A-Za-z0-9_-]+"), "_").take(40)
                    createPdf.launch("Barangay-ID-${safeName.ifBlank { employee.controlNumber }}.pdf")
                },
                modifier = Modifier.fillMaxWidth().height(54.dp)
            ) { Text("Generate A4 PDF") }
        }

        if (message.isNotBlank()) Text(message)
        Text("Important: sa print dialog piliin ang Actual Size / 100%. Huwag Fit to Page.", style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(12.dp))
    }
}

private const val TIGHT_PT_PER_MM = 72f / 25.4f

private fun writeTightPortraitPdf(
    context: Context,
    uri: Uri,
    first: Employee,
    second: Employee?,
    prefs: SharedPreferences
): Boolean {
    val pdf = PdfDocument()
    return try {
        val pageW = 595f
        val pageH = 842f
        val page = pdf.startPage(PdfDocument.PageInfo.Builder(pageW.toInt(), pageH.toInt(), 1).create())
        val canvas = page.canvas
        canvas.drawColor(Color.WHITE)

        val cardW = 53.98f * TIGHT_PT_PER_MM
        val cardH = 85.60f * TIGHT_PT_PER_MM
        val blockW = cardW * 2f
        val blockH = cardH * 2f
        val left = (pageW - blockW) / 2f
        val top = (pageH - blockH) / 2f

        val topLeft = RectF(left, top, left + cardW, top + cardH)
        val topRight = RectF(left + cardW, top, left + blockW, top + cardH)
        val bottomLeft = RectF(left, top + cardH, left + cardW, top + blockH)
        val bottomRight = RectF(left + cardW, top + cardH, left + blockW, top + blockH)

        tightDrawFront(context, canvas, topLeft, first, prefs)
        tightDrawBack(context, canvas, topRight, first, prefs)

        if (second != null) {
            tightDrawFront(context, canvas, bottomLeft, second, prefs)
            tightDrawBack(context, canvas, bottomRight, second, prefs)
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
    tightLoadBitmap(context, e.photoUri)?.let { canvas.drawBitmap(it, null, photoRect, p) } ?: tightLabeledBox(canvas, photoRect, "PHOTO")

    tightTextFit(canvas, e.fullName, r.centerX(), r.top + 154f, r.width() - 18f, text, 10.5f, 6f)
    text.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
    tightTextFit(canvas, e.position, r.centerX(), r.top + 169f, r.width() - 18f, text, 8f, 5.5f)
    tightTextFit(canvas, "ID NO. ${e.controlNumber}", r.centerX(), r.top + 184f, r.width() - 18f, text, 7.5f, 5.2f)

    val sigRect = RectF(r.left + 16f, r.bottom - 50f, r.left + 82f, r.bottom - 25f)
    tightLoadBitmap(context, e.signatureUri)?.let { canvas.drawBitmap(it, null, sigRect, p) }

    val qrRect = RectF(r.right - 51f, r.bottom - 56f, r.right - 10f, r.bottom - 15f)
    tightLoadBitmap(context, e.qrImageUri)?.let { canvas.drawBitmap(it, null, qrRect, p) } ?: tightLabeledBox(canvas, qrRect, "QR")
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
    tightLoadBitmap(context, prefs.getString("captain_signature_uri", null))?.let { canvas.drawBitmap(it, null, sigRect, p) }

    val captain = prefs.getString("captain_name", "")?.ifBlank { "PUNONG BARANGAY" } ?: "PUNONG BARANGAY"
    tightTextFit(canvas, captain, x, r.bottom - 28f, r.width() - 24f, text, 8f, 5.2f)
    text.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
    tightTextFit(canvas, prefs.getString("captain_title", "PUNONG BARANGAY") ?: "PUNONG BARANGAY", x, r.bottom - 15f, r.width() - 24f, text, 6.8f, 4.8f)
}

private fun tightDrawTemplate(context: Context, canvas: Canvas, r: RectF, uri: String?, front: Boolean) {
    val bitmap = tightLoadBitmap(context, uri)
    if (bitmap != null) {
        tightCenterCrop(canvas, bitmap, r)
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
    canvas.drawBitmap(bitmap, src, target, Paint(Paint.ANTI_ALIAS_FLAG))
}

private fun tightDrawLogoOrB(context: Context, canvas: Canvas, r: RectF, uri: String?) {
    val bitmap = tightLoadBitmap(context, uri)
    if (bitmap != null) {
        canvas.drawBitmap(bitmap, null, r, Paint(Paint.ANTI_ALIAS_FLAG))
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

private fun tightLoadBitmap(context: Context, uri: String?): Bitmap? {
    if (uri.isNullOrBlank()) return null
    return runCatching {
        context.contentResolver.openInputStream(Uri.parse(uri))?.use { BitmapFactory.decodeStream(it) }
    }.getOrNull()
}
