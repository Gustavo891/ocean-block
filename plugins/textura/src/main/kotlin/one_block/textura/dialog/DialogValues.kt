package one_block.textura.dialog

import java.text.Normalizer

object DialogValues {

    private val KEY = Regex("[a-z0-9_.-]+")
    private val MARKS = Regex("\\p{M}+")

    fun optionId(label: String): String =
        MARKS.replace(Normalizer.normalize(label.trim(), Normalizer.Form.NFD), "").lowercase().replace(' ', '_')

    fun validKey(key: String): Boolean = KEY.matches(key)
}
