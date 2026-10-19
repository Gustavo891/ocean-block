package one_block.textura.api

import org.bukkit.entity.Player
import one_block.textura.hud.HudContent
import one_block.textura.hud.HudManager
import one_block.textura.hud.HudSecondary
import one_block.textura.hud.HudSide

/**
 * API pública do sistema de HUD (BossBar desenhada por glyphs). Inicializada em
 * `TexturaPlugin.onEnable` via [init]; outros plugins consomem por aqui.
 *
 * O estado por jogador vive no [HudManager]; esta fachada só delega. Enquanto a
 * API não estiver inicializada, todas as chamadas são no-op silencioso.
 */
object HudApi {

    @Volatile
    private var manager: HudManager? = null

    internal fun init(manager: HudManager) {
        this.manager = manager
    }

    /** Cria ou substitui a HUD do [slot] e a exibe ao jogador. */
    fun show(player: Player, slot: String, content: HudContent) {
        manager?.show(player, slot, content)
    }

    /** Atalho de [show] para o caso comum de só texto + alinhamento. */
    fun show(player: Player, slot: String, text: String) {
        manager?.show(player, slot, HudContent(text))
    }

    /** Atualiza a HUD do [slot] se existir; caso contrário, no-op. */
    fun update(player: Player, slot: String, content: HudContent) {
        manager?.update(player, slot, content)
    }

    /**
     * Define a HUD secundária do [slot] — uma barra menor na *mesma* linha, colada
     * à esquerda ou à direita da principal, que continua centralizada.
     *
     * No-op enquanto o jogador não tiver uma HUD principal nesse slot.
     */
    fun showSecondary(player: Player, slot: String, secondary: HudSecondary) {
        manager?.showSecondary(player, slot, secondary)
    }

    /** Atalho de [showSecondary] para o caso comum de só texto + lado. */
    fun showSecondary(player: Player, slot: String, text: String) {
        showSecondary(player, slot, text, HudSide.RIGHT)
    }

    fun showSecondary(player: Player, slot: String, text: String, side: HudSide) {
        manager?.showSecondary(player, slot, HudSecondary(text, side))
    }

    /** Remove a HUD secundária do [slot], mantendo a principal. */
    fun hideSecondary(player: Player, slot: String) {
        manager?.hideSecondary(player, slot)
    }

    /** `true` se o [slot] tem uma HUD secundária ativa. */
    fun hasSecondary(player: Player, slot: String): Boolean =
        manager?.hasSecondary(player, slot) == true

    /** Remove a HUD do [slot]. */
    fun hide(player: Player, slot: String) {
        manager?.hide(player, slot)
    }

    /** Remove todas as HUDs do jogador. */
    fun hideAll(player: Player) {
        manager?.hideAll(player)
    }

    /** `true` se o jogador tem uma HUD ativa no [slot]. */
    fun has(player: Player, slot: String): Boolean =
        manager?.has(player, slot) == true
}
