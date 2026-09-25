package io.github.kitswas.virtualgamepadmobile.ui.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

/**
 * One full-width entry in a left sidebar.
 *
 * @param selected pass it for a tab (a category or a mode); the selected one is filled
 * with the theme colour. Leave it null for an action, which is announced as a button.
 * @param emphasized fill an action with the theme colour, e.g. the screen's main action
 */
@Composable
fun SidebarItem(
    label: String,
    onClick: () -> Unit,
    selected: Boolean? = null,
    enabled: Boolean = true,
    emphasized: Boolean = false,
) {
    val filled = selected == true || emphasized
    val interaction = if (selected != null) {
        Modifier.selectable(selected = selected, enabled = enabled, role = Role.Tab, onClick = onClick)
    } else {
        Modifier.clickable(enabled = enabled, role = Role.Button, onClick = onClick)
    }
    Text(
        label,
        style = MaterialTheme.typography.bodyLarge,
        color = when {
            !enabled -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
            filled -> MaterialTheme.colorScheme.onPrimaryContainer
            else -> MaterialTheme.colorScheme.onSurface
        },
        modifier = Modifier
            .fillMaxWidth()
            .then(interaction)
            .background(if (filled) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
            .padding(horizontal = 20.dp, vertical = 12.dp),
    )
}
