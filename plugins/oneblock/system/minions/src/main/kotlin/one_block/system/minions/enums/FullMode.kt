package one_block.system.minions.enums

enum class FullMode(val prettyName: String, val description: String) {
    STOP("Parar", "Pausa o minion até liberar espaço."),
    DROP("Dropar no chão", "Continua minerando; o excesso cai no chão."),
    TRASH("Lixo", "Continua minerando; o excesso é descartado.");

    fun next(): FullMode = entries[(ordinal + 1) % entries.size]
}
