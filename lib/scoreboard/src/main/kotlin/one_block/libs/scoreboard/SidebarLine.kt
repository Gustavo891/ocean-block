package one_block.libs.scoreboard

import net.kyori.adventure.text.Component

data class SidebarLine(
    val score: Int,
    val text: Component,
)
