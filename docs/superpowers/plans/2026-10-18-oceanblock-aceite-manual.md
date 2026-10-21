# OceanBlock — Aceite manual (fork base + ilha)

Verificações que precisam de jogador no jogo; os agentes não conseguiram rodar. Servidor: `server/`, porta 25566. Suba com `java -jar paper.jar --nogui` direto (o `start.bat` reinicia em loop).

## 1. Preparação
- [ ] Conferir que `server/plugins/ocean-block/templates/island.schem` existe (vem versionado no repositório).

## 2. Mundo
- [ ] Voar ~2000 blocos em duas direções: só água, nenhuma terra.
- [ ] Mergulhar em (0, 0): fundo de areia/cascalho por volta de Y 41–44.
- [ ] Em X=5000, Z=5000: construir na superfície e cavar o fundo, selecionar 100×100 da altura mínima à máxima e `//regen`: água e fundo voltam.

## 3. Ilha
- [ ] Jogador novo entra (ou `/ilha`, ou o portal do spawn): a ilha do schematic aparece no centro do lote e o jogador é levado para cima dela só depois da colagem, no centro, com os pés em Y 64 (chão em Y 63), sem cair na água nem ficar preso em bloco.
- [ ] Morrer e renascer: volta em cima da ilha no centro, não dentro do tronco da árvore.
- [ ] Renomear `island.schem` temporariamente e criar ilha com outro jogador novo: 3×3 de grama sobre terra (grama em Y 63, terra em Y 62), jogador em pé na grama + log `[ilha]` severe. Voltar o nome.
- [ ] Quebrar o bloco central: dropa normalmente, não regenera, nenhum mob nasce.
- [ ] Não existe mais HUD "Ilha • NxN" no topo da tela.
- [ ] Scoreboard com título `OCEAN BLOCK` e nenhum `%placeholder%` cru.
- [ ] `/ilha`, `/island` e `/is` abrem o comando; `/raft`, `/balsa`, `/is phases` e `/is setphase` não existem mais.
- [ ] Mensagens, menus e scoreboard dizem "ilha" com concordância certa ("sua ilha", "Sua ilha:").

## 4. Construção e membros
- [ ] Construir para fora a partir da borda da ilha com terra, pedregulho e tábua; fora da borda é bloqueado.
- [ ] `/ilha member <outro>`: o outro constrói; sem membro/trust, não.
- [ ] `/ilha sethome` fora da própria ilha é recusado; dentro funciona e sobrevive a reinício.

## 5. Reset
- [ ] Construir para fora, cavar o fundo, soltar um item, spawnar um mob, definir home em outro lugar; `/ilha reset terrain <dono> -confirm`: mar e fundo restaurados, ilha recolada, item e mob removidos, jogador volta em cima da ilha (pés em Y 64) e a home volta para o centro.
- [ ] Bloco custom colocado no lote antes do reset deixa de ser rastreado depois.

## 6. Geral
- [ ] Sair e entrar de novo: volta para a ilha.
- [ ] `/locate structure minecraft:shipwreck` não encontra nada.
- [ ] `/oceanblock reload`, `/shop`, `/sell`, `/tops` respondem.

## 7. Deriva e gancho
Pré-requisito: ilha criada e o servidor com o plugin novo. A config fica em `server/plugins/ocean-block/drift.yml`.

### Config e itens
- [ ] Na primeira subida, `server/plugins/ocean-block/drift.yml` foi criado com os 4 tipos (`tabua`, `folhas`, `sucata`, `barril`).
- [ ] Jogador novo (nunca entrou antes) recebe o gancho ao entrar; jogador antigo não recebe de novo.
- [ ] `/citem give gancho` entrega o gancho.
- [ ] A bancada não faz mais o gancho (`..P / .PS / P.S` com tábua e linha não dá resultado); ele só sai pela tela de receitas.

### Detritos
- [ ] Em pé na ilha, detritos aparecem a montante e passam boiando ao lado da ilha, rente à água (Y ~62,6), movendo suave, na direção da corrente (leste por padrão). Passam dos dois lados, entre `largura-minima` (8) e `largura` (13) blocos do centro, ou seja, 2 a 7 blocos fora da costa (a parte sólida da ilha vai até ~6 blocos do centro); nenhum vem pelo meio nem bate de frente na ilha.
- [ ] Em ~2 minutos já passam tábuas suficientes para crescer a ilha; nunca mais de 8 detritos ao mesmo tempo no lote.
- [ ] Detrito que passa do fim da trilha (40 blocos) some sem sobrar entidade.
- [ ] Detrito que encosta numa borda construída na trilha com 1 bloco de profundidade (tábua, laje, cerca na altura da água) afunda devagar, passa por baixo sem atravessar blocos, volta à superfície do outro lado e segue a corrente; submerso, solta bolhas em vez de respingo e balança menos.
- [ ] Parede sólida da superfície até o fundo (ou mais funda que 3 blocos): o detrito para encostado e, após ~20 s, afunda meio bloco e encolhe até sumir (~1 s), sem sobrar entidade.
- [ ] Detrito preso na parede pode ser pego por toque e puxado pelo gancho; submerso (mergulhando sob uma borda) também. Só o detrito sumindo não é coletado.
- [ ] Alga ou grama-marinha que chega à superfície NÃO faz o detrito mergulhar; tábua, laje e cerca fazem.
- [ ] Tirar a água de uma área na frente da trilha com balde ou esponja: o detrito entra no buraco e cai (acelerando, sem andar para os lados). Se cair na água de baixo, volta a boiar e sobe até a superfície dela. Se cair num bloco, fica no chão, parado e sem girar.
- [ ] Tirar a água logo embaixo de um detrito boiando faz ele cair na hora.
- [ ] Mergulho não passa por ar: com um bolsão de ar sob uma borda construída, o detrito não mergulha por ele (cai ou fica preso).
- [ ] Detrito no chão ou preso pode ser pego nadando ou andando até ele e com o gancho; se ninguém pegar, some em ~20 s.
- [ ] Devolver a água sobre um detrito no chão faz ele voltar a boiar.
- [ ] Detrito que cai num buraco muito fundo (mais de 64 blocos abaixo da superfície do mar ou fora do mundo) é removido sem erro no log.
- [ ] Detritos nunca forçam o carregamento de chunks: com a distância de renderização baixa (ex.: `view-distance=2`), podem nascer menos detritos.

### Coleta
- [ ] Lançar a linha do gancho perto de um detrito e recolher: o detrito desliza até o jogador em ~0,5 s, com som, respingo e a notificação `+N item` (ícone do item) na lista à direita, abaixo da scoreboard.
- [ ] Nadar até um detrito (a menos de 1,5 bloco) coleta na hora; na beira da ilha também.
- [ ] Barril dá loot variado (pão, linha, alga, muda, bacalhau etc.) e nunca vem vazio (pior caso: 1 linha).
- [ ] Com inventário cheio, o que não cabe cai aos pés do jogador.
- [ ] Pesca vanilla com o gancho continua dando peixe.
- [ ] Vara de pesca comum não puxa detrito.
- [ ] Visitante sem membro/trust perto do detrito não coleta (nem por toque, nem com gancho) e não recebe mensagem; membro e trust coletam.
- [ ] Espectador (modo espectador) perto de um detrito: anotar o que acontece (coleta, ignora ou erro no log). O comportamento ainda não foi decidido.

### Rede de detritos
- [ ] A bancada não faz mais a rede (`PSP / SSS / PSP` com tábua e linha não dá resultado); ela só sai pela tela de receitas. `/cblock give rede` também entrega.
- [ ] Colocar a rede na borda da ilha: detritos que passam a até 3 blocos dela (distância 3D até o centro do bloco) deslizam até a rede em ~0,5 s e somem com respingo no bloco, sem action bar para ninguém.
- [ ] Detrito mais perto do jogador (a menos de 1,5 bloco) vai para o jogador, não para a rede; o gancho não puxa um detrito que já está indo para a rede.
- [ ] Clique direito na rede (dono, membro ou trust) abre `Rede de Detritos` com 27 slots; clicar num item passa ele para o inventário, `Coletar tudo` passa tudo; não dá para colocar itens nem arrastar; o tear vanilla nunca abre (nem agachado).
- [ ] Com inventário cheio, clicar num item avisa `Seu inventário está cheio.` e o item fica na rede.
- [ ] Duas pessoas com a tela aberta ao mesmo tempo: o item que uma tirou não sai de novo para a outra (o clique da segunda só atualiza a tela).
- [ ] Visitante sem membro/trust clica na rede: nada abre e nenhuma mensagem aparece.
- [ ] Rede cheia (os 27 slots ocupados, mesmo com pilhas incompletas): os detritos passam direto por ela e seguem a corrente. Encher a rede durante um puxão faz o detrito sumir sem entregar e sem dropar nada no chão; o que não cabe de uma entrega é descartado.
- [ ] Quebrar a rede devolve o item com os itens guardados (`Itens guardados: N` na lore); recolocar mantém os itens. Quebrar durante um puxão faz o detrito sumir sem entregar.
- [ ] Limite de 4 redes por ilha: a 5ª recebe `Limite de 4 por ilha atingido.`
- [ ] Com duas redes no alcance, o detrito vai para a mais próxima que não está cheia.
- [ ] Rede em chunk não carregado não puxa nada e nada força o carregamento do chunk dela.

### Limpeza
- [ ] Sair do lote (ou desconectar) apaga os detritos do lote, sem ficar nenhum flutuando.
- [ ] `/ilha reset terrain <dono> -confirm` não deixa detrito fantasma (somem no tick seguinte).
- [ ] Reiniciar o servidor com detritos no mundo: depois de subir não sobra nenhum `ItemDisplay` de detrito.
- [ ] Sem erros `[deriva]` no log durante toda a sessão.

## 8. Receitas

### Preparação
- [ ] Depois de instalar o jar com a tela e reiniciar, o pack que o Textura monta na subida termina sem aviso `[ui:receitas]` no console e o pack novo chega aos jogadores (o hash muda); o slot do card já mostra a vara de pesca (Gancho) e o tear (Rede), sem precisar de `/textura gerar`.
- [ ] Reiniciar o servidor de novo: os ícones continuam aparecendo nos cards.
- [ ] Editar `receitas.json` com o servidor ligado e `/textura reload`: a mudança de altura ou ícone só aparece certa depois de `/textura gerar`.

### Abrir
- [ ] `/receitas`, `/craft` e `/receita` abrem a mesma tela, com título `Receitas`, tema de pedra e o rodapé `Os itens vão para o seu inventário.`
- [ ] Clicar no NPC "RECEITAS" do spawn abre a tela.
- [ ] Sem a textura aceita, aparece só o aviso `Textura necessária` do Textura em vez da tela: nada no chat e nenhum aviso `[receitas]` no console.
- [ ] Renomear `configs/ui/receitas.json`, `/textura reload` e rodar `/receitas`: chat `A tela de receitas não está disponível.` e um aviso `[receitas]` no console. Rodar `/receitas` de novo: o chat repete, o console não. Voltar o nome e `/textura reload`.

### Categorias e chips
- [ ] A coluna da esquerda lista `Ferramentas` (picareta) e `Coleta` (baú); a aberta fica destacada e o tooltip diz `✔ Aberta agora!`, a outra `Clique para ver!`.
- [ ] Clicar em `Coleta` troca o cabeçalho para `Coleta` com o ícone do baú e mostra só a Rede; voltar para `Ferramentas` mostra só o Gancho.
- [ ] Os chips do topo mostram quanto o jogador tem dos ingredientes da categoria aberta (ex.: `12 Tábuas`, `1 Linha`, singular com 1); pegar ou largar linha e reabrir atualiza o número.
- [ ] Fechar a tela em `Coleta` e abrir de novo: continua em `Coleta`. Sair do servidor e entrar de novo: volta para `Ferramentas`.

### Cards
- [ ] Cada card mostra o item no slot (tooltip com nome e descrição), o nome, a linha de custo (`3 Tábuas ∙ 2 Linhas` no Gancho, `4 Tábuas ∙ 5 Linhas` na Rede) e o botão `Craftar` com o carrinho.
- [ ] Sem material, o botão `Craftar` aparece desabilitado, não reage ao clique e o tooltip termina com `⌀ Falta: ...` listando só o que falta (ex.: `⌀ Falta: 1 Tábua ∙ 2 Linhas`).
- [ ] Com material, o tooltip termina com `✔ Pronto! Clique para craftar.`
- [ ] Com 6 receitas ou menos numa categoria, a linha de páginas (`«  1/1  »`) não aparece.

### Craftar
- [ ] Com 3 tábuas e 2 linhas, `Craftar` no Gancho: consome exatamente isso, entrega um gancho que funciona (puxa detrito), mostra `Você craftou 1x Gancho.` e a tela se redesenha com os chips e os botões atualizados.
- [ ] Com 4 tábuas e 5 linhas, `Craftar` na Rede: entrega a rede, que pode ser colocada e coleta detritos.
- [ ] Tábuas divididas em pilhas de madeiras diferentes (ex.: 1 de carvalho + 2 de abeto, em slots separados) contam juntas e são consumidas certo; o que sobra fica no inventário.
- [ ] Inventário cheio (sem slot livre) com pilhas maiores que o custo (ex.: 10 tábuas e 10 linhas, de modo que nenhum slot esvazia): o craft consome os ingredientes e o item cai aos pés do jogador.
- [ ] Craftar duas redes avança o desafio `CRAFT_REDE` do battle pass.
- [ ] Com a tela aberta e exatamente 3 tábuas e 2 linhas (Gancho pronto), rodar no console `/clear <jogador> string 2` e clicar `Craftar` (botão ainda ativo): nada é consumido, aparece `⌀ Falta: ...` em vermelho e a tela se redesenha com o botão desabilitado.
- [ ] Com material para exatamente 2 ganchos (6 tábuas e 4 linhas), clicar `Craftar` várias vezes bem rápido: sai um gancho por redesenho e nunca mais de 2 no total; sobram 0 tábuas e 0 linhas e nenhum material fica negativo ou duplicado.
- [ ] No tooltip do botão, `✔ Pronto! Clique para craftar.` aparece em verde, `⌀ Falta: ...` em vermelho e as linhas `∙ ...` do custo em destaque.

### Bancada
- [ ] A bancada de trabalho não faz mais o gancho nem a rede com tábua e linha em nenhum formato.
- [ ] Sem erros `[receitas]` ou `[ui:receitas]` no log durante toda a sessão.

## 9. Notificações
Pré-requisito: resource pack aceito (o shader do pack é quem posiciona as notificações).
- [ ] Puxar detritos seguidos: cada item aparece numa linha à direita, abaixo da scoreboard, com ícone, `+N` em verde e o nome do item; coletar o mesmo item de novo enquanto a linha está na tela vira `+2`, `+3`... na mesma linha.
- [ ] Itens diferentes empilham para baixo, no máximo 6 linhas; a mais antiga sai quando chega a 7ª; cada linha some ~3 s depois da última coleta daquele item.
- [ ] Encher uma Rede de Detritos: aparece no topo esquerdo `Sua Rede de Detritos ficou cheia!` com o ícone do tear, por ~5 s, para o dono e membros online; não repete enquanto a rede continuar cheia; tirar itens e encher de novo avisa outra vez.
- [ ] As duas regiões aparecem ao mesmo tempo sem se sobrepor.
- [ ] Trocar a escala de GUI (1, 2, 3, 4, Auto) e redimensionar a janela: as notificações continuam encostadas nos cantos certos.
- [ ] Com a barra do battle pass ativa, as notificações continuam no lugar certo e nenhuma barra fica pulando.
- [ ] Textos normais (chat, títulos, nomes) não mudam de lugar nem de cor.

## 10. Peneira, fundição e pesquisa

### Preparação
- [ ] `server/plugins/ocean-block/drift.yml` atualizado com os torrões (`torrao_areia`, `torrao_cascalho`, `torrao_lodo`) e o barril com areia e cascalho; `peneira.yml` copiado na primeira subida.
- [ ] Depois de reiniciar, o pack sobe sem aviso `[ui:pesquisa]` e os cards da Mesa mostram os ícones (tear, composteira, alto-forno, mesa de cartografia, funil).

### Peneira
- [ ] Puxar torrões da corrente rende areia, cascalho e terra.
- [ ] Craftar a Peneira no `/receitas` (categoria `Processamento`): 4 Tábuas, 2 Linhas, 2 Gravetos. Colocar: aparece uma composteira.
- [ ] Clique direito com cascalho na mão: carrega 1 cascalho; cada clique seguinte avança uma etapa (a composteira baixa um nível) até a 6ª, que entrega as saídas no inventário com notificação na região `coleta` (`+1 Pedaço de ferro` etc.). Cliques rápidos demais não pulam etapas.
- [ ] Shift + clique direito com cascalho: carrega e já dá uma etapa, sem abrir nada vanilla.
- [ ] A composteira nunca chega no nível de farinha de osso; um funil embaixo não puxa nada dela.
- [ ] Sair do servidor no meio de uma peneirada e voltar: a peneira continua de onde parou.

### Pedaços e fornalha vanilla
- [ ] Pedaço de ferro na fornalha, no alto-forno e no defumador vanilla: não derrete (a fornalha não começa).
- [ ] Pedaço na bancada de trabalho (ex.: 9 pedaços para bloco de ferro bruto): nenhum resultado aparece.

### Fundição
- [ ] Craftar a Fundição (8 Argilas + 4 Tábuas) e colocar: aparece um alto-forno. Clique direito não abre o inventário vanilla.
- [ ] Clique direito com 8 pedaços de ferro carrega a entrada; com tábua carrega combustível. A fundição acende e, em ~20 s, sai 1 lingote de ferro a cada 4 pedaços; o item em cima mostra o que está fundindo.
- [ ] Shift + clique direito retira todos os lingotes prontos.
- [ ] Minério bruto vanilla (ex.: `RAW_IRON` sem ser pedaço) funde 1 para 1.
- [ ] Quebrar a fundição carregada (entrada, combustível e saída) e colocar de novo: o item quebrado guarda tudo e a fundição volta igual.
- [ ] Funil vanilla acima, ao lado ou abaixo da Fundição básica ou de uma Peneira: não entra nem sai nada (o inventário vanilla do alto-forno e da composteira fica intocado).

### Mesa de Pesquisa
- [ ] Ilha nova: no `/receitas`, a Rede de Detritos aparece com `⌀ Pesquise na Mesa: Rede de Detritos` e não crafta; o Gancho crafta normal.
- [ ] Craftar a Mesa de Pesquisa (6 Tábuas, 3 Linhas, 2 Pepitas de ferro) e colocar: clique direito (e shift + clique) abre a tela `pesquisa`, nunca a mesa de cartografia vanilla.
- [ ] A tela mostra categorias `Coleta`, `Fundição`, `Raft`, a linha de vagas (`1 vaga`), a fila e os cards com custo, tempo e estado.
- [ ] Dois membros entregam a Rede aos poucos (cada um uma parte das 10 Tábuas e 6 Linhas): só sai do inventário o que falta; ao completar, a pesquisa inicia (20 s).
- [ ] Ao concluir, aparece na região `avisos` a notificação para os membros online; a Rede passa a craftar no `/receitas`.
- [ ] Com 1 vaga, completar o custo de duas pesquisas: a segunda entra na fila e começa sozinha quando a primeira termina.
- [ ] Concluir `Bancada auxiliar`: a linha passa a mostrar 2 vagas e duas pesquisas rodam juntas.
- [ ] Desligar o servidor no meio de uma pesquisa, esperar mais que o tempo restante, ligar e abrir a Mesa: a pesquisa já está concluída (e a da fila, se cabia no tempo, também).
- [ ] Visitante sem permissão: shift + clique na Mesa de outra ilha não abre nada.

### Peneira automática
- [ ] Pesquisar e craftar a Peneira automática. Baú com cascalho em cima, funil embaixo levando para um baú: a peneira puxa do baú de cima e manda as saídas para o funil sozinha, um passo por segundo.
- [ ] Com o baú de baixo cheio, a peneira espera na última etapa sem perder itens.
- [ ] Sem erros `[peneira]` ou `[pesquisa]` no log durante toda a sessão.
