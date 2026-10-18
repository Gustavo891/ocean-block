package one_block.system.battle_pass.view

import me.devnatan.inventoryframework.View
import me.devnatan.inventoryframework.ViewConfigBuilder
import me.devnatan.inventoryframework.context.OpenContext
import me.devnatan.inventoryframework.context.RenderContext
import one_block.libs.item_builder.itemStack
import one_block.libs.item_builder.meta
import one_block.libs.item_builder.name
import one_block.libs.item_builder.setLore
import one_block.system.battle_pass.challenge.ChallengeManager
import one_block.system.battle_pass.challenge.ChallengeManager.StartResult
import one_block.system.battle_pass.challenge.ProgressManager
import one_block.system.battle_pass.config.Challenges
import one_block.system.battle_pass.util.MessageUtil
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.Sound
import org.bukkit.enchantments.Enchantment
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import java.util.UUID
import one_block.libs.formatter.Mini

class ChallengesView : View() {

    private val pagination = lazyPaginationState(
        { ctx ->
            val playerUuid = ctx.player.uniqueId
            val activeId = ChallengeManager.getActiveChallenge(playerUuid)?.id

            Challenges.entries.sortedWith(
                compareBy<Challenges> { it.id == activeId }.reversed()
                    .thenBy { ProgressManager.getProgress(playerUuid, it.id)?.completed ?: false }
                    .thenBy { it.id }
            ).toMutableList()
        }
    ) { ctx, builder, _, challenge ->
        builder.renderWith { createChallengeItem(ctx.player.uniqueId, challenge) }
            .onClick { click ->
                val player = click.player
                val completed = ProgressManager
                    .getProgress(player.uniqueId, challenge.id)?.completed == true
                if (completed) return@onClick

                val active = ChallengeManager.getActiveChallenge(player.uniqueId)
                if (active?.id == challenge.id) {
                    ChallengeManager.cancelActiveChallenge(player)
                    player.playSound(player, Sound.UI_BUTTON_CLICK, 1f, 0.8f)
                    player.sendRichMessage(
                        MessageUtil.CHALLENGE_CANCELLED,
                        Placeholder.parsed("name", challenge.displayName),
                    )
                } else {
                    when (val result = ChallengeManager.startChallenge(player, challenge)) {
                        StartResult.Started -> {
                            player.playSound(player, Sound.UI_TOAST_IN, 1f, 1f)
                            player.sendRichMessage(
                                MessageUtil.CHALLENGE_STARTED,
                                Placeholder.parsed("name", challenge.displayName),
                            )
                        }

                        StartResult.AlreadyCompleted -> {
                            player.sendRichMessage(MessageUtil.CHALLENGE_ALREADY_COMPLETED)
                        }

                        is StartResult.AlreadyActive -> {
                            val currentName = result.current?.displayName ?: "outro desafio"
                            player.sendRichMessage(
                                MessageUtil.CHALLENGE_ALREADY_ACTIVE,
                                Placeholder.parsed("name", currentName),
                            )
                        }
                    }
                }
                click.parent.update()
            }
    }

    override fun onOpen(open: OpenContext) {
        open.modifyConfig().title(
            Mini.mm
                .deserialize("<white><shift:-8><glyph:quests_menu><font:oneblock:gui><shift:-170><dark_gray>Lista de Missões:")
        )
    }

    override fun onInit(config: ViewConfigBuilder) {
        config.cancelInteractions()
        config.layout(
            " OOOOOOO ",
            " OOOOOOO ",
            " BBBF    "
        )
    }

    override fun onFirstRender(render: RenderContext) {
        val pg = pagination.get(render)

        render.layoutSlot('F')
            .renderWith {
                itemStack(Material.PAPER) {
                    meta<ItemMeta> {
                        itemModel = NamespacedKey("oneblock", "blank")
                        name("<yellow>Fechar menu")
                    }
                }
            }
            .onClick { _ ->
                render.closeForPlayer()
            }

        render.layoutSlot('B')
            .renderWith {
                itemStack(Material.PAPER) {
                    meta<ItemMeta> {
                        itemModel = NamespacedKey("oneblock", "blank")
                        name("<yellow>← Voltar ao Passe")
                    }
                }
            }
            .onClick { _ ->
                render.openForPlayer(BattlePassView::class.java, 0)
            }
    }

    private fun createChallengeItem(playerUuid: UUID, challenge: Challenges): ItemStack {
        val progress = ProgressManager.getProgress(playerUuid, challenge.id)
        val current = progress?.current ?: 0
        val goal = challenge.objective.goal
        val completed = progress?.completed ?: false

        val active = ChallengeManager.getActiveChallenge(playerUuid)
        val isActive = active?.id == challenge.id
        val hasOtherActive = active != null && !isActive

        val barLength = 10
        val filled = if (goal > 0) (current * barLength / goal).coerceIn(0, barLength) else 0
        val progressBar = "<green>${"■".repeat(filled)}<dark_gray>${"■".repeat(barLength - filled)}"

        val item = when {
            completed -> itemStack(Material.LIME_STAINED_GLASS_PANE) {}
            else -> challenge.icon.clone()
        }

        if (isActive) {
            item.addUnsafeEnchantment(Enchantment.UNBREAKING, 1)
        }

        item.meta {
            name(
                when {
                    completed -> "<green>${challenge.displayName} <dark_green>⭐"
                    isActive -> "<yellow>${challenge.displayName}"
                    else -> "<white>${challenge.displayName}"
                }
            )
            if (isActive) addItemFlags(ItemFlag.HIDE_ENCHANTS)
            setLore {
                +""
                +" <white>Objetivo:"
                for (line in challenge.description) +" <gray>$line"
                +""
                +" <white>Progresso: $progressBar <dark_gray>($current/$goal)"
                +" <white>XP: <aqua>+${challenge.xpReward}"
                +""
                when {
                    completed -> +"<green>Missão completa."
                    isActive -> {
                        +"<yellow>Missão em andamento."
                        +"<yellow>Clique para cancelar."
                    }

                    hasOtherActive -> +"<red>Outra missão em andamento."
                    else -> +"<green>Clique para começar a missão."
                }
            }
        }

        return item
    }
}
