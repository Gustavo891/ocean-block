package one_block.textura.hud

import net.kyori.adventure.bossbar.BossBar


data class HudContent(
    val text: String,
    val color: BossBar.Color = BossBar.Color.YELLOW,
    val overlay: BossBar.Overlay = BossBar.Overlay.PROGRESS,
    val progress: Float = 1.0f,
)
