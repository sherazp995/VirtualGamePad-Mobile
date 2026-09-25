package io.github.kitswas.virtualgamepadmobile.ui.composables

import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.kitswas.VGP_Data_Exchange.GameButtons
import io.github.kitswas.virtualgamepadmobile.R
import io.github.kitswas.virtualgamepadmobile.data.PadInput
import io.github.kitswas.virtualgamepadmobile.data.ButtonComponent
import io.github.kitswas.virtualgamepadmobile.data.ButtonConfig
import io.github.kitswas.virtualgamepadmobile.ui.theme.gamepadStyle
import io.github.kitswas.virtualgamepadmobile.ui.theme.gamepadSurface
import io.github.kitswas.virtualgamepadmobile.data.LayoutItem

enum class ShoulderButtonType {
    LEFT, RIGHT
}

enum class MenuButtonType {
    VIEW, MENU
}

@Composable
fun ShoulderButton(
    type: ShoulderButtonType,
    modifier: Modifier = Modifier,
    size: Dp,
) {
    val gameButton = when (type) {
        ShoulderButtonType.LEFT -> GameButtons.LeftShoulder
        ShoulderButtonType.RIGHT -> GameButtons.RightShoulder
    }
    val text = when (type) {
        ShoulderButtonType.LEFT -> stringResource(R.string.button_l_shoulder)
        ShoulderButtonType.RIGHT -> stringResource(R.string.button_r_shoulder)
    }

    val key = remember { Any() }
    val isPressed = isHeld(key)

    val style = gamepadStyle()
    Box(
        modifier = modifier
            .height(size)
            .width(size * 1.8f)
            .buttonTarget(key, PadInput(buttons = gameButton.value), round = false)
            .semantics { role = Role.Button }
            .gamepadSurface(RoundedCornerShape(percent = 50), isPressed, style),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = style.glyph, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun MenuButton(
    type: MenuButtonType,
    modifier: Modifier = Modifier,
    size: Dp,
) {
    val gameButton = when (type) {
        MenuButtonType.VIEW -> GameButtons.View
        MenuButtonType.MENU -> GameButtons.Menu
    }
    val iconPainter = when (type) {
        MenuButtonType.VIEW -> null
        MenuButtonType.MENU -> painterResource(R.drawable.ic_menu)
    }

    val key = remember { Any() }
    val isPressed = isHeld(key)

    val style = gamepadStyle()
    Box(
        modifier = modifier
            .size(size)
            .buttonTarget(key, PadInput(buttons = gameButton.value), round = true)
            .semantics { role = Role.Button }
            .gamepadSurface(CircleShape, isPressed, style),
        contentAlignment = Alignment.Center,
    ) {
        if (iconPainter != null) {
            Icon(
                painter = iconPainter,
                contentDescription = stringResource(R.string.content_desc_button, gameButton.name),
                modifier = Modifier.size(size / 2),
                tint = style.glyph
            )
        } else {
            Icon(
                imageVector = ImageVector.vectorResource(R.drawable.screenicon),
                contentDescription = stringResource(R.string.content_desc_button, gameButton.name),
                modifier = Modifier.size(size / 2),
                tint = style.glyph
            )
        }
    }
}

/**
 * The central buttons section containing shoulder buttons (L/R bumpers) and menu buttons (View/Menu) for the gamepad.
 */
@Composable
fun CentralButtons(
    baseDp: Int,
    buttonConfigs: Map<ButtonComponent, ButtonConfig>,
    itemModifier: (LayoutItem) -> Modifier = { Modifier },
    showHidden: Boolean = false,
) {
    // Helper function to get config for a component
    fun getConfig(component: ButtonComponent) =
        buttonConfigs[component] ?: ButtonConfig.default(component)

    // Helper function to render a button component at its configured position
    @Composable
    fun RenderButton(
        component: ButtonComponent,
        content: @Composable (config: ButtonConfig, placement: Modifier) -> Unit
    ) {
        val config = getConfig(component)
        Placed(
            placement = config.placement,
            baseDp = baseDp,
            showHidden = showHidden,
            itemModifier = itemModifier(LayoutItem.BuiltIn(component)),
        ) { placement -> content(config, placement) }
    }

    // Left Shoulder button
    RenderButton(ButtonComponent.LEFT_SHOULDER) { config, placement ->
        ShoulderButton(
            type = ShoulderButtonType.LEFT,
            modifier = placement,
            size = (baseDp / 8 * config.scale).dp,
        )
    }

    // Right Shoulder button
    RenderButton(ButtonComponent.RIGHT_SHOULDER) { config, placement ->
        ShoulderButton(
            type = ShoulderButtonType.RIGHT,
            modifier = placement,
            size = (baseDp / 8 * config.scale).dp,
        )
    }

    // Select button (View)
    RenderButton(ButtonComponent.SELECT_BUTTON) { config, placement ->
        MenuButton(
            type = MenuButtonType.VIEW,
            modifier = placement,
            size = (baseDp / 8 * config.scale).dp,
        )
    }

    // Start button (Menu)
    RenderButton(ButtonComponent.START_BUTTON) { config, placement ->
        MenuButton(
            type = MenuButtonType.MENU,
            modifier = placement,
            size = (baseDp / 8 * config.scale).dp,
        )
    }
}

@Preview(showBackground = false)
@Composable
fun CentralButtonsPreview() {
    CentralButtons(
        baseDp = 400,
        buttonConfigs = io.github.kitswas.virtualgamepadmobile.data.defaultButtonConfigs,
    )
}
