package io.github.kitswas.virtualgamepadmobile.ui.utils

import androidx.compose.runtime.saveable.Saver
import kotlinx.serialization.json.Json

/**
 * Keeps unsaved edits (a layout, a list of custom buttons) across activity recreation,
 * e.g. a dark mode or language change, by storing them as JSON.
 *
 * The default saver would put a map into the Bundle as a Java-serialized object, which
 * fails when saving because the map's values are Parcelable, not Serializable.
 */
inline fun <reified T> jsonSaver(): Saver<T?, String> = Saver(
    save = { value -> value?.let { Json.encodeToString(it) } },
    restore = { Json.decodeFromString<T>(it) },
)
