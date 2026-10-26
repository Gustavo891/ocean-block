package ocean_block.system.sell.notify

import ocean_block.libs.formatter.NumberFormatter
import one_block.textura.api.NotificationApi
import one_block.textura.notify.NotificationLine
import org.bukkit.entity.Player

object SaleNotifier {

    private const val REGION = "avisos"
    private const val DURATION_TICKS = 100

    fun notify(player: Player, total: Double, amount: Int) {
        SaleCard.lines(NumberFormatter.format(total), amount).forEachIndexed { index, line ->
            NotificationApi.push(player, REGION, NotificationLine(line.text, line.icon, DURATION_TICKS, "venda_$index"))
        }
    }
}
