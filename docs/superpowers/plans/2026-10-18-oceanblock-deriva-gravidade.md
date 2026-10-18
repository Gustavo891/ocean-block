# OceanBlock — Deriva: gravidade e detecção de água (ajuste)

Pedido do usuário: se a água sai do lugar, o detrito continua andando como se voasse. É para ter gravidade.

## Regras

**Água** num bloco = `type == WATER` ou `BUBBLE_COLUMN`, `KELP`, `KELP_PLANT`, `SEAGRASS`, `TALL_SEAGRASS`, ou `blockData is Waterlogged && isWaterlogged`. **Sólido** = `isSolid` (como hoje). **Ar** = nem água nem sólido.

Para cada coluna (x, z) existe a **superfície da água**: o bloco de água mais alto em `raftY` (`floor(altura)`) ou abaixo, buscando até `obstaculo.profundidade-maxima + 4` blocos, contanto que não haja bloco sólido entre `raftY` e ele. Se não houver, a coluna é **seca**.

Estados do detrito (substituem/estendem os do mergulho):
- **Boiando**: segue como hoje, mas a altura de flutuação é a superfície da água da coluna atual (`superfície + (altura − raftY) − afundamento + balanço`), não `altura` fixo. Com o mar normal é igual ao comportamento atual.
- **Mergulhando / submerso**: como no ajuste de mergulho, mas um nível só é passável se for **água** (não basta não ser sólido). Mergulho nunca atravessa ar.
- **Caindo**: quando a coluna atual não tem água na altura do detrito e o bloco logo abaixo dele não é sólido. Cai com gravidade de item: `vy -= 0.04` por tick, `vy *= 0.98`, limite `-1.0` bloco/tick, sem avanço horizontal. Ao entrar num bloco de água volta a **boiar** (sobe até a superfície daquela coluna na velocidade de mergulho). Ao encostar num bloco sólido embaixo fica **no chão** apoiado no topo do bloco (`y = topo + 0.25·escala`), sem balanço nem giro.
- **No chão**: parado. Se a coluna voltar a ter água acima dele, volta a boiar. Conta como preso para o tempo de sumir.
- **Preso** (sem passagem no mergulho) e **no chão**: **podem** ser coletados por toque e puxados pelo gancho. Corrige a regressão do ajuste de mergulho, em que preso deixou de ser coletável.
- **Sumindo**: como hoje (afunda 0.5 e encolhe em `tempo-sumir-ticks`); não coletável.

Avançar na trilha: o detrito só avança horizontalmente quando está boiando ou submerso no alvo. Se a próxima coluna for seca e o bloco da superfície nela não for sólido, ele avança para ela e passa a cair. Se for sólido, vale a regra do mergulho.

Altura mínima de segurança: se cair abaixo de `world.minHeight` ou mais de 64 blocos abaixo de `raftY`, é removido.

Nunca carregar chunk: toda leitura de bloco atrás do `isLoaded` existente.

## Unidades puras (Kotest)

`GravityRules` (sem Bukkit):
- `fall(vy: Double, ticks: Int): Double` — aplica `vy -= 0.04; vy *= 0.98` por tick, limite `-1.0`.
- `surfaceDepth(maxScan: Int, isWater: (Int) -> Boolean, isSolid: (Int) -> Boolean): Int?` — profundidade (0 = `raftY`) do bloco de água mais alto sem sólido acima dele; `null` se seca.
- `state(...)` ou funções equivalentes para decidir boiando/caindo/no chão a partir de lambdas.

Testes: queda acelera e respeita o limite; coluna com água em 0 → 0; água só em 2 com ar em 0–1 → 2; sólido em 1 e água em 2 → `null` (não atravessa sólido); coluna só de ar → `null`; decisão "cai" quando sem água e sem sólido abaixo; "no chão" quando sólido logo abaixo; "boia" ao entrar na água.

`DiveRules`: os testes existentes continuam valendo com a passabilidade injetada; a injeção em produção passa a usar "é água".

## Aceite (seção 7 do roteiro)
- Tirar a água de uma área com balde/esponja na frente da trilha: o detrito cai; cai na água de baixo e volta a boiar; cai em bloco e fica no chão.
- Detrito no chão e preso pode ser pego nadando/andando até ele e com o gancho; some em ~20 s se ninguém pegar.
