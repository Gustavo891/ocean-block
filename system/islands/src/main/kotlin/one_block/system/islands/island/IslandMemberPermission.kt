package one_block.system.islands.island

enum class IslandMemberPermission {

    Containers();

    fun has(value: Byte) = (value.toInt() and (1 shl ordinal)) != 0

    fun add(value: Byte) = (value.toInt() or (1 shl ordinal)).toByte()

    fun remove(value: Byte) = (value.toInt() and (1 shl ordinal).inv()).toByte()
}