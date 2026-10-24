package ocean_block.system.machines.energy

import java.util.UUID

class EnergyMeter {

    private class Window(var start: Int, var accumulated: Long = 0, var lastRate: Long = 0)

    private val windows = mutableMapOf<UUID, Window>()

    fun record(id: UUID, amount: Long, tick: Int) {
        val window = windows.getOrPut(id) { Window(tick - Math.floorMod(tick, TICKS_PER_SECOND)) }
        roll(window, tick)
        window.accumulated += amount
    }

    fun perSecond(id: UUID, tick: Int): Long {
        val window = windows[id] ?: return 0
        roll(window, tick)
        return window.lastRate
    }

    fun forget(id: UUID) {
        windows.remove(id)
    }

    private fun roll(window: Window, tick: Int) {
        val steps = (tick - window.start) / TICKS_PER_SECOND
        if (steps < 1) return
        window.lastRate = if (steps == 1) window.accumulated else 0
        window.accumulated = 0
        window.start += steps * TICKS_PER_SECOND
    }

    private companion object {
        const val TICKS_PER_SECOND = 20
    }
}
