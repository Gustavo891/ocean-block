package one_block.textura.pack

object VanillaTextures {

    private val BLOCK_SUFFIXES = listOf("", "_front", "_side", "_top")

    fun candidates(name: String): List<String> =
        listOf("item/$name") + BLOCK_SUFFIXES.map { "block/$name$it" }

    fun <T : Any> find(name: String, read: (String) -> T?): T? =
        candidates(name).firstNotNullOfOrNull(read)
}
