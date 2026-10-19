package one_block.textura.hud

/**
 * HUD menor desenhada na *mesma* bossbar da principal, colada ao lado dela.
 *
 * A principal continua onde estava: o renderer compensa o avanço da secundária com
 * um espaçamento negativo de mesmo tamanho, então o total medido pelo cliente (que
 * é o que define a centralização) não muda.
 */
data class HudSecondary(
    val text: String,
    val side: HudSide,
    val gap: Int,
) {
    constructor(text: String) : this(text, HudSide.RIGHT)

    constructor(text: String, side: HudSide) : this(text, side, DEFAULT_GAP)
}

private const val DEFAULT_GAP = 6
