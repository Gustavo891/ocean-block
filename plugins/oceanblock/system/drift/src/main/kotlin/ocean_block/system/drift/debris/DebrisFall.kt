package ocean_block.system.drift.debris

import ocean_block.system.drift.config.DriftSettings

internal object DebrisFall {

    fun begin(debris: Debris) {
        debris.support = Support.FALL
        debris.velocityY = 0.0
        debris.atSurface = false
        debris.stuckSinceMillis = null
    }

    fun fall(debris: Debris, settings: DriftSettings, nowMillis: Long): Boolean {
        val raftY = DiveRules.raftY(settings.surfaceY)
        val here = DebrisDive.hereColumn(debris, raftY)
        if (!here.isLoaded) return false

        val toY = debris.levelY + GravityRules.descent(debris.velocityY, DebrisRules.TICKS_PER_STEP)
        if (GravityRules.outOfWorld(toY, raftY, debris.entity.world.minHeight)) return false

        debris.velocityY = GravityRules.fall(debris.velocityY, DebrisRules.TICKS_PER_STEP)
        debris.ageTicks += DebrisRules.TICKS_PER_STEP
        when (val outcome = GravityRules.settle(debris.levelY, toY, here::isWaterAt, here::isSolidAt)) {
            Fall.Airborne -> debris.levelY = toY
            is Fall.Splash -> splash(debris, outcome.blockY, maxOf(toY, outcome.blockY.toDouble()), raftY)
            is Fall.Landed -> land(debris, outcome.topY, nowMillis)
        }
        DebrisDive.hold(debris, settings)
        return true
    }

    fun rest(debris: Debris, settings: DriftSettings, nowMillis: Long): Boolean {
        val raftY = DiveRules.raftY(settings.surfaceY)
        val here = DebrisDive.hereColumn(debris, raftY)
        if (!here.isLoaded) return false

        val landed = debris.landedTopY
        when (GravityRules.restingSupport(landed, here::isWaterAt, here::isSolidAt)) {
            Support.FLOAT -> splash(debris, landed, debris.levelY, raftY)
            Support.FALL -> begin(debris)
            Support.GROUND -> if (debris.stuckSinceMillis == null) debris.stuckSinceMillis = nowMillis
        }
        return true
    }

    private fun splash(debris: Debris, blockY: Int, y: Double, raftY: Int) {
        debris.support = Support.FLOAT
        debris.depth = raftY - blockY
        debris.atSurface = false
        debris.velocityY = 0.0
        debris.levelY = y
        debris.stuckSinceMillis = null
    }

    private fun land(debris: Debris, topY: Int, nowMillis: Long) {
        debris.support = Support.GROUND
        debris.velocityY = 0.0
        debris.levelY = GravityRules.restY(topY, debris.type.scale.toDouble())
        debris.restAgeTicks = debris.ageTicks
        debris.landedTopY = topY
        debris.stuckSinceMillis = nowMillis
    }
}
