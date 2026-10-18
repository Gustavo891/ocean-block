# OceanBlock — Deriva: mergulhar sob obstáculos e sumir ao travar (ajuste)

Pedido do usuário: quando o detrito não consegue passar, ele afunda para tentar passar por baixo, e não trava em blocos à frente. Se travar mesmo assim, some depois de um tempo.

## Requisitos (valores padrão no `drift.yml`)

- Seção nova `obstaculo`:
  - `profundidade-maxima: 3` (quantos blocos abaixo da linha do raft o detrito pode descer)
  - `velocidade-mergulho: 0.08` (blocos por tick, para descer e para subir)
- `spawn.tempo-preso-segundos`: padrão **20** (antes 60).
- `obstaculo.tempo-sumir-ticks: 20` (duração da animação de sumir).

## Comportamento

Seja `raftY = floor(altura)` (62) e `depth` a profundidade atual do detrito em blocos inteiros (0 = superfície, 1 = um bloco abaixo, ...). Para a próxima posição renderizada (x, z) na trilha:

1. **Escolher a profundidade-alvo:** a menor profundidade `d` em `0..profundidade-maxima` tal que o bloco em `(x, raftY - d, z)` seja passável (`!isSolid`), **e** todos os blocos entre a profundidade atual e `d` na coluna atual também sejam passáveis (não atravessa bloco sólido na vertical). Preferir subir: se a superfície estiver livre na próxima posição, o alvo é 0.
2. **Se existe alvo:** o detrito avança horizontalmente somente quando a sua altura atual já está na faixa do alvo (diferença ≤ 0.05); enquanto isso desce ou sobe na velocidade `velocidade-mergulho`, sem avançar. Ao chegar, avança normalmente.
3. **Se não existe alvo:** o detrito fica parado (preso); o relógio de preso começa. Depois de `tempo-preso-segundos`, entra em **sumiço**: por `tempo-sumir-ticks` ticks afunda 0.5 bloco e a escala vai a 0 (linear), então a entidade é removida.
4. Ao voltar a avançar, o relógio de preso zera.

Altura renderizada: na superfície (depth 0) = `altura − afundamento + balanço` como hoje; submerso = `raftY − depth + 0.5` com balanço reduzido a 30% e sem sinuosidade vertical extra. A transição entre níveis é a interpolação da posição atual em direção ao alvo, na velocidade de mergulho.

Partículas: na superfície, rastro como hoje; submerso, `Particle.BUBBLE` (2) em vez de respingo, mesmo ritmo; sem rastro durante o sumiço.

Coleta: toque e gancho continuam funcionando submerso (usam a posição da entidade). Durante o sumiço o detrito não pode ser coletado nem puxado.

## Unidades puras (Kotest, sem Bukkit)

`DiveRules` (pacote `debris`):
- `targetDepth(currentDepth: Int, maxDepth: Int, aheadPassable: (Int) -> Boolean, columnPassable: (Int) -> Boolean): Int?` — regra do item 1; `null` = sem passagem.
- `stepHeight(currentY: Double, targetY: Double, speedPerStep: Double): Double` — aproxima sem ultrapassar.
- `atTarget(currentY: Double, targetY: Double): Boolean` — tolerância 0.05.
- `fadeProgress(ticksIntoFade: Int, fadeTicks: Int): Double` em `[0, 1]`.

Testes:
- superfície livre → 0;
- superfície bloqueada e 1 abaixo livre → 1;
- bloqueado em 0..3 → null;
- bloqueado em 0 e 1, livre em 2, mas coluna atual sólida em 1 → não atravessa: null (ou o menor alcançável);
- submerso em 2 com superfície à frente livre e coluna livre → 0 (sobe);
- `stepHeight` não ultrapassa o alvo (subindo e descendo);
- `fadeProgress` 0 no início, 1 no fim, limitado.

Parser: chaves novas com padrões e avisos; teste do YAML padrão cobre `obstaculo.*` e `tempo-preso-segundos 20`.

## Fora do escopo
- Desviar para os lados.
