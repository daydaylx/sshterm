package com.example.privatessh.ui.hostlist

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxState
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.privatessh.R
import com.example.privatessh.domain.model.AuthType
import com.example.privatessh.domain.model.HostProfile
import com.example.privatessh.ui.components.StatusChip
import com.example.privatessh.ui.theme.AppTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch

/**
 * Modern Material 3 card for a host profile with swipe-to-action support.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HostCard(
    host: HostProfile,
    onConnect: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    isDeleting: Boolean = false
) {
    val scope = rememberCoroutineScope()
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            when (value) {
                SwipeToDismissBoxValue.StartToEnd -> {
                    // StartToEnd = Right swipe -> Connect
                    onConnect()
                    // Returning false ensures the card snaps back to settled state
                    false
                }
                SwipeToDismissBoxValue.EndToStart -> {
                    // EndToStart = Left swipe -> Delete
                    onDelete()
                    // Don't auto-dismiss yet, confirmation dialog will handle it
                    false
                }
                else -> false
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        modifier = modifier.padding(horizontal = 20.dp),
        backgroundContent = { SwipeBackground(dismissState) },
        enableDismissFromStartToEnd = true,
        enableDismissFromEndToStart = true
    ) {
        OutlinedCard(
            modifier = Modifier.fillMaxWidth(),
            onClick = onEdit, // Default click opens the editor
            colors = CardDefaults.outlinedCardColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                contentColor = MaterialTheme.colorScheme.onSurface
            ),
            border = androidx.compose.foundation.BorderStroke(
                width = if (isDeleting) 2.dp else 1.dp,
                color = if (isDeleting) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outlineVariant
            ),
            elevation = CardDefaults.outlinedCardElevation(defaultElevation = 2.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Host Icon Box
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            color = if (host.isTailscale()) MaterialTheme.colorScheme.secondaryContainer 
                                   else MaterialTheme.colorScheme.primaryContainer,
                            shape = MaterialTheme.shapes.medium
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Terminal,
                        contentDescription = null,
                        tint = if (host.isTailscale()) MaterialTheme.colorScheme.onSecondaryContainer 
                               else MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = host.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        if (host.isTailscale()) {
                            StatusChip(
                                text = "TS",
                                color = AppTheme.info
                            )
                        }
                    }
                    Text(
                        text = host.getHostWithPort(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = host.lastConnectedAt?.let { formatTimestamp(it) }
                            ?: stringResource(R.string.host_card_never_connected),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }

                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = if (host.authType == AuthType.PASSWORD) Icons.Default.Lock else Icons.Default.Key,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.secondary.copy(alpha = 0.6f)
                    )
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.outlineVariant
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeBackground(state: SwipeToDismissBoxState) {
    val direction = state.dismissDirection ?: return

    val color by animateColorAsState(
        when (direction) {
            SwipeToDismissBoxValue.StartToEnd -> AppTheme.success // Connect color
            SwipeToDismissBoxValue.EndToStart -> AppTheme.danger // Delete color
            else -> Color.Transparent
        }, label = "swipeColor"
    )

    val alignment = when (direction) {
        SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
        SwipeToDismissBoxValue.EndToStart -> Alignment.CenterEnd
        else -> Alignment.Center
    }

    val icon = when (direction) {
        SwipeToDismissBoxValue.StartToEnd -> Icons.Default.PlayArrow
        SwipeToDismissBoxValue.EndToStart -> Icons.Default.Delete
        else -> Icons.Default.Delete
    }

    val label = when (direction) {
        SwipeToDismissBoxValue.StartToEnd -> stringResource(R.string.swipe_connect)
        SwipeToDismissBoxValue.EndToStart -> stringResource(R.string.swipe_delete)
        else -> ""
    }

    val scale by animateFloatAsState(
        if (state.targetValue == SwipeToDismissBoxValue.Settled) 0.75f else 1f, label = "scale"
    )

    Box(
        Modifier
            .fillMaxSize()
            .background(color, shape = MaterialTheme.shapes.medium)
            .padding(horizontal = 20.dp),
        contentAlignment = alignment
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.scale(scale)
        ) {
            if (direction == SwipeToDismissBoxValue.StartToEnd) {
                Icon(icon, contentDescription = null, tint = Color.White)
                Text(label, color = Color.White, style = MaterialTheme.typography.titleSmall)
            } else {
                Text(label, color = Color.White, style = MaterialTheme.typography.titleSmall)
                Icon(icon, contentDescription = null, tint = Color.White)
            }
        }
    }
}

@Composable
private fun formatTimestamp(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp

    return when {
        diff < 60_000 -> stringResource(R.string.host_card_just_now)
        diff < 3600_000 -> stringResource(R.string.host_card_minutes_ago, diff / 60_000)
        diff < 86400_000 -> stringResource(R.string.host_card_hours_ago, diff / 3600_000)
        diff < 604800_000 -> stringResource(R.string.host_card_days_ago, diff / 86400_000)
        else -> SimpleDateFormat(
            stringResource(R.string.host_card_date_format),
            Locale.getDefault()
        ).format(Date(timestamp))
    }
}
