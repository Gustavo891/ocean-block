package one_block.textura.api

fun interface DialogBindsConfig {
    fun configure(binds: DialogBinds)
}

fun interface DialogButtonAction {
    fun run(click: DialogClick)
}

fun interface DialogListAction {
    fun run(click: DialogClick, index: Int)
}
