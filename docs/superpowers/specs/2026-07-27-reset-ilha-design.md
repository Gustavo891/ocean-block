# Reset de ilha — design

**Data:** 2026-07-27
**Módulo:** `plugins/oneblock/system/islands`
**Status:** aprovado, pronto para plano de implementação

## Objetivo

Dar ao staff um comando para resetar a ilha de um jogador. Duas operações independentes:

- `terrain` — apaga o que está construído no mundo.
- `progress` — devolve o documento da ilha ao estado de ilha recém-criada.

Resetar "do zero" é rodar as duas. Nenhuma das duas invoca a outra.

## Comandos

Subcomandos de `IslandCommand` (`@CommandAlias("island|is")`), ambos com `@CommandPermission("screen.dev")`:

```
/is reset terrain <jogador> [-confirm]
/is reset progress <jogador> [-confirm]
```

- O dono comum **não** reseta a própria ilha. Não existe forma sem alvo.
- Alvo resolvido por `IslandManager.getIslandByOwnerName(nome.lowercase())`, o mesmo caminho de `/is <jogador>`. Funciona com o dono offline.
- Alvo sem ilha: `<red>Esse jogador não tem uma ilha!`.

### Confirmação

Sem `-confirm` o comando é somente leitura e imprime um preview:

- **terrain** — quantidade de blocos não-ar, mobs, itens no chão, minions, spawners e blocos custom que seriam removidos, e o tamanho da área.
- **progress** — fase e grid atuais, contagem de membros/trusted/blacklist e o nível de cada upgrade que seria perdido.

O preview termina lembrando que a outra metade do reset não será executada. Com `-confirm`, executa.

## Arquitetura

Novo pacote `one_block.system.islands.reset`:

| Arquivo | Responsabilidade |
|---|---|
| `IslandResetService` | Ponto de entrada único: `previewTerrain`, `previewProgress`, `resetTerrain`, `resetProgress`. Mantém um `Set<UUID>` de ilhas com reset de terreno em andamento; chamada concorrente na mesma ilha é no-op com aviso ao admin. |
| `IslandArea` | Retângulo da ilha em coordenadas de mundo: `world`, `minX/maxX/minZ/maxZ`, `minY/maxY`, e `contains(Location)`. Calculado por `IslandArea.of(island)`. É o contrato que service, cleaner e hooks compartilham, para ninguém repetir a matemática do cuboide. |
| `TerrainCleaner` | Limpeza em lotes de uma `IslandArea`. Não conhece `Island`: recebe a área e callbacks de progresso e término. |
| `IslandResetHook` | `onTerrainReset(island, area)` e `describeTerrainReset(island, area): String?` com implementação padrão retornando `null`. Interface comum, não `fun interface`, porque tem dois métodos. |
| `IslandResetHooks` | Registro dos hooks. `register(hook)` público; `fireTerrainReset` e `describeAll` `internal`. |

O comando não contém lógica: valida permissão e alvo, e delega ao service.

### Inversão de dependência

`islands` **não pode** depender de `minions`, `spawners` ou `custom-items` — a direção é a oposta (esses sistemas fazem `compileOnly(projects.plugins.oneblock.system.islands)`). Logo, a limpeza dos registros deles não pode morar dentro de `islands`.

Cada sistema registra o próprio hook no seu `init`, seguindo o padrão que `minions` já usa com `Crafts.register(...)`:

- `MinionsSystem` — remove via `PlacedMinionManager` os minions cuja `location` cai na `IslandArea`, junto com armor stand e text display.
- `SpawnersSystem` — mesma varredura no `PlacedSpawnerManager`.
- `CustomItemSystem` — mesma varredura no `PlacedBlockManager`.

Cada hook filtra pelo `area.contains(location)` recebido, **não** por `IslandManager.getIslandAt(...)`: aquele método testa contra a border atual da ilha, enquanto o reset varre a border máxima, e a diferença deixaria registros órfãos apontando para blocos já apagados.

`describeTerrainReset` devolve uma linha para o preview (ex.: `"12 minions"`) ou `null` quando o sistema não tem nada na área. O preview do service concatena o que os hooks devolverem com a própria contagem de blocos, mobs e itens.

Cada hook mexe apenas no próprio armazenamento. Um sistema novo com estado por ilha passa a precisar só de mais um `IslandResetHooks.register(...)`.

**Ordem:** os hooks rodam **antes** da limpeza dos blocos, para que consigam despawnar entidades enquanto os blocos de origem ainda existem.

Mobs e itens no chão não usam hook — `islands` remove direto com `world.getNearbyEntities` sobre o cuboide, ignorando `Player`.

## `TerrainCleaner`

### Área

`IslandArea.of(island)`: retângulo centrado em `island.position.originX` / `originZ`, lado `Island.BORDER_SIZE.last()` (100). Usa a border **máxima comprável**, não a atual, senão sobra entulho de quem já teve upgrade de tamanho e o perdeu. Altura: `world.minHeight` até `world.maxHeight`. No máximo 7×7 chunks.

### Fases

1. **Varredura.** Para cada chunk da área, tira um `ChunkSnapshot` e descarta as seções com `isSectionEmpty(i)` — numa skyblock a maior parte do volume é vazia. Nas seções restantes, coleta as posições com bloco não-ar dentro do cuboide. Essa lista alimenta tanto o preview quanto a limpeza.
2. **Limpeza.** Task síncrona a cada tick consumindo até 20.000 posições, com `block.setBlockData(AIR, false)`. `applyPhysics = false` é obrigatório: com física, areia e água em cascata custam caro e disparam updates inúteis. Progresso em porcentagem para o admin a cada segundo.

### Finalização

Recria `BEDROCK` em `center.y - 1` e, em `center`, um bloco sorteado do `blockPool` da fase atual. Reaproveita `IslandManager.generateStarterIsland`, hoje `private`, que passa a `internal`.

### Falhas

- Mundo `world` não carregado: aborta com erro antes de tocar em qualquer coisa.
- Reset já em andamento na mesma ilha: recusa com aviso.

## `resetProgress`

Devolve o documento ao padrão de ilha nova. Preserva apenas `uuid`, `ownerUuid`, `ownerName`, `center` e `position`.

```
currentPhaseId = "origin"      grid = 0              pendingRewardPhases.clear()
upgrades.clear()               size = 30             refreshWorldBorder()
members.clear()                trustedNames.clear()  blackList = null
settings = null                isPublic = false
spawn = SimpleLocation(position.originX + 0.5, 100.0, position.originZ + 0.5, "world")
```

### Efeitos colaterais obrigatórios

- **Membros expulsos.** Toda entrada de `IslandManager.playerIsland` apontando para essa ilha, exceto a do dono, é removida. Quem estiver online e fisicamente dentro da ilha é teleportado para o spawn do mundo — sem isso fica preso numa border que não é mais dele.
- **Border.** Cada jogador online na ilha recebe o `currentWorldBorder` novo via `island.onJoin(player)`; sem isso continua com a border antiga até relogar.
- **Animação em curso.** Se a ilha estiver congelada no meio de um avanço de fase, o id sai do `frozen` de `PhaseAdvancement`; caso contrário a ilha trava para sempre. Exige um `PhaseAdvancement.clear(islandId)` novo.
- `IslandHud.refresh(island)` e `island.markDirty()` ao final.

Não toca em nenhum bloco do mundo.

## Fora de escopo

- Reset iniciado pelo próprio dono.
- Desfazer / backup do que foi apagado.
- GUI de confirmação.
- Realocar a ilha para outra posição do grid.
- Apagar a ilha do banco (`IslandRepository.remove` continua sem uso).

## Convenções

Comentários e mensagens em português. Sem comentários no código. Sem testes.
