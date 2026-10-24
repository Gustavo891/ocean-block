package ocean_block.system.preferences

import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.persistence.PersistentDataType

object Preferences {

    private const val NAMESPACE = "oceanblock"
    private const val PREFIX = "pref_"

    fun isEnabled(player: Player, preference: Preference): Boolean =
        PreferenceRules.isEnabled(player.persistentDataContainer.get(keyOf(preference), PersistentDataType.BYTE), preference)

    fun toggle(player: Player, preference: Preference): Boolean {
        val container = player.persistentDataContainer
        val next = PreferenceRules.toggled(container.get(keyOf(preference), PersistentDataType.BYTE), preference)
        container.set(keyOf(preference), PersistentDataType.BYTE, next)
        return next != 0.toByte()
    }

    private fun keyOf(preference: Preference) = NamespacedKey(NAMESPACE, PREFIX + preference.key)
}
