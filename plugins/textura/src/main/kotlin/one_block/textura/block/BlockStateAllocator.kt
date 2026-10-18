package one_block.textura.block

interface BlockStateAllocator {

    val baseName: String

    val capacity: Int

    fun sync(ids: Collection<String>)

    fun propertiesOf(id: String): String?

    fun idByProperties(properties: String): String?

    fun activeAllocations(): List<Pair<String, String>>

    fun save()
}
