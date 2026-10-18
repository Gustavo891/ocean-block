package ocean_block.system.drift.debris

class ColumnKinds(private val read: (Int) -> BlockKind) {

    private val known = HashMap<Int, BlockKind>()

    fun at(blockY: Int): BlockKind = known.getOrPut(blockY) { read(blockY) }
}
