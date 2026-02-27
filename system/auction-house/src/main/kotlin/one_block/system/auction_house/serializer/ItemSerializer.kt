package one_block.system.auction_house.serializer

import org.bukkit.inventory.ItemStack
import org.bukkit.util.io.BukkitObjectInputStream
import org.bukkit.util.io.BukkitObjectOutputStream
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.*

object ItemSerializer {

    fun serialize(item: ItemStack): String {
        val outputStream = ByteArrayOutputStream()
        BukkitObjectOutputStream(outputStream).use { it.writeObject(item) }
        return Base64.getEncoder().encodeToString(outputStream.toByteArray())
    }

    fun deserialize(base64: String): ItemStack {
        val bytes = Base64.getDecoder().decode(base64)
        val inputStream = ByteArrayInputStream(bytes)
        return BukkitObjectInputStream(inputStream).use {
            it.readObject() as ItemStack
        }
    }
}