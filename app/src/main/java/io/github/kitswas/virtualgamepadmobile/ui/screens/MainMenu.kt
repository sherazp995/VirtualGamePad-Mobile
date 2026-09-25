package io.github.kitswas.virtualgamepadmobile.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.kitswas.virtualgamepadmobile.R
import io.github.kitswas.virtualgamepadmobile.data.PreviewBase
import io.github.kitswas.virtualgamepadmobile.data.PreviewHeightDp
import io.github.kitswas.virtualgamepadmobile.data.PreviewWidthDp
import io.github.kitswas.virtualgamepadmobile.data.SavedConnection
import io.github.kitswas.virtualgamepadmobile.ui.composables.SidebarItem

/**
 * The landing page: the app's sections on the left, saved connections on the right
 * so a known PC is one tap away.
 */
@Composable
fun MainMenu(
    savedConnections: List<SavedConnection>,
    onConnect: (SavedConnection) -> Unit,
    onDeleteConnection: (SavedConnection) -> Unit,
    onNavigateToConnectScreen: () -> Unit,
    onNavigateToSettingsScreen: () -> Unit,
    onNavigateToAboutScreen: () -> Unit,
    onExit: () -> Unit
) {
    Scaffold { paddingValues ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .width(240.dp)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.surfaceContainerLow)
                    .padding(vertical = 8.dp),
            ) {
                Text(
                    stringResource(R.string.home_title),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                )
                SidebarItem(
                    label = stringResource(R.string.menu_start),
                    emphasized = true,
                    onClick = onNavigateToConnectScreen,
                )
                SidebarItem(stringResource(R.string.menu_settings), onNavigateToSettingsScreen)
                SidebarItem(stringResource(R.string.menu_about), onNavigateToAboutScreen)
                Box(modifier = Modifier.weight(1f))
                SidebarItem(stringResource(R.string.menu_exit), onExit)
            }
            VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.home_saved_connections),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceContainer)
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.primary)
                if (savedConnections.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            stringResource(R.string.home_no_saved_connections),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                    }
                } else {
                    LazyColumn {
                        items(savedConnections, key = { it.id }) { connection ->
                            SavedConnectionRow(
                                connection = connection,
                                onConnect = { onConnect(connection) },
                                onDelete = { onDeleteConnection(connection) },
                            )
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SavedConnectionRow(
    connection: SavedConnection,
    onConnect: () -> Unit,
    onDelete: () -> Unit,
) {
    ListItem(
        modifier = Modifier.clickable(onClick = onConnect),
        headlineContent = { Text(connection.name, style = MaterialTheme.typography.bodyLarge) },
        supportingContent = {
            Text(
                "${connection.ipAddress}:${connection.port}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        trailingContent = {
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = stringResource(R.string.home_delete_connection, connection.name),
                )
            }
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
    )
}

@Preview(
    widthDp = PreviewWidthDp,
    heightDp = PreviewHeightDp,
)
@Composable
fun MainMenuPreview() {
    PreviewBase {
        MainMenu(
            savedConnections = listOf(
                SavedConnection("Desktop", "192.168.1.20", "7878"),
                SavedConnection("Laptop", "192.168.1.31", "7878"),
            ),
            onConnect = {},
            onDeleteConnection = {},
            onNavigateToConnectScreen = {},
            onNavigateToSettingsScreen = {},
            onNavigateToAboutScreen = {},
            onExit = {}
        )
    }
}
