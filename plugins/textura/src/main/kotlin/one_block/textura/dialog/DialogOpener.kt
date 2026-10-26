package one_block.textura.dialog

import io.papermc.paper.connection.PlayerGameConnection
import io.papermc.paper.dialog.Dialog
import io.papermc.paper.event.player.PlayerCustomClickEvent
import io.papermc.paper.registry.data.dialog.ActionButton
import io.papermc.paper.registry.data.dialog.DialogBase
import io.papermc.paper.registry.data.dialog.action.DialogAction
import io.papermc.paper.registry.data.dialog.body.DialogBody
import io.papermc.paper.registry.data.dialog.type.DialogType
import net.kyori.adventure.key.Key
import net.kyori.adventure.nbt.api.BinaryTagHolder
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.JoinConfiguration
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.format.NamedTextColor
import one_block.textura.api.DialogBinds
import one_block.textura.api.DialogOpenResult
import one_block.textura.api.TexturaTags
import one_block.textura.dialog.json.ScreenCompiler
import one_block.textura.dialog.json.ScreenDefinition
import one_block.textura.dialog.kit.UiKit
import one_block.textura.dialog.layout.UiLayout
import one_block.textura.dialog.node.UiAction
import one_block.textura.dialog.node.UiOutcome
import one_block.textura.dialog.render.UiRenderer
import one_block.textura.listener.PackStatusTracker
import one_block.textura.registry.UiScreenRegistry
import one_block.textura.registry.UiThemeRegistry
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.plugin.java.JavaPlugin
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import java.util.logging.Level

class DialogOpener(
    private val plugin: JavaPlugin,
    private val kit: () -> UiKit?,
    private val screens: UiScreenRegistry,
    private val themes: UiThemeRegistry,
    private val packStatus: PackStatusTracker,
    private val applyPack: (Player) -> Unit,
    private val settings: PlayerSettings,
) : Listener {

    private class Current(val token: Long, val session: DialogSession, val actions: List<UiAction>)

    private val counter = AtomicLong()
    private val current = ConcurrentHashMap<UUID, Current>()
    private val warned = ConcurrentHashMap.newKeySet<String>()

    @Volatile
    private var warnedKit: UiKit? = null

    fun open(player: Player, screenId: String, values: Map<String, String>, binds: DialogBinds): DialogOpenResult {
        if (!Bukkit.isPrimaryThread()) {
            Bukkit.getScheduler().runTask(plugin, Runnable { if (player.isOnline) open(player, screenId, values, binds) })
            return DialogOpenResult.Shown
        }
        return show(player, DialogSession(screenId, binds, values, player, settings))
    }

    fun setting(player: Player, key: String): String? = settings.get(player, key)

    fun setSetting(player: Player, key: String, value: String) {
        settings.set(player, key, value)
    }

    fun close(player: Player) {
        if (!Bukkit.isPrimaryThread()) {
            Bukkit.getScheduler().runTask(plugin, Runnable { close(player) })
            return
        }
        current.remove(player.uniqueId)
        player.closeDialog()
    }

    fun forget(id: UUID) {
        current.remove(id)
    }

    fun refresh(player: Player, screenId: String): Boolean {
        val state = current[player.uniqueId]?.takeIf { it.session.screenId == screenId } ?: return false
        return show(player, state.session) == DialogOpenResult.Shown
    }

    private fun show(player: Player, session: DialogSession): DialogOpenResult {
        val definition = screens.screens[session.screenId] ?: run {
            plugin.logger.warning("[ui] Tela '${session.screenId}' não existe")
            return DialogOpenResult.MissingScreen
        }
        val theme = themes.themes[definition.theme] ?: run {
            plugin.logger.warning("[ui:${definition.id}] Tema '${definition.theme}' não existe")
            return DialogOpenResult.Failed
        }
        session.persistent = definition.controlBinds
        session.defaults = definition.defaults
        session.choices = definition.choices
        val requested = session.page
        if (requested != null && definition.pages.isNotEmpty() && requested !in definition.pages) {
            plugin.logger.warning("[ui:${definition.id}] Página '$requested' não existe")
            session.page = null
        }
        if (session.page == null) session.page = definition.firstPage
        val snapshot = kit() ?: run {
            player.sendMessage(Component.text("Interface indisponível agora, tente de novo em instantes.", NamedTextColor.RED))
            return DialogOpenResult.NeedsPack
        }
        if (!packStatus.loaded(player)) {
            player.showDialog(fallback())
            return DialogOpenResult.NeedsPack
        }

        val warn = warner(snapshot, definition.id)
        checkBinds(definition, session, warn)

        val token = counter.incrementAndGet()
        val actions = mutableListOf<UiAction>()
        val rendered = runCatching {
            val compiler = ScreenCompiler(snapshot, warn)
            val screen = compiler.compile(definition, theme, session)
            val renderer = UiRenderer(snapshot, { action -> actions += action; click("$token:${actions.lastIndex}") }, warn)
            val lines = renderer.render(UiLayout(snapshot).layout(screen))
            val body = DialogBody.plainMessage(Component.join(JoinConfiguration.newlines(), lines), UiRenderer.dialogWidth(screen.width))
            dialog(definition, compiler.title(definition, session), body)
        }.getOrElse {
            plugin.logger.log(Level.SEVERE, "[ui:${definition.id}] Falha ao montar a tela", it)
            return DialogOpenResult.Failed
        }

        current[player.uniqueId] = Current(token, session, actions.toList())
        player.showDialog(rendered)
        return DialogOpenResult.Shown
    }

    @EventHandler
    fun onCustomClick(event: PlayerCustomClickEvent) {
        if (event.identifier != CLICK_KEY) return
        val player = (event.commonConnection as? PlayerGameConnection)?.player ?: return
        val payload = event.tag?.string().orEmpty()

        Bukkit.getScheduler().runTask(plugin, Runnable {
            if (player.isOnline) handle(player, payload)
        })
    }

    private fun handle(player: Player, payload: String) {
        when {
            CLOSE in payload -> close(player)
            APPLY in payload -> applyPack(player)
            else -> {
                val match = ACTION.find(payload) ?: return
                val token = match.groupValues[1].toLongOrNull() ?: return
                val index = match.groupValues[2].toIntOrNull() ?: return
                val state = current[player.uniqueId]?.takeIf { it.token == token } ?: return
                val action = state.actions.getOrNull(index) ?: return
                if (!current.remove(player.uniqueId, state)) return
                run(player, state.session, action)
            }
        }
    }

    private fun run(player: Player, session: DialogSession, action: UiAction) {
        val before = current[player.uniqueId]
        val outcome = runCatching { action.run(player) }.getOrElse {
            plugin.logger.log(Level.SEVERE, "[ui:${session.screenId}] Erro no handler de um controle", it)
            UiOutcome.Refresh
        }
        if (current[player.uniqueId] !== before) return
        when (outcome) {
            UiOutcome.Refresh -> show(player, session)
            UiOutcome.Close -> close(player)
            is UiOutcome.Page -> {
                val definition = screens.screens[session.screenId]
                if (definition != null && definition.pages.isNotEmpty() && outcome.id !in definition.pages) {
                    plugin.logger.warning("[ui:${session.screenId}] Página '${outcome.id}' não existe")
                } else {
                    session.page = outcome.id
                }
                show(player, session)
            }
            is UiOutcome.Open -> show(player, DialogSession(outcome.screenId, outcome.binds, session.carried() + outcome.values, player, settings))
        }
    }

    private fun checkBinds(definition: ScreenDefinition, session: DialogSession, warn: (String) -> Unit) {
        (session.binds.keys - definition.keys).forEach { warn("bind '$it' fornecido pelo código não existe na tela") }
    }

    private fun warner(snapshot: UiKit, screenId: String): (String) -> Unit {
        if (warnedKit !== snapshot) {
            warned.clear()
            warnedKit = snapshot
        }
        return { message -> if (warned.add("$screenId|$message")) plugin.logger.warning("[ui:$screenId] $message") }
    }

    private fun click(payload: String): ClickEvent<*> = ClickEvent.custom(CLICK_KEY, BinaryTagHolder.binaryTagHolder("\"$payload\""))

    private fun dialog(definition: ScreenDefinition, title: Component?, body: DialogBody): Dialog = Dialog.create { factory ->
        val base = DialogBase.builder(title ?: Component.empty())
            .canCloseWithEscape(definition.escape || definition.close == null)
            .pause(definition.pause)
            .afterAction(DialogBase.DialogAfterAction.NONE)
            .body(listOf(body))
            .build()
        val close = definition.close?.let { label ->
            ActionButton.builder(TexturaTags.miniMessage.deserialize(label))
                .width(BUTTON_WIDTH)
                .action(DialogAction.staticAction(click(CLOSE)))
                .build()
        }
        factory.empty().base(base).type(if (close != null) DialogType.notice(close) else DialogType.notice())
    }

    private fun fallback(): Dialog = Dialog.create { factory ->
        val base = DialogBase.builder(Component.text("Textura necessária", NamedTextColor.YELLOW))
            .canCloseWithEscape(true)
            .body(listOf(DialogBody.plainMessage(Component.text("Aceite a textura do servidor para ver esta tela.", NamedTextColor.GRAY))))
            .build()
        val apply = ActionButton.builder(Component.text("Aplicar textura", NamedTextColor.GREEN))
            .width(BUTTON_WIDTH)
            .action(DialogAction.staticAction(click(APPLY)))
            .build()
        factory.empty().base(base).type(DialogType.notice(apply))
    }

    private companion object {
        const val BUTTON_WIDTH = 150
        const val CLOSE = "close"
        const val APPLY = "apply"
        val CLICK_KEY: Key = Key.key("textura", "ui")
        val ACTION = Regex("""(\d+):(\d+)""")
    }
}
