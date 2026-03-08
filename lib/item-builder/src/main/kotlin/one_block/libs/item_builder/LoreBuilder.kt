package one_block.libs.item_builder

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.text.minimessage.MiniMessage

class LoreBuilder {
    val lore = mutableListOf<Component>()

    operator fun Component.unaryPlus() {
        lore += this
    }

    operator fun String.unaryPlus() {
        lore += MiniMessage.miniMessage().deserialize(this).decoration(TextDecoration.ITALIC, false)
    }

    operator fun List<String>.unaryPlus() {
        lore += this.map { MiniMessage.miniMessage().deserialize(it).decoration(TextDecoration.ITALIC, false) }
    }
}