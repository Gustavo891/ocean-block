package ocean_block.system.islands.member

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import java.util.UUID

private val TARGET = UUID.randomUUID()
private val ISLAND_A = UUID.randomUUID()
private val ISLAND_B = UUID.randomUUID()

class IslandInvitesTest : StringSpec({

    "convite aceito dentro do prazo sai da lista" {
        val invites = IslandInvites(60_000)
        invites.add(TARGET, ISLAND_A, "Dono", 1_000)

        invites.take(TARGET, "dono", 30_000) shouldBe IslandInvite(ISLAND_A, "Dono", 61_000)
        invites.take(TARGET, "Dono", 30_000) shouldBe null
    }

    "convite expira depois de 60 segundos" {
        val invites = IslandInvites(60_000)
        invites.add(TARGET, ISLAND_A, "Dono", 1_000)

        invites.take(TARGET, "Dono", 61_001) shouldBe null
    }

    "novo convite da mesma ilha renova o prazo" {
        val invites = IslandInvites(60_000)
        invites.add(TARGET, ISLAND_A, "Dono", 1_000)
        invites.add(TARGET, ISLAND_A, "Dono", 50_000)

        invites.pending(TARGET, 100_000) shouldContainExactly listOf(IslandInvite(ISLAND_A, "Dono", 110_000))
    }

    "convites de ilhas diferentes convivem e so os vivos aparecem" {
        val invites = IslandInvites(60_000)
        invites.add(TARGET, ISLAND_A, "Dono", 0)
        invites.add(TARGET, ISLAND_B, "Outro", 30_000)

        invites.pending(TARGET, 70_000) shouldContainExactly listOf(IslandInvite(ISLAND_B, "Outro", 90_000))
    }

    "sem dono informado aceita o unico convite vivo" {
        val invites = IslandInvites(60_000)
        invites.add(TARGET, ISLAND_A, "Dono", 0)

        invites.take(TARGET, null, 10_000) shouldBe IslandInvite(ISLAND_A, "Dono", 60_000)
    }

    "sem dono informado e com dois convites nao escolhe nenhum" {
        val invites = IslandInvites(60_000)
        invites.add(TARGET, ISLAND_A, "Dono", 0)
        invites.add(TARGET, ISLAND_B, "Outro", 0)

        invites.take(TARGET, null, 10_000) shouldBe null
        invites.pending(TARGET, 10_000).size shouldBe 2
    }

    "esquecer apaga os convites do jogador" {
        val invites = IslandInvites(60_000)
        invites.add(TARGET, ISLAND_A, "Dono", 0)
        invites.forget(TARGET)

        invites.pending(TARGET, 0).shouldBeEmpty()
    }
})
