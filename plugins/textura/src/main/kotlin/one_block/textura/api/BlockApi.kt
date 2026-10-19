package one_block.textura.api

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import one_block.textura.block.NoteBlockAllocator
import one_block.textura.block.TexturaBlockKeys
import one_block.textura.model.BlockDefinition
import one_block.textura.model.BlockEntryType
import one_block.textura.registry.BlockRegistry
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.block.Block
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType

object BlockApi {

    @Volatile
    private var registry: BlockRegistry? = null

    @Volatile
    private var allocator: NoteBlockAllocator? = null

    fun init(registry: BlockRegistry, allocator: NoteBlockAllocator) {
        this.registry = registry
        this.allocator = allocator
    }

    fun has(id: String): Boolean = registry?.blocks?.containsKey(id) == true

    fun get(id: String): BlockDefinition? = registry?.blocks?.get(id)

    fun ids(): Set<String> = registry?.blocks?.keys.orEmpty()

    fun customIds(): Set<String> = registry?.customIds.orEmpty()

    fun blockData(id: String): org.bukkit.block.data.BlockData? {
        val definition = get(id) ?: return null
        if (definition.type != BlockEntryType.CUSTOM) return null
        val properties = allocator?.propertiesOf(id) ?: return null

        return runCatching { Bukkit.createBlockData("minecraft:note_block[$properties]") }.getOrNull()
    }

    fun idAt(block: Block): String? {
        if (block.type != Material.NOTE_BLOCK) return null
        val properties = block.blockData.asString
            .substringAfter('[', "")
            .substringBefore(']')
        if (properties.isEmpty()) return null

        return allocator?.idByProperties(properties)
    }

    fun itemModelKey(id: String): NamespacedKey? {
        val definition = get(id) ?: return null
        if (definition.type != BlockEntryType.CUSTOM) return null
        return NamespacedKey(BlockDefinition.NAMESPACE, definition.id)
    }

    fun itemMaterial(id: String): Material? {
        val definition = get(id) ?: return null
        return Material.matchMaterial(definition.itemMaterial)
    }

    fun createItem(id: String): ItemStack? = createItem(id, 1)

    fun createItem(id: String, amount: Int): ItemStack? {
        val definition = get(id) ?: return null

        if (definition.type == BlockEntryType.OVERRIDE) {
            val target = definition.target?.let { Material.matchMaterial(it) } ?: return null
            return ItemStack(target, amount)
        }

        val material = Material.matchMaterial(definition.itemMaterial) ?: return null
        val item = ItemStack(material, amount)
        item.editMeta { meta ->
            meta.itemModel = NamespacedKey(BlockDefinition.NAMESPACE, definition.id)
            definition.name?.let {
                meta.displayName(
                    Component.text(it, NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false)
                )
            }
            meta.persistentDataContainer.set(TexturaBlockKeys.BLOCK_ID, PersistentDataType.STRING, definition.id)
        }
        return item
    }

    fun overrideFor(material: Material): BlockDefinition? =
        registry?.overridesByTarget?.get(material.key.value())

    fun dropFor(definition: BlockDefinition): ItemStack? {
        val dropId = definition.dropId ?: return null
        val amount = definition.dropAmount
        return ItemApi.create(dropId, amount) ?: createItem(dropId, amount)
    }

    /** Definição do bloco nesta posição, seja ela `CUSTOM` ou `OVERRIDE`. */
    fun definitionAt(block: Block): BlockDefinition? {
        idAt(block)?.let { return get(it) }
        return overrideFor(block.type)
    }

    /**
     * O que este bloco deve dropar, ou `null` se ele não é um bloco da Textura.
     *
     * Uma lista vazia é resposta legítima e diferente de `null`: significa "é um
     * bloco meu e ele não dropa nada". Quem chama precisa distinguir os dois para
     * não cair de volta no drop vanilla.
     */
    fun dropsAt(block: Block): List<ItemStack>? {
        val definition = definitionAt(block) ?: return null
        return dropsFor(definition)
    }

    fun dropsFor(definition: BlockDefinition): List<ItemStack> {
        val drops = mutableListOf<ItemStack>()

        // Em OVERRIDE o drop vanilla é sempre cancelado; só CUSTOM tem "o próprio item".
        if (definition.type == BlockEntryType.CUSTOM && definition.defaultDrop) {
            createItem(definition.id)?.let { drops += it }
        }

        dropFor(definition)?.let { drops += it }
        return drops
    }

    fun idFromItem(item: ItemStack?): String? {
        if (item == null || item.type.isAir) return null
        val meta = item.itemMeta ?: return null
        return meta.persistentDataContainer.get(TexturaBlockKeys.BLOCK_ID, PersistentDataType.STRING)
    }
}
