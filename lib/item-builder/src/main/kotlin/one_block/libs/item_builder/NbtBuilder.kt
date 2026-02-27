package one_block.libs.item_builder

import org.bukkit.NamespacedKey
import org.bukkit.persistence.PersistentDataContainer
import org.bukkit.persistence.PersistentDataType

class NbtBuilder(
    private val container: PersistentDataContainer,
) {

    fun string(namespace: String, key: String, value: String) {
        container.set(NamespacedKey(namespace, key), PersistentDataType.STRING, value)
    }

    fun int(namespace: String, key: String, value: Int) {
        container.set(NamespacedKey(namespace, key), PersistentDataType.INTEGER, value)
    }

    fun double(namespace: String, key: String, value: Double) {
        container.set(NamespacedKey(namespace, key), PersistentDataType.DOUBLE, value)
    }


}