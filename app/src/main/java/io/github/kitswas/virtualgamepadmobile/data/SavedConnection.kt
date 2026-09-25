package io.github.kitswas.virtualgamepadmobile.data

import kotlinx.serialization.Serializable
import java.util.UUID

/** A PC the user connects to often, saved under a name of their choosing. */
@Serializable
data class SavedConnection(
    val name: String,
    val ipAddress: String,
    val port: String,
    val id: String = UUID.randomUUID().toString(),
)
