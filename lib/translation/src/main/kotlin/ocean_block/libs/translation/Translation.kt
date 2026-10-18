package ocean_block.libs.translation

import com.google.gson.Gson
import ocean_block.libs.instance.Instance

private val translations: Map<String, String> by lazy {
    val stream = requireNotNull(Instance.javaClass.getResourceAsStream("/pt_br.json")) {
        "pt_br.json nao esta no jar: o recurso de traducao nao foi empacotado."
    }
    val json = stream.bufferedReader().readText()
    Gson().fromJson(json, Map::class.java) as Map<String, String>
}

fun translate(key: String) = translations[key] ?: key
