# OceanBlock — Aceite manual (fork base + raft)

Verificações que precisam de jogador no jogo; os agentes não conseguiram rodar. Servidor: `server/`, porta 25566. Suba com `java -jar paper.jar --nogui` direto (o `start.bat` reinicia em loop).

## 1. Preparação
- [ ] Salvar o schematic do raft: em qualquer ponto do oceano, 3×3 de `OAK_PLANKS` em Y 62 e um baú em cima da tábua do canto (+1, 63, +1). `//pos1` (-1, 62, -1) e `//pos2` (+1, 63, +1) relativos ao centro; em pé em cima da tábua central (pés em Y 63), `//copy` e `//schem save raft`. Copiar `plugins/FastAsyncWorldEdit/schematics/raft.schem` para `server/plugins/ocean-block/templates/raft.schem`.

## 2. Mundo
- [ ] Voar ~2000 blocos em duas direções: só água, nenhuma terra.
- [ ] Mergulhar em (0, 0): fundo de areia/cascalho por volta de Y 41–44.
- [ ] Em X=5000, Z=5000: construir na superfície e cavar o fundo, selecionar 100×100 da altura mínima à máxima e `//regen`: água e fundo voltam.

## 3. Raft
- [ ] Jogador novo entra (ou `/raft`): aparece em cima do raft, rente à água, com baú (ou o 3×3 de fallback sem schematic).
- [ ] Renomear `raft.schem` temporariamente e criar raft com outro jogador novo: 3×3 de tábuas + log `[raft]` severe. Voltar o nome.
- [ ] Quebrar a tábua central: dropa `OAK_PLANKS`, não regenera, nenhum mob nasce.
- [ ] HUD mostra `Raft • 30x30`; depois de um upgrade de borda mostra `Raft • 50x50` na hora.
- [ ] Scoreboard com título `OCEAN BLOCK` e nenhum `%placeholder%` cru.
- [ ] `/raft`, `/balsa`, `/ilha`, `/island` e `/is` abrem o comando; `/is phases` e `/is setphase` não existem mais.
- [ ] Mensagens e menus dizem "raft" com concordância certa ("seu raft").

## 4. Construção e membros
- [ ] Construir para fora a partir da borda do raft com terra, pedregulho e tábua; fora da borda é bloqueado.
- [ ] `/raft member <outro>`: o outro constrói; sem membro/trust, não.
- [ ] `/raft sethome` fora do próprio raft é recusado; dentro funciona e sobrevive a reinício.

## 5. Reset
- [ ] Construir para fora, cavar o fundo, soltar um item, spawnar um mob, definir home em outro lugar; `/raft reset terrain <dono> -confirm`: mar e fundo restaurados, raft recolado, item e mob removidos, jogador volta em cima do raft e a home volta para o centro.
- [ ] Bloco custom colocado no lote antes do reset deixa de ser rastreado depois.

## 6. Geral
- [ ] Sair e entrar de novo: volta para o raft.
- [ ] `/locate structure minecraft:shipwreck` não encontra nada.
- [ ] `/oceanblock reload`, `/shop`, `/sell`, `/tops` respondem.
