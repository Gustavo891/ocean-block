package ocean_block.system.level.config

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldStartWith
import org.bukkit.configuration.file.YamlConfiguration
import java.io.StringReader

private fun parse(yaml: String, warnings: MutableList<String> = mutableListOf()): LevelConfig =
    LevelConfigParser.parse(YamlConfiguration.loadConfiguration(StringReader(yaml)), { warnings += it })

private fun bundled(): String =
    LevelConfigParser::class.java.classLoader.getResourceAsStream("nivel.yml")!!.bufferedReader().use { it.readText() }

class LevelConfigParserTest : StringSpec({

    "o arquivo empacotado tem os valores da spec" {
        val warnings = mutableListOf<String>()
        val config = parse(bundled(), warnings)

        config.debrisDefault shouldBe 4
        config.debris["barril"] shouldBe 10
        config.debris["barril_ferro"] shouldBe 25
        config.generator["COBBLESTONE"] shouldBe 1
        config.generator["DIAMOND_ORE"] shouldBe 20
        config.recipeXp(8) shouldBe 34
        warnings.shouldBeEmpty()
    }

    "valor invalido vira o padrao com aviso" {
        val warnings = mutableListOf<String>()
        val config = parse(
            """
            detritos:
              padrao: -3
              tipos:
                barril: abc
            receitas:
              base: 10
              por-nivel: -1
            """.trimIndent(),
            warnings,
        )

        config.debrisDefault shouldBe 4
        config.debris.containsKey("barril") shouldBe false
        config.recipePerLevel shouldBe 3
        warnings.size shouldBe 3
        warnings.forEach { it shouldStartWith "[nivel]" }
    }

    "arquivo vazio usa os padroes da spec" {
        val config = parse("")

        config.debrisDefault shouldBe 4
        config.debris["sucata"] shouldBe 4
        config.generator["EMERALD_ORE"] shouldBe 15
        config.recipeBase shouldBe 10
        config.recipePerLevel shouldBe 3
    }

    "mobs valem pela tabela e os que faltam caem no padrao" {
        val config = parse(bundled())

        config.mobXp("ZOMBIE") shouldBe 3
        config.mobXp("CREEPER") shouldBe 5
        config.mobXp("ENDERMAN") shouldBe 8
        config.mobXp("WARDEN") shouldBe 10
        config.mobXp("COW") shouldBe 1
        config.mobXp("PIG") shouldBe 1
    }

    "arquivo sem a secao mobs usa a tabela padrao" {
        val config = parse("receitas:\n  base: 10\n")

        config.mobXp("SKELETON") shouldBe 3
        config.mobXp("SHEEP") shouldBe 1
    }

    "nome do mob aceita minusculas no yml" {
        val config = parse("mobs:\n  padrao: 2\n  tipos:\n    zombie: 7\n")

        config.mobXp("ZOMBIE") shouldBe 7
        config.mobXp("CHICKEN") shouldBe 2
    }
})
