package one_block.textura.hud

import net.kyori.adventure.bossbar.BossBar

data class HudContent(
    val text: String,
    val color: BossBar.Color,
    val overlay: BossBar.Overlay,
    val progress: Float,
) {
    constructor(text: String) : this(text, BossBar.Color.YELLOW, BossBar.Overlay.PROGRESS, 1.0f)
}
