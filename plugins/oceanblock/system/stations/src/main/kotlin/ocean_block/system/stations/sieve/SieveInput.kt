package ocean_block.system.stations.sieve

enum class SieveInput(val material: String, val key: String) {
    SAND("SAND", "areia"),
    GRAVEL("GRAVEL", "cascalho"),
    DIRT("DIRT", "terra"),
    ;

    companion object {
        fun of(material: String): SieveInput? = entries.firstOrNull { it.material == material }
    }
}
