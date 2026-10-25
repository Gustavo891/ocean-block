package one_block.textura.notify.region

import one_block.textura.hud.anchor.AnchorMarker

internal object NotificationAnimation {

    fun frame(age: Long, duration: Int): Int {
        val hold = duration / 2
        val fade = duration - hold
        if (age < hold || fade <= 0) return 0
        return minOf(AnchorMarker.MAX_LIFT.toLong(), 1 + (age - hold) * AnchorMarker.MAX_LIFT / fade).toInt()
    }
}
