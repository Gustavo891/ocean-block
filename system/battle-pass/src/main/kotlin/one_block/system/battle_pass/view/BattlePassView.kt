package one_block.system.battle_pass.view

import me.devnatan.inventoryframework.View
import me.devnatan.inventoryframework.ViewConfigBuilder
import me.devnatan.inventoryframework.context.RenderContext
import one_block.libs.item_builder.itemStack
import one_block.libs.item_builder.meta
import one_block.libs.item_builder.name
import one_block.libs.item_builder.setLore
import one_block.system.battle_pass.player.PlayerPassData
import one_block.system.battle_pass.player.BattlePassManager
import one_block.system.battle_pass.config.BattlePassTier
import one_block.system.battle_pass.util.MessageUtil
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta

class BattlePassView : View() {

    private val pageState = initialState<Int>()

    companion object {
        private const val TIERS_PER_PAGE = 7
    }

    override fun onInit(config: ViewConfigBuilder) {
        config.cancelInteractions()
        config.title("Passe de Batalha")
        config.layout(
            "         ",
            "         ",
            "<       >",
            "         ",
            "         ",
            "    I  M "
        )
    }

    override fun onFirstRender(render: RenderContext) {
        val page = pageState.get(render) ?: 0
        val player = render.player
        val bp = BattlePassManager.get(player.uniqueId)
        val tiers = BattlePassTier.ordered.filter { it.hasAnyReward }
        val totalPages = if (tiers.isEmpty()) 1 else (tiers.size + TIERS_PER_PAGE - 1) / TIERS_PER_PAGE

        render.layoutSlot('I').renderWith { headerItem(bp) }

        render.layoutSlot('M').renderWith { challengesShortcut() }
            .onClick { _ ->
                render.openForPlayer(ChallengesView::class.java)
            }

        val startIdx = page * TIERS_PER_PAGE
        val pageTiers = tiers.drop(startIdx).take(TIERS_PER_PAGE)

        pageTiers.forEachIndexed { i, tier ->
            val col = i + 2

            if (tier.freeIcon != Material.AIR) {
                render.slot(2, col).renderWith { rewardSlotItem(player, bp, tier, premium = false) }
                    .onClick { ctx ->
                        if (BattlePassManager.claimFree(player, tier.tier)) {
                            player.playSound(player, Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f)
                            player.sendRichMessage(
                                MessageUtil.REWARD_CLAIMED,
                                Placeholder.parsed("tier", tier.tier.toString()),
                            )
                            ctx.parent.update()
                        }
                    }
            }

            render.slot(3, col).renderWith { tierIndicatorItem(tier, bp) }

            if(tier.premiumIcon != Material.AIR) {
                render.slot(4, col).renderWith { rewardSlotItem(player, bp, tier, premium = true) }
                    .onClick { ctx ->
                        val canClaim = BattlePassManager.claimPremium(player, tier.tier)
                        if (canClaim) {
                            player.playSound(player, Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.2f)
                            player.sendRichMessage(
                                MessageUtil.REWARD_CLAIMED_VIP,
                                Placeholder.parsed("tier", tier.tier.toString()),
                            )
                            ctx.parent.update()
                        } else if (!BattlePassManager.isPremium(player)) {
                            player.sendRichMessage(MessageUtil.VIP_REQUIRED)
                        }
                    }
            }
        }

        if (page > 0) {
            render.layoutSlot('<').renderWith { navItem("Voltar") }
                .onClick { ->
                    render.openForPlayer(BattlePassView::class.java, page - 1)
                }
        }
        if (page < totalPages - 1) {
            render.layoutSlot('>').renderWith { navItem("Próxima") }
                .onClick { ->
                    render.openForPlayer(BattlePassView::class.java, page + 1)
                }
        }
    }

    private fun headerItem(bp: PlayerPassData): ItemStack {
        val (currentTier, into, totalForNext) = BattlePassTier.xpProgress(bp.xp)

        return itemStack(Material.NETHER_STAR) {
            meta<ItemMeta> {
                name("<gold>Status:")
                setLore {
                    +"<gray>Veja as informações do"
                    +"<gray>seu passe de batalha."
                    +""
                    +"  <white>Nível: <yellow>$currentTier"
                    +"  <white>XP: <aqua>${bp.xp}"
                    +""
                }
            }
        }
    }

    private fun challengesShortcut() = itemStack(Material.BOOK) {
        meta<ItemMeta> {
            name("<gold>Missões")
            setLore {
                +"<gray>Complete missões para ganhar"
                +"<gray>XP e coletar as recompensas."
                +""
                +"<gold>Clique para acessá-las."
            }
        }
    }

    private fun tierIndicatorItem(tier: BattlePassTier, bp: PlayerPassData): ItemStack {
        val unlocked = BattlePassTier.fromXp(bp.xp) >= tier.tier
        val material = if (unlocked) Material.LIME_STAINED_GLASS_PANE else Material.GRAY_STAINED_GLASS_PANE

        return itemStack(material) {
            meta<ItemMeta> {
                name("${if (unlocked) "<green>" else "<red>"}Nível ${tier.tier}")
                setLore {
                    +"<white>XP Necessário: ${tier.xpRequired}"
                    +""
                    +(if (unlocked) "<green>Desbloqueado" else "<red>Bloqueado")
                }
            }
        }
    }

    private fun rewardSlotItem(player: Player, bp: PlayerPassData, tier: BattlePassTier, premium: Boolean): ItemStack {
        val rewardIcon = if (premium) tier.premiumIcon else tier.freeIcon
        
        if (rewardIcon == Material.AIR) {
            return itemStack(Material.GRAY_STAINED_GLASS_PANE) {
                meta<ItemMeta> { 
                    name("<gray>Sem recompensa ${if (premium) "Premium" else "Grátis"}")
                    setLore { +"<dark_gray>Este nível não possui recompensa neste slot." }
                }
            }
        }

        val unlocked = BattlePassTier.fromXp(bp.xp) >= tier.tier
        val claimedSet = if (premium) bp.claimedPremium else bp.claimedFree
        val claimed = tier.tier in claimedSet
        val isPremium = BattlePassManager.isPremium(player)
        val canClaim = unlocked && !claimed && (!premium || isPremium)
        val rewardLore = if (premium) tier.premiumLore else tier.freeLore

        val icon = when {
            canClaim -> Material.CHEST_MINECART
            claimed -> Material.MINECART
            else -> Material.TNT_MINECART
        }

        return itemStack(icon) {
            meta<ItemMeta> {
                name("${if (unlocked) "<green>" else "<red>"}Nível ${tier.tier}")

                setLore {
                    +""
                    +" <white>Recompensas:"
                    rewardLore.forEach { +" <dark_gray>▸<gray> $it" }
                    +""
                    when {
                        claimed -> +"<green>Você já coletou essa recompensa."
                        !unlocked -> +"<red>Recompensa bloqueada."
                        premium && !isPremium -> +"<red>Recompensa exclusiva para jogadores VIPs."
                        canClaim -> +"<green>Clique para coletar a recompensa."
                        else -> +"<red>Recompensa bloqueada."
                    }
                }
            }
        }
    }

    private fun navItem(label: String) = itemStack(Material.ARROW) {
        meta<ItemMeta> { name("<yellow>$label") }
    }
}
