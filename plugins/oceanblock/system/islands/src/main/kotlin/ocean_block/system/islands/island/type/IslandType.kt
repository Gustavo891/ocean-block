package ocean_block.system.islands.island.type

enum class IslandType(val id: String, val nome: String, val descricao: String, val biome: String?) {
    PADRAO("padrao", "Padrão", "A ilha de sempre: grama, árvore e lava.", null),
    DESERTO("deserto", "Deserto", "Areia, arenito e cactos sob o sol.", "desert"),
    GELO("gelo", "Gelo", "Neve, abetos e gelo que não derrete.", "snowy_plains"),
    JANGADA("jangada", "Jangada", "Só três por três de tábuas. Para quem quer o desafio.", null);

    companion object {
        fun fromId(id: String?): IslandType = entries.firstOrNull { it.id == id } ?: PADRAO
    }
}
