package ocean_block.system.playtime_rewards.dialog

import ocean_block.system.playtime_rewards.PlaytimeRewards
import ocean_block.system.playtime_rewards.notify.RewardNotifier
import ocean_block.system.playtime_rewards.rules.MilestoneRules
import ocean_block.system.playtime_rewards.text.RewardText
import one_block.textura.api.DialogApi
import one_block.textura.api.DialogBinds
import one_block.textura.api.DialogOpenResult
import one_block.textura.api.TranslationApi
import org.bukkit.Material
import org.bukkit.entity.Player

object RewardDialog {

    private const val SCREEN = "recompensas"
    private const val PER_PAGE = 3

    fun registerIcons() {
        DialogApi.registerItemIcons((PlaytimeRewards.ladder.map { RewardRows.iconTexture(it.icon) } + RewardNotifier.ICON_MATERIAL).distinct())
    }

    fun open(player: Player): Boolean {
        val result = DialogApi.openResult(player, SCREEN) { binds -> bind(binds, player) }
        return result == DialogOpenResult.Shown || result == DialogOpenResult.NeedsPack
    }

    fun itemName(material: String): String =
        Material.matchMaterial(material)?.let { TranslationApi.translate(it.translationKey()) }
            ?: material.lowercase().replace('_', ' ').replaceFirstChar { it.uppercase() }

    private fun bind(binds: DialogBinds, player: Player) {
        val ladder = PlaytimeRewards.ladder
        val pages = maxOf(1, (ladder.size + PER_PAGE - 1) / PER_PAGE)
        var page = MilestoneRules.startPage(ladder, PlaytimeRewards.claimed(player), PER_PAGE)
        fun current(): Int = page.coerceIn(0, pages - 1)
        fun seconds(): Long = PlaytimeRewards.seconds(player) ?: 0L

        binds.text("tempo") { "Jogado: ${RewardText.played(seconds())}" }
        binds.text("pagina") { "${current() + 1}/$pages" }
        binds.text("sem_anterior") { (current() == 0).toString() }
        binds.text("sem_proxima") { (current() >= pages - 1).toString() }
        binds.text("sem_pendentes") { PlaytimeRewards.available(player).isEmpty().toString() }
        binds.list("marcos") {
            val seconds = seconds()
            val claimed = PlaytimeRewards.claimed(player)
            ladder.map { RewardRows.row(it, seconds, claimed, ::itemName) }
        }
        binds.listPage("marcos") { current() }
        binds.listButton("resgatar") { click, index -> ladder.getOrNull(index)?.let { PlaytimeRewards.claim(click.player, it.id) } }
        binds.button("resgatar_tudo") { click -> PlaytimeRewards.claimAll(click.player) }
        binds.button("anterior") { page = maxOf(0, current() - 1) }
        binds.button("proxima") { page = minOf(pages - 1, current() + 1) }
    }
}
