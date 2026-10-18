package ocean_block.system.drift.config

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import org.bukkit.Material
import org.bukkit.configuration.file.YamlConfiguration
import java.io.StringReader

private fun parse(yaml: String, warnings: MutableList<String> = mutableListOf()): DriftConfig? =
    DriftConfigParser.parse(YamlConfiguration.loadConfiguration(StringReader(yaml))) { warnings += it }

private fun defaultYaml(): String =
    DriftConfigParser::class.java.classLoader.getResourceAsStream("drift.yml")!!
        .bufferedReader().use { it.readText() }

class DriftConfigParserTest : StringSpec({

    "yaml padrao gera quatro tipos com pesos da spec" {
        val config = parse(defaultYaml())!!

        config.types.map { it.id to it.weight } shouldBe
            listOf("tabua" to 50, "folhas" to 25, "sucata" to 17, "barril" to 8)
        config.settings.direction shouldBe CurrentDirection.EAST
        config.settings.speed shouldBe 0.05
        config.settings.maxPerLot shouldBe 8
        config.settings.surfaceY shouldBe 62.6
    }

    "material invalido e ignorado com aviso" {
        val warnings = mutableListOf<String>()
        val config = parse(
            """
            detritos:
              ruim:
                peso: 10
                exibicao: NAO_EXISTE
                drops:
                  - {material: STICK, min: 1, max: 1, chance: 1.0}
              bom:
                peso: 10
                exibicao: OAK_PLANKS
                drops:
                  - {material: STICK, min: 1, max: 1, chance: 1.0}
            """.trimIndent(),
            warnings,
        )!!

        config.types.map { it.id } shouldBe listOf("bom")
        warnings.filter { "ruim" in it } shouldHaveSize 1
    }

    "drop com material invalido some mas o tipo fica" {
        val warnings = mutableListOf<String>()
        val config = parse(
            """
            detritos:
              misto:
                peso: 10
                exibicao: BARREL
                drops:
                  - {material: NAO_EXISTE, min: 1, max: 1, chance: 1.0}
                  - {material: STICK, min: 1, max: 2, chance: 0.5}
            """.trimIndent(),
            warnings,
        )!!

        config.types shouldHaveSize 1
        config.types.single().drops shouldBe listOf(DropEntry(Material.STICK, 1, 2, 0.5))
        warnings shouldHaveSize 1
    }

    "sem tipos validos retorna null" {
        parse("corrente:\n  direcao: EAST\n").shouldBeNull()
        parse(
            """
            detritos:
              ruim:
                peso: 10
                exibicao: NAO_EXISTE
            """.trimIndent(),
        ).shouldBeNull()
    }

    "direcao invalida usa EAST com aviso" {
        val warnings = mutableListOf<String>()
        val config = parse(
            """
            corrente:
              direcao: CIMA
            detritos:
              tabua:
                peso: 1
                exibicao: OAK_PLANKS
                drops:
                  - {material: OAK_PLANKS, min: 1, max: 1, chance: 1.0}
            """.trimIndent(),
            warnings,
        )!!

        config.settings.direction shouldBe CurrentDirection.EAST
        warnings shouldHaveSize 1
    }
})
