# Recompensas por tempo online — design

Data: 2026-10-26. Pedido do usuário: "Adicione sistema de recompensa por tempo online".

## Intenção

Dar ao jogador objetivos de longo prazo por ficar no servidor: marcos de tempo total jogado que liberam prêmios resgatáveis numa tela própria.

## Decisões do usuário

- Formato: marcos de tempo acumulado (únicos, não repetem).
- Paga Coins, Pérolas, XP de nível e itens/comandos.
- Resgate manual numa tela; nada cai sozinho.
- Tempo AFK conta: usa o mesmo tempo do `%playtime_total%` (`PlayerPlaytime.getElapsedSeconds()`).
- Aviso: notificação Textura na região `avisos` + som; lembrete no login quando há pendentes.
- Comando próprio `/recompensas` (aliases `/recompensa`, `/tempo`); nada no `/ilha`.
- Escada padrão de 10 marcos até 100 h.

## Escada padrão (`server/plugins/ocean-block/recompensas.yml`)

| id | minutos | coins | xp | pérolas | itens |
|---|---|---|---|---|---|
| 30m | 30 | 2.000 | 100 | | |
| 1h | 60 | 5.000 | 200 | | |
| 2h | 120 | 10.000 | 400 | | |
| 4h | 240 | 20.000 | 700 | 1 | |
| 8h | 480 | 35.000 | 1.000 | 2 | 16 IRON_INGOT |
| 12h | 720 | 50.000 | 1.500 | 3 | 8 GOLD_INGOT |
| 24h | 1440 | 100.000 | 2.500 | 5 | 4 DIAMOND |
| 48h | 2880 | 150.000 | 3.500 | 10 | 8 DIAMOND |
| 72h | 4320 | 200.000 | 4.000 | 15 | 4 IRON_BLOCK |
| 100h | 6000 | 300.000 | 5.000 | 25 | 2 DIAMOND_BLOCK |

Formato por marco: `marcos.<id>.minutos`, `coins`, `perolas`, `xp`, `icone` (material, padrão `clock`), `itens` (lista `MATERIAL:quantidade`), `comandos` (lista, `%player%` vira o nome). O arquivo é copiado do bundle quando falta e lido uma vez no `init`. O parser puro registra valores ruins sob `[recompensas]` e pula o marco (minutos ≤ 0, material inválido, id repetido de minutos). Sem marcos válidos o sistema fica sem marcos e loga `severe`, sem derrubar o plugin.

## Regras

- Situação de um marco para `segundos` jogados e o conjunto `resgatados`: `Resgatado`, `Disponivel` (minutos × 60 ≤ segundos) ou `Bloqueado(faltam)`.
- Marcos recém-atingidos entre duas leituras: os que estão em `(antes, agora]` e não foram resgatados.
- Resgate: marca o id como resgatado **antes** de pagar; um id já resgatado ou ainda bloqueado não paga nada. `Resgatar tudo` resgata todos os disponíveis em ordem.
- Pagamento: `Currencies.deposit(uuid, "coins" | "perolas", valor)`, XP por `PlayerLevels.award`; em criativo ou espectador, onde `award` não paga, por `PlayerLevels.setXp(xp atual + valor)` (decisão do builder, para não perder o XP de um marco resgatado), itens e comandos por `RewardGranter` (sobra cai no chão).
- Duração em texto (pura): `30 min`, `1 h`, `1 h 30 min`, `100 h`; tempo faltando arredondado para minuto, mínimo `1 min`.

## Runtime

- Módulo novo `system/playtime-rewards` (pacote `ocean_block.system.playtime_rewards`), `PlaytimeRewardsSystem.init(plugin, paperCommandManager)` depois de `MissionsSystem`; `onDisable` no `OceanBlockPlugin.onDisable()`.
- Dados: `PlaytimeRewards(uuid, claimed: List<String>)`, coleção `playtime_rewards`, índice único `uuid`, manager com dirty-set (flush de 30 s, limpa só após sucesso).
- Tarefa a cada 20 s no main thread: para cada online, compara o último tempo lido com o atual; cada marco recém-atingido gera a notificação `Recompensa de <nome> liberada! Use /recompensas` (região `avisos`, ícone `item:clock`, 100 ticks, chave `tempo`) e o som `BLOCK_NOTE_BLOCK_BELL`. No join, se há pendentes: `Você tem N recompensa(s) por tempo para resgatar! Use /recompensas`.

## Tela `/recompensas`

Dialog Textura `configs/ui/recompensas.json`, tema `mare`: cabeçalho `Recompensas por tempo` com `Jogado: X` à direita; lista `marcos` com `perPage` 3 (com 4 a tela passava da altura visível em GUI 4 a 1080p) (ícone, nome, prêmios, situação em texto ou barra de progresso até o marco, botão `Resgatar` ativo só quando disponível); `Resgatar tudo` (desligado sem pendentes) e setas de página. Abre na página do primeiro marco não resgatado. Fallback em chat com `[RESGATAR]` (`/recompensas resgatar <id>`). Admin `oneblock.admin`: `/recompensas resetar <jogador>`.

## Fora do escopo

Detecção de AFK, botão no `/ilha`, marcos repetitivos ou diários, chaves de caixa (não existe plugin de caixas; `comandos` cobre isso quando existir).

## Testes

Kotest nas regras puras (situação, recém-atingidos, resgate, duração, parser). O lado Bukkit fica no manual de aceite.
