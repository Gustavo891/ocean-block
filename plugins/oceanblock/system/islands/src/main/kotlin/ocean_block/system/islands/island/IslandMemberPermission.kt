package ocean_block.system.islands.island

enum class IslandMemberPermission(
    val prettyName: String,
    val icon: Char
) {

    INVITE("Adicionar jogadores", 'M'),
    KILL("Matar mobs", 'k'),
    KICK("Expulsar jogadores", 'x')

}
