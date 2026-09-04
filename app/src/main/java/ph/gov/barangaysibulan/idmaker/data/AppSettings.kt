package ph.gov.barangaysibulan.idmaker.data

import android.content.Context

class AppSettings(context: Context) {
    private val prefs = context.getSharedPreferences("id_maker_settings", Context.MODE_PRIVATE)

    var logo1Uri: String?
        get() = prefs.getString("logo1_uri", null)
        set(value) = prefs.edit().putString("logo1_uri", value).apply()

    var logo2Uri: String?
        get() = prefs.getString("logo2_uri", null)
        set(value) = prefs.edit().putString("logo2_uri", value).apply()

    var captainName: String
        get() = prefs.getString("captain_name", "") ?: ""
        set(value) = prefs.edit().putString("captain_name", value).apply()

    var captainTitle: String
        get() = prefs.getString("captain_title", "PUNONG BARANGAY") ?: "PUNONG BARANGAY"
        set(value) = prefs.edit().putString("captain_title", value).apply()

    var captainSignatureUri: String?
        get() = prefs.getString("captain_signature_uri", null)
        set(value) = prefs.edit().putString("captain_signature_uri", value).apply()
}
