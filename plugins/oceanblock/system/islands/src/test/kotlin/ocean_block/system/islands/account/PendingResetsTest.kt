package ocean_block.system.islands.account

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import java.util.UUID

class PendingResetsTest : StringSpec({
    val player = UUID.fromString("00000000-0000-0000-0000-000000000001")
    val other = UUID.fromString("00000000-0000-0000-0000-000000000002")

    "marca uma vez so" {
        val pending = PendingResets()
        pending.add(player) shouldBe true
        pending.add(player) shouldBe false
    }

    "consumir tira da fila e so vale uma vez" {
        val pending = PendingResets()
        pending.add(player)
        pending.consume(player) shouldBe true
        pending.consume(player) shouldBe false
    }

    "quem nao foi marcado nao reseta no login" {
        val pending = PendingResets()
        pending.add(player)
        pending.consume(other) shouldBe false
        pending.contains(player) shouldBe true
    }

    "carregar do banco junta com a fila" {
        val pending = PendingResets()
        pending.load(listOf(player, other))
        pending.contains(player) shouldBe true
        pending.contains(other) shouldBe true
    }
})
