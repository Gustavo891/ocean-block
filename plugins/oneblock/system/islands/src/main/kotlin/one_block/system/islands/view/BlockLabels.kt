package one_block.system.islands.view

import one_block.libs.translation.translate
import one_block.textura.api.BlockApi
import org.bukkit.Material

fun blockLabel(material: Material): String =
    BlockApi.overrideFor(material)?.name ?: translate(material.translationKey())
