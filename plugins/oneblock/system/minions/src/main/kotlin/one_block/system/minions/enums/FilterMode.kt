package one_block.system.minions.enums

enum class FilterMode(val prettyName: String, val description: String) {
    WHITELIST("Aceitar", "Guarda apenas os itens da lista."),

    BLACKLIST("Filtrar", "Guarda tudo, exceto os itens da lista.");

    fun next(): FilterMode = entries[(ordinal + 1) % entries.size]
}
