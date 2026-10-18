package one_block.textura.model

data class Vec3(val x: Float, val y: Float, val z: Float) {
    fun maxComponent(): Float = maxOf(x, y, z)
}

/**
 * Um bloco de `display` do model JSON. Cada campo nulo cai no padrao do vanilla
 * na hora de gerar o modelo.
 */
data class DisplayTransform(
    val rotation: Vec3? = null,
    val translation: Vec3? = null,
    val scale: Vec3? = null,
) {
    val isEmpty: Boolean get() = rotation == null && translation == null && scale == null
}
