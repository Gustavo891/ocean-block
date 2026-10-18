package one_block.textura.hud

/** Lado da HUD principal em que a HUD secundária é desenhada. */
enum class HudSide { LEFT, RIGHT }

/**
 * HUD menor desenhada na *mesma* bossbar da principal, colada ao lado dela.
 *
 * A principal continua onde estava: o renderer compensa o avanço da secundária com
 * um espaçamento negativo de mesmo tamanho, então o total medido pelo cliente (que
 * é o que define a centralização) não muda.
 */
data class HudSecondary(
    val text: String,
    val side: HudSide = HudSide.RIGHT,
    val gap: Int = DEFAULT_GAP,
) {
    companion object {
        /** Espaço em pixels entre a borda da HUD principal e a secundária. */
        const val DEFAULT_GAP = 6
    }
}
