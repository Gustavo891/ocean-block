package ocean_block.system.stations.sieve

enum class SieveTier(val blockId: String, val steps: Int, val table: String) {
    WOOD("peneira", 6, "madeira"),
    IRON("peneira_ferro", 5, "ferro"),
    DIAMOND("peneira_diamante", 4, "diamante"),
    AUTOMATIC("peneira_automatica", 5, "ferro"),
}
