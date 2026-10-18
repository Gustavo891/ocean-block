# Dialog UI — funcionalidades do Studio — design

**Data:** 2026-10-12
**Módulo:** `plugins/textura` (`dialog/`, `api/`)
**Base:** `docs/superpowers/specs/2026-10-12-dialog-ui-json-design.md` (engine atual)
**Referência:** formato dos dialogs desenhados do Dialog Engine / Studio (`github.com/resourcepack-ai/dialog-engine`, samples `drawn/samples/social` e `punish`)
**Status:** rascunho para revisão

## Objetivo

Dar ao engine de telas do Textura as funcionalidades do formato que o editor Studio exporta. O alvo é montar telas completas **só com JSON**:
- várias páginas com navegação lateral;
- botões que executam ações;
- dropdown e slider;
- textos e ícones que mudam com o estado;
- estado que persiste no jogador sem código.

Um `document` exportado do Studio deve funcionar colado em `configs/ui/<id>.json`, com as mesmas chaves.

**Licença.** O Dialog Engine é GPL-3.0. Nada é copiado de lá, nem código nem shaders. Só o formato (nomes de chave e semântica) é seguido, e a implementação é nossa, sobre o pipeline existente: layout, render, surfaces por altura, custom click.

## Decisões

| Decisão | Escolha |
|---|---|
| Arquivo | Continua `configs/ui/<id>.json`; aceita as chaves do Studio |
| Chaves só de render do Studio | Aceitas e ignoradas sem aviso: `nudge`, `drawButtons`, `contentWidth`, `columns`, `showWarning`, `look`, `compact`, `accent`, `face`, `frame`, `style` (no raiz), `buttons`, `inputs`, `afterAction`, `command`, `commandPermission` |
| Valores | Sempre texto |
| Persistência | PDC do jogador, chave `textura:ui_<bind>` |
| PlaceholderAPI | Opcional (softdepend), via `PlaceholderAPI.setPlaceholders` quando presente |
| Fora desta rodada | Cabeça de jogador (`head:`), `look`/`accent`, listas `for-each`, condicionais `if:`, comando que abre a tela (`command:`) |

## Formato

### Raiz

O raiz pode ser:
1. **um container**, como hoje: `{"column": [...], "theme": "stone", ...}`;
2. **uma tela com páginas:**

```json
{
  "title": "Social",
  "theme": "stone",
  "hideFooter": true,
  "canCloseWithEscape": true,
  "pause": false,
  "document": { "row": [ ... ] },
  "pages": [
    { "id": "profile", "name": "Profile" },
    { "id": "chat", "name": "Chat", "document": { "row": [ ... ] } }
  ]
}
```

- `document` é a primeira página. Se `pages` existir, a primeira entrada sem `document` é o id dessa página.
- Uma página sem `document` usa o `document` do raiz.
- `theme` pode vir no raiz ou dentro do `document`; o do raiz vence.
- `hideFooter: true` tira o botão de fechar, como `"close": null`.
- `canCloseWithEscape` e `pause` vão direto para o `DialogBase` (defaults: `true` e `false`).
- `title` aceita placeholders.

### Folhas novas e estendidas

| Forma | Campos | Comportamento |
|---|---|---|
| `{"nav": "Rótulo"}` | `icon`, `w`, `onClick` | Item de aba. Fica "ativo" quando `onClick.action == page` e `onClick.value` é a página atual |
| `{"dropdown": "Default"}` | `options` (lista de rótulos), `bind`, `w`, `tooltip` | Mostra o rótulo atual com uma seta. Cada clique avança para a próxima opção, voltando ao início no fim. O valor inicial é o id do texto do campo |
| `{"slider": 40}` | `min`, `max`, `step`, `unit`, `bind`, `w`, `tooltip` | Trilho com preenchimento proporcional e o texto `<valor><unit>` centralizado. Clicar na metade esquerda faz `−step` e na direita `+step`, sempre dentro de `[min, max]` |
| `{"icon": "ui:x"}` | `+ bind`, `states: {valor: icone}` | Desenha `states[valor atual]`; sem correspondência, desenha o `icon` |
| `{"button": "Rótulo"}` | `+ onClick` | Com `onClick`, executa a ação. Com `bind`, chama o handler do código como hoje. Com os dois, o handler roda primeiro e as ações depois |
| `{"segmented": [...]}` | `selected` (índice) | O valor passa a ser o id da opção |
| `{"space": "grow"}` | — | Espaço que ocupa o que sobra na linha. O Studio usa isso para empurrar botões |

**Id de opção:** o rótulo em minúsculas, com espaços trocados por `_`. Exemplo: `Perm` vira `perm`, `Last seen` vira `last_seen`. O Studio faz o mesmo (`{pun_length?perm:Perm|…}`).

### `onClick`

```json
"onClick": { "action": "page", "value": "chat" }
"onClick": { "action": "close", "value": "" }
"onClick": { "action": "run_command", "value": "[message] <green>Salvo!" }
```

O `value` do `run_command` é uma ou mais linhas separadas por `\n`. Cada linha:

| Linha | Efeito |
|---|---|
| `[open] id` | Abre a tela `id`. Os valores da sessão atual são levados junto |
| `[page] id` | Troca de página e redesenha |
| `[close]` | Fecha |
| `[refresh]` | Redesenha |
| `[set] chave=valor ...` | Grava valores. Se a chave tem `bind` na tela, segue a mesma regra de persistência dos controles |
| `[message] texto` | MiniMessage para o jogador |
| `[console] comando` | Console executa |
| `[player] comando` | O jogador executa |
| `[sound] id [volume] [pitch]` | Toca o som |
| sem tag | Igual a `[player]` |

As linhas rodam em ordem. Depois delas, a tela é redesenhada, a menos que uma ação tenha feito `[open]`, `[close]` ou `[page]`.

**Segurança.**
- Em `[console]` e `[player]`, todo placeholder substituído precisa casar `[A-Za-z0-9_.:-]*`. Caso contrário, aquela linha e as seguintes não rodam, e o console registra `[ui:<id>] valor de '{chave}' recusado em comando`.
- `{chave|raw}` libera um valor explicitamente.
- Os valores vêm de controles com opções fixas, de settings e de PlaceholderAPI. Não existe campo de texto livre.

## Placeholders

Valem em `text`, rótulos (`button`, `nav`, `dropdown`), `tooltip`, `title` e ações.

| Forma | Resultado |
|---|---|
| `{chave}` | Valor da chave (ver resolução) |
| `{player}`, `{player_uuid}`, `{online}`, `{page}` | Embutidos |
| `{chave?a:A\|b:B\|Padrão}` | Texto do caso que casa com o valor; sem caso, o último item sem `:`; sem padrão, vazio. `$value` dentro do texto é o valor |
| `{chave\|raw}` | Igual a `{chave}`, mas liberado em comandos |
| `%papi%` | PlaceholderAPI, quando o plugin está presente |
| `{value}` | Em tooltip de controle, o rótulo do valor atual (como hoje) |

Uma chave que ninguém conhece fica escrita como está.

Onde o texto é MiniMessage (tooltip, título, `[message]`), os valores entram via `Placeholder.unparsed` e não podem injetar tags. Em `text` e rótulos, que não são MiniMessage, entram como texto puro, com `\n` virando espaço.

## Valores e estado

`DialogSession` guarda um mapa `valores: Map<String, String>` mais `page`. A leitura de uma chave segue esta ordem:
1. bind do código (`DialogBinds`);
2. setting salvo no jogador (PDC), se a chave é `bind` de algum controle da tela;
3. valor da sessão (vindo de `[set]`, `[open]` e cliques);
4. default do JSON (`switch: true`, `selected`, texto do `dropdown`, número do `slider`).

Na escrita de um clique ou `[set]`:
- se há bind do código, chama o `set` dele;
- senão, se a chave é `bind` de um controle da tela, grava no PDC;
- senão, grava só na sessão.

Os binds atuais seguem funcionando:
- `switch(key, get: Boolean, set)` converte de/para `"true"`/`"false"`;
- `segmented(key, get: Int, set)` converte índice ↔ id da opção;
- novos: `dropdown(key, get: () -> String, set)`, `slider(key, get: () -> Int, set)` e `value(key, get: () -> String, set)` (genérico).

**API:**
- `DialogApi.setting(player, key): String?` e `DialogApi.setSetting(player, key, value)` leem e gravam o PDC, para outros sistemas usarem o estado salvo.
- `DialogApi.open(player, id, values: Map<String, String> = emptyMap()) { binds }` passa valores iniciais, como `target`.

## Tema

Arte nova em `UiTheme`, com default por id do tema:

```yaml
stone:
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

- `surfaces` ganha `well` no tema `stone`.
- A arte placeholder nova (nav normal/ativo 40×14, dropdown 40×12, seta 7×4, trilho/preenchimento 24×10 com `.slice 2 2`, well 16×16 com `.slice 3 3 3 3`) é gerada pelo mesmo script PowerShell da primeira rodada.
- `ui:<nome>` de ícones do Studio que não existem (`ui:dot-green`, `ui:music`…) resolvem pelo alias `icons:` do tema, como hoje.

## Pack (`UiUsage`)

A coleta de arte passa a cobrir:
- **todas as páginas** de cada tela, compiladas uma a uma com estado default;
- **todos os `states`** de cada ícone com estado, e o `icon` padrão;
- a arte de nav, dropdown e slider do tema (`controlArt()`), já que o estado ativo/inativo e o preenchimento variam em runtime.

Alturas continuam estáticas por página. O preenchimento do slider é uma imagem fatiada esticada na largura, então não cria tamanhos novos.

## Runtime

**Compilação.** `ScreenCompiler` compila a página atual (`session.page`). Os placeholders são resolvidos por um `PlaceholderResolver` novo em `dialog/text/`. Ele usa a sessão e o jogador (`player` passa a ser conhecido na compilação; no `UiUsage` é `null`, e placeholders do jogador viram texto vazio).

**Ações.** `UiAction` passa a carregar uma lista de passos:
- `Set(key, value)`, `Page(id)`, `Open(id)`, `Close`, `Refresh`;
- `Message(text)`, `Command(sender, line)`, `Sound(id, vol, pitch)`;
- `Bind(key)` (handler do código).

O `DialogOpener` executa os passos em ordem, na thread principal, aplica a regra de segurança de comandos e decide o resultado final (redesenhar, trocar página, abrir, fechar).

**Persistência.** `PlayerSettings` (novo, `dialog/`) lê e grava strings no PDC com `NamespacedKey(textura, "ui_<chave>")`. Chaves que não são `[a-z0-9_.-]` são recusadas com aviso.

## Erros e avisos

- Seguem o padrão atual: um aviso por chave e por kit, com o caminho do nó.
- Ação desconhecida (`[xyz]`): aviso, linha ignorada.
- `page` para uma página que não existe: aviso, nada acontece.
- Slider com `min >= max` ou `step <= 0`: aviso, e o slider é desenhado parado.
- `dropdown` sem `options`: aviso, desenhado vazio.

## Verificação

Sem testes automatizados (preferência do projeto). Verificação in-game com uma tela de exemplo `configs/ui/social.json`, montada com 2 páginas e um nav, um dropdown, um slider, um ícone com `states`, um botão com `[message]` + `[page]` e textos com `{player}` e `{chave?…}`:
- nav troca de página e marca a ativa;
- dropdown cicla;
- slider respeita min/max/step;
- o valor sobrevive a relog (PDC);
- um `[console]` com valor inseguro é recusado;
- o `settings.json` atual continua funcionando igual.
