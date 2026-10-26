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

    "os toasts de nivel e tempo dividem uma pilha no canto superior direito" {
        RegionConfigParser.DEFAULTS.first { it.id == "toasts" } shouldBe
            NotificationRegion("toasts", ScreenAnchor.TOP_RIGHT, -2, 5, 6, 1, false, StackDirection.DOWN, animated = true)
        RegionConfigParser.DEFAULTS.none { it.id == "nivel" || it.id == "tempo" } shouldBe true
    }

    "os avisos ficam no canto superior esquerdo com espaco para o toast de venda" {
        RegionConfigParser.DEFAULTS.first { it.id == "avisos" } shouldBe
            NotificationRegion("avisos", ScreenAnchor.TOP_LEFT, 2, 5, 5, 1, true, StackDirection.DOWN, animated = true)
    }

    "a linha guarda quando comecou e a fase, e a troca pela mesma chave recomeca" {
        val lines = RegionLines(1)
        lines.push(NotificationLine("+1", null, 70, "xp"), 10, 37)
        lines.lines().single().startedAt shouldBe 10L
        lines.lines().single().phase shouldBe 37

        lines.push(NotificationLine("+2", null, 70, "xp"), 25, 52)
        lines.lines().single().startedAt shouldBe 25L
        lines.lines().single().phase shouldBe 52
        lines.lines().single().expiresAt shouldBe 95L
    }

    "a fase e o tempo do mundo no ciclo de 200 ticks que divide o dia" {
        NotificationAnimation.phase(0) shouldBe 0
        NotificationAnimation.phase(250) shouldBe 50
        NotificationAnimation.phase(23999) shouldBe 199
        NotificationAnimation.phase(24000) shouldBe 0
        NotificationAnimation.phase(24250) shouldBe 50
    }

    "toasts e avisos sao animados e a linha deles dura sempre o tempo da animacao" {
        RegionConfigParser.DEFAULTS.filter { it.id == "toasts" || it.id == "avisos" }.all { it.animated } shouldBe true
        val avisos = RegionConfigParser.DEFAULTS.first { it.id == "avisos" }
        val xp = RegionConfigParser.DEFAULTS.first { it.id == "xp" }
        NotificationAnimation.lifetime(avisos, NotificationLine("a", 80)).durationTicks shouldBe NotificationAnimation.SLIDE_LIFE
        NotificationAnimation.lifetime(xp, NotificationLine("a", 70)).durationTicks shouldBe 70
        NotificationAnimation.lifetime(RegionConfigParser.DEFAULTS.first { it.id == "coleta" }, NotificationLine("a", 60)).durationTicks shouldBe 60
    }
})
