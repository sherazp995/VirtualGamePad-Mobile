package io.github.kitswas.virtualgamepadmobile.data

import androidx.annotation.StringRes
import io.github.kitswas.VGP_Data_Exchange.GameButtons
import io.github.kitswas.virtualgamepadmobile.R
import kotlinx.serialization.Serializable
import java.util.UUID

/** Everything a custom button can press. Triggers are analog, the rest are buttons. */
@Serializable
enum class ComboInput(@StringRes val nameRes: Int, val button: GameButtons?) {
    A(R.string.button_a, GameButtons.A),
    B(R.string.button_b, GameButtons.B),
    X(R.string.button_x, GameButtons.X),
    Y(R.string.button_y, GameButtons.Y),
    DPAD_UP(R.string.combo_dpad_up, GameButtons.DPadUp),
    DPAD_DOWN(R.string.combo_dpad_down, GameButtons.DPadDown),
    DPAD_LEFT(R.string.combo_dpad_left, GameButtons.DPadLeft),
    DPAD_RIGHT(R.string.combo_dpad_right, GameButtons.DPadRight),
    LEFT_SHOULDER(R.string.combo_lb, GameButtons.LeftShoulder),
    RIGHT_SHOULDER(R.string.combo_rb, GameButtons.RightShoulder),
    LEFT_TRIGGER(R.string.button_lt, null),
    RIGHT_TRIGGER(R.string.button_rt, null),
    VIEW(R.string.combo_view, GameButtons.View),
    MENU(R.string.combo_menu, GameButtons.Menu),
    LEFT_STICK(R.string.combo_left_stick, GameButtons.LeftThumbstick),
    RIGHT_STICK(R.string.combo_right_stick, GameButtons.RightThumbstick),
}

/** Custom buttons are numbered, and there are at most nine. */
val CUSTOM_BUTTON_NUMBERS = 1..9

/**
 * An extra on-screen button the user adds, shown as its [number]. Holding it presses
 * every one of its [inputs] at once, e.g. A and B together.
 */
@Serializable
data class CustomButton(
    val number: Int,
    val inputs: Set<ComboInput>,
    val placement: Placement = Placement(),
    val id: String = UUID.randomUUID().toString(),
)

/** The lowest number not taken yet, or null when all nine are in use. */
fun List<CustomButton>.nextFreeNumber(): Int? =
    CUSTOM_BUTTON_NUMBERS.firstOrNull { number -> none { it.number == number } }

/** Everything this set of combo inputs presses, as one [PadInput]. */
fun Set<ComboInput>.toPadInput() = PadInput(
    buttons = fold(0) { mask, input -> mask or (input.button?.value ?: 0) },
    leftTrigger = ComboInput.LEFT_TRIGGER in this,
    rightTrigger = ComboInput.RIGHT_TRIGGER in this,
)
