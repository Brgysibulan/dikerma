package ph.gov.barangaysibulan.idmaker

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
fun GenerateScreen() {
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
            message = if (writeA4TestPdf(context, uri, selected, prefs)) {
                "PDF created successfully."
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
        Text("Test mode is ready. If a logo is missing, the app uses a simple B placeholder so you can try the flow now.")

        if (employees.isEmpty()) {
            Card(Modifier.fillMaxWidth()) {
                Text("No employee record found. Add one in Records first.", Modifier.padding(16.dp))
            }
            return@Column
        }

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
            Text("Preview", style = MaterialTheme.typography.titleMedium)
            TestFrontPreview(employee, prefs)
            TestBackPreview(employee, prefs)

            Button(
                onClick = {
                    val safeName = employee.fullName.replace(Regex("[^A-Za-z0-9_-]+"), "_").take(40)
                    createPdf.launch("Barangay-ID-${safeName.ifBlank { employee.controlNumber }}.pdf")
                },
                modifier = Modifier.fillMaxWidth().height(54.dp)
            ) {
                Text("Generate A4 Test PDF")
            }
        }

        if (message.isNotBlank()) Text(message, style = MaterialTheme.typography.bodyMedium)
        Text(
            "The PDF keeps the ID at CR80 size (85.60 × 53.98 mm). Front and back are placed side-by-side on A4 for testing.",
            style = MaterialTheme.typography.bodySmall
        )
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun TestFrontPreview(employee: Employee, prefs: SharedPreferences) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                PlaceholderLogo()
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(prefs.getString("barangay", "BARANGAY SIBULAN") ?: "BARANGAY SIBULAN", fontWeight = FontWeight.Bold)
                    Text(prefs.getString("id_heading", "BARANGAY EMPLOYEE ID") ?: "BARANGAY EMPLOYEE ID", style = MaterialTheme.typography.titleSmall)
                }
                PlaceholderLogo()
            }
            HorizontalDivider()
            Text(employee.fullName, fontWeight = FontWeight.Bold)
            Text(employee.position)
            Text("ID No.: ${employee.controlNumber}")
            Text(if (employee.qrImageUri.isNullOrBlank()) "QR: not uploaded" else "QR: uploaded", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun TestBackPreview(employee: Employee, prefs: SharedPreferences) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text("BACK", fontWeight = FontWeight.Bold)
            Text("Birthdate: ${employee.birthdate.ifBlank { "—" }}")
            Text("Address: ${employee.address.ifBlank { "—" }}")
            Text("Sex: ${employee.sex.ifBlank { "—" }}")
            Text("Civil Status: ${employee.civilStatus.ifBlank { "—" }}")
            HorizontalDivider()
            Text("Approved by:", style = MaterialTheme.typography.bodySmall)
            Text(prefs.getString("captain_name", "")?.ifBlank { "Punong Barangay" } ?: "Punong Barangay", fontWeight = FontWeight.Bold)
            Text(prefs.getString("captain_title", "PUNONG BARANGAY") ?: "PUNONG BARANGAY", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun PlaceholderLogo() {
    Surface(shape = CircleShape, tonalElevation = 3.dp, modifier = Modifier.size(42.dp)) {
        Box(contentAlignment = Alignment.Center) {
            Text("B", fontWeight = FontWeight.Bold)
        }
    }
}

private const val PT_PER_MM = 72f / 25.4f

private fun writeA4TestPdf(context: Context, uri: Uri, employee: Employee, prefs: SharedPreferences): Boolean {
    val pdf = PdfDocument()
    return try {
        val page = pdf.startPage(PdfDocument.PageInfo.Builder(595, 842, 1).create())
        val canvas = page.canvas
        canvas.drawColor(Color.WHITE)

        val cardW = 85.60f * PT_PER_MM
        val cardH = 53.98f * PT_PER_MM
        val left = 30f
        val top = 55f
        val gap = 45f

        drawFrontCard(context, canvas, RectF(left, top, left + cardW, top + cardH), employee, prefs)
        drawBackCard(context, canvas, RectF(left + cardW + gap, top, left + cardW + gap + cardW, top + cardH), employee, prefs)

        val infoPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.DKGRAY
            textSize = 10f
        }
        canvas.drawText("CR80 85.60 × 53.98 mm • Print at Actual Size / 100%", left, top + cardH + 28f, infoPaint)

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

private fun drawFrontCard(context: Context, canvas: Canvas, r: RectF, e: Employee, prefs: SharedPreferences) {
    val p = Paint(Paint.ANTI_ALIAS_FLAG)
    val template = loadBitmap(context, prefs.getString("front_template_uri", null))
    if (template != null) {
        canvas.drawBitmap(template, null, r, p)
    } else {
        p.color = Color.WHITE
        canvas.drawRect(r, p)
        p.style = Paint.Style.STROKE
        p.strokeWidth = 1.2f
        p.color = Color.rgb(28, 92, 48)
        canvas.drawRect(r, p)
        p.style = Paint.Style.FILL
        p.color = Color.rgb(28, 92, 48)
        canvas.drawRect(r.left, r.top, r.right, r.top + 29f, p)
    }

    val logoSize = 25f
    drawLogoOrB(context, canvas, RectF(r.left + 7f, r.top + 5f, r.left + 7f + logoSize, r.top + 5f + logoSize), prefs.getString("logo1_uri", null))
    drawLogoOrB(context, canvas, RectF(r.right - 7f - logoSize, r.top + 5f, r.right - 7f, r.top + 5f + logoSize), prefs.getString("logo2_uri", null))

    val text = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
    }
    val centerX = r.centerX()
    drawTextFit(canvas, prefs.getString("barangay", "BARANGAY SIBULAN") ?: "BARANGAY SIBULAN", centerX, r.top + 15f, r.width() - 76f, text, 8.2f, 5.5f)
    drawTextFit(canvas, prefs.getString("id_heading", "BARANGAY EMPLOYEE ID") ?: "BARANGAY EMPLOYEE ID", centerX, r.top + 25f, r.width() - 76f, text, 8.8f, 5.8f)

    val photoRect = RectF(r.left + 10f, r.top + 43f, r.left + 64f, r.bottom - 14f)
    val photo = loadBitmap(context, e.photoUri)
    if (photo != null) canvas.drawBitmap(photo, null, photoRect, p) else drawLabeledBox(canvas, photoRect, "PHOTO")

    text.textAlign = Paint.Align.LEFT
    text.color = Color.BLACK
    drawTextFit(canvas, e.fullName, r.left + 72f, r.top + 58f, r.width() - 82f, text, 12f, 7f)
    text.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
    drawTextFit(canvas, e.position, r.left + 72f, r.top + 73f, r.width() - 82f, text, 9f, 6f)
    drawTextFit(canvas, "ID NO. ${e.controlNumber}", r.left + 72f, r.top + 88f, r.width() - 82f, text, 8.5f, 6f)

    val sigRect = RectF(r.left + 72f, r.bottom - 43f, r.left + 142f, r.bottom - 13f)
    loadBitmap(context, e.signatureUri)?.let { canvas.drawBitmap(it, null, sigRect, p) }

    val qrRect = RectF(r.right - 49f, r.bottom - 49f, r.right - 8f, r.bottom - 8f)
    val qr = loadBitmap(context, e.qrImageUri)
    if (qr != null) canvas.drawBitmap(qr, null, qrRect, p) else drawLabeledBox(canvas, qrRect, "QR")
}

private fun drawBackCard(context: Context, canvas: Canvas, r: RectF, e: Employee, prefs: SharedPreferences) {
    val p = Paint(Paint.ANTI_ALIAS_FLAG)
    val template = loadBitmap(context, prefs.getString("back_template_uri", null))
    if (template != null) {
        canvas.drawBitmap(template, null, r, p)
    } else {
        p.color = Color.WHITE
        canvas.drawRect(r, p)
        p.style = Paint.Style.STROKE
        p.strokeWidth = 1.2f
        p.color = Color.rgb(28, 92, 48)
        canvas.drawRect(r, p)
        p.style = Paint.Style.FILL
    }

    val text = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        textSize = 8f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
    }
    val x = r.left + 12f
    var y = r.top + 22f
    listOf(
        "BIRTHDATE: ${e.birthdate.ifBlank { "—" }}",
        "ADDRESS: ${e.address.ifBlank { "—" }}",
        "SEX: ${e.sex.ifBlank { "—" }}",
        "CIVIL STATUS: ${e.civilStatus.ifBlank { "—" }}"
    ).forEach { line ->
        drawTextFit(canvas, line, x, y, r.width() - 24f, text, 8f, 5.5f)
        y += 15f
    }

    text.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    drawTextFit(canvas, "APPROVED BY:", x, r.bottom - 48f, r.width() - 24f, text, 7.5f, 5.5f)
    val captainName = prefs.getString("captain_name", "")?.ifBlank { "PUNONG BARANGAY" } ?: "PUNONG BARANGAY"
    drawTextFit(canvas, captainName, x, r.bottom - 20f, r.width() - 24f, text, 8.5f, 5.5f)
    text.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
    drawTextFit(canvas, prefs.getString("captain_title", "PUNONG BARANGAY") ?: "PUNONG BARANGAY", x, r.bottom - 9f, r.width() - 24f, text, 7f, 5f)

    val captainSig = loadBitmap(context, prefs.getString("captain_signature_uri", null))
    captainSig?.let {
        val sigRect = RectF(x, r.bottom - 45f, x + 80f, r.bottom - 21f)
        canvas.drawBitmap(it, null, sigRect, p)
    }
}

private fun drawLogoOrB(context: Context, canvas: Canvas, r: RectF, uriString: String?) {
    val p = Paint(Paint.ANTI_ALIAS_FLAG)
    val bitmap = loadBitmap(context, uriString)
    if (bitmap != null) {
        canvas.drawBitmap(bitmap, null, r, p)
        return
    }
    p.color = Color.WHITE
    canvas.drawCircle(r.centerX(), r.centerY(), r.width() / 2f, p)
    p.color = Color.rgb(28, 92, 48)
    p.textAlign = Paint.Align.CENTER
    p.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    p.textSize = 13f
    canvas.drawText("B", r.centerX(), r.centerY() + 4.5f, p)
}

private fun drawLabeledBox(canvas: Canvas, r: RectF, label: String) {
    val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.LTGRAY
        style = Paint.Style.STROKE
        strokeWidth = 1f
    }
    canvas.drawRect(r, p)
    p.style = Paint.Style.FILL
    p.color = Color.DKGRAY
    p.textSize = 7f
    p.textAlign = Paint.Align.CENTER
    canvas.drawText(label, r.centerX(), r.centerY() + 2.5f, p)
}

private fun drawTextFit(canvas: Canvas, value: String, x: Float, y: Float, maxWidth: Float, paint: Paint, maxSize: Float, minSize: Float) {
    val text = value.ifBlank { "—" }
    paint.textSize = maxSize
    while (paint.textSize > minSize && paint.measureText(text) > maxWidth) paint.textSize -= 0.5f
    canvas.drawText(text, x, y, paint)
}

private fun loadBitmap(context: Context, uriString: String?): Bitmap? {
    if (uriString.isNullOrBlank()) return null
    return runCatching {
        context.contentResolver.openInputStream(Uri.parse(uriString))?.use { BitmapFactory.decodeStream(it) }
    }.getOrNull()
}
