package one_block.libs.formatter

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import one_block.textura.api.TexturaTags

object Mini {

    val mm: MiniMessage = TexturaTags.miniMessage

    fun deserialize(input: String): Component = mm.deserialize(input)
}
