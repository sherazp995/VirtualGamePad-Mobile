package io.github.kitswas.virtualgamepadmobile.ui.composables

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.kitswas.VGP_Data_Exchange.GamepadReading
import io.github.kitswas.virtualgamepadmobile.data.ButtonComponent
import io.github.kitswas.virtualgamepadmobile.data.ButtonConfig
import io.github.kitswas.virtualgamepadmobile.data.CustomButton
import io.github.kitswas.virtualgamepadmobile.data.LayoutItem

/** Used only when the screen reports no contact size: about a fingertip. */
private val fallbackContactRadius = 20.dp

/** Largest contact counted, so a palm resting on the screen doesn't press everything. */
private val maxContactRadius = 48.dp

/**
 * @param customButtons the user's numbered combo buttons, drawn on top of the built-in controls
 * @param itemModifier extra modifier for each control, after its placement.
 * The visual layout editor uses it to highlight and measure controls.
 * @param showHidden draw hidden controls faded, so the layout editor can show them again.
 */
@Composable
fun DrawGamepad(
    widthDp: Int,
    heightDp: Int,
    gamepadState: GamepadReading,
    buttonConfigs: Map<ButtonComponent, ButtonConfig>,
    customButtons: List<CustomButton> = emptyList(),
    itemModifier: (LayoutItem) -> Modifier = { Modifier },
    showHidden: Boolean = false,
) {
    // Assuming Landscape orientation
    val baseDp = heightDp

    val deadZonePadding = baseDp / 18

    // Helper function to get config for a component
    fun getConfig(component: ButtonComponent) =
        buttonConfigs[component] ?: ButtonConfig.default(component)

    // Helper function to render a button component at its configured position
    @Composable
    fun RenderComponent(
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

    // First we make a box that will contain the gamepad
    // And put padding around it so that it doesn't touch the edges of the screen
    val view = LocalView.current
    val router = remember(gamepadState) { TouchRouter(gamepadState, view) }
    // Leaving the screen mid-press must not leave buttons stuck down on the PC.
    DisposableEffect(router) {
        onDispose { router.releaseAll() }
    }

    Surface {
        CompositionLocalProvider(LocalTouchRouter provides router) {
        // Every finger on the gamepad goes through the router, so one touch can press
        // buttons of different controls. Sticks consume their own drags and are skipped.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .onGloballyPositioned { router.coordinates = it }
                .pointerInput(router) {
                    val fallbackRadius = fallbackContactRadius.toPx()
                    val maxSemiAxis = maxContactRadius.toPx()
                    val claims = PointerClaims()
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            val contacts = event.changes
                                .filter { claims.isFree(it.id.value, it.pressed, it.isConsumed) }
                                .map {
                                    contactAt(it.position, event, router.coordinates, maxSemiAxis)
                                        ?: Contact(it.position, fallbackRadius, fallbackRadius)
                                }
                            router.onContacts(contacts)
                        }
                    }
                }
        ) {
        Box(
            modifier = Modifier
                .padding(deadZonePadding.dp)
                .fillMaxSize()
        ) {
            // Left Analog Stick
            RenderComponent(ButtonComponent.LEFT_ANALOG_STICK) { config, placement ->
                AnalogStick(
                    modifier = placement,
                    outerCircleWidth = (baseDp / 8 * config.scale).dp,
                    innerCircleRadius = (baseDp / 12 * config.scale).dp,
                    rotation = config.rotation,
                    gamepadState = gamepadState,
                    type = AnalogStickType.LEFT,
                )
            }

            // Right Analog Stick
            RenderComponent(ButtonComponent.RIGHT_ANALOG_STICK) { config, placement ->
                AnalogStick(
                    modifier = placement,
                    outerCircleWidth = (baseDp / 8 * config.scale).dp,
                    innerCircleRadius = (baseDp / 12 * config.scale).dp,
                    rotation = config.rotation,
                    gamepadState = gamepadState,
                    type = AnalogStickType.RIGHT,
                )
            }

            // D-Pad
            RenderComponent(ButtonComponent.DPAD) { config, placement ->
                Dpad(
                    modifier = placement,
                    size = (0.45 * baseDp * config.scale).dp,
                    rotation = config.rotation,
                )
            }

            // Face Buttons
            RenderComponent(ButtonComponent.FACE_BUTTONS) { config, placement ->
                FaceButtons(
                    modifier = placement,
                    size = (0.45 * baseDp * config.scale).dp,
                )
            }

            // Left Trigger
            RenderComponent(ButtonComponent.LEFT_TRIGGER) { config, placement ->
                Trigger(
                    modifier = placement,
                    type = TriggerType.LEFT,
                    size = (baseDp / 6 * config.scale).dp,
                )
            }

            // Right Trigger
            RenderComponent(ButtonComponent.RIGHT_TRIGGER) { config, placement ->
                Trigger(
                    modifier = placement,
                    type = TriggerType.RIGHT,
                    size = (baseDp / 6 * config.scale).dp,
                )
            }

            // Central Buttons (LB, RB, Select, Start)
            CentralButtons(
                baseDp = baseDp,
                buttonConfigs = buttonConfigs,
                itemModifier = itemModifier,
                showHidden = showHidden,
            )

            customButtons.forEach { button ->
                Placed(
                    placement = button.placement,
                    baseDp = baseDp,
                    showHidden = showHidden,
                    itemModifier = itemModifier(LayoutItem.Custom(button.id)),
                ) { placement ->
                    ComboButton(
                        button = button,
                        modifier = placement,
                        size = (baseDp / 7 * button.placement.scale).dp,
                    )
                }
            }
        }
        }
        }
    }
}
