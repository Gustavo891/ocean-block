package one_block.textura.notify.region

internal object NotificationAnimation {

    const val PERIOD = 200
    private const val DAY = 24000

    fun phase(gameTime: Long): Int = ((gameTime % DAY) % PERIOD).toInt()
}
