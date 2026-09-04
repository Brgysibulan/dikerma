package ph.gov.barangaysibulan.idmaker

import android.content.SharedPreferences
import android.graphics.Color

internal const val ID_LAYOUT_WIDTH_MM = 85f
internal const val ID_LAYOUT_HEIGHT_MM = 115f

internal enum class IdLayoutSide { FRONT, BACK }

internal enum class IdLayoutKind { TEXT, IMAGE }

internal enum class IdTextAlignment { LEFT, CENTER, RIGHT }

/**
 * Every overlay is defined in millimetres inside the physical 85 x 115 mm card.
 * The editor and PDF renderer read this same model so a saved layout is uniform
 * for every employee, independent of phone size or preview zoom.
 */
internal enum class IdLayoutElement(
    val storageKey: String,
    val side: IdLayoutSide,
    val displayName: String,
    val kind: IdLayoutKind,
    val defaultXmm: Float,
    val defaultYmm: Float,
    val defaultWidthMm: Float,
    val defaultHeightMm: Float,
    val defaultFontPt: Float = 7f,
    val defaultAlignment: IdTextAlignment = IdTextAlignment.LEFT,
    val defaultColor: Int = Color.BLACK,
    val sampleText: String = ""
) {
    FRONT_LOGO_1("front_logo_1", IdLayoutSide.FRONT, "Logo 1", IdLayoutKind.IMAGE, 5f, 4f, 14f, 14f),
    FRONT_LOGO_2("front_logo_2", IdLayoutSide.FRONT, "Logo 2", IdLayoutKind.IMAGE, 67f, 4f, 12f, 12f),
    FRONT_BARANGAY(
        "front_barangay", IdLayoutSide.FRONT, "Barangay title", IdLayoutKind.TEXT,
        20f, 5.2f, 45f, 6f, 12.5f, IdTextAlignment.CENTER, Color.WHITE, "BARANGAY SIBULAN"
    ),
    FRONT_LOCATION(
        "front_location", IdLayoutSide.FRONT, "Location", IdLayoutKind.TEXT,
        20f, 11.2f, 45f, 4.5f, 7.2f, IdTextAlignment.CENTER, Color.WHITE, "STA. CRUZ, DAVAO DEL SUR"
    ),
    FRONT_ID_TITLE(
        "front_id_title", IdLayoutSide.FRONT, "ID title", IdLayoutKind.TEXT,
        20f, 17.2f, 45f, 5f, 8.6f, IdTextAlignment.CENTER, Color.rgb(0, 82, 45), "BARANGAY EMPLOYEE ID"
    ),
    FRONT_PHOTO("front_photo", IdLayoutSide.FRONT, "Employee photo", IdLayoutKind.IMAGE, 6f, 27f, 31f, 40f),
    FRONT_NAME_LABEL(
        "front_name_label", IdLayoutSide.FRONT, "NAME label", IdLayoutKind.TEXT,
        41f, 28f, 38f, 4f, 6.8f, IdTextAlignment.LEFT, Color.rgb(0, 82, 45), "NAME"
    ),
    FRONT_NAME_VALUE(
        "front_name_value", IdLayoutSide.FRONT, "Employee name", IdLayoutKind.TEXT,
        41f, 33.2f, 38f, 12f, 10.5f, IdTextAlignment.LEFT, Color.BLACK, "ROWENA A. TABO"
    ),
    FRONT_DESIGNATION_LABEL(
        "front_designation_label", IdLayoutSide.FRONT, "DESIGNATION label", IdLayoutKind.TEXT,
        41f, 47f, 38f, 4f, 6.8f, IdTextAlignment.LEFT, Color.rgb(0, 82, 45), "DESIGNATION"
    ),
    FRONT_DESIGNATION_VALUE(
        "front_designation_value", IdLayoutSide.FRONT, "Designation", IdLayoutKind.TEXT,
        41f, 52.2f, 38f, 12f, 9.4f, IdTextAlignment.LEFT, Color.BLACK, "PUNONG BARANGAY"
    ),
    FRONT_EMPLOYEE_NO_LABEL(
        "front_employee_no_label", IdLayoutSide.FRONT, "EMPLOYEE NO. label", IdLayoutKind.TEXT,
        41f, 65f, 38f, 4f, 6.8f, IdTextAlignment.LEFT, Color.rgb(0, 82, 45), "EMPLOYEE NO."
    ),
    FRONT_EMPLOYEE_NO_VALUE(
        "front_employee_no_value", IdLayoutSide.FRONT, "Employee number", IdLayoutKind.TEXT,
        41f, 70.2f, 38f, 5f, 10f, IdTextAlignment.LEFT, Color.BLACK, "2026001"
    ),
    FRONT_SIGNATURE("front_signature", IdLayoutSide.FRONT, "Holder signature", IdLayoutKind.IMAGE, 7f, 79f, 33f, 10f),
    FRONT_SIGNATURE_LABEL(
        "front_signature_label", IdLayoutSide.FRONT, "Signature label", IdLayoutKind.TEXT,
        6f, 90.5f, 35f, 4f, 6.6f, IdTextAlignment.CENTER, Color.rgb(0, 82, 45), "SIGNATURE OF HOLDER"
    ),
    FRONT_QR_LABEL(
        "front_qr_label", IdLayoutSide.FRONT, "QR label", IdLayoutKind.TEXT,
        55f, 77.5f, 24f, 4f, 6.3f, IdTextAlignment.CENTER, Color.rgb(0, 82, 45), "SCAN TO VERIFY"
    ),
    FRONT_QR("front_qr", IdLayoutSide.FRONT, "QR image", IdLayoutKind.IMAGE, 57f, 82.5f, 20f, 20f),

    BACK_DOB_LABEL(
        "back_dob_label", IdLayoutSide.BACK, "DATE OF BIRTH label", IdLayoutKind.TEXT,
        7f, 7f, 22f, 4f, 7f, IdTextAlignment.LEFT, Color.rgb(0, 82, 45), "DATE OF BIRTH:"
    ),
    BACK_DOB_VALUE(
        "back_dob_value", IdLayoutSide.BACK, "Date of birth", IdLayoutKind.TEXT,
        30f, 7f, 48f, 4f, 7.8f, IdTextAlignment.LEFT, Color.BLACK, "January 12, 1987"
    ),
    BACK_SEX_LABEL(
        "back_sex_label", IdLayoutSide.BACK, "SEX label", IdLayoutKind.TEXT,
        7f, 14f, 10f, 4f, 7f, IdTextAlignment.LEFT, Color.rgb(0, 82, 45), "SEX:"
    ),
    BACK_SEX_VALUE(
        "back_sex_value", IdLayoutSide.BACK, "Sex", IdLayoutKind.TEXT,
        17f, 14f, 18f, 4f, 7.8f, IdTextAlignment.LEFT, Color.BLACK, "Female"
    ),
    BACK_CIVIL_LABEL(
        "back_civil_label", IdLayoutSide.BACK, "CIVIL STATUS label", IdLayoutKind.TEXT,
        40f, 14f, 23f, 4f, 7f, IdTextAlignment.LEFT, Color.rgb(0, 82, 45), "CIVIL STATUS:"
    ),
    BACK_CIVIL_VALUE(
        "back_civil_value", IdLayoutSide.BACK, "Civil status", IdLayoutKind.TEXT,
        63f, 14f, 15f, 4f, 7.2f, IdTextAlignment.LEFT, Color.BLACK, "Married"
    ),
    BACK_ADDRESS_LABEL(
        "back_address_label", IdLayoutSide.BACK, "ADDRESS label", IdLayoutKind.TEXT,
        7f, 21f, 71f, 4f, 7f, IdTextAlignment.LEFT, Color.rgb(0, 82, 45), "ADDRESS:"
    ),
    BACK_ADDRESS_VALUE(
        "back_address_value", IdLayoutSide.BACK, "Address", IdLayoutKind.TEXT,
        7f, 26f, 71f, 9f, 7.2f, IdTextAlignment.LEFT, Color.BLACK,
        "Sitio Tungcaling, Barangay Sibulan, Sta. Cruz, Davao del Sur"
    ),
    BACK_IDENTIFICATION_HEADING(
        "back_identification_heading", IdLayoutSide.BACK, "IDENTIFICATION heading", IdLayoutKind.TEXT,
        9f, 38.5f, 67f, 5f, 8.5f, IdTextAlignment.CENTER, Color.rgb(0, 82, 45), "IDENTIFICATION"
    ),
    BACK_IDENTIFICATION_BODY(
        "back_identification_body", IdLayoutSide.BACK, "Identification paragraph", IdLayoutKind.TEXT,
        9f, 44f, 67f, 18f, 7f, IdTextAlignment.LEFT, Color.BLACK,
        "This identification card is issued to the bearer whose photograph appears herein and who is a bona fide employee of the Barangay Local Government Unit of Sibulan."
    ),
    BACK_ISSUED_LABEL(
        "back_issued_label", IdLayoutSide.BACK, "ISSUED BY label", IdLayoutKind.TEXT,
        8f, 64f, 30f, 4f, 7.5f, IdTextAlignment.LEFT, Color.rgb(0, 82, 45), "ISSUED BY:"
    ),
    BACK_ISSUER_VALUE(
        "back_issuer_value", IdLayoutSide.BACK, "Issuer", IdLayoutKind.TEXT,
        8f, 70f, 30f, 5f, 8f, IdTextAlignment.LEFT, Color.BLACK, "BLGU - SIBULAN"
    ),
    BACK_APPROVED_LABEL(
        "back_approved_label", IdLayoutSide.BACK, "APPROVED BY label", IdLayoutKind.TEXT,
        45f, 64f, 31f, 4f, 7.5f, IdTextAlignment.LEFT, Color.rgb(0, 82, 45), "APPROVED BY:"
    ),
    BACK_CAPTAIN_SIGNATURE("back_captain_signature", IdLayoutSide.BACK, "Approver signature", IdLayoutKind.IMAGE, 47f, 67.5f, 27f, 8f),
    BACK_CAPTAIN_NAME(
        "back_captain_name", IdLayoutSide.BACK, "Approver name", IdLayoutKind.TEXT,
        45f, 76f, 33f, 4f, 7.4f, IdTextAlignment.LEFT, Color.BLACK, "ROWENA A. TABO"
    ),
    BACK_CAPTAIN_TITLE(
        "back_captain_title", IdLayoutSide.BACK, "Approver title", IdLayoutKind.TEXT,
        45f, 80f, 31f, 4f, 6.2f, IdTextAlignment.LEFT, Color.BLACK, "Punong Barangay"
    ),
    BACK_NOTICE_HEADING(
        "back_notice_heading", IdLayoutSide.BACK, "IMPORTANT NOTICE heading", IdLayoutKind.TEXT,
        9f, 84f, 67f, 4f, 7.8f, IdTextAlignment.CENTER, Color.rgb(0, 82, 45), "IMPORTANT NOTICE"
    ),
    BACK_NOTICE_BODY(
        "back_notice_body", IdLayoutSide.BACK, "Important notice", IdLayoutKind.TEXT,
        9f, 88.5f, 67f, 14f, 5.7f, IdTextAlignment.LEFT, Color.BLACK,
        "– This ID is non-transferable.\n– This ID remains the property of the BLGU of Sibulan.\n– If lost, report immediately to the Barangay Office.\n– Unauthorized use, alteration, or reproduction is prohibited."
    ),
    BACK_FOOTER_ADDRESS(
        "back_footer_address", IdLayoutSide.BACK, "Footer address", IdLayoutKind.TEXT,
        5f, 104f, 75f, 4f, 5.4f, IdTextAlignment.CENTER, Color.WHITE,
        "Barangay Hall, Sitio Centro, Barangay Sibulan, Sta. Cruz, Davao del Sur"
    ),
    BACK_FOOTER_CONTACT(
        "back_footer_contact", IdLayoutSide.BACK, "Footer contact", IdLayoutKind.TEXT,
        5f, 108f, 75f, 4f, 5.3f, IdTextAlignment.CENTER, Color.WHITE,
        "brgysibulan8001@gmail.com  |  0970 972 3363"
    );

    companion object {
        fun forSide(side: IdLayoutSide): List<IdLayoutElement> = entries.filter { it.side == side }
    }
}

internal data class IdElementPlacement(
    val xMm: Float,
    val yMm: Float,
    val widthMm: Float,
    val heightMm: Float,
    val fontScale: Float,
    val alignment: IdTextAlignment,
    val textColor: Int,
    val underlineEnabled: Boolean,
    val textOutlineEnabled: Boolean,
    val textOutlineWidthPt: Float,
    val visible: Boolean
) {
    fun clamped(): IdElementPlacement {
        val safeWidth = widthMm.coerceIn(4f, ID_LAYOUT_WIDTH_MM)
        val safeHeight = heightMm.coerceIn(2f, ID_LAYOUT_HEIGHT_MM)
        return copy(
            widthMm = safeWidth,
            heightMm = safeHeight,
            xMm = xMm.coerceIn(0f, ID_LAYOUT_WIDTH_MM - safeWidth),
            yMm = yMm.coerceIn(0f, ID_LAYOUT_HEIGHT_MM - safeHeight),
            fontScale = fontScale.coerceIn(0.65f, 1.50f),
            textOutlineWidthPt = textOutlineWidthPt.coerceIn(0.15f, 1.50f)
        )
    }
}

internal fun IdLayoutElement.defaultPlacement(): IdElementPlacement = IdElementPlacement(
    xMm = defaultXmm,
    yMm = defaultYmm,
    widthMm = defaultWidthMm,
    heightMm = defaultHeightMm,
    fontScale = 1f,
    alignment = defaultAlignment,
    textColor = defaultColor,
    underlineEnabled = false,
    textOutlineEnabled = false,
    textOutlineWidthPt = 0.35f,
    visible = true
)

internal class IdLayoutStore(private val prefs: SharedPreferences) {
    fun load(element: IdLayoutElement): IdElementPlacement {
        val defaults = element.defaultPlacement()
        val prefix = prefix(element)
        return IdElementPlacement(
            xMm = prefs.getFloat("${prefix}_x", defaults.xMm),
            yMm = prefs.getFloat("${prefix}_y", defaults.yMm),
            widthMm = prefs.getFloat("${prefix}_w", defaults.widthMm),
            heightMm = prefs.getFloat("${prefix}_h", defaults.heightMm),
            fontScale = prefs.getFloat("${prefix}_font_scale", defaults.fontScale),
            alignment = prefs.getString("${prefix}_align", defaults.alignment.name)
                ?.let { runCatching { IdTextAlignment.valueOf(it) }.getOrNull() }
                ?: defaults.alignment,
            textColor = prefs.getInt("${prefix}_color", defaults.textColor),
            underlineEnabled = prefs.getBoolean("${prefix}_underline", defaults.underlineEnabled),
            textOutlineEnabled = prefs.getBoolean("${prefix}_text_outline", defaults.textOutlineEnabled),
            textOutlineWidthPt = prefs.getFloat("${prefix}_text_outline_width", defaults.textOutlineWidthPt),
            visible = prefs.getBoolean("${prefix}_visible", defaults.visible)
        ).clamped()
    }

    fun loadSide(side: IdLayoutSide): Map<IdLayoutElement, IdElementPlacement> =
        IdLayoutElement.forSide(side).associateWith(::load)

    fun save(placements: Map<IdLayoutElement, IdElementPlacement>, locked: Boolean) {
        val editor = prefs.edit()
        placements.forEach { (element, rawPlacement) ->
            val placement = rawPlacement.clamped()
            val prefix = prefix(element)
            editor
                .putFloat("${prefix}_x", placement.xMm)
                .putFloat("${prefix}_y", placement.yMm)
                .putFloat("${prefix}_w", placement.widthMm)
                .putFloat("${prefix}_h", placement.heightMm)
                .putFloat("${prefix}_font_scale", placement.fontScale)
                .putString("${prefix}_align", placement.alignment.name)
                .putInt("${prefix}_color", placement.textColor)
                .putBoolean("${prefix}_underline", placement.underlineEnabled)
                .putBoolean("${prefix}_text_outline", placement.textOutlineEnabled)
                .putFloat("${prefix}_text_outline_width", placement.textOutlineWidthPt)
                .putBoolean("${prefix}_visible", placement.visible)
        }
        editor
            .putBoolean(KEY_LAYOUT_LOCKED, locked)
            .putBoolean(KEY_LAYOUT_SAVED, true)
            .apply()
    }

    fun isLocked(): Boolean = prefs.getBoolean(KEY_LAYOUT_LOCKED, false)

    fun isSaved(): Boolean = prefs.getBoolean(KEY_LAYOUT_SAVED, false)

    private fun prefix(element: IdLayoutElement): String = "id_layout_v1_${element.storageKey}"

    companion object {
        const val KEY_LAYOUT_LOCKED = "id_layout_v1_locked"
        const val KEY_LAYOUT_SAVED = "id_layout_v1_saved"
    }
}
