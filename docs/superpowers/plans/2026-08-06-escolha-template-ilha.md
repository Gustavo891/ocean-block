# Escolha de template da ilha — plano de implementação

> **Para agentes:** use `superpowers:subagent-driven-development` ou `superpowers:executing-plans` para executar tarefa a tarefa. Os passos usam checkbox (`- [ ]`).

**Spec:** [`docs/superpowers/specs/2026-08-06-escolha-template-ilha-design.md`](../specs/2026-08-06-escolha-template-ilha-design.md)

**Objetivo:** substituir a criação silenciosa de ilha por uma escolha: quem entra sem ilha vai para um showcase em spectator, navega cinco builds prontas por um menu aberto com SHIFT, e a ilha nasce com a build e o bioma do template escolhido.

**Arquitetura:** builds pré-construídas num mundo `templates`; cinco entidades-câmera compartilhadas por todo o servidor; trocar de template é trocar o `spectatorTarget`. O único trabalho pesado — o paste da schematic via FAWE — roda uma vez por jogador, assíncrono, na confirmação. Código novo em `plugins/oneblock/system/islands/.../template/`.

**Stack:** Kotlin/JVM 2.3.0, Paper 1.21.11, inventory-framework 3.7.1, FastAsyncWorldEdit (API `com.sk89q.worldedit`), Textura (`HudApi`, `Mini.mm`), `lib:actionbar`, MongoDB via `lib:connection`.

## Restrições globais

- **Sem testes.** Este projeto não escreve testes; nenhuma tarefa cria arquivo em `src/test`. A verificação de cada tarefa é compilar e testar em jogo.
- **Sem commits.** O plano nunca roda `git commit` nem `git push`. Quem commita é o dono do repositório.
- **Sem comentários de código** além dos que o padrão do arquivo já pedir.
- **Strings de usuário em português**, MiniMessage via `one_block.libs.formatter.Mini` — nunca `MiniMessage.miniMessage()`, que perderia as tags `<glyph:>` / `<shift:>` da Textura.
- **Build sempre em modo local:** todo comando Gradle precisa de `local` no nome da tarefa, ou a resolução de `com.redescreen:sdk:1.0` falha. O comando padrão de verificação é:

```bash
./gradlew :plugins:oneblock:plugin:localDeploy
```

- **O servidor precisa estar parado** antes do `localDeploy` — o Windows tranca o jar em uso.
- **Direção de dependência:** `islands` nunca depende do módulo do plugin. Novos `lib:*` entram como `compileOnly`.
- **Campo novo no Mongo é nullable.** Documento antigo não tem o campo, e primitivo com default quebra a decodificação.

---

## Estrutura de arquivos

Tudo em `plugins/oneblock/system/islands/src/main/kotlin/one_block/system/islands/template/`, exceto onde indicado.

| Arquivo | Responsabilidade |
|---|---|
| `IslandTemplate.kt` | Modelo imutável de um template. |
| `TemplateRegistry.kt` | Carrega e recarrega `templates.yml`. |
| `TemplateWorld.kt` | Garante o mundo `templates` carregado; expõe se o fluxo está disponível. |
| `TemplateCameras.kt` | Cria/reaproveita/remove as entidades-câmera. |
| `TemplatePaster.kt` | Paste FAWE async + bioma. |
| `SelectionSession.kt` | Estado por jogador. |
| `TemplateSelectionService.kt` | `enter` / `preview` / `confirm` / `exit`. |
| `TemplateSelectView.kt` | Menu com a lista de templates. |
| `TemplateDetailView.kt` | Visualizar / Selecionar. |
| `TemplateListener.kt` | Eventos e guardas. |
| `TemplateCommand.kt` | `/istemplate setpoint\|reload\|list`. |

Modificados: `Island.kt`, `IslandManager.kt`, `IslandsSystem.kt`, `listener/PlayerListener.kt`, `reset/IslandResetService.kt`, `lib/actionbar/.../Channel.kt`, `OneBlockPlugin.kt`, `build-logic/src/main/kotlin/paper-common.gradle.kts`, `gradle/libs.versions.toml`, `plugins/oneblock/system/islands/build.gradle.kts`, `plugins/oneblock/plugin/build.gradle.kts`.

---

## Task 1: Dependências (FAWE, actionbar, repositório)

**Files:**
- Modify: `build-logic/src/main/kotlin/paper-common.gradle.kts:19`
- Modify: `gradle/libs.versions.toml`
- Modify: `plugins/oneblock/system/islands/build.gradle.kts:20`
- Modify: `plugins/oneblock/plugin/build.gradle.kts:48`

**Interfaces:**
- Consumes: nada.
- Produces: `libs.worldedit` disponível no catálogo; `com.sk89q.worldedit.*` e `one_block.libs.actionbar.*` compiláveis dentro de `islands`.

- [ ] **Passo 1: adicionar o repositório do EngineHub**

Em `paper-common.gradle.kts`, na lista de `repositories`, logo após a linha do codemc:

```kotlin
    maven("https://maven.enginehub.org/repo/")
```

- [ ] **Passo 2: adicionar a versão e a lib no catálogo**

Em `gradle/libs.versions.toml`, em `[versions]`:

```toml
worldedit = "7.3.8"
```

Em `[libraries]`:

```toml
worldedit = { module = "com.sk89q.worldedit:worldedit-bukkit", version.ref = "worldedit" }
```

Se `7.3.8` não resolver, suba a versão até a mais recente publicada em `maven.enginehub.org` que a FAWE 2.15.1 acompanha. As classes usadas (`ClipboardFormats`, `ClipboardHolder`, `BukkitAdapter`, `CuboidRegion`) existem em toda a linha 7.3.x.

- [ ] **Passo 3: declarar as dependências no módulo islands**

Em `plugins/oneblock/system/islands/build.gradle.kts`, dentro de `dependencies`:

```kotlin
    compileOnly(libs.worldedit)
    compileOnly(projects.lib.actionbar)
```

`compileOnly` porque a FAWE é um plugin do servidor (não pode ser shaded) e `lib:actionbar` já é shaded pelo módulo do plugin.

- [ ] **Passo 4: declarar a FAWE como dependência dura do plugin**

Em `plugins/oneblock/plugin/build.gradle.kts`, no bloco `bukkit`:

```kotlin
    depend = listOf("PlaceholderAPI", "SkinsRestorer", "Textura", "FastAsyncWorldEdit")
```

- [ ] **Passo 5: verificar**

```bash
./gradlew :plugins:oneblock:plugin:localDeploy
```

Esperado: BUILD SUCCESSFUL. Se falhar na resolução do worldedit, ajuste a versão do passo 2 e repita.

---

## Task 2: Modelo e registry dos templates

**Files:**
- Create: `.../template/IslandTemplate.kt`
- Create: `.../template/TemplateRegistry.kt`
- Create: `server/plugins/one-block/templates/templates.yml`

**Interfaces:**
- Consumes: `libs.coordinate.SimpleLocation`, `libs.instance.Instance`.
- Produces:
  - `data class IslandTemplate(id: String, displayName: String, description: List<String>, biome: Biome, schematic: String, iconMaterial: Material, iconModel: String?, camera: SimpleLocation)`
  - `TemplateRegistry.load()`, `TemplateRegistry.all(): List<IslandTemplate>`, `TemplateRegistry.get(id: String): IslandTemplate?`, `TemplateRegistry.isEmpty(): Boolean`, `TemplateRegistry.file(template: IslandTemplate): File`, `TemplateRegistry.setCamera(id: String, location: SimpleLocation): Boolean`

- [ ] **Passo 1: criar o modelo**

`IslandTemplate.kt`:

```kotlin
package one_block.system.islands.template

import one_block.libs.coordinate.SimpleLocation
import org.bukkit.Material
import org.bukkit.block.Biome

data class IslandTemplate(
    val id: String,
    val displayName: String,
    val description: List<String>,
    val biome: Biome,
    val schematic: String,
    val iconMaterial: Material,
    val iconModel: String?,
    val camera: SimpleLocation?,
)
```

`camera` é nullable porque um template recém-adicionado ao YAML ainda não teve o ponto capturado pelo comando de admin. Template sem câmera é carregado, mas não entra no menu.

- [ ] **Passo 2: criar o registry**

`TemplateRegistry.kt`. Mesmo contrato do `PhaseManager`: entrada inválida é logada e pulada, nunca derruba o carregamento.

```kotlin
package one_block.system.islands.template

import one_block.libs.coordinate.SimpleLocation
import one_block.libs.instance.Instance
import org.bukkit.Material
import org.bukkit.block.Biome
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File

object TemplateRegistry {

    private const val FOLDER = "templates"
    private const val FILE_NAME = "templates.yml"

    private val templates = mutableListOf<IslandTemplate>()

    private val folder: File
        get() = File(Instance.dataFolder, FOLDER)

    private val configFile: File
        get() = File(folder, FILE_NAME)

    fun load() {
        templates.clear()

        if (!configFile.exists()) {
            Instance.logger.severe("[templates] $FILE_NAME não encontrado em ${folder.path}.")
            return
        }

        val config = YamlConfiguration.loadConfiguration(configFile)
        val section = config.getConfigurationSection("templates")

        if (section == null) {
            Instance.logger.severe("[templates] $FILE_NAME não tem a seção 'templates'.")
            return
        }

        section.getKeys(false).forEach { id ->
            val entry = section.getConfigurationSection(id) ?: return@forEach
            val template = parse(id, entry) ?: return@forEach

            if (!File(folder, template.schematic).exists()) {
                Instance.logger.severe("[templates] '$id' ignorado: schematic '${template.schematic}' não existe.")
                return@forEach
            }

            templates.add(template)
        }

        Instance.logger.info("[templates] ${templates.size} template(s) carregado(s).")
    }

    fun all(): List<IslandTemplate> = templates.filter { it.camera != null }

    fun get(id: String): IslandTemplate? = templates.find { it.id == id }

    fun isEmpty(): Boolean = all().isEmpty()

    fun file(template: IslandTemplate): File = File(folder, template.schematic)

    fun setCamera(id: String, location: SimpleLocation): Boolean {
        if (!configFile.exists()) return false

        val config = YamlConfiguration.loadConfiguration(configFile)
        val path = "templates.$id.camera"

        if (!config.contains("templates.$id")) return false

        config.set("$path.world", location.world)
        config.set("$path.x", location.x)
        config.set("$path.y", location.y)
        config.set("$path.z", location.z)
        config.set("$path.yaw", location.yaw.toDouble())
        config.set("$path.pitch", location.pitch.toDouble())
        config.save(configFile)

        load()
        return true
    }

    private fun parse(id: String, entry: org.bukkit.configuration.ConfigurationSection): IslandTemplate? {
        val biomeName = entry.getString("biome") ?: run {
            Instance.logger.severe("[templates] '$id' ignorado: falta 'biome'.")
            return null
        }

        val biome = runCatching { Biome.valueOf(biomeName.uppercase()) }.getOrNull() ?: run {
            Instance.logger.severe("[templates] '$id' ignorado: bioma '$biomeName' inválido.")
            return null
        }

        val schematic = entry.getString("schematic") ?: run {
            Instance.logger.severe("[templates] '$id' ignorado: falta 'schematic'.")
            return null
        }

        val iconName = entry.getString("icon.material") ?: "GRASS_BLOCK"
        val iconMaterial = runCatching { Material.valueOf(iconName.uppercase()) }.getOrNull() ?: run {
            Instance.logger.severe("[templates] '$id' ignorado: material '$iconName' inválido.")
            return null
        }

        val cameraSection = entry.getConfigurationSection("camera")
        val camera = cameraSection?.let {
            SimpleLocation(
                x = it.getDouble("x"),
                y = it.getDouble("y"),
                z = it.getDouble("z"),
                yaw = it.getDouble("yaw").toFloat(),
                pitch = it.getDouble("pitch").toFloat(),
                world = it.getString("world") ?: "templates",
            )
        }

        if (camera == null) {
            Instance.logger.warning("[templates] '$id' sem câmera: use /istemplate setpoint $id para capturar.")
        }

        return IslandTemplate(
            id = id,
            displayName = entry.getString("display-name") ?: id,
            description = entry.getStringList("description"),
            biome = biome,
            schematic = schematic,
            iconMaterial = iconMaterial,
            iconModel = entry.getString("icon.model"),
            camera = camera,
        )
    }
}
```

`Biome` em 1.21.11 pode já ser uma `Registry` em vez de enum. Se `Biome.valueOf` não compilar, troque por
`Registry.BIOME.get(NamespacedKey.minecraft(biomeName.lowercase()))` e ajuste o tipo do campo. Confira contra o jar da paper-api antes de assumir.

- [ ] **Passo 3: criar o YAML de exemplo**

`server/plugins/one-block/templates/templates.yml`:

```yaml
templates:
  lush:
    display-name: "Região Exuberante"
    description:
      - "Florestas densas, campos abertos"
      - "e montanhas acidentadas."
    biome: PLAINS
    schematic: lush.schem
    icon:
      material: GRASS_BLOCK
  nether:
    display-name: "Terras Ardentes"
    description:
      - "Rocha viva e rios de lava."
    biome: NETHER_WASTES
    schematic: nether.schem
    icon:
      material: NETHERRACK
```

A ordem das chaves é a ordem do menu. Os `.schem` são colocados na mesma pasta pelo builder; o registry pula templates sem arquivo.

- [ ] **Passo 4: verificar**

```bash
./gradlew :plugins:oneblock:plugin:localDeploy
```

Esperado: BUILD SUCCESSFUL.

---

## Task 3: Mundo de templates e entidades-câmera

**Files:**
- Create: `.../template/TemplateWorld.kt`
- Create: `.../template/TemplateCameras.kt`

**Interfaces:**
- Consumes: `TemplateRegistry.all()`, `IslandTemplate.camera`.
- Produces:
  - `TemplateWorld.WORLD_NAME: String`, `TemplateWorld.world(): World?`, `TemplateWorld.isAvailable(): Boolean`
  - `TemplateCameras.ensure()`, `TemplateCameras.of(template: IslandTemplate): Entity?`, `TemplateCameras.clear()`

- [ ] **Passo 1: criar TemplateWorld**

```kotlin
package one_block.system.islands.template

import one_block.libs.instance.Instance
import org.bukkit.Bukkit
import org.bukkit.World

object TemplateWorld {

    const val WORLD_NAME = "templates"

    fun world(): World? = Bukkit.getWorld(WORLD_NAME)

    fun isAvailable(): Boolean {
        if (world() == null) {
            Instance.logger.severe(
                "[templates] Mundo '$WORLD_NAME' não carregado — a escolha de template está desligada."
            )
            return false
        }

        if (TemplateRegistry.isEmpty()) {
            Instance.logger.severe(
                "[templates] Nenhum template válido com câmera — a escolha de template está desligada."
            )
            return false
        }

        return true
    }
}
```

O mundo é criado pelo Multiverse (já instalado no servidor), não por código. `isAvailable()` é a chave que faz o fluxo antigo assumir quando algo está faltando.

- [ ] **Passo 2: criar TemplateCameras**

```kotlin
package one_block.system.islands.template

import one_block.libs.instance.Instance
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.Entity
import org.bukkit.persistence.PersistentDataType
import org.bukkit.NamespacedKey

object TemplateCameras {

    private const val EYE_OFFSET = 1.62

    private val key = NamespacedKey(Instance, "template_camera")

    private val cameras = mutableMapOf<String, ArmorStand>()

    fun ensure() {
        clear()

        val world = TemplateWorld.world() ?: return

        world.entities
            .filter { it.persistentDataContainer.has(key, PersistentDataType.STRING) }
            .forEach { it.remove() }

        TemplateRegistry.all().forEach { template ->
            val camera = template.camera ?: return@forEach
            val location = camera.toBukkit() ?: return@forEach
            val anchor = location.clone().subtract(0.0, EYE_OFFSET, 0.0)

            val chunk = anchor.chunk
            world.setChunkForceLoaded(chunk.x, chunk.z, true)

            val stand = world.spawn(anchor, ArmorStand::class.java) { stand ->
                stand.isVisible = false
                stand.setGravity(false)
                stand.isInvulnerable = true
                stand.isSilent = true
                stand.isPersistent = false
                stand.setAI(false)
                stand.setRotation(camera.yaw, camera.pitch)
                stand.persistentDataContainer.set(key, PersistentDataType.STRING, template.id)
            }

            cameras[template.id] = stand
        }

        Instance.logger.info("[templates] ${cameras.size} câmera(s) posicionada(s).")
    }

    fun of(template: IslandTemplate): Entity? = cameras[template.id]

    fun clear() {
        cameras.values.forEach { it.remove() }
        cameras.clear()
    }
}
```

O chunk é forçado a ficar carregado para a câmera não sumir e a build renderizar assim que o jogador entra. As câmeras são `isPersistent = false` e recriadas todo boot; a varredura por PDC limpa sobras de um desligamento sujo.

- [ ] **Passo 3: registrar no boot**

Em `IslandsSystem.init`, dentro do `runTaskLater` existente, depois de `IslandManager.onInit()`:

```kotlin
            TemplateRegistry.load()
            if (TemplateWorld.isAvailable()) TemplateCameras.ensure()
```

Importe `one_block.system.islands.template.*`.

- [ ] **Passo 4: verificar em jogo**

Deploy, suba o servidor, e confira no console: `[templates] N template(s) carregado(s).` e `[templates] N câmera(s) posicionada(s).`

Com o mundo `templates` ausente, o log esperado é `Mundo 'templates' não carregado` e nenhuma câmera — sem stacktrace.

---

## Task 4: Paste da schematic e campo `templateId`

**Files:**
- Create: `.../template/TemplatePaster.kt`
- Modify: `.../island/Island.kt:22-39`
- Modify: `.../island/IslandManager.kt:110-162,230-240`

**Interfaces:**
- Consumes: `TemplateRegistry.file(template)`, `IslandTemplate.biome`, `reset.IslandArea`.
- Produces:
  - `TemplatePaster.paste(island: Island, template: IslandTemplate, onDone: (Boolean) -> Unit)` — chama `onDone` na main thread com `true` em sucesso.
  - `Island.templateId: String?`
  - `IslandManager.createIsland(player: Player, template: IslandTemplate?): Island?`

- [ ] **Passo 1: adicionar o campo no modelo**

Em `Island.kt`, no construtor, após `var settings: MutableMap<String, Boolean>? = null,`:

```kotlin
    var templateId: String? = null,
```

Nullable de propósito: ilha antiga no Mongo não tem o campo, e primitivo com default quebra a decodificação do documento existente.

- [ ] **Passo 2: criar o paster**

```kotlin
package one_block.system.islands.template

import com.sk89q.worldedit.WorldEdit
import com.sk89q.worldedit.bukkit.BukkitAdapter
import com.sk89q.worldedit.function.operation.Operations
import com.sk89q.worldedit.math.BlockVector3
import com.sk89q.worldedit.regions.CuboidRegion
import com.sk89q.worldedit.session.ClipboardHolder
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats
import one_block.libs.instance.Instance
import one_block.system.islands.island.Island
import one_block.system.islands.reset.IslandArea
import org.bukkit.Bukkit
import java.io.FileInputStream

object TemplatePaster {

    fun paste(island: Island, template: IslandTemplate, onDone: (Boolean) -> Unit) {
        val area = IslandArea.of(island)
        val center = island.center.toBukkit()

        if (area == null || center == null) {
            Instance.logger.severe("[templates] Paste abortado: mundo da ilha ${island.uuid} não carregado.")
            onDone(false)
            return
        }

        val file = TemplateRegistry.file(template)

        Bukkit.getScheduler().runTaskAsynchronously(Instance, Runnable {
            val ok = runCatching {
                val format = ClipboardFormats.findByFile(file)
                    ?: error("formato de schematic desconhecido: ${file.name}")

                val clipboard = FileInputStream(file).use { input ->
                    format.getReader(input).use { it.read() }
                }

                val weWorld = BukkitAdapter.adapt(area.world)

                WorldEdit.getInstance().newEditSession(weWorld).use { edit ->
                    val operation = ClipboardHolder(clipboard)
                        .createPaste(edit)
                        .to(BlockVector3.at(center.blockX, center.blockY, center.blockZ))
                        .ignoreAirBlocks(false)
                        .build()

                    Operations.complete(operation)

                    val region = CuboidRegion(
                        weWorld,
                        BlockVector3.at(area.minX, area.minY, area.minZ),
                        BlockVector3.at(area.maxX, area.maxY, area.maxZ),
                    )

                    edit.setBiome(region, BukkitAdapter.adapt(template.biome))
                }
            }.onFailure {
                Instance.logger.severe(
                    "[templates] Falha ao colar '${template.id}' na ilha ${island.uuid}: ${it.message}"
                )
            }.isSuccess

            Bukkit.getScheduler().runTask(Instance, Runnable { onDone(ok) })
        })
    }
}
```

Roda em `runTaskAsynchronously` e não no `ScheduledExecutor` porque o `EditSession` da FAWE é feito para thread paralela e devolve o resultado por callback — o `ScheduledExecutor` é reservado a trabalho que nunca toca o mundo.

`edit.setBiome(region, biomeType)` e `BukkitAdapter.adapt(Biome)` mudam de assinatura entre linhas do WorldEdit. Se não compilar, confira as assinaturas reais no jar antes de improvisar; a alternativa é iterar as colunas do `CuboidRegion` chamando `edit.setBiome(BlockVector3, BiomeType)`.

- [ ] **Passo 3: aceitar o template na criação da ilha**

Em `IslandManager.kt`, troque a assinatura e o corpo relevante de `createIsland`:

```kotlin
    fun createIsland(player: Player, template: IslandTemplate? = null): Island? {
```

Remova a chamada `generateStarterIsland(center)` de dentro dela e coloque, no lugar:

```kotlin
        if (template == null) generateStarterIsland(center)
```

E, na construção do `Island(...)`, acrescente o campo:

```kotlin
            isPublic = false,
            templateId = template?.id,
```

`generateStarterIsland` **continua existindo**: é o caminho de fallback quando o fluxo de template não está disponível ou o paste falha.

Importe `one_block.system.islands.template.IslandTemplate`.

- [ ] **Passo 4: verificar**

```bash
./gradlew :plugins:oneblock:plugin:localDeploy
```

Esperado: BUILD SUCCESSFUL. Se o compilador reclamar de `setBiome` ou `adapt`, ajuste conforme o passo 2.

---

## Task 5: Sessão, serviço e canal da action bar

**Files:**
- Create: `.../template/SelectionSession.kt`
- Create: `.../template/TemplateSelectionService.kt`
- Modify: `lib/actionbar/src/main/kotlin/one_block/libs/actionbar/Channel.kt`

**Interfaces:**
- Consumes: `TemplateRegistry.all()`, `TemplateCameras.of(...)`, `TemplatePaster.paste(...)`, `IslandManager.createIsland(player, template)`, `HudApi`, `ActionBar`.
- Produces:
  - `Channel.TemplateSelect`
  - `TemplateSelectionService.enter(player: Player)`, `.preview(player: Player, template: IslandTemplate)`, `.confirm(player: Player, template: IslandTemplate)`, `.exit(player: Player)`, `.isSelecting(player: Player): Boolean`, `.current(player: Player): IslandTemplate?`, `.isConfirming(player: Player): Boolean`

- [ ] **Passo 1: adicionar o canal**

Em `Channel.kt`:

```kotlin
enum class Channel(val priority: Int) {
    FlyTimer(0),
    Combat(10),
    SkillXp(20),
    TemplateSelect(30),
}
```

Prioridade acima de todas: durante a seleção nada mais pode roubar o rodapé.

- [ ] **Passo 2: criar a sessão**

```kotlin
package one_block.system.islands.template

data class SelectionSession(
    var templateId: String,
    var confirming: Boolean = false,
)
```

- [ ] **Passo 3: criar o serviço**

Único ponto do sistema que mexe em gamemode e `spectatorTarget`.

```kotlin
package one_block.system.islands.template

import one_block.libs.actionbar.ActionBar
import one_block.libs.actionbar.Channel
import one_block.libs.formatter.Mini
import one_block.libs.instance.Instance
import one_block.system.islands.island.IslandManager
import one_block.textura.api.HudApi
import one_block.textura.hud.HudContent
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.entity.Player
import java.util.UUID

object TemplateSelectionService {

    const val HUD_SLOT = "template"

    private const val HINT = "<gray>Aperte <white>SHIFT</white> para escolher sua ilha"
    private const val ACTION_BAR_TICKS = 60

    private val sessions = mutableMapOf<UUID, SelectionSession>()

    private var refreshTask: Int = -1

    fun isSelecting(player: Player): Boolean = sessions.containsKey(player.uniqueId)

    fun isConfirming(player: Player): Boolean = sessions[player.uniqueId]?.confirming == true

    fun current(player: Player): IslandTemplate? =
        sessions[player.uniqueId]?.let { TemplateRegistry.get(it.templateId) }

    fun enter(player: Player) {
        val first = TemplateRegistry.all().firstOrNull() ?: return

        sessions[player.uniqueId] = SelectionSession(first.id)

        player.inventory.clear()
        player.gameMode = GameMode.SPECTATOR
        attach(player, first)
        startRefresh()
    }

    fun preview(player: Player, template: IslandTemplate) {
        val session = sessions[player.uniqueId] ?: return
        if (session.confirming) return

        session.templateId = template.id
        attach(player, template)
    }

    fun confirm(player: Player, template: IslandTemplate) {
        val session = sessions[player.uniqueId] ?: return
        if (session.confirming) return

        session.confirming = true

        player.closeInventory()
        player.sendTitlePart(
            net.kyori.adventure.title.TitlePart.TIMES,
            net.kyori.adventure.title.Title.Times.times(
                java.time.Duration.ofMillis(250),
                java.time.Duration.ofSeconds(5),
                java.time.Duration.ofMillis(500),
            ),
        )
        player.sendTitlePart(net.kyori.adventure.title.TitlePart.TITLE, Mini.deserialize("<black>"))
        player.sendTitlePart(
            net.kyori.adventure.title.TitlePart.SUBTITLE,
            Mini.deserialize("<gray>Construindo sua ilha..."),
        )

        val island = IslandManager.createIsland(player, template)

        if (island == null) {
            session.confirming = false
            player.sendRichMessage("<red>Não foi possível criar sua ilha agora. Avise um administrador.")
            player.clearTitle()
            return
        }

        TemplatePaster.paste(island, template) { ok ->
            if (!ok) {
                player.sendRichMessage("<red>Sua ilha foi criada, mas o template falhou ao ser colado.")
            }

            exit(player)
            player.clearTitle()
            island.teleportTo(player)
        }
    }

    fun exit(player: Player) {
        sessions.remove(player.uniqueId) ?: return

        player.spectatorTarget = null
        player.gameMode = GameMode.SURVIVAL
        HudApi.hide(player, HUD_SLOT)
        ActionBar.clear(player, Channel.TemplateSelect)

        stopRefreshIfIdle()
    }

    private fun attach(player: Player, template: IslandTemplate) {
        val camera = TemplateCameras.of(template)

        if (camera == null) {
            Instance.logger.severe("[templates] Sem câmera para '${template.id}'.")
            return
        }

        player.spectatorTarget = null
        player.teleport(camera.location)
        player.spectatorTarget = camera

        render(player, template)
    }

    private fun render(player: Player, template: IslandTemplate) {
        val description = template.description.joinToString("<gray> ") { it }

        HudApi.show(
            player,
            HUD_SLOT,
            HudContent(text = "<white>${template.displayName}<gray> — $description"),
        )

        ActionBar.send(player, Channel.TemplateSelect, Mini.deserialize(HINT), ACTION_BAR_TICKS)
    }

    private fun startRefresh() {
        if (refreshTask != -1) return

        refreshTask = Bukkit.getScheduler().runTaskTimer(Instance, Runnable {
            sessions.keys.toList().forEach { uuid ->
                val player = Bukkit.getPlayer(uuid) ?: return@forEach
                if (isConfirming(player)) return@forEach
                val template = current(player) ?: return@forEach
                render(player, template)
            }
        }, 40L, 40L).taskId
    }

    private fun stopRefreshIfIdle() {
        if (sessions.isNotEmpty() || refreshTask == -1) return

        Bukkit.getScheduler().cancelTask(refreshTask)
        refreshTask = -1
    }
}
```

A `ActionBar` já se reenvia sozinha enquanto a entrada está viva; o task de 40 ticks existe para o caso do prazo expirar e para manter a HUD do topo em pé.

- [ ] **Passo 4: verificar**

```bash
./gradlew :plugins:oneblock:plugin:localDeploy
```

Esperado: BUILD SUCCESSFUL. `sendTitlePart` e `Title.Times` são da Adventure embutida na paper-api; se a assinatura divergir, confira no jar.

---

## Task 6: Menus de seleção e detalhe

**Files:**
- Create: `.../template/TemplateSelectView.kt`
- Create: `.../template/TemplateDetailView.kt`
- Modify: `.../IslandsSystem.kt:33-41`

**Interfaces:**
- Consumes: `TemplateRegistry.all()`, `TemplateSelectionService.preview/confirm/current`.
- Produces: as duas views registradas no `viewFrame`; `TemplateDetailView` recebe o `IslandTemplate` como estado inicial (`openForPlayer(TemplateDetailView::class.java, template)`).

Segue o par que `islands` já usa: `PhaseView` → `PhaseDetailView`.

- [ ] **Passo 1: criar a view da lista**

```kotlin
package one_block.system.islands.template

import me.devnatan.inventoryframework.View
import me.devnatan.inventoryframework.ViewConfigBuilder
import one_block.libs.item_builder.addLore
import one_block.libs.item_builder.itemStack
import one_block.libs.item_builder.meta
import one_block.libs.item_builder.name
import org.bukkit.inventory.meta.ItemMeta

class TemplateSelectView : View() {

    private val pagination = lazyPaginationState(
        { TemplateRegistry.all().toMutableList() }
    ) { _, builder, _, value ->
        builder.renderWith { createItem(value) }
            .onClick { click -> click.openForPlayer(TemplateDetailView::class.java, value) }
    }

    override fun onInit(config: ViewConfigBuilder) {
        config.cancelInteractions()
        config.title("Escolha sua ilha")
        config.layout(
            "         ",
            "  OOOOO  ",
            "         ",
        )
    }

    private fun createItem(value: IslandTemplate) = itemStack(value.iconMaterial) {
        meta<ItemMeta> {
            name("<yellow>${value.displayName}")

            addLore {
                value.description.forEach { +"<gray>$it" }
                +""
                +"<white>Clique para ver as opções"
            }
        }
    }
}
```

- [ ] **Passo 2: criar a view de detalhe**

```kotlin
package one_block.system.islands.template

import me.devnatan.inventoryframework.View
import me.devnatan.inventoryframework.ViewConfigBuilder
import me.devnatan.inventoryframework.context.RenderContext
import one_block.libs.item_builder.addLore
import one_block.libs.item_builder.itemStack
import one_block.libs.item_builder.meta
import one_block.libs.item_builder.name
import org.bukkit.Material
import org.bukkit.inventory.meta.ItemMeta

class TemplateDetailView : View() {

    private val template = initialState<IslandTemplate>()

    override fun onInit(config: ViewConfigBuilder) {
        config.cancelInteractions()
        config.title("Template")
        config.layout(
            "         ",
            "   V S   ",
            "         ",
        )
    }

    override fun onFirstRender(render: RenderContext) {
        val value = template[render]

        render.slot('V', preview(value)).onClick { click ->
            TemplateSelectionService.preview(click.player, value)
            click.closeForPlayer()
        }

        render.slot('S', select(value)).onClick { click ->
            TemplateSelectionService.confirm(click.player, value)
        }
    }

    private fun preview(value: IslandTemplate) = itemStack(Material.ENDER_EYE) {
        meta<ItemMeta> {
            name("<yellow>Visualizar")

            addLore {
                +"<gray>Ver <white>${value.displayName}</white> de perto."
            }
        }
    }

    private fun select(value: IslandTemplate) = itemStack(Material.LIME_DYE) {
        meta<ItemMeta> {
            name("<green>Selecionar")

            addLore {
                +"<gray>Começar sua ilha com"
                +"<gray>o template <white>${value.displayName}</white>."
                +""
                +"<red>Essa escolha não pode ser trocada depois."
            }
        }
    }
}
```

Se `render.slot(char, item)` não existir nessa versão da inventory-framework, use o mesmo caminho do `PhaseView`/`createEmptyRender` já presente no projeto. Confira `ViewItems.kt` antes de inventar API.

- [ ] **Passo 3: registrar as views**

Em `IslandsSystem.init`, dentro do `viewFrame.with(...)`, some:

```kotlin
            TemplateSelectView(),
            TemplateDetailView(),
```

- [ ] **Passo 4: verificar**

```bash
./gradlew :plugins:oneblock:plugin:localDeploy
```

Esperado: BUILD SUCCESSFUL.

---

## Task 7: Listener, guardas e desvio do spawn

**Files:**
- Create: `.../template/TemplateListener.kt`
- Modify: `.../listener/PlayerListener.kt:27-38`
- Modify: `plugins/oneblock/plugin/.../OneBlockPlugin.kt:144-149`

**Interfaces:**
- Consumes: `TemplateSelectionService`, `TemplateWorld`, `TemplateRegistry`, `IslandManager`.
- Produces: `TemplateEligibility.needsTemplate(player: Player): Boolean` (dentro de `TemplateListener.kt`).

- [ ] **Passo 1: criar o listener**

```kotlin
package one_block.system.islands.template

import me.devnatan.inventoryframework.ViewFrame
import one_block.libs.instance.Instance
import one_block.system.islands.island.IslandManager
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.FoodLevelChangeEvent
import org.bukkit.event.player.PlayerCommandPreprocessEvent
import org.bukkit.event.player.PlayerDropItemEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.event.player.PlayerStopSpectatingEntityEvent
import org.bukkit.event.player.PlayerTeleportEvent
import org.bukkit.event.player.PlayerToggleSneakEvent

object TemplateEligibility {

    fun needsTemplate(player: Player): Boolean {
        if (IslandManager.getIslandByOwnerUUID(player.uniqueId) != null) return false

        val name = player.name.lowercase()

        return IslandManager.islandsById.values.none {
            name in it.members.keys || name in it.trustedNames
        }
    }
}

class TemplateListener(private val viewFrame: ViewFrame) : Listener {

    private val allowedCommands = setOf("/spawn", "/ajuda", "/help")

    @EventHandler
    fun onJoin(event: PlayerJoinEvent) {
        val player = event.player
        if (!TemplateWorld.isAvailable()) return
        if (!TemplateEligibility.needsTemplate(player)) return

        Bukkit.getScheduler().runTaskLater(Instance, Runnable {
            if (!player.isOnline) return@Runnable
            TemplateSelectionService.enter(player)
        }, 5L)
    }

    @EventHandler
    fun onSneak(event: PlayerToggleSneakEvent) {
        val player = event.player
        if (!event.isSneaking) return
        if (!TemplateSelectionService.isSelecting(player)) return
        if (TemplateSelectionService.isConfirming(player)) return

        viewFrame.open(TemplateSelectView::class.java, player)
    }

    @EventHandler
    fun onStopSpectating(event: PlayerStopSpectatingEntityEvent) {
        if (!TemplateSelectionService.isSelecting(event.player)) return
        event.isCancelled = true
    }

    @EventHandler
    fun onQuit(event: PlayerQuitEvent) {
        if (!TemplateSelectionService.isSelecting(event.player)) return
        TemplateSelectionService.exit(event.player)
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onTeleport(event: PlayerTeleportEvent) {
        if (!TemplateSelectionService.isSelecting(event.player)) return
        if (event.cause == PlayerTeleportEvent.TeleportCause.PLUGIN) return
        event.isCancelled = true
    }

    @EventHandler
    fun onDamage(event: EntityDamageEvent) {
        val player = event.entity as? Player ?: return
        if (!TemplateSelectionService.isSelecting(player)) return
        event.isCancelled = true
    }

    @EventHandler
    fun onHunger(event: FoodLevelChangeEvent) {
        val player = event.entity as? Player ?: return
        if (!TemplateSelectionService.isSelecting(player)) return
        event.isCancelled = true
    }

    @EventHandler
    fun onDrop(event: PlayerDropItemEvent) {
        if (!TemplateSelectionService.isSelecting(event.player)) return
        event.isCancelled = true
    }

    @EventHandler
    fun onCommand(event: PlayerCommandPreprocessEvent) {
        val player = event.player
        if (!TemplateSelectionService.isSelecting(player)) return
        if (player.hasPermission("screen.dev")) return

        val command = event.message.split(" ").first().lowercase()
        if (command in allowedCommands) return

        event.isCancelled = true
        player.sendRichMessage("<red>Escolha sua ilha antes de usar comandos.")
    }
}
```

O `exit` no quit apenas limpa o estado em memória — nada é gravado, então o jogador volta ao showcase no próximo login.

- [ ] **Passo 2: desviar o spawn e parar de criar ilha às cegas**

Em `listener/PlayerListener.kt`, substitua `onPlayerSpawnLocation` inteiro por:

```kotlin
    @EventHandler(priority = EventPriority.HIGHEST)
    fun onPlayerSpawnLocation(event: PlayerSpawnLocationEvent) {
        val player = event.player

        if (!TemplateEligibility.needsTemplate(player)) return

        if (TemplateWorld.isAvailable()) {
            val camera = TemplateRegistry.all().firstOrNull()?.camera?.toBukkit() ?: return
            event.spawnLocation = camera
            return
        }

        val created = IslandManager.createIsland(player) ?: return
        val spawn = created.spawn.add(.0, 2.0, .0).toBukkit() ?: return

        event.spawnLocation = spawn
    }
```

Com o fluxo disponível, o spawn cai no mundo `templates` e o `TemplateListener` assume no join. Sem ele, o comportamento antigo continua valendo — ninguém fica preso.

Importe `one_block.system.islands.template.TemplateEligibility`, `TemplateRegistry` e `TemplateWorld`.

- [ ] **Passo 3: registrar o listener**

Em `OneBlockPlugin.setupListeners()`:

```kotlin
        registerEvents(one_block.system.islands.template.TemplateListener(viewFrame), this@OneBlockPlugin)
```

Use a mesma referência de `viewFrame` já criada no `onEnable`.

- [ ] **Passo 4: verificar em jogo**

Deploy, servidor de pé, e com uma conta **sem ilha**:

1. Entrar → cai em spectator, câmera travada no primeiro template, HUD no topo com nome/descrição, instrução no rodapé.
2. SHIFT → abre o menu com os templates.
3. Clicar num template → duas opções. **Visualizar** fecha o menu e troca a câmera; a HUD acompanha.
4. Tentar sair da câmera com SHIFT durante a cena → continua preso.
5. Tomar dano/fome/soltar item/teleportar → nada acontece.
6. Deslogar sem escolher e voltar → cai no showcase de novo, sem ilha criada.

---

## Task 8: Reset recola o template

**Files:**
- Modify: `.../reset/IslandResetService.kt:152-158`

**Interfaces:**
- Consumes: `Island.templateId`, `TemplateRegistry.get(id)`, `TemplatePaster.paste(...)`.
- Produces: nada novo.

- [ ] **Passo 1: trocar o restoreCenter**

Em `IslandResetService.kt`, substitua `restoreCenter` por:

```kotlin
    private fun restoreCenter(island: Island) {
        val template = island.templateId?.let { TemplateRegistry.get(it) }

        if (template == null) {
            restoreDefaultCenter(island)
            return
        }

        TemplatePaster.paste(island, template) { ok ->
            if (!ok) restoreDefaultCenter(island)
        }
    }

    private fun restoreDefaultCenter(island: Island) {
        val center = island.center.toBukkit() ?: return
        val phaseBlock = island.getCurrentPhase()?.blockPool?.getRandomByWeight() ?: Material.STONE

        center.world.getBlockAt(center.blockX, center.blockY - 1, center.blockZ).type = Material.BEDROCK
        center.block.type = phaseBlock
    }
```

Ilha sem `templateId` — documento anterior ao sistema — cai no comportamento atual. Importe `one_block.system.islands.template.TemplatePaster` e `TemplateRegistry`.

- [ ] **Passo 2: verificar em jogo**

Com uma ilha criada por template, rode `/is reset terrain <jogador> -confirm`. Esperado: o terreno é limpo e a build do template volta, com o bioma. Com uma ilha antiga (sem `templateId`), esperado: bedrock + bloco da fase, exatamente como antes.

---

## Task 9: Comando de administração e fechamento

**Files:**
- Create: `.../template/TemplateCommand.kt`
- Modify: `plugins/oneblock/plugin/.../OneBlockPlugin.kt:120-142`

**Interfaces:**
- Consumes: `TemplateRegistry.setCamera/all/load`, `TemplateCameras.ensure`.
- Produces: `/istemplate list|setpoint <id>|reload`.

- [ ] **Passo 1: criar o comando**

```kotlin
package one_block.system.islands.template

import co.aikar.commands.BaseCommand
import co.aikar.commands.annotation.CommandAlias
import co.aikar.commands.annotation.CommandPermission
import co.aikar.commands.annotation.Default
import co.aikar.commands.annotation.Subcommand
import one_block.libs.coordinate.SimpleLocation
import org.bukkit.entity.Player

@CommandAlias("istemplate")
@CommandPermission("screen.dev")
class TemplateCommand : BaseCommand() {

    @Default
    @Subcommand("list")
    fun list(player: Player) {
        val all = TemplateRegistry.all()

        if (all.isEmpty()) {
            player.sendRichMessage("<red>Nenhum template com câmera configurada.")
            return
        }

        player.sendRichMessage("<gold>Templates:")
        all.forEach {
            player.sendRichMessage("<gray>- <white>${it.id}</white> (${it.displayName})")
        }
    }

    @Subcommand("setpoint")
    fun setPoint(player: Player, id: String) {
        val location = SimpleLocation.fromBukkit(player.location)

        if (location.world != TemplateWorld.WORLD_NAME) {
            player.sendRichMessage("<red>Você precisa estar no mundo <white>${TemplateWorld.WORLD_NAME}</white>.")
            return
        }

        if (!TemplateRegistry.setCamera(id, location)) {
            player.sendRichMessage("<red>Template <white>$id</white> não existe no templates.yml.")
            return
        }

        TemplateCameras.ensure()
        player.sendRichMessage("<green>Câmera de <white>$id</white> gravada na sua posição.")
    }

    @Subcommand("reload")
    fun reload(player: Player) {
        TemplateRegistry.load()
        TemplateCameras.ensure()
        player.sendRichMessage("<green>Templates recarregados: <white>${TemplateRegistry.all().size}</white>.")
    }
}
```

O `setpoint` grava a posição do olho do admin com yaw/pitch; o `TemplateCameras` desconta a altura ao posicionar a armor stand, então o enquadramento gravado é o que o jogador vai ver.

- [ ] **Passo 2: registrar o comando**

Em `OneBlockPlugin.setupCommands()`, junto dos outros `registerCommand`:

```kotlin
        registerCommand(one_block.system.islands.template.TemplateCommand())
```

Siga a forma exata que os comandos vizinhos usam no arquivo (`paperCommandManager.registerCommand(...)` ou o helper local).

- [ ] **Passo 3: verificar em jogo, fluxo completo**

1. `/istemplate list` lista os templates carregados.
2. Ir ao mundo `templates`, posicionar a câmera no enquadramento desejado, `/istemplate setpoint lush`. Reentrar no showcase: o enquadramento é o gravado.
3. Com conta sem ilha: entrar, SHIFT, escolher um template, **Selecionar**.
4. Esperado: tela escurece, ilha é criada, build aparece com o bioma correto, jogador chega em survival no spawn da ilha, HUD e action bar limpas.
5. Quebrar o bloco central e confirmar que a progressão de fase segue normal.
6. `/istemplate reload` depois de editar o YAML recarrega sem reiniciar.

---

## Auto-revisão

**Cobertura da spec:**

| Requisito da spec | Task |
|---|---|
| Mundo `templates` dedicado | 3 |
| Cinco câmeras compartilhadas, sem custo por jogador | 3 |
| Spectator preso na câmera, SHIFT abre menu | 5, 7 |
| Menu com Visualizar / Selecionar | 6 |
| `PlayerStopSpectatingEntityEvent` cancelado | 7 |
| HUD no topo (`HudApi`) e instrução no rodapé (`ActionBar`) | 5 |
| `Channel.TemplateSelect` acima de `SkillXp` | 5 |
| Guardas (dano, fome, drop, teleporte, comandos) | 7 |
| Sair sem escolher não grava nada | 7 |
| Fade preto, paste async, teleporte | 5, 4 |
| Bioma no grid inteiro | 4 |
| `templateId` nullable no `Island` | 4 |
| `.schem` + `templates.yml` | 2 |
| Comando admin de captura de ponto | 9 |
| Reset recola o mesmo template | 8 |
| Só quem não é dono nem membro entra no fluxo | 7 |
| FAWE `depend`, `lib:actionbar` `compileOnly` | 1 |
| Fallbacks de erro (mundo ausente, YAML vazio, paste falho, grid cheio) | 3, 5, 7, 8 |

**Pontos que exigem conferir a API contra o jar antes de improvisar** — o projeto já avisa que essas APIs mudam entre versões:

- `Biome` como enum ou registry (Task 2).
- `EditSession.setBiome(Region, BiomeType)` e `BukkitAdapter.adapt(Biome)` (Task 4).
- `RenderContext.slot(char, item)` da inventory-framework (Task 6).
- Rotação efetiva da câmera ao espectar uma armor stand: se o pitch não for respeitado, troque a entidade da Task 3 por uma que aceite pitch de corpo e recapture os pontos com `/istemplate setpoint`.
