package ocean_block.system.sell.rules

data class SaleLine<K>(val slot: Int, val key: K, val amount: Int)

data class SoldLine<K>(val slot: Int, val key: K, val amount: Int, val value: Double)

data class SaleQuote<K>(
    val sold: List<SoldLine<K>>,
    val unsellable: List<SaleLine<K>>,
) {
    val total: Double = sold.sumOf { it.value }
    val isEmpty: Boolean = sold.isEmpty() && unsellable.isEmpty()
}

object SaleRules {

    fun <K> quote(lines: List<SaleLine<K>>, price: (K) -> Double): SaleQuote<K> {
        val sold = mutableListOf<SoldLine<K>>()
        val unsellable = mutableListOf<SaleLine<K>>()
        lines.forEach { line ->
            val unit = price(line.key)
            if (unit > 0.0) {
                sold += SoldLine(line.slot, line.key, line.amount, unit * line.amount)
            } else {
                unsellable += line
            }
        }
        return SaleQuote(sold, unsellable)
    }
}
