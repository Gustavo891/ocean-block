package one_block.textura.pack

import one_block.textura.block.NoteBlockAllocator
import one_block.textura.registry.BlockRegistry
import one_block.textura.registry.FontRegistry
import one_block.textura.registry.GlyphRegistry
import one_block.textura.registry.ItemRegistry
import one_block.textura.registry.TitleFontRegistry
import team.unnamed.creative.base.Writable
import java.io.File
import java.util.logging.Logger
import one_block.textura.dialog.kit.UiKit
import one_block.textura.notify.render.NotificationKit
import java.nio.file.Files
import java.nio.file.StandardCopyOption

data class BuiltPack(val zip: File, val uiKit: UiKit?, val notificationKit: NotificationKit?)

/**
 * Monta o resource pack a partir dos registries e da pasta de dados.
 *
 * Só orquestra: cada domínio tem seu escritor ([FontPackWriter], [ItemPackWriter],
 * [BlockPackWriter]), e quem conhece a API do Creative é o [PackBuilder].
 */
class ResourcePackBuilder(
    glyphRegistry: GlyphRegistry,
    fontRegistry: FontRegistry,
    titleFontRegistry: TitleFontRegistry,
    itemRegistry: ItemRegistry,
    blockRegistry: BlockRegistry,
    allocator: NoteBlockAllocator,
    private val dataFolder: File,
    private val logger: Logger,
    private val packFormat: Int = PackBuilder.DEFAULT_PACK_FORMAT,
    private val ui: UiPackWriter,
    private val notifications: NotificationPackWriter,
) {
    private val generatedFolder = File(dataFolder, "generated")
    private val assets = PackAssets(dataFolder)

    private val fonts = FontPackWriter(glyphRegistry, fontRegistry, titleFontRegistry, assets, dataFolder, logger)
    private val items = ItemPackWriter(itemRegistry, assets, logger)
    private val blocks = BlockPackWriter(blockRegistry, allocator, assets, logger)

    fun build(): BuiltPack {
        val builder = PackBuilder(packFormat)

        // Tudo que está em resource/ entra no pack exatamente onde está. Vem antes
        // dos escritores para que eles apenas anexem providers/definições.
        addResourceFiles(builder)

        fonts.write(builder)
        items.write(builder)
        blocks.write(builder)
        blocks.writeLangOverrides(builder)
        val uiKit = runCatching { ui.write(builder) }.getOrElse {
            logger.log(java.util.logging.Level.SEVERE, "[ui] Falha ao gerar a UI de dialog, o resto do pack segue", it)
            null
        }
        val notificationKit = runCatching { notifications.write(builder) }.getOrElse {
            logger.log(java.util.logging.Level.SEVERE, "[notificacoes] Falha ao gerar as fontes de notificação, o resto do pack segue", it)
            null
        }
        mergeExternalPacks(builder)

        val target = File(generatedFolder, ZIP_NAME)
        val temp = File(generatedFolder, "$ZIP_NAME.tmp")
        builder.build(temp)
        Files.move(temp.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
        return BuiltPack(target, uiKit, notificationKit)
    }

    /**
     * Copia `resource/` para dentro do pack sem traduzir nada: o caminho do
     * arquivo na pasta é o caminho dele no zip. Os langs ficam de fora quando
     * algum bloco renomeia itens, porque nesse caso eles são mesclados depois.
     */
    private fun addResourceFiles(builder: PackBuilder) {
        val langsSobrescritos = if (blocks.temOverrideDeNome()) {
            assets.langFiles().map { it.packPath }.toSet()
        } else {
            emptySet()
        }

        val files = assets.resourceFiles()
            .filterNot { it.packPath in langsSobrescritos || it.packPath.endsWith(SLICE_EXTENSION) }
        for (asset in files) {
            builder.pack.unknownFile(asset.packPath, Writable.file(asset.file))
        }
        if (files.isNotEmpty()) {
            logger.info("${files.size} arquivo(s) de resource/ copiado(s) para o pack.")
        }
    }

    /** Mescla os resource packs da pasta `external/` sobre o pack principal. */
    private fun mergeExternalPacks(builder: PackBuilder) {
        val merged = ResourcePackMerger(File(dataFolder, "external"), logger, packFormat).mergeInto(builder.pack)
        if (merged > 0) {
            logger.info("$merged resource pack(s) externo(s) mesclado(s).")
        }
    }

    private companion object {
        const val SLICE_EXTENSION = ".slice"
        const val ZIP_NAME = "resourcepack.zip"
    }
}
