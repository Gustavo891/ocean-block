package ocean_block.system.islands.member

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import ocean_block.libs.coordinate.SimpleLocation
import ocean_block.system.islands.island.Island
import ocean_block.system.islands.island.IslandMember
import ocean_block.system.islands.island.IslandUpgrade
import ocean_block.system.islands.position.Position
import java.util.UUID

private val OWNER = UUID.randomUUID()
private val TARGET = UUID.randomUUID()

private fun island(owner: UUID = OWNER, ownerName: String = "Dono") = Island(
    uuid = UUID.randomUUID(),
    ownerUuid = owner,
    ownerName = ownerName,
    center = SimpleLocation(0.0, 63.0, 0.0),
    position = Position.fromIndex(0),
    spawn = SimpleLocation(0.5, 64.0, 0.5),
    isPublic = false,
)

private fun Island.withMembers(count: Int) = apply {
    repeat(count) { addMember("membro$it", IslandMember("Membro$it", mutableSetOf())) }
}

class MemberRulesTest : StringSpec({

    "limite de membros comeca em 3 e sobe 3 por nivel do upgrade" {
        val island = island()
        island.maxMembers() shouldBe 3

        island.setUpgradeLevel(IslandUpgrade.Members, 1)
        island.maxMembers() shouldBe 6
    }

    "dono convida jogador de fora com vaga livre" {
        MemberRules.checkInvite(island(), OWNER, TARGET, "Alvo") shouldBe InviteCheck.Allowed
    }

    "so o dono convida" {
        MemberRules.checkInvite(island(), TARGET, UUID.randomUUID(), "Outro") shouldBe InviteCheck.NotOwner
    }

    "nao convida a si mesmo" {
        MemberRules.checkInvite(island(), OWNER, OWNER, "Dono") shouldBe InviteCheck.Self
    }

    "nao convida quem ja e membro, sem diferenciar maiusculas" {
        val island = island().apply { addMember("alvo", IslandMember("Alvo", mutableSetOf())) }

        MemberRules.checkInvite(island, OWNER, TARGET, "ALVO") shouldBe InviteCheck.AlreadyMember
    }

    "ilha cheia recusa convite" {
        MemberRules.checkInvite(island().withMembers(3), OWNER, TARGET, "Alvo") shouldBe InviteCheck.Full
    }

    "jogador bloqueado precisa ser desbloqueado antes" {
        val island = island().apply { blackListNames().add("alvo") }

        MemberRules.checkInvite(island, OWNER, TARGET, "Alvo") shouldBe InviteCheck.Blocked
    }

    "aceitar revalida vaga e membro" {
        MemberRules.checkAccept(island(), TARGET, "Alvo") shouldBe InviteCheck.Allowed
        MemberRules.checkAccept(island().withMembers(3), TARGET, "Alvo") shouldBe InviteCheck.Full
        MemberRules.checkAccept(island(), OWNER, "Dono") shouldBe InviteCheck.Self
    }

    "menu abre a ilha onde o jogador esta quando ele e membro dela" {
        val here = island(owner = UUID.randomUUID(), ownerName = "Amigo")
            .apply { addMember("alvo", IslandMember("Alvo", mutableSetOf())) }
        val owned = island(owner = TARGET, ownerName = "Alvo")

        MemberRules.menuIsland(here, owned, TARGET, "Alvo") shouldBe here
    }

    "menu abre a ilha propria quando o jogador so visita outra" {
        val here = island(owner = UUID.randomUUID(), ownerName = "Amigo")
        val owned = island(owner = TARGET, ownerName = "Alvo")

        MemberRules.menuIsland(here, owned, TARGET, "Alvo") shouldBe owned
        MemberRules.menuIsland(null, owned, TARGET, "Alvo") shouldBe owned
        MemberRules.menuIsland(here, null, TARGET, "Alvo") shouldBe null
    }

    "bloquear: só o dono, nunca a si mesmo, membro, confiável ou já bloqueado" {
        val island = island().withMembers(1).apply { addTrustedMember("Amigo"); blackList = mutableSetOf("chato") }

        MemberRules.checkBlock(island, OWNER, TARGET, "Alvo") shouldBe BlockCheck.Allowed
        MemberRules.checkBlock(island, TARGET, UUID.randomUUID(), "Alvo") shouldBe BlockCheck.NotOwner
        MemberRules.checkBlock(island, OWNER, OWNER, "Dono") shouldBe BlockCheck.Self
        MemberRules.checkBlock(island, OWNER, TARGET, "Membro0") shouldBe BlockCheck.Member
        MemberRules.checkBlock(island, OWNER, TARGET, "amigo") shouldBe BlockCheck.Member
        MemberRules.checkBlock(island, OWNER, TARGET, "Chato") shouldBe BlockCheck.AlreadyBlocked
    }
})
