package one_block.textura.hud.anchor

enum class ScreenAnchor(val id: Int, val alignsRight: Boolean, val isBottom: Boolean) {
    TOP_LEFT(1, false, false),
    TOP_RIGHT(2, true, false),
    MIDDLE_LEFT(3, false, false),
    MIDDLE_RIGHT(4, true, false),
    BOTTOM_LEFT(5, false, true),
    BOTTOM_RIGHT(6, true, true),
}
