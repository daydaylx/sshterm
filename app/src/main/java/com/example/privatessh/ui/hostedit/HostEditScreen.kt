package com.example.privatessh.ui.hostedit

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.privatessh.R
import com.example.privatessh.domain.model.AuthType
import com.example.privatessh.domain.model.NetworkTargetType
import com.example.privatessh.presentation.hostedit.HostEditUiState
import com.example.privatessh.presentation.hostedit.HostEditViewModel
import com.example.privatessh.ui.components.AppPanel
import com.example.privatessh.ui.components.AppScreenScaffold
import com.example.privatessh.ui.components.HeroPanel
import com.example.privatessh.ui.components.InlineLoadingView
import com.example.privatessh.ui.components.SectionHeader
import com.example.privatessh.ui.components.SectionIntro
import com.example.privatessh.ui.theme.AppTheme

@Composable
fun HostEditScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HostEditViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    var name by rememberSaveable { mutableStateOf("") }
    var host by rememberSaveable { mutableStateOf("") }
    var port by rememberSaveable { mutableStateOf("22") }
    var user by rememberSaveable { mutableStateOf("") }
    var authType by rememberSaveable { mutableStateOf(AuthType.PASSWORD) }
    var targetType by rememberSaveable { mutableStateOf(NetworkTargetType.DIRECT) }
    var privateKeyPem by rememberSaveable { mutableStateOf("") }

    var sectionConnectionExpanded by rememberSaveable { mutableStateOf(true) }
    var sectionAuthExpanded by rememberSaveable { mutableStateOf(true) }
    var sectionTargetExpanded by rememberSaveable { mutableStateOf(false) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            context.contentResolver.openInputStream(it)?.use { stream ->
                privateKeyPem = stream.bufferedReader().readText()
            }
        }
    }

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

    AppScreenScaffold(
        title = stringResource(
            if (uiState.isNewHost) {
                R.string.host_edit_title_new
            } else {
                R.string.host_edit_title_edit
            }
        ),
        subtitle = stringResource(R.string.host_edit_subtitle),
        onNavigateBack = onBack,
        modifier = modifier.fillMaxSize(),
        actions = {
            if (!uiState.isNewHost) {
                FilledTonalIconButton(
                    onClick = viewModel::onDelete,
                    enabled = !uiState.isBusy
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = stringResource(R.string.host_edit_delete)
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(paddingValues)
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            HeroPanel {
                SectionIntro(
                    eyebrow = stringResource(R.string.host_edit_hero_eyebrow),
                    title = stringResource(
                        if (uiState.isNewHost) {
                            R.string.host_edit_hero_title_new
                        } else {
                            R.string.host_edit_hero_title_edit
                        }
                    ),
                    supportingText = stringResource(R.string.host_edit_hero_body)
                )
            }

            if (uiState.isBusy) {
                AppPanel {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = stringResource(R.string.host_edit_working),
                            style = MaterialTheme.typography.titleSmall
                        )
                        InlineLoadingView()
                    }
                }
            }

            ExpandableSection(
                title = stringResource(R.string.host_edit_connection_section),
                isExpanded = sectionConnectionExpanded,
                onToggle = { sectionConnectionExpanded = !sectionConnectionExpanded }
            ) {
                Text(
                    text = stringResource(R.string.host_edit_connection_body),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Field(
                    value = name,
                    label = stringResource(R.string.host_edit_name),
                    error = uiState.getFieldError(HostEditUiState.FIELD_NAME),
                    onValueChange = {
                        name = it
                        viewModel.onFieldChange(HostEditUiState.FIELD_NAME, it)
                    }
                )
                Field(
                    value = host,
                    label = stringResource(R.string.host_edit_host),
                    error = uiState.getFieldError(HostEditUiState.FIELD_HOST),
                    onValueChange = {
                        host = it
                        viewModel.onFieldChange(HostEditUiState.FIELD_HOST, it)
                    }
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Field(
                        value = port,
                        label = stringResource(R.string.host_edit_port),
                        error = uiState.getFieldError(HostEditUiState.FIELD_PORT),
                        keyboardType = KeyboardType.Number,
                        modifier = Modifier.weight(0.35f),
                        onValueChange = {
                            port = it
                            viewModel.onFieldChange(HostEditUiState.FIELD_PORT, it)
                        }
                    )
                    Field(
                        value = user,
                        label = stringResource(R.string.host_edit_user),
                        error = uiState.getFieldError(HostEditUiState.FIELD_USER),
                        modifier = Modifier.weight(0.65f),
                        onValueChange = {
                            user = it
                            viewModel.onFieldChange(HostEditUiState.FIELD_USER, it)
                        }
                    )
                }
            }

            ExpandableSection(
                title = stringResource(R.string.host_edit_auth_section),
                isExpanded = sectionAuthExpanded,
                onToggle = { sectionAuthExpanded = !sectionAuthExpanded }
            ) {
                SegmentedChoiceRow(
                    selected = authType.name,
                    onSelect = {
                        authType = AuthType.valueOf(it)
                        viewModel.onAuthTypeChange(authType)
                    },
                    options = listOf(
                        AuthType.PASSWORD.name to stringResource(R.string.host_edit_auth_password),
                        AuthType.PRIVATE_KEY.name to stringResource(R.string.host_edit_auth_private_key)
                    ),
                    icons = mapOf(
                        AuthType.PASSWORD.name to Icons.Default.Lock,
                        AuthType.PRIVATE_KEY.name to Icons.Default.Key
                    )
                )

                if (authType == AuthType.PRIVATE_KEY) {
                    if (uiState.hasStoredPrivateKey && privateKeyPem.isBlank()) {
                        AppPanel(
                            emphasized = true,
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.host_edit_private_key_stored),
                                style = MaterialTheme.typography.titleSmall,
                                color = AppTheme.success
                            )
                            Text(
                                text = stringResource(R.string.host_edit_private_key_stored_body),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            TextButton(
                                onClick = viewModel::onClearPrivateKey,
                                enabled = !uiState.isBusy
                            ) {
                                Text(stringResource(R.string.host_edit_private_key_clear))
                            }
                        }
                    }

                    if (!uiState.hasStoredPrivateKey || privateKeyPem.isNotBlank()) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = privateKeyPem,
                                onValueChange = { privateKeyPem = it },
                                label = {
                                    Text(
                                        stringResource(
                                            if (uiState.hasStoredPrivateKey) {
                                                R.string.host_edit_private_key_replace
                                            } else {
                                                R.string.host_edit_private_key_label
                                            }
                                        )
                                    )
                                },
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 5,
                                maxLines = 10
                            )
                            
                            OutlinedButton(
                                onClick = { filePickerLauncher.launch("*/*") },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = !uiState.isBusy
                            ) {
                                Icon(Icons.Default.FolderOpen, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Schlüsseldatei laden")
                            }
                            
                            Text(
                                text = stringResource(R.string.host_edit_private_key_support),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            ExpandableSection(
                title = stringResource(R.string.host_edit_target_section),
                isExpanded = sectionTargetExpanded,
                onToggle = { sectionTargetExpanded = !sectionTargetExpanded }
            ) {
                SegmentedChoiceRow(
                    selected = targetType.name,
                    onSelect = {
                        targetType = NetworkTargetType.valueOf(it)
                        viewModel.onTargetTypeChange(targetType)
                    },
                    options = listOf(
                        NetworkTargetType.DIRECT.name to stringResource(R.string.host_edit_target_direct),
                        NetworkTargetType.TAILSCALE.name to stringResource(R.string.host_edit_target_tailscale)
                    )
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onBack,
                    enabled = !uiState.isBusy,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.host_edit_cancel))
                }
                FilledTonalButton(
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
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.host_edit_save))
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ExpandableSection(
    title: String,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggle() }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null
                )
            }
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    content = content
                )
            }
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
                Text(text = error, color = MaterialTheme.colorScheme.error)
            }
        },
        singleLine = keyboardType != KeyboardType.Text,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        modifier = modifier.fillMaxWidth()
    )
}

@Composable
private fun SegmentedChoiceRow(
    selected: String,
    onSelect: (String) -> Unit,
    options: List<Pair<String, String>>,
    icons: Map<String, androidx.compose.ui.graphics.vector.ImageVector> = emptyMap()
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEach { (value, label) ->
            FilterChip(
                selected = value == selected,
                onClick = { onSelect(value) },
                label = { Text(label) },
                leadingIcon = icons[value]?.let { icon ->
                    {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            modifier = Modifier.height(18.dp)
                        )
                    }
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                ),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun HostMetricPill(
    label: String,
    value: String,
    accent: androidx.compose.ui.graphics.Color
) {
    Surface(
        color = accent.copy(alpha = 0.12f),
        contentColor = accent,
        shape = MaterialTheme.shapes.small,
        border = androidx.compose.foundation.BorderStroke(1.dp, accent.copy(alpha = 0.24f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = label.uppercase(),
                style = MaterialTheme.typography.labelSmall
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall
            )
        }
    }
}
