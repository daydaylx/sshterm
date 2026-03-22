package com.example.privatessh.ui.hostedit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.privatessh.domain.model.AuthType
import com.example.privatessh.domain.model.NetworkTargetType
import com.example.privatessh.presentation.hostedit.HostEditUiState
import com.example.privatessh.presentation.hostedit.HostEditViewModel
import com.example.privatessh.ui.components.InlineLoadingView
import com.example.privatessh.ui.components.SectionHeader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HostEditScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HostEditViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var name by rememberSaveable { mutableStateOf("") }
    var host by rememberSaveable { mutableStateOf("") }
    var port by rememberSaveable { mutableStateOf("22") }
    var user by rememberSaveable { mutableStateOf("") }
    var authType by rememberSaveable { mutableStateOf(AuthType.PASSWORD) }
    var targetType by rememberSaveable { mutableStateOf(NetworkTargetType.DIRECT) }
    var privateKeyPem by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(uiState.hostProfile?.id) {
        name = uiState.getFieldValueOrDefault(HostEditUiState.FIELD_NAME)
        host = uiState.getFieldValueOrDefault(HostEditUiState.FIELD_HOST)
        port = uiState.getFieldValueOrDefault(HostEditUiState.FIELD_PORT)
        user = uiState.getFieldValueOrDefault(HostEditUiState.FIELD_USER)
        authType = uiState.authType
        targetType = uiState.targetType
    }

    LaunchedEffect(uiState.generalError) {
        uiState.generalError?.let { snackbarHostState.showSnackbar(it) }
    }

    LaunchedEffect(uiState.didSave, uiState.didDelete) {
        if (uiState.didSave || uiState.didDelete) {
            onBack()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(if (uiState.isNewHost) "New host" else "Edit host")
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (!uiState.isNewHost) {
                        IconButton(
                            onClick = viewModel::onDelete,
                            enabled = !uiState.isBusy
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete host")
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(paddingValues)
        ) {
            if (uiState.isBusy) {
                InlineLoadingView(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                )
            }

            SectionHeader(text = "Connection", modifier = Modifier.padding(horizontal = 16.dp))
            Field(
                value = name,
                label = "Name",
                error = uiState.getFieldError(HostEditUiState.FIELD_NAME),
                onValueChange = {
                    name = it
                    viewModel.onFieldChange(HostEditUiState.FIELD_NAME, it)
                }
            )
            Field(
                value = host,
                label = "Hostname or IP",
                error = uiState.getFieldError(HostEditUiState.FIELD_HOST),
                onValueChange = {
                    host = it
                    viewModel.onFieldChange(HostEditUiState.FIELD_HOST, it)
                }
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Field(
                    value = port,
                    label = "Port",
                    error = uiState.getFieldError(HostEditUiState.FIELD_PORT),
                    keyboardType = KeyboardType.Number,
                    modifier = Modifier.weight(1f),
                    onValueChange = {
                        port = it
                        viewModel.onFieldChange(HostEditUiState.FIELD_PORT, it)
                    }
                )
            }

            Field(
                value = user,
                label = "Username",
                error = uiState.getFieldError(HostEditUiState.FIELD_USER),
                onValueChange = {
                    user = it
                    viewModel.onFieldChange(HostEditUiState.FIELD_USER, it)
                }
            )

            Spacer(modifier = Modifier.height(8.dp))
            SectionHeader(text = "Target", modifier = Modifier.padding(horizontal = 16.dp))
            ChoiceRow(
                selected = targetType.name,
                onSelect = {
                    targetType = if (it == NetworkTargetType.TAILSCALE.name) {
                        NetworkTargetType.TAILSCALE
                    } else {
                        NetworkTargetType.DIRECT
                    }
                    viewModel.onTargetTypeChange(targetType)
                },
                options = listOf(
                    NetworkTargetType.DIRECT.name to "Direct",
                    NetworkTargetType.TAILSCALE.name to "Tailscale"
                )
            )

            Spacer(modifier = Modifier.height(8.dp))
            SectionHeader(text = "Authentication", modifier = Modifier.padding(horizontal = 16.dp))
            ChoiceRow(
                selected = authType.name,
                onSelect = {
                    authType = AuthType.valueOf(it)
                    viewModel.onAuthTypeChange(authType)
                },
                options = listOf(
                    AuthType.PASSWORD.name to "Password",
                    AuthType.PRIVATE_KEY.name to "Private key"
                )
            )

            if (authType == AuthType.PRIVATE_KEY) {
                OutlinedTextField(
                    value = privateKeyPem,
                    onValueChange = { privateKeyPem = it },
                    label = { Text("Private key (PEM)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    minLines = 8
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    viewModel.onSave(
                        name = name,
                        host = host,
                        port = port,
                        user = user,
                        authType = authType,
                        targetType = targetType,
                        privateKeyPem = privateKeyPem
                    )
                },
                enabled = !uiState.isBusy,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Text("Save host")
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun Field(
    value: String,
    label: String,
    error: String?,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        isError = error != null,
        supportingText = {
            if (error != null) {
                Text(error)
            }
        },
        singleLine = keyboardType != KeyboardType.Text,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

@Composable
private fun ChoiceRow(
    selected: String,
    onSelect: (String) -> Unit,
    options: List<Pair<String, String>>
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEach { (value, label) ->
            val selectedOption = value == selected
            if (selectedOption) {
                Button(onClick = { onSelect(value) }, modifier = Modifier.weight(1f)) {
                    Text(label)
                }
            } else {
                OutlinedButton(onClick = { onSelect(value) }, modifier = Modifier.weight(1f)) {
                    Text(label)
                }
            }
        }
    }
}
