package ocean_block.system.drift.loot

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.ints.shouldBeInRange
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import ocean_block.system.drift.config.DebrisType
import org.bukkit.Material
import kotlin.random.Random

private fun type(id: String, weight: Int) =
    DebrisType(id, weight, Material.OAK_PLANKS, 1f, emptyList())

class DebrisPickerTest : StringSpec({

    "lista vazia retorna null" {
        DebrisPicker.pick(emptyList(), Random(1)).shouldBeNull()
    }

    "peso zero nunca e sorteado" {
        val types = listOf(type("a", 0), type("b", 1))
        val random = Random(1)

        val ids = (1..1000).map { DebrisPicker.pick(types, random)!!.id }.toSet()

        ids shouldBe setOf("b")
    }

    "frequencia segue o peso" {
        val types = listOf(type("a", 3), type("b", 1))
        val random = Random(7)

        val aCount = (1..4000).count { DebrisPicker.pick(types, random)!!.id == "a" }

        aCount shouldBeInRange 2850..3150
    }
})
