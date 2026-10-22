package one_block.textura.pack

object VanillaTextures {

    private val BLOCK_SUFFIXES = listOf("", "_front", "_side", "_top")
    private val TOP_SUFFIXES = listOf("_top", "")
    private val FRONT_SUFFIXES = listOf("_front", "_side1", "_side", "")
    private val SIDE_SUFFIXES = listOf("_side", "_side2", "_side1", "")
    private val UNDER_SUFFIXES = listOf("_bottom")

    fun candidates(name: String): List<String> =
        listOf("item/$name") + BLOCK_SUFFIXES.map { "block/$name$it" }

    fun <T : Any> find(name: String, read: (String) -> T?): T? =
        candidates(name).firstNotNullOfOrNull(read)

    fun <T : Any> faces(name: String, read: (String) -> T?): BlockFaces<T>? {
        fun first(suffixes: List<String>): T? = suffixes.firstNotNullOfOrNull { read("block/$name$it") }
        val top = first(TOP_SUFFIXES)
        val front = first(FRONT_SUFFIXES)
        val side = first(SIDE_SUFFIXES)
        if (top == null && front == null && side == null) return null
        val wall = front ?: side ?: top!!
        return BlockFaces(top ?: wall, wall, side ?: wall, first(UNDER_SUFFIXES))
    }
}
