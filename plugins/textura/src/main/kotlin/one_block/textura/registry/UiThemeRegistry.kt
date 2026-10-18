package one_block.textura.registry

import one_block.textura.dialog.theme.ButtonLook
import one_block.textura.dialog.theme.ButtonTheme
import one_block.textura.dialog.theme.ChipTheme
import one_block.textura.dialog.theme.DropdownTheme
import one_block.textura.dialog.theme.NavTheme
import one_block.textura.dialog.theme.SliderTheme
import one_block.textura.dialog.theme.SlotsTheme
import one_block.textura.dialog.theme.SegmentedTheme
import one_block.textura.dialog.theme.SwitchTheme
import one_block.textura.dialog.theme.UiColors
import one_block.textura.dialog.theme.UiTextStyle
import one_block.textura.dialog.theme.UiTheme
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File
import java.util.logging.Logger

class UiThemeRegistry(private val logger: Logger) {

    @Volatile
    var themes: Map<String, UiTheme> = emptyMap()
        private set

    fun load(folder: File) {
        val files = folder.listFiles { file -> file.isFile && file.extension == "yml" }.orEmpty().sortedBy { it.name }
        themes = files.flatMap { file ->
            val yaml = YamlConfiguration.loadConfiguration(file)
            yaml.getKeys(false).mapNotNull { key -> yaml.getConfigurationSection(key)?.let { key to read(key, it) } }
        }.toMap()
    }

    private fun read(id: String, section: ConfigurationSection): UiTheme = UiTheme(
        id = id,
        iconGap = section.getInt("icon-gap", 3),
        iconSize = section.getInt("icon-size", 8).coerceIn(1, 256),
        styles = styles(id, section.getConfigurationSection("text")),
        surfaces = strings(section.getConfigurationSection("surfaces")),
        switch = SwitchTheme(
            on = section.getString("switch.on") ?: "ui:$id/switch_on",
            off = section.getString("switch.off") ?: "ui:$id/switch_off",
            onLabel = section.getString("switch.labels.on") ?: "Ligado",
            offLabel = section.getString("switch.labels.off") ?: "Desligado",
        ),
        segmented = SegmentedTheme(
            art = section.getString("segmented.art") ?: "ui:$id/segment",
            selectedArt = section.getString("segmented.selected-art") ?: "ui:$id/segment_selected",
            textStyle = section.getString("segmented.text-style") ?: "muted",
            selectedTextStyle = section.getString("segmented.selected-text-style") ?: UiTheme.DEFAULT_STYLE,
            pad = section.getInt("segmented.pad", 4),
            frame = section.getString("segmented.frame"),
        ),
        button = ButtonTheme(
            art = section.getString("button.art") ?: "ui:$id/button",
            textStyle = section.getString("button.text-style") ?: UiTheme.DEFAULT_STYLE,
            looks = looks(section.getConfigurationSection("button.looks")),
        ),
        nav = NavTheme(
            art = section.getString("nav.art") ?: "ui:$id/nav",
            activeArt = section.getString("nav.active-art") ?: "ui:$id/nav_active",
            textStyle = section.getString("nav.text-style") ?: UiTheme.DEFAULT_STYLE,
            activeTextStyle = section.getString("nav.active-text-style") ?: "heading",
            pad = section.getInt("nav.pad", 2),
        ),
        dropdown = DropdownTheme(
            art = section.getString("dropdown.art") ?: "ui:$id/dropdown",
            arrow = section.getString("dropdown.arrow") ?: "ui:$id/arrow_down",
            textStyle = section.getString("dropdown.text-style") ?: UiTheme.DEFAULT_STYLE,
        ),
        slider = SliderTheme(
            track = section.getString("slider.track") ?: "ui:$id/slider_track",
            fill = section.getString("slider.fill") ?: "ui:$id/slider_fill",
            textStyle = section.getString("slider.text-style") ?: UiTheme.DEFAULT_STYLE,
        ),
        slots = SlotsTheme(
            art = section.getString("slots.art") ?: "ui:$id/slot",
            textStyle = section.getString("slots.text-style") ?: "heading",
        ),
        chip = ChipTheme(
            art = section.getString("chip.art") ?: "ui:$id/chip",
            textStyle = section.getString("chip.text-style") ?: UiTheme.DEFAULT_STYLE,
            height = section.getInt("chip.height", 20),
            pad = section.getInt("chip.pad", 8),
        ),
        icons = strings(section.getConfigurationSection("icons")),
    )

    private fun looks(section: ConfigurationSection?): Map<String, ButtonLook> {
        if (section == null) return emptyMap()
        return section.getKeys(false).mapNotNull { name ->
            val look = section.getConfigurationSection(name) ?: return@mapNotNull null
            val art = look.getString("art") ?: return@mapNotNull null
            val textStyle = look.getString("text-style") ?: UiTheme.DEFAULT_STYLE
            name to ButtonLook(
                art = art,
                textStyle = textStyle,
                selectedArt = look.getString("selected-art") ?: art,
                selectedTextStyle = look.getString("selected-text-style") ?: textStyle,
                disabledArt = look.getString("disabled-art") ?: art,
                disabledTextStyle = look.getString("disabled-text-style") ?: textStyle,
            )
        }.toMap()
    }

    private fun styles(themeId: String, section: ConfigurationSection?): Map<String, UiTextStyle> {
        if (section == null) return emptyMap()
        return section.getKeys(false).associateWith { name ->
            val raw = section.getString("$name.color")
            val color = UiColors.parse(raw)
            if (raw != null && color == null) logger.warning("[ui] Tema '$themeId': cor '$raw' do estilo '$name' inválida")
            UiTextStyle(color, section.getBoolean("$name.bold", false), section.getBoolean("$name.shadow", true))
        }
    }

    private fun strings(section: ConfigurationSection?): Map<String, String> =
        section?.getKeys(false)?.mapNotNull { key -> section.getString(key)?.let { key to it } }?.toMap().orEmpty()
}
