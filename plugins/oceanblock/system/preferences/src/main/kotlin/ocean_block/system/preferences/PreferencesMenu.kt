package ocean_block.system.preferences

import me.devnatan.inventoryframework.View
import me.devnatan.inventoryframework.ViewConfigBuilder
import me.devnatan.inventoryframework.context.RenderContext
import ocean_block.libs.formatter.Mini
import ocean_block.libs.item_builder.itemStack
import ocean_block.libs.item_builder.meta
import ocean_block.libs.item_builder.name
import ocean_block.libs.item_builder.setLore
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack

class PreferencesMenu : View() {

    override fun onInit(config: ViewConfigBuilder) {
        config.cancelInteractions()
        config.title(Mini.mm.deserialize("<dark_gray>Preferências"))
        config.size(3)
    }

    override fun onFirstRender(render: RenderContext) {
        Preference.entries.forEachIndexed { index, preference ->
            render.slot(2, FIRST_COLUMN + index)
                .renderWith { preferenceItem(render.player, preference) }
                .onClick { click ->
                    val enabled = Preferences.toggle(click.player, preference)
                    click.player.playSound(click.player, Sound.UI_BUTTON_CLICK, 0.6f, if (enabled) 1.4f else 0.9f)
                    click.update()
                }
        }
    }

    private fun preferenceItem(player: Player, preference: Preference): ItemStack {
        val enabled = Preferences.isEnabled(player, preference)
        val material = Material.matchMaterial(preference.icon) ?: Material.PAPER
        return itemStack(material) {
            meta {
                name((if (enabled) "<green>" else "<red>") + preference.displayName)
                setLore {
                    +"<gray>${preference.description}"
                    +""
                    +(if (enabled) "<green>Ligado" else "<red>Desligado")
                    +"<yellow>Clique para alternar."
                }
                if (enabled) addEnchant(Enchantment.UNBREAKING, 1, true)
                addItemFlags(*ItemFlag.entries.toTypedArray())
            }
        }
    }

    companion object {
        private const val FIRST_COLUMN = 2
    }
}
