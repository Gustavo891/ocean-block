package one_block.textura.api

import java.util.function.Function

object TranslationApi {

    @Volatile
    private var source: Function<String, String?>? = null

    internal fun init(source: Function<String, String?>) {
        this.source = source
    }

    fun translate(key: String): String? = source?.apply(key)
}
