package one_block.textura.hud

import net.kyori.adventure.bossbar.BossBar

data class HudContent(
    val text: String,
    val color: BossBar.Color,
    val overlay: BossBar.Overlay,
    val progress: Float,
    val background: String,
) {
    constructor(text: String) : this(text, BossBar.Color.YELLOW, BossBar.Overlay.PROGRESS, 1.0f, HudBackground.DEFAULT_PREFIX)

    constructor(text: String, background: String) : this(text, BossBar.Color.YELLOW, BossBar.Overlay.PROGRESS, 1.0f, background)

    constructor(text: String, color: BossBar.Color, overlay: BossBar.Overlay, progress: Float) :
        this(text, color, overlay, progress, HudBackground.DEFAULT_PREFIX)
}
