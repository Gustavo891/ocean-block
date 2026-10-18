package one_block.system.custom_items.block

interface BlockUpgrade {
    val id: String
    val prettyName: String
    val maxLevel: Int
    val lore: List<String>

    fun costFor(level: Int): Double

    fun statLine(level: Int): String
}
