package ocean_block.system.playtime_rewards.config

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import ocean_block.system.playtime_rewards.model.ItemPrize
import org.bukkit.configuration.file.YamlConfiguration
import java.io.StringReader

private val knownItems = setOf("CLOCK", "IRON_INGOT", "GOLD_INGOT", "DIAMOND", "IRON_BLOCK", "DIAMOND_BLOCK")

private fun parse(yaml: String, warnings: MutableList<String> = mutableListOf()) =
    RewardsConfigParser.parse(
        YamlConfiguration.loadConfiguration(StringReader(yaml)),
        { it.uppercase() in knownItems },
        { warnings += it },
    )

private fun bundled(): String =
    RewardsConfigParser::class.java.classLoader.getResourceAsStream("recompensas.yml")!!
        .bufferedReader().use { it.readText() }

class RewardsConfigParserTest : StringSpec({

    "o arquivo empacotado tem a escada da spec" {
        val warnings = mutableListOf<String>()
        val ladder = parse(bundled(), warnings)

        ladder.map { it.id } shouldContainExactly listOf("30m", "1h", "2h", "4h", "8h", "12h", "24h", "48h", "72h", "100h")
        ladder.map { it.minutes } shouldContainExactly listOf(30, 60, 120, 240, 480, 720, 1440, 2880, 4320, 6000)
        ladder.first().coins shouldBe 2000
        ladder.first().xp shouldBe 100
        ladder[3].pearls shouldBe 1
        ladder[4].items shouldContainExactly listOf(ItemPrize("IRON_INGOT", 16))
        ladder.last().coins shouldBe 300_000
        ladder.last().pearls shouldBe 25
        ladder.last().items shouldContainExactly listOf(ItemPrize("DIAMOND_BLOCK", 2))
        warnings.shouldBeEmpty()
    }

    "marcos saem ordenados por minutos" {
        val ladder = parse(
            """
            marcos:
              b: { minutos: 120 }
              a: { minutos: 30 }
            """.trimIndent()
        )
        ladder.map { it.id } shouldContainExactly listOf("a", "b")
    }

    "ícone padrão é relógio e comandos são lidos" {
        val ladder = parse(
            """
            marcos:
              a:
                minutos: 10
                comandos: ["say %player%"]
            """.trimIndent()
        )
        ladder.single().icon shouldBe "clock"
        ladder.single().commands shouldContainExactly listOf("say %player%")
    }

    "marco sem minutos válidos é ignorado com aviso" {
        val warnings = mutableListOf<String>()
        val ladder = parse("marcos:\n  a: { minutos: 0 }\n  b: { minutos: 5 }", warnings)
        ladder.map { it.id } shouldContainExactly listOf("b")
        warnings shouldHaveSize 1
        warnings.single() shouldContain "[recompensas]"
    }

    "dois marcos com os mesmos minutos mantêm o primeiro" {
        val warnings = mutableListOf<String>()
        val ladder = parse("marcos:\n  a: { minutos: 5 }\n  b: { minutos: 5 }", warnings)
        ladder.map { it.id } shouldContainExactly listOf("a")
        warnings shouldHaveSize 1
    }

    "item inválido ou malformado é ignorado com aviso e o resto do marco vale" {
        val warnings = mutableListOf<String>()
        val ladder = parse(
            """
            marcos:
              a:
                minutos: 5
                coins: 10
                itens: ["PEDRA_MAGICA:1", "DIAMOND:x", "DIAMOND:3"]
            """.trimIndent(),
            warnings,
        )
        ladder.single().items shouldContainExactly listOf(ItemPrize("DIAMOND", 3))
        ladder.single().coins shouldBe 10
        warnings shouldHaveSize 2
    }

    "ícone inválido volta para relógio com aviso" {
        val warnings = mutableListOf<String>()
        val ladder = parse("marcos:\n  a: { minutos: 5, icone: nada }", warnings)
        ladder.single().icon shouldBe "clock"
        warnings shouldHaveSize 1
    }

    "valor negativo vira zero com aviso" {
        val warnings = mutableListOf<String>()
        val ladder = parse("marcos:\n  a: { minutos: 5, coins: -3 }", warnings)
        ladder.single().coins shouldBe 0
        warnings shouldHaveSize 1
    }

    "sem seção de marcos a escada é vazia" {
        parse("outra: 1").shouldBeEmpty()
    }
})
