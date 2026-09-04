package ph.gov.barangaysibulan.idmaker

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.RectangleShape
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ph.gov.barangaysibulan.idmaker.data.AppDatabase
import ph.gov.barangaysibulan.idmaker.data.Employee
import java.util.Locale
import kotlin.math.roundToInt

private val LayoutEditorGreen = Color(0xFF00522D)

@Composable
internal fun IdLayoutEditorScreen(onClose: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("id_maker_settings", Context.MODE_PRIVATE) }
    val store = remember(prefs) { IdLayoutStore(prefs) }
    val employeeDao = remember(context) { AppDatabase.get(context).employeeDao() }
    val employees by employeeDao.observeAll().collectAsState(initial = emptyList())
    val previewEmployee = employees.firstOrNull()
    val placements = remember {
        mutableStateMapOf<IdLayoutElement, IdElementPlacement>().apply {
            IdLayoutElement.entries.forEach { put(it, store.load(it)) }
        }
    }

    var side by remember { mutableStateOf(IdLayoutSide.FRONT) }
    var selected by remember { mutableStateOf(IdLayoutElement.FRONT_PHOTO) }
    var selectorOpen by remember { mutableStateOf(false) }
    var showGuides by remember { mutableStateOf(true) }
    var locked by remember { mutableStateOf(store.isLocked()) }
    var dirty by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }
    var showDiscardDialog by remember { mutableStateOf(false) }

    fun selectSide(newSide: IdLayoutSide) {
        side = newSide
        selected = IdLayoutElement.forSide(newSide).first()
        message = ""
    }

    fun update(element: IdLayoutElement, transform: (IdElementPlacement) -> IdElementPlacement) {
        if (locked) return
        val current = placements[element] ?: element.defaultPlacement()
        placements[element] = transform(current).clamped()
        dirty = true
        message = ""
    }

    fun requestClose() {
        if (dirty) showDiscardDialog = true else onClose()
    }

    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = { Text("Discard unsaved placement?") },
            text = { Text("The saved layout used by existing and future IDs will stay unchanged.") },
            confirmButton = {
                TextButton(onClick = onClose) { Text("Discard") }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardDialog = false }) { Text("Continue editing") }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            TextButton(onClick = ::requestClose) { Text("Back") }
            Column(Modifier.weight(1f)) {
                Text("ID Layout Studio", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("One saved placement applies to every ID.", style = MaterialTheme.typography.bodySmall)
            }
        }

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Easy mode", fontWeight = FontWeight.Bold)
                Text(
                    "Tap an item, drag it on the card, then save. Yellow selection boxes and guides are editor-only and never print.",
                    style = MaterialTheme.typography.bodySmall
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = side == IdLayoutSide.FRONT,
                        onClick = { selectSide(IdLayoutSide.FRONT) },
                        label = { Text("Front") }
                    )
                    FilterChip(
                        selected = side == IdLayoutSide.BACK,
                        onClick = { selectSide(IdLayoutSide.BACK) },
                        label = { Text("Back") }
                    )
                    Spacer(Modifier.weight(1f))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Guides", style = MaterialTheme.typography.bodySmall)
                        Switch(checked = showGuides, onCheckedChange = { showGuides = it })
                    }
                }
            }
        }

        IdLayoutCanvas(
            side = side,
            selected = selected,
            placements = placements,
            prefs = prefs,
            previewEmployee = previewEmployee,
            showGuides = showGuides,
            locked = locked,
            onSelect = { selected = it },
            onMove = { element, dxMm, dyMm ->
                update(element) { it.copy(xMm = it.xMm + dxMm, yMm = it.yMm + dyMm) }
            }
        )

        val overlapWarnings = layoutOverlapWarnings(side, placements)
        if (overlapWarnings.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
            ) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text("Check placement", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onErrorContainer)
                    overlapWarnings.forEach { warning ->
                        Text("• $warning", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer)
                    }
                }
            }
        }

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Selected item", fontWeight = FontWeight.Bold)
                Box {
                    OutlinedButton(
                        onClick = { selectorOpen = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(selected.displayName, modifier = Modifier.weight(1f))
                        Text("Change")
                    }
                    DropdownMenu(
                        expanded = selectorOpen,
                        onDismissRequest = { selectorOpen = false }
                    ) {
                        IdLayoutElement.forSide(side).forEach { element ->
                            DropdownMenuItem(
                                text = { Text(element.displayName) },
                                onClick = {
                                    selected = element
                                    selectorOpen = false
                                }
                            )
                        }
                    }
                }

                val selectedPlacement = placements[selected] ?: selected.defaultPlacement()
                SettingsToggle(
                    label = "Show this item",
                    checked = selectedPlacement.visible,
                    enabled = !locked
                ) { value -> update(selected) { it.copy(visible = value) } }

                Text("Position", style = MaterialTheme.typography.titleSmall)
                LayoutSlider(
                    label = "Horizontal",
                    value = selectedPlacement.xMm,
                    range = 0f..(ID_LAYOUT_WIDTH_MM - selectedPlacement.widthMm).coerceAtLeast(0.1f),
                    enabled = !locked
                ) { value -> update(selected) { it.copy(xMm = value) } }
                LayoutSlider(
                    label = "Vertical",
                    value = selectedPlacement.yMm,
                    range = 0f..(ID_LAYOUT_HEIGHT_MM - selectedPlacement.heightMm).coerceAtLeast(0.1f),
                    enabled = !locked
                ) { value -> update(selected) { it.copy(yMm = value) } }

                Text("Size", style = MaterialTheme.typography.titleSmall)
                LayoutSlider(
                    label = "Width",
                    value = selectedPlacement.widthMm,
                    range = 4f..(ID_LAYOUT_WIDTH_MM - selectedPlacement.xMm).coerceAtLeast(4f),
                    enabled = !locked
                ) { value ->
                    update(selected) {
                        if (selected == IdLayoutElement.FRONT_QR) it.copy(widthMm = value, heightMm = value)
                        else it.copy(widthMm = value)
                    }
                }
                LayoutSlider(
                    label = "Height",
                    value = selectedPlacement.heightMm,
                    range = 2f..(ID_LAYOUT_HEIGHT_MM - selectedPlacement.yMm).coerceAtLeast(2f),
                    enabled = !locked
                ) { value ->
                    update(selected) {
                        if (selected == IdLayoutElement.FRONT_QR) it.copy(widthMm = value, heightMm = value)
                        else it.copy(heightMm = value)
                    }
                }

                if (selected.kind == IdLayoutKind.TEXT) {
                    LayoutSlider(
                        label = "Text size",
                        value = selectedPlacement.fontScale,
                        range = 0.65f..1.50f,
                        enabled = !locked,
                        valueText = "${(selectedPlacement.fontScale * 100).roundToInt()}%"
                    ) { value -> update(selected) { it.copy(fontScale = value) } }

                    Text("Alignment", style = MaterialTheme.typography.titleSmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        IdTextAlignment.entries.forEach { alignment ->
                            FilterChip(
                                selected = selectedPlacement.alignment == alignment,
                                onClick = { update(selected) { it.copy(alignment = alignment) } },
                                enabled = !locked,
                                label = { Text(alignment.name.lowercase().replaceFirstChar { it.uppercase() }) }
                            )
                        }
                    }

                    Text("Text color", style = MaterialTheme.typography.titleSmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        LayoutColorChoice("Black", android.graphics.Color.BLACK, selectedPlacement, !locked) {
                            update(selected) { current -> current.copy(textColor = it) }
                        }
                        LayoutColorChoice("White", android.graphics.Color.WHITE, selectedPlacement, !locked) {
                            update(selected) { current -> current.copy(textColor = it) }
                        }
                        LayoutColorChoice("Green", android.graphics.Color.rgb(0, 82, 45), selectedPlacement, !locked) {
                            update(selected) { current -> current.copy(textColor = it) }
                        }
                    }

                    SettingsToggle(
                        label = "Underline",
                        checked = selectedPlacement.underlineEnabled,
                        enabled = !locked
                    ) { value -> update(selected) { it.copy(underlineEnabled = value) } }

                    SettingsToggle(
                        label = "Text outline",
                        checked = selectedPlacement.textOutlineEnabled,
                        enabled = !locked
                    ) { value -> update(selected) { it.copy(textOutlineEnabled = value) } }
                    if (selectedPlacement.textOutlineEnabled) {
                        LayoutSlider(
                            label = "Text outline thickness",
                            value = selectedPlacement.textOutlineWidthPt,
                            range = 0.15f..1.50f,
                            enabled = !locked
                        ) { value -> update(selected) { it.copy(textOutlineWidthPt = value) } }
                        Text(
                            "The outline automatically uses black behind light text and white behind dark text.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SettingsToggle("Lock placement after saving", locked, true) {
                    locked = it
                    dirty = true
                    message = if (it) "Layout locked. Unlock it to edit again." else "Layout unlocked."
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            IdLayoutElement.forSide(side).forEach { placements[it] = it.defaultPlacement() }
                            selected = IdLayoutElement.forSide(side).first()
                            dirty = true
                            message = "${if (side == IdLayoutSide.FRONT) "Front" else "Back"} restored to the professional default."
                        },
                        enabled = !locked,
                        modifier = Modifier.weight(1f)
                    ) { Text("Reset ${if (side == IdLayoutSide.FRONT) "Front" else "Back"}") }
                    OutlinedButton(onClick = ::requestClose, modifier = Modifier.weight(1f)) { Text("Close") }
                }
                Button(
                    onClick = {
                        store.save(placements.toMap(), locked)
                        dirty = false
                        message = "Placement saved and will be used for every employee ID."
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                ) { Text("Save placement • Apply to all IDs") }
                if (message.isNotBlank()) {
                    Text(message, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        Text(
            "The physical card remains exactly 85 × 115 mm. Always test one PDF at Actual Size / 100% before printing all 200 IDs.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.tertiary
        )
        Spacer(Modifier.height(18.dp))
    }
}

@Composable
private fun IdLayoutCanvas(
    side: IdLayoutSide,
    selected: IdLayoutElement,
    placements: SnapshotStateMap<IdLayoutElement, IdElementPlacement>,
    prefs: android.content.SharedPreferences,
    previewEmployee: Employee?,
    showGuides: Boolean,
    locked: Boolean,
    onSelect: (IdLayoutElement) -> Unit,
    onMove: (IdLayoutElement, Float, Float) -> Unit
) {
    val templateUri = if (side == IdLayoutSide.FRONT) {
        prefs.getString("front_template_uri", null)
    } else {
        prefs.getString("back_template_uri", null)
    }
    val backgroundBitmap by rememberLayoutBitmap(templateUri)

    Card(
        modifier = Modifier.fillMaxWidth(),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shape = RectangleShape
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(ID_LAYOUT_WIDTH_MM / ID_LAYOUT_HEIGHT_MM)
                .background(Color.White)
        ) {
            backgroundBitmap?.let { bitmap ->
                Image(
                    bitmap = bitmap,
                    contentDescription = if (side == IdLayoutSide.FRONT) "Front ID background" else "Back ID background",
                    modifier = Modifier.matchParentSize(),
                    contentScale = ContentScale.FillBounds
                )
            } ?: LayoutFallbackBackground(side)

            val cardWidthDp = maxWidth
            val cardHeightDp = maxHeight
            val density = androidx.compose.ui.platform.LocalDensity.current
            val cardWidthPx = with(density) { cardWidthDp.toPx() }.coerceAtLeast(1f)
            val cardHeightPx = with(density) { cardHeightDp.toPx() }.coerceAtLeast(1f)

            if (showGuides) {
                androidx.compose.foundation.Canvas(Modifier.matchParentSize()) {
                    val marginX = size.width * (3f / ID_LAYOUT_WIDTH_MM)
                    val marginY = size.height * (3f / ID_LAYOUT_HEIGHT_MM)
                    drawRect(
                        color = Color(0x99F2C94C),
                        topLeft = Offset(marginX, marginY),
                        size = Size(size.width - marginX * 2f, size.height - marginY * 2f),
                        style = Stroke(width = 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f)))
                    )
                }
            }

            IdLayoutElement.forSide(side).forEach { element ->
                val placement = placements[element] ?: element.defaultPlacement()
                if (placement.visible) {
                    val x = cardWidthDp * (placement.xMm / ID_LAYOUT_WIDTH_MM)
                    val y = cardHeightDp * (placement.yMm / ID_LAYOUT_HEIGHT_MM)
                    val width = cardWidthDp * (placement.widthMm / ID_LAYOUT_WIDTH_MM)
                    val height = cardHeightDp * (placement.heightMm / ID_LAYOUT_HEIGHT_MM)
                    val selectedNow = selected == element
                    val itemModifier = Modifier
                        .offset(x = x, y = y)
                        .size(width = width, height = height)
                        .then(
                            if (selectedNow) Modifier.border(2.dp, Color(0xFFE0A800), RoundedCornerShape(2.dp))
                            else Modifier
                        )
                        .clickable { onSelect(element) }
                        .pointerInput(element, locked, cardWidthPx, cardHeightPx) {
                            detectDragGestures(
                                onDragStart = { onSelect(element) }
                            ) { change, dragAmount ->
                                change.consume()
                                if (!locked) {
                                    onMove(
                                        element,
                                        dragAmount.x / cardWidthPx * ID_LAYOUT_WIDTH_MM,
                                        dragAmount.y / cardHeightPx * ID_LAYOUT_HEIGHT_MM
                                    )
                                }
                            }
                        }

                    LayoutElementPreview(
                        element = element,
                        placement = placement,
                        cardWidthDp = cardWidthDp.value,
                        selected = selectedNow,
                        previewText = layoutPreviewText(element, prefs, previewEmployee),
                        previewImageUri = layoutPreviewImageUri(element, prefs, previewEmployee),
                        globalFontScale = prefs.getFloat("font_scale", 1f).coerceIn(0.85f, 1.20f),
                        fontFamilyKey = prefs.getString("font_family", "sans") ?: "sans",
                        modifier = itemModifier
                    )
                }
            }
        }
    }
}

@Composable
private fun LayoutElementPreview(
    element: IdLayoutElement,
    placement: IdElementPlacement,
    cardWidthDp: Float,
    selected: Boolean,
    previewText: String,
    previewImageUri: String?,
    globalFontScale: Float,
    fontFamilyKey: String,
    modifier: Modifier
) {
    if (element.kind == IdLayoutKind.IMAGE) {
        val previewBitmap by rememberLayoutBitmap(previewImageUri)
        val label = when (element) {
            IdLayoutElement.FRONT_PHOTO -> "PHOTO"
            IdLayoutElement.FRONT_QR -> "QR"
            IdLayoutElement.FRONT_SIGNATURE, IdLayoutElement.BACK_CAPTAIN_SIGNATURE -> "Signature"
            else -> element.displayName
        }
        val background = when (element) {
            IdLayoutElement.FRONT_SIGNATURE, IdLayoutElement.BACK_CAPTAIN_SIGNATURE -> Color.Transparent
            else -> Color.White.copy(alpha = 0.78f)
        }
        Surface(
            modifier = modifier,
            color = background,
            border = if (selected) null else BorderStroke(0.5.dp, Color.Black.copy(alpha = 0.20f)),
            shape = RoundedCornerShape(2.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                if (previewBitmap != null) {
                    Image(
                        bitmap = previewBitmap!!,
                        contentDescription = element.displayName,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = if (element == IdLayoutElement.FRONT_PHOTO) ContentScale.Crop else ContentScale.Fit
                    )
                } else {
                    Text(
                        label,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Black.copy(alpha = 0.72f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
        return
    }

    val fontSizeSp = (
        element.defaultFontPt / (72f / 25.4f) * (cardWidthDp / ID_LAYOUT_WIDTH_MM) * placement.fontScale * globalFontScale
        ).coerceIn(5f, 30f)
    val fontFamily = when (fontFamilyKey) {
        "serif" -> FontFamily.Serif
        "monospace" -> FontFamily.Monospace
        else -> FontFamily.SansSerif
    }
    val textAlign = when (placement.alignment) {
        IdTextAlignment.LEFT -> TextAlign.Left
        IdTextAlignment.CENTER -> TextAlign.Center
        IdTextAlignment.RIGHT -> TextAlign.Right
    }
    val fontWeight = if (
        element.name.contains("LABEL") ||
        element.name.contains("HEADING") ||
        element == IdLayoutElement.FRONT_BARANGAY ||
        element == IdLayoutElement.FRONT_ID_TITLE ||
        element == IdLayoutElement.FRONT_NAME_VALUE ||
        element == IdLayoutElement.FRONT_DESIGNATION_VALUE ||
        element == IdLayoutElement.FRONT_EMPLOYEE_NO_VALUE ||
        element == IdLayoutElement.BACK_ISSUER_VALUE ||
        element == IdLayoutElement.BACK_CAPTAIN_NAME
    ) FontWeight.Bold else FontWeight.Normal

    Box(modifier = modifier, contentAlignment = Alignment.TopStart) {
        if (placement.textOutlineEnabled) {
            val outlineColor = if (isLightColor(placement.textColor)) Color.Black else Color.White
            Text(
                text = previewText,
                modifier = Modifier.fillMaxWidth(),
                color = outlineColor,
                fontSize = fontSizeSp.sp,
                fontFamily = fontFamily,
                fontWeight = fontWeight,
                textAlign = textAlign,
                textDecoration = if (placement.underlineEnabled) TextDecoration.Underline else TextDecoration.None,
                overflow = TextOverflow.Ellipsis,
                style = TextStyle(drawStyle = Stroke(width = placement.textOutlineWidthPt.coerceAtLeast(0.15f) * 1.4f))
            )
        }
        Text(
            text = previewText,
            modifier = Modifier.fillMaxWidth(),
            color = Color(placement.textColor),
            fontSize = fontSizeSp.sp,
            fontFamily = fontFamily,
            fontWeight = fontWeight,
            textAlign = textAlign,
            textDecoration = if (placement.underlineEnabled) TextDecoration.Underline else TextDecoration.None,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private fun layoutPreviewImageUri(
    element: IdLayoutElement,
    prefs: android.content.SharedPreferences,
    employee: Employee?
): String? = when (element) {
    IdLayoutElement.FRONT_LOGO_1 -> prefs.getString("logo1_uri", null)
    IdLayoutElement.FRONT_LOGO_2 -> prefs.getString("logo2_uri", null)
    IdLayoutElement.FRONT_PHOTO -> employee?.photoUri
    IdLayoutElement.FRONT_SIGNATURE -> employee?.signatureUri
    IdLayoutElement.FRONT_QR -> employee?.qrImageUri
    IdLayoutElement.BACK_CAPTAIN_SIGNATURE -> prefs.getString("captain_signature_uri", null)
    else -> null
}

private fun layoutPreviewText(
    element: IdLayoutElement,
    prefs: android.content.SharedPreferences,
    employee: Employee?
): String = when (element) {
    IdLayoutElement.FRONT_BARANGAY -> prefs.getString("barangay", element.sampleText) ?: element.sampleText
    IdLayoutElement.FRONT_ID_TITLE -> prefs.getString("id_heading", element.sampleText) ?: element.sampleText
    IdLayoutElement.BACK_ISSUER_VALUE -> prefs.getString("issuer_name", element.sampleText) ?: element.sampleText
    IdLayoutElement.BACK_CAPTAIN_NAME ->
        prefs.getString("captain_name", element.sampleText)?.ifBlank { element.sampleText } ?: element.sampleText
    IdLayoutElement.BACK_CAPTAIN_TITLE -> prefs.getString("captain_title", element.sampleText) ?: element.sampleText
    IdLayoutElement.BACK_FOOTER_CONTACT -> {
        val email = prefs.getString("office_email", "brgysibulan8001@gmail.com") ?: "brgysibulan8001@gmail.com"
        val phone = prefs.getString("office_phone", "0970 972 3363") ?: "0970 972 3363"
        "$email  |  $phone"
    }
    IdLayoutElement.FRONT_NAME_VALUE -> employee?.fullName?.uppercase(Locale.ENGLISH).orEmpty().ifBlank { element.sampleText }
    IdLayoutElement.FRONT_DESIGNATION_VALUE -> employee?.position?.uppercase(Locale.ENGLISH).orEmpty().ifBlank { element.sampleText }
    IdLayoutElement.FRONT_EMPLOYEE_NO_VALUE -> employee?.controlNumber.orEmpty().ifBlank { element.sampleText }
    IdLayoutElement.BACK_DOB_VALUE -> employee?.birthdate?.let(::formatBirthdateForPdf).orEmpty().ifBlank { element.sampleText }
    IdLayoutElement.BACK_SEX_VALUE -> employee?.sex.orEmpty().ifBlank { element.sampleText }
    IdLayoutElement.BACK_CIVIL_VALUE -> employee?.civilStatus.orEmpty().ifBlank { element.sampleText }
    IdLayoutElement.BACK_ADDRESS_VALUE -> employee?.address.orEmpty().ifBlank { element.sampleText }
    else -> element.sampleText
}

@Composable
private fun LayoutFallbackBackground(side: IdLayoutSide) {
    Box(Modifier.fillMaxSize().background(Color.White)) {
        if (side == IdLayoutSide.FRONT) {
            Box(Modifier.fillMaxWidth().height(70.dp).background(LayoutEditorGreen))
            Box(Modifier.fillMaxWidth().height(34.dp).align(Alignment.BottomCenter).background(LayoutEditorGreen))
        }
    }
}

@Composable
private fun rememberLayoutBitmap(uriString: String?): State<androidx.compose.ui.graphics.ImageBitmap?> {
    val context = LocalContext.current
    return produceState<androidx.compose.ui.graphics.ImageBitmap?>(initialValue = null, key1 = uriString) {
        value = if (uriString.isNullOrBlank()) {
            null
        } else {
            withContext(Dispatchers.IO) {
                runCatching {
                    val uri = Uri.parse(uriString)
                    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        BitmapFactory.decodeStream(input, null, bounds)
                    }
                    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@runCatching null
                    var sample = 1
                    while (maxOf(bounds.outWidth / sample, bounds.outHeight / sample) > 1600) sample *= 2
                    val options = BitmapFactory.Options().apply {
                        inSampleSize = sample
                        inPreferredConfig = android.graphics.Bitmap.Config.ARGB_8888
                    }
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        BitmapFactory.decodeStream(input, null, options)?.asImageBitmap()
                    }
                }.getOrNull()
            }
        }
    }
}

@Composable
private fun LayoutSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    enabled: Boolean,
    valueText: String = "${"%.1f".format(value)} mm",
    onValueChange: (Float) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.bodySmall)
            Text(valueText, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
        }
        Slider(
            value = value.coerceIn(range.start, range.endInclusive),
            onValueChange = onValueChange,
            valueRange = range,
            enabled = enabled
        )
    }
}

@Composable
private fun SettingsToggle(
    label: String,
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled)
    }
}

@Composable
private fun LayoutColorChoice(
    label: String,
    color: Int,
    placement: IdElementPlacement,
    enabled: Boolean,
    onSelect: (Int) -> Unit
) {
    FilterChip(
        selected = placement.textColor == color,
        onClick = { onSelect(color) },
        enabled = enabled,
        label = { Text(label) }
    )
}

private fun isLightColor(color: Int): Boolean {
    val r = android.graphics.Color.red(color)
    val g = android.graphics.Color.green(color)
    val b = android.graphics.Color.blue(color)
    return (0.2126f * r + 0.7152f * g + 0.0722f * b) >= 150f
}

private fun layoutOverlapWarnings(
    side: IdLayoutSide,
    placements: Map<IdLayoutElement, IdElementPlacement>
): List<String> {
    fun visible(element: IdLayoutElement): IdElementPlacement? =
        placements[element]?.takeIf { it.visible }

    fun overlaps(first: IdLayoutElement, second: IdLayoutElement): Boolean {
        val a = visible(first) ?: return false
        val b = visible(second) ?: return false
        val horizontal = a.xMm < b.xMm + b.widthMm && a.xMm + a.widthMm > b.xMm
        val vertical = a.yMm < b.yMm + b.heightMm && a.yMm + a.heightMm > b.yMm
        return horizontal && vertical
    }

    val warnings = mutableListOf<String>()
    if (side == IdLayoutSide.FRONT) {
        val info = listOf(
            IdLayoutElement.FRONT_NAME_LABEL,
            IdLayoutElement.FRONT_NAME_VALUE,
            IdLayoutElement.FRONT_DESIGNATION_LABEL,
            IdLayoutElement.FRONT_DESIGNATION_VALUE,
            IdLayoutElement.FRONT_EMPLOYEE_NO_LABEL,
            IdLayoutElement.FRONT_EMPLOYEE_NO_VALUE
        )
        if (info.any { overlaps(IdLayoutElement.FRONT_PHOTO, it) }) {
            warnings += "Employee photo overlaps the information area."
        }
        if (
            overlaps(IdLayoutElement.FRONT_QR, IdLayoutElement.FRONT_EMPLOYEE_NO_LABEL) ||
            overlaps(IdLayoutElement.FRONT_QR, IdLayoutElement.FRONT_EMPLOYEE_NO_VALUE) ||
            overlaps(IdLayoutElement.FRONT_QR_LABEL, IdLayoutElement.FRONT_EMPLOYEE_NO_VALUE)
        ) {
            warnings += "QR block overlaps Employee No. Move it lower or farther right."
        }
        if (overlaps(IdLayoutElement.FRONT_QR, IdLayoutElement.FRONT_SIGNATURE)) {
            warnings += "QR and holder signature overlap."
        }
        if (overlaps(IdLayoutElement.FRONT_SIGNATURE, IdLayoutElement.FRONT_EMPLOYEE_NO_VALUE)) {
            warnings += "Holder signature overlaps Employee No."
        }
    } else {
        if (
            overlaps(IdLayoutElement.BACK_ADDRESS_VALUE, IdLayoutElement.BACK_IDENTIFICATION_HEADING) ||
            overlaps(IdLayoutElement.BACK_ADDRESS_VALUE, IdLayoutElement.BACK_IDENTIFICATION_BODY)
        ) {
            warnings += "Personal information overlaps the Identification section."
        }
        if (
            overlaps(IdLayoutElement.BACK_IDENTIFICATION_BODY, IdLayoutElement.BACK_ISSUED_LABEL) ||
            overlaps(IdLayoutElement.BACK_IDENTIFICATION_BODY, IdLayoutElement.BACK_APPROVED_LABEL)
        ) {
            warnings += "Identification text overlaps the Issued/Approved section."
        }
        if (
            overlaps(IdLayoutElement.BACK_NOTICE_BODY, IdLayoutElement.BACK_FOOTER_ADDRESS) ||
            overlaps(IdLayoutElement.BACK_NOTICE_BODY, IdLayoutElement.BACK_FOOTER_CONTACT)
        ) {
            warnings += "Important Notice overlaps the footer."
        }
    }
    return warnings
}
