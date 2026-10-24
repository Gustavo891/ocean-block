package ocean_block.system.machines

enum class MachineFacing(private val suffix: String) {
    NORTH(""),
    EAST("_leste"),
    SOUTH("_sul"),
    WEST("_oeste");

    fun textureId(baseId: String): String = baseId + suffix

    companion object {
        const val KEY = "frente"

        fun frontFor(playerFacing: String): MachineFacing = when (playerFacing) {
            "NORTH" -> SOUTH
            "SOUTH" -> NORTH
            "EAST" -> WEST
            "WEST" -> EAST
            else -> NORTH
        }

        fun fromCounter(value: Long?): MachineFacing = entries.getOrNull(value?.toInt() ?: 0) ?: NORTH
    }
}
