package ocean_block.system.machines

object MachineIds {

    const val GENERATOR = "gerador_energia"
    const val SOLAR = "painel_solar"
    const val FURNACE = "fornalha_eletrica"
    const val MINER = "minerador"
    const val PICKAXE = "picareta_fundidora"
    const val NO_POWER_KEY = "sem_energia"

    val blocks = listOf(GENERATOR, SOLAR, FURNACE, MINER)

    val sources = setOf(GENERATOR, SOLAR)
}
