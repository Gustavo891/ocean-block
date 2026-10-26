package one_block.textura.pack

import com.google.gson.JsonParser
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import net.kyori.adventure.key.Key
import one_block.textura.dialog.kit.TextProvider
import one_block.textura.dialog.kit.UiTextFont
import one_block.textura.notify.layout.PlaquePieces
import one_block.textura.notify.region.RegionConfigParser
import java.awt.image.BufferedImage
import java.nio.file.Files
import java.util.logging.Logger
import java.util.zip.ZipFile

class NotificationPackWriterTest : StringSpec({

    val textFont = UiTextFont(listOf(TextProvider(Key.key("minecraft", "font/ascii_custom.png"), 8, 7, listOf("ab"))), emptyMap())

    fun item(): BufferedImage = BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB).apply {
        for (x in 0 until 12) for (y in 0 until 16) setRGB(x, y, 0xFF336699.toInt())
    }

    "gera uma fonte por altura de linha com texto, placa e ícones na mesma altura" {
        val builder = PackBuilder()
        val kit = NotificationPackWriter(
            RegionConfigParser.DEFAULTS,
            textFont,
            setOf("oak_planks", "nada"),
            { name -> if (name == "oak_planks") item() else null },
            emptyMap(),
            null,
            Logger.getLogger("NotificationPackWriterTest"),
        ).write(builder)

        kit.textTops.toList() shouldBe listOf(0, 4, 14, 18, 28, 32, 42, 56, 70, 84, 98, 112, 126, 140)
        kit.icons.keys shouldBe setOf("item:oak_planks")
        kit.icons.getValue("item:oak_planks").advance shouldBe 7
        kit.plaque shouldNotBe null
        kit.plaque!!.bodies.keys shouldBe PlaquePieces.BODY_WIDTHS.toSet()

        val zip = Files.createTempFile("notify", ".zip").toFile().apply { deleteOnExit() }
        builder.build(zip)
        ZipFile(zip).use { file ->
            val font = file.getEntry("assets/textura/font/notify/70.json") ?: error("fonte notify/70 ausente")
            val json = JsonParser.parseString(file.getInputStream(font).use { it.readBytes().decodeToString() }).asJsonObject
            val providers = json.getAsJsonArray("providers").map { it.asJsonObject }
            val bitmaps = providers.filter { it.get("type").asString == "bitmap" }
            bitmaps.first { Key.key(it.get("file").asString) == Key.key("minecraft", "font/ascii_custom.png") }.get("ascent").asInt shouldBe 7 - 70
            bitmaps.filter { it.get("file").asString.startsWith("textura:notify/plaque") }.map { it.get("ascent").asInt }.toSet() shouldBe setOf(10 - 70)
            bitmaps.single { it.get("file").asString == "textura:notify/item/oak_planks.png" }.get("ascent").asInt shouldBe 7 - 70
            providers.map { it.get("type").asString }.toSet() shouldContainAll listOf("space", "bitmap")
            file.getEntry("assets/textura/textures/notify/item/oak_planks.png") shouldNotBe null
        }
    }

    "glyph toast_ vira fundo no tamanho real, ancorado 3 px acima do texto da linha" {
        val card = Files.createTempFile("toast_card", ".png").toFile().apply { deleteOnExit() }
        javax.imageio.ImageIO.write(BufferedImage(132, 28, BufferedImage.TYPE_INT_ARGB).apply { setRGB(0, 0, 0xFF212121.toInt()) }, "png", card)
        val builder = PackBuilder()
        val kit = NotificationPackWriter(
            RegionConfigParser.DEFAULTS,
            textFont,
            emptySet(),
            { null },
            mapOf("toast_card" to card),
            null,
            Logger.getLogger("NotificationPackWriterTest"),
        ).write(builder)

        kit.backdrop("glyph:toast_card")?.width shouldBe 132
        kit.icons.containsKey("glyph:toast_card") shouldBe false

        val zip = Files.createTempFile("notify", ".zip").toFile().apply { deleteOnExit() }
        builder.build(zip)
        ZipFile(zip).use { file ->
            val font = file.getEntry("assets/textura/font/notify/70.json") ?: error("fonte notify/70 ausente")
            val json = JsonParser.parseString(file.getInputStream(font).use { it.readBytes().decodeToString() }).asJsonObject
            val backdrop = json.getAsJsonArray("providers").map { it.asJsonObject }
                .single { it.get("type").asString == "bitmap" && it.get("file").asString == "textura:notify/toast/toast_card.png" }
            backdrop.get("height").asInt shouldBe 28
            backdrop.get("ascent").asInt shouldBe 10 - 70
        }
    }
})
