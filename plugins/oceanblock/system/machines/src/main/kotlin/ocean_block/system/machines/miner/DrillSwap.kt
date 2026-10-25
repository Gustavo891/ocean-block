package ocean_block.system.machines.miner

object DrillSwap {

    data class Result(val next: DrillTier, val returned: DrillTier?)

    fun mount(current: DrillTier, inserted: DrillTier): Result? {
        if (current == inserted) return null
        return Result(inserted, current.takeIf { it != DrillTier.IRON })
    }

    fun unmount(current: DrillTier): Result? {
        if (current == DrillTier.IRON) return null
        return Result(DrillTier.IRON, current)
    }
}
