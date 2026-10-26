package one_block.textura.notify.region

import one_block.textura.hud.anchor.ScreenAnchor
import one_block.textura.notify.NotificationLine

internal object NotificationAnimation {

    const val PERIOD = 200
    const val SLIDE_LIFE = 100
    private const val DAY = 24000

    fun phase(gameTime: Long): Int = ((gameTime % DAY) % PERIOD).toInt()

    fun lifetime(region: NotificationRegion, line: NotificationLine): NotificationLine =
        if (region.animated && region.anchor != ScreenAnchor.CENTER) line.copy(durationTicks = SLIDE_LIFE) else line
}
