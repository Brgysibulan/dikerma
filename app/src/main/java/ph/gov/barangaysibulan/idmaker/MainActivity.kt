package ph.gov.barangaysibulan.idmaker

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import ph.gov.barangaysibulan.idmaker.ui.AppScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
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
                            AppScreen.GENERATE -> GenerateScreen()
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
        Text("Barangay ID Maker", style = MaterialTheme.typography.headlineMedium)
        Text("Create and print employee IDs offline.", style = MaterialTheme.typography.bodyLarge)

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
        Text("1. Upload the front and back ID design in Settings.\n2. Upload Logo 1, Logo 2 and the Punong Barangay signature.\n3. Add employee records.\n4. Select an employee and generate the A4 PDF.")
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
        }
    }
    val backTemplatePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            backTemplateUri = it.toString()
            persistPickedUri(backTemplateUri, "back_template_uri")
        }
    }
    val logo1Picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            logo1Uri = it.toString()
            persistPickedUri(logo1Uri, "logo1_uri")
        }
    }
    val logo2Picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            logo2Uri = it.toString()
            persistPickedUri(logo2Uri, "logo2_uri")
        }
    }
    val signaturePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            captainSignatureUri = it.toString()
            persistPickedUri(captainSignatureUri, "captain_signature_uri")
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("ID Setup & Settings", style = MaterialTheme.typography.headlineSmall)
        Text("Set these once. The app will reuse them automatically when generating IDs.", style = MaterialTheme.typography.bodyMedium)

        Text("ID Design", style = MaterialTheme.typography.titleMedium)
        Text("Upload the blank front and back design. Keep both images in the same fixed CR80 layout.", style = MaterialTheme.typography.bodySmall)
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
            Text(savedMessage, style = MaterialTheme.typography.bodyMedium)
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

@Composable
private fun PlaceholderScreen(title: String, subtitle: String) {
    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, style = MaterialTheme.typography.headlineSmall)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
