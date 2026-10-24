package ocean_block.system.machines.furnace

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class FurnaceRulesTest : StringSpec({

    val recipes = mapOf("RAW_IRON" to "IRON_INGOT", "SAND" to "GLASS", "COBBLESTONE" to "STONE")
    val empty = FurnaceSlot(null, 0, null, 0, 0)

    fun state(vararg slots: FurnaceSlot) = FurnaceState(slots.toList() + List(FurnaceRules.SLOTS - slots.size) { empty })

    "insert prefere o slot com o mesmo item e depois o primeiro vazio" {
        val start = state(FurnaceSlot("SAND", 10, null, 0, 0), empty, FurnaceSlot("RAW_IRON", 60, null, 0, 0))
        val (next, taken) = FurnaceRules.insert(start, "RAW_IRON", 10, recipes)
        taken shouldBe 10
        next.slots[2].inputCount shouldBe 64
        next.slots[1] shouldBe FurnaceSlot("RAW_IRON", 6, null, 0, 0)
    }

    "insert sem receita recusa" {
        FurnaceRules.insert(FurnaceState.EMPTY, "DIRT", 5, recipes) shouldBe (FurnaceState.EMPTY to 0)
    }

    "insert com tudo cheio devolve o que nao coube" {
        val full = FurnaceState(List(FurnaceRules.SLOTS) { FurnaceSlot("SAND", 64, null, 0, 0) })
        FurnaceRules.insert(full, "SAND", 5, recipes).second shouldBe 0
    }

    "quatro slots trabalham em paralelo e 50 ticks fazem 1 item" {
        val start = state(
            FurnaceSlot("SAND", 2, null, 0, 0),
            FurnaceSlot("RAW_IRON", 2, null, 0, 0),
            FurnaceSlot("COBBLESTONE", 2, null, 0, 40),
            FurnaceSlot("SAND", 1, "GLASS", 3, 0),
        )
        val next = (1..5).fold(start) { acc, _ -> FurnaceRules.tick(acc, 10, recipes) }
        next.slots[0] shouldBe FurnaceSlot("SAND", 1, "GLASS", 1, 0)
        next.slots[1] shouldBe FurnaceSlot("RAW_IRON", 1, "IRON_INGOT", 1, 0)
        next.slots[2] shouldBe FurnaceSlot("COBBLESTONE", 1, "STONE", 1, 40)
        next.slots[3] shouldBe FurnaceSlot(null, 0, "GLASS", 4, 0)
    }

    "saida com outro item pausa" {
        val start = state(FurnaceSlot("SAND", 3, "STONE", 2, 0))
        FurnaceRules.status(start.slots[0], recipes) shouldBe FurnaceSlotStatus.OUTPUT_FULL
        FurnaceRules.tick(start, 50, recipes) shouldBe start
    }

    "saida com 64 pausa" {
        val start = state(FurnaceSlot("SAND", 3, "GLASS", 64, 0))
        FurnaceRules.status(start.slots[0], recipes) shouldBe FurnaceSlotStatus.OUTPUT_FULL
        FurnaceRules.tick(start, 50, recipes) shouldBe start
    }

    "slot vazio fica parado" {
        FurnaceRules.status(empty, recipes) shouldBe FurnaceSlotStatus.EMPTY
        FurnaceRules.status(FurnaceSlot("SAND", 1, null, 0, 0), recipes) shouldBe FurnaceSlotStatus.WORKING
    }

    "energia de 2 slots ativos em 10 ticks e 20" {
        val start = state(FurnaceSlot("SAND", 3, null, 0, 0), FurnaceSlot("SAND", 3, "STONE", 1, 0), FurnaceSlot("RAW_IRON", 1, null, 0, 0))
        FurnaceRules.energyNeeded(start, 10, recipes) shouldBe 20
    }

    "retirar entrada e saida" {
        val start = state(FurnaceSlot("SAND", 3, "GLASS", 2, 30))
        FurnaceRules.takeInput(start, 0) shouldBe (state(FurnaceSlot(null, 0, "GLASS", 2, 0)) to ("SAND" to 3))
        FurnaceRules.takeOutput(start, 0) shouldBe (state(FurnaceSlot("SAND", 3, null, 0, 30)) to ("GLASS" to 2))
        FurnaceRules.takeOutput(FurnaceState.EMPTY, 1) shouldBe (FurnaceState.EMPTY to null)
        FurnaceRules.takeInput(FurnaceState.EMPTY, 9) shouldBe (FurnaceState.EMPTY to null)
    }

    "fornalha antiga em note block vira alto-forno" {
        FurnaceRules.adopts("NOTE_BLOCK") shouldBe true
        FurnaceRules.adopts("FURNACE") shouldBe false
        FurnaceRules.adopts("AIR") shouldBe false
    }

    "ida e volta pelos counters" {
        val start = state(FurnaceSlot("SAND", 3, "GLASS", 2, 30), empty, empty, FurnaceSlot(null, 0, "IRON_INGOT", 7, 0))
        FurnaceState.fromCounters(start.toCounters()) shouldBe start
        FurnaceState.fromCounters(emptyMap()) shouldBe FurnaceState.EMPTY
    }
})
