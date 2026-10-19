package one_block.textura.dialog

import net.kyori.adventure.key.Key
import one_block.textura.dialog.json.ScreenDefinition
import one_block.textura.dialog.json.ScreenParser
import one_block.textura.dialog.kit.IconSize
import one_block.textura.dialog.kit.SurfaceSize
import one_block.textura.dialog.kit.UiImage
import one_block.textura.dialog.kit.UiKit
import one_block.textura.dialog.kit.UiPiece
import one_block.textura.dialog.theme.ButtonLook
import one_block.textura.dialog.theme.ButtonTheme
import one_block.textura.dialog.theme.ChipTheme
import one_block.textura.dialog.theme.DropdownTheme
import one_block.textura.dialog.theme.NavTheme
import one_block.textura.dialog.theme.SegmentedTheme
import one_block.textura.dialog.theme.SliderTheme
import one_block.textura.dialog.theme.SlotsTheme
import one_block.textura.dialog.theme.SwitchTheme
import one_block.textura.dialog.theme.UiTextStyle
import one_block.textura.dialog.theme.UiTheme
import org.bukkit.entity.Player
import java.io.File
import java.lang.reflect.Proxy
import java.util.logging.Handler
import java.util.logging.LogRecord
import java.util.logging.Logger

object DialogFixtures {

    val kit: UiKit = kit { it.isNotBlank() && !it.contains('{') }

    fun kit(known: (String) -> Boolean): UiKit = object : UiKit {
        override fun has(id: String): Boolean = known(id)
        override fun image(id: String): UiImage = UiImage("x", 10, 10, 10)
        override fun surface(size: SurfaceSize): UiImage? = null
        override fun icon(size: IconSize): UiImage? = null
        override fun imageFont(offset: Int): Key = Key.key("test", "image_$offset")
        override fun textFont(offset: Int): Key = Key.key("test", "text_$offset")
        override val spaceFont: Key = Key.key("test", "space")
        override fun spaceChar(amount: Int): String? = null
        override fun charAdvance(codePoint: Int): Int = 6
        override val hit: UiPiece? = null
    }

    val theme = UiTheme(
        id = "stone",
        iconGap = 3,
        iconSize = 8,
        styles = mapOf("body" to UiTextStyle(null, false)),
        surfaces = mapOf("sheet" to "ui:stone/sheet"),
        switch = SwitchTheme("ui:on", "ui:off", "On", "Off"),
        segmented = SegmentedTheme("ui:seg", "ui:seg_on", "body", "body", 4),
        button = ButtonTheme(
            "ui:button", "body",
            mapOf("default" to ButtonLook("ui:button", "body", "ui:button_selected", "body", "ui:button_disabled", "body")),
        ),
        nav = NavTheme("ui:nav", "ui:nav_active", "body", "body", 2),
        dropdown = DropdownTheme("ui:dropdown", "ui:arrow", "body"),
        slider = SliderTheme("ui:track", "ui:fill", "body"),
        slots = SlotsTheme("ui:slot", "body"),
        chip = ChipTheme("ui:chip", "body", 12, 3),
        icons = emptyMap(),
    )

    class Warnings : Handler() {
        val messages = mutableListOf<String>()
        override fun publish(record: LogRecord) {
            messages += record.message
        }
        override fun flush() = Unit
        override fun close() = Unit
    }

    fun parse(json: String, id: String = "tela"): Pair<ScreenDefinition?, List<String>> {
        val warnings = Warnings()
        val logger = Logger.getAnonymousLogger().apply {
            useParentHandlers = false
            addHandler(warnings)
        }
        val dir = kotlin.io.path.createTempDirectory("ui").toFile()
        val file = File(dir, "$id.json").apply { writeText(json) }
        return try {
            ScreenParser(logger).parse(file) to warnings.messages.toList()
        } finally {
            dir.deleteRecursively()
        }
    }

    fun player(): Player = Proxy.newProxyInstance(Player::class.java.classLoader, arrayOf(Player::class.java)) { _, method, _ ->
        when (method.returnType) {
            java.lang.Boolean.TYPE -> false
            Integer.TYPE -> 0
            java.lang.Long.TYPE -> 0L
            java.lang.Double.TYPE -> 0.0
            java.lang.Float.TYPE -> 0f
            String::class.java -> "Tester"
            else -> null
        }
    } as Player
}
