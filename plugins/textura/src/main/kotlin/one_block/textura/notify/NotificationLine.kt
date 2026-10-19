package one_block.textura.notify

data class NotificationLine(
    val text: String,
    val icon: String?,
    val durationTicks: Int,
    val key: String?,
) {

    constructor(text: String, durationTicks: Int) : this(text, null, durationTicks, null)

    constructor(text: String, icon: String?, durationTicks: Int) : this(text, icon, durationTicks, null)

    init {
        require(durationTicks > 0) { "durationTicks precisa ser positivo: $durationTicks" }
    }

    fun withIcon(icon: String?): NotificationLine = NotificationLine(text, icon, durationTicks, key)

    fun withKey(key: String?): NotificationLine = NotificationLine(text, icon, durationTicks, key)
}
