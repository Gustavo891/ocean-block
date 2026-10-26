package ocean_block.system.islands.generator

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import org.bukkit.configuration.file.YamlConfiguration
import java.io.StringReader
import kotlin.random.Random

private val BLOCKS = setOf("COBBLESTONE", "STONE", "COAL_ORE", "COPPER_ORE", "IRON_ORE", "GOLD_ORE", "REDSTONE_ORE", "LAPIS_ORE", "EMERALD_ORE", "DIAMOND_ORE")

private fun parse(yaml: String, warnings: MutableList<String> = mutableListOf()): GeneratorTable =
    GeneratorTableParser.parse(YamlConfiguration.loadConfiguration(StringReader(yaml)), { it in BLOCKS }, { warnings += it })

private fun bundled(): String =
    GeneratorTableParser::class.java.classLoader.getResourceAsStream("gerador.yml")!!.bufferedReader().use { it.readText() }

private class FixedRandom(private val value: Int) : Random() {
    override fun nextBits(bitCount: Int): Int = 0
    override fun nextInt(until: Int): Int = value.coerceAtMost(until - 1)
}

class GeneratorTableTest : StringSpec({

    "le os pesos de cada nivel" {
        val table = parse(
            """
            niveis:
              0: {COBBLESTONE: 90, COAL_ORE: 10}
              1: {COBBLESTONE: 70, COAL_ORE: 10, IRON_ORE: 20}
            """.trimIndent(),
        )

        table.weights(0) shouldContainExactly listOf(GeneratorWeight("COBBLESTONE", 90), GeneratorWeight("COAL_ORE", 10))
        table.weights(1).map { it.block } shouldContainExactly listOf("COBBLESTONE", "COAL_ORE", "IRON_ORE")
    }

    "nivel acima do maior usa o maior e nivel sem tabela usa o anterior" {
        val table = parse(
            """
            niveis:
              0: {COBBLESTONE: 100}
              2: {COBBLESTONE: 50, DIAMOND_ORE: 50}
            """.trimIndent(),
        )

        table.weights(1) shouldBe table.weights(0)
        table.weights(9) shouldBe table.weights(2)
        table.maxLevel shouldBe 2
    }

    "bloco desconhecido, peso invalido e nivel nao numerico viram aviso" {
        val warnings = mutableListOf<String>()
        val table = parse(
            """
            niveis:
              0: {COBBLESTONE: 90, NAO_EXISTE: 5, COAL_ORE: -3}
              x: {COBBLESTONE: 1}
            """.trimIndent(),
            warnings,
        )

        table.weights(0) shouldContainExactly listOf(GeneratorWeight("COBBLESTONE", 90))
        warnings shouldHaveSize 3
    }

    "sem niveis validos so gera pedregulho" {
        val warnings = mutableListOf<String>()

        parse("niveis: {}", warnings).weights(0) shouldContainExactly listOf(GeneratorWeight("COBBLESTONE", 1))
        warnings shouldHaveSize 1
    }

    "sorteio respeita os pesos acumulados" {
        val table = parse("niveis:\n  0: {COBBLESTONE: 90, COAL_ORE: 10}")

        GeneratorRoller.roll(table, 0, FixedRandom(0)) shouldBe "COBBLESTONE"
        GeneratorRoller.roll(table, 0, FixedRandom(89)) shouldBe "COBBLESTONE"
        GeneratorRoller.roll(table, 0, FixedRandom(90)) shouldBe "COAL_ORE"
    }

    "tabela padrao libera os minerios por nivel" {
        val warnings = mutableListOf<String>()
        val table = parse(bundled(), warnings)
        val ores = { level: Int -> table.weights(level).map { it.block }.toSet() }
        val base = setOf("COBBLESTONE", "STONE", "COAL_ORE", "COPPER_ORE", "IRON_ORE")

        warnings shouldHaveSize 0
        table.maxLevel shouldBe 4
        ores(0) shouldBe base
        ores(1) shouldBe base + setOf("LAPIS_ORE", "REDSTONE_ORE")
        ores(2) shouldBe ores(1) + setOf("GOLD_ORE", "EMERALD_ORE")
        ores(3) shouldBe ores(2) + "DIAMOND_ORE"
        ores(4) shouldBe ores(3)
    }

    "o nivel 4 aumenta a chance de diamante sobre o nivel 3" {
        val table = parse(bundled())
        val chance = { level: Int ->
            val weights = table.weights(level)
            weights.first { it.block == "DIAMOND_ORE" }.weight.toDouble() / weights.sumOf { it.weight }
        }

        (chance(4) > chance(3)) shouldBe true
    }
})
