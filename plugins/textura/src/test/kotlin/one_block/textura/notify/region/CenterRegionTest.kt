package one_block.textura.notify.region

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import one_block.textura.hud.anchor.ScreenAnchor
import one_block.textura.notify.NotificationLine
import org.bukkit.configuration.file.YamlConfiguration
import java.util.function.Consumer

class CenterRegionTest : StringSpec({

    fun parse(yml: String): Pair<List<NotificationRegion>, List<String>> {
        val warnings = mutableListOf<String>()
        val regions = RegionConfigParser.parse(YamlConfiguration().apply { loadFromString(yml) }, Consumer { warnings += it })
        return regions to warnings
    }

    "CENTRO e animacao viram uma regiao animada no centro da tela" {
        val (regions, warnings) = parse(
            """
            xp:
              ancora: CENTRO
              x: 8
              y: 28
              linhas: 1
              fundo: false
              animacao: true
            """.trimIndent(),
        )

        warnings.shouldBeEmpty()
        regions.single() shouldBe NotificationRegion("xp", ScreenAnchor.CENTER, 8, 28, 1, 1, false, StackDirection.DOWN, animated = true)
    }

    "sem animacao a regiao continua parada" {
        parse("a:\n  ancora: CENTRO\n").first.single().animated shouldBe false
    }

    "regiao animada acima do alcance e ignorada com aviso" {
        val (regions, warnings) = parse("a:\n  ancora: CENTRO\n  y: -2\n  animacao: true\n")

        regions.shouldBeEmpty()
        warnings.single() shouldContain "animada"
    }

    "a regiao xp e padrao" {
        RegionConfigParser.DEFAULTS.first { it.id == "xp" } shouldBe
            NotificationRegion("xp", ScreenAnchor.CENTER, 8, 28, 1, 1, false, StackDirection.DOWN, animated = true)
    }

    "a linha guarda quando comecou e a troca pela mesma chave recomeca" {
        val lines = RegionLines(1)
        lines.push(NotificationLine("+1", null, 40, "xp"), 10)
        lines.lines().single().startedAt shouldBe 10L

        lines.push(NotificationLine("+2", null, 40, "xp"), 25)
        lines.lines().single().startedAt shouldBe 25L
        lines.lines().single().expiresAt shouldBe 65L
    }

    "o quadro fica parado na primeira metade e chega a 15 no fim" {
        NotificationAnimation.frame(0, 40) shouldBe 0
        NotificationAnimation.frame(19, 40) shouldBe 0
        NotificationAnimation.frame(20, 40) shouldBe 1
        NotificationAnimation.frame(30, 40) shouldBe 8
        NotificationAnimation.frame(39, 40) shouldBe 15
        NotificationAnimation.frame(80, 40) shouldBe 15
    }
})
