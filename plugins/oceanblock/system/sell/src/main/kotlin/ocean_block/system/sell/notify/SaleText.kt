package ocean_block.system.sell.notify

object SaleText {

    fun line(formattedTotal: String, amount: Int): String {
        val sold = if (amount == 1) "1 item vendido" else "$amount itens vendidos"
        return "<green>+<dark_green>$<white>$formattedTotal <gray>($sold)"
    }
}
