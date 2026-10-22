package one_block.textura.dialog.theme

import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor

object UiColors {

    fun parse(raw: String?): TextColor? {
        val value = raw?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        return if (value.startsWith("#")) TextColor.fromHexString(value) else NamedTextColor.NAMES.value(value.lowercase())
    }
}

data class UiTextStyle(val color: TextColor?, val bold: Boolean, val shadow: Boolean = true)

data class SwitchTheme(val on: String, val off: String, val onLabel: String, val offLabel: String)

data class SegmentedTheme(
    val art: String,
    val selectedArt: String,
    val textStyle: String,
    val selectedTextStyle: String,
    val pad: Int,
    val frame: String? = null,
)

data class ButtonLook(
    val art: String,
    val textStyle: String,
    val selectedArt: String,
    val selectedTextStyle: String,
    val disabledArt: String,
    val disabledTextStyle: String,
) {
    fun arts(): Set<String> = setOf(art, selectedArt, disabledArt)
}

data class ButtonTheme(val art: String, val textStyle: String, val looks: Map<String, ButtonLook> = emptyMap()) {

    val base: ButtonLook get() = looks[DEFAULT_LOOK] ?: ButtonLook(art, textStyle, art, textStyle, art, textStyle)

    fun look(name: String?): ButtonLook = name?.let { looks[it] } ?: base

    companion object {
        const val DEFAULT_LOOK = "default"
    }
}

data class ChipTheme(val art: String, val textStyle: String, val height: Int, val pad: Int)

data class NavTheme(val art: String, val activeArt: String, val textStyle: String, val activeTextStyle: String, val pad: Int)

data class DropdownTheme(val art: String, val arrow: String, val textStyle: String)

data class SliderTheme(val track: String, val fill: String, val textStyle: String)

data class SlotsTheme(val art: String, val textStyle: String, val missingArt: String? = null)

data class UiTheme(
    val id: String,
    val iconGap: Int,
    val iconSize: Int,
    val styles: Map<String, UiTextStyle>,
    val surfaces: Map<String, String>,
    val switch: SwitchTheme,
    val segmented: SegmentedTheme,
    val button: ButtonTheme,
    val nav: NavTheme,
    val dropdown: DropdownTheme,
    val slider: SliderTheme,
    val slots: SlotsTheme,
    val chip: ChipTheme,
    val icons: Map<String, String>,
) {

    fun style(name: String?): UiTextStyle =
        styles[name ?: DEFAULT_STYLE] ?: styles[DEFAULT_STYLE] ?: UiTextStyle(null, false)

    fun controlArt(): Set<String> = setOf(
        switch.on, switch.off, segmented.art, segmented.selectedArt, button.art,
        nav.art, nav.activeArt, dropdown.art, dropdown.arrow, slider.track, slider.fill, slots.art,
    ) + listOfNotNull(slots.missingArt) + button.looks.values.flatMap { it.arts() }

    companion object {
        const val DEFAULT_STYLE = "body"
        const val TOOLTIP_STYLE = "tooltip"
    }
}
