package one_block.textura.notify.region

import one_block.textura.hud.anchor.ScreenAnchor

internal enum class StackDirection { DOWN, UP }

internal data class NotificationRegion(
    val id: String,
    val anchor: ScreenAnchor,
    val x: Int,
    val y: Int,
    val maxLines: Int,
    val spacing: Int,
    val background: Boolean,
    val direction: StackDirection,
    val animated: Boolean = false,
)
