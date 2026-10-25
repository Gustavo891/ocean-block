package ocean_block.system.islands.generator

import org.bukkit.block.Block

data class BlockKey(val world: String, val x: Int, val y: Int, val z: Int) {

    companion object {
        fun of(block: Block): BlockKey = BlockKey(block.world.name, block.x, block.y, block.z)
    }
}
