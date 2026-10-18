# Blocos custom no Textura — design

**Data:** 2026-07-28
**Status:** aprovado, aguardando plano de implementação

## Problema

O Textura hoje gera glyphs, fontes e itens. Não sabe nada sobre blocos. Duas necessidades:

1. **Retexturar blocos vanilla** — `DEEPSLATE_EMERALD_ORE` vira "Minério de Rubi" com PNG própria, nome próprio e drop próprio. A troca é global no servidor.
2. **Blocos custom de verdade** — blocos novos, com aparência própria no mundo, montados sobre as variantes de blockstate do `note_block`.

O `system/custom-items` já tem um eixo `CustomBlock`, mas ele usa material vanilla e só troca a aparência do **item**; colocado no mundo o bloco continua sendo um hopper/barrel comum.

## Divisão de responsabilidade

**Textura = visual.** Gera blockstates, modelos, texturas e lang; aloca as variantes de note_block; expõe `BlockApi`.

**oneblock (`system/custom-items`) = gameplay.** Colocar, quebrar, drop, som, partícula, limites, guardas de integridade.

Segue o padrão já estabelecido no repo: configuração visual em yml (editável sem build), regra de jogo em Kotlin (validada pelo compilador). A direção de dependência não muda — `custom-items` toma `compileOnly(projects.plugins.textura)`, como `islands` já faz.

## Textura

### Configuração

`server/plugins/Textura/configs/blocks/*.yml`, carregada recursivamente igual a `configs/items`.

```yaml
ruby_ore:
  type: OVERRIDE
  target: deepslate_emerald_ore
  texture: block/ruby_ore.png
  name: "Minério de Rubi"

ruby_block:
  type: CUSTOM
  base: NOTE_BLOCK
  texture: block/ruby_block.png
  item_material: paper

crystal_lamp:
  type: CUSTOM
  base: NOTE_BLOCK
  model: models/block/crystal_lamp.json
  textures:
    top: block/lamp_top.png
    side: block/lamp_side.png
  item_material: paper
```

Campos:

| Campo | Tipo | Obrigatório | Descrição |
|---|---|---|---|
| `type` | `OVERRIDE` \| `CUSTOM` | sim | Modo da entrada |
| `target` | string | `OVERRIDE` | Material vanilla a retexturar (minúsculo, sem namespace) |
| `name` | string | não | Nome exibido; injetado no lang como `block.minecraft.<target>` (OVERRIDE) ou `block.<ns>.<id>` (CUSTOM) |
| `base` | `NOTE_BLOCK` | `CUSTOM` | Bloco base que hospeda a variante. Único valor na fase 1 |
| `texture` | string | ver abaixo | PNG única; gera modelo `cube_all` |
| `textures` | mapa | ver abaixo | `top`/`bottom`/`side`/`north`/... ; gera `cube_bottom_top` ou `cube` |
| `model` | string | ver abaixo | Caminho de um JSON próprio; copiado sem tradução |
| `item_material` | string | não | Material base do item no inventário. Padrão `paper` |

`CUSTOM` exige exatamente uma fonte de aparência: `texture`, `textures` ou `model`. Declarar duas é erro de validação — a entrada é ignorada com aviso, não derruba o load. Mesma política de tolerância a erro que `ItemRegistry` e `PhaseManager` já usam.

`OVERRIDE` exige `texture`; `name` é opcional.

### Resolução de caminho

Reaproveita `PackAssets.resolve` — o mesmo mecanismo que items e glyphs usam, com fallback entre `resource/` e a pasta de configs. Arquivo já presente em `resource/` não é reescrito (o creative recusa dois arquivos no mesmo caminho do zip).

### Saída no pack — OVERRIDE

- `assets/minecraft/textures/block/<target>.png`
- Entrada `block.minecraft.<target>` mesclada nos lang existentes (`pt_br.json`, `en_us.json`)

Nenhum código de servidor participa. Uma entrada OVERRIDE sem `name` é literalmente só uma cópia de PNG — o passthrough de `resource/` já faria isso; o yml existe para manter a intenção declarada num lugar só e habilitar o merge de lang.

O merge de lang lê o JSON existente em `resource/assets/minecraft/lang/`, aplica as chaves das entradas OVERRIDE por cima e escreve o resultado no pack. Chave declarada no yml vence a do arquivo.

### Saída no pack — CUSTOM

- `assets/oneblock/textures/block/<id>.png` (ou as N faces declaradas)
- `assets/oneblock/models/block/<id>.json` — gerado ou copiado
- `assets/oneblock/models/item/<id>.json` — `parent` apontando para o modelo de bloco, para o ícone no inventário
- `assets/oneblock/items/<id>.json` — definição de item apontando para o modelo acima
- Uma variant em `assets/minecraft/blockstates/note_block.json`

Geração de modelo:

- `texture` sozinha → `parent: minecraft:block/cube_all`, `textures.all`
- `textures` com `top`+`bottom`+`side` → `parent: minecraft:block/cube_bottom_top`
- `textures` com faces individuais → `parent: minecraft:block/cube`
- `model` → JSON copiado literalmente; as texturas de `textures` ainda são gravadas para ele referenciar

### Alocação de blockstate

`note_block` tem 23 `instrument` × 25 `note` × 2 `powered` = **1150** combinações. `harp/0/false` fica reservada para o fallback vanilla, restando **1149** slots.

`generated/block-allocations.yml` guarda `id -> instrument/note/powered`.

Regras, nesta ordem de prioridade:

1. Um id já presente no arquivo mantém sua alocação para sempre.
2. Um id novo recebe o próximo índice de um contador monotônico, nunca um índice já usado.
3. Um id removido do yml **não perde** a reserva: a entrada continua no arquivo, apenas não gera variant no pack. Se o id voltar, recupera a mesma blockstate.

A regra 3 é o que impede um bloco já colocado no mundo de trocar de identidade. Sem ela, remover `ruby_block` e adicionar `sapphire_block` faria toda ruby colocada virar sapphire. Manter a reserva em vez de queimá-la também faz um id removido por engano voltar intacto.

Índice ↔ blockstate é determinístico: `powered = índice % 2`, `note = (índice / 2) % 25`, `instrument = índice / 50`. O índice 0 (`harp/0/false`) é o fallback vanilla e nunca é alocado.

Quando a capacidade acaba, o load registra erro e ignora as entradas excedentes — o pack continua sendo gerado.

`assets/minecraft/blockstates/note_block.json` recebe uma variant por alocação viva. Toda combinação não alocada aponta para `minecraft:block/note_block`, preservando o note block vanilla.

Fase 2 (fora deste escopo): `mushroom_block`, `tripwire`, `chorus_plant`. A arquitetura já as acomoda através da interface:

```kotlin
interface BlockStateAllocator {
    val base: Material
    fun allocate(id: String): BlockData
    fun capacity(): Int
    fun blockStateJson(): String
}
```

`NoteBlockAllocator` é a única implementação da fase 1.

### API

`one_block.textura.api.BlockApi`, mesmo formato dos outros api objects (referência `@Volatile` para o registry vivo, para que `/textura reload` propague):

```kotlin
object BlockApi {
    fun init(registry: BlockRegistry)
    fun has(id: String): Boolean
    fun get(id: String): BlockDefinition?
    fun ids(): Set<String>

    /** BlockData a colocar no mundo, ou null se o id não existe / não é CUSTOM. */
    fun blockData(id: String): org.bukkit.block.data.BlockData?

    /** Id do bloco custom nesta posição, ou null se for bloco comum. */
    fun idAt(block: org.bukkit.block.Block): String?

    /** Chave de item_model do item que representa o bloco. */
    fun itemModelKey(id: String): NamespacedKey?

    /** Material base do item do bloco. */
    fun itemMaterial(id: String): Material?
}
```

`idAt` lê `instrument`/`note`/`powered` do `Note Block` e consulta um mapa reverso pré-computado. Nunca toca em banco.

### Arquivos novos no Textura

```
model/BlockDefinition.kt          — data class + enum BlockEntryType
registry/BlockRegistry.kt         — leitura dos yml, validação
block/BlockStateAllocator.kt      — interface
block/NoteBlockAllocator.kt       — implementação + persistência das alocações
block/BlockModelWriter.kt         — geração de cube_all / cube_bottom_top / cube
api/BlockApi.kt
```

`ResourcePackBuilder` ganha `addBlocks(builder)`, chamado depois de `addItems`. `TexturaPlugin` instancia `BlockRegistry`, chama `BlockApi.init` e inclui o registry no caminho de `/textura reload`.

## oneblock — `system/custom-items`

### Extensão do `CustomBlock`

```kotlin
open val texturaBlockId: String? = null
```

Quando presente, `CustomBlockListener` aplica `BlockApi.blockData(texturaBlockId)` no bloco recém-colocado, e `buildItem` usa `BlockApi.itemMaterial` / `BlockApi.itemModelKey` em vez dos campos `material`/`itemModel` da classe.

Identidade no mundo vem da blockstate, não do banco: `BlockApi.idAt(block)` resolve o id sem query. `PlacedBlock` continua sendo criado **apenas** para blocos que têm estado próprio (storage, upgrades, filtro) — o vacuum hopper. Blocos decorativos e minérios não geram registro no Mongo.

`CustomBlockRegistry` ganha um índice `texturaBlockId -> CustomBlock` para o caminho de quebra.

### `NoteBlockGuard` — listener obrigatório

Sem isto o sistema se corrompe em minutos de jogo. Quatro cancelamentos, todos restritos a note blocks que `BlockApi.idAt` reconhece:

| Evento | Por quê |
|---|---|
| `NotePlayEvent` | Silencia o bloco |
| `BlockPhysicsEvent` | O servidor recalcula `instrument` a partir do bloco de baixo / cabeça acima a cada update. Sem cancelar, o bloco troca de identidade sozinho |
| `PlayerInteractEvent` (clique direito) | Clique incrementa `note` no vanilla |
| `BlockRedstoneEvent` | Redstone adjacente flipa `powered` |

O `PlayerInteractEvent` é cancelado **antes** de o `CustomBlock.onRightClick` rodar — o guard cancela o efeito vanilla, não o hook do bloco.

### Som e partícula

Não é possível pelo resource pack por bloco: `sounds.json` remapeia som globalmente (mudaria todo deepslate) e o note_block só tem som de madeira. Fica server-side, no caminho de quebra, com dois modos:

**Aditivo (padrão).** `event.isDropItems = false`, drop calculado à mão, e som/partícula tocados por cima. Preserva tudo que o vanilla faz na quebra: dano na ferramenta, XP, estatística, advancement. O som vanilla de quebra continua tocando junto com o custom.

**Substitutivo** (`replaceEffects = true`). Cancela o evento, `block.setType(Material.AIR, false)` (sem physics update nos vizinhos), dropa e toca só o som/partícula custom. Controle total do feel, ao custo de perder dano de ferramenta, XP e estatística — quem registra o bloco assume isso.

**Limitação aceita nos dois modos:** a partícula usa a textura de um material vanilla parecido, escolhido por bloco. Partícula de bloco não aceita textura custom no cliente.

### Override de bloco vanilla

`VanillaOverrideRegistry` — Kotlin, não yml, porque carrega comportamento:

```kotlin
data class VanillaOverride(
    val target: Material,
    val drop: (fortuneLevel: Int) -> List<ItemStack>,
    val sound: Sound? = null,
    val particleLookalike: Material? = null,
)
```

`VanillaOverrideListener` intercepta `BlockBreakEvent` nos materiais registrados e segue o caminho de quebra acima.

**Silk Touch: ignorado.** Quebrar com Silk Touch dropa o item custom, igual a quebrar sem. Não há caminho que devolva o bloco em si.

**Fortuna: respeitada**, com a fórmula vanilla de `ore_drops`:

```
i = Random.nextInt(level + 2) - 1     // -1 até level
if (i < 0) i = 0
multiplicador = i + 1                  // 1 até level + 1
```

Nível 0 sempre 1×. Nível 3 sorteia entre 1× e 4×, com 1× saindo em 2/5 dos casos e 2×/3×/4× em 1/5 cada — o clamp do `-1` é o que dobra a chance do 1×. Isso reproduz o `apply_bonus`/`ore_drops` vanilla exatamente.

O nível de fortuna vem da ferramenta na mão principal no momento do evento.

### Arquivos novos no oneblock

```
block/NoteBlockGuard.kt
block/VanillaOverride.kt
block/VanillaOverrideRegistry.kt
block/VanillaOverrideListener.kt
block/BlockBreakEffects.kt        — caminho compartilhado de quebra (cancelar / air / drop / som / partícula)
```

Alterados: `CustomBlock.kt` (campo `texturaBlockId`, `buildItem`), `CustomBlockListener.kt` (aplicar BlockData no place; resolver por blockstate no break), `CustomBlockRegistry` (índice reverso), `CustomItemSystem.init` (registrar os dois listeners novos).

## Fluxo de dados

**Colocar:** jogador usa item → `CustomBlockListener.onPlace` → resolve `CustomBlock` pelo NBT do item → se `texturaBlockId != null`, aplica `BlockApi.blockData(id)` → só cria `PlacedBlock` se o bloco tem estado.

**Quebrar:** `BlockBreakEvent` → se material é note_block, `BlockApi.idAt` resolve o id → `CustomBlockRegistry` acha o `CustomBlock` → `BlockBreakEffects` executa o caminho de quebra → `onBreak` do bloco roda → se havia `PlacedBlock`, remove.

**Reload:** `/textura reload` relê `configs/blocks`, o allocator preserva as alocações do arquivo, o pack é reconstruído. Blocos já no mundo mantêm identidade porque a alocação nunca muda.

## Erros e validação

Toda a leitura de yml segue o padrão do repo: entrada inválida gera aviso no log e é pulada, nunca derruba o load.

- `type` desconhecido → pula
- `target` que não é um Material válido → pula
- `CUSTOM` sem fonte de aparência, ou com mais de uma → pula
- Textura declarada que não existe em disco → pula, com o caminho no aviso
- Capacidade de blockstate esgotada → erro, entradas excedentes puladas
- `BlockApi.blockData` de um id inexistente → `null`; o listener não coloca nada e loga uma vez

## Fora de escopo

- Dureza de quebra custom (decidido: fica a do bloco base)
- Bases além de `note_block`
- Blocos não-cubo (modelo `cross`, tripwire) — depende da fase 2
- Migração do vacuum hopper para visual custom — possível depois, não faz parte desta entrega
- Regras de drop por mundo ou por ilha (decidido: override vale no servidor inteiro)
