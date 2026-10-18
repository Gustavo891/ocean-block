package one_block.system.minions.model

import com.ticxo.modelengine.api.ModelEngineAPI
import com.ticxo.modelengine.api.model.ActiveModel
import com.ticxo.modelengine.api.model.bone.BoneBehaviorTypes
import com.ticxo.modelengine.api.model.bone.ModelBone
import com.ticxo.modelengine.api.model.bone.behavior.BoneBehavior
import com.ticxo.modelengine.api.model.bone.behavior.BoneBehaviorType
import com.ticxo.modelengine.api.model.bone.type.Head
import one_block.libs.instance.Instance
import org.bukkit.Bukkit
import org.bukkit.entity.ArmorStand
import java.util.UUID

object MinionModel {

    enum class HeadMode { OFF, LOCAL, GLOBAL }

    private data class Handle(
        val modelId: String,
        val standUuid: UUID,
        var yaw: Float,
        val scale: Double,
        var headMode: HeadMode = HeadMode.OFF,
    )

    private val handles = mutableMapOf<UUID, Handle>()

    fun isAvailable(): Boolean = Bukkit.getPluginManager().isPluginEnabled("ModelEngine")

    fun attach(minionUuid: UUID, stand: ArmorStand, modelId: String, yaw: Float, scale: Double): Boolean {
        val blueprint = ModelEngineAPI.getBlueprint(modelId)
        if (blueprint == null) {
            Instance.logger.warning("[minions] modelo '$modelId' nao existe no ModelEngine; minion fica sem modelo.")
            return false
        }

        detach(minionUuid)

        val model = ModelEngineAPI.createActiveModel(blueprint)
        val modeled = ModelEngineAPI.createModeledEntity(stand)
        modeled.addModel(model, true)
        modeled.setBaseEntityVisible(false)

        handles[minionUuid] = Handle(modelId, stand.uniqueId, yaw, scale)
        applyProperties(model, yaw, scale)
        applyHeadMode(model, HeadMode.OFF)
        return true
    }

    fun face(minionUuid: UUID, yaw: Float) {
        val handle = handles[minionUuid] ?: return
        handle.yaw = yaw
        resolve(minionUuid)
    }

    fun markOverride(modelId: String, animation: String): Boolean {
        val blueprint = ModelEngineAPI.getBlueprint(modelId) ?: return false
        val blueprintAnimation = blueprint.animations[animation] ?: run {
            Instance.logger.warning("[minions] animacao '$animation' nao existe no modelo '$modelId'.")
            return false
        }

        blueprintAnimation.isOverride = true
        return true
    }

    fun headMode(minionUuid: UUID): HeadMode? = handles[minionUuid]?.headMode

    fun playLoop(minionUuid: UUID, animation: String) {
        val model = resolve(minionUuid) ?: return
        model.animationHandler.playAnimation(animation, 0.0, 0.0, 1.0, false)
    }

    fun playAnimation(minionUuid: UUID, animation: String, speed: Double): Boolean {
        val model = resolve(minionUuid) ?: return false
        return model.animationHandler.playAnimation(animation, 0.0, 0.1, speed, true) != null
    }

    fun stopAnimation(minionUuid: UUID, animation: String) {
        resolve(minionUuid)?.animationHandler?.stopAnimation(animation)
    }

    fun has(minionUuid: UUID): Boolean = handles.containsKey(minionUuid)

    fun detach(minionUuid: UUID) {
        val handle = handles.remove(minionUuid) ?: return
        val stand = Bukkit.getEntity(handle.standUuid) ?: return
        ModelEngineAPI.getModeledEntity(stand)?.destroy()
    }

    fun detachAll() {
        handles.keys.toList().forEach { detach(it) }
    }

    private fun resolve(minionUuid: UUID): ActiveModel? {
        val handle = handles[minionUuid] ?: return null
        val stand = Bukkit.getEntity(handle.standUuid) ?: return null
        val modeled = ModelEngineAPI.getModeledEntity(stand) ?: return null
        val model = modeled.getModel(handle.modelId).orElse(null) ?: return null
        applyProperties(model, handle.yaw, handle.scale)
        applyHeadMode(model, handle.headMode)
        return model
    }

    fun applyHeadMode(model: ActiveModel, mode: HeadMode) {
        model.bones.values.forEach { bone -> applyHeadMode(bone, mode) }
    }

    private fun applyHeadMode(bone: ModelBone, mode: HeadMode) {
        @Suppress("UNCHECKED_CAST")
        val type = BoneBehaviorTypes.HEAD as BoneBehaviorType<BoneBehavior>

        if (mode == HeadMode.OFF) {
            if (bone.hasBoneBehavior(type)) bone.removeBoneBehavior(type)
            return
        }

        val head = bone.getBoneBehavior(type).orElse(null) as? Head ?: return
        head.isLocal = mode == HeadMode.LOCAL
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
