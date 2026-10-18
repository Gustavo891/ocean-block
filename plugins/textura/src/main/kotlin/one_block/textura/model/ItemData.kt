package one_block.textura.model

enum class ItemType {
    FLAT, MODEL
}

data class ItemData(
    val id: String,
    val type: ItemType,
    val material: String,
    /** `-1` quando o item usa só `item_model` e não reserva um custom_model_data. */
    val customModelData: Int,
    val texturePath: String? = null,
    val modelPath: String? = null,
    /** Chave do componente `minecraft:item_model` no formato `namespace:path` (1.21.4+). */
    val itemModel: String? = null,
    /**
     * Chave onde a textura de [texturePath] é gravada no pack (ex.:
     * `oneblock:gui/button_left`). Sem ela, a textura vai para `minecraft:item/<id>`.
     * Usada por itens `model` cujo JSON referencia uma textura em namespace próprio.
     */
    val textureKey: String? = null,
    /**
     * `oversized_in_gui` da definição: deixa o item desenhar fora do quadrado do
     * slot. Obrigatório para modelos com `display.gui` de escala/translação grande.
     * `null` = decide pelo material (cabeças pedem por padrão).
     */
    val oversizedInGui: Boolean? = null,
    /** Nome exibido, em MiniMessage. Sem ele o item herda o nome do material base. */
    val name: String? = null,
    /** Lore, em MiniMessage, uma linha por entrada. */
    val lore: List<String> = emptyList(),
    /**
     * Transformações `display` do model, por perspectiva (`gui`, `head`, `fixed`,
     * `ground`, `thirdperson_righthand`, ...). Vazio = o modelo sai como está.
     * Assim dá para escalar/girar um item sem escrever um model JSON só pra isso.
     */
    val display: Map<String, DisplayTransform> = emptyMap(),
) {
    /** Só aplica `custom_model_data` quando definido (>= 0). */
    val hasCustomModelData: Boolean get() = customModelData >= 0

    private val guiScale: Vec3? get() = display["gui"]?.scale

    /**
     * Resolve [oversizedInGui] com o padrão do material. Um `gui` ampliado só
     * aparece inteiro fora do slot, então ele liga a flag sozinho.
     */
    val isOversizedInGui: Boolean
        get() = oversizedInGui ?: (material == "player_head" || (guiScale?.maxComponent() ?: 1f) > 1f)

    val itemModelNamespace: String?
        get() = itemModel?.let { if (it.contains(':')) it.substringBefore(':') else "minecraft" }

    val itemModelPath: String?
        get() = itemModel?.substringAfter(':')

    /** Namespace de [textureKey], ou `minecraft` quando ela vem sem namespace. */
    val textureNamespace: String? get() = textureKey?.let { if (it.contains(':')) it.substringBefore(':') else "minecraft" }

    /** Caminho de [textureKey] (`gui/button_left` em `oneblock:gui/button_left`). */
    val texturePathInPack: String? get() = textureKey?.substringAfter(':')
}
