package one_block.system.craft.forge

import one_block.libs.instance.Instance
import one_block.textura.api.ItemApi

object TexturaIngredients {

    fun requirement(prettyName: String, itemId: String, amount: Int): ForgeRequirement? {
        val item = ItemApi.create(itemId)
        if (item == null) {
            Instance.logger.warning(
                "[craft] item '$itemId' nao existe na Textura; o requisito '$prettyName' foi ignorado."
            )
            return null
        }
        return ForgeRequirement(prettyName, item, amount) { ItemApi.matches(it, itemId) }
    }

    fun item(itemId: String, amount: Int = 1) = ItemApi.create(itemId, amount)
}
