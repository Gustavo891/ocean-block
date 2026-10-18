# Dialog UI — funcionalidades do Studio — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Fazer o engine de telas do Textura aceitar o formato do Studio — páginas e nav, ações no clique, dropdown e slider, placeholders (incluindo escolha e PlaceholderAPI), ícones por estado, e estado salvo no jogador — para montar telas completas só com JSON.

**Architecture:** O pipeline atual (parser → compilador → layout → render → custom click) é mantido. A `DialogSession` passa a guardar valores em texto e a página atual, com persistência em PDC para chaves de controle. O parser ganha folhas e passos de ação novos. O compilador ganha os controles novos e os placeholders. O opener passa a executar páginas e aberturas com valores. O `UiUsage` pré-gera a arte de todas as páginas e estados.

**Tech Stack:** Kotlin 2.3 / JDK 21, Paper API 1.21.11 (PDC, `PlayerCustomClickEvent`, Dialog), Adventure/MiniMessage (`Placeholder.unparsed`), Gson, PlaceholderAPI (`me.clip:placeholderapi`, `compileOnly`, softdepend).

**Spec:** `docs/superpowers/specs/2026-10-12-dialog-ui-studio-features-design.md`. Base do engine: `docs/superpowers/specs/2026-10-12-dialog-ui-json-design.md`.

## Global Constraints

- **Sem comentários no código** (CLAUDE.md).
- **Sem testes automatizados** (preferência do projeto). O gate de cada task é o build verde; o do conjunto é in-game (Task 5).
- **Sem commits.** O usuário commita.
- Strings visíveis ao jogador e logs em **português**.
- **Nada copiado do Dialog Engine (GPL-3.0).** Só o formato é seguido.
- Valores são sempre texto:
  - switch `"true"`/`"false"`;
  - dropdown e segmented: o id da opção, que é `rótulo.trim().lowercase().replace(' ', '_')`;
  - slider: o número.
- Ordem de leitura de um valor: bind do código → PDC (se a chave é `bind` de um controle da tela) → valor da sessão → default do JSON.
- PDC: `NamespacedKey(plugin textura, "ui_<chave>")`. Só aceita chaves `[a-z0-9_.-]+`.
- Comandos: todo valor substituído em `[console]`/`[player]` precisa casar `[A-Za-z0-9_.:-]*`; `{chave|raw}` libera. Se falhar, a linha e as seguintes não rodam.
- Chaves do Studio só de render são ignoradas **sem aviso**: `nudge`, `drawButtons`, `contentWidth`, `showWarning`, `look`, `compact`, `accent`, `face`, `frame`, `style`, `buttons`, `inputs`, `afterAction`, `command`, `commandPermission`, `perPage`, `scale`, `columns`.
- Build: `./gradlew :plugins:textura:localJar` na raiz `D:\one-block`. Deploy: `./gradlew :plugins:textura:localDeploy` com o servidor parado.
- Os escapes `\uXXXX` são convertidos pelas ferramentas de edição. Para caracteres fora do ASCII, use `Char(0x....)`.

## Review Focus

Cada linha vira item do checklist in-game da Task 5 (passo 5.6):

1. **Documento do Studio colado com chaves de render** (`look`, `nudge`, `accent`…): renderiza sem aviso de "chave desconhecida".
2. **Valor persistido:** muda o slider ou o dropdown, sai do servidor, volta e reabre: o valor continua. Com bind do código, o código vence o PDC.
3. **Comando inseguro:** um `[console]` com um valor de PlaceholderAPI que tem espaço ou `§` é recusado, o console avisa e as linhas seguintes não rodam.
4. **Página inexistente:** `onClick: {action: page, value: "nao_existe"}` avisa e a tela fica na página atual.
5. **Tela antiga:** `settings.json` continua igual. Os binds dela agora persistem no jogador (mudança intencional).

---

## Mapa de arquivos

Os caminhos são relativos a `plugins/textura/src/main/kotlin/one_block/textura/`.

| Arquivo | Ação | Responsabilidade |
|---|---|---|
| `dialog/DialogValues.kt` | criar | id de opção, validação de chave |
| `dialog/PlayerSettings.kt` | criar | ler e gravar settings no PDC |
| `dialog/DialogSession.kt` | reescrever | valores em texto, página, persistência, escolha por índice |
| `api/DialogBinds.kt` | reescrever | binds em texto, binds por índice, botões |
| `api/DialogClick.kt` | modificar | `open` com valores |
| `api/DialogApi.kt` | modificar | `open` com valores, `setting`/`setSetting` |
| `dialog/node/UiNode.kt` | modificar | `UiOutcome.Page`, `UiOutcome.Open` com valores |
| `dialog/text/Placeholders.kt` | criar | `{chave}`, escolha, `|raw`, `%papi%`, embutidos; saídas plain/rich/command |
| `dialog/text/Papi.kt` | criar | ponte opcional com PlaceholderAPI |
| `dialog/theme/UiTheme.kt` | modificar | `NavTheme`, `DropdownTheme`, `SliderTheme` |
| `registry/UiThemeRegistry.kt` | modificar | ler `nav`, `dropdown`, `slider` |
| `dialog/json/ScreenDefinition.kt` | modificar | `DefNav`, `DefDropdown`, `DefSlider`, `DefStep`, `DefClick`, páginas, opções da tela |
| `dialog/json/ScreenParser.kt` | modificar | raiz com `document`/`pages`, folhas e passos novos, chaves ignoradas |
| `dialog/json/ScreenCompiler.kt` | modificar | controles novos, placeholders, ações |
| `dialog/DialogActions.kt` | criar | executa os passos de um clique |
| `dialog/DialogOpener.kt` | modificar | settings, página, `Open` com valores, escape/pause |
| `dialog/kit/UiUsage.kt` | modificar | todas as páginas e todos os estados de ícone |
| `TexturaPlugin.kt` | modificar | `PlayerSettings` no opener |
| `plugins/textura/build.gradle.kts` | modificar | PlaceholderAPI `compileOnly` + softdepend |

---

### Task 1: Valores em texto, settings no PDC e API

**Files:**
- Create: `dialog/DialogValues.kt`, `dialog/PlayerSettings.kt`
- Rewrite: `dialog/DialogSession.kt`, `api/DialogBinds.kt`
- Modify: `api/DialogClick.kt`, `api/DialogApi.kt`, `dialog/node/UiNode.kt`, `dialog/DialogOpener.kt`, `dialog/json/ScreenDefinition.kt`, `dialog/json/ScreenParser.kt`, `dialog/json/ScreenCompiler.kt`, `TexturaPlugin.kt`

**Interfaces:**
- Produces:
  - `object DialogValues { fun optionId(label: String): String; fun validKey(key: String): Boolean }`
  - `class PlayerSettings(plugin: JavaPlugin) { fun get(player: Player, key: String): String?; fun set(player: Player, key: String, value: String) }`
  - `class DialogSession(screenId: String, binds: DialogBinds, initial: Map<String, String> = emptyMap(), player: Player? = null, settings: PlayerSettings? = null)` com:
    - `var page: String?`, `var persistent: Set<String>`;
    - `value(key): String?`, `set(key, value)`;
    - `choice(key, options, default): Int`, `choose(key, options, index)`;
    - `carried(): Map<String, String>`, `press(key, player): UiOutcome`;
    - `val player: Player?`.
  - `DialogBinds`: `switch`, `segmented`, `dropdown(key, get: () -> String, set)`, `slider(key, get: () -> Int, set)`, `value(key, get, set = {})`, `text(key, value)`, `button(key, onClick)`. Internos: `values`, `indexes`, `buttons`, `keys`.
  - `sealed interface UiOutcome { Refresh; Close; Page(id: String); Open(screenId: String, binds: DialogBinds, values: Map<String, String> = emptyMap()) }`
  - `DialogApi.open(player, screenId, values: Map<String, String> = emptyMap(), binds: DialogBinds.() -> Unit = {})`, `DialogApi.setting(player, key): String?`, `DialogApi.setSetting(player, key, value)`
  - `ScreenDefinition.controlBinds: Set<String>`

- [ ] **Step 1: `dialog/DialogValues.kt` e `dialog/PlayerSettings.kt`**

```kotlin
package one_block.textura.dialog

object DialogValues {

    private val KEY = Regex("[a-z0-9_.-]+")

    fun optionId(label: String): String = label.trim().lowercase().replace(' ', '_')

    fun validKey(key: String): Boolean = KEY.matches(key)
}
```

```kotlin
package one_block.textura.dialog

import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.persistence.PersistentDataType
import org.bukkit.plugin.java.JavaPlugin

class PlayerSettings(private val plugin: JavaPlugin) {

    fun get(player: Player, key: String): String? =
        namespaced(key)?.let { player.persistentDataContainer.get(it, PersistentDataType.STRING) }

    fun set(player: Player, key: String, value: String) {
        val namespaced = namespaced(key) ?: run {
            plugin.logger.warning("[ui] Chave de setting inválida '$key': use [a-z0-9_.-]")
            return
        }
        player.persistentDataContainer.set(namespaced, PersistentDataType.STRING, value)
    }

    private fun namespaced(key: String): NamespacedKey? =
        if (DialogValues.validKey(key)) NamespacedKey(plugin, "ui_$key") else null
}
```

- [ ] **Step 2: Reescrever `api/DialogBinds.kt`**

```kotlin
package one_block.textura.api

class DialogBinds {

    internal class ValueBind(val get: () -> String, val set: (String) -> Unit)

    internal class IndexBind(val get: () -> Int, val set: (Int) -> Unit)

    internal val values = mutableMapOf<String, ValueBind>()
    internal val indexes = mutableMapOf<String, IndexBind>()
    internal val buttons = mutableMapOf<String, (DialogClick) -> Unit>()

    internal val keys: Set<String> get() = values.keys + indexes.keys + buttons.keys

    fun switch(key: String, get: () -> Boolean, set: (Boolean) -> Unit) {
        values[key] = ValueBind({ get().toString() }, { set(it == "true") })
    }

    fun segmented(key: String, get: () -> Int, set: (Int) -> Unit) {
        indexes[key] = IndexBind(get, set)
    }

    fun dropdown(key: String, get: () -> String, set: (String) -> Unit) {
        values[key] = ValueBind(get, set)
    }

    fun slider(key: String, get: () -> Int, set: (Int) -> Unit) {
        values[key] = ValueBind({ get().toString() }, { raw -> raw.toIntOrNull()?.let(set) })
    }

    fun value(key: String, get: () -> String, set: (String) -> Unit = {}) {
        values[key] = ValueBind(get, set)
    }

    fun text(key: String, value: () -> String) {
        values[key] = ValueBind(value) {}
    }

    fun button(key: String, onClick: (DialogClick) -> Unit) {
        buttons[key] = onClick
    }
}
```

- [ ] **Step 3: `UiOutcome` (em `dialog/node/UiNode.kt`) e `api/DialogClick.kt`**

Troque o bloco `sealed interface UiOutcome { ... }` por:

```kotlin
sealed interface UiOutcome {
    data object Refresh : UiOutcome
    data object Close : UiOutcome
    data class Page(val id: String) : UiOutcome
    class Open(val screenId: String, val binds: DialogBinds, val values: Map<String, String> = emptyMap()) : UiOutcome
}
```

Em `api/DialogClick.kt`, troque `open` por:

```kotlin
    fun open(screenId: String, values: Map<String, String> = emptyMap(), binds: DialogBinds.() -> Unit = {}) {
        outcome = UiOutcome.Open(screenId, DialogBinds().apply(binds), values)
    }

    fun page(id: String) {
        outcome = UiOutcome.Page(id)
    }
```

- [ ] **Step 4: Reescrever `dialog/DialogSession.kt`**

```kotlin
package one_block.textura.dialog

import one_block.textura.api.DialogBinds
import one_block.textura.api.DialogClick
import one_block.textura.dialog.node.UiOutcome
import org.bukkit.entity.Player

class DialogSession(
    val screenId: String,
    internal val binds: DialogBinds,
    initial: Map<String, String> = emptyMap(),
    val player: Player? = null,
    private val settings: PlayerSettings? = null,
) {

    var page: String? = null
    var persistent: Set<String> = emptySet()

    private val values = initial.toMutableMap()

    fun value(key: String): String? {
        binds.values[key]?.let { return it.get() }
        if (key in persistent && player != null) settings?.get(player, key)?.let { return it }
        return values[key]
    }

    fun set(key: String, value: String) {
        val bind = binds.values[key]
        when {
            bind != null -> bind.set(value)
            key in persistent && player != null && settings != null -> settings.set(player, key, value)
            else -> values[key] = value
        }
    }

    fun choice(key: String, options: List<String>, default: Int): Int {
        binds.indexes[key]?.let { return it.get().coerceIn(0, options.lastIndex) }
        val current = value(key) ?: return default
        return options.indexOfFirst { DialogValues.optionId(it) == current }.takeIf { it >= 0 } ?: default
    }

    fun choose(key: String, options: List<String>, index: Int) {
        val bind = binds.indexes[key]
        if (bind != null) bind.set(index) else set(key, DialogValues.optionId(options[index]))
    }

    fun carried(): Map<String, String> = values.toMap()

    fun press(key: String, player: Player): UiOutcome {
        val handler = binds.buttons[key] ?: return UiOutcome.Refresh
        val click = DialogClick(player)
        handler(click)
        return click.outcome
    }
}
```

- [ ] **Step 5: `controlBinds` em `ScreenDefinition` e no parser**

Em `dialog/json/ScreenDefinition.kt`, adicione `val controlBinds: Set<String>` como último campo de `ScreenDefinition`.

Em `ScreenParser.parse`, troque o `return ScreenDefinition(...)` por:

```kotlin
        return ScreenDefinition(id, theme, obj.string("title"), close, root, keysOf(root, obj.string("title")), controlBindsOf(root))
```

e adicione na classe externa:

```kotlin
    private fun controlBindsOf(root: DefContainer): Set<String> {
        val binds = mutableSetOf<String>()
        fun walk(node: DefNode) {
            when (node) {
                is DefContainer -> node.children.forEach(::walk)
                is DefSwitch -> node.bind?.let(binds::add)
                is DefSegmented -> node.bind?.let(binds::add)
                else -> Unit
            }
        }
        walk(root)
        return binds
    }
```

Os avisos da Task 4 substituem esta função pela versão com todas as páginas.

- [ ] **Step 6: Compilador: switch e segmented na API nova**

Em `ScreenCompiler.kt`, no `switch(def)`, troque as duas primeiras linhas de leitura e a ação por:

```kotlin
            val key = def.bind ?: def.path
            val value = session.value(key)?.let { it == "true" } ?: def.value
            val label = if (value) theme.switch.onLabel else theme.switch.offLabel
            val action = UiAction {
                val now = session.value(key)?.let { it == "true" } ?: def.value
                session.set(key, (!now).toString())
                UiOutcome.Refresh
            }
```

No `segmented(def)`, troque `val selected = session.segment(key, def.selected).coerceIn(0, def.options.lastIndex)` por:

```kotlin
            val selected = session.choice(key, def.options, def.selected.coerceIn(0, def.options.lastIndex))
```

e o corpo da ação de cada segmento por `session.choose(key, def.options, index)` seguido de `UiOutcome.Refresh`.

- [ ] **Step 7: Opener, API e plugin**

Em `dialog/DialogOpener.kt`:
1. Adicione `private val settings: PlayerSettings` como último parâmetro do construtor.
2. Troque `open` por:

```kotlin
    fun open(player: Player, screenId: String, values: Map<String, String>, binds: DialogBinds): Boolean {
        if (!Bukkit.isPrimaryThread()) {
            Bukkit.getScheduler().runTask(plugin, Runnable { if (player.isOnline) open(player, screenId, values, binds) })
            return true
        }
        return show(player, DialogSession(screenId, binds, values, player, settings))
    }

    fun setting(player: Player, key: String): String? = settings.get(player, key)

    fun setSetting(player: Player, key: String, value: String) {
        settings.set(player, key, value)
    }
```

3. Em `show`, logo depois de resolver `theme`, adicione `session.persistent = definition.controlBinds`.
4. Troque o `when (outcome)` de `run` por:

```kotlin
        when (outcome) {
            UiOutcome.Refresh -> show(player, session)
            UiOutcome.Close -> close(player)
            is UiOutcome.Page -> {
                session.page = outcome.id
                show(player, session)
            }
            is UiOutcome.Open -> show(player, DialogSession(outcome.screenId, outcome.binds, session.carried() + outcome.values, player, settings))
        }
```

5. Troque `checkBinds` por:

```kotlin
    private fun checkBinds(definition: ScreenDefinition, session: DialogSession, warn: (String) -> Unit) {
        (session.binds.keys - definition.keys).forEach { warn("bind '$it' fornecido pelo código não existe na tela") }
    }
```

Em `api/DialogApi.kt`, troque `open` e adicione os settings:

```kotlin
    fun open(
        player: Player,
        screenId: String,
        values: Map<String, String> = emptyMap(),
        binds: DialogBinds.() -> Unit = {},
    ): Boolean = opener?.open(player, screenId, values, DialogBinds().apply(binds)) ?: false

    fun setting(player: Player, key: String): String? = opener?.setting(player, key)

    fun setSetting(player: Player, key: String, value: String) {
        opener?.setSetting(player, key, value)
    }
```

Em `TexturaPlugin.kt`, na criação do opener, passe `PlayerSettings(this)` como último argumento:

```kotlin
        dialogOpener = DialogOpener(this, { uiKit }, uiScreenRegistry, uiThemeRegistry, packStatus, ::applyPack, PlayerSettings(this))
```

e importe `one_block.textura.dialog.PlayerSettings`.

- [ ] **Step 8: Build**

Run: `./gradlew :plugins:textura:localJar`
Expected: `BUILD SUCCESSFUL`

---

### Task 2: Placeholders (`{chave}`, escolha, `|raw`, `%papi%`)

**Files:**
- Create: `dialog/text/Placeholders.kt`, `dialog/text/Papi.kt`
- Modify: `dialog/json/ScreenCompiler.kt`, `dialog/json/ScreenParser.kt` (regex de chaves), `plugins/textura/build.gradle.kts`

**Interfaces:**
- Consumes: `DialogSession.value`, `DialogSession.player`, `DialogSession.page` (Task 1)
- Produces:
  - `class Placeholders(session: DialogSession, extra: Map<String, String> = emptyMap())` com:
    - `fun plain(text: String): String`;
    - `fun rich(text: String): Component`;
    - `fun command(text: String, onUnsafe: (String) -> Unit): String?`.
  - `companion object { val TOKEN: Regex; const val VALUE_KEY = "value" }`
  - `object Papi { fun resolve(player: Player?, identifier: String): String? }`

- [ ] **Step 1: PlaceholderAPI no build**

Em `plugins/textura/build.gradle.kts`, dentro de `dependencies`, adicione `compileOnly(libs.placeholder)`. No bloco `bukkit { }`, adicione `softDepend = listOf("PlaceholderAPI")`.

- [ ] **Step 2: `dialog/text/Papi.kt`**

```kotlin
package one_block.textura.dialog.text

import me.clip.placeholderapi.PlaceholderAPI
import org.bukkit.Bukkit
import org.bukkit.entity.Player

object Papi {

    private const val PLUGIN = "PlaceholderAPI"

    fun resolve(player: Player?, identifier: String): String? {
        if (player == null || !Bukkit.getPluginManager().isPluginEnabled(PLUGIN)) return null
        val token = "%$identifier%"
        return PlaceholderAPI.setPlaceholders(player, token).takeIf { it != token }
    }
}
```

- [ ] **Step 3: `dialog/text/Placeholders.kt`**

```kotlin
package one_block.textura.dialog.text

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import one_block.textura.api.TexturaTags
import one_block.textura.dialog.DialogSession
import org.bukkit.Bukkit

class Placeholders(private val session: DialogSession, private val extra: Map<String, String> = emptyMap()) {

    private class Piece(val key: String, val rendered: String, val untrusted: String?)

    fun plain(text: String): String =
        expand(text) { it.rendered.replace('\n', ' ').replace('\r', ' ') }

    fun rich(text: String): Component {
        val resolvers = mutableListOf<TagResolver>()
        val converted = expand(text) { piece ->
            val tag = "bind_${resolvers.size}"
            resolvers += Placeholder.unparsed(tag, piece.rendered)
            "<$tag>"
        }
        return TexturaTags.miniMessage.deserialize(converted, TagResolver.resolver(resolvers))
    }

    fun command(text: String, onUnsafe: (String) -> Unit): String? {
        var refused: String? = null
        val result = expand(text) { piece ->
            if (refused == null && piece.untrusted != null && !SAFE.matches(piece.untrusted)) refused = piece.key
            piece.rendered
        }
        refused?.let {
            onUnsafe(it)
            return null
        }
        return result
    }

    private fun expand(text: String, render: (Piece) -> String): String =
        TOKEN.replace(text) { match ->
            val papi = match.groupValues[3]
            if (papi.isNotEmpty()) {
                val value = Papi.resolve(session.player, papi) ?: return@replace match.value
                return@replace render(Piece(papi, value, value))
            }
            val key = match.groupValues[1]
            val modifier = match.groupValues[2]
            val value = resolve(key)
            when {
                modifier.startsWith("?") -> {
                    val body = modifier.substring(1)
                    val chosen = choose(body, value).replace(VALUE_MARK, value.orEmpty())
                    render(Piece(key, chosen, if (body.contains(VALUE_MARK)) value else null))
                }
                value == null -> match.value
                modifier == RAW -> render(Piece(key, value, null))
                else -> render(Piece(key, value, value))
            }
        }

    private fun choose(body: String, value: String?): String {
        val entries = body.split('|')
        entries.forEach { entry ->
            val colon = entry.indexOf(':')
            if (colon >= 0 && entry.substring(0, colon) == value) return entry.substring(colon + 1)
        }
        return entries.lastOrNull { !it.contains(':') }.orEmpty()
    }

    private fun resolve(key: String): String? = extra[key] ?: builtin(key) ?: session.value(key)

    private fun builtin(key: String): String? = when (key) {
        "player" -> session.player?.name.orEmpty()
        "player_uuid" -> session.player?.uniqueId?.toString().orEmpty()
        "online" -> Bukkit.getOnlinePlayers().size.toString()
        "page" -> session.page.orEmpty()
        else -> null
    }

    companion object {
        const val VALUE_KEY = "value"
        private const val RAW = "|raw"
        private const val VALUE_MARK = "\$value"
        private val SAFE = Regex("[A-Za-z0-9_.:-]*")
        val TOKEN = Regex("""\{([A-Za-z0-9_.-]+)(\?[^}]*|\|raw)?}|%([A-Za-z0-9_.:-]+)%""")
    }
}
```

- [ ] **Step 4: Compilador usa `Placeholders`**

Em `ScreenCompiler.kt`:
1. Apague as funções `plain(...)` e `rich(...)` e o campo `mini`.
2. Troque `title` por:

```kotlin
    fun title(definition: ScreenDefinition, session: DialogSession): Component? =
        definition.title?.let { Placeholders(session).rich(it) }
```

3. Dentro de `Context`, adicione o campo `private val placeholders = Placeholders(session)` e:
   - troque `plain(def.text, session)` por `placeholders.plain(def.text)`;
   - troque `plain(def.label, session)` por `placeholders.plain(def.label)`.
4. Troque `tooltip` por:

```kotlin
        private fun tooltip(text: String, value: String?): Component {
            val extra = if (value == null) emptyMap() else mapOf(Placeholders.VALUE_KEY to value)
            return Placeholders(session, extra).rich(text).colorIfAbsent(theme.style(UiTheme.TOOLTIP_STYLE).color)
        }
```

5. Remova `VALUE_KEY` do `companion object` do compilador e troque as referências (`ScreenCompiler.VALUE_KEY` em `ScreenParser.keysOf`) por `Placeholders.VALUE_KEY`.
6. Remova os imports que ficaram sem uso (`MiniMessage`, `Placeholder`, `TagResolver`, `TexturaTags`) e importe `one_block.textura.dialog.text.Placeholders`.

Em `ScreenParser.kt`, troque `val PLACEHOLDER = Regex("""\{([a-z0-9_-]+)}""")` por:

```kotlin
        val PLACEHOLDER = Regex("""\{([A-Za-z0-9_.-]+)""")
```

- [ ] **Step 5: Build**

Run: `./gradlew :plugins:textura:localJar`
Expected: `BUILD SUCCESSFUL`

---

### Task 3: Tema (nav, dropdown, slider) e arte placeholder

**Files:**
- Modify: `dialog/theme/UiTheme.kt`, `registry/UiThemeRegistry.kt`
- Create (assets): `server/plugins/Textura/resource/assets/oneblock/textures/ui/stone/{nav,nav_active,dropdown,arrow_down,slider_track,slider_fill,well}.png` + `.slice`
- Modify: `server/plugins/Textura/configs/ui/stone.yml`

**Interfaces:**
- Produces:
  - `data class NavTheme(art, activeArt, textStyle, activeTextStyle)`, `DropdownTheme(art, arrow, textStyle)`, `SliderTheme(track, fill, textStyle)`
  - `UiTheme.nav`, `UiTheme.dropdown`, `UiTheme.slider`; `controlArt()` inclui toda essa arte

- [ ] **Step 1: `UiTheme.kt`**

Adicione depois de `ButtonTheme`:

```kotlin
data class NavTheme(val art: String, val activeArt: String, val textStyle: String, val activeTextStyle: String)

data class DropdownTheme(val art: String, val arrow: String, val textStyle: String)

data class SliderTheme(val track: String, val fill: String, val textStyle: String)
```

No construtor de `UiTheme`, depois de `val button: ButtonTheme,`, adicione:

```kotlin
    val nav: NavTheme,
    val dropdown: DropdownTheme,
    val slider: SliderTheme,
```

e troque `controlArt()` por:

```kotlin
    fun controlArt(): Set<String> = setOf(
        switch.on, switch.off, segmented.art, segmented.selectedArt, button.art,
        nav.art, nav.activeArt, dropdown.art, dropdown.arrow, slider.track, slider.fill,
    )
```

- [ ] **Step 2: `UiThemeRegistry.kt`**

Em `read`, depois do bloco `button = ButtonTheme(...)`, adicione:

```kotlin
        nav = NavTheme(
            art = section.getString("nav.art") ?: "ui:$id/nav",
            activeArt = section.getString("nav.active-art") ?: "ui:$id/nav_active",
            textStyle = section.getString("nav.text-style") ?: UiTheme.DEFAULT_STYLE,
            activeTextStyle = section.getString("nav.active-text-style") ?: "heading",
        ),
        dropdown = DropdownTheme(
            art = section.getString("dropdown.art") ?: "ui:$id/dropdown",
            arrow = section.getString("dropdown.arrow") ?: "ui:$id/arrow_down",
            textStyle = section.getString("dropdown.text-style") ?: UiTheme.DEFAULT_STYLE,
        ),
        slider = SliderTheme(
            track = section.getString("slider.track") ?: "ui:$id/slider_track",
            fill = section.getString("slider.fill") ?: "ui:$id/slider_fill",
            textStyle = section.getString("slider.text-style") ?: UiTheme.DEFAULT_STYLE,
        ),
```

e importe `NavTheme`, `DropdownTheme`, `SliderTheme`.

- [ ] **Step 3: Arte placeholder**

Salve no scratchpad como `make-ui-art-2.ps1` e rode com a ferramenta PowerShell:

```powershell
Add-Type -AssemblyName System.Drawing
$root = 'D:\one-block\server\plugins\Textura\resource\assets\oneblock\textures\ui\stone'
New-Item -ItemType Directory -Force $root | Out-Null

function Color($rgb) { [System.Drawing.Color]::FromArgb(255, $rgb[0], $rgb[1], $rgb[2]) }

function Save($bmp, $name, $slice) {
  $bmp.Save("$root\$name.png", [System.Drawing.Imaging.ImageFormat]::Png)
  $bmp.Dispose()
  if ($slice) { [System.IO.File]::WriteAllText("$root\$name.png.slice", $slice) }
}

function Box($name, $w, $h, $fill, $border, $slice) {
  $bmp = New-Object System.Drawing.Bitmap $w, $h, ([System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
  $g = [System.Drawing.Graphics]::FromImage($bmp)
  $g.Clear((Color $fill))
  $pen = New-Object System.Drawing.Pen (Color $border), 1
  $g.DrawRectangle($pen, 0, 0, $w - 1, $h - 1)
  $pen.Dispose(); $g.Dispose()
  Save $bmp $name $slice
}

function Arrow($name) {
  $bmp = New-Object System.Drawing.Bitmap 7, 4, ([System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
  $c = Color @(220,220,220)
  for ($y = 0; $y -lt 4; $y++) { for ($x = $y; $x -lt (7 - $y); $x++) { $bmp.SetPixel($x, $y, $c) } }
  Save $bmp $name $null
}

Box 'nav' 40 14 @(49,50,56) @(49,50,56) '2 2'
Box 'nav_active' 40 14 @(70,72,80) @(214,150,52) '2 2'
Box 'dropdown' 40 12 @(62,63,70) @(28,28,32) '2 2'
Box 'slider_track' 24 10 @(40,41,46) @(22,22,26) '2 2'
Box 'slider_fill' 24 10 @(214,150,52) @(120,80,20) '2 2'
Box 'well' 16 16 @(38,39,44) @(22,22,26) '3 3 3 3'
Arrow 'arrow_down'
```

Confira que existem os 7 PNGs novos e 6 `.slice` novos em `...\textures\ui\stone\`.

- [ ] **Step 4: `stone.yml`**

Acrescente ao tema `stone` em `server/plugins/Textura/configs/ui/stone.yml`, mantendo o que já existe:
- em `surfaces:`, a linha `well: ui:stone/well`;
- os blocos abaixo, no mesmo nível de `button:`;
- em `icons:`, os aliases `dot-green: online_status`, `dot-gold: hourglass`, `dot-red: offline_status`, `music: star`, `user: crown`.

```yaml
  nav:
    art: ui:stone/nav
    active-art: ui:stone/nav_active
    text-style: body
    active-text-style: heading
  dropdown:
    art: ui:stone/dropdown
    arrow: ui:stone/arrow_down
    text-style: body
  slider:
    track: ui:stone/slider_track
    fill: ui:stone/slider_fill
    text-style: body
```

- [ ] **Step 5: Build**

Run: `./gradlew :plugins:textura:localJar`
Expected: `BUILD SUCCESSFUL`

---

### Task 4: Páginas, nav, dropdown, slider, ícone por estado e ações

**Files:**
- Modify: `dialog/json/ScreenDefinition.kt`, `dialog/json/ScreenParser.kt`, `dialog/json/ScreenCompiler.kt`, `dialog/DialogOpener.kt`
- Create: `dialog/DialogActions.kt`

**Interfaces:**
- Consumes: Tasks 1–3
- Produces:
  - Definições:
    - `DefNav(label, icon, w, click: DefClick?, path)`;
    - `DefDropdown(initial, options, bind, w, tooltip, path)`;
    - `DefSlider(value, min, max, step, unit, bind, w, tooltip, path)`;
    - `DefIcon(..., bind: String? = null, states: Map<String, String> = emptyMap())`;
    - `DefSpace(px, path, grow: Boolean = false)`;
    - `DefButton(..., click: DefClick? = null)`.
  - `sealed interface DefStep { Page(id); Close; Refresh; Open(id, args); Set(pairs); Message(text); Command(console, line); Sound(id, volume, pitch) }`, `data class DefClick(steps: List<DefStep>)`
  - `ScreenDefinition(id, theme, title, close, root, keys, controlBinds, pages: Map<String, DefContainer>, firstPage: String?, escape: Boolean, pause: Boolean)` + `fun page(id: String?): DefContainer` + `fun containers(): List<DefContainer>`
  - `object DialogActions { fun run(steps: List<DefStep>, player: Player, session: DialogSession, warn: (String) -> Unit): UiOutcome }`
  - `ScreenCompiler.resolveIcon(theme: UiTheme, id: String): String` (público, usado pelo `UiUsage`)

- [ ] **Step 1: `ScreenDefinition.kt`**

Troque `DefSpace`, `DefIcon`, `DefButton` e `ScreenDefinition` por estas versões, e adicione os tipos novos:

```kotlin
data class DefSpace(val px: Int, override val path: String, val grow: Boolean = false) : DefNode

data class DefIcon(
    val id: String,
    val size: Int,
    val position: UiPosition?,
    override val path: String,
    val bind: String? = null,
    val states: Map<String, String> = emptyMap(),
) : DefNode

data class DefButton(
    val label: String,
    val bind: String?,
    val w: UiSize,
    val icon: String?,
    val tooltip: String?,
    override val path: String,
    val click: DefClick? = null,
) : DefNode

data class DefNav(
    val label: String,
    val icon: String?,
    val w: UiSize,
    val click: DefClick?,
    override val path: String,
) : DefNode

data class DefDropdown(
    val initial: String,
    val options: List<String>,
    val bind: String?,
    val w: UiSize,
    val tooltip: String?,
    override val path: String,
) : DefNode

data class DefSlider(
    val value: Int,
    val min: Int,
    val max: Int,
    val step: Int,
    val unit: String,
    val bind: String?,
    val w: UiSize,
    val tooltip: String?,
    override val path: String,
) : DefNode

sealed interface DefStep {
    data class Page(val id: String) : DefStep
    data object Close : DefStep
    data object Refresh : DefStep
    data class Open(val id: String, val args: Map<String, String>) : DefStep
    data class Set(val pairs: Map<String, String>) : DefStep
    data class Message(val text: String) : DefStep
    data class Command(val console: Boolean, val line: String) : DefStep
    data class Sound(val id: String, val volume: Float, val pitch: Float) : DefStep
}

data class DefClick(val steps: List<DefStep>)

data class ScreenDefinition(
    val id: String,
    val theme: String,
    val title: String?,
    val close: String?,
    val root: DefContainer,
    val keys: Set<String>,
    val controlBinds: Set<String>,
    val pages: Map<String, DefContainer> = emptyMap(),
    val firstPage: String? = null,
    val escape: Boolean = true,
    val pause: Boolean = false,
) {
    fun page(id: String?): DefContainer = id?.let { pages[it] } ?: root

    fun containers(): List<DefContainer> = (listOf(root) + pages.values).distinct()
}
```

- [ ] **Step 2: Parser: raiz com `document`/`pages` e opções da tela**

Em `ScreenParser.kt`, troque o corpo de `parse` a partir de `val theme = ...` (inclusive) até o `return` por:

```kotlin
        val document = obj.get("document")?.takeIf { it.isJsonObject }?.asJsonObject
        val rootObj = document ?: obj
        val theme = obj.string("theme") ?: document?.string("theme") ?: run {
            logger.warning("[ui:$id] falta 'theme' no raiz ou no document")
            return null
        }
        val reader = Reader(id)
        if (document != null) reader.wrapper(obj)
        val root = reader.node(rootObj, "", 0, root = true) as? DefContainer ?: run {
            logger.warning("[ui:$id] o nó raiz precisa ser row, column, box ou grid")
            return null
        }

        val pages = linkedMapOf<String, DefContainer>()
        var firstPage: String? = null
        obj.get("pages")?.takeIf { it.isJsonArray }?.asJsonArray?.forEachIndexed { index, element ->
            val page = element.takeIf { it.isJsonObject }?.asJsonObject ?: return@forEachIndexed
            val pageId = page.string("id") ?: run {
                logger.warning("[ui:$id] página #$index sem 'id', ignorada")
                return@forEachIndexed
            }
            val pageDocument = page.get("document")?.takeIf { it.isJsonObject }?.asJsonObject
            if (pageDocument == null && firstPage == null) firstPage = pageId
            pages[pageId] = pageDocument?.let { reader.node(it, pageId, index, root = true) as? DefContainer } ?: root
        }
        if (firstPage == null) firstPage = pages.keys.firstOrNull()

        val title = obj.string("title")
        val hideFooter = obj.get("hideFooter")?.let { runCatching { it.asBoolean }.getOrNull() } == true
        val close = when {
            hideFooter -> null
            obj.get("close")?.isJsonNull == true -> null
            else -> obj.string("close") ?: DEFAULT_CLOSE
        }
        val containers = (listOf(root) + pages.values).distinct()

        return ScreenDefinition(
            id = id,
            theme = theme,
            title = title,
            close = close,
            root = root,
            keys = keysOf(containers, title),
            controlBinds = controlBindsOf(containers),
            pages = pages,
            firstPage = firstPage,
            escape = obj.get("canCloseWithEscape")?.let { runCatching { it.asBoolean }.getOrNull() } ?: true,
            pause = obj.get("pause")?.let { runCatching { it.asBoolean }.getOrNull() } ?: false,
        )
```

- [ ] **Step 3: Parser: chaves e binds de todas as páginas**

Troque `keysOf` e `controlBindsOf` por:

```kotlin
    private fun keysOf(containers: List<DefContainer>, title: String?): Set<String> {
        val keys = mutableSetOf<String>()
        fun placeholders(text: String?) {
            if (text != null) PLACEHOLDER.findAll(text).forEach { keys += it.groupValues[1] }
        }
        fun steps(click: DefClick?) {
            click?.steps?.forEach { step ->
                when (step) {
                    is DefStep.Message -> placeholders(step.text)
                    is DefStep.Command -> placeholders(step.line)
                    is DefStep.Set -> step.pairs.values.forEach(::placeholders)
                    is DefStep.Open -> step.args.values.forEach(::placeholders)
                    else -> Unit
                }
            }
        }
        fun walk(node: DefNode) {
            when (node) {
                is DefContainer -> node.children.forEach(::walk)
                is DefText -> {
                    placeholders(node.text)
                    placeholders(node.tooltip)
                }
                is DefSwitch -> {
                    node.bind?.let(keys::add)
                    placeholders(node.tooltip)
                }
                is DefSegmented -> {
                    node.bind?.let(keys::add)
                    placeholders(node.tooltip)
                }
                is DefDropdown -> {
                    node.bind?.let(keys::add)
                    placeholders(node.tooltip)
                }
                is DefSlider -> {
                    node.bind?.let(keys::add)
                    placeholders(node.tooltip)
                }
                is DefButton -> {
                    node.bind?.let(keys::add)
                    placeholders(node.label)
                    placeholders(node.tooltip)
                    steps(node.click)
                }
                is DefNav -> {
                    placeholders(node.label)
                    steps(node.click)
                }
                is DefIcon -> node.bind?.let(keys::add)
                is DefSpace, is DefImage -> Unit
            }
        }
        containers.forEach(::walk)
        placeholders(title)
        return keys - Placeholders.VALUE_KEY
    }

    private fun controlBindsOf(containers: List<DefContainer>): Set<String> {
        val binds = mutableSetOf<String>()
        fun walk(node: DefNode) {
            when (node) {
                is DefContainer -> node.children.forEach(::walk)
                is DefSwitch -> node.bind?.let(binds::add)
                is DefSegmented -> node.bind?.let(binds::add)
                is DefDropdown -> node.bind?.let(binds::add)
                is DefSlider -> node.bind?.let(binds::add)
                is DefIcon -> node.bind?.let(binds::add)
                else -> Unit
            }
        }
        containers.forEach(::walk)
        return binds
    }
```

Importe `one_block.textura.dialog.text.Placeholders`.

- [ ] **Step 4: Parser: folhas novas, `onClick` e chaves ignoradas**

No `Reader.node`, troque o `when (shape)` por:

```kotlin
            val node = when (shape) {
                "space" -> space(obj, path)
                "text" -> text(obj, path)
                "icon" -> icon(obj, path)
                "image" -> DefImage(obj.string("image").orEmpty(), size(obj, "w", path), position(obj, path), path)
                "switch" -> DefSwitch(obj.bool("switch", path), obj.string("bind"), obj.string("tooltip"), path)
                "segmented" -> segmented(obj, path)
                "button" -> DefButton(
                    singleLine(obj.string("button").orEmpty(), path), obj.string("bind"), size(obj, "w", path),
                    obj.string("icon"), obj.string("tooltip"), path, click(obj, path),
                )
                "nav" -> DefNav(singleLine(obj.string("nav").orEmpty(), path), obj.string("icon"), size(obj, "w", path), click(obj, path), path)
                "dropdown" -> dropdown(obj, path)
                "slider" -> slider(obj, path)
                else -> container(obj, CONTAINERS.getValue(shape), shape, path)
            }

            val allowed = KNOWN.getValue(shape) + COMMON + IGNORED + if (root) ROOT else emptySet()
```

(A linha `obj.keySet().filterNot { it in allowed }...` que vem depois continua igual.)

Adicione dentro de `Reader`:

```kotlin
        fun wrapper(obj: JsonObject) {
            obj.keySet().filterNot { it in WRAPPER || it in IGNORED }.forEach { warn("raiz", "chave desconhecida '$it'") }
        }

        private fun space(obj: JsonObject, path: String): DefSpace {
            val raw = obj.get("space")
            if (raw?.isJsonPrimitive == true && raw.asJsonPrimitive.isString && raw.asString == "grow") return DefSpace(0, path, grow = true)
            return DefSpace(obj.int("space", 0, path).coerceAtLeast(0), path)
        }

        private fun icon(obj: JsonObject, path: String): DefIcon {
            val states = obj.get("states")?.takeIf { it.isJsonObject }?.asJsonObject?.entrySet()
                ?.mapNotNull { (state, icon) -> runCatching { icon.asString }.getOrNull()?.let { state to it } }
                ?.toMap().orEmpty()
            return DefIcon(obj.string("icon").orEmpty(), iconSize(obj, path), position(obj, path), path, obj.string("bind"), states)
        }

        private fun dropdown(obj: JsonObject, path: String): DefDropdown {
            val options = obj.array("options", path).mapNotNull { runCatching { it.asString }.getOrNull() }
            if (options.isEmpty()) warn(path, "dropdown sem opções")
            return DefDropdown(obj.string("dropdown").orEmpty(), options, obj.string("bind"), size(obj, "w", path), obj.string("tooltip"), path)
        }

        private fun slider(obj: JsonObject, path: String): DefSlider {
            val min = obj.int("min", 0, path)
            val max = obj.int("max", 100, path)
            val step = obj.int("step", 1, path)
            if (max <= min || step <= 0) warn(path, "slider com min/max/step inválidos")
            return DefSlider(
                obj.int("slider", min, path), min, max, step, obj.string("unit").orEmpty(),
                obj.string("bind"), size(obj, "w", path), obj.string("tooltip"), path,
            )
        }

        private fun click(obj: JsonObject, path: String): DefClick? {
            val click = obj.get("onClick")?.takeIf { it.isJsonObject }?.asJsonObject ?: return null
            val value = click.string("value").orEmpty()
            val steps = when (val action = click.string("action")) {
                "page" -> listOf(DefStep.Page(value))
                "close" -> listOf(DefStep.Close)
                "run_command" -> value.lines().mapNotNull { step(it.trim(), path) }
                else -> {
                    warn(path, "onClick.action '$action' desconhecida")
                    return null
                }
            }
            return DefClick(steps)
        }

        private fun step(line: String, path: String): DefStep? {
            if (line.isEmpty()) return null
            val tagged = TAG.matchEntire(line) ?: return DefStep.Command(console = false, line = line.removePrefix("/"))
            val name = tagged.groupValues[1]
            val rest = tagged.groupValues[2].trim()
            val words = rest.split(' ').filter { it.isNotBlank() }
            return when (name) {
                "open" -> DefStep.Open(words.firstOrNull().orEmpty(), args(words.drop(1)))
                "page" -> DefStep.Page(rest)
                "close" -> DefStep.Close
                "refresh" -> DefStep.Refresh
                "set" -> DefStep.Set(args(words))
                "message" -> DefStep.Message(rest)
                "console" -> DefStep.Command(console = true, line = rest.removePrefix("/"))
                "player" -> DefStep.Command(console = false, line = rest.removePrefix("/"))
                "sound" -> DefStep.Sound(
                    words.getOrElse(0) { "" },
                    words.getOrNull(1)?.toFloatOrNull() ?: 1f,
                    words.getOrNull(2)?.toFloatOrNull() ?: 1f,
                )
                else -> {
                    warn(path, "ação '[$name]' desconhecida, ignorada")
                    null
                }
            }
        }

        private fun args(words: List<String>): Map<String, String> = words.mapNotNull { word ->
            val parts = word.split('=', limit = 2)
            if (parts.size == 2 && parts[0].isNotBlank()) parts[0] to parts[1] else null
        }.toMap()
```

No `companion object`, troque `LEAVES`, `ROOT` e `KNOWN` e adicione `WRAPPER`, `IGNORED` e `TAG`:

```kotlin
        private val LEAVES = listOf("text", "button", "nav", "dropdown", "slider", "segmented", "switch", "image", "icon", "space")
        private val ROOT = setOf("theme", "title", "close")
        private val WRAPPER = setOf("title", "theme", "document", "pages", "hideFooter", "canCloseWithEscape", "pause", "close")
        private val IGNORED = setOf(
            "nudge", "drawButtons", "contentWidth", "showWarning", "look", "compact", "accent", "face", "frame",
            "style", "buttons", "inputs", "afterAction", "command", "commandPermission", "perPage", "scale", "columns",
        )
        private val TAG = Regex("""^\[([a-z_:]+)]\s*(.*)$""")
        private val KNOWN: Map<String, Set<String>> = CONTAINERS.keys.associateWith { CONTAINER_KEYS + it } + mapOf(
            "space" to setOf("space"),
            "text" to setOf("text", "style", "align", "w", "icon", "color", "bold", "scale", "tooltip", "x", "y"),
            "icon" to setOf("icon", "size", "x", "y", "bind", "states"),
            "image" to setOf("image", "w", "x", "y"),
            "switch" to setOf("switch", "bind", "tooltip"),
            "segmented" to setOf("segmented", "selected", "bind", "w", "tooltip"),
            "button" to setOf("button", "bind", "w", "icon", "tooltip", "onClick"),
            "nav" to setOf("nav", "icon", "w", "onClick"),
            "dropdown" to setOf("dropdown", "options", "bind", "w", "tooltip"),
            "slider" to setOf("slider", "min", "max", "step", "unit", "bind", "w", "tooltip"),
        )
```

O `Reader.warn(path, message)` existente é usado também por `wrapper` (path `"raiz"`).

- [ ] **Step 5: `dialog/DialogActions.kt`**

```kotlin
package one_block.textura.dialog

import net.kyori.adventure.key.Key
import net.kyori.adventure.sound.Sound
import one_block.textura.api.DialogBinds
import one_block.textura.dialog.json.DefStep
import one_block.textura.dialog.node.UiOutcome
import one_block.textura.dialog.text.Placeholders
import org.bukkit.Bukkit
import org.bukkit.entity.Player

object DialogActions {

    fun run(steps: List<DefStep>, player: Player, session: DialogSession, warn: (String) -> Unit): UiOutcome {
        val placeholders = Placeholders(session)
        for (step in steps) {
            when (step) {
                is DefStep.Set -> step.pairs.forEach { (key, value) -> session.set(key, placeholders.plain(value)) }
                is DefStep.Message -> player.sendMessage(placeholders.rich(step.text))
                is DefStep.Command -> {
                    val line = placeholders.command(step.line) { key -> warn("valor de '{$key}' recusado em comando") }
                        ?: return UiOutcome.Refresh
                    Bukkit.dispatchCommand(if (step.console) Bukkit.getConsoleSender() else player, line)
                }
                is DefStep.Sound -> {
                    val key = runCatching { Key.key(step.id) }.getOrNull()
                    if (key == null) warn("som '${step.id}' inválido") else player.playSound(Sound.sound(key, Sound.Source.MASTER, step.volume, step.pitch))
                }
                is DefStep.Page -> return UiOutcome.Page(step.id)
                is DefStep.Open -> return UiOutcome.Open(step.id, DialogBinds(), step.args.mapValues { placeholders.plain(it.value) })
                DefStep.Close -> return UiOutcome.Close
                DefStep.Refresh -> Unit
            }
        }
        return UiOutcome.Refresh
    }
}
```

- [ ] **Step 6: Compilador: página, nós novos e ações**

Em `ScreenCompiler.kt`:
1. Em `compile`, troque `Context(theme, session).container(definition.root)` por `Context(theme, session).container(definition.page(session.page))`.
2. Adicione o método público (fora de `Context`):

```kotlin
    fun resolveIcon(theme: UiTheme, id: String): String {
        if (!id.startsWith(UiArtCatalog.UI_PREFIX) || kit.has(id)) return id
        val name = id.removePrefix(UiArtCatalog.UI_PREFIX)
        return UiArtCatalog.GLYPH_PREFIX + (theme.icons[name] ?: name)
    }
```

e troque o corpo de `Context.icon(id)` por `resolveIcon(theme, id)`.

3. Troque `Context.node(def)` por:

```kotlin
        fun node(def: DefNode): UiNode = when (def) {
            is DefContainer -> container(def)
            is DefSpace -> if (def.grow) ContainerNode(ContainerKind.Box, emptyList(), w = UiSize.Grow, h = UiSize.Px(0)) else ContainerNode.space(def.px)
            is DefText -> text(def)
            is DefIcon -> IconNode(icon(stateIcon(def)), def.size, def.position)
            is DefImage -> ImageNode(def.id, def.w, def.position)
            is DefSwitch -> switch(def)
            is DefSegmented -> segmented(def)
            is DefButton -> button(def)
            is DefNav -> nav(def)
            is DefDropdown -> dropdown(def)
            is DefSlider -> slider(def)
        }
```

4. No `button(def)`, troque a linha `val action = UiAction { player -> session.press(key, player) }` por:

```kotlin
            val action = def.click?.let { clickAction(it, def.bind) } ?: UiAction { player -> session.press(key, player) }
```

5. Adicione dentro de `Context`:

```kotlin
        private fun stateIcon(def: DefIcon): String =
            def.bind?.let { session.value(it) }?.let { def.states[it] } ?: def.id

        private fun clickAction(click: DefClick, bind: String?): UiAction = UiAction { player ->
            val first = bind?.let { session.press(it, player) } ?: UiOutcome.Refresh
            if (first != UiOutcome.Refresh) first else DialogActions.run(click.steps, player, session, warn)
        }

        private fun nav(def: DefNav): UiNode {
            val target = (def.click?.steps?.singleOrNull() as? DefStep.Page)?.id
            val active = target != null && target == session.page
            val style = theme.style(if (active) theme.nav.activeTextStyle else theme.nav.textStyle)
            val label = listOfNotNull(
                ContainerNode.space(2),
                def.icon?.let { IconNode(icon(it), theme.iconSize) },
                TextNode(placeholders.plain(def.label), style.color, style.bold),
            )
            return ContainerNode(
                ContainerKind.Box,
                listOf(
                    ImageNode(if (active) theme.nav.activeArt else theme.nav.art, w = UiSize.Grow),
                    ContainerNode(ContainerKind.Row, label, gap = theme.iconGap, cross = UiAlign.Center),
                    HotspotNode(def.click?.let { clickAction(it, null) }, null),
                ),
                align = UiAlign.Start,
                cross = UiAlign.Center,
                w = def.w,
            )
        }

        private fun dropdown(def: DefDropdown): UiNode {
            if (def.options.isEmpty()) return ContainerNode.space(0)
            val key = def.bind ?: def.path
            val default = def.options.indexOf(def.initial).coerceAtLeast(0)
            val label = def.options[session.choice(key, def.options, default)]
            val style = theme.style(theme.dropdown.textStyle)
            val action = UiAction {
                session.choose(key, def.options, (session.choice(key, def.options, default) + 1) % def.options.size)
                UiOutcome.Refresh
            }
            return ContainerNode(
                ContainerKind.Box,
                listOf(
                    ImageNode(theme.dropdown.art, w = UiSize.Grow),
                    ContainerNode(
                        ContainerKind.Row,
                        listOf(
                            ContainerNode.space(3),
                            TextNode(placeholders.plain(label), style.color, style.bold, w = UiSize.Grow),
                            ImageNode(theme.dropdown.arrow),
                            ContainerNode.space(3),
                        ),
                        gap = 2,
                        cross = UiAlign.Center,
                        w = UiSize.Grow,
                    ),
                    HotspotNode(action, def.tooltip?.let { tooltip(it, label) }),
                ),
                cross = UiAlign.Center,
                w = def.w,
            )
        }

        private fun slider(def: DefSlider): UiNode {
            val key = def.bind ?: def.path
            val valid = def.max > def.min && def.step > 0
            val current = (session.value(key)?.toIntOrNull() ?: def.value).coerceIn(def.min, maxOf(def.min, def.max))
            val width = (def.w as? UiSize.Px)?.value ?: DEFAULT_SLIDER_WIDTH
            val fraction = if (valid) (current - def.min).toDouble() / (def.max - def.min) else 0.0
            val fillWidth = Math.round(width * fraction).toInt()
            val minimumFill = kit.image(theme.slider.fill)?.slice?.let { it.left + it.right } ?: 1
            val style = theme.style(theme.slider.textStyle)
            val label = "$current${def.unit}"
            val tooltip = def.tooltip?.let { tooltip(it, label) }

            fun move(delta: Int) = UiAction {
                if (valid) {
                    val now = session.value(key)?.toIntOrNull() ?: def.value
                    session.set(key, (now + delta).coerceIn(def.min, def.max).toString())
                }
                UiOutcome.Refresh
            }

            val children = listOfNotNull(
                ImageNode(theme.slider.track, w = UiSize.Grow),
                if (fillWidth >= maxOf(minimumFill, 1)) ImageNode(theme.slider.fill, w = UiSize.Px(fillWidth)) else null,
                TextNode(label, style.color, style.bold, UiTextAlign.Center, UiSize.Grow),
                ContainerNode(
                    ContainerKind.Row,
                    listOf(HotspotNode(move(-def.step), tooltip), HotspotNode(move(def.step), tooltip)),
                    w = UiSize.Grow,
                    h = UiSize.Grow,
                ),
            )
            return ContainerNode(ContainerKind.Box, children, cross = UiAlign.Center, w = UiSize.Px(width))
        }
```

6. No `companion object` do compilador, adicione `const val DEFAULT_SLIDER_WIDTH = 80`.
7. Importe `one_block.textura.dialog.DialogActions` e `one_block.textura.dialog.node.UiTextAlign`.

- [ ] **Step 7: Opener: página padrão, validação, escape e pause**

Em `DialogOpener.show`, logo depois de `session.persistent = definition.controlBinds` (Task 1), adicione:

```kotlin
        val requested = session.page
        if (requested != null && definition.pages.isNotEmpty() && requested !in definition.pages) {
            plugin.logger.warning("[ui:${definition.id}] Página '$requested' não existe")
            session.page = null
        }
        if (session.page == null) session.page = definition.firstPage
```

Em `dialog(...)`, troque `.canCloseWithEscape(true)` por `.canCloseWithEscape(definition.escape || definition.close == null)` e `.pause(false)` por `.pause(definition.pause)`.

A checagem acima zera uma página inexistente para a primeira página. Para cumprir o Review Focus 4 ("fica na página atual"), troque, em `run`, o ramo `is UiOutcome.Page` por:

```kotlin
            is UiOutcome.Page -> {
                val definition = screens.screens[session.screenId]
                if (definition != null && definition.pages.isNotEmpty() && outcome.id !in definition.pages) {
                    plugin.logger.warning("[ui:${session.screenId}] Página '${outcome.id}' não existe")
                } else {
                    session.page = outcome.id
                }
                show(player, session)
            }
```

- [ ] **Step 8: Build**

Run: `./gradlew :plugins:textura:localJar`
Expected: `BUILD SUCCESSFUL`

---

### Task 5: Pack de todas as páginas, tela de exemplo, docs e verificação

**Files:**
- Modify: `dialog/kit/UiUsage.kt`
- Create: `server/plugins/Textura/configs/ui/social.json`
- Modify: `server/plugins/Textura/README.md`, `CLAUDE.md`

**Interfaces:**
- Consumes: `ScreenDefinition.pages`, `ScreenDefinition.containers()`, `DefIcon.states`, `ScreenCompiler.resolveIcon` (Task 4)

- [ ] **Step 1: `UiUsage` percorre páginas e estados**

Troque o bloco dentro de `screens.forEach { definition -> ... }`, a partir de `images += theme.controlArt()`, por:

```kotlin
                images += theme.controlArt()

                val compiler = ScreenCompiler(kit) { warn("Tela '${definition.id}': $it") }
                val pages: List<String?> = definition.pages.keys.toList().ifEmpty { listOf(null) }
                pages.forEach { page ->
                    val session = DialogSession(definition.id, DialogBinds()).also { it.page = page }
                    val screen = compiler.compile(definition, theme, session)
                    UiLayout(kit).layout(screen).images.forEach { placed ->
                        when (val variant = placed.variant) {
                            ImageVariant.Plain -> images += placed.id
                            is ImageVariant.Icon -> icons += IconSize(placed.id, variant.size)
                            ImageVariant.Surface -> surfaces += SurfaceHeight(placed.id, placed.height)
                        }
                    }
                }

                definition.containers().forEach { container ->
                    collectStates(container) { icon ->
                        (icon.states.values + icon.id).forEach { id -> icons += IconSize(compiler.resolveIcon(theme, id), icon.size) }
                    }
                }
```

Adicione no `companion object`:

```kotlin
        private fun collectStates(node: DefNode, sink: (DefIcon) -> Unit) {
            when (node) {
                is DefContainer -> node.children.forEach { collectStates(it, sink) }
                is DefIcon -> if (node.states.isNotEmpty()) sink(node)
                else -> Unit
            }
        }
```

e importe `DefContainer`, `DefIcon` e `DefNode` de `one_block.textura.dialog.json`.

- [ ] **Step 2: Tela de exemplo `server/plugins/Textura/configs/ui/social.json`**

```json
{
  "title": "Social",
  "theme": "stone",
  "hideFooter": false,
  "canCloseWithEscape": true,
  "showWarning": false,
  "document": {
    "row": [
      {
        "column": [
          {
            "row": [
              { "icon": "ui:dot-green", "size": 9, "bind": "status", "states": { "online": "ui:dot-green", "away": "ui:dot-gold", "busy": "ui:dot-red" } },
              { "text": "{player}", "style": "heading", "w": "fit" }
            ],
            "w": "grow", "h": "fit", "gap": 3, "pad": 3, "cross": "center", "surface": "well"
          },
          { "nav": "Perfil", "icon": "ui:user", "w": "grow", "onClick": { "action": "page", "value": "profile" } },
          { "nav": "Som", "icon": "ui:music", "w": "grow", "onClick": { "action": "page", "value": "audio" } }
        ],
        "w": 96, "h": "fit", "gap": 2, "pad": 2, "surface": "sheet", "name": "Sidebar"
      },
      {
        "column": [
          {
            "row": [
              { "space": 3 },
              { "text": "Perfil", "style": "heading", "w": "grow" }
            ],
            "w": "grow", "h": 15, "cross": "center", "surface": "head", "name": "Heading"
          },
          {
            "column": [
              {
                "row": [
                  { "text": "Status", "style": "body", "w": "grow" },
                  { "dropdown": "Online", "options": ["Online", "Away", "Busy"], "bind": "status", "w": 70, "tooltip": "Status\n⏵ Atual: {value}" }
                ],
                "w": "grow", "gap": 5, "cross": "center"
              },
              { "text": "Você está {status?online:disponível|away:ausente|busy:ocupado|disponível}.", "style": "muted", "w": "grow" },
              {
                "row": [
                  { "space": "grow" },
                  { "button": "Salvar", "w": "fit", "onClick": { "action": "run_command", "value": "[message] <green>Status salvo: {status}\n[sound] entity.experience_orb.pickup 1 1.4" } }
                ],
                "w": "grow"
              }
            ],
            "w": "grow", "h": "fit", "gap": 6, "pad": 5, "name": "Body"
          }
        ],
        "w": 200, "h": "fit", "pad": 1, "surface": "sheet", "name": "Profile"
      }
    ],
    "w": "fit", "h": "fit", "gap": 4
  },
  "pages": [
    { "id": "profile", "name": "Perfil" },
    {
      "id": "audio",
      "name": "Som",
      "document": {
        "row": [
          {
            "column": [
              {
                "row": [
                  { "icon": "ui:dot-green", "size": 9, "bind": "status", "states": { "online": "ui:dot-green", "away": "ui:dot-gold", "busy": "ui:dot-red" } },
                  { "text": "{player}", "style": "heading", "w": "fit" }
                ],
                "w": "grow", "h": "fit", "gap": 3, "pad": 3, "cross": "center", "surface": "well"
              },
              { "nav": "Perfil", "icon": "ui:user", "w": "grow", "onClick": { "action": "page", "value": "profile" } },
              { "nav": "Som", "icon": "ui:music", "w": "grow", "onClick": { "action": "page", "value": "audio" } }
            ],
            "w": 96, "h": "fit", "gap": 2, "pad": 2, "surface": "sheet", "name": "Sidebar"
          },
          {
            "column": [
              {
                "row": [
                  { "space": 3 },
                  { "text": "Som", "style": "heading", "w": "grow" }
                ],
                "w": "grow", "h": 15, "cross": "center", "surface": "head", "name": "Heading"
              },
              {
                "column": [
                  {
                    "row": [
                      { "text": "Música", "style": "body", "w": "grow" },
                      { "slider": 40, "min": 0, "max": 100, "step": 10, "unit": "%", "bind": "music", "w": 90, "tooltip": "Volume da música: {value}" }
                    ],
                    "w": "grow", "gap": 5, "cross": "center"
                  },
                  {
                    "row": [
                      { "text": "Sons de interface", "style": "body", "w": "grow" },
                      { "switch": true, "bind": "ui_sounds", "tooltip": "Sons de interface: {value}" }
                    ],
                    "w": "grow", "gap": 5, "cross": "center"
                  }
                ],
                "w": "grow", "h": "fit", "gap": 6, "pad": 5, "name": "Body"
              }
            ],
            "w": 200, "h": "fit", "pad": 1, "surface": "sheet", "name": "Audio"
          }
        ],
        "w": "fit", "h": "fit", "gap": 4
      }
    }
  ]
}
```

- [ ] **Step 3: Docs**

Em `server/plugins/Textura/README.md`, na seção `configs/ui/`, adicione uma subseção **"Formato do Studio"** cobrindo:
- a raiz com `document`/`pages`/`hideFooter`/`canCloseWithEscape`/`pause`;
- as folhas `nav`, `dropdown`, `slider`, `icon` com `bind`+`states`, e `space: "grow"`;
- `onClick` (`page`, `close`, `run_command` com a tabela de tags da spec);
- a regra de segurança de comandos e `{chave|raw}`;
- os placeholders (`{chave}`, `{chave?a:A|b:B|Padrão}`, `$value`, `{player}`, `{online}`, `{page}`, `%papi%`);
- a ordem de leitura de valores e a persistência em PDC;
- `DialogApi.setting`/`setSetting` e `DialogApi.open(player, id, mapOf("target" to nome)) { ... }`;
- a lista de chaves do Studio ignoradas.

Em `CLAUDE.md`, no bullet `dialog/` da seção Textura, acrescente ao fim:

```markdown
 Screens also accept the Studio editor's keys (pages + `nav`, `onClick` actions `[open] [page] [set] [message] [console] [player] [sound]`, `dropdown`, `slider`, icon `states`, `{key?a:A|b:B}` / `%papi%` placeholders); control values are strings persisted per player in PDC (`textura:ui_<bind>`) unless code binds them, readable via `DialogApi.setting(player, key)`.
```

- [ ] **Step 4: Build e deploy**

Run: `./gradlew :plugins:textura:localJar`
Expected: `BUILD SUCCESSFUL`

Pare o servidor (aba do terminal: Ctrl-C ou `stop`; espere fechar), rode `./gradlew :plugins:textura:localDeploy` e suba de novo com `.\start.bat` em `server/`. No console, espere a linha `[ui] N peça(s), …, 2 tela(s).` e nenhum `chave desconhecida`.

- [ ] **Step 5: Verificação in-game (usuário)**

Peça ao usuário para rodar `/textura dialog social` e conferir:
1. A sidebar mostra `{player}` resolvido e o ponto de status. Clicar em **Som** troca de página e marca o nav ativo; **Perfil** volta.
2. O dropdown **Status** cicla Online → Away → Busy, e o ponto e o texto `Você está …` mudam junto.
3. **Salvar** manda a mensagem verde com o status e toca o som.
4. O slider **Música**: metade esquerda −10, direita +10, preso entre 0 e 100, e o preenchimento acompanha.
5. **Review Focus 2:** sair do servidor, voltar e reabrir: status, música e o switch continuam.
6. **Review Focus 3.** Valores de controle nunca têm espaço, então o teste usa um valor que pode ter.
   1. Troque o `value` do botão Salvar para `[console] say %player_displayname%\n[message] não deveria aparecer` e rode `/textura reload`.
   2. Com PlaceholderAPI e a expansão `player`, e um display name com espaço ou cor, o clique é recusado: aparece `valor de '{player_displayname}' recusado em comando` no console e a mensagem não aparece.
   3. Sem um valor desses disponível, confira só o caminho seguro: `[console] say {status}` executa. Registre que o caminho inseguro ficou verificado só pela revisão de código.
   4. Desfaça.
7. **Review Focus 4:** troque um nav para `"value": "nao_existe"`, rode `/textura reload`, clique: aviso no console e a tela fica onde estava. Desfaça.
8. **Review Focus 5:** `/textura dialog settings` continua funcionando como antes.
9. **Review Focus 1:** acrescente `"look": "light", "compact": true` num botão e `"nudge": {"x":0,"y":0}` no raiz, e rode `/textura reload`. Nenhum aviso.
