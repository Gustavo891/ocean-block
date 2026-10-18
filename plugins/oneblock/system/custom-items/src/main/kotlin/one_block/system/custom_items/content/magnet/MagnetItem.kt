package one_block.system.custom_items.content.magnet

import one_block.libs.item_builder.getNbtBoolean
import one_block.libs.item_builder.meta
import one_block.libs.item_builder.nbt
import one_block.libs.item_builder.setLore
import one_block.system.custom_items.item.CustomItem
import one_block.system.custom_items.item.context.InteractContext
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.Sound
import org.bukkit.entity.Item
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.util.Vector

class MagnetItem : CustomItem() {

    override val id = "magnet"
    override val material = Material.IRON_INGOT
    override val itemModel = NamespacedKey("oneblock", "magnet")
    override val displayName = "<gray>Ímã"
    override val heldTickInterval = 5L
    override val cooldownMillis = 1000L

    override fun build(amount: Int): ItemStack {
        val stack = super.build(amount)
        stack.meta<ItemMeta> {
            nbt { boolean(ACTIVE_KEY, true) }
            setEnchantmentGlintOverride(true)
            setLore { loreFor(true).forEach { +it } }
        }
        return stack
    }

    override fun onRightClick(ctx: InteractContext) {
        val active = !ctx.item.getNbtBoolean(ACTIVE_KEY, true)
        val updated = ctx.item.clone().apply {
            meta<ItemMeta> {
                nbt { boolean(ACTIVE_KEY, active) }
                setEnchantmentGlintOverride(active)
                setLore { loreFor(active).forEach { +it } }
            }
        }
        ctx.setItem(updated)
        ctx.cancel()
        ctx.markCooldown()

        ctx.player.sendRichMessage(if (active) "<green>Ímã ativado" else "<red>Ímã desativado")
        ctx.player.playSound(
            ctx.player,
            if (active) Sound.BLOCK_NOTE_BLOCK_PLING else Sound.BLOCK_NOTE_BLOCK_BASS,
            1f,
            1f
        )
    }

    override fun onHeldTick(player: Player, item: ItemStack) {
        if (!item.getNbtBoolean(ACTIVE_KEY, true)) return

        val target = player.location.toVector().add(Vector(0.0, 0.5, 0.0))
        player.world.getNearbyEntities(player.location, RADIUS, RADIUS, RADIUS) { it is Item }
            .forEach { entity ->
                val drop = entity as Item
                if (drop.pickupDelay >= Short.MAX_VALUE.toInt()) return@forEach
                val dir = target.clone().subtract(drop.location.toVector())
                if (dir.lengthSquared() < 0.6) return@forEach
                drop.velocity = dir.normalize().multiply(0.45)
            }
    }

    private fun loreFor(active: Boolean): List<String> = listOf(
        "<gray>Puxa itens num raio de <white>${RADIUS.toInt()}<gray> blocos.",
        "",
        if (active) "<green>▶ Ativado" else "<red>■ Desativado",
        "<dark_gray>Clique-direito para alternar."
    )

    companion object {
        val ACTIVE_KEY = NamespacedKey("oneblock", "magnet_active")
        const val RADIUS = 5.0
    }
}
