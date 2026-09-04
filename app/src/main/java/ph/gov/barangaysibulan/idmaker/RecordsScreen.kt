package ph.gov.barangaysibulan.idmaker

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import ph.gov.barangaysibulan.idmaker.data.AppDatabase
import ph.gov.barangaysibulan.idmaker.data.Employee

@Composable
fun RecordsScreen() {
    val context = LocalContext.current
    val dao = remember { AppDatabase.get(context).employeeDao() }
    val employees by dao.observeAll().collectAsState(initial = emptyList())

    var search by remember { mutableStateOf("") }
    var editingEmployee by remember { mutableStateOf<Employee?>(null) }
    var addingNew by remember { mutableStateOf(false) }
    var employeeToDelete by remember { mutableStateOf<Employee?>(null) }

    if (addingNew || editingEmployee != null) {
        EmployeeEditorScreen(
            employee = editingEmployee,
            onCancel = {
                addingNew = false
                editingEmployee = null
            },
            onSaved = {
                addingNew = false
                editingEmployee = null
            }
        )
        return
    }

    val filtered = remember(employees, search) {
        val q = search.trim()
        if (q.isBlank()) employees else employees.filter { employee ->
            employee.fullName.contains(q, ignoreCase = true) ||
                employee.position.contains(q, ignoreCase = true) ||
                employee.controlNumber.contains(q, ignoreCase = true) ||
                employee.status.contains(q, ignoreCase = true)
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("Employee Records", style = MaterialTheme.typography.headlineSmall)
                Text("${employees.size} saved record${if (employees.size == 1) "" else "s"}", style = MaterialTheme.typography.bodySmall)
            }
            Button(onClick = { addingNew = true }) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("Add")
            }
        }

        OutlinedTextField(
            value = search,
            onValueChange = { search = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            label = { Text("Search name, position or ID no.") }
        )

        if (filtered.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(if (employees.isEmpty()) "No employee records yet" else "No matching records")
                    if (employees.isEmpty()) {
                        Spacer(Modifier.height(10.dp))
                        Button(onClick = { addingNew = true }) { Text("Add First Employee") }
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(filtered, key = { it.id }) { employee ->
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable { editingEmployee = employee }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(employee.fullName, style = MaterialTheme.typography.titleMedium)
                                Text(employee.position, style = MaterialTheme.typography.bodyMedium)
                                Text("ID No.: ${employee.controlNumber}", style = MaterialTheme.typography.bodySmall)
                                Text(employee.status, style = MaterialTheme.typography.labelMedium)
                            }
                            IconButton(onClick = { editingEmployee = employee }) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit")
                            }
                            IconButton(onClick = { employeeToDelete = employee }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete")
                            }
                        }
                    }
                }
            }
        }
    }

    employeeToDelete?.let { employee ->
        val scope = rememberCoroutineScope()
        AlertDialog(
            onDismissRequest = { employeeToDelete = null },
            title = { Text("Delete employee?") },
            text = { Text("Delete ${employee.fullName}? This record will be removed from this device.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch { dao.delete(employee) }
                        employeeToDelete = null
                    }
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { employeeToDelete = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun EmployeeEditorScreen(
    employee: Employee?,
    onCancel: () -> Unit,
    onSaved: () -> Unit
) {
    val context = LocalContext.current
    val dao = remember { AppDatabase.get(context).employeeDao() }
    val scope = rememberCoroutineScope()

    key(employee?.id ?: 0L) {
        var fullName by remember { mutableStateOf(employee?.fullName ?: "") }
        var position by remember { mutableStateOf(employee?.position ?: "") }
        var controlNumber by remember { mutableStateOf(employee?.controlNumber ?: "") }
        var birthdate by remember { mutableStateOf(employee?.birthdate ?: "") }
        var address by remember { mutableStateOf(employee?.address ?: "") }
        var sex by remember { mutableStateOf(employee?.sex ?: "") }
        var civilStatus by remember { mutableStateOf(employee?.civilStatus ?: "") }
        var photoUri by remember { mutableStateOf(employee?.photoUri ?: "") }
        var signatureUri by remember { mutableStateOf(employee?.signatureUri ?: "") }
        var qrToken by remember { mutableStateOf(employee?.qrToken ?: "") }
        var qrImageUri by remember { mutableStateOf(employee?.qrImageUri ?: "") }
        var status by remember { mutableStateOf(employee?.status ?: "Active") }
        var message by remember { mutableStateOf("") }
        var saving by remember { mutableStateOf(false) }

        var photoProcessing by remember { mutableStateOf(false) }
        var signatureProcessing by remember { mutableStateOf(false) }
        var photoMessage by remember { mutableStateOf("") }
        var signatureMessage by remember { mutableStateOf("") }
        var pendingPhotoCamera by remember { mutableStateOf<CameraTarget?>(null) }
        var pendingSignatureCamera by remember { mutableStateOf<CameraTarget?>(null) }

        fun keepUri(uriString: String) {
            if (uriString.isBlank()) return
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    Uri.parse(uriString),
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
        }

        fun processPhoto(source: Uri, cameraTarget: CameraTarget? = null) {
            photoProcessing = true
            photoMessage = "Processing photo fully offline…"
            scope.launch {
                val result = runCatching { OfflineImageProcessor.processIdPhoto(context, source) }.getOrNull()
                cameraTarget?.file?.delete()
                if (result != null) {
                    photoUri = result.uri
                    photoMessage = result.note
                } else {
                    photoMessage = "Could not separate the background. Retake using a plain solid background with even lighting."
                }
                photoProcessing = false
            }
        }

        fun processSignature(source: Uri, cameraTarget: CameraTarget? = null) {
            signatureProcessing = true
            signatureMessage = "Processing signature fully offline…"
            scope.launch {
                val result = runCatching { OfflineImageProcessor.processSignature(context, source) }.getOrNull()
                cameraTarget?.file?.delete()
                if (result != null) {
                    signatureUri = result.uri
                    signatureMessage = result.note
                } else {
                    signatureMessage = "Could not detect the signature clearly. Use black/dark ink on clean white or light paper and retake."
                }
                signatureProcessing = false
            }
        }

        val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            uri?.let { processPhoto(it) }
        }
        val signaturePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            uri?.let { processSignature(it) }
        }
        val qrImagePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            uri?.let {
                qrImageUri = it.toString()
                keepUri(qrImageUri)
            }
        }

        val photoCamera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            val target = pendingPhotoCamera
            pendingPhotoCamera = null
            if (success && target != null) {
                processPhoto(target.uri, target)
            } else {
                target?.file?.delete()
            }
        }

        val signatureCamera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            val target = pendingSignatureCamera
            pendingSignatureCamera = null
            if (success && target != null) {
                processSignature(target.uri, target)
            } else {
                target?.file?.delete()
            }
        }

        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(if (employee == null) "Add Employee" else "Edit Employee", style = MaterialTheme.typography.headlineSmall)
                    Text("Employee information and image processing stay only on this device.", style = MaterialTheme.typography.bodySmall)
                }
            }

            Spacer(Modifier.height(12.dp))

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 12.dp)
            ) {
                item { EmployeeTextField("Full Name *", fullName) { fullName = it } }
                item { EmployeeTextField("Position / Designation *", position) { position = it } }
                item { EmployeeTextField("Control / ID Number *", controlNumber) { controlNumber = it } }
                item { EmployeeTextField("Birthdate", birthdate, "MM/DD/YYYY") { birthdate = it } }
                item { EmployeeTextField("Address", address, singleLine = false) { address = it } }

                item {
                    Text("Sex", style = MaterialTheme.typography.labelLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ChoiceButton("Male", sex == "Male") { sex = "Male" }
                        ChoiceButton("Female", sex == "Female") { sex = "Female" }
                    }
                }

                item { EmployeeTextField("Civil Status", civilStatus, "Single / Married / etc.") { civilStatus = it } }

                item {
                    HorizontalDivider()
                    Spacer(Modifier.height(6.dp))
                    Text("Photo & Signature — Offline Auto Cleanup", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "ID photo: use a plain solid-color background. Signature: use black/dark ink on clean white or light paper.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                item {
                    ProcessedAssetCard(
                        title = "ID Photo",
                        instruction = "The app removes the plain background, replaces it with pure white, then auto-crops the photo.",
                        uriString = photoUri,
                        processing = photoProcessing,
                        statusText = photoMessage,
                        onCamera = {
                            runCatching {
                                OfflineImageProcessor.createCameraTarget(context, "id_photo_")
                            }.onSuccess { target ->
                                pendingPhotoCamera = target
                                photoCamera.launch(target.uri)
                            }.onFailure {
                                photoMessage = "Could not open a camera target on this device."
                            }
                        },
                        onGallery = { photoPicker.launch(arrayOf("image/*")) },
                        onClear = {
                            photoUri = ""
                            photoMessage = "Photo cleared."
                        }
                    )
                }

                item {
                    ProcessedAssetCard(
                        title = "Employee Signature",
                        instruction = "The app removes the light paper/background, keeps the dark signature, auto-crops it, and saves transparent PNG.",
                        uriString = signatureUri,
                        processing = signatureProcessing,
                        statusText = signatureMessage,
                        onCamera = {
                            runCatching {
                                OfflineImageProcessor.createCameraTarget(context, "signature_")
                            }.onSuccess { target ->
                                pendingSignatureCamera = target
                                signatureCamera.launch(target.uri)
                            }.onFailure {
                                signatureMessage = "Could not open a camera target on this device."
                            }
                        },
                        onGallery = { signaturePicker.launch(arrayOf("image/*")) },
                        onClear = {
                            signatureUri = ""
                            signatureMessage = "Signature cleared."
                        }
                    )
                }

                item {
                    HorizontalDivider()
                    Spacer(Modifier.height(6.dp))
                    Text("WEBV3LITE Verification", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "You can either paste the WEBV3LITE token/verification URL or upload the QR image downloaded from WEBV3LITE.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                item { EmployeeTextField("QR Token / Verification URL (optional)", qrToken) { qrToken = it } }
                item {
                    EmployeeAssetRow("Upload WEBV3LITE QR Image", qrImageUri.isNotBlank()) {
                        qrImagePicker.launch(arrayOf("image/*"))
                    }
                }

                item {
                    Text("Status", style = MaterialTheme.typography.labelLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ChoiceButton("Active", status == "Active") { status = "Active" }
                        ChoiceButton("Inactive", status == "Inactive") { status = "Inactive" }
                        ChoiceButton("Archived", status == "Archived") { status = "Archived" }
                    }
                }

                if (message.isNotBlank()) {
                    item { Text(message, style = MaterialTheme.typography.bodyMedium) }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.weight(1f),
                    enabled = !saving && !photoProcessing && !signatureProcessing
                ) { Text("Cancel") }

                Button(
                    onClick = {
                        if (fullName.isBlank() || position.isBlank() || controlNumber.isBlank()) {
                            message = "Please complete Full Name, Position and ID Number."
                            return@Button
                        }

                        saving = true
                        message = ""
                        scope.launch {
                            runCatching {
                                val record = Employee(
                                    id = employee?.id ?: 0L,
                                    fullName = fullName.trim(),
                                    position = position.trim(),
                                    controlNumber = controlNumber.trim(),
                                    birthdate = birthdate.trim(),
                                    address = address.trim(),
                                    sex = sex.trim(),
                                    civilStatus = civilStatus.trim(),
                                    photoUri = photoUri.ifBlank { null },
                                    signatureUri = signatureUri.ifBlank { null },
                                    qrToken = qrToken.trim().ifBlank { null },
                                    qrImageUri = qrImageUri.ifBlank { null },
                                    status = status
                                )
                                if (employee == null) dao.insert(record) else dao.update(record)
                            }.onSuccess {
                                onSaved()
                            }.onFailure {
                                saving = false
                                message = if ((it.message ?: "").contains("UNIQUE", ignoreCase = true)) {
                                    "That Control / ID Number is already used by another employee."
                                } else {
                                    "Could not save record. Please check the information and try again."
                                }
                            }
                        }
                    },
                    modifier = Modifier.weight(1f),
                    enabled = !saving && !photoProcessing && !signatureProcessing
                ) { Text(if (saving) "Saving…" else "Save Employee") }
            }
        }
    }
}

@Composable
private fun ProcessedAssetCard(
    title: String,
    instruction: String,
    uriString: String,
    processing: Boolean,
    statusText: String,
    onCamera: () -> Unit,
    onGallery: () -> Unit,
    onClear: () -> Unit
) {
    val context = LocalContext.current
    val preview = remember(uriString) {
        if (uriString.isBlank()) null else OfflineImageProcessor.loadPreview(context, uriString)
    }

    Card(Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(instruction, style = MaterialTheme.typography.bodySmall)

            if (preview != null) {
                Image(
                    bitmap = preview.asImageBitmap(),
                    contentDescription = "$title processed preview",
                    modifier = Modifier.fillMaxWidth().height(170.dp),
                    contentScale = ContentScale.Fit
                )
                Text("Processed preview", style = MaterialTheme.typography.labelMedium)
            } else {
                Text("No processed image yet", style = MaterialTheme.typography.bodySmall)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onCamera,
                    enabled = !processing,
                    modifier = Modifier.weight(1f)
                ) { Text(if (processing) "Processing…" else "Camera") }

                OutlinedButton(
                    onClick = onGallery,
                    enabled = !processing,
                    modifier = Modifier.weight(1f)
                ) { Text("Gallery") }
            }

            if (uriString.isNotBlank()) {
                TextButton(onClick = onClear, enabled = !processing) { Text("Clear") }
            }

            if (statusText.isNotBlank()) {
                Text(statusText, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun EmployeeTextField(
    label: String,
    value: String,
    placeholder: String = "",
    singleLine: Boolean = true,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label) },
        placeholder = if (placeholder.isBlank()) null else ({ Text(placeholder) }),
        singleLine = singleLine,
        minLines = if (singleLine) 1 else 2,
        maxLines = if (singleLine) 1 else 3
    )
}

@Composable
private fun ChoiceButton(label: String, selected: Boolean, onClick: () -> Unit) {
    if (selected) {
        Button(onClick = onClick, contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)) { Text(label) }
    } else {
        OutlinedButton(onClick = onClick, contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)) { Text(label) }
    }
}

@Composable
private fun EmployeeAssetRow(label: String, selected: Boolean, onPick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            Text(if (selected) "Saved" else "Not selected", style = MaterialTheme.typography.bodySmall)
        }
        OutlinedButton(onClick = onPick) { Text(if (selected) "Replace" else "Upload") }
    }
}
