package one_block.textura.block

object NoteBlockStates {

    val INSTRUMENTS = listOf(
        "harp", "basedrum", "snare", "hat", "bass", "flute", "bell", "guitar",
        "chime", "xylophone", "iron_xylophone", "cow_bell", "didgeridoo", "bit",
        "banjo", "pling", "zombie", "skeleton", "creeper", "dragon",
        "wither_skeleton", "piglin", "custom_head",
    )

    const val NOTES = 25
    const val POWERED_STATES = 2
    const val FIRST_ALLOCATABLE = 1

    val CAPACITY = INSTRUMENTS.size * NOTES * POWERED_STATES

    fun propertiesOf(index: Int): String {
        require(index in 0 until CAPACITY) { "indice de blockstate fora do intervalo: $index" }
        val powered = index % POWERED_STATES == 1
        val note = (index / POWERED_STATES) % NOTES
        val instrument = INSTRUMENTS[index / (POWERED_STATES * NOTES)]
        return "instrument=$instrument,note=$note,powered=$powered"
    }

    fun indexOf(properties: String): Int? {
        val pares = properties.split(',')
            .mapNotNull { par ->
                val partes = par.split('=', limit = 2)
                if (partes.size == 2) partes[0].trim() to partes[1].trim() else null
            }
            .toMap()

        val instrument = INSTRUMENTS.indexOf(pares["instrument"] ?: return null)
        if (instrument < 0) return null
        val note = pares["note"]?.toIntOrNull()?.takeIf { it in 0 until NOTES } ?: return null
        val powered = when (pares["powered"]) {
            "true" -> 1
            "false" -> 0
            else -> return null
        }

        return instrument * POWERED_STATES * NOTES + note * POWERED_STATES + powered
    }

    fun blockDataString(index: Int): String = "minecraft:note_block[${propertiesOf(index)}]"
}
