package one_block.textura.dialog.kit

import java.security.MessageDigest

object UiCodepoints {

    private const val BASE = 0xF0000
    private const val SPAN = 0xFFFE

    fun assign(names: Collection<String>): Map<String, String> {
        val unique = names.toSortedSet()
        require(unique.size <= SPAN) { "Peças de ui demais: ${unique.size} para $SPAN codepoints" }

        val taken = BooleanArray(SPAN)
        val result = sortedMapOf<String, String>()

        unique.map { it to hashSlot(it) }
            .sortedWith(compareBy({ it.second }, { it.first }))
            .forEach { (name, start) ->
                var index = start
                while (taken[index]) index = (index + 1) % SPAN
                taken[index] = true
                result[name] = String(Character.toChars(BASE + index))
            }
        return result
    }

    private fun hashSlot(name: String): Int {
        val digest = MessageDigest.getInstance("SHA-1").digest(name.toByteArray(Charsets.UTF_8))
        val value = ((digest[0].toInt() and 0xFF) shl 24) or ((digest[1].toInt() and 0xFF) shl 16) or
            ((digest[2].toInt() and 0xFF) shl 8) or (digest[3].toInt() and 0xFF)
        return Math.floorMod(value, SPAN)
    }
}
