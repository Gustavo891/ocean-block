package one_block.system.spawn.npc

import com.destroystokyo.paper.profile.ProfileProperty
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.JoinConfiguration
import io.papermc.paper.datacomponent.item.ResolvableProfile
import one_block.libs.coordinate.SimpleLocation
import one_block.libs.formatter.Mini
import one_block.libs.instance.Instance
import one_block.system.spawn.npc.packet.PacketNpcService
import org.bukkit.Bukkit
import org.bukkit.Color
import org.bukkit.Location
import org.bukkit.NamespacedKey
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.Entity
import org.bukkit.entity.Mannequin
import org.bukkit.entity.Display
import org.bukkit.entity.Player
import org.bukkit.entity.TextDisplay
import org.bukkit.persistence.PersistentDataType

object NpcManager {

    private val TAG: NamespacedKey by lazy { NamespacedKey(Instance, "spawn_npc") }

    private const val HOLOGRAM_OFFSET = 2.1

    private val npcs = linkedMapOf<String, SpawnNpc>()
    private val entities = mutableMapOf<String, java.util.UUID>()

    fun onInit() {
        NpcFile.setup()

        npcs.clear()
        NpcFile.readAll().forEach { npcs[it.id] = it }
        BuiltInNpcs.all().forEach { npcs.putIfAbsent(it.id, it) }

        PacketNpcService.onInteract { player, id -> interact(player, id) }
    }

    fun all(): List<SpawnNpc> = npcs.values.toList()

    fun get(id: String): SpawnNpc? = npcs[id]

    fun spawnAll() {
        clearTaggedEntities()
        entities.clear()

        npcs.values.forEach { spawn(it) }

        NpcLook.start()
    }

    fun despawnAll() {
        NpcLook.stop()
        PacketNpcService.despawnAll()
        NpcModel.detachAll()
        clearTaggedEntities()
        entities.clear()
    }

    fun create(npc: SpawnNpc): Boolean {
        remove(npc.id)

        npcs[npc.id] = npc
        NpcFile.write(npc)
        return spawn(npc)
    }

    fun remove(id: String): Boolean {
        val existing = npcs.remove(id) ?: return false

        despawn(id)
        NpcFile.remove(id)
        return existing.id.isNotEmpty()
    }

    fun update(npc: SpawnNpc) {
        npcs[npc.id] = npc
        NpcFile.write(npc)

        despawn(npc.id)
        spawn(npc)
    }

    fun entityOf(id: String): Entity? = entities[id]?.let { Bukkit.getEntity(it) }

    fun npcIdOf(entity: Entity): String? =
        entity.persistentDataContainer.get(TAG, PersistentDataType.STRING)

    fun interact(player: Player, id: String) {
        val npc = npcs[id] ?: return
        val value = npc.action.value

        when (npc.action.type) {
            NpcActionType.NONE -> Unit
            NpcActionType.MESSAGE -> value?.let { player.sendMessage(Mini.deserialize(it)) }
            NpcActionType.PLAYER_COMMAND -> value?.let { player.performCommand(it.removePrefix("/")) }
            NpcActionType.CONSOLE_COMMAND -> value?.let {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), it.replace("%player%", player.name))
            }
        }
    }

    private fun spawn(npc: SpawnNpc): Boolean {
        val location = npc.location.toBukkit()
        if (location == null) {
            Instance.logger.warning("[spawn] mundo '${npc.location.world}' do npc '${npc.id}' não está carregado.")
            return false
        }

        location.chunk.isForceLoaded = true

        val entity = when (npc.type) {
            NpcType.MANNEQUIN -> spawnMannequin(npc, location)
            NpcType.MODEL_ENGINE -> spawnModel(npc, location)
            NpcType.PACKET_PLAYER -> {
                if (!PacketNpcService.spawn(npc)) return false
                spawnHologram(npc, location)
            }
        }

        if (entity != null) entities[npc.id] = entity.uniqueId
        return true
    }

    private fun spawnHologram(npc: SpawnNpc, location: Location): Entity? {
        val lines = listOfNotNull(npc.displayName, npc.description).map(Mini::deserialize)
        if (lines.isEmpty()) return null

        val text = Component.join(JoinConfiguration.newlines(), lines)

        return location.world.spawn(location.clone().add(0.0, HOLOGRAM_OFFSET, 0.0), TextDisplay::class.java) { display ->
            display.text(text)
            display.billboard = Display.Billboard.CENTER
            display.alignment = TextDisplay.TextAlignment.CENTER
            display.isPersistent = true
            display.backgroundColor = Color.fromARGB(0, 0, 0, 0)
            display.isShadowed = true

            tag(display, npc.id)
        }
    }

    private fun spawnMannequin(npc: SpawnNpc, location: Location): Entity =
        location.world.spawn(location, Mannequin::class.java) { mannequin ->
            mannequin.setProfile(profileOf(npc))
            mannequin.isImmovable = true
            mannequin.isInvulnerable = true
            mannequin.isPersistent = true
            mannequin.setGravity(false)

            npc.displayName?.let {
                mannequin.customName(Mini.deserialize(it))
                mannequin.isCustomNameVisible = true
            }
            npc.description?.let { mannequin.setDescription(Mini.deserialize(it)) }

            tag(mannequin, npc.id)
        }

    private fun spawnModel(npc: SpawnNpc, location: Location): Entity {
        val stand = location.world.spawn(location, ArmorStand::class.java) { stand ->
            stand.isMarker = true
            stand.isVisible = false
            stand.isInvulnerable = true
            stand.isPersistent = true
            stand.setGravity(false)
            stand.setBasePlate(false)

            npc.displayName?.let {
                stand.customName(Mini.deserialize(it))
                stand.isCustomNameVisible = true
            }

            tag(stand, npc.id)
        }

        val model = npc.model
        if (model != null && NpcModel.isAvailable()) {
            NpcModel.attach(npc.id, stand, model, npc.location.yaw, npc.scale)
        }

        return stand
    }

    private fun despawn(id: String) {
        NpcModel.detach(id)
        PacketNpcService.despawn(id)

        val uuid = entities.remove(id) ?: return
        Bukkit.getEntity(uuid)?.remove()
    }

    private fun clearTaggedEntities() {
        for (world in Bukkit.getWorlds()) {
            world.entities
                .filter { it.persistentDataContainer.has(TAG, PersistentDataType.STRING) }
                .forEach { it.remove() }
        }
    }

    private fun tag(entity: Entity, id: String) {
        entity.persistentDataContainer.set(TAG, PersistentDataType.STRING, id)
    }

    private fun profileOf(npc: SpawnNpc): ResolvableProfile {
        val texture = npc.texture
        if (!texture.isNullOrBlank()) {
            return ResolvableProfile.resolvableProfile()
                .addProperty(ProfileProperty("textures", texture, npc.textureSignature))
                .build()
        }

        val skin = npc.skin
        if (skin.isNullOrBlank()) return Mannequin.defaultProfile()

        return ResolvableProfile.resolvableProfile().name(skin).build()
    }

    fun locationOf(id: String): SimpleLocation? = npcs[id]?.location
}
