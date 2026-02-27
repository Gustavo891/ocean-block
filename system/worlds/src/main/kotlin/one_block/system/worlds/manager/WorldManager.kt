package one_block.system.worlds.manager

import one_block.libs.instance.Instance
import one_block.system.worlds.enums.WorldType
import one_block.system.worlds.generator.EmptyChunkGenerator
import one_block.system.worlds.utils.WorldFile
import org.bukkit.Bukkit
import org.bukkit.WorldCreator
import org.bukkit.entity.Player

object WorldManager {

    fun load() {
        val section = WorldFile.config().getConfigurationSection("worlds") ?: return

        section.getKeys(false).forEach {
            val configurationSection = section.getConfigurationSection(it) ?: return@forEach
            val typeString = configurationSection.getString("type") ?: return@forEach

            val worldType = try {
                WorldType.valueOf(typeString)
            } catch (ex: Exception) {
                Instance.logger.warning("Invalid world type '$typeString' in world '$it'")
                return@forEach
            }

            val createdWorld = WorldCreator(it)
            if (worldType == WorldType.Void) {
                createdWorld.generator(EmptyChunkGenerator())
            }

            val createWorld = createdWorld.createWorld()
            if (createWorld == null) {
                Instance.logger.warning("<red>Erro ao criar o mundo.")
                return
            }

            Instance.logger.info("Mundo $it carregado com sucesso!")
        }
    }

    fun create(player: Player, worldId: String, worldType: WorldType) {
        val world = Bukkit.getWorld(worldId)
        if (world != null) {
            player.sendRichMessage("<red>Já existe um mundo com esse id.")
            return
        }

        val createdWorld = WorldCreator(worldId)
        if (worldType == WorldType.Void) {
            createdWorld.generator(EmptyChunkGenerator())
        }

        val createWorld = createdWorld.createWorld()
        if (createWorld == null) {
            player.sendRichMessage("<red>Erro ao criar o mundo.")
            return
        }

        val createSection = WorldFile.config().createSection("worlds.$worldId")
        createSection.set("type", worldType.name)
        WorldFile.save()

        player.sendRichMessage("<green>Mundo criado com sucesso!")
    }

    fun delete() {
        WorldFile.config().set("worlds", null)
        WorldFile.save()
    }
}