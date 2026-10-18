package ocean_block.system.drift.config

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.shouldBe
import org.bukkit.Material
import org.bukkit.configuration.file.YamlConfiguration
import java.io.StringReader

private fun parse(
    yaml: String,
    warnings: MutableList<String> = mutableListOf(),
    isItem: (Material) -> Boolean = { true },
    maxStack: (Material) -> Int = { 64 },
): DriftConfig? =
    DriftConfigParser.parse(YamlConfiguration.loadConfiguration(StringReader(yaml)), { warnings += it }, isItem, maxStack)

private fun defaultYaml(): String =
    DriftConfigParser::class.java.classLoader.getResourceAsStream("drift.yml")!!
        .bufferedReader().use { it.readText() }

private val DEFAULT_ANIMATION = AnimationSettings(
    bobAmplitude = 0.06,
    bobPeriodTicks = 50,
    tiltDegrees = 8.0,
    spinDegreesPerSecond = 10.0,
    meander = 0.4,
    meanderPeriodTicks = 120,
    speedVariation = 0.2,
    wake = true,
)

private val DEFAULT_OBSTACLE = ObstacleSettings(
    maxDepth = 3,
    diveSpeed = 0.08,
    fadeTicks = 20,
)

class DriftConfigParserTest : StringSpec({

    "yaml padrao gera quatro tipos com pesos da spec" {
        val config = parse(defaultYaml())!!

        config.types.map { it.id to it.weight } shouldBe
            listOf("tabua" to 50, "folhas" to 25, "sucata" to 17, "barril" to 8)
        config.settings.direction shouldBe CurrentDirection.EAST
        config.settings.speed shouldBe 0.05
        config.settings.maxPerLot shouldBe 8
        config.settings.surfaceY shouldBe 62.85
        config.settings.streaks shouldBe true
        config.settings.streaksPerSecond shouldBe 6.0
        config.settings.animation shouldBe DEFAULT_ANIMATION
        config.types.associate { it.id to it.sink } shouldBe mapOf(
            "tabua" to 0.0,
            "folhas" to 0.0,
            "sucata" to 0.0,
            "barril" to 0.12,
        )
        config.settings.laneLength shouldBe 40.0
        config.settings.laneWidth shouldBe 10.0
        config.settings.spawnIntervalTicks shouldBe 100L
        config.settings.stuckSeconds shouldBe 20
        config.settings.obstacle shouldBe DEFAULT_OBSTACLE
        config.settings.hookRadius shouldBe 2.0
        config.settings.touchRadius shouldBe 1.5

        config.types.associate { it.id to it.display } shouldBe mapOf(
            "tabua" to Material.OAK_PLANKS,
            "folhas" to Material.OAK_LEAVES,
            "sucata" to Material.IRON_NUGGET,
            "barril" to Material.BARREL,
        )
        val drops = config.types.associate { it.id to it.drops }
        drops["tabua"] shouldBe listOf(DropEntry(Material.OAK_PLANKS, 1, 2, 1.0))
        drops["folhas"] shouldBe listOf(
            DropEntry(Material.OAK_LEAVES, 1, 2, 1.0),
            DropEntry(Material.STICK, 1, 2, 0.5),
            DropEntry(Material.OAK_SAPLING, 1, 1, 0.1),
            DropEntry(Material.STRING, 1, 1, 0.3),
        )
        drops["sucata"] shouldBe listOf(
            DropEntry(Material.IRON_NUGGET, 1, 3, 1.0),
            DropEntry(Material.RAW_COPPER, 1, 1, 0.25),
        )
        drops["barril"] shouldBe listOf(
            DropEntry(Material.BREAD, 1, 2, 0.6),
            DropEntry(Material.STRING, 1, 3, 0.6),
            DropEntry(Material.KELP, 2, 4, 0.5),
            DropEntry(Material.OAK_SAPLING, 1, 1, 0.3),
            DropEntry(Material.COD, 1, 2, 0.4),
            DropEntry(Material.IRON_INGOT, 1, 1, 0.1),
            DropEntry(Material.NAME_TAG, 1, 1, 0.02),
        )
    }

    "min maior que max e chance fora do intervalo geram aviso" {
        val warnings = mutableListOf<String>()
        val config = parse(
            """
            detritos:
              tabua:
                peso: 1
                exibicao: OAK_PLANKS
                drops:
                  - {material: STICK, min: 5, max: 2, chance: 1.0}
                  - {material: STRING, min: 1, max: 1, chance: 1.5}
            """.trimIndent(),
            warnings,
        )!!

        config.types.single().drops shouldBe listOf(
            DropEntry(Material.STICK, 5, 5, 1.0),
            DropEntry(Material.STRING, 1, 1, 1.0),
        )
        warnings shouldHaveSize 2
        warnings.count { "STICK" in it && "tabua" in it } shouldBe 1
        warnings.count { "STRING" in it && "tabua" in it } shouldBe 1
    }

    "valor nao numerico em configuracao usa padrao com aviso" {
        val warnings = mutableListOf<String>()
        val config = parse(
            """
            corrente:
              velocidade: abc
            detritos:
              tabua:
                peso: 1
                exibicao: OAK_PLANKS
                drops:
                  - {material: STICK, min: 1, max: 1, chance: 1.0}
            """.trimIndent(),
            warnings,
        )!!

        config.settings.speed shouldBe 0.05
        warnings shouldHaveSize 1
        warnings.single() shouldContain "velocidade"
    }

    "secoes ausentes usam padroes" {
        val warnings = mutableListOf<String>()
        val config = parse(
            """
            detritos:
              tabua:
                peso: 1
                exibicao: OAK_PLANKS
                drops:
                  - {material: STICK, min: 1, max: 1, chance: 1.0}
            """.trimIndent(),
            warnings,
        )!!

        config.settings shouldBe DriftSettings(
            direction = CurrentDirection.EAST,
            speed = 0.05,
            laneLength = 40.0,
            laneWidth = 10.0,
            surfaceY = 62.85,
            spawnIntervalTicks = 100L,
            maxPerLot = 8,
            stuckSeconds = 20,
            hookRadius = 2.0,
            touchRadius = 1.5,
            streaks = true,
            streaksPerSecond = 6.0,
            animation = DEFAULT_ANIMATION,
            obstacle = DEFAULT_OBSTACLE,
        )
        config.types.single().sink shouldBe 0.0
        warnings shouldHaveSize 0
    }

    "animacao com valores invalidos usa padroes e limites com aviso" {
        val warnings = mutableListOf<String>()
        val config = parse(
            """
            corrente:
              riscos: talvez
              riscos-por-segundo: -3
            animacao:
              balanco: abc
              periodo-balanco-ticks: 0
              inclinacao-graus: -5
              giro-graus-por-segundo: 12.5
              sinuosidade: -1
              periodo-sinuosidade-ticks: xyz
              variacao-velocidade: 2.0
              rastro: false
            detritos:
              tabua:
                peso: 1
                exibicao: OAK_PLANKS
                afundamento: nada
                drops:
                  - {material: STICK, min: 1, max: 1, chance: 1.0}
            """.trimIndent(),
            warnings,
        )!!

        config.settings.streaks shouldBe true
        config.settings.streaksPerSecond shouldBe 0.0
        config.settings.animation shouldBe AnimationSettings(
            bobAmplitude = 0.06,
            bobPeriodTicks = 1,
            tiltDegrees = 0.0,
            spinDegreesPerSecond = 12.5,
            meander = 0.0,
            meanderPeriodTicks = 120,
            speedVariation = 0.9,
            wake = false,
        )
        config.types.single().sink shouldBe 0.0
        warnings shouldHaveSize 9
        listOf(
            "riscos ", "riscos-por-segundo ", "balanco ", "periodo-balanco-ticks ", "inclinacao-graus ",
            "sinuosidade ", "periodo-sinuosidade-ticks ", "variacao-velocidade ", "afundamento ",
        ).forEach { key -> warnings.count { ".$key" in it } shouldBe 1 }
    }

    "obstaculo com valores validos e lido" {
        val config = parse(
            """
            obstaculo:
              profundidade-maxima: 5
              velocidade-mergulho: 0.12
              tempo-sumir-ticks: 40
            detritos:
              tabua:
                peso: 1
                exibicao: OAK_PLANKS
                drops:
                  - {material: STICK, min: 1, max: 1, chance: 1.0}
            """.trimIndent(),
        )!!

        config.settings.obstacle shouldBe ObstacleSettings(maxDepth = 5, diveSpeed = 0.12, fadeTicks = 40)
    }

    "obstaculo com valores invalidos usa padroes e limites com aviso" {
        val warnings = mutableListOf<String>()
        val config = parse(
            """
            spawn:
              tempo-preso-segundos: 0
            obstaculo:
              profundidade-maxima: -2
              velocidade-mergulho: nada
              tempo-sumir-ticks: 0
            detritos:
              tabua:
                peso: 1
                exibicao: OAK_PLANKS
                drops:
                  - {material: STICK, min: 1, max: 1, chance: 1.0}
            """.trimIndent(),
            warnings,
        )!!

        config.settings.stuckSeconds shouldBe 1
        config.settings.obstacle shouldBe ObstacleSettings(maxDepth = 0, diveSpeed = 0.08, fadeTicks = 1)
        warnings shouldHaveSize 4
        listOf("tempo-preso-segundos ", "profundidade-maxima ", "velocidade-mergulho ", "tempo-sumir-ticks ")
            .forEach { key -> warnings.count { ".$key" in it } shouldBe 1 }
    }

    "escala fora de 0.1 a 2.0 e limitada com aviso" {
        val warnings = mutableListOf<String>()
        val config = parse(
            """
            detritos:
              grande:
                peso: 1
                exibicao: OAK_PLANKS
                escala: 5.0
                drops:
                  - {material: STICK, min: 1, max: 1, chance: 1.0}
              pequeno:
                peso: 1
                exibicao: OAK_PLANKS
                escala: 0.01
                drops:
                  - {material: STICK, min: 1, max: 1, chance: 1.0}
            """.trimIndent(),
            warnings,
        )!!

        config.types.associate { it.id to it.scale } shouldBe mapOf("grande" to 2.0f, "pequeno" to 0.1f)
        warnings shouldHaveSize 2
        warnings.count { "grande.escala" in it } shouldBe 1
        warnings.count { "pequeno.escala" in it } shouldBe 1
    }

    "afundamento valido por tipo" {
        val config = parse(
            """
            detritos:
              barril:
                peso: 1
                exibicao: BARREL
                afundamento: 0.3
                drops:
                  - {material: STICK, min: 1, max: 1, chance: 1.0}
            """.trimIndent(),
        )!!

        config.types.single().sink shouldBe 0.3
    }

    "tipo sem drops validos permanece com aviso" {
        val warnings = mutableListOf<String>()
        val config = parse(
            """
            detritos:
              vazio:
                peso: 1
                exibicao: OAK_PLANKS
                drops:
                  - {material: NAO_EXISTE, min: 1, max: 1, chance: 1.0}
            """.trimIndent(),
            warnings,
        )!!

        config.types.single().drops shouldHaveSize 0
        warnings.count { "vazio" in it } shouldBe 2
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

    "material que nao e item e ignorado com aviso" {
        val warnings = mutableListOf<String>()
        val config = parse(
            """
            detritos:
              agua:
                peso: 10
                exibicao: WATER
                drops:
                  - {material: STICK, min: 1, max: 1, chance: 1.0}
              misto:
                peso: 10
                exibicao: OAK_PLANKS
                drops:
                  - {material: WATER, min: 1, max: 1, chance: 1.0}
                  - {material: STICK, min: 1, max: 1, chance: 1.0}
            """.trimIndent(),
            warnings,
            isItem = { it != Material.WATER },
        )!!

        config.types.map { it.id } shouldBe listOf("misto")
        config.types.single().drops shouldBe listOf(DropEntry(Material.STICK, 1, 1, 1.0))
        warnings shouldHaveSize 2
        warnings.count { "agua" in it } shouldBe 1
        warnings.count { "misto" in it } shouldBe 1
    }

    "max acima do tamanho da pilha e limitado com aviso" {
        val warnings = mutableListOf<String>()
        val config = parse(
            """
            detritos:
              tabua:
                peso: 10
                exibicao: OAK_PLANKS
                drops:
                  - {material: STICK, min: 1, max: 40, chance: 1.0}
            """.trimIndent(),
            warnings,
            maxStack = { 16 },
        )!!

        config.types.single().drops shouldBe listOf(DropEntry(Material.STICK, 1, 16, 1.0))
        warnings shouldHaveSize 1
        warnings.single() shouldContain "STICK"
    }
})
