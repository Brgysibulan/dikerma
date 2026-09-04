package ph.gov.barangaysibulan.idmaker.data

import android.content.Context

class AppSettings(context: Context) {
    private val prefs = context.getSharedPreferences("id_maker_settings", Context.MODE_PRIVATE)

    var logo1Uri: String?
        get() = prefs.getString("logo1Uri", null)
        set(value) = prefs.edit().putString("logo1Uri", value).apply()

    var logo2Uri: String?
        get() = prefs.getString("logo2Uri", null)
        set(value) = prefs.edit().putString("logo2Uri", value).apply()

    var captainName: String
        get() = prefs.getString("captainName", "") ?: ""
        set(value) = prefs.edit().putString("captainName", value).apply()

    var captainSignatureUri: String?
        get() = prefs.getString("captainSignatureUri", null)
        set(value) = prefs.edit().putString("captainSignatureUri", value).apply()
}
