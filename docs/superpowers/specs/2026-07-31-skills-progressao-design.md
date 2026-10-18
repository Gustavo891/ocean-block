# Progressão de Skills — Design

**Data:** 2026-07-31
**Estado:** aprovado, pronto para plano de implementação

## Problema

O `system/skills` já existe mas é raso: quatro skills, cem níveis, e o único efeito do
nível é um bônus percentual linear que nenhum outro sistema lê. Não há nada para
alcançar. O jogador sobe de nível e não recebe nada além de uma linha no chat.

Este design transforma o nível em moeda de progressão: ele destrava mecânica,
receita e recompensa.

## Escopo

Cinco skills, cada uma com quatro camadas de retorno:

```
nível 1..100  ─┬─ bônus passivo escalonado   (+X% por nível, uma stat por skill)
               ├─ perk marco                 (mecânica nomeada, automática)
               ├─ receita liberada na /forge  (gate genérico, ver adiante)
               └─ recompensa de nível         (item / coins / comando)
```

Perks são **automáticos**. Não há pontos, árvore de talentos nem respec — o nível
sozinho decide. Essa decisão simplifica o modelo de dados (nada a persistir sobre
perks) e a interface.

## As cinco skills

| Skill | id | Fonte de XP | Passivo |
|---|---|---|---|
| Mineração | `mining` | quebrar pedra/minério | velocidade de quebra |
| Lenhador | `foraging` | quebrar log | drop de madeira |
| Agricultor | `farming` | colher crop madura | crescimento de plantação |
| Exterminador | `combat` | matar mob | dano causado |
| Pescador | `fishing` | pescar | sorte de pesca |

**Lenhador mantém o id `foraging`.** O nome de exibição muda, o id não — renomear o
id deixaria órfão todo o progresso já gravado no Mongo, que é chaveado por string.

`fishing` é novo e não tem dado nenhum gravado. Toda leitura cai em zero por padrão,
então documentos antigos decodificam sem tratamento especial.

### Persistência do progresso

O nível é **gravado**, não derivado do XP acumulado:

```kotlin
var levels: MutableMap<String, Int>? = null
var currentXp: MutableMap<String, Long>? = null
```

O acumulado, quando alguém precisa dele, é calculado:
`Skill.totalXpForLevel(level) + currentXp`.

O motivo é rebalanceamento. Com o nível derivado do acumulado, qualquer mexida em
`XP_BASE` ou `XP_EXPONENT` rebaixa todo mundo que já jogou — a curva nova reinterpreta
o mesmo número guardado. Com o nível gravado, mudar a curva só altera o que falta para
o próximo degrau.

`SkillData.xp` continua na classe como campo legado: é dele que
`migrateLegacyXp()` decompõe o acumulado antigo em nível + progresso, uma única vez, no
`SkillManager.load`. Depois da conversão o mapa é esvaziado. Os três mapas são
**nullable** — mapa com valor padrão quebra a decodificação dos documentos gravados
antes do campo existir.

Essa conversão aplica a curva vigente uma vez, então quem já tinha nível antes da
migração é reposicionado nela. Da migração em diante o nível fica imune a mudanças de
curva.

`SkillsView` tem layout de cinco slots (`"  OOOOO  "`), que bate exato com as cinco
skills. Sem paginação.

## Perks

Dois por skill, desbloqueio automático nos níveis 25 e 60. Todos foram escolhidos
para o loop real do OneBlock — quebrar o bloco central, farm construída na ilha,
mob-pool e monster party. Nada que dependa de mundo gerado (caverna, veia de
minério, árvore) faz sentido aqui.

### Mineração
| Nv | Perk | Efeito |
|---|---|---|
| 25 | Fundição | minério dropa já derretido — tira a fornalha do loop |
| 60 | Fortuna Extra | +1 nível efetivo de Fortuna no drop do bloco |

### Lenhador
| Nv | Perk | Efeito |
|---|---|---|
| 25 | Serra Afiada | chance de drop duplo de log |
| 60 | Seiva | chance de drop bônus junto ao log (sapling raro, favo) |

### Agricultor
| Nv | Perk | Efeito |
|---|---|---|
| 25 | Replantio | replanta a crop ao colher, consumindo semente do drop |
| 60 | Colheita em Área | colhe 3x3 de crops maduras |

### Exterminador
| Nv | Perk | Efeito |
|---|---|---|
| 25 | Pilhagem Extra | +1 nível efetivo de Pilhagem no loot |
| 60 | Massacre | dano em área ao matar — casa com monster party |

### Pescador
| Nv | Perk | Efeito |
|---|---|---|
| 25 | Isca Rápida | reduz tempo de fisgada |
| 60 | Rede | chance de puxar 2 itens de uma vez |

Os níveis 25 e 60 são valores de partida. Ficam como campo no enum `SkillPerk`,
ajustáveis sem tocar em nenhum listener.

### Modelo

```kotlin
enum class SkillPerk(
    val skill: Skill,
    val level: Int,
    val id: String,
    val prettyName: String,
    val description: String,
)
```

`Skills.hasPerk(uuid, perk)` = `level(uuid, perk.skill) >= perk.level`. Derivado,
não armazenado.

Cada skill tem um listener próprio em `perk/`, que consulta `hasPerk` antes de
aplicar o efeito. Um listener por skill em vez de um listener gigante: os arquivos
ficam pequenos e um perk quebrado não derruba os outros.

## Árvore de progressão

`/skills` → clique numa skill → `SkillTreeView`. Trilha nível a nível, com um galho em
cada nível que entrega alguma coisa.

A trilha é uma serpente que percorre as **colunas**, não as linhas: desce a coluna 0,
vira pela linha de baixo, sobe a coluna 2, vira pela linha de cima, desce a coluna 4,
e assim por diante. A célula da virada é um nível como qualquer outro, então a
numeração nunca salta. Em miniatura (3 linhas × 7 colunas):

```
1 789 5      col0=1  col2=7  col3=8  col4=9  col6=15
2 6 0 4      col0=2  col2=6          col4=10 col6=14
345 123      col0=3  col1=4  col2=5  col4=11 col5=12 col6=13
```

No inventário real são 5 linhas de trilha × 9 colunas: cinco segmentos verticais nas
colunas pares mais quatro células de virada nas ímpares = **29 níveis por página**,
4 páginas para os 100. A sexta linha é a navegação. As colunas ímpares fora da virada
ficam vazias — é o vão que deixa a serpente legível.

Cores do nó: verde alcançado, laranja o próximo, vermelho bloqueado. A quantidade do
item desenha o número do nível no slot (o cliente não desenha acima de 64, por isso o
nome também o carrega).

Nível que entrega alguma coisa troca o vidro pelo ícone do que entrega — livro
encantado para perk, o próprio item para recompensa, ouro quando é só coins. Como a
serpente ocupa todas as células do caminho, marcar no próprio nó é o único jeito de
destacar o galho sem furar a numeração.

Galho de perk usa `ENCHANTED_BOOK` (livro comum quando travado); galho de recompensa
usa o ícone do próprio item, ou ouro quando a recompensa é só coins.

A paginação é um `mutableState(0)` próprio, não `Pagination`: o galho precisa cair
exatamente na coluna seguinte à do seu nível, e a paginação da inventory-framework
preenche slots sem garantir esse pareamento.

## Desbloqueio de receitas

Nem toda receita depende de skill. Collections, fases da ilha e battle pass também
precisam poder travar receita. Um campo `requiredSkill` no `ForgeRecipe` só
atenderia skills e obrigaria mais um campo por sistema novo.

`system/craft` define uma interface abstrata e não conhece nenhum sistema:

```kotlin
// system/craft/forge/RecipeUnlock.kt
interface RecipeUnlock {
    fun isUnlocked(player: Player): Boolean
    fun requirementLine(player: Player): String
}
```

Cada sistema implementa a sua no próprio módulo, com `compileOnly(craft)`:

```kotlin
// system/skills
data class SkillUnlock(val skill: Skill, val level: Int) : RecipeUnlock

// system/collections
data class CollectionUnlock(val entry: CollectionEntry, val tier: Int) : RecipeUnlock

// system/islands
data class PhaseUnlock(val phaseId: String) : RecipeUnlock
```

A dependência aponta sempre para baixo. `craft` nunca aprende que skills existe.

### Como a receita é travada

Por id, depois do registro — não por campo no construtor:

```kotlin
object Crafts {
    fun register(recipe: ForgeRecipe)
    fun gate(recipeId: String, unlock: RecipeUnlock)
}
```

`ForgeRecipeManager` guarda um `Map<String, RecipeUnlock>` separado da lista de
receitas. Razões:

- **Quem trava não precisa ser quem registra.** As receitas do próprio `CraftSystem`
  ganham gate de skill sem `craft` depender de `skills`: o `SkillsSystem.init` chama
  `Crafts.gate(...)`.
- `ForgeRecipe` continua `data class` imutável.
- Sem gate registrado = receita livre. Nenhuma receita atual muda de comportamento.
- Ordem de `init` não importa — o mapa é consultado ao renderizar o GUI, não no
  registro.

Composição para receita com duas exigências:

```kotlin
Crafts.gate("mega_minion", RecipeUnlocks.all(
    SkillUnlock(Skill.Mining, 40),
    CollectionUnlock(CollectionEntry.Diamond, 5),
))
```

`RecipeUnlocks` expõe `all(vararg)` e `any(vararg)`.

O nível exigido por um gate de receita é independente dos níveis 25 e 60 dos perks —
uma receita pode exigir Mineração 40 sem que 40 seja marco de nada.

### Sem persistência no craft

Toda condição existente hoje é monotônica: nível de skill, tier de collection e fase
da ilha nunca regridem. A checagem é ao vivo, sem estado. Se um dia surgir um
desbloqueio de evento único, o sistema dono guarda a própria flag e o `isUnlocked`
dele lê — `craft` continua sem armazenar nada.

### Renderização

`CraftView` mostra a receita travada, não a esconde: receita invisível não motiva
ninguém a upar. Ícone vira `BARRIER`, a lore lista a `requirementLine` de cada
condição (verde se cumprida, vermelha se não), e o clique recusa com mensagem.

## Recompensas de nível

Reuso do `Reward` / `RewardType` que já existe. Eles moram em
`system/islands/reward`; fazer `skills` depender de `islands` só por um DTO inverte a
intenção do grafo. **`Reward` e `RewardType` migram para um módulo `lib:rewards`**,
consumido por islands e skills. Refactor pequeno e diretamente no caminho do trabalho.

Sem fila de recompensa offline: o level up só acontece com o jogador online. Se o
inventário estiver cheio, o item dropa no pé. `PhaseRewarder.grantOrQueue` não é
reaproveitado — ele existe porque a fase avança para uma ilha cujo dono pode estar
offline, o que não se aplica aqui.

Anti-duplicata: `SkillData` ganha

```kotlin
val rewardedLevels: MutableMap<String, Int>? = null
```

o maior nível já pago por skill. **O campo tem que ser nullable** — um campo primitivo
com valor padrão quebra a decodificação dos documentos já gravados.

`SkillRewarder` roda no level up, paga todos os níveis entre `rewardedLevels[skill]` e
o nível atual (cobre o caso de ganhar vários níveis de uma vez), e atualiza o mapa.

## Curva de XP

A curva atual é inalcançável: `100 * n^1.5` soma cerca de 4.000.000 de XP até o nível
100, com 1 XP por bloco quebrado.

Ajuste: base 100 → 60, expoente 1.5 → 1.35, e XP por ação bem maior.

| Skill | XP por ação | Nv 25 | Nv 60 | Nv 100 |
|---|---|---|---|---|
| Mineração | 3 / bloco | ~16k blocos | ~128k | ~427k |
| Lenhador | 8 / log | ~6k logs | ~48k | ~160k |
| Agricultor | 6 / crop madura | ~8k | ~64k | ~213k |
| Exterminador | 15 / mob comum, 40 / monster party | ~3k mobs | ~26k | ~85k |
| Pescador | 40 / fisgada | ~1,2k | ~10k | ~32k |

As taxas diferem por skill de propósito. Log e crop saem do block-pool com peso muito
menor que pedra, e crop ainda depende de o jogador construir a farm. Taxa única faria
Mineração disparar e as outras travarem.

`xpPerAction` vira campo do enum `Skill`. Os números acima são ponto de partida e
precisam de playtest.

## Estrutura de arquivos

```
lib/rewards/
  Reward.kt, RewardType.kt                novo    movidos de system/islands/reward
  RewardGranter.kt                        novo    entrega item/comando/mensagem

system/craft/
  forge/RecipeUnlock.kt                   novo    interface + RecipeUnlocks.all/any
  forge/ForgeRecipeManager.kt             alterado  Map<String, RecipeUnlock>
  Crafts.kt                               alterado  + gate(recipeId, unlock)
  view/CraftView.kt                       alterado  render travado, clique recusa
  view/CategoriesView.kt                  alterado  não conta receita travada

system/skills/
  skill/Skill.kt                          alterado  + Fishing, xpPerAction, curva nova
  skill/SkillPerk.kt                      novo    enum dos 10 perks
  skill/SkillReward.kt                    novo    SkillLevelReward + tabela SkillRewards
  skill/SkillData.kt                      alterado  + rewardedLevels nullable
  craft/SkillUnlock.kt                    novo    RecipeUnlock por skill+nível
  craft/SkillRecipeGates.kt               novo    as travas concretas, chamadas no init
  perk/MiningPerks.kt                     novo    ┐
  perk/LumberPerks.kt                     novo    │ um listener por skill,
  perk/FarmingPerks.kt                    novo    │ cada um checa hasPerk
  perk/CombatPerks.kt                     novo    │
  perk/FishingPerks.kt                    novo    ┘
  reward/SkillRewarder.kt                 novo    paga recompensa no level up
  listener/SkillListener.kt               alterado  + pesca, taxas por skill
  view/SkillProgress.kt                   novo    barra de progresso compartilhada
  view/SkillsView.kt                      alterado  clique abre a árvore
  view/SkillTreeView.kt                   novo    árvore de progressão nível a nível
  Skills.kt                               alterado  + hasPerk
  SkillsSystem.kt                         alterado  registra listeners de perk e gates

system/collections/craft/CollectionUnlock.kt   novo
system/islands/craft/PhaseUnlock.kt            novo
system/islands/reward/PhaseRewarder.kt         alterado  delega a RewardGranter
```

### Travas aplicadas às receitas existentes

| Receita | Exigência |
|---|---|
| `platinum_ingot` | Mineração 15 |
| `miner_minion` | Mineração 30 |
| `vacuum_hopper` | Mineração 40 |
| `farmer_minion` | Agricultor 30 |
| `terminator_minion` | Exterminador 30 |
| `repair_2` | Exterminador 20 |
| `repair_3` | Exterminador 45 |

`leather` e `repair_1` seguem livres — são a porta de entrada de cada linha.

## Fora de escopo

- Pontos de perk, árvore de talentos, respec — descartados por decisão de design.
- Alquimista e Comerciante como skills — avaliados e cortados.
- Config em YAML. Tudo em enum, no código. Rebalancear exige rebuild.
- Fila de recompensa offline.
- Ranking / leaderboard de skills.
