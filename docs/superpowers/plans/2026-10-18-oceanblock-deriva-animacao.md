# OceanBlock — Deriva: animação de boiar (ajuste)

Feedback do usuário: o detrito anda reto num mar parado e fica esquisito. Ajuste aprovado em chat: balanço vertical, inclinação e giro, caminho sinuoso com velocidade variada, rastro na água, altura acima da linha d'água, e riscos de corrente na água com interruptor.

## Requisitos (valores padrão, todos no `drift.yml`)

- `corrente.altura`: **62.85** (antes 62.6).
- `corrente.riscos`: `true`; `corrente.riscos-por-segundo`: `6` por lote ativo.
- Seção nova `animacao`:
  - `balanco: 0.06` (amplitude vertical em blocos), `periodo-balanco-ticks: 50`
  - `inclinacao-graus: 8.0` (pitch e roll oscilando ±), `giro-graus-por-segundo: 10.0` (yaw contínuo, sentido sorteado por detrito)
  - `sinuosidade: 0.4` (deslocamento lateral ± em blocos), `periodo-sinuosidade-ticks: 120`
  - `variacao-velocidade: 0.2` (fator de velocidade por detrito uniforme em `[1 - v, 1 + v]`)
  - `rastro: true`
- Por tipo, chave opcional `afundamento` (blocos abaixo de `altura`, padrão `0.0`); `barril: 0.12`.

## Comportamento

- Cada detrito sorteia no spawn: fase do balanço, fase da sinuosidade, fase da inclinação, sentido do giro, fator de velocidade.
- `Debris.point` continua sendo o ponto base na trilha (reta). A posição renderizada = base + perpendicular × sinuosidade(idade) e `y = altura − afundamento + balanço(idade)`.
- A checagem de bloco sólido (preso) e o fim da trilha usam a posição renderizada (x, z); a altura do bloco checado continua `floor(altura)`.
- Rotação via `ItemDisplay.transformation` (`leftRotation` a partir de yaw/pitch/roll), com `interpolationDelay = 0` e `interpolationDuration = 2` a cada passo, para suavizar junto com o `teleportDuration = 2`. A escala do tipo é preservada.
- Rastro (quando `rastro`): a cada 10 ticks de vida, `Particle.SPLASH` (3, espalhamento 0.1) e `Particle.BUBBLE_POP` (1) logo atrás do detrito (0.4 bloco contra a corrente), em `y = altura + 0.05`. Não emite enquanto preso ou sendo puxado.
- Riscos de corrente (quando `riscos`): por lote ativo, distribuídos ao longo do segundo (a cada tick de movimento emite a fração correspondente), `Particle.FISHING` com `count = 0` e vetor na direção da corrente (velocidade 0.05), em pontos aleatórios da trilha (ao longo do comprimento, ± largura lateral) em `y = altura + 0.1`. Só em chunks carregados; nunca carrega chunk.
- Puxão e coleta: inalterados.

## Unidades puras (Kotest)

`FloatMotion` (pacote `debris`), sem Bukkit:
- `bob(ageTicks, phase, amplitude, periodTicks): Double` — `amplitude * sin(2π·age/period + phase)`.
- `meander(...)` — mesmo formato com a amplitude da sinuosidade.
- `tilt(...)` em graus, mesmo formato.
- `yaw(ageTicks, degreesPerSecond, direction: Int): Double` — `direction * degreesPerSecond * age / 20`.
- `speedFactor(random, variation): Double` em `[1 - variation, 1 + variation]`.
- `wakeDue(ageTicks): Boolean` a cada 10 ticks.

Testes: valor em idade 0 com fase 0 é 0; em um quarto do período é a amplitude; nunca passa da amplitude em 1000 amostras; yaw cresce linear e inverte com direção −1; speedFactor sempre no intervalo (seed fixa); wakeDue verdadeiro em 0, 10, 20 e falso em 5.

Parser: chaves novas com padrões e os mesmos avisos de valor inválido; teste do YAML padrão cobre os novos valores (`altura 62.85`, `animacao.*`, `riscos`, `afundamento` do barril 0.12, demais tipos 0.0).

## Fora do escopo
- Mexer na água real, ondas de bloco, sons novos.
