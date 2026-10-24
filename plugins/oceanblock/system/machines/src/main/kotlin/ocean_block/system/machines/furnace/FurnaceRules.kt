package ocean_block.system.machines.furnace

enum class FurnaceSlotStatus { EMPTY, WORKING, OUTPUT_FULL }

object FurnaceRules {

    const val SLOTS = 4
    const val MAX_STACK = 64
    const val TICKS_PER_ITEM = 50

    fun adopts(material: String): Boolean = material == "NOTE_BLOCK"

    fun insert(state: FurnaceState, material: String, amount: Int, recipes: Map<String, String>): Pair<FurnaceState, Int> {
        if (material !in recipes || amount <= 0) return state to 0
        val order = state.slots.indices.filter { state.slots[it].input == material } +
            state.slots.indices.filter { state.slots[it].input == null }
        var current = state
        var left = amount
        for (index in order) {
            if (left == 0) break
            val slot = current.slots[index]
            val taken = minOf(left, MAX_STACK - slot.inputCount)
            if (taken <= 0) continue
            current = current.with(index, slot.copy(input = material, inputCount = slot.inputCount + taken))
            left -= taken
        }
        return current to amount - left
    }

    fun takeInput(state: FurnaceState, slot: Int): Pair<FurnaceState, Pair<String, Int>?> {
        val current = state.slots.getOrNull(slot) ?: return state to null
        val input = current.input ?: return state to null
        return state.with(slot, current.copy(input = null, inputCount = 0, progress = 0)) to (input to current.inputCount)
    }

    fun takeOutput(state: FurnaceState, slot: Int): Pair<FurnaceState, Pair<String, Int>?> {
        val current = state.slots.getOrNull(slot) ?: return state to null
        val output = current.output ?: return state to null
        return state.with(slot, current.copy(output = null, outputCount = 0)) to (output to current.outputCount)
    }

    fun status(slot: FurnaceSlot, recipes: Map<String, String>): FurnaceSlotStatus {
        val input = slot.input?.takeIf { slot.inputCount > 0 } ?: return FurnaceSlotStatus.EMPTY
        val result = recipes[input] ?: return FurnaceSlotStatus.EMPTY
        val fits = (slot.output == null || slot.output == result) && slot.outputCount < MAX_STACK
        return if (fits) FurnaceSlotStatus.WORKING else FurnaceSlotStatus.OUTPUT_FULL
    }

    fun energyNeeded(state: FurnaceState, ticks: Int, recipes: Map<String, String>): Long =
        state.slots.count { status(it, recipes) == FurnaceSlotStatus.WORKING }.toLong() * ticks

    fun tick(state: FurnaceState, ticks: Int, recipes: Map<String, String>): FurnaceState =
        FurnaceState(state.slots.map { tickSlot(it, ticks, recipes) })

    private fun tickSlot(slot: FurnaceSlot, ticks: Int, recipes: Map<String, String>): FurnaceSlot {
        if (status(slot, recipes) != FurnaceSlotStatus.WORKING) return slot
        var current = slot.copy(progress = slot.progress + ticks)
        while (current.progress >= TICKS_PER_ITEM && status(current, recipes) == FurnaceSlotStatus.WORKING) {
            val input = checkNotNull(current.input)
            val left = current.inputCount - 1
            current = current.copy(
                input = input.takeIf { left > 0 },
                inputCount = left,
                output = recipes.getValue(input),
                outputCount = current.outputCount + 1,
                progress = current.progress - TICKS_PER_ITEM,
            )
        }
        return if (current.input == null) current.copy(progress = 0) else current
    }
}
