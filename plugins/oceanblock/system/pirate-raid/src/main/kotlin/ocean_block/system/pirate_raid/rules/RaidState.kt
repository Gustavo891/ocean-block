package ocean_block.system.pirate_raid.rules

sealed interface RaidPhase {
    data class Wave(val number: Int) : RaidPhase
    data object Won : RaidPhase
    data object Fled : RaidPhase
    data object Abandoned : RaidPhase
}

object RaidState {

    fun next(
        wave: Int,
        waves: Int,
        alive: Int,
        elapsedTicks: Long,
        durationTicks: Long,
        emptyTicks: Long,
        emptyLimit: Long,
    ): RaidPhase = when {
        alive <= 0 && wave >= waves -> RaidPhase.Won
        emptyTicks >= emptyLimit -> RaidPhase.Abandoned
        elapsedTicks >= durationTicks -> RaidPhase.Fled
        alive <= 0 -> RaidPhase.Wave(wave + 1)
        else -> RaidPhase.Wave(wave)
    }
}
