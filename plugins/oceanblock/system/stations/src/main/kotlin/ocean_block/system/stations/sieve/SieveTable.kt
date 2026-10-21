package ocean_block.system.stations.sieve

data class SieveDrop(val output: String, val min: Int, val max: Int, val chance: Double)

class SieveTable(private val drops: Map<SieveInput, Map<String, List<SieveDrop>>>) {

    fun drops(input: SieveInput, tier: SieveTier): List<SieveDrop> = drops[input]?.get(tier.table).orEmpty()

    fun outputs(): Set<String> = drops.values.flatMap { byTier -> byTier.values.flatten() }.map { it.output }.toSet()
}
