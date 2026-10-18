package ocean_block.system.skills.view

import me.devnatan.inventoryframework.View
import me.devnatan.inventoryframework.ViewConfigBuilder
import me.devnatan.inventoryframework.context.Context
import me.devnatan.inventoryframework.context.OpenContext
import me.devnatan.inventoryframework.context.RenderContext
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import ocean_block.libs.formatter.Mini
import one_block.textura.api.GlyphApi
import one_block.textura.api.TitleApi
import ocean_block.libs.item_builder.addLore
import ocean_block.libs.item_builder.itemStack
import ocean_block.libs.item_builder.meta
import ocean_block.libs.item_builder.name
import ocean_block.system.skills.skill.Skill
import ocean_block.system.skills.skill.SkillData
import ocean_block.system.skills.skill.SkillManager
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import kotlin.math.roundToInt

class SkillsView : View() {

    override fun onInit(config: ViewConfigBuilder) {
        config.cancelInteractions()
        config.cancelInteractions()
        config.title(BACKGROUND)
        config.layout(
            "1aaa 4ddd",
            "2bbb 5eee",
            "3ccc 6fff"
        )
    }

    override fun onFirstRender(render: RenderContext) {
        for (skill in Skill.entries) {
            render.layoutSlot('1' + skill.ordinal)
                .withItem(createSkillItem(render, skill))
                .onClick { click ->
                    click.openForPlayer(SkillTreeView::class.java, skill)
                }

            render.layoutSlot('a' + skill.ordinal)
                .withItem(createBarItem(render, skill))
                .onClick { click ->
                    click.openForPlayer(SkillTreeView::class.java, skill)
                }
        }
    }

    override fun onOpen(open: OpenContext) {
        open.modifyConfig().title(buildTitle(open))
    }

    private fun buildTitle(context: Context): Component {
        val data = SkillManager.get(context.player.uniqueId) ?: return BACKGROUND
        val builder = TitleApi.builder() ?: return BACKGROUND

        for (skill in Skill.entries) {
            val column = skill.ordinal / BAR_ROW_FONT.size
            val row = skill.ordinal % BAR_ROW_FONT.size
            if (column >= BAR_X.size) continue

            val font = BAR_ROW_FONT[row]
            val id = "bar${barStage(data, skill)}"
            val char = GlyphApi.getChar(id, font) ?: continue
            val key = GlyphApi.getFontKey(id, font) ?: continue

            builder.addRaw(char, BAR_X[column] - TITLE_ORIGIN_X, key, BAR_ADVANCE)
        }

        return Component.empty()
            .append(BACKGROUND)
            .append(Mini.mm.deserialize("<shift:$BACKGROUND_RESET>"))
            .append(builder.build().color(NamedTextColor.WHITE))
    }

    private fun createBarItem(context: Context, skill: Skill): ItemStack =
        skillItem(context, skill, NamespacedKey(NAMESPACE, "blank"))

    private fun barStage(data: SkillData, skill: Skill): Int {
        if (data.levelOf(skill) >= Skill.MAX_LEVEL) return BAR_STAGES

        val needed = data.nextLevelCost(skill)
        if (needed <= 0) return BAR_STAGES

        val fraction = data.progressOf(skill).toDouble() / needed
        return (fraction * BAR_STAGES).roundToInt().coerceIn(0, BAR_STAGES)
    }

    private fun createSkillItem(context: Context, skill: Skill): ItemStack =
        skillItem(context, skill, skill.model)

    private fun skillItem(context: Context, skill: Skill, model: NamespacedKey): ItemStack {
        val data = SkillManager.get(context.player.uniqueId)

        return itemStack(Material.PAPER) {
            meta<ItemMeta> {
                itemModel = NamespacedKey("oneblock", "blank")
                name("<green>${skill.prettyName}")

                addLore {
                    +skill.description.map { "<gray>$it" }

                    if (data == null) {
                        +""
                        +"<red>Dados não carregados."
                        return@addLore
                    }

                    val level = data.levelOf(skill)
                    val perks = perkLines(skill, level)

                    +""
                    +" <white>Nível: <gray>$level/${Skill.MAX_LEVEL}"
                    +" ${SkillProgress.line(data, skill)}"

                    if (perks.isNotEmpty()) {
                        +""
                        +" <white>Habilidades:"
                        +perks
                    }

                    +""
                    +"<white><glyph:mouse_left> <gray>Clique <white>para ver a progressão."
                }
            }
        }
    }

    private fun perkLines(skill: Skill, level: Int): List<String> = skill.perks.mapNotNull { perk ->
        val tier = perk.tierAt(level) ?: return@mapNotNull null

        " <dark_gray>▸ <gray>${perk.prettyName} ${tier.roman} (${tier.percent}%)"
    }

    private companion object {

        const val NAMESPACE = "oneblock"

        val BACKGROUND = Mini.mm.deserialize("<white><shift:-8><glyph:skills_menu>")

        const val BACKGROUND_RESET = -169

        const val TITLE_ORIGIN_X = 8

        const val BAR_STAGES = 10

        const val BAR_ADVANCE = 42

        val BAR_X = intArrayOf(31, 121)

        val BAR_ROW_FONT = arrayOf("skills_row0", "skills_row1", "skills_row2")
    }
}
