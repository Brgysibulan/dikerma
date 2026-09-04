package ph.gov.barangaysibulan.idmaker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
                            AppScreen.RECORDS -> PlaceholderScreen("Employee Records", "CRUD + search screen is wired next.")
                            AppScreen.GENERATE -> PlaceholderScreen("Generate ID", "Fixed CR80 front/back renderer + A4 PDF comes next.")
                            AppScreen.SETTINGS -> PlaceholderScreen("Settings", "Logo 1, Logo 2, Punong Barangay name/signature will be stored locally.")
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
        Text("Offline • Single-user • CR80 • A4 PDF", style = MaterialTheme.typography.bodyMedium)
        Button(onClick = onRecords, modifier = Modifier.fillMaxWidth().height(56.dp)) { Text("Employee Records") }
        Button(onClick = onGenerate, modifier = Modifier.fillMaxWidth().height(56.dp)) { Text("Generate ID") }
        OutlinedButton(onClick = onSettings, modifier = Modifier.fillMaxWidth().height(56.dp)) { Text("Settings") }
        HorizontalDivider()
        Text("Permission-safe setup", style = MaterialTheme.typography.titleMedium)
        Text("No broad storage permission. Images will use Android's system picker and PDFs will use Save As/Create Document.")
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
