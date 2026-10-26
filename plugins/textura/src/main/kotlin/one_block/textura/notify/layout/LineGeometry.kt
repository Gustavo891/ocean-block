package one_block.textura.notify.layout

import one_block.textura.hud.anchor.AnchorMarker
import one_block.textura.notify.region.NotificationRegion
import one_block.textura.notify.region.StackDirection

internal data class LineSlot(val textTop: Int, val lift: Int)

internal data class LineSpan(val boxLeft: Int, val boxWidth: Int, val contentLeft: Int)

internal object LineGeometry {

    const val BASE_ASCENT = 7
    const val LINE_HEIGHT = 13
    const val TEXT_TOP_PADDING = 3
    const val LIFT_STEP = 12
    const val ICON_SIZE = 8
    const val ICON_GAP = 2
    const val ICON_BOX = ICON_SIZE + ICON_GAP
    const val PADDING_X = 5
    const val BACKDROP_TEXT_X = 28
    const val BACKDROP_TEXT_TOP = 2

    fun pitch(region: NotificationRegion): Int = LINE_HEIGHT + region.spacing

    fun offset(region: NotificationRegion, index: Int): Int = when (region.direction) {
        StackDirection.DOWN -> region.y + index * pitch(region)
        StackDirection.UP -> region.y - index * pitch(region)
    }

    fun slot(region: NotificationRegion, index: Int): LineSlot? {
        val top = offset(region, index)
        if (top >= 0) return LineSlot(top, 0)
        val lift = (-top + LIFT_STEP - 1) / LIFT_STEP
        if (lift > AnchorMarker.MAX_LIFT) return null
        return LineSlot(top + lift * LIFT_STEP, lift)
    }

    fun fittingLines(region: NotificationRegion): Int =
        (0 until region.maxLines).takeWhile { slot(region, it) != null }.count()

    fun textTops(regions: Collection<NotificationRegion>): Set<Int> =
        regions.flatMap { region -> (0 until region.maxLines).mapNotNull { slot(region, it)?.textTop } }.toSortedSet()

    fun contentWidth(textWidth: Int, hasIcon: Boolean): Int = (if (hasIcon) ICON_BOX else 0) + textWidth

    fun span(region: NotificationRegion, contentWidth: Int): LineSpan {
        val padding = if (region.background) PADDING_X else 0
        val boxWidth = contentWidth + 2 * padding
        val boxLeft = if (region.anchor.alignsRight) region.x - boxWidth else region.x
        return LineSpan(boxLeft, boxWidth, boxLeft + padding)
    }

    fun textAscent(providerAscent: Int, textTop: Int): Int = providerAscent - textTop

    fun iconAscent(textTop: Int): Int = BASE_ASCENT - textTop

    fun plaqueAscent(textTop: Int): Int = BASE_ASCENT + TEXT_TOP_PADDING - textTop
}
