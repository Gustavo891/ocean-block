package ocean_block.system.spawn.npc

import ocean_block.libs.coordinate.SimpleLocation

enum class NpcType {
    MANNEQUIN,
    MODEL_ENGINE,
    PACKET_PLAYER,
}

enum class NpcActionType {
    NONE,
    PLAYER_COMMAND,
    CONSOLE_COMMAND,
    MESSAGE,
}

data class NpcAction(
    val type: NpcActionType = NpcActionType.NONE,
    val value: String? = null,
)


data class SpawnNpc(
    val id: String,
    val type: NpcType,
    val location: SimpleLocation,
    val displayName: String? = null,
    val description: String? = null,
    val skin: String? = null,
    val texture: String? = null,
    val textureSignature: String? = null,
    val model: String? = null,
    val scale: Double = 1.0,
    val looksAtPlayers: Boolean = true,
    val action: NpcAction = NpcAction(),
)
