package ph.gov.barangaysibulan.idmaker

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Color as AndroidColor
import android.net.Uri
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ph.gov.barangaysibulan.idmaker.data.AppDatabase
import ph.gov.barangaysibulan.idmaker.data.Employee
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

private val EditorGreen = Color(0xFF00522D)
private val GuideYellow = Color(0xFFE0A800)
private val GuideBlue = Color(0xFF2368C4)

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
    var showSafeMargin by remember { mutableStateOf(true) }
    var showCenterGuides by remember { mutableStateOf(true) }
    var showGrid by remember { mutableStateOf(true) }
    var snapToGrid by remember { mutableStateOf(true) }
    var snapStepMm by remember { mutableFloatStateOf(0.5f) }
    var nudgeMm by remember { mutableFloatStateOf(0.5f) }
    var locked by remember { mutableStateOf(store.isLocked()) }
    var dirty by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }
    var showDiscardDialog by remember { mutableStateOf(false) }

    fun selectSide(newSide: IdLayoutSide) {
        side = newSide
        selected = IdLayoutElement.forSide(newSide).first()
        message = ""
    }

    fun snap(value: Float, step: Float): Float = if (!snapToGrid || step <= 0f) value else (value / step).roundToInt() * step

    fun smartSnapX(value: Float, width: Float): Float {
        if (!snapToGrid) return value
        val centers = listOf(
            ID_LAYOUT_SAFE_MARGIN_MM,
            (ID_LAYOUT_WIDTH_MM - width) / 2f,
            ID_LAYOUT_WIDTH_MM - ID_LAYOUT_SAFE_MARGIN_MM - width
        )
        return centers.minByOrNull { abs(it - value) }?.takeIf { abs(it - value) <= 0.8f } ?: value
    }

    fun smartSnapY(value: Float, height: Float): Float {
        if (!snapToGrid) return value
        val centers = listOf(
            ID_LAYOUT_SAFE_MARGIN_MM,
            (ID_LAYOUT_HEIGHT_MM - height) / 2f,
            ID_LAYOUT_HEIGHT_MM - ID_LAYOUT_SAFE_MARGIN_MM - height
        )
        return centers.minByOrNull { abs(it - value) }?.takeIf { abs(it - value) <= 0.8f } ?: value
    }

    fun update(element: IdLayoutElement, transform: (IdElementPlacement) -> IdElementPlacement) {
        if (locked) return
        val current = placements[element] ?: element.defaultPlacement()
        placements[element] = transform(current).clamped()
        dirty = true
        message = ""
    }

    fun move(element: IdLayoutElement, dxMm: Float, dyMm: Float) {
        update(element) { current ->
            val rawX = current.xMm + dxMm
            val rawY = current.yMm + dyMm
            current.copy(
                xMm = smartSnapX(snap(rawX, snapStepMm), current.widthMm),
                yMm = smartSnapY(snap(rawY, snapStepMm), current.heightMm)
            )
        }
    }

    fun requestClose() {
        if (dirty) showDiscardDialog = true else onClose()
    }

    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = { Text("Discard unsaved changes?") },
            text = { Text("The last saved layout will remain unchanged.") },
            confirmButton = { TextButton(onClick = onClose) { Text("Discard") } },
            dismissButton = { TextButton(onClick = { showDiscardDialog = false }) { Text("Continue editing") } }
        )
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = ::requestClose) { Text("Back") }
            Column(Modifier.weight(1f)) {
                Text("ID Layout Studio", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("Arrange once, save once, apply to every employee ID.", style = MaterialTheme.typography.bodySmall)
            }
        }

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = side == IdLayoutSide.FRONT, onClick = { selectSide(IdLayoutSide.FRONT) }, label = { Text("Front") })
                    FilterChip(selected = side == IdLayoutSide.BACK, onClick = { selectSide(IdLayoutSide.BACK) }, label = { Text("Back") })
                }
                Text("Editor guides never print in the PDF.", style = MaterialTheme.typography.bodySmall)
                SettingsToggle("Safe margin guide", showSafeMargin, true) { showSafeMargin = it }
                SettingsToggle("Center guides", showCenterGuides, true) { showCenterGuides = it }
                SettingsToggle("Grid", showGrid, true) { showGrid = it }
                SettingsToggle("Snap to grid / smart guides", snapToGrid, true) { snapToGrid = it }
                if (snapToGrid) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(0.5f, 1f).forEach { step ->
                            FilterChip(selected = snapStepMm == step, onClick = { snapStepMm = step }, label = { Text("${step} mm snap") })
                        }
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
            showSafeMargin = showSafeMargin,
            showCenterGuides = showCenterGuides,
            showGrid = showGrid,
            locked = locked,
            onSelect = { selected = it },
            onMove = ::move
        )

        val warnings = layoutOverlapWarnings(side, placements)
        if (warnings.isNotEmpty()) {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text("Check placement", fontWeight = FontWeight.Bold)
                    warnings.forEach { Text("• $it", style = MaterialTheme.typography.bodySmall) }
                }
            }
        }

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Selected item", fontWeight = FontWeight.Bold)
                Box {
                    OutlinedButton(onClick = { selectorOpen = true }, modifier = Modifier.fillMaxWidth()) {
                        Text(selected.displayName, modifier = Modifier.weight(1f))
                        Text("Change")
                    }
                    DropdownMenu(expanded = selectorOpen, onDismissRequest = { selectorOpen = false }) {
                        IdLayoutElement.forSide(side).forEach { element ->
                            DropdownMenuItem(text = { Text(element.displayName) }, onClick = { selected = element; selectorOpen = false })
                        }
                    }
                }

                val p = placements[selected] ?: selected.defaultPlacement()
                SettingsToggle("Show this item", p.visible, !locked) { value -> update(selected) { it.copy(visible = value) } }

                Text("POSITION & ALIGNMENT", style = MaterialTheme.typography.titleSmall)
                Text("X ${"%.1f".format(p.xMm)} mm • Y ${"%.1f".format(p.yMm)} mm • W ${"%.1f".format(p.widthMm)} mm • H ${"%.1f".format(p.heightMm)} mm", style = MaterialTheme.typography.bodySmall)
                LayoutSlider("Horizontal X", p.xMm, 0f..(ID_LAYOUT_WIDTH_MM - p.widthMm).coerceAtLeast(0.1f), !locked) { value -> update(selected) { it.copy(xMm = snap(value, snapStepMm)) } }
                LayoutSlider("Vertical Y", p.yMm, 0f..(ID_LAYOUT_HEIGHT_MM - p.heightMm).coerceAtLeast(0.1f), !locked) { value -> update(selected) { it.copy(yMm = snap(value, snapStepMm)) } }

                Text("Quick align", style = MaterialTheme.typography.bodySmall)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    SmallAction("Left", !locked) { update(selected) { it.copy(xMm = ID_LAYOUT_SAFE_MARGIN_MM) } }
                    SmallAction("Center", !locked) { update(selected) { it.copy(xMm = (ID_LAYOUT_WIDTH_MM - it.widthMm) / 2f) } }
                    SmallAction("Right", !locked) { update(selected) { it.copy(xMm = ID_LAYOUT_WIDTH_MM - ID_LAYOUT_SAFE_MARGIN_MM - it.widthMm) } }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    SmallAction("Top", !locked) { update(selected) { it.copy(yMm = ID_LAYOUT_SAFE_MARGIN_MM) } }
                    SmallAction("Middle", !locked) { update(selected) { it.copy(yMm = (ID_LAYOUT_HEIGHT_MM - it.heightMm) / 2f) } }
                    SmallAction("Bottom", !locked) { update(selected) { it.copy(yMm = ID_LAYOUT_HEIGHT_MM - ID_LAYOUT_SAFE_MARGIN_MM - it.heightMm) } }
                }

                Text("Precision nudge", style = MaterialTheme.typography.bodySmall)
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    listOf(0.25f, 0.5f, 1f).forEach { step ->
                        FilterChip(selected = nudgeMm == step, onClick = { nudgeMm = step }, label = { Text("$step mm") })
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    SmallAction("←", !locked) { move(selected, -nudgeMm, 0f) }
                    SmallAction("↑", !locked) { move(selected, 0f, -nudgeMm) }
                    SmallAction("↓", !locked) { move(selected, 0f, nudgeMm) }
                    SmallAction("→", !locked) { move(selected, nudgeMm, 0f) }
                }

                Text("SIZE", style = MaterialTheme.typography.titleSmall)
                LayoutSlider("Width", p.widthMm, 4f..(ID_LAYOUT_WIDTH_MM - p.xMm).coerceAtLeast(4f), !locked) { value ->
                    update(selected) { if (selected == IdLayoutElement.FRONT_QR) it.copy(widthMm = value, heightMm = value) else it.copy(widthMm = value) }
                }
                LayoutSlider("Height", p.heightMm, 2f..(ID_LAYOUT_HEIGHT_MM - p.yMm).coerceAtLeast(2f), !locked) { value ->
                    update(selected) { if (selected == IdLayoutElement.FRONT_QR) it.copy(widthMm = value, heightMm = value) else it.copy(heightMm = value) }
                }

                if (selected.kind == IdLayoutKind.TEXT) {
                    Text("TEXT", style = MaterialTheme.typography.titleSmall)
                    LayoutSlider("Text size", p.fontScale, 0.55f..1.80f, !locked, "${(p.fontScale * 100).roundToInt()}%") { value -> update(selected) { it.copy(fontScale = value) } }
                    Text("Font family", style = MaterialTheme.typography.bodySmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        FontChip("Sans", "sans", p, !locked) { update(selected) { current -> current.copy(fontFamilyKey = it) } }
                        FontChip("Serif", "serif", p, !locked) { update(selected) { current -> current.copy(fontFamilyKey = it) } }
                        FontChip("Mono", "monospace", p, !locked) { update(selected) { current -> current.copy(fontFamilyKey = it) } }
                    }
                    SettingsToggle("Bold", p.bold, !locked) { value -> update(selected) { it.copy(bold = value) } }
                    Text("Text alignment", style = MaterialTheme.typography.bodySmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        IdTextAlignment.entries.forEach { a ->
                            FilterChip(selected = p.alignment == a, onClick = { update(selected) { it.copy(alignment = a) } }, enabled = !locked, label = { Text(a.name.lowercase().replaceFirstChar { it.uppercase() }) })
                        }
                    }

                    Text("COLOR", style = MaterialTheme.typography.titleSmall)
                    ColorPalette("Text color", p.textColor, !locked) { color -> update(selected) { it.copy(textColor = color) } }
                    CustomHexColor(p.textColor, !locked) { color -> update(selected) { it.copy(textColor = color) } }

                    Text("UNDERLINE", style = MaterialTheme.typography.titleSmall)
                    SettingsToggle("Underline", p.underlineEnabled, !locked) { value -> update(selected) { it.copy(underlineEnabled = value) } }
                    if (p.underlineEnabled) {
                        ColorPalette("Underline color", p.underlineColor, !locked) { color -> update(selected) { it.copy(underlineColor = color) } }
                        LayoutSlider("Underline thickness", p.underlineThicknessPt, 0.15f..2f, !locked, "${"%.2f".format(p.underlineThicknessPt)} pt") { value -> update(selected) { it.copy(underlineThicknessPt = value) } }
                        LayoutSlider("Underline offset", p.underlineOffsetMm, 0f..3f, !locked) { value -> update(selected) { it.copy(underlineOffsetMm = value) } }
                        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                            IdUnderlineWidthMode.entries.forEach { mode ->
                                FilterChip(selected = p.underlineWidthMode == mode, onClick = { update(selected) { it.copy(underlineWidthMode = mode) } }, enabled = !locked, label = { Text(if (mode == IdUnderlineWidthMode.TEXT) "Text width" else "Full width") })
                            }
                        }
                    }

                    Text("OUTLINE", style = MaterialTheme.typography.titleSmall)
                    SettingsToggle("Text outline", p.textOutlineEnabled, !locked) { value -> update(selected) { it.copy(textOutlineEnabled = value) } }
                    if (p.textOutlineEnabled) {
                        ColorPalette("Outline color", p.textOutlineColor, !locked) { color -> update(selected) { it.copy(textOutlineColor = color) } }
                        LayoutSlider("Outline thickness", p.textOutlineWidthPt, 0.15f..2f, !locked, "${"%.2f".format(p.textOutlineWidthPt)} pt") { value -> update(selected) { it.copy(textOutlineWidthPt = value) } }
                    }

                    Text("SHADOW", style = MaterialTheme.typography.titleSmall)
                    SettingsToggle("Text shadow", p.shadowEnabled, !locked) { value -> update(selected) { it.copy(shadowEnabled = value) } }
                    if (p.shadowEnabled) {
                        ColorPalette("Shadow color", p.shadowColor, !locked) { color -> update(selected) { it.copy(shadowColor = color) } }
                        LayoutSlider("Shadow opacity", p.shadowOpacity, 0f..1f, !locked, "${(p.shadowOpacity * 100).roundToInt()}%") { value -> update(selected) { it.copy(shadowOpacity = value) } }
                        LayoutSlider("Shadow X", p.shadowDxMm, -3f..3f, !locked) { value -> update(selected) { it.copy(shadowDxMm = value) } }
                        LayoutSlider("Shadow Y", p.shadowDyMm, -3f..3f, !locked) { value -> update(selected) { it.copy(shadowDyMm = value) } }
                        LayoutSlider("Shadow size", p.shadowRadiusPt, 0f..4f, !locked, "${"%.1f".format(p.shadowRadiusPt)} pt") { value -> update(selected) { it.copy(shadowRadiusPt = value) } }
                    }
                }

                Text("RESET SELECTED", style = MaterialTheme.typography.titleSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    SmallAction("Position", !locked) {
                        val d = selected.defaultPlacement(); update(selected) { it.copy(xMm = d.xMm, yMm = d.yMm) }
                    }
                    SmallAction("Size", !locked) {
                        val d = selected.defaultPlacement(); update(selected) { it.copy(widthMm = d.widthMm, heightMm = d.heightMm) }
                    }
                    SmallAction("Style", !locked) {
                        val d = selected.defaultPlacement(); update(selected) {
                            it.copy(
                                fontScale = d.fontScale, fontFamilyKey = d.fontFamilyKey, bold = d.bold,
                                alignment = d.alignment, textColor = d.textColor,
                                underlineEnabled = d.underlineEnabled, underlineColor = d.underlineColor,
                                underlineThicknessPt = d.underlineThicknessPt, underlineOffsetMm = d.underlineOffsetMm,
                                underlineWidthMode = d.underlineWidthMode,
                                textOutlineEnabled = d.textOutlineEnabled, textOutlineColor = d.textOutlineColor,
                                textOutlineWidthPt = d.textOutlineWidthPt,
                                shadowEnabled = d.shadowEnabled, shadowColor = d.shadowColor, shadowOpacity = d.shadowOpacity,
                                shadowDxMm = d.shadowDxMm, shadowDyMm = d.shadowDyMm, shadowRadiusPt = d.shadowRadiusPt
                            )
                        }
                    }
                }
            }
        }

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SettingsToggle("Lock placement after saving", locked, true) { locked = it; dirty = true }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = {
                        IdLayoutElement.forSide(side).forEach { placements[it] = it.defaultPlacement() }
                        selected = IdLayoutElement.forSide(side).first(); dirty = true
                    }, enabled = !locked, modifier = Modifier.weight(1f)) { Text("Reset ${if (side == IdLayoutSide.FRONT) "Front" else "Back"}") }
                    OutlinedButton(onClick = ::requestClose, modifier = Modifier.weight(1f)) { Text("Close") }
                }
                Button(onClick = {
                    store.save(placements.toMap(), locked)
                    dirty = false
                    message = "Layout and text styles saved for all IDs."
                }, modifier = Modifier.fillMaxWidth().height(52.dp)) { Text("Save placement • Apply to all IDs") }
                if (message.isNotBlank()) Text(message, color = MaterialTheme.colorScheme.primary)
            }
        }

        Text("Physical card stays exactly 85 × 115 mm. Print one test at Actual Size / 100% before mass production.", style = MaterialTheme.typography.bodySmall)
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
    showSafeMargin: Boolean,
    showCenterGuides: Boolean,
    showGrid: Boolean,
    locked: Boolean,
    onSelect: (IdLayoutElement) -> Unit,
    onMove: (IdLayoutElement, Float, Float) -> Unit
) {
    val templateUri = if (side == IdLayoutSide.FRONT) prefs.getString("front_template_uri", null) else prefs.getString("back_template_uri", null)
    val backgroundBitmap by rememberLayoutBitmap(templateUri)

    Card(modifier = Modifier.fillMaxWidth()) {
        BoxWithConstraints(Modifier.fillMaxWidth().aspectRatio(ID_LAYOUT_WIDTH_MM / ID_LAYOUT_HEIGHT_MM).background(Color.White)) {
            backgroundBitmap?.let {
                Image(bitmap = it, contentDescription = null, modifier = Modifier.matchParentSize(), contentScale = ContentScale.FillBounds)
            } ?: LayoutFallbackBackground(side)

            val cardWidthDp = maxWidth
            val cardHeightDp = maxHeight
            val density = androidx.compose.ui.platform.LocalDensity.current
            val cardWidthPx = with(density) { cardWidthDp.toPx() }.coerceAtLeast(1f)
            val cardHeightPx = with(density) { cardHeightDp.toPx() }.coerceAtLeast(1f)

            Canvas(Modifier.matchParentSize()) {
                if (showGrid) {
                    val gridColor = Color.Black.copy(alpha = 0.10f)
                    var xMm = 5f
                    while (xMm < ID_LAYOUT_WIDTH_MM) {
                        val x = size.width * xMm / ID_LAYOUT_WIDTH_MM
                        drawLine(gridColor, Offset(x, 0f), Offset(x, size.height), 0.5.dp.toPx())
                        xMm += 5f
                    }
                    var yMm = 5f
                    while (yMm < ID_LAYOUT_HEIGHT_MM) {
                        val y = size.height * yMm / ID_LAYOUT_HEIGHT_MM
                        drawLine(gridColor, Offset(0f, y), Offset(size.width, y), 0.5.dp.toPx())
                        yMm += 5f
                    }
                }
                if (showSafeMargin) {
                    val mx = size.width * ID_LAYOUT_SAFE_MARGIN_MM / ID_LAYOUT_WIDTH_MM
                    val my = size.height * ID_LAYOUT_SAFE_MARGIN_MM / ID_LAYOUT_HEIGHT_MM
                    drawRect(GuideYellow.copy(alpha = 0.8f), Offset(mx, my), Size(size.width - 2 * mx, size.height - 2 * my), style = Stroke(1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(7f, 7f))))
                }
                if (showCenterGuides) {
                    drawLine(GuideBlue.copy(alpha = 0.65f), Offset(size.width / 2f, 0f), Offset(size.width / 2f, size.height), 1.dp.toPx())
                    drawLine(GuideBlue.copy(alpha = 0.65f), Offset(0f, size.height / 2f), Offset(size.width, size.height / 2f), 1.dp.toPx())
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
                    val modifier = Modifier.offset(x, y).size(width, height)
                        .then(if (selectedNow) Modifier.border(2.dp, GuideYellow, RoundedCornerShape(2.dp)) else Modifier)
                        .clickable { onSelect(element) }
                        .pointerInput(element, locked, cardWidthPx, cardHeightPx) {
                            detectDragGestures(onDragStart = { onSelect(element) }) { change, drag ->
                                change.consume()
                                if (!locked) onMove(element, drag.x / cardWidthPx * ID_LAYOUT_WIDTH_MM, drag.y / cardHeightPx * ID_LAYOUT_HEIGHT_MM)
                            }
                        }
                    LayoutElementPreview(
                        element, placement, cardWidthDp.value,
                        previewText = layoutPreviewText(element, prefs, previewEmployee),
                        previewImageUri = layoutPreviewImageUri(element, prefs, previewEmployee),
                        modifier = modifier
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
    previewText: String,
    previewImageUri: String?,
    modifier: Modifier
) {
    if (element.kind == IdLayoutKind.IMAGE) {
        val previewBitmap by rememberLayoutBitmap(previewImageUri)
        Box(modifier.background(if (element == IdLayoutElement.FRONT_SIGNATURE || element == IdLayoutElement.BACK_CAPTAIN_SIGNATURE) Color.Transparent else Color.White.copy(alpha = 0.25f)), contentAlignment = Alignment.Center) {
            if (previewBitmap != null) {
                Image(bitmap = previewBitmap!!, contentDescription = element.displayName, modifier = Modifier.fillMaxSize(), contentScale = if (element == IdLayoutElement.FRONT_PHOTO) ContentScale.Crop else ContentScale.Fit)
            } else Text(element.displayName, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center)
        }
        return
    }

    val fontSizeSp = (element.defaultFontPt / (72f / 25.4f) * (cardWidthDp / ID_LAYOUT_WIDTH_MM) * placement.fontScale).coerceIn(4.5f, 34f)
    val family = when (placement.fontFamilyKey) { "serif" -> FontFamily.Serif; "monospace" -> FontFamily.Monospace; else -> FontFamily.SansSerif }
    val align = when (placement.alignment) { IdTextAlignment.LEFT -> TextAlign.Left; IdTextAlignment.CENTER -> TextAlign.Center; IdTextAlignment.RIGHT -> TextAlign.Right }
    val shadow = if (placement.shadowEnabled) {
        val base = Color(placement.shadowColor)
        Shadow(base.copy(alpha = placement.shadowOpacity), Offset(placement.shadowDxMm * 1.7f, placement.shadowDyMm * 1.7f), placement.shadowRadiusPt * 1.5f)
    } else null

    Box(modifier, contentAlignment = Alignment.TopStart) {
        if (placement.textOutlineEnabled) {
            Text(
                previewText, modifier = Modifier.fillMaxWidth(), color = Color(placement.textOutlineColor),
                fontSize = fontSizeSp.sp, fontFamily = family, fontWeight = if (placement.bold) FontWeight.Bold else FontWeight.Normal,
                textAlign = align, overflow = TextOverflow.Ellipsis,
                style = TextStyle(drawStyle = Stroke(width = placement.textOutlineWidthPt * 1.35f))
            )
        }
        Text(
            previewText, modifier = Modifier.fillMaxWidth(), color = Color(placement.textColor),
            fontSize = fontSizeSp.sp, fontFamily = family, fontWeight = if (placement.bold) FontWeight.Bold else FontWeight.Normal,
            textAlign = align, overflow = TextOverflow.Ellipsis, style = TextStyle(shadow = shadow)
        )
        if (placement.underlineEnabled) {
            Canvas(Modifier.matchParentSize()) {
                val y = (size.height * 0.78f + placement.underlineOffsetMm * 1.5f).coerceAtMost(size.height - 1f)
                val full = placement.underlineWidthMode == IdUnderlineWidthMode.ELEMENT
                val width = if (full) size.width else size.width * 0.68f
                val startX = when (placement.alignment) {
                    IdTextAlignment.LEFT -> 0f
                    IdTextAlignment.CENTER -> (size.width - width) / 2f
                    IdTextAlignment.RIGHT -> size.width - width
                }
                drawLine(Color(placement.underlineColor), Offset(startX, y), Offset(startX + width, y), placement.underlineThicknessPt.coerceAtLeast(0.15f))
            }
        }
    }
}

private fun layoutPreviewImageUri(element: IdLayoutElement, prefs: android.content.SharedPreferences, employee: Employee?): String? = when (element) {
    IdLayoutElement.FRONT_LOGO_1 -> prefs.getString("logo1_uri", null)
    IdLayoutElement.FRONT_LOGO_2 -> prefs.getString("logo2_uri", null)
    IdLayoutElement.FRONT_PHOTO -> employee?.photoUri
    IdLayoutElement.FRONT_SIGNATURE -> employee?.signatureUri
    IdLayoutElement.FRONT_QR -> employee?.qrImageUri
    IdLayoutElement.BACK_CAPTAIN_SIGNATURE -> prefs.getString("captain_signature_uri", null)
    else -> null
}

private fun layoutPreviewText(element: IdLayoutElement, prefs: android.content.SharedPreferences, employee: Employee?): String = when (element) {
    IdLayoutElement.FRONT_BARANGAY -> prefs.getString("barangay", element.sampleText) ?: element.sampleText
    IdLayoutElement.FRONT_MUNICIPALITY -> cleanPlaceLabel(prefs.getString("municipality", "Municipality of Sta. Cruz"), "Municipality of", "STA. CRUZ")
    IdLayoutElement.FRONT_PROVINCE -> cleanPlaceLabel(prefs.getString("province", "Province of Davao del Sur"), "Province of", "DAVAO DEL SUR")
    IdLayoutElement.FRONT_ID_TITLE -> prefs.getString("id_heading", element.sampleText) ?: element.sampleText
    IdLayoutElement.BACK_ISSUER_VALUE -> prefs.getString("issuer_name", element.sampleText) ?: element.sampleText
    IdLayoutElement.BACK_CAPTAIN_NAME -> prefs.getString("captain_name", element.sampleText)?.ifBlank { element.sampleText } ?: element.sampleText
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

internal fun cleanPlaceLabel(raw: String?, prefix: String, fallback: String): String {
    val value = raw.orEmpty().trim()
    if (value.isBlank()) return fallback
    return value.replace(Regex("(?i)^\\s*${Regex.escape(prefix)}\\s*"), "").trim().uppercase(Locale.ENGLISH).ifBlank { fallback }
}

@Composable
private fun LayoutFallbackBackground(side: IdLayoutSide) {
    Box(Modifier.fillMaxSize().background(Color.White)) {
        if (side == IdLayoutSide.FRONT) Box(Modifier.fillMaxWidth().height(70.dp).background(EditorGreen))
    }
}

@Composable
private fun rememberLayoutBitmap(uriString: String?): State<androidx.compose.ui.graphics.ImageBitmap?> {
    val context = LocalContext.current
    return produceState<androidx.compose.ui.graphics.ImageBitmap?>(initialValue = null, key1 = uriString) {
        value = if (uriString.isNullOrBlank()) null else withContext(Dispatchers.IO) {
            runCatching {
                val uri = Uri.parse(uriString)
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
                if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@runCatching null
                var sample = 1
                while (maxOf(bounds.outWidth / sample, bounds.outHeight / sample) > 1600) sample *= 2
                val options = BitmapFactory.Options().apply { inSampleSize = sample; inPreferredConfig = android.graphics.Bitmap.Config.ARGB_8888 }
                context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options)?.asImageBitmap() }
            }.getOrNull()
        }
    }
}

@Composable
private fun LayoutSlider(label: String, value: Float, range: ClosedFloatingPointRange<Float>, enabled: Boolean, valueText: String = "${"%.1f".format(value)} mm", onValueChange: (Float) -> Unit) {
    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.bodySmall)
            Text(valueText, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
        }
        Slider(value = value.coerceIn(range.start, range.endInclusive), onValueChange = onValueChange, valueRange = range, enabled = enabled)
    }
}

@Composable
private fun SettingsToggle(label: String, checked: Boolean, enabled: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled)
    }
}

@Composable
private fun SmallAction(label: String, enabled: Boolean, action: () -> Unit) {
    OutlinedButton(onClick = action, enabled = enabled, contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)) { Text(label) }
}

@Composable
private fun FontChip(label: String, key: String, placement: IdElementPlacement, enabled: Boolean, select: (String) -> Unit) {
    FilterChip(selected = placement.fontFamilyKey == key, onClick = { select(key) }, enabled = enabled, label = { Text(label) })
}

private val palette = listOf(
    "Black" to AndroidColor.BLACK,
    "White" to AndroidColor.WHITE,
    "Green" to AndroidColor.rgb(0, 82, 45),
    "Dark Green" to AndroidColor.rgb(0, 55, 30),
    "Yellow" to AndroidColor.rgb(247, 190, 0),
    "Red" to AndroidColor.rgb(196, 28, 38),
    "Blue" to AndroidColor.rgb(30, 90, 180),
    "Gray" to AndroidColor.rgb(100, 100, 100)
)

@Composable
private fun ColorPalette(label: String, selected: Int, enabled: Boolean, choose: (Int) -> Unit) {
    Text(label, style = MaterialTheme.typography.bodySmall)
    palette.chunked(4).forEach { row ->
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            row.forEach { (name, color) ->
                FilterChip(selected = selected == color, onClick = { choose(color) }, enabled = enabled, label = { Text(name) })
            }
        }
    }
}

@Composable
private fun CustomHexColor(current: Int, enabled: Boolean, choose: (Int) -> Unit) {
    var hex by remember(current) { mutableStateOf(String.format("#%06X", 0xFFFFFF and current)) }
    OutlinedTextField(
        value = hex,
        onValueChange = { value ->
            hex = value.take(7)
            if (hex.matches(Regex("#[0-9A-Fa-f]{6}"))) runCatching { AndroidColor.parseColor(hex) }.getOrNull()?.let(choose)
        },
        label = { Text("Custom HEX") },
        singleLine = true,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth()
    )
}

private fun layoutOverlapWarnings(side: IdLayoutSide, placements: Map<IdLayoutElement, IdElementPlacement>): List<String> {
    fun p(e: IdLayoutElement) = placements[e]?.takeIf { it.visible }
    fun overlaps(a: IdLayoutElement, b: IdLayoutElement): Boolean {
        val x = p(a) ?: return false; val y = p(b) ?: return false
        return x.xMm < y.xMm + y.widthMm && x.xMm + x.widthMm > y.xMm && x.yMm < y.yMm + y.heightMm && x.yMm + x.heightMm > y.yMm
    }
    val warnings = mutableListOf<String>()
    if (side == IdLayoutSide.FRONT) {
        val info = listOf(IdLayoutElement.FRONT_NAME_LABEL, IdLayoutElement.FRONT_NAME_VALUE, IdLayoutElement.FRONT_DESIGNATION_LABEL, IdLayoutElement.FRONT_DESIGNATION_VALUE, IdLayoutElement.FRONT_EMPLOYEE_NO_LABEL, IdLayoutElement.FRONT_EMPLOYEE_NO_VALUE)
        if (info.any { overlaps(IdLayoutElement.FRONT_PHOTO, it) }) warnings += "Employee photo overlaps the information area."
        if (overlaps(IdLayoutElement.FRONT_QR, IdLayoutElement.FRONT_EMPLOYEE_NO_VALUE) || overlaps(IdLayoutElement.FRONT_QR_LABEL, IdLayoutElement.FRONT_EMPLOYEE_NO_VALUE)) warnings += "QR block overlaps Employee No."
        if (overlaps(IdLayoutElement.FRONT_QR, IdLayoutElement.FRONT_SIGNATURE)) warnings += "QR and holder signature overlap."
    } else {
        if (overlaps(IdLayoutElement.BACK_ADDRESS_VALUE, IdLayoutElement.BACK_IDENTIFICATION_HEADING)) warnings += "Address overlaps Identification."
        if (overlaps(IdLayoutElement.BACK_IDENTIFICATION_BODY, IdLayoutElement.BACK_ISSUED_LABEL)) warnings += "Identification overlaps Issued/Approved section."
        if (overlaps(IdLayoutElement.BACK_NOTICE_BODY, IdLayoutElement.BACK_FOOTER_ADDRESS) || overlaps(IdLayoutElement.BACK_NOTICE_BODY, IdLayoutElement.BACK_FOOTER_CONTACT)) warnings += "Important Notice overlaps the footer."
    }
    return warnings
}
