# Spawners Stackáveis — Design

**Date:** 2026-07-24
**Status:** Approved (design), pending implementation plan
**Módulo:** `plugins/oneblock/system/spawners` (novo)

## Goal

Spawner colocável que o jogador empilha para escalar a produção de mobs. Cada
bloco guarda quantos spawners tem dentro (`stackSize`) e quantos mobs já nasceram
e continuam vivos (`liveMobs`). Os mobs nascem **stackados numa única entidade
sem IA**, parada em cima do bloco, que o jogador descasca a golpes — cada morte
tira um do stack e dropa o loot de um mob.

O ganho central é de TPS: um spawner com 128 spawners empilhados e 500 mobs vivos
custa **uma** entidade e **um** tick, não 500 de cada.

## Decisões (brainstorm)

- **Modelo de spawn:** custom, controlado pelo tick do módulo. O bloco `SPAWNER`
  vanilla é decorativo — spawn nativo desligado.
- **Mobs stackados, sem IA:** uma entidade representa N mobs.
- **Morte:** descasca. Cada golpe letal tira 1 do stack e dropa loot de 1 mob.
- **Comportamento do mob:** parado, inofensivo, imune a dano de ambiente. Só o
  jogador mata.
- **O que o stack controla:** quantos mobs nascem por intervalo. Spawner x64 põe
  64 mobs por ciclo.
- **Acúmulo:** o spawn novo funde no stack vivo (x64 vira x128…) até um teto por
  spawner. No teto, o tick simplesmente não spawna.
- **Empilhar:** clique direito no bloco com spawner do mesmo mob na mão.
- **Retirar:** só pela GUI, escolhendo a quantidade.
- **Limite por ilha:** nenhum. O freio é distância mínima de 5 blocos entre
  spawners.
- **Mobs suportados:** lista curada, agrupada em três tiers.
- **Loot:** tabela própria do spawner, por mob. Cai no chão. Não usa `mob-drops`.
- **Config:** tudo em código (enum + constantes). Sem YAML.
- **GUI:** info + retirar + liga/desliga. Sem upgrades compráveis.
- **Persistência dos mobs vivos:** `liveMobs` é salvo no bloco; a entidade é
  descartável e recriada quando o chunk carrega.
- **Obtenção:** loja própria do módulo (coins) + comando admin.
- **Módulo:** system novo e independente, não `content/` dentro de `custom-items`.

### Decisões descartadas e por quê

- **Vanilla amplificado** (mexer em `spawnCount`/`delay` do spawner nativo): lag
  escala com o stack e o vanilla ignora boa parte do controle que o sistema
  precisa.
- **Spawner virtual** (drops direto no storage, sem entidade): mata o combate.
- **Forçar chunk carregado** para manter os mobs vivos: cada spawner viraria um
  chunk ticket permanente, e o custo cresceria com a base de jogadores em vez de
  com quem está online.
- **`content/spawner/` dentro de `custom-items`**, reusando o framework
  `CustomBlock`: economizaria a infra de persistência, tick e listeners, mas
  acoplaria spawners ao vacuum hopper. Optado por módulo independente, ciente de
  que a infra é reescrita.
- **Estender o `Product` do `system/shop`** para vender item custom: centralizaria
  a compra, mas mexe em sistema em produção e devolveria o preço pro YAML.

## Arquitetura

### Por que módulo próprio (e o que isso custa)

O framework `CustomBlock` de `system/custom-items` resolve quase tudo que este
sistema precisa: persistência com flush em lote, tick global por bloco, listeners
de colocar/quebrar/clicar, proteção contra explosão e pistão, e remoção de
registro cujo bloco físico sumiu.

A decisão foi por um módulo independente mesmo assim, para que spawners evoluam
sem acoplamento com o vacuum hopper. A contrapartida precisa estar escrita: a
infra do `PlacedBlockManager` **é reescrita aqui**, incluindo os problemas que ela
já resolveu. Os que a implementação precisa reproduzir estão listados em
"Persistência" e "Casos-limite" — não são detalhe de implementação, são o preço
desta escolha.

### Estrutura de pacotes

```
one_block/system/spawners/
├── SpawnersSystem.kt              wiring: init, listeners, comandos, completions
│
├── spawner/
│   ├── SpawnerType.kt             enum — a config inteira (mob, tier, preço, loot…)
│   ├── SpawnerTier.kt             enum — I, II, III
│   ├── LootEntry.kt               material + faixa de quantidade + chance
│   ├── PlacedSpawner.kt           estado persistido de um spawner colocado
│   ├── PlacedSpawnerManager.kt    fonte de verdade em runtime + flush
│   └── PlacedSpawnerRepository.kt acesso ao Mongo, sem regra de negócio
│
├── rules/                         ARITMÉTICA — funções sem estado
│   ├── StackRules.kt              empilhar com teto, retirar N, sobra
│   ├── SpawnRules.kt              incremento por tick, decremento por morte
│   ├── PlacementRules.kt          distância mínima entre spawners
│   └── LootRoll.kt                sorteio da tabela, com Random injetado
│
├── entity/
│   ├── SpawnerEntity.kt           criação/atualização da entidade stackada
│   ├── SpawnerDisplay.kt          TextDisplay acima do bloco
│   └── SpawnerEntityKeys.kt       chaves de PDC (uuid do spawner)
│
├── listener/
│   ├── SpawnerBlockListener.kt    colocar, quebrar, clique, explosão, pistão
│   └── SpawnerDeathListener.kt    morte por jogador, dano, despawn
│
├── command/
│   └── SpawnerCommand.kt          /spawner give, /spawner loja
│
└── view/
    ├── SpawnerView.kt             info + retirar + liga/desliga
    └── SpawnerShopView.kt         loja por tier
```

A pasta `rules/` concentra toda a aritmética do sistema — empilhar, retirar,
incrementar mobs vivos, sortear loot — em funções sem estado. O resto é casca
fina em volta: listeners traduzem evento do Bukkit em chamada de regra, e o
manager guarda o resultado. Separar assim mantém as decisões numéricas legíveis
num lugar só, em vez de espalhadas por listener e GUI.

### Dependências

`compileOnly` em `lib:instance`, `lib:coordinate`, `lib:item-builder`,
`lib:formatter` e nos systems `islands` (ilha da localização, dono e membros) e
`currencies` (cobrança da loja). Registrado em `settings.gradle.kts`; o módulo
entra automaticamente no loop de shading do plugin por estar sob
`:plugins:oneblock:system:`.

`SpawnersSystem.init(plugin, viewFrame, paperCommandManager)` entra no
`setupSystems()` do `OneBlockPlugin`.

**`PlacedSpawnerManager.onDisable()` precisa entrar no `onDisable()` do
`OneBlockPlugin`.** Nada wira isso sozinho; sem essa linha, a última janela de
flush é perdida em todo desligamento.

## Modelo de dados

### `SpawnerType`

Enum, um valor por mob suportado. É a configuração inteira do sistema — sem YAML,
sem instância, sem estado.

Campos: `entityType`, `tier`, `displayName`, `price`, `spawnIntervalTicks`,
`maxLiveMobs`, `loot: List<LootEntry>`, `xpPerKill`.

O `typeId` persistido é o `name` do enum.

### `PlacedSpawner`

```kotlin
data class PlacedSpawner(
    val uuid: UUID,
    val location: SimpleLocation,
    val typeId: String,
    val ownerUuid: UUID,
    var stackSize: Int = 1,
    var liveMobs: Int = 0,
    var enabled: Boolean = true,
)
```

Sem `ItemStack` em campo nenhum, então o codec POJO do Mongo dá conta direto — o
problema de serialização que o `PlacedBlock` resolveu com base64 não existe aqui.
Se um campo de item aparecer no futuro, ele vai como `String` base64 via
`ItemSerializer` de `lib:item-builder`, nunca como objeto aninhado.

Coleção: `spawners`, índice único em `uuid`.

### Estado que **não** é persistido

O uuid da entidade viva e o do `TextDisplay` não entram no modelo. São
recalculados a cada reconciliação varrendo as entidades próximas pelo PDC. Guardar
o uuid convidaria a tratar a entidade como fonte de verdade — que é exatamente o
que este design evita.

O contador de ticks desde o último spawn também **não** é persistido: vive num
mapa em memória no `PlacedSpawnerManager`, zerado a cada spawn. Consequência
aceita: um restart reinicia o ciclo de todos os spawners, custando no máximo um
intervalo. Persistir um timestamp em troca disso não vale a escrita extra a cada
ciclo — e um timestamp salvo abriria a porta pra "spawn retroativo" ao carregar,
que é justamente o acúmulo offline que este design não quer.

## Ciclo de vida

### Tick

`PlacedSpawnerManager` roda uma task por tick que percorre os spawners
registrados. Para cada um, nesta ordem, saindo cedo:

1. `typeId` resolve num `SpawnerType`? Não → loga uma vez e ignora.
2. `enabled`? Não → ignora.
3. Chunk carregado? Não → ignora. **Consequência intencional:** spawner de
   jogador offline não acumula.
4. Bloco físico ainda é `SPAWNER`? Não → desregistra (validação preguiçosa, uma
   vez por spawner, no primeiro tick com o chunk carregado).
5. Passou `spawnIntervalTicks` desde o último spawn?
   → `liveMobs = min(liveMobs + stackSize, type.maxLiveMobs)`, marca dirty.
6. Reconcilia entidade e display.

### Reconciliação da entidade

Invariante: com o chunk carregado e `liveMobs > 0`, existe **exatamente uma**
entidade viva marcada com o uuid deste spawner.

- Não existe → cria em cima do bloco: `setAI(false)`, invulnerável, silenciosa,
  `setPersistent(false)` (não é salva com o chunk — a entidade é descartável e o
  dado real está no bloco), nome `<mob> x<liveMobs>` sempre visível, PDC com o
  uuid do spawner.
- Existe → atualiza só o nome se `liveMobs` mudou.
- `liveMobs == 0` → remove a entidade.

O `TextDisplay` acima do bloco segue a mesma regra e mostra o mob, o `stackSize` e
o `liveMobs` — e o estado desligado quando pausado.

### Morte

`SpawnerDeathListener` intercepta a morte de entidade com o PDC do sistema:

- Cancela o loot e o XP vanilla sempre.
- **Só** decrementa e dropa se a morte veio de golpe de jogador (`killer != null`).
  Qualquer outra causa — `/kill`, plugin externo, limpeza de entidades, restauração
  de backup — não mexe em `liveMobs` e não gera loot; a entidade reaparece no tick
  seguinte com o número correto.
- Golpe de jogador: `liveMobs -= 1`, sorteia `LootRoll` sobre a tabela do
  `SpawnerType`, dropa no chão no ponto do bloco, dropa o XP configurado, marca
  dirty.

Isso é o que fecha dupe: nenhuma remoção de entidade fora do caminho de morte por
jogador consegue gerar item.

Dano em área (poção de splash, varredura de espada) acerta a entidade uma vez e
portanto mata **1**, não o stack. Decisão consciente, coerente com "descasca -1
por morte".

## Interação do jogador

### Colocar

`BlockPlaceEvent` com spawner na mão. Valida em ordem, cancelando com mensagem na
primeira falha:

1. Está dentro de uma ilha (`IslandManager.getIslandAt`).
2. O jogador é dono ou membro (`island.ownerUuid == uuid || island.isMember(name)`).
3. Não há outro spawner num raio de 5 blocos (`PlacementRules`). A distância é
   euclidiana em 3D sobre as coordenadas do bloco, comparada ao quadrado para não
   pagar raiz — dois spawners a menos de 5.0 de distância são recusados.

Passou: cria `PlacedSpawner(stackSize = 1)`, seta `CreatureSpawner.spawnedType`
para o mob e `requiredPlayerRange = 0` (desliga o spawn nativo), cria o display,
`saveNow`.

### Empilhar

Clique direito no bloco com spawner na mão:

- Mob **diferente** → recusa com mensagem. Spawner de zumbi não vira de blaze.
- Mob igual → `stackSize += quantidade` até o teto de **128**; consome os itens
  usados e devolve a sobra na mão; atualiza display; `saveNow`.

### GUI

Clique direito com a mão vazia, para dono ou membro da ilha. Uma view, três zonas:

- **Info** — mob, `stackSize`, `liveMobs`/teto, segundos pro próximo spawn, dono.
- **Retirar** — botões de 1 / 8 / 64 / tudo. Devolve o item do `SpawnerType` **no
  inventário** (diferente de quebrar, que dropa no chão) e abate de `stackSize`.
  Inventário cheio → retira só o que couber e avisa. Retirar tudo remove o bloco,
  a entidade e o display — mas **só se tudo couber no inventário**; senão retira o
  que coube, o bloco continua de pé com o resto e o jogador é avisado.
- **Liga/desliga** — alterna `enabled`. Desligado, o tick não spawna e a entidade
  viva é removida, mas **`liveMobs` continua salvo** e a entidade volta ao
  religar. Desligar não é perder o acúmulo.

### Quebrar

Devolve `stackSize` spawners num item só, remove entidade, display e registro.
`event.isDropItems = false` — sem isso o Bukkit dropa também o `SPAWNER` vanilla e
duplica material.

### Item na mão

Sem estado no PDC além do id do `SpawnerType`. Dois spawners do mesmo mob empilham
no inventário normalmente — o que é o que faz "empilhar ao colocar" ter sentido.

### Comandos

- `/spawner give <tipo> <jogador> [qtd]` — permissão de admin, completion do tipo
  alimentada pelo enum (mesmo padrão de `registerCompletions` já usado no projeto).
- `/spawner loja` — abre a `SpawnerShopView`.

### Loja

View própria do módulo, agrupada por tier. Compra cobra com
`Currencies.withdraw(uuid, "coins", price)` depois de checar `Currencies.has`, e
entrega o item. O preço mora no enum, junto do resto da config.

Não usa o `system/shop`: o `Product` de lá é `material + price` vindo de YAML e não
representa item com NBT.

## Persistência

Padrão Manager + Repository do projeto:

- **`PlacedSpawnerRepository`** — dona da coleção `spawners`, cria o índice único
  em `init`, expõe `toList()` / `update(x)` / `updateAll(list)` (bulk
  `ReplaceOneModel` com upsert) / `remove(uuid)`. Sem regra de negócio.
- **`PlacedSpawnerManager`** — `byLocation` e `byUuid` como `ConcurrentHashMap`,
  `dirtyIds` sincronizado. `onInit()` carrega tudo com atraso de boot (o mundo
  precisa estar carregado). Flush em lote a cada 10s. `onDisable()` grava
  **síncrono** — o `ScheduledExecutor` pode já estar encerrando e engoliria a
  última janela.

Escritas por tick (spawn, morte) vão para `dirtyIds`. Ações que o jogador veria
sumir num crash — colocar, empilhar, retirar, quebrar, ligar/desligar — usam
`saveNow`.

Quatro armadilhas que a reescrita da infra precisa reproduzir, todas já pagas uma
vez no `PlacedBlockManager`:

1. **Snapshot na main thread, I/O fora dela.** O flush copia os objetos sujos na
   main e só então entrega as cópias ao `ScheduledExecutor`. Entregar o objeto
   vivo faria o encoder do Mongo percorrer campos que a main thread está alterando.
2. **`upsert` não pode ressuscitar registro apagado.** `markDirty` e `saveNow`
   ignoram objeto que não é mais o registro vivo daquele uuid — senão um clique
   numa GUI de spawner já quebrado reinsere o documento.
3. **Chunk descarregado não roda tick.**
4. **Validação preguiçosa do bloco físico**, no primeiro tick com o chunk
   carregado. Validar tudo no boot forçaria carregar todos os chunks.

## Casos-limite

**Entidade e display órfãos.** O servidor cai entre remover o bloco e limpar as
entidades, ou alguém apaga o registro direto no Mongo. Sem tratamento sobra um mob
invulnerável e imortal no mundo, que nada além de `/kill` remove — o pior modo de
falha do sistema. No boot, depois de carregar os registros, varre as entidades
marcadas com o PDC do sistema e remove as que não têm `PlacedSpawner`
correspondente.

**`typeId` desconhecido.** Enum renomeado com registro já no banco. Loga uma vez
por uuid; o spawner fica inerte, sem tick e sem entidade. **Não apaga** — o dado
do jogador não some por causa de um rename.

**Bloco físico sumiu** (WorldEdit, rollback, backup). Detectado na validação
preguiçosa: desregistra e loga.

**Explosão e pistão.** Blocos registrados são retirados da lista da explosão e
movimentos de pistão sobre eles são cancelados. Deixar explodir sumiria com o
bloco físico e manteria a entrada no Mongo.

**Inventário cheio ao retirar.** Retira só o que couber, avisa, mantém o resto no
bloco. Nunca dropa no chão sem avisar.

**Ilha deletada.** *Limitação conhecida.* `IslandManager` não expõe exclusão de
ilha hoje — `IslandRepository.remove` existe mas nada o chama —, então não há
gancho onde limpar os spawners de uma ilha removida. Se a exclusão for
implementada, ela precisa varrer e remover os `PlacedSpawner` da região. Fica
registrado aqui em vez de resolvido pela metade.

## Balanço

| Tier | Mobs | Intervalo | Teto de vivos | Preço |
|---|---|---|---|---|
| I | Zumbi, Esqueleto, Aranha | 10s | 500 | 5.000 coins |
| II | Creeper, Enderman, Zumbi Pigman | 15s | 400 | 25.000 coins |
| III | Blaze, Esqueleto Wither, Bruxa | 20s | 300 | 100.000 coins |

Teto de `stackSize` por bloco: **128**, igual para todos os tiers.
Distância mínima entre spawners: **5 blocos**.

Ficam fora de propósito:

- **Slime e Magma Cube** — dividem ao morrer, brigam com o modelo de stack.
- **Ghast** — hitbox grande demais parada em cima do bloco.
- **Guardian** — precisa de água.

Loot: tabela por mob no enum, como lista de `LootEntry(material, min..max,
chance)` mais o XP por morte.

## Verificação

**Sem testes automatizados, por decisão.** O módulo não adiciona Kotest nem
`tasks.test`, e o `build.gradle.kts` fica só com as dependências de compilação.

A verificação é manual, em servidor, e por isso precisa ser roteirizada — o plano
de implementação carrega o roteiro completo. Os pontos que não podem faltar,
porque falham em silêncio:

- **Dupe.** `/kill` na entidade não pode gerar loot nem alterar `liveMobs`; a
  entidade deve reaparecer no tick seguinte com o mesmo número.
- **Imunidade a ambiente.** O mob não toma dano de lava, queda nem fogo, mas
  morre normalmente por golpe de jogador.
- **Persistência do acúmulo.** Reiniciar o servidor com mobs vivos e conferir que
  a entidade volta com o número salvo.
- **Distância mínima.** Colocar a 3 blocos é recusado; a 6, aceito.
- **Retirada parcial.** Inventário cheio retira só o que couber e mantém o bloco
  de pé com o resto.

O contra desta escolha, registrado: a aritmética de `rules/` — teto de stack,
sobra ao empilhar, corte no `maxLiveMobs`, faixa de quantidade do loot — é
exatamente o tipo de erro que passa despercebido em teste manual, porque o
resultado errado ainda parece plausível em jogo. Se em algum momento o sistema
ganhar testes, é por `rules/` que eles começam: as funções lá já são puras e não
precisam de nada além de Kotest para rodar.

## Fora de escopo

- Upgrades compráveis no spawner (a progressão vem de empilhar mais).
- Venda pelo `system/shop`.
- Integração com `mob-drops` (a tabela de loot é própria).
- Obtenção por fase do one block ou battle pass.
- Spawner de mob passivo.
- Limpeza de spawners na exclusão de ilha (ver "Casos-limite").
