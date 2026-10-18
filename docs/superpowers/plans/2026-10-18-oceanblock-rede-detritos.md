# OceanBlock — Rede de Detritos (coletor passivo com inventário)

Pedido do usuário: um bloco que coleta os recursos que passam, com inventário próprio. Decisões do Claude (o usuário pediu "faça").

## Bloco
- `CustomBlock` no módulo `drift` (pacote `ocean_block.system.drift.net`), id `rede`, material `LOOM`, nome `<white>Rede de Detritos`, lore `<gray>Coleta detritos que passam`, `<gray>a até 3 blocos.`, `<gray>Clique direito para abrir.`
- Registrado em `CustomBlockRegistry` no `DriftSystem.init` (mesmo com a deriva desligada).
- `limitPerIsland = 4`; `storageCapacity = 27`.
- Receita vanilla `"PSP" / "SSS" / "PSP"`, `P` = `Tag.PLANKS`, `S` = `STRING`, chave `oceanblock:rede`, resultado `buildItem()`. Só registra se ausente.
- `/citem` (ou o comando de bloco custom existente) consegue dar o item.
- Clique direito abre a tela de armazenamento (27 slots) somente para quem passa em `CollectorRule.canCollect(ilha do bloco, player)`; os demais não abrem e não recebem mensagem. A interação vanilla do tear é cancelada sempre.
- Quebrar: comportamento padrão do framework (o item volta com o armazenamento guardado no item).

## Tela de armazenamento
- Reaproveitar o padrão de `HopperStorageView` do vacuum hopper removido (`git show 49a100b^:plugins/oneblock/system/custom-items/src/main/kotlin/one_block/system/custom_items/content/vacuum_hopper/view/HopperStorageView.kt` e arquivos vizinhos `HopperViewGuard.kt`, `VacuumHopperView.kt`), adaptado: só a parte de armazenamento (sem upgrades, sem filtro), título `Rede de Detritos`, jogador pode tirar itens e não pode colocar. Ao fechar/alterar, `placed.setStorage(...)` + `PlacedBlockManager.markDirty(placed)`. Proteger contra duas pessoas abrindo ao mesmo tempo como o guard original fazia.
- Registrar a view no `viewFrame`: `DriftSystem.init` passa a receber o `ViewFrame` (`init(plugin, viewFrame)`), e o `OceanBlockPlugin` passa o frame.

## Coleta
- Raio `rede.raio: 3.0` (distância 3D entre a posição renderizada do detrito e o centro do bloco), novo no `drift.yml` (seção `rede`), com parser/avisos e teste como os demais.
- A cada passo de movimento, para cada detrito coletável (`isCollectable`, não sendo puxado), procura a rede mais próxima dentro do raio **no mesmo lote** cuja armazenagem não esteja cheia. Se achar, o detrito passa a ser puxado **para a rede** (mesma animação do gancho, 10 ticks, destino = centro do bloco + 0.5) e, ao chegar, os drops de `DropRoller.roll` vão para o armazenamento da rede (sobras descartadas, nunca dropadas no chão), com respingo no bloco. Se a rede ficar cheia ou sumir no meio do puxão, o detrito é descartado sem entrega.
- Generalizar o alvo do puxão: `sealed interface PullTarget { Player(uuid); Net(placedUuid) }` em `Debris`. Gancho e rede usam o mesmo caminho.
- Índice de redes por lote: `NetIndex` reconstruído a cada tick de spawn (ou quando um bloco `rede` é colocado/quebrado) a partir de `PlacedBlockManager.all()` filtrando `blockId == "rede"` e agrupando pela ilha em `IslandManager.getIslandAt(location)`. O passo de movimento consulta só a lista do lote (no máximo 4).
- Sem leitura de bloco nova; nunca carregar chunk (a rede só conta se o chunk dela estiver carregado).

## Unidades puras (Kotest)
- `NetRules.nearest(debrisX, debrisY, debrisZ, nets: List<NetPoint>, radius: Double): NetPoint?` — mais próxima dentro do raio, ignorando cheias.
- `NetRules.fits(...)` ou equivalente para decidir se cabe (pode ser via contagem de slots livres + pilhas).
- Testes: fora do raio → null; escolhe a mais próxima; ignora cheia; empate estável.

## Docs
- CLAUDE.md, seção Drift: parágrafo da rede.
- Roteiro de aceite, seção 7: craftar a rede, colocar na borda da ilha, detritos que passam a até 3 blocos vão para ela, abrir e tirar itens, visitante não abre, rede cheia deixa passar, quebrar e recolocar mantém os itens, limite de 4.
