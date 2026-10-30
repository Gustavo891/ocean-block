package one_block.textura.dialog.layout

object TextWrap {

    fun lines(text: String, width: Int, measure: (String) -> Int): List<String> {
        val words = text.split(' ').filter { it.isNotEmpty() }
        if (words.isEmpty()) return emptyList()
        val lines = mutableListOf<String>()
        var current = ""
        words.forEach { word ->
            val candidate = if (current.isEmpty()) word else "$current $word"
            if (current.isNotEmpty() && measure(candidate) > width) {
                lines += current
                current = word
            } else {
                current = candidate
            }
        }
        if (current.isNotEmpty()) lines += current
        return lines
    }
}
