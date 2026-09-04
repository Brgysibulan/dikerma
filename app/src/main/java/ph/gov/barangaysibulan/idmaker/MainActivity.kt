package ph.gov.barangaysibulan.idmaker

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ph.gov.barangaysibulan.idmaker.ui.AppScreen

private val BarangayGreen = Color(0xFF1C5C30)
private val BarangayGreenContainer = Color(0xFFDDEBDF)
private val BarangayYellow = Color(0xFFF2C94C)
private val BarangayRed = Color(0xFFB23A3A)

@Composable
private fun BarangayIdMakerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = BarangayGreen,
            onPrimary = Color.White,
            primaryContainer = BarangayGreenContainer,
            onPrimaryContainer = Color(0xFF0B2614),
            secondary = BarangayYellow,
            onSecondary = Color(0xFF241A00),
            tertiary = BarangayRed,
            onTertiary = Color.White
        ),
        content = content
    )
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BarangayIdMakerTheme {
                var screen by remember { mutableStateOf(AppScreen.HOME) }
                Scaffold(
                    bottomBar = {
                        NavigationBar {
                            NavigationBarItem(
                                selected = screen == AppScreen.HOME,
                                onClick = { screen = AppScreen.HOME },
                                icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                                label = { Text("Home") }
                            )
                            NavigationBarItem(
                                selected = screen == AppScreen.RECORDS,
                                onClick = { screen = AppScreen.RECORDS },
                                icon = { Icon(Icons.Default.People, contentDescription = "Records") },
                                label = { Text("Records") }
                            )
                            NavigationBarItem(
                                selected = screen == AppScreen.GENERATE,
                                onClick = { screen = AppScreen.GENERATE },
                                icon = { Icon(Icons.Default.Badge, contentDescription = "Generate") },
                                label = { Text("Generate") }
                            )
                            NavigationBarItem(
                                selected = screen == AppScreen.SETTINGS,
                                onClick = { screen = AppScreen.SETTINGS },
                                icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                                label = { Text("Settings") }
                            )
                        }
                    }
                ) { padding ->
                    Surface(Modifier.fillMaxSize().padding(padding)) {
                        when (screen) {
                            AppScreen.HOME -> HomeScreen(
                                onRecords = { screen = AppScreen.RECORDS },
                                onGenerate = { screen = AppScreen.GENERATE },
                                onSettings = { screen = AppScreen.SETTINGS }
                            )
                            AppScreen.RECORDS -> RecordsScreen()
                            AppScreen.GENERATE -> TightPortraitGenerateScreen()
                            AppScreen.SETTINGS -> SettingsScreen()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeScreen(onRecords: () -> Unit, onGenerate: () -> Unit, onSettings: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(58.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        "B",
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 26.sp
                    )
                }
            }
            Column(Modifier.weight(1f)) {
                Text("Barangay ID Maker", style = MaterialTheme.typography.headlineMedium)
                Text("Fully offline ID preparation and printing.", style = MaterialTheme.typography.bodyMedium)
            }
        }

        Card(Modifier.fillMaxWidth()) {
            Text(
                "Camera or Gallery photo • Offline white-background cleanup • QR image upload • Up to 2 people per A4 sheet",
                modifier = Modifier.padding(14.dp),
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Button(onClick = onRecords, modifier = Modifier.fillMaxWidth().height(58.dp)) {
            Text("Employee Records")
        }
        Button(onClick = onGenerate, modifier = Modifier.fillMaxWidth().height(58.dp)) {
            Text("Generate ID")
        }
        OutlinedButton(onClick = onSettings, modifier = Modifier.fillMaxWidth().height(58.dp)) {
            Text("ID Setup & Settings")
        }

        HorizontalDivider()
        Text("Quick Setup", style = MaterialTheme.typography.titleMedium)
        Text(
            "1. Upload the front and back ID design in Settings.\n" +
                "2. Upload Logo 1, Logo 2 and the Punong Barangay signature.\n" +
                "3. Add employee records and use Camera or Gallery for the ID photo.\n" +
                "4. Upload the employee WEBV3LITE QR image when available.\n" +
                "5. Select Person 1 and optional Person 2, then generate the A4 PDF."
        )
        Text(
            "Print using Actual Size / 100%. Do not use Fit to Page.",
            color = MaterialTheme.colorScheme.tertiary,
            fontWeight = FontWeight.SemiBold,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun SettingsScreen() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("id_maker_settings", android.content.Context.MODE_PRIVATE) }

    var republic by remember { mutableStateOf(prefs.getString("republic", "REPUBLIC OF THE PHILIPPINES") ?: "") }
    var province by remember { mutableStateOf(prefs.getString("province", "Province of Davao del Sur") ?: "") }
    var municipality by remember { mutableStateOf(prefs.getString("municipality", "Municipality of Sta. Cruz") ?: "") }
    var barangay by remember { mutableStateOf(prefs.getString("barangay", "BARANGAY SIBULAN") ?: "") }
    var idHeading by remember { mutableStateOf(prefs.getString("id_heading", "BARANGAY EMPLOYEE ID") ?: "") }
    var captainName by remember { mutableStateOf(prefs.getString("captain_name", "") ?: "") }
    var captainTitle by remember { mutableStateOf(prefs.getString("captain_title", "PUNONG BARANGAY") ?: "") }

    var frontTemplateUri by remember { mutableStateOf(prefs.getString("front_template_uri", "") ?: "") }
    var backTemplateUri by remember { mutableStateOf(prefs.getString("back_template_uri", "") ?: "") }
    var logo1Uri by remember { mutableStateOf(prefs.getString("logo1_uri", "") ?: "") }
    var logo2Uri by remember { mutableStateOf(prefs.getString("logo2_uri", "") ?: "") }
    var captainSignatureUri by remember { mutableStateOf(prefs.getString("captain_signature_uri", "") ?: "") }
    var savedMessage by remember { mutableStateOf("") }

    fun persistPickedUri(uriString: String, key: String) {
        if (uriString.isBlank()) return
        runCatching {
            val uri = android.net.Uri.parse(uriString)
            context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        prefs.edit().putString(key, uriString).apply()
    }

    val frontTemplatePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            frontTemplateUri = it.toString()
            persistPickedUri(frontTemplateUri, "front_template_uri")
            savedMessage = "Front ID design saved"
        }
    }
    val backTemplatePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            backTemplateUri = it.toString()
            persistPickedUri(backTemplateUri, "back_template_uri")
            savedMessage = "Back ID design saved"
        }
    }
    val logo1Picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            logo1Uri = it.toString()
            persistPickedUri(logo1Uri, "logo1_uri")
            savedMessage = "Logo 1 saved"
        }
    }
    val logo2Picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            logo2Uri = it.toString()
            persistPickedUri(logo2Uri, "logo2_uri")
            savedMessage = "Logo 2 saved"
        }
    }
    val signaturePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            captainSignatureUri = it.toString()
            persistPickedUri(captainSignatureUri, "captain_signature_uri")
            savedMessage = "Punong Barangay signature saved"
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("ID Setup & Settings", style = MaterialTheme.typography.headlineSmall)
        Text("Set these once. The app reuses the saved assets automatically when generating IDs.", style = MaterialTheme.typography.bodyMedium)

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Setup Status", fontWeight = FontWeight.Bold)
                Text("Front design: ${if (frontTemplateUri.isNotBlank()) "Ready" else "Missing"}")
                Text("Back design: ${if (backTemplateUri.isNotBlank()) "Ready" else "Missing"}")
                Text("Logo 1: ${if (logo1Uri.isNotBlank()) "Ready" else "Optional / Missing"}")
                Text("Logo 2: ${if (logo2Uri.isNotBlank()) "Ready" else "Optional / Missing"}")
                Text("Signatory signature: ${if (captainSignatureUri.isNotBlank()) "Ready" else "Optional / Missing"}")
            }
        }

        Text("ID Design", style = MaterialTheme.typography.titleMedium)
        Text("Upload the blank front and back design using the same CR80 portrait layout (53.98 × 85.60 mm ratio).", style = MaterialTheme.typography.bodySmall)
        AssetPickerRow("Front ID Design", frontTemplateUri.isNotBlank()) { frontTemplatePicker.launch(arrayOf("image/*")) }
        AssetPickerRow("Back ID Design", backTemplateUri.isNotBlank()) { backTemplatePicker.launch(arrayOf("image/*")) }

        HorizontalDivider()
        Text("Logos", style = MaterialTheme.typography.titleMedium)
        AssetPickerRow("Logo 1", logo1Uri.isNotBlank()) { logo1Picker.launch(arrayOf("image/*")) }
        AssetPickerRow("Logo 2", logo2Uri.isNotBlank()) { logo2Picker.launch(arrayOf("image/*")) }

        HorizontalDivider()
        Text("ID Heading", style = MaterialTheme.typography.titleMedium)
        SettingsTextField("Republic", republic) { republic = it }
        SettingsTextField("Province", province) { province = it }
        SettingsTextField("Municipality", municipality) { municipality = it }
        SettingsTextField("Barangay", barangay) { barangay = it }
        SettingsTextField("ID Heading", idHeading) { idHeading = it }

        HorizontalDivider()
        Text("Punong Barangay / Signatory", style = MaterialTheme.typography.titleMedium)
        SettingsTextField("Name", captainName) { captainName = it }
        SettingsTextField("Position", captainTitle) { captainTitle = it }
        AssetPickerRow("Signature PNG", captainSignatureUri.isNotBlank()) { signaturePicker.launch(arrayOf("image/*")) }

        Button(
            onClick = {
                prefs.edit()
                    .putString("republic", republic.trim())
                    .putString("province", province.trim())
                    .putString("municipality", municipality.trim())
                    .putString("barangay", barangay.trim())
                    .putString("id_heading", idHeading.trim())
                    .putString("captain_name", captainName.trim())
                    .putString("captain_title", captainTitle.trim())
                    .apply()
                savedMessage = "Settings saved"
            },
            modifier = Modifier.fillMaxWidth().height(52.dp)
        ) { Text("Save Settings") }

        if (savedMessage.isNotBlank()) {
            Text(savedMessage, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodyMedium)
        }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun SettingsTextField(label: String, value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true
    )
}

@Composable
private fun AssetPickerRow(label: String, isSelected: Boolean, onPick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            Text(if (isSelected) "Saved on device" else "Not selected", style = MaterialTheme.typography.bodySmall)
        }
        OutlinedButton(onClick = onPick) { Text(if (isSelected) "Replace" else "Upload") }
    }
}
