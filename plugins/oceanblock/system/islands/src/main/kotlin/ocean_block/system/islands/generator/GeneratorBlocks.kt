package ocean_block.system.islands.generator

import java.util.concurrent.ConcurrentHashMap

class GeneratorBlockSet {

    private val keys: MutableSet<BlockKey> = ConcurrentHashMap.newKeySet()

    fun mark(key: BlockKey) {
        keys += key
    }

    fun consume(key: BlockKey): Boolean = keys.remove(key)

    fun forget(key: BlockKey) {
        keys -= key
    }
}

object GeneratorBlocks {

    private val blocks = GeneratorBlockSet()

    fun mark(key: BlockKey) = blocks.mark(key)

    fun consume(key: BlockKey): Boolean = blocks.consume(key)

    fun forget(key: BlockKey) = blocks.forget(key)
}
