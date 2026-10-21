package ocean_block.system.stations.sieve

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import org.bukkit.configuration.file.YamlConfiguration
import java.io.StringReader

private val KNOWN = setOf("pedaco_ferro", "pedaco_cobre", "pedaco_ouro", "REDSTONE", "COAL", "FLINT", "DIAMOND", "CLAY_BALL", "PRISMARINE_CRYSTALS", "LAPIS_LAZULI", "WHEAT_SEEDS", "SUGAR_CANE", "BONE", "OAK_SAPLING")

private fun parse(yaml: String, warnings: MutableList<String> = mutableListOf()): SieveTable =
    SieveTableParser.parse(YamlConfiguration.loadConfiguration(StringReader(yaml)), { it in KNOWN }, { warnings += it })

private fun bundled(): String =
    SieveTableParser::class.java.classLoader.getResourceAsStream("peneira.yml")!!.bufferedReader().use { it.readText() }

class SieveTableParserTest : StringSpec({

    "saida desconhecida vira aviso e a linha valida fica" {
        val warnings = mutableListOf<String>()
        val table = parse(
            """
            cascalho:
              madeira:
                - {saida: pedaco_ferro, min: 1, max: 2, chance: 0.3}
                - {saida: NAO_EXISTE, min: 1, max: 1, chance: 0.5}
            """.trimIndent(),
            warnings,
        )

        table.drops(SieveInput.GRAVEL, SieveTier.WOOD) shouldBe listOf(SieveDrop("pedaco_ferro", 1, 2, 0.3))
        warnings shouldHaveSize 1
    }

    "chance fora de 0..1 e min maior que max sao ignorados com aviso" {
        val warnings = mutableListOf<String>()
        val table = parse(
            """
            areia:
              ferro:
                - {saida: CLAY_BALL, min: 1, max: 2, chance: 1.5}
                - {saida: CLAY_BALL, min: 3, max: 1, chance: 0.4}
            """.trimIndent(),
            warnings,
        )

        table.drops(SieveInput.SAND, SieveTier.IRON) shouldBe emptyList()
        warnings shouldHaveSize 2
    }

    "automatica usa a tabela de ferro" {
        val table = parse("terra:\n  ferro:\n    - {saida: BONE, min: 1, max: 1, chance: 0.15}\n")

        table.drops(SieveInput.DIRT, SieveTier.AUTOMATIC) shouldBe listOf(SieveDrop("BONE", 1, 1, 0.15))
    }

    "peneira.yml embutido segue a tabela do spec" {
        val warnings = mutableListOf<String>()
        val table = parse(bundled(), warnings)

        warnings shouldBe emptyList()
        table.drops(SieveInput.GRAVEL, SieveTier.IRON) shouldContain SieveDrop("pedaco_ferro", 1, 2, 0.40)
        table.drops(SieveInput.GRAVEL, SieveTier.IRON) shouldContain SieveDrop("REDSTONE", 1, 2, 0.04)
        table.drops(SieveInput.GRAVEL, SieveTier.WOOD).map { it.output } shouldBe listOf("pedaco_ferro", "pedaco_cobre", "COAL", "FLINT")
        table.drops(SieveInput.SAND, SieveTier.DIAMOND) shouldContain SieveDrop("LAPIS_LAZULI", 1, 2, 0.10)
        table.drops(SieveInput.DIRT, SieveTier.WOOD) shouldContain SieveDrop("OAK_SAPLING", 1, 1, 0.05)
    }
})
