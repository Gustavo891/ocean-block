package one_block.system.spawn.command

import co.aikar.commands.BaseCommand
import co.aikar.commands.annotation.CommandAlias
import co.aikar.commands.annotation.CommandCompletion
import co.aikar.commands.annotation.CommandPermission
import co.aikar.commands.annotation.Default
import co.aikar.commands.annotation.Optional
import co.aikar.commands.annotation.Subcommand
import one_block.libs.coordinate.SimpleLocation
import one_block.system.spawn.npc.NpcAction
import one_block.system.spawn.npc.NpcActionType
import one_block.system.spawn.npc.NpcManager
import one_block.system.spawn.npc.NpcType
import one_block.system.spawn.npc.SpawnNpc
import org.bukkit.entity.Player

@CommandAlias("npc|npcs")
@CommandPermission("screen.dev")
class NpcCommand : BaseCommand() {

    @Default
    @Subcommand("ajuda")
    fun onHelp(sender: Player) {
        sender.sendRichMessage("<gold>NPCs do spawn:")
        sender.sendRichMessage("<yellow>/npc criar <id> <MANNEQUIN|MODEL_ENGINE> [skin ou modelo]")
        sender.sendRichMessage("<yellow>/npc remover <id>")
        sender.sendRichMessage("<yellow>/npc listar")
        sender.sendRichMessage("<yellow>/npc tp <id>")
        sender.sendRichMessage("<yellow>/npc mover <id>")
        sender.sendRichMessage("<yellow>/npc nome <id> <texto>")
        sender.sendRichMessage("<yellow>/npc descricao <id> <texto>")
        sender.sendRichMessage("<yellow>/npc textura <id> <valor base64>")
        sender.sendRichMessage("<yellow>/npc escala <id> <valor>")
        sender.sendRichMessage("<yellow>/npc acao <id> <NONE|PLAYER_COMMAND|CONSOLE_COMMAND|MESSAGE> [valor]")
    }

    @Subcommand("criar")
    @CommandCompletion("@nothing MANNEQUIN|MODEL_ENGINE")
    fun onCreate(sender: Player, id: String, type: NpcType, @Optional value: String?) {
        if (NpcManager.get(id) != null) {
            sender.sendRichMessage("<red>Já existe um npc com o id <white>$id<red>.")
            return
        }

        if (type == NpcType.MODEL_ENGINE && value.isNullOrBlank()) {
            sender.sendRichMessage("<red>Informe o id do modelo do ModelEngine.")
            return
        }

        val npc = SpawnNpc(
            id = id,
            type = type,
            location = SimpleLocation.fromBukkit(sender.location),
            skin = value.takeIf { type == NpcType.MANNEQUIN },
            model = value.takeIf { type == NpcType.MODEL_ENGINE },
        )

        if (!NpcManager.create(npc)) {
            sender.sendRichMessage("<red>Não foi possível criar o npc.")
            return
        }

        sender.sendRichMessage("<green>Npc <white>$id <green>criado.")
    }

    @Subcommand("remover")
    @CommandCompletion("@spawn-npcs")
    fun onRemove(sender: Player, id: String) {
        if (!NpcManager.remove(id)) {
            sender.sendRichMessage("<red>Npc não encontrado.")
            return
        }

        sender.sendRichMessage("<green>Npc <white>$id <green>removido.")
    }

    @Subcommand("listar")
    fun onList(sender: Player) {
        val npcs = NpcManager.all()
        if (npcs.isEmpty()) {
            sender.sendRichMessage("<red>Nenhum npc cadastrado.")
            return
        }

        sender.sendRichMessage("<gold>Npcs (<white>${npcs.size}<gold>):")
        npcs.forEach {
            val extra = it.model ?: it.skin ?: "-"
            sender.sendRichMessage("<yellow>- <white>${it.id} <gray>(${it.type}, $extra)")
        }
    }

    @Subcommand("tp")
    @CommandCompletion("@spawn-npcs")
    fun onTeleport(sender: Player, id: String) {
        val location = NpcManager.locationOf(id)?.toBukkit()
        if (location == null) {
            sender.sendRichMessage("<red>Npc não encontrado ou mundo descarregado.")
            return
        }

        sender.teleport(location)
    }

    @Subcommand("mover")
    @CommandCompletion("@spawn-npcs")
    fun onMove(sender: Player, id: String) {
        val npc = NpcManager.get(id) ?: run {
            sender.sendRichMessage("<red>Npc não encontrado.")
            return
        }

        NpcManager.update(npc.copy(location = SimpleLocation.fromBukkit(sender.location)))
        sender.sendRichMessage("<green>Npc <white>$id <green>movido para a sua posição.")
    }

    @Subcommand("nome")
    @CommandCompletion("@spawn-npcs")
    fun onName(sender: Player, id: String, name: String) {
        val npc = NpcManager.get(id) ?: run {
            sender.sendRichMessage("<red>Npc não encontrado.")
            return
        }

        NpcManager.update(npc.copy(displayName = name))
        sender.sendRichMessage("<green>Nome atualizado.")
    }

    @Subcommand("descricao")
    @CommandCompletion("@spawn-npcs")
    fun onDescription(sender: Player, id: String, description: String) {
        val npc = NpcManager.get(id) ?: run {
            sender.sendRichMessage("<red>Npc não encontrado.")
            return
        }

        NpcManager.update(npc.copy(description = description))
        sender.sendRichMessage("<green>Descrição atualizada.")
    }

    @Subcommand("textura")
    @CommandCompletion("@spawn-npcs")
    fun onTexture(sender: Player, id: String, texture: String) {
        val npc = NpcManager.get(id) ?: run {
            sender.sendRichMessage("<red>Npc não encontrado.")
            return
        }

        NpcManager.update(npc.copy(texture = texture))
        sender.sendRichMessage("<green>Textura atualizada.")
    }

    @Subcommand("escala")
    @CommandCompletion("@spawn-npcs")
    fun onScale(sender: Player, id: String, scale: Double) {
        val npc = NpcManager.get(id) ?: run {
            sender.sendRichMessage("<red>Npc não encontrado.")
            return
        }

        NpcManager.update(npc.copy(scale = scale.coerceIn(0.1, 10.0)))
        sender.sendRichMessage("<green>Escala atualizada.")
    }

    @Subcommand("acao")
    @CommandCompletion("@spawn-npcs NONE|PLAYER_COMMAND|CONSOLE_COMMAND|MESSAGE")
    fun onAction(sender: Player, id: String, type: NpcActionType, @Optional value: String?) {
        val npc = NpcManager.get(id) ?: run {
            sender.sendRichMessage("<red>Npc não encontrado.")
            return
        }

        if (type != NpcActionType.NONE && value.isNullOrBlank()) {
            sender.sendRichMessage("<red>Informe o valor da ação.")
            return
        }

        NpcManager.update(npc.copy(action = NpcAction(type, value)))
        sender.sendRichMessage("<green>Ação atualizada.")
    }
}
