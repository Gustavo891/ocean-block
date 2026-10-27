package ocean_block.system.drift.loot

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.ints.shouldBeInRange
import io.kotest.matchers.shouldBe
import ocean_block.system.drift.config.DebrisType
import ocean_block.system.drift.config.DropEntry
import org.bukkit.Material
import kotlin.random.Random

private fun typeWith(vararg drops: DropEntry) =
    DebrisType("t", 1, Material.OAK_PLANKS, 1f, drops.toList())

class DropRollerTest : StringSpec({

    "chance 1 sempre cai com quantidade no intervalo" {
        val type = typeWith(DropEntry(Material.STICK, 1, 3, 1.0))
        val random = Random(3)

        val amounts = (1..500).map { DropRoller.roll(type, random).single().second }

        amounts.forEach { it shouldBeInRange 1..3 }
        (1 in amounts) shouldBe true
        (3 in amounts) shouldBe true
    }

    "chance 0 nunca cai" {
        val type = typeWith(
            DropEntry(Material.STICK, 1, 3, 0.0),
            DropEntry(Material.COAL, 1, 1, 1.0),
        )
        val random = Random(5)

        val materials = (1..500).flatMap { DropRoller.roll(type, random) }.map { it.first }

        (Material.STICK in materials) shouldBe false
        (Material.COAL in materials) shouldBe true
    }

    "resultado vazio vira um barbante" {
        val type = typeWith(DropEntry(Material.STICK, 1, 3, 0.0))

        DropRoller.roll(type, Random(1)) shouldBe listOf(Material.STRING to 1)
    }

    "tipo que mostra o drop exibe um dos itens sorteados" {
        val type = typeWith().copy(showsDrop = true)
        val drops = listOf(Material.STICK to 3, Material.DIRT to 1, Material.FLINT to 1)
        val random = Random(7)

        val shown = (1..300).map { DropRoller.shown(type, drops, random) }.toSet()

        shown shouldBe setOf(Material.STICK, Material.DIRT, Material.FLINT)
    }

    "tipo que nao mostra o drop exibe sempre a exibicao" {
        val type = typeWith()
        val drops = listOf(Material.STICK to 3, Material.DIRT to 1)

        (1..50).map { DropRoller.shown(type, drops, Random(it)) }.toSet() shouldBe setOf(Material.OAK_PLANKS)
    }

    "sem drops sorteados exibe a exibicao" {
        val type = typeWith().copy(showsDrop = true)

        DropRoller.shown(type, emptyList(), Random(2)) shouldBe Material.OAK_PLANKS
    }
})
