package ocean_block.system.chest_sort

interface StackKind<T> {
    fun same(a: T, b: T): Boolean
    fun amount(stack: T): Int
    fun maxStack(stack: T): Int
    fun name(stack: T): String
    fun withAmount(stack: T, amount: Int): T
}
