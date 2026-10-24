package ocean_block.system.machines.energy

import java.util.UUID

class EnergyMeter {

    private class Window(var start: Int, var accumulated: Long = 0, var lastRate: Long = 0)

    private val windows = mutableMapOf<UUID, Window>()
    private val links = mutableMapOf<UUID, MutableMap<UUID, Int>>()

    fun record(id: UUID, amount: Long, tick: Int, consumer: UUID? = null) {
        val window = windows.getOrPut(id) { Window(tick - Math.floorMod(tick, TICKS_PER_SECOND)) }
        roll(window, tick)
        window.accumulated += amount
        consumer?.let { links.getOrPut(id) { linkedMapOf() }[it] = tick }
    }

    fun perSecond(id: UUID, tick: Int): Long {
        val window = windows[id] ?: return 0
        roll(window, tick)
        return window.lastRate
    }

    fun consumers(id: UUID, tick: Int): List<UUID> {
        val recent = links[id] ?: return emptyList()
        recent.entries.removeIf { tick - it.value > LINK_TICKS }
        return recent.keys.toList()
    }

    fun forget(id: UUID) {
        windows.remove(id)
        links.remove(id)
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
        const val LINK_TICKS = 40
    }
}
