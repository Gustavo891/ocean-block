package ocean_block.system.spawn.npc

import ocean_block.libs.coordinate.SimpleLocation
import ocean_block.libs.instance.Instance
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File

internal object NpcFile {

    private const val ROOT = "npcs"

    private lateinit var file: File
    private lateinit var config: FileConfiguration

    fun setup() {
        file = File(Instance.dataFolder, "spawn-npcs.yml")
        if (!file.exists()) {
            Instance.dataFolder.mkdirs()
            file.createNewFile()
        }

        config = YamlConfiguration.loadConfiguration(file)
    }

    fun readAll(): List<SpawnNpc> {
        val section = config.getConfigurationSection(ROOT) ?: return emptyList()

        return section.getKeys(false).mapNotNull { id -> read(id) }
    }

    fun write(npc: SpawnNpc) {
        val path = "$ROOT.${npc.id}"

        config.set("$path.type", npc.type.name)
        config.set("$path.display-name", npc.displayName)
        config.set("$path.description", npc.description)
        config.set("$path.skin", npc.skin)
        config.set("$path.texture", npc.texture)
        config.set("$path.texture-signature", npc.textureSignature)
        config.set("$path.model", npc.model)
        config.set("$path.scale", npc.scale)
        config.set("$path.look-at-players", npc.looksAtPlayers)
        config.set("$path.action.type", npc.action.type.name)
        config.set("$path.action.value", npc.action.value)
        config.set("$path.location.world", npc.location.world)
        config.set("$path.location.x", npc.location.x)
        config.set("$path.location.y", npc.location.y)
        config.set("$path.location.z", npc.location.z)
        config.set("$path.location.yaw", npc.location.yaw.toDouble())
        config.set("$path.location.pitch", npc.location.pitch.toDouble())

        save()
    }

    fun remove(id: String) {
        config.set("$ROOT.$id", null)
        save()
    }

    private fun readType(raw: String?): NpcType? = when (raw) {
        null -> null
        "BETTER_MODEL" -> NpcType.MODEL_ENGINE
        else -> runCatching { NpcType.valueOf(raw) }.getOrNull()
    }

    private fun read(id: String): SpawnNpc? {
        val path = "$ROOT.$id"

        val type = readType(config.getString("$path.type"))
        if (type == null) {
            Instance.logger.warning("[spawn] npc '$id' tem tipo inválido; ignorado.")
            return null
        }

        val world = config.getString("$path.location.world")
        if (world == null) {
            Instance.logger.warning("[spawn] npc '$id' está sem mundo; ignorado.")
            return null
        }

        val actionType = runCatching {
            NpcActionType.valueOf(config.getString("$path.action.type").orEmpty())
        }.getOrDefault(NpcActionType.NONE)

        return SpawnNpc(
            id = id,
            type = type,
            location = SimpleLocation(
                world = world,
                x = config.getDouble("$path.location.x"),
                y = config.getDouble("$path.location.y"),
                z = config.getDouble("$path.location.z"),
                yaw = config.getDouble("$path.location.yaw").toFloat(),
                pitch = config.getDouble("$path.location.pitch").toFloat(),
            ),
            displayName = config.getString("$path.display-name"),
            description = config.getString("$path.description"),
            skin = config.getString("$path.skin"),
            texture = config.getString("$path.texture"),
            textureSignature = config.getString("$path.texture-signature"),
            model = config.getString("$path.model"),
            scale = config.getDouble("$path.scale", 1.0),
            looksAtPlayers = config.getBoolean("$path.look-at-players", true),
            action = NpcAction(actionType, config.getString("$path.action.value")),
        )
    }

    private fun save() = config.save(file)
}
