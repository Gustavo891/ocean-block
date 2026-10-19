package one_block.textura.hud.anchor

enum class ScreenAnchor(val id: Int, val alignsRight: Boolean) {
    TOP_LEFT(1, false),
    TOP_RIGHT(2, true),
    MIDDLE_LEFT(3, false),
    MIDDLE_RIGHT(4, true),
    BOTTOM_LEFT(5, false),
    BOTTOM_RIGHT(6, true),
}
