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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ph.gov.barangaysibulan.idmaker.data.AppDatabase
import ph.gov.barangaysibulan.idmaker.data.Employee

@Composable
fun PortraitGenerateScreen() {
    val context = LocalContext.current
    val dao = remember { AppDatabase.get(context).employeeDao() }
    val employees by dao.observeAll().collectAsState(initial = emptyList())
    val prefs = remember { context.getSharedPreferences("id_maker_settings", Context.MODE_PRIVATE) }

    var selectedId by remember { mutableStateOf<Long?>(null) }
    var menuOpen by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }

    LaunchedEffect(employees) {
        if (selectedId == null && employees.isNotEmpty()) selectedId = employees.first().id
        if (selectedId != null && employees.none { it.id == selectedId }) selectedId = employees.firstOrNull()?.id
    }

    val selected = employees.firstOrNull { it.id == selectedId }
    val createPdf = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
        if (uri != null && selected != null) {
            message = if (portraitWritePdf(context, uri, selected, prefs)) "Portrait PDF created." else "Could not create PDF."
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Generate Portrait ID", style = MaterialTheme.typography.headlineSmall)
        Text("CR80 portrait: 53.98 mm wide × 85.60 mm high. The A4 page stays portrait.")

        if (employees.isEmpty()) {
            Card(Modifier.fillMaxWidth()) {
                Text("No employee record found. Add one in Records first.", Modifier.padding(16.dp))
            }
        } else {
            Text("Select Employee", style = MaterialTheme.typography.titleMedium)
            Box {
                OutlinedButton(onClick = { menuOpen = true }, modifier = Modifier.fillMaxWidth()) {
                    Text(selected?.let { "${it.fullName} • ${it.controlNumber}" } ?: "Choose employee")
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    employees.forEach { employee ->
                        DropdownMenuItem(
                            text = { Text("${employee.fullName} • ${employee.controlNumber}") },
                            onClick = {
                                selectedId = employee.id
                                menuOpen = false
                                message = ""
                            }
                        )
                    }
                }
            }

            selected?.let { employee ->
                Text("Portrait Preview", style = MaterialTheme.typography.titleMedium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.Top
                ) {
                    PortraitMiniCard("FRONT", employee.fullName, employee.position)
                    PortraitMiniCard("BACK", "Birthdate", employee.birthdate.ifBlank { "—" })
                }

                Button(
                    onClick = {
                        val safeName = employee.fullName.replace(Regex("[^A-Za-z0-9_-]+"), "_").take(40)
                        createPdf.launch("Barangay-ID-Portrait-${safeName.ifBlank { employee.controlNumber }}.pdf")
                    },
                    modifier = Modifier.fillMaxWidth().height(54.dp)
                ) { Text("Generate Portrait A4 PDF") }
            }
        }

        if (message.isNotBlank()) Text(message)
        Text("Print using Actual Size / 100%. Uploaded front/back designs are center-cropped to the portrait card without stretching.", style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun PortraitMiniCard(title: String, line1: String, line2: String) {
    Card(Modifier.width(145.dp).aspectRatio(53.98f / 85.60f)) {
        Column(
            modifier = Modifier.fillMaxSize().padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(8.dp))
            Text(line1, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
            Text(line2, style = MaterialTheme.typography.bodySmall)
        }
    }
}

private const val PORTRAIT_PT_PER_MM = 72f / 25.4f

private fun portraitWritePdf(context: Context, uri: Uri, employee: Employee, prefs: SharedPreferences): Boolean {
    val pdf = PdfDocument()
    return try {
        val page = pdf.startPage(PdfDocument.PageInfo.Builder(595, 842, 1).create())
        val canvas = page.canvas
        canvas.drawColor(Color.WHITE)

        val cardW = 53.98f * PORTRAIT_PT_PER_MM
        val cardH = 85.60f * PORTRAIT_PT_PER_MM
        val gap = 35f
        val left = (595f - (cardW * 2f + gap)) / 2f
        val top = 60f

        portraitDrawFront(context, canvas, RectF(left, top, left + cardW, top + cardH), employee, prefs)
        portraitDrawBack(context, canvas, RectF(left + cardW + gap, top, left + cardW + gap + cardW, top + cardH), employee, prefs)

        val note = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.DKGRAY
            textSize = 10f
        }
        canvas.drawText("CR80 PORTRAIT 53.98 × 85.60 mm • Actual Size / 100%", left, top + cardH + 28f, note)

        pdf.finishPage(page)
        context.contentResolver.openOutputStream(uri)?.use {
            pdf.writeTo(it)
            true
        } ?: false
    } catch (_: Exception) {
        false
    } finally {
        pdf.close()
    }
}

private fun portraitDrawFront(context: Context, canvas: Canvas, r: RectF, e: Employee, prefs: SharedPreferences) {
    val p = Paint(Paint.ANTI_ALIAS_FLAG)
    portraitDrawTemplateOrFallback(context, canvas, r, prefs.getString("front_template_uri", null), true)

    val logo = 24f
    portraitDrawLogoOrB(context, canvas, RectF(r.left + 8f, r.top + 7f, r.left + 8f + logo, r.top + 7f + logo), prefs.getString("logo1_uri", null))
    portraitDrawLogoOrB(context, canvas, RectF(r.right - 8f - logo, r.top + 7f, r.right - 8f, r.top + 7f + logo), prefs.getString("logo2_uri", null))

    val text = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    portraitTextFit(canvas, prefs.getString("barangay", "BARANGAY SIBULAN") ?: "BARANGAY SIBULAN", r.centerX(), r.top + 18f, r.width() - 72f, text, 7.2f, 4.8f)
    portraitTextFit(canvas, prefs.getString("id_heading", "BARANGAY EMPLOYEE ID") ?: "BARANGAY EMPLOYEE ID", r.centerX(), r.top + 31f, r.width() - 28f, text, 8.5f, 5.2f)

    val photoRect = RectF(r.left + 37f, r.top + 48f, r.right - 37f, r.top + 137f)
    portraitLoadBitmap(context, e.photoUri)?.let { canvas.drawBitmap(it, null, photoRect, p) } ?: portraitLabeledBox(canvas, photoRect, "PHOTO")

    portraitTextFit(canvas, e.fullName, r.centerX(), r.top + 154f, r.width() - 18f, text, 10.5f, 6f)
    text.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
    portraitTextFit(canvas, e.position, r.centerX(), r.top + 169f, r.width() - 18f, text, 8f, 5.5f)
    portraitTextFit(canvas, "ID NO. ${e.controlNumber}", r.centerX(), r.top + 184f, r.width() - 18f, text, 7.5f, 5.2f)

    val sigRect = RectF(r.left + 16f, r.bottom - 50f, r.left + 82f, r.bottom - 25f)
    portraitLoadBitmap(context, e.signatureUri)?.let { canvas.drawBitmap(it, null, sigRect, p) }

    val qrRect = RectF(r.right - 51f, r.bottom - 56f, r.right - 10f, r.bottom - 15f)
    portraitLoadBitmap(context, e.qrImageUri)?.let { canvas.drawBitmap(it, null, qrRect, p) } ?: portraitLabeledBox(canvas, qrRect, "QR")
}

private fun portraitDrawBack(context: Context, canvas: Canvas, r: RectF, e: Employee, prefs: SharedPreferences) {
    val p = Paint(Paint.ANTI_ALIAS_FLAG)
    portraitDrawTemplateOrFallback(context, canvas, r, prefs.getString("back_template_uri", null), false)

    val text = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        textAlign = Paint.Align.LEFT
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
    }
    val x = r.left + 12f
    var y = r.top + 34f
    val lines = listOf(
        "BIRTHDATE: ${e.birthdate.ifBlank { "—" }}",
        "ADDRESS: ${e.address.ifBlank { "—" }}",
        "SEX: ${e.sex.ifBlank { "—" }}",
        "CIVIL STATUS: ${e.civilStatus.ifBlank { "—" }}"
    )
    lines.forEach {
        portraitTextFit(canvas, it, x, y, r.width() - 24f, text, 7.4f, 4.7f)
        y += 22f
    }

    text.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    portraitTextFit(canvas, "APPROVED BY:", x, r.bottom - 78f, r.width() - 24f, text, 7.2f, 5f)

    val sigRect = RectF(x, r.bottom - 72f, x + 82f, r.bottom - 42f)
    portraitLoadBitmap(context, prefs.getString("captain_signature_uri", null))?.let { canvas.drawBitmap(it, null, sigRect, p) }

    val captain = prefs.getString("captain_name", "")?.ifBlank { "PUNONG BARANGAY" } ?: "PUNONG BARANGAY"
    portraitTextFit(canvas, captain, x, r.bottom - 28f, r.width() - 24f, text, 8f, 5.2f)
    text.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
    portraitTextFit(canvas, prefs.getString("captain_title", "PUNONG BARANGAY") ?: "PUNONG BARANGAY", x, r.bottom - 15f, r.width() - 24f, text, 6.8f, 4.8f)
}

private fun portraitDrawTemplateOrFallback(context: Context, canvas: Canvas, r: RectF, uri: String?, front: Boolean) {
    val bitmap = portraitLoadBitmap(context, uri)
    if (bitmap != null) {
        portraitCenterCrop(canvas, bitmap, r)
        return
    }
    val p = Paint(Paint.ANTI_ALIAS_FLAG)
    p.color = Color.WHITE
    canvas.drawRect(r, p)
    p.style = Paint.Style.STROKE
    p.strokeWidth = 1.2f
    p.color = Color.rgb(28, 92, 48)
    canvas.drawRect(r, p)
    p.style = Paint.Style.FILL
    if (front) {
        p.color = Color.rgb(28, 92, 48)
        canvas.drawRect(r.left, r.top, r.right, r.top + 42f, p)
    }
}

private fun portraitCenterCrop(canvas: Canvas, bitmap: Bitmap, target: RectF) {
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

private fun portraitDrawLogoOrB(context: Context, canvas: Canvas, r: RectF, uri: String?) {
    val bitmap = portraitLoadBitmap(context, uri)
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

private fun portraitLabeledBox(canvas: Canvas, r: RectF, label: String) {
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

private fun portraitTextFit(canvas: Canvas, value: String, x: Float, y: Float, maxWidth: Float, paint: Paint, maxSize: Float, minSize: Float) {
    val text = value.ifBlank { "—" }
    paint.textSize = maxSize
    while (paint.textSize > minSize && paint.measureText(text) > maxWidth) paint.textSize -= 0.4f
    canvas.drawText(text, x, y, paint)
}

private fun portraitLoadBitmap(context: Context, uri: String?): Bitmap? {
    if (uri.isNullOrBlank()) return null
    return runCatching {
        context.contentResolver.openInputStream(Uri.parse(uri))?.use { BitmapFactory.decodeStream(it) }
    }.getOrNull()
}
