package ocean_block.system.custom_items.cooldown

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import java.util.UUID

class CooldownStoreTest : StringSpec({

    "pronto por padrão quando nunca marcado" {
        val uuid = UUID.randomUUID()
        CooldownStore.isReady(uuid, "magnet") shouldBe true
        CooldownStore.remaining(uuid, "magnet") shouldBe 0L
    }

    "stamp bloqueia e depois expira" {
        val uuid = UUID.randomUUID()
        CooldownStore.stamp(uuid, "skill", 50L)
        CooldownStore.isReady(uuid, "skill") shouldBe false

        Thread.sleep(70L)
        CooldownStore.isReady(uuid, "skill") shouldBe true
    }

    "duração zero é no-op" {
        val uuid = UUID.randomUUID()
        CooldownStore.stamp(uuid, "free", 0L)
        CooldownStore.isReady(uuid, "free") shouldBe true
    }

    "cooldowns são isolados por id" {
        val uuid = UUID.randomUUID()
        CooldownStore.stamp(uuid, "a", 10_000L)
        CooldownStore.isReady(uuid, "a") shouldBe false
        CooldownStore.isReady(uuid, "b") shouldBe true
    }

    "clear remove os cooldowns do player" {
        val uuid = UUID.randomUUID()
        CooldownStore.stamp(uuid, "a", 10_000L)
        CooldownStore.clear(uuid)
        CooldownStore.isReady(uuid, "a") shouldBe true
    }
})
