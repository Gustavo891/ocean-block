package ocean_block.libs.item_builder

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextDecoration
import ocean_block.libs.formatter.Mini

class LoreBuilder {
    val lore = mutableListOf<Component>()

    operator fun Component.unaryPlus() {
        lore += this
    }

    operator fun String.unaryPlus() {
        lore += Mini.deserialize(this).decoration(TextDecoration.ITALIC, false)
    }

    operator fun List<String>.unaryPlus() {
        lore += this.map { Mini.deserialize(it).decoration(TextDecoration.ITALIC, false) }
    }
}
