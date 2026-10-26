package ocean_block.system.sell.notify

data class ToastLine(val text: String, val icon: String)

object SaleCard {

    private const val BACKDROP = "glyph:toast_venda"
    private const val LINE = "glyph:toast_linha"

    fun lines(formattedTotal: String, amount: Int): List<ToastLine> {
        val sold = if (amount == 1) "1 item vendido" else "$amount itens vendidos"
        return listOf(
            ToastLine("<green>+$$formattedTotal</green>", BACKDROP),
            ToastLine("<white>$sold</white>", LINE),
        )
    }
}
