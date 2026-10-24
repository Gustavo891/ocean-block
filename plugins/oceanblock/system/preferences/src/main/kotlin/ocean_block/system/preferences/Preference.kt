package ocean_block.system.preferences

enum class Preference(
    val key: String,
    val displayName: String,
    val description: String,
    val icon: String,
    val default: Boolean,
) {
    ENERGY_TRAIL(
        key = "rastro_energia",
        displayName = "Rastro de energia",
        description = "Mostra a energia indo do gerador até a máquina.",
        icon = "glowstone_dust",
        default = true,
    ),
}

object PreferenceRules {

    fun isEnabled(stored: Byte?, preference: Preference): Boolean =
        stored?.let { it != 0.toByte() } ?: preference.default

    fun toggled(stored: Byte?, preference: Preference): Byte =
        if (isEnabled(stored, preference)) 0 else 1
}
