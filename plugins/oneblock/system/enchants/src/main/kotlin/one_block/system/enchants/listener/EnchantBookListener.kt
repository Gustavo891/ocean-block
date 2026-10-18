package one_block.system.enchants.listener

import one_block.system.enchants.Enchants
import one_block.system.enchants.book.EnchantBooks
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.ClickType
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.ItemStack
import kotlin.random.Random

class EnchantBookListener : Listener {

    @EventHandler
    fun onInventoryClick(event: InventoryClickEvent) {
        if (event.click != ClickType.LEFT && event.click != ClickType.RIGHT) return

        val player = event.whoClicked as? Player ?: return
        val book = event.cursor
        if (!EnchantBooks.isBook(book)) return

        val target = event.currentItem ?: return
        if (target.type.isAir) return

        event.isCancelled = true

        val enchant = EnchantBooks.enchantOf(book) ?: return
        val level = EnchantBooks.levelOf(book)

        if (!enchant.canApplyTo(target)) {
            player.sendRichMessage("<red>Este encantamento não pode ser aplicado neste item.")
            return
        }

        if (Enchants.levelOf(target, enchant) >= level) {
            player.sendRichMessage("<red>O item já possui este encantamento em nível igual ou maior.")
            return
        }

        consumeBook(player, book)

        if (Random.nextDouble() > enchant.rarity.successChance) {
            player.playSound(player.location, Sound.ENTITY_ITEM_BREAK, 1f, 1f)
            player.sendRichMessage("<red>O encantamento falhou.")

            if (enchant.rarity.destroyOnFailure) {
                event.currentItem = null
                player.sendRichMessage("<dark_red>O item foi destruído no processo.")
            }

            return
        }

        if (!Enchants.apply(target, enchant, level)) {
            player.sendRichMessage("<red>Não foi possível aplicar o encantamento.")
            return
        }

        event.currentItem = target
        player.playSound(player.location, Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1f, 1f)
        player.sendRichMessage("<green>Encantamento aplicado com sucesso!")
    }

    private fun consumeBook(player: Player, book: ItemStack) {
        if (book.amount <= 1) {
            player.setItemOnCursor(null)
            return
        }

        book.amount -= 1
        player.setItemOnCursor(book)
    }
}
