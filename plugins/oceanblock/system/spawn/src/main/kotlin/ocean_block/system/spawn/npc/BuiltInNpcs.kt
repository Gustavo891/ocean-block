package ocean_block.system.spawn.npc

import ocean_block.libs.coordinate.SimpleLocation
import ocean_block.system.spawn.spawn.SpawnManager


internal object BuiltInNpcs {

    fun all(): List<SpawnNpc> {
        val world = SpawnManager.location()?.world ?: return emptyList()

        return listOf(recipes(world))
    }

    private fun recipes(world: String) = SpawnNpc(
        id = "receitas",
        type = NpcType.PACKET_PLAYER,
        location = SimpleLocation(world = world, x = 31.740, y = 58.0, z = -9.8, yaw = 90f),
        displayName = "<gold><b>RECEITAS",
        description = "<yellow>Clique para acessar.",
        texture = RECIPES_TEXTURE,
        action = NpcAction(NpcActionType.PLAYER_COMMAND, "craft"),
    )

    private const val RECIPES_TEXTURE =
        "62afa42d2ef1edaaa630678244b87f9c6035385b9dca9c956e747aabe54af00f"
}
