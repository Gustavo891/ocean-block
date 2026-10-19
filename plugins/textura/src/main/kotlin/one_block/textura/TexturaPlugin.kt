package one_block.textura

import me.devnatan.inventoryframework.ViewFrame
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.event.Listener
import org.bukkit.plugin.java.JavaPlugin
import one_block.textura.api.BlockApi
import one_block.textura.api.GlyphApi
import one_block.textura.api.HudApi
import one_block.textura.api.ItemApi
import one_block.textura.api.TitleApi
import one_block.textura.block.NoteBlockAllocator
import one_block.textura.registry.BlockRegistry
import one_block.textura.command.HudCommand
import one_block.textura.command.TexturaCommand
import one_block.textura.hud.HudManager
import one_block.textura.hud.anchor.HudAnchorProbe
import one_block.textura.http.ResourcePackServer
import one_block.textura.listener.ChatListener
import one_block.textura.menu.BlocksMenu
import one_block.textura.menu.ItemsMenu
import one_block.textura.registry.FontRegistry
import one_block.textura.registry.GlyphRegistry
import one_block.textura.registry.ItemRegistry
import one_block.textura.registry.TitleFontRegistry
import one_block.textura.pack.PackAssets
import one_block.textura.pack.PackBuilder
import one_block.textura.pack.ResourcePackBuilder
import one_block.textura.pack.PackHash
import one_block.textura.api.DialogApi
import one_block.textura.dialog.DialogOpener
import one_block.textura.dialog.PlayerSettings
import one_block.textura.dialog.kit.BaseUiKit
import one_block.textura.dialog.kit.UiArtCatalog
import one_block.textura.dialog.kit.SurfaceSize
import one_block.textura.dialog.kit.UiKit
import one_block.textura.dialog.kit.UiTextFont
import one_block.textura.dialog.kit.UiTextFontLoader
import one_block.textura.dialog.kit.UiUsage
import one_block.textura.listener.PackStatusTracker
import one_block.textura.pack.UiPackWriter
import one_block.textura.pack.VanillaAssets
import one_block.textura.registry.UiScreenRegistry
import one_block.textura.registry.UiThemeRegistry
import co.aikar.commands.PaperCommandManager
import net.kyori.adventure.resource.ResourcePackInfo
import net.kyori.adventure.resource.ResourcePackRequest
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import java.io.File
import java.io.IOException
import java.net.URI
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import java.util.logging.Level

data class PackRelease(val url: String, val hash: String)

class TexturaPlugin : JavaPlugin(), Listener {

    lateinit var glyphRegistry: GlyphRegistry
        private set
    lateinit var fontRegistry: FontRegistry
        private set
    lateinit var titleFontRegistry: TitleFontRegistry
        private set
    lateinit var itemRegistry: ItemRegistry
        private set
    lateinit var blockRegistry: BlockRegistry
        private set
    lateinit var blockAllocator: NoteBlockAllocator
        private set
    lateinit var viewFrame: me.devnatan.inventoryframework.ViewFrame
        private set
    lateinit var uiScreenRegistry: UiScreenRegistry
        private set
    lateinit var uiThemeRegistry: UiThemeRegistry
        private set
    @Volatile
    var uiKit: UiKit? = null
        private set
    private lateinit var dialogOpener: DialogOpener
    private val packStatus = PackStatusTracker(PACK_ID) { dialogOpener.forget(it) }
    private var server: ResourcePackServer? = null
    private var localUrl: String = ""
    @Volatile
    private var release: PackRelease? = null
    private val generating = AtomicBoolean(false)

    val isGenerating: Boolean get() = generating.get()

    override fun onEnable() {
        saveDefaultConfig()

        glyphRegistry = GlyphRegistry(logger)
        GlyphApi.init(glyphRegistry)
        fontRegistry = FontRegistry()
        titleFontRegistry = TitleFontRegistry()
        TitleApi.init(titleFontRegistry)
        HudAnchorProbe.registerFonts()
        itemRegistry = ItemRegistry(logger)
        ItemApi.init(itemRegistry)
        blockRegistry = BlockRegistry(logger)
        blockAllocator = NoteBlockAllocator(blockAllocationsFile(), logger)
        BlockApi.init(blockRegistry, blockAllocator)
        uiScreenRegistry = UiScreenRegistry(logger)
        uiThemeRegistry = UiThemeRegistry(logger)

        File(dataFolder, "configs/glyphs").mkdirs()
        if (!File(dataFolder, "configs/glyphs/emojis.yml").exists()) {
            saveResource("configs/glyphs/emojis.yml", false)
        }
        File(dataFolder, "configs/fonts").mkdirs()
        File(dataFolder, "generated").mkdirs()
        File(dataFolder, "data").mkdirs()
        File(dataFolder, "external").mkdirs()
        File(dataFolder, "configs/items").mkdirs()
        File(dataFolder, "configs/blocks").mkdirs()
        File(dataFolder, "configs/ui").mkdirs()
        if (!File(dataFolder, "configs/items/exemplo.yml").exists()) {
            saveResource("configs/items/exemplo.yml", false)
        }

        reloadConfigData()

        val lobfileOn = config.getBoolean("host.lobfile.enabled", false)
        val port = config.getInt("host.porta", 8080)
        val host = config.getString("host.ip", "127.0.0.1")
        val publicUrl = config.getString("host.url", "")?.trim().orEmpty()
        localUrl = publicUrl.ifEmpty { "http://$host:$port/resourcepack.zip" }

        if (lobfileOn) {
            // LobFile serve o pack; nao sobe o HTTP local. packUrl vem do upload.
            logger.info("LobFile habilitado — servidor HTTP local desativado.")
        } else {
            server = ResourcePackServer(port, config.getString("host.bind")?.trim()?.takeIf { it.isNotEmpty() })
            try {
                server?.start()
            } catch (e: IOException) {
                logger.log(Level.SEVERE, "Falha ao iniciar servidor HTTP", e)
            }
        }

        viewFrame = ViewFrame.create(this)
            .with(ItemsMenu(itemRegistry))
            .with(BlocksMenu(blockRegistry, blockAllocator))
            .register()

        val hudManager = HudManager()
        HudApi.init(hudManager)

        dialogOpener = DialogOpener(this, { uiKit }, uiScreenRegistry, uiThemeRegistry, packStatus, ::applyPack, PlayerSettings(this))
        DialogApi.init(dialogOpener)

        val commandManager = PaperCommandManager(this)
        commandManager.registerCommand(TexturaCommand(this))
        commandManager.registerCommand(HudCommand())
        commandManager.commandCompletions.registerCompletion("uiscreens") { uiScreenRegistry.screens.keys }

        Bukkit.getPluginManager().registerEvents(this, this)
        Bukkit.getPluginManager().registerEvents(ChatListener(this), this)
        Bukkit.getPluginManager().registerEvents(one_block.textura.listener.NoteBlockGuard(), this)
        Bukkit.getPluginManager().registerEvents(one_block.textura.listener.CustomBlockListener(logger), this)
        Bukkit.getPluginManager().registerEvents(hudManager, this)
        Bukkit.getPluginManager().registerEvents(HudAnchorProbe, this)
        Bukkit.getPluginManager().registerEvents(packStatus, this)
        Bukkit.getPluginManager().registerEvents(dialogOpener, this)

        Bukkit.getScheduler().runTask(this, Runnable {
            generatePack(Bukkit.getConsoleSender())
        })

        logger.info("Modulo ResourcePack iniciado com sucesso!")
    }

    override fun onDisable() {
        server?.stop()
        logger.info("Modulo ResourcePack desligado.")
    }

    fun reloadConfigData() {
        reloadConfig()
        val assets = PackAssets(dataFolder)
        glyphRegistry.load(assets.configFolder("glyphs"), assets)
        fontRegistry.load(assets.configFolder("fonts"))
        itemRegistry.load(assets.configFolder("items"))
        blockRegistry.load(assets.configFolder("blocks"))
        blockAllocator.sync(blockRegistry.customIds)
        blockAllocator.save()
        one_block.textura.block.VanillaRecipeRemover.apply(blockRegistry, logger)
        uiThemeRegistry.load(assets.configFolder("ui"))
        uiScreenRegistry.load(assets.configFolder("ui"))
        auditUi()
    }

    fun uiCatalog(download: Boolean = false): UiArtCatalog {
        val assets = PackAssets(dataFolder)
        val glyphsFolder = assets.configFolder("glyphs")
        val vanilla = VanillaAssets(File(dataFolder, VANILLA_FOLDER), Bukkit.getMinecraftVersion(), logger)
        return UiArtCatalog(File(assets.resourceRoot, UI_ART_FOLDER), { id ->
            glyphRegistry.glyphs[id]?.takeIf { it.sheetKey == null }?.let { assets.resolveGlyph(it.caminho, glyphsFolder)?.file }
        }, logger) { name -> vanilla.itemTexture(name, download) }
    }

    fun uiTextFont(): UiTextFont {
        val assets = PackAssets(dataFolder)
        val json = File(assets.configFolder("fonts"), config.getString("ui.fonte-texto") ?: DEFAULT_UI_TEXT_FONT)
        return UiTextFontLoader.load(json, assets.resourceRoot, logger)
    }

    private fun auditUi() {
        val kit = uiKit ?: return
        val usage = UiUsage.collect(uiScreenRegistry.screens.values, uiThemeRegistry.themes, BaseUiKit(uiCatalog(), uiTextFont())) { }
        val missing = usage.surfaces.filter { kit.surface(SurfaceSize(it.id, 0, it.height)) == null }.map { "surface ${it.id} altura ${it.height}" } +
            usage.icons.filter { kit.icon(it) == null }.map { "ícone ${it.id} ${it.size}px" } +
            usage.images.filter { !kit.has(it) }.map { "imagem $it" }
        if (missing.isNotEmpty()) {
            logger.warning("[ui] Fora do pack atual (rode /textura gerar): ${missing.joinToString(", ")}")
        }
    }

    fun generatePack(sender: CommandSender) {
        if (!generating.compareAndSet(false, true)) {
            sender.sendMessage("§eJa existe uma geracao em andamento. Aguarde.")
            return
        }
        Bukkit.getScheduler().runTaskAsynchronously(this, Runnable {
            try {
                val ui = UiPackWriter(uiCatalog(download = true), uiTextFont(), uiScreenRegistry.screens.values, uiThemeRegistry.themes, logger, config.getBoolean("ui.shader", true))
                val builder = ResourcePackBuilder(
                    glyphRegistry, fontRegistry, titleFontRegistry, itemRegistry,
                    blockRegistry, blockAllocator, dataFolder, logger,
                    config.getInt("pack.formato", PackBuilder.DEFAULT_PACK_FORMAT),
                    ui,
                )
                val built = builder.build()
                val zip = built.zip
                val hash = PackHash.calculateSHA1(zip)
                val previous = release

                val url = when {
                    !config.getBoolean("host.lobfile.enabled", false) -> localUrl
                    previous != null && previous.hash == hash -> previous.url
                    else -> uploadToLobFile(zip, sender)
                }

                if (url != null) {
                    server?.publish(zip.readBytes(), hash)
                    release = PackRelease(url, hash)
                    uiKit = built.uiKit
                    if (previous?.hash != hash) {
                        packStatus.reset()
                        Bukkit.getScheduler().runTask(this, Runnable { Bukkit.getOnlinePlayers().forEach(::applyPack) })
                    }
                }

                Bukkit.getScheduler().runTask(this, Runnable {
                    if (url != null) {
                        sender.sendMessage("§aResource Pack gerado com sucesso.")
                    } else {
                        sender.sendMessage("§eZip gerado, mas o pack nao foi publicado; os jogadores seguem com o anterior. Veja o console.")
                    }
                    sender.sendMessage("§7Glyphs: ${glyphRegistry.glyphs.size}")
                    sender.sendMessage("§7Fonts: ${fontRegistry.fonts.size}")
                    sender.sendMessage("§7Items: ${itemRegistry.items.size}")
                    sender.sendMessage("§7Blocos: ${blockRegistry.blocks.size}")
                    sender.sendMessage("§7Telas de UI: ${uiScreenRegistry.screens.size}")
                    sender.sendMessage("§7Hash: $hash")
                })
            } catch (e: Exception) {
                logger.log(Level.SEVERE, "Erro ao gerar resource pack", e)
                Bukkit.getScheduler().runTask(this, Runnable {
                    sender.sendMessage("§cErro ao gerar resource pack: ${e.message}")
                })
            } finally {
                generating.set(false)
            }
        })
    }

    private fun uploadToLobFile(zip: File, sender: CommandSender): String? {
        val apiKey = System.getenv(LOBFILE_KEY_ENV)?.trim()?.takeIf { it.isNotEmpty() }
            ?: config.getString("host.lobfile.api-key", "").orEmpty().trim()
        if (apiKey.isEmpty()) {
            logger.warning("LobFile habilitado mas sem api-key ($LOBFILE_KEY_ENV ou host.lobfile.api-key).")
            Bukkit.getScheduler().runTask(this, Runnable {
                sender.sendMessage("§cLobFile: defina a variavel de ambiente $LOBFILE_KEY_ENV.")
            })
            return null
        }

        return runCatching { one_block.textura.http.LobFileUploader.upload(zip, apiKey) }
            .fold(
                onSuccess = { url ->
                    logger.info("Pack enviado ao LobFile: $url")
                    Bukkit.getScheduler().runTask(this, Runnable {
                        sender.sendMessage("§aPack no LobFile: §7$url")
                    })
                    url
                },
                onFailure = { ex ->
                    logger.log(Level.SEVERE, "Upload LobFile falhou", ex)
                    Bukkit.getScheduler().runTask(this, Runnable {
                        sender.sendMessage("§cUpload LobFile falhou: ${ex.message}")
                    })
                    null
                }
            )
    }

    fun applyPack(p: Player) {
        val current = release
        if (current == null) {
            if (p.hasPermission(ADMIN_PERMISSION)) {
                p.sendMessage("§cResource pack ainda nao foi gerado. Use /textura gerar.")
            }
            return
        }
        val info = ResourcePackInfo.resourcePackInfo(PACK_ID, URI.create(current.url), current.hash)
        p.sendResourcePacks(
            ResourcePackRequest.resourcePackRequest()
                .packs(info)
                .required(true)
                .replace(true)
                .prompt(Component.text("Este servidor usa uma textura própria. Aceite para continuar jogando.", NamedTextColor.YELLOW))
                .build()
        )
    }

    @org.bukkit.event.EventHandler
    fun onPlayerJoin(event: org.bukkit.event.player.PlayerJoinEvent) {
        applyPack(event.player)
    }

    /**
     * O mapa bloco -> variante de note_block e estado, nao artefato: se ele sumir, os
     * blocos custom ja colocados no mundo trocam de aparencia. Por isso mora em
     * `data/`, fora de `generated/`, que pode ser apagado a qualquer momento.
     */
    private fun blockAllocationsFile(): File {
        val destino = File(dataFolder, "data/block-allocations.yml")
        if (destino.isFile) return destino

        destino.parentFile.mkdirs()
        val legado = File(dataFolder, "generated/block-allocations.yml")
        if (legado.isFile && legado.renameTo(destino)) {
            logger.info("block-allocations.yml movido de generated/ para data/.")
        }
        return destino
    }

    private companion object {
        const val LOBFILE_KEY_ENV = "TEXTURA_LOBFILE_API_KEY"
        const val ADMIN_PERMISSION = "textura.admin"
        val PACK_ID: UUID = UUID.nameUUIDFromBytes("textura:resourcepack".toByteArray(Charsets.UTF_8))
        const val UI_ART_FOLDER = "assets/oneblock/textures/ui"
        const val VANILLA_FOLDER = "generated/vanilla"
        const val DEFAULT_UI_TEXT_FONT = "custom.json"
    }
}
