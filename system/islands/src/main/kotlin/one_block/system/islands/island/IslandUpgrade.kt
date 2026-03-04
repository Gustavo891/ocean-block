package one_block.system.islands.island

enum class IslandUpgrade(
    val maxLevel: Int
) {
    Members(4),
    Size(3),
    CropGrowth(5);

    fun getPrice(level: Int): Double {
        return when(this) {
            Members -> when(level) {
                0 -> 10_000.0
                1 -> 20_000.0
                2 -> 30_000.0
                3 -> 40_000.0
                4 -> 50_000.0
                5 -> 60_000.0
                else -> Double.MAX_VALUE
            }

            Size -> when(level) {
                0 -> 10_000.0
                1 -> 20_000.0
                2 -> 30_000.0
                3 -> 40_000.0
                4 -> 50_000.0
                5 -> 60_000.0
                else -> Double.MAX_VALUE
            }

            CropGrowth -> when(level) {
                0 -> 10_000.0
                1 -> 20_000.0
                2 -> 30_000.0
                3 -> 40_000.0
                4 -> 50_000.0
                5 -> 60_000.0
                else -> Double.MAX_VALUE
            }
        }
    }

}