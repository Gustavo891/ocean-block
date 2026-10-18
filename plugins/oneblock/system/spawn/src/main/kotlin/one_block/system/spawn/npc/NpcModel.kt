package one_block.system.spawn.npc

import com.ticxo.modelengine.api.ModelEngineAPI
import com.ticxo.modelengine.api.model.ActiveModel
import one_block.libs.instance.Instance
import org.bukkit.Bukkit
import org.bukkit.entity.ArmorStand
import java.util.UUID

object NpcModel {

    private const val IDLE_ANIMATION = "idle"

    private data class Handle(val modelId: String, val standUuid: UUID, var yaw: Float, val scale: Double)

    private val handles = mutableMapOf<String, Handle>()

    fun isAvailable(): Boolean = Bukkit.getPluginManager().isPluginEnabled("ModelEngine")

    fun attach(npcId: String, stand: ArmorStand, modelId: String, yaw: Float, scale: Double): Boolean {
        val blueprint = ModelEngineAPI.getBlueprint(modelId)
        if (blueprint == null) {
            Instance.logger.warning("[spawn] modelo '$modelId' não existe no ModelEngine; npc '$npcId' fica sem modelo.")
            return false
        }

        detach(npcId)

        val model = ModelEngineAPI.createActiveModel(blueprint)
        val modeled = ModelEngineAPI.createModeledEntity(stand)
        modeled.addModel(model, true)
        modeled.setBaseEntityVisible(false)

        handles[npcId] = Handle(modelId, stand.uniqueId, yaw, scale)
        applyProperties(model, yaw, scale)
        playIdle(model)
        return true
    }

    fun face(npcId: String, yaw: Float) {
        val handle = handles[npcId] ?: return
        handle.yaw = yaw
        resolve(npcId)
    }

    fun detach(npcId: String) {
        val handle = handles.remove(npcId) ?: return
        val stand = Bukkit.getEntity(handle.standUuid) ?: return
        ModelEngineAPI.getModeledEntity(stand)?.destroy()
    }

    fun detachAll() {
        handles.keys.toList().forEach { detach(it) }
    }

    private fun resolve(npcId: String): ActiveModel? {
        val handle = handles[npcId] ?: return null
        val stand = Bukkit.getEntity(handle.standUuid) ?: return null
        val modeled = ModelEngineAPI.getModeledEntity(stand) ?: return null
        val model = modeled.getModel(handle.modelId).orElse(null) ?: return null
        applyProperties(model, handle.yaw, handle.scale)
        return model
    }

    private fun playIdle(model: ActiveModel) {
        model.animationHandler.playAnimation(IDLE_ANIMATION, 0.0, 0.0, 1.0, false)
    }

    private fun applyProperties(model: ActiveModel, yaw: Float, scale: Double) {
        model.setCanHurt(false)
        model.setScale(scale)
        model.setHitboxScale(scale)
        model.setModelRotationLocked(true)
        model.lockedYBodyRot = yaw
        model.lockedYHeadRot = yaw
        model.lockedXHeadRot = 0.0f

        val modeled = model.modeledEntity ?: return
        modeled.isModelRotationLocked = true
        modeled.setYBodyRotImmediately(yaw)
        modeled.setYHeadRotImmediately(yaw)
    }
}
