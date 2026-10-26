package ocean_block.system.islands.dialog

import java.awt.image.BufferedImage
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin
import kotlin.math.sqrt

object PlayerFigure {

    const val WIDTH = 44
    const val HEIGHT = 76

    private const val UNIT = 2.0
    private const val CENTER_Y = 16.0
    private const val FAR = 50.0
    private const val YAW = -25.0
    private const val PITCH = 15.0
    private const val OPAQUE = 128
    private const val HEAD_INFLATE = 0.5
    private const val LIMB_INFLATE = 0.25
    private const val AMBIENT = 0.45
    private const val DIFFUSE = 0.6
    const val BUST_WIDTH = 48
    const val BUST_HEIGHT = 56
    private const val BUST_UNIT = 2.9
    private const val SUPERSAMPLE = 3
    private const val BUST_CENTER_Y = 25.0
    private const val BUST_YAW = 32.0
    private const val BUST_PITCH = 22.0

    private data class Vec(val x: Double, val y: Double, val z: Double) {
        operator fun plus(o: Vec) = Vec(x + o.x, y + o.y, z + o.z)
        operator fun times(s: Double) = Vec(x * s, y * s, z * s)
        fun dot(o: Vec) = x * o.x + y * o.y + z * o.z
    }

    private data class Box(val u: Int, val v: Int, val x: Double, val y: Double, val z: Double, val w: Int, val h: Int, val d: Int, val inflate: Double) {
        val min = Vec(x - inflate, y - inflate, z - inflate)
        val max = Vec(x + w + inflate, y + h + inflate, z + d + inflate)
    }

    private class Hit(val t: Double, val argb: Int, val normal: Vec)

    private val light = Vec(-0.5, 0.7, 0.5).let { it * (1 / sqrt(it.dot(it))) }

    private class Frame(val width: Int, val height: Int, val unit: Double, val centerY: Double, val yaw: Double, val pitch: Double) {
        fun scaled(factor: Int) = Frame(width * factor, height * factor, unit * factor, centerY, yaw, pitch)
    }

    private val fullBody = Frame(WIDTH, HEIGHT, UNIT, CENTER_Y, YAW, PITCH)

    fun render(skin: BufferedImage, slim: Boolean, yaw: Double = YAW): BufferedImage =
        draw(skin, slim, Frame(fullBody.width, fullBody.height, fullBody.unit, fullBody.centerY, yaw, fullBody.pitch))

    fun bust(skin: BufferedImage, slim: Boolean, background: Int, width: Int = BUST_WIDTH, height: Int = BUST_HEIGHT): BufferedImage {
        val frame = Frame(width, height, BUST_UNIT * width / BUST_WIDTH, BUST_CENTER_Y, BUST_YAW, BUST_PITCH)
        val fine = draw(skin, slim, frame.scaled(SUPERSAMPLE))
        return downsample(fine, SUPERSAMPLE, background)
    }

    private fun downsample(fine: BufferedImage, factor: Int, background: Int): BufferedImage {
        val image = BufferedImage(fine.width / factor, fine.height / factor, BufferedImage.TYPE_INT_ARGB)
        val samples = factor * factor
        for (x in 0 until image.width) {
            for (y in 0 until image.height) {
                var covered = 0
                var r = 0
                var g = 0
                var b = 0
                for (dx in 0 until factor) {
                    for (dy in 0 until factor) {
                        val argb = fine.getRGB(x * factor + dx, y * factor + dy)
                        val sample = if ((argb ushr 24) == 0) background else argb.also { covered++ }
                        r += (sample shr 16) and 0xFF
                        g += (sample shr 8) and 0xFF
                        b += sample and 0xFF
                    }
                }
                if (covered == 0) continue
                image.setRGB(x, y, (0xFF shl 24) or ((r / samples) shl 16) or ((g / samples) shl 8) or (b / samples))
            }
        }
        return image
    }

    private fun draw(skin: BufferedImage, slim: Boolean, frame: Frame): BufferedImage {
        val boxes = boxes(slim, legacy = skin.height < skin.width)
        val image = BufferedImage(frame.width, frame.height, BufferedImage.TYPE_INT_ARGB)
        val camera = Camera(Math.toRadians(frame.yaw), Math.toRadians(frame.pitch))
        val direction = camera.toModel(Vec(0.0, 0.0, -1.0))
        for (py in 0 until frame.height) {
            for (px in 0 until frame.width) {
                val view = Vec((px + 0.5 - frame.width / 2.0) / frame.unit, (frame.height / 2.0 - (py + 0.5)) / frame.unit, FAR)
                val origin = camera.toModel(view) + Vec(0.0, frame.centerY, 0.0)
                val hit = boxes.mapNotNull { trace(skin, it, origin, direction) }.minByOrNull { it.t } ?: continue
                image.setRGB(px, py, lit(hit, camera))
            }
        }
        return image
    }

    private fun boxes(slim: Boolean, legacy: Boolean): List<Box> {
        val arm = if (slim) 3 else 4
        val leftArm = if (legacy) 40 to 16 else 32 to 48
        val leftLeg = if (legacy) 0 to 16 else 16 to 48
        val base = listOf(
            Box(0, 0, -4.0, 24.0, -4.0, 8, 8, 8, 0.0),
            Box(16, 16, -4.0, 12.0, -2.0, 8, 12, 4, 0.0),
            Box(40, 16, -4.0 - arm, 12.0, -2.0, arm, 12, 4, 0.0),
            Box(leftArm.first, leftArm.second, 4.0, 12.0, -2.0, arm, 12, 4, 0.0),
            Box(0, 16, -4.0, 0.0, -2.0, 4, 12, 4, 0.0),
            Box(leftLeg.first, leftLeg.second, 0.0, 0.0, -2.0, 4, 12, 4, 0.0),
        )
        val hat = Box(32, 0, -4.0, 24.0, -4.0, 8, 8, 8, HEAD_INFLATE)
        if (legacy) return base + hat
        return base + hat + listOf(
            Box(16, 32, -4.0, 12.0, -2.0, 8, 12, 4, LIMB_INFLATE),
            Box(40, 32, -4.0 - arm, 12.0, -2.0, arm, 12, 4, LIMB_INFLATE),
            Box(48, 48, 4.0, 12.0, -2.0, arm, 12, 4, LIMB_INFLATE),
            Box(0, 32, -4.0, 0.0, -2.0, 4, 12, 4, LIMB_INFLATE),
            Box(0, 48, 0.0, 0.0, -2.0, 4, 12, 4, LIMB_INFLATE),
        )
    }

    private fun trace(skin: BufferedImage, box: Box, origin: Vec, direction: Vec): Hit? {
        var near = Double.NEGATIVE_INFINITY
        var far = Double.POSITIVE_INFINITY
        var axis = -1
        val o = doubleArrayOf(origin.x, origin.y, origin.z)
        val d = doubleArrayOf(direction.x, direction.y, direction.z)
        val lo = doubleArrayOf(box.min.x, box.min.y, box.min.z)
        val hi = doubleArrayOf(box.max.x, box.max.y, box.max.z)
        for (i in 0..2) {
            if (d[i] == 0.0) {
                if (o[i] < lo[i] || o[i] > hi[i]) return null
                continue
            }
            val t1 = (lo[i] - o[i]) / d[i]
            val t2 = (hi[i] - o[i]) / d[i]
            val enter = minOf(t1, t2)
            if (enter > near) {
                near = enter
                axis = i
            }
            far = minOf(far, maxOf(t1, t2))
        }
        if (axis < 0 || near > far || far < 0) return null
        val sign = if (d[axis] > 0) -1.0 else 1.0
        val normal = when (axis) {
            0 -> Vec(sign, 0.0, 0.0)
            1 -> Vec(0.0, sign, 0.0)
            else -> Vec(0.0, 0.0, sign)
        }
        val point = origin + direction * near
        val argb = texel(skin, box, point, normal)
        if ((argb ushr 24) < OPAQUE) return null
        return Hit(near, argb, normal)
    }

    private fun texel(skin: BufferedImage, box: Box, point: Vec, normal: Vec): Int {
        fun fraction(value: Double, lo: Double, hi: Double) = ((value - lo) / (hi - lo)).coerceIn(0.0, 0.9999)
        val fx = fraction(point.x, box.min.x, box.max.x)
        val fy = 1 - fraction(point.y, box.min.y, box.max.y)
        val fz = fraction(point.z, box.min.z, box.max.z)
        val (w, h, d) = Triple(box.w, box.h, box.d)
        val (tx, ty) = when {
            normal.z > 0 -> box.u + d + cell(fx, w) to box.v + d + cell(fy, h)
            normal.z < 0 -> box.u + 2 * d + w + cell(1 - fx, w) to box.v + d + cell(fy, h)
            normal.x < 0 -> box.u + cell(fz, d) to box.v + d + cell(fy, h)
            normal.x > 0 -> box.u + d + w + cell(1 - fz, d) to box.v + d + cell(fy, h)
            normal.y > 0 -> box.u + d + cell(fx, w) to box.v + cell(fz, d)
            else -> box.u + d + w + cell(fx, w) to box.v + cell(fz, d)
        }
        if (tx !in 0 until skin.width || ty !in 0 until skin.height) return 0
        return skin.getRGB(tx, ty)
    }

    private fun cell(fraction: Double, size: Int): Int = floor(fraction.coerceIn(0.0, 0.9999) * size).toInt()

    private fun lit(hit: Hit, camera: Camera): Int {
        val factor = AMBIENT + DIFFUSE * maxOf(0.0, camera.toView(hit.normal).dot(light))
        val r = (((hit.argb shr 16) and 0xFF) * factor).toInt().coerceIn(0, 255)
        val g = (((hit.argb shr 8) and 0xFF) * factor).toInt().coerceIn(0, 255)
        val b = ((hit.argb and 0xFF) * factor).toInt().coerceIn(0, 255)
        return (0xFF shl 24) or (r shl 16) or (g shl 8) or b
    }

    private class Camera(yaw: Double, pitch: Double) {
        private val cy = cos(yaw)
        private val sy = sin(yaw)
        private val cp = cos(pitch)
        private val sp = sin(pitch)

        fun toView(p: Vec): Vec {
            val y1 = Vec(p.x * cy + p.z * sy, p.y, -p.x * sy + p.z * cy)
            return Vec(y1.x, y1.y * cp - y1.z * sp, y1.y * sp + y1.z * cp)
        }

        fun toModel(v: Vec): Vec {
            val x1 = Vec(v.x, v.y * cp + v.z * sp, -v.y * sp + v.z * cp)
            return Vec(x1.x * cy - x1.z * sy, x1.y, x1.x * sy + x1.z * cy)
        }
    }
}
