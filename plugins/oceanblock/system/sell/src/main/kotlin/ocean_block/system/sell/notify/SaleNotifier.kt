package ocean_block.system.sell.notify

import ocean_block.libs.formatter.NumberFormatter
import one_block.textura.api.NotificationApi
import one_block.textura.notify.NotificationLine
import org.bukkit.entity.Player

object SaleNotifier {

    const val ICON_MATERIAL = "emerald"

    private const val REGION = "avisos"
    private const val DURATION_TICKS = 80

    fun notify(player: Player, total: Double, amount: Int) {
        val line = NotificationLine(
            SaleText.line(NumberFormatter.format(total), amount),
            "item:$ICON_MATERIAL",
            DURATION_TICKS,
            "venda",
        )
        NotificationApi.push(player, REGION, line)
    }
}
