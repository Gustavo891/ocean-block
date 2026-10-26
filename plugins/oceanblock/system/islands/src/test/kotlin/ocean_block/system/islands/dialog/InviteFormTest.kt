package ocean_block.system.islands.dialog

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import ocean_block.system.islands.member.InviteCheck

class InviteFormTest : StringSpec({

    "o nome digitado perde os espaços das pontas" {
        InviteForm.clean("  Steve ") shouldBe "Steve"
        InviteForm.clean(null) shouldBe ""
    }

    "campo vazio pede um nome" {
        InviteForm.check("", online = false) shouldBe "DIGITE O NOME DE UM JOGADOR."
    }

    "nome com caractere inválido ou fora de 3 a 16 letras é recusado" {
        InviteForm.check("ab", online = true) shouldBe "NOME INVÁLIDO."
        InviteForm.check("nome com espaco", online = true) shouldBe "NOME INVÁLIDO."
        InviteForm.check("a".repeat(17), online = true) shouldBe "NOME INVÁLIDO."
    }

    "jogador offline não pode ser convidado" {
        InviteForm.check("Steve", online = false) shouldBe "STEVE NÃO ESTÁ ONLINE."
    }

    "nome válido de jogador online passa" {
        InviteForm.check("Steve_01", online = true).shouldBeNull()
    }

    "cada recusa das regras de membro vira um aviso em maiúsculas" {
        InviteForm.message(InviteCheck.NotOwner) shouldBe "SÓ O DONO DA ILHA PODE CONVIDAR."
        InviteForm.message(InviteCheck.Self) shouldBe "VOCÊ JÁ É O DONO DESTA ILHA."
        InviteForm.message(InviteCheck.AlreadyMember) shouldBe "ESSE JOGADOR JÁ É MEMBRO."
        InviteForm.message(InviteCheck.Full) shouldBe "A ILHA ESTÁ CHEIA. EVOLUA O UPGRADE MEMBROS."
        InviteForm.message(InviteCheck.Blocked) shouldBe "ESSE JOGADOR ESTÁ BLOQUEADO NAS VISITAS."
    }

    "convite enviado avisa o nome" {
        InviteForm.sent("Steve") shouldBe "CONVITE ENVIADO PARA STEVE."
    }
})
