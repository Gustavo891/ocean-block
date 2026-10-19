package one_block.textura.api

import net.kyori.adventure.text.format.TextDecoration
import one_block.textura.item.TexturaItemKeys
import one_block.textura.model.ItemData
import one_block.textura.registry.ItemRegistry
import org.bukkit.persistence.PersistentDataType
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.inventory.ItemStack

/**
 * Acesso aos itens registrados no Textura a partir de outros plugins.
 *
 * Um item pode ser identificado por `custom_model_data`, pela chave de
 * `item_model` (1.21.4+) ou por ambos — [create] aplica o que o yml declarar.
 */
object ItemApi {

    @Volatile
    private var registry: ItemRegistry? = null

    fun init(registry: ItemRegistry) {
        this.registry = registry
    }

    /** `true` se existe um item com esse [id]. */
    fun has(id: String): Boolean = registry?.items?.containsKey(id) == true

    /** Dados brutos do item, ou `null` se o id não existir. */
    fun get(id: String): ItemData? = registry?.items?.get(id)

    /** Ids de todos os itens registrados. */
    fun ids(): Set<String> = registry?.items?.keys.orEmpty()

    /**
     * Monta o ItemStack do item [id] com o material, o `item_model` e o
     * `custom_model_data` declarados no yml, ou `null` se o id não existir ou o
     * material for inválido.
     */
    fun create(id: String): ItemStack? = create(id, 1)

    fun create(id: String, amount: Int): ItemStack? {
        val data = get(id) ?: return null
        val material = Material.matchMaterial(data.material) ?: return null

        val item = ItemStack(material, amount)
        item.editMeta { meta ->
            data.itemModel?.let { meta.itemModel = itemModelKey(data) }
            if (data.hasCustomModelData) {
                @Suppress("DEPRECATION")
                meta.setCustomModelData(data.customModelData)
            }
            meta.persistentDataContainer.set(TexturaItemKeys.ITEM_ID, PersistentDataType.STRING, data.id)

            data.name?.let { meta.displayName(text(it)) }
            if (data.lore.isNotEmpty()) meta.lore(data.lore.map { text(it) })
        }
        return item
    }

    private fun text(raw: String) =
        TexturaTags.miniMessage.deserialize(raw).decoration(TextDecoration.ITALIC, false)

    /**
     * Id do item da Textura gravado no NBT, ou `null` se não for um.
     *
     * É por aqui que outros sistemas devem reconhecer um item — nome e modelo
     * mudam quando o yml muda, o id não.
     */
    fun idFromItem(item: ItemStack?): String? {
        if (item == null || item.type.isAir) return null
        val meta = item.itemMeta ?: return null
        return meta.persistentDataContainer.get(TexturaItemKeys.ITEM_ID, PersistentDataType.STRING)
    }

    /** `true` se [item] é o item da Textura de id [id]. */
    fun matches(item: ItemStack?, id: String): Boolean = idFromItem(item) == id

    /** Chave do componente `minecraft:item_model` do item, ou `null` se ele não usa uma. */
    fun itemModelKey(data: ItemData): NamespacedKey? {
        val namespace = data.itemModelNamespace ?: return null
        val path = data.itemModelPath ?: return null
        return NamespacedKey(namespace, path)
    }

    /** Como [itemModelKey], mas partindo do id do item. */
    fun itemModelKey(id: String): NamespacedKey? = get(id)?.let { itemModelKey(it) }
}
