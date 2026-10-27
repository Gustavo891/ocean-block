package ocean_block.system.pirate_raid.config

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.bukkit.configuration.file.YamlConfiguration

class RaidConfigParserTest : FunSpec({

    val materials = setOf("DIAMOND", "GOLDEN_APPLE", "IRON_INGOT")

    fun parse(yaml: String, warnings: MutableList<String> = mutableListOf()): RaidConfig =
        RaidConfigParser.parse(
            YamlConfiguration().apply { loadFromString(yaml) },
            { it in materials },
            { warnings += it },
        )

    test("arquivo vazio usa o padrão") {
        parse("") shouldBe RaidConfig.DEFAULT
    }

    test("lê valores e faixas ordenadas") {
        val config = parse(
            """
            sorteio:
              chance: 0.5
              intervalo-ticks: 600
              nivel-minimo: 5
            raid:
              duracao-segundos: 120
            faixas:
              "20":
                ondas: [4, 5, 6]
                brutamontes: 0.25
              "10":
                ondas: [1, 2, 3]
                brutamontes: 0
            """.trimIndent(),
        )
        config.chance shouldBe 0.5
        config.intervalTicks shouldBe 600L
        config.minLevel shouldBe 5
        config.durationSeconds shouldBe 120
        config.bands.map { it.minLevel } shouldContainExactly listOf(10, 20)
        config.bands[1].waves shouldContainExactly listOf(4, 5, 6)
        config.bands[1].bruteShare shouldBe 0.25
    }

    test("valores ruins caem no padrão e avisam") {
        val warnings = mutableListOf<String>()
        val config = parse(
            """
            sorteio:
              chance: 2
              nivel-minimo: -1
            raid:
              duracao-segundos: 0
            """.trimIndent(),
            warnings,
        )
        config.chance shouldBe RaidConfig.DEFAULT.chance
        config.minLevel shouldBe RaidConfig.DEFAULT.minLevel
        config.durationSeconds shouldBe RaidConfig.DEFAULT.durationSeconds
        warnings shouldHaveSize 3
        warnings.forEach { it shouldContain "[piratas]" }
    }

    test("faixa sem ondas válidas é ignorada") {
        val warnings = mutableListOf<String>()
        val config = parse(
            """
            faixas:
              "10":
                ondas: [0, 2]
              "30":
                ondas: [5, 6, 8]
                brutamontes: 0.35
            """.trimIndent(),
            warnings,
        )
        config.bands.map { it.minLevel } shouldContainExactly listOf(30)
        warnings shouldHaveSize 1
    }

    test("drops com material inválido são ignorados; moeda e pérola são especiais") {
        val warnings = mutableListOf<String>()
        val config = parse(
            """
            drops:
              moeda: { chance: 1.0, min: 1, max: 1 }
              perola: { chance: 0.2, min: 1, max: 2 }
              DIAMOND: { chance: 0.1, min: 1, max: 1 }
              PEDRA_MAGICA: { chance: 0.5, min: 1, max: 1 }
              IRON_INGOT: { chance: 0.3, min: 3, max: 1 }
            """.trimIndent(),
            warnings,
        )
        config.drops.map { it.item } shouldContainExactly listOf("moeda", "perola", "DIAMOND")
        warnings shouldHaveSize 2
    }

    test("sem faixas válidas usa as faixas padrão") {
        val config = parse(
            """
            faixas:
              "10":
                ondas: []
            """.trimIndent(),
        )
        config.bands shouldBe RaidConfig.DEFAULT.bands
    }
})
