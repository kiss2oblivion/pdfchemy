package com.example.shrinkpdf.ui.textconverter

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.shrinkpdf.logic.TextFormatConverter
import androidx.compose.ui.res.stringResource
import com.example.shrinkpdf.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextConverterScreen(viewModel: TextConverterViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    val inputText by viewModel.inputText.collectAsState()
    val inputFormat by viewModel.inputFormat.collectAsState()
    val outputFormat by viewModel.outputFormat.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val validOutputFormats = viewModel.getValidOutputFormats(inputFormat)

    var showInputDropdown by remember { mutableStateOf(false) }
    var showOutputDropdown by remember { mutableStateOf(false) }

    val pickFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.loadFile(context, uri)
        }
    }

    val saveFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(getMimeType(outputFormat))
    ) { uri ->
        if (uri != null) {
            viewModel.convertAndSave(context, uri)
        }
    }

    LaunchedEffect(uiState) {
        when (val state = uiState) {
            is TextConverterViewModel.UiState.Success -> {
                Toast.makeText(context, state.message, Toast.LENGTH_SHORT).show()
                viewModel.dismissMessage()
            }
            is TextConverterViewModel.UiState.Error -> {
                Toast.makeText(context, state.message, Toast.LENGTH_LONG).show()
                viewModel.dismissMessage()
            }
            else -> {}
        }
    }

    BackHandler { onBack() }

    Scaffold(containerColor = Color.Transparent, 
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.text_format_converter)) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = androidx.compose.ui.graphics.Color.Transparent, scrolledContainerColor = androidx.compose.ui.graphics.Color.Transparent), navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp)) {
                
                // Format Selection Row
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Input Format Dropdown
                    Box(modifier = Modifier.weight(1f)) {
                        OutlinedCard(
                            modifier = Modifier.fillMaxWidth().clickable { showInputDropdown = true }
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("From: ${inputFormat.name}")
                                Icon(Icons.Rounded.ArrowDropDown, contentDescription = null)
                            }
                        }
                        DropdownMenu(
                            expanded = showInputDropdown,
                            onDismissRequest = { showInputDropdown = false }
                        ) {
                            TextFormatConverter.Format.values().forEach { format ->
                                DropdownMenuItem(
                                    text = { Text(format.name) },
                                    onClick = {
                                        viewModel.setInputFormat(format)
                                        showInputDropdown = false
                                    }
                                )
                            }
                        }
                    }

                    Icon(
                        Icons.Rounded.ArrowForward, 
                        contentDescription = "to", 
                        modifier = Modifier.padding(horizontal = 8.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )

                    // Output Format Dropdown
                    Box(modifier = Modifier.weight(1f)) {
                        OutlinedCard(
                            modifier = Modifier.fillMaxWidth().clickable { showOutputDropdown = true }
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("To: ${outputFormat.name}")
                                Icon(Icons.Rounded.ArrowDropDown, contentDescription = null)
                            }
                        }
                        DropdownMenu(
                            expanded = showOutputDropdown,
                            onDismissRequest = { showOutputDropdown = false }
                        ) {
                            validOutputFormats.forEach { format ->
                                DropdownMenuItem(
                                    text = { Text(format.name) },
                                    onClick = {
                                        viewModel.setOutputFormat(format)
                                        showOutputDropdown = false
                                    }
                                )
                            }
                        }
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { viewModel.setInputText(it) },
                        label = { Text("Type or paste ${inputFormat.name} here") },
                        modifier = Modifier.fillMaxSize().padding(8.dp),
                        placeholder = { Text(stringResource(R.string.enter_content)) }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { pickFileLauncher.launch(arrayOf("*/*")) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                    ) {
                        Icon(Icons.Rounded.UploadFile, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.import_btn))
                    }

                    Button(
                        onClick = { 
                            if (inputText.isBlank()) {
                                Toast.makeText(context, "Please enter some text.", Toast.LENGTH_SHORT).show()
                            } else {
                                val filename = com.example.shrinkpdf.logic.FileUtil.generateSuggestedName(null, "converted_text", "Document", outputFormat.name.lowercase())
                                saveFileLauncher.launch(filename)
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Rounded.Save, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Save as ${outputFormat.name}")
                    }
                }
            }

            if (uiState is TextConverterViewModel.UiState.Processing) {
                Box(
                    modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}

fun getMimeType(format: TextFormatConverter.Format): String {
    return when(format) {
        TextFormatConverter.Format.TXT -> "text/plain"
        TextFormatConverter.Format.MD -> "text/markdown"
        TextFormatConverter.Format.HTML -> "text/html"
        TextFormatConverter.Format.CSV -> "text/csv"
        TextFormatConverter.Format.TSV -> "text/tab-separated-values"
        TextFormatConverter.Format.JSON -> "application/json"
        TextFormatConverter.Format.YAML -> "application/x-yaml"
        TextFormatConverter.Format.XML -> "application/xml"
    }
}



