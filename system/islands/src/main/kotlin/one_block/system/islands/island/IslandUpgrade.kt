package one_block.system.islands.island

enum class IslandUpgrade(
    val maxLevel: Int
) {

    Size(3),
    CropGrowth(5);

    fun getPrice(level: Int): Double {
        return when(this) {
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
                0 -> 100.0
                1 -> 200.0
                2 -> 300.0
                3 -> 400.0
                4 -> 500.0
                5 -> 600.0
                6 -> 700.0
                7 -> 800.0
                else -> Double.MAX_VALUE
            }
        }
    }

}