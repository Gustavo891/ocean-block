# OceanBlock — Aceite manual (fork base + ilha)

Verificações que precisam de jogador no jogo; os agentes não conseguiram rodar. Servidor: `server/`, porta 25566. Suba com `java -jar paper.jar --nogui` direto (o `start.bat` reinicia em loop).

## 1. Preparação
- [ ] Conferir que `server/plugins/ocean-block/templates/island.schem` existe (vem versionado no repositório).

## 2. Mundo
- [ ] Voar ~2000 blocos em duas direções: só água, nenhuma terra.
- [ ] Mergulhar em (0, 0): fundo de areia/cascalho por volta de Y 41–44.
- [ ] Em X=5000, Z=5000: construir na superfície e cavar o fundo, selecionar 100×100 da altura mínima à máxima e `//regen`: água e fundo voltam.

## 3. Ilha
- [ ] Jogador novo entra (ou `/ilha`): a ilha do schematic aparece no centro do lote e o jogador é levado para cima dela só depois da colagem, no centro, com os pés em Y 64 (chão em Y 63), sem cair na água nem ficar preso em bloco.
- [ ] Jogador que já tem ilha e entra fora do mundo das ilhas (ex.: ficou no mundo `spawn`) aparece na própria ilha; morrer sem cama também renasce na ilha. Membro sem ilha própria vai para a ilha onde é membro. `/spawn` não existe mais.
- [ ] Morrer e renascer: volta em cima da ilha no centro, não dentro do tronco da árvore.
- [ ] Renomear `island.schem` temporariamente e criar ilha com outro jogador novo: 3×3 de grama sobre terra (grama em Y 63, terra em Y 62), jogador em pé na grama + log `[ilha]` severe. Voltar o nome.
- [ ] Quebrar o bloco central: dropa normalmente, não regenera, nenhum mob nasce.
- [ ] Não existe mais HUD "Ilha • NxN" no topo da tela.
- [ ] Scoreboard com título `OCEAN BLOCK` e nenhum `%placeholder%` cru.
- [ ] `/ilha`, `/island` e `/is` abrem o comando; `/raft`, `/balsa`, `/is phases` e `/is setphase` não existem mais.
- [ ] Mensagens, menus e scoreboard dizem "ilha" com concordância certa ("sua ilha", "Sua ilha:").

## 4. Construção e membros
- [ ] Ilha nova (e ilha antiga sem upgrade) tem borda 50x50; com o upgrade de Borda passa a 70x70. `/ilha` mostra `Borda 50x50`.
- [ ] Construir para fora a partir da borda da ilha com terra, pedregulho e tábua; fora da borda é bloqueado.
- [ ] `/ilha convidar <outro>` (ou `/ilha member`) e o outro clica `[ACEITAR]`: o outro constrói; sem membro/trust, não.
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

### Menu
- [ ] Depois de instalar o jar e reiniciar, o pack sobe com o glyph `receitas_menu`; `/receitas`, `/craft` e `/receita` abrem um baú com o fundo desenhado e o título `Receitas`.
- [ ] A coluna da esquerda mostra `Ferramentas` (picareta de ferro), `Coleta` (baú) e `Estações` (composteira); a aberta brilha e diz `Aberta agora.`. Clicar em outra troca as receitas e volta para a primeira página.
- [ ] Fechar o menu em `Coleta` e abrir de novo: continua em `Coleta`. Sair do servidor e entrar de novo: volta para `Ferramentas`.
- [ ] Cada receita é o item resultado; a lore mostra a descrição, `Nível N`, um ingrediente por linha com `✔` verde ou `✖` vermelho e `(você tem N)`, e no fim `Clique para craftar!`, `Faltam itens.` ou `Requer nível N`.
- [ ] Com mais de 28 receitas numa categoria, as setas `<` `>` trocam de página.

### Craftar
- [ ] Com 3 tábuas e 2 linhas, clicar no Gancho: consome exatamente isso, entrega um gancho que funciona (puxa detrito), mostra `Você craftou 1x Gancho.` e o menu se redesenha com as contagens atualizadas.
- [ ] Com 4 tábuas e 5 linhas, clicar na Rede: entrega a rede, que pode ser colocada e coleta detritos.
- [ ] Tábuas divididas em pilhas de madeiras diferentes (ex.: 1 de carvalho + 2 de abeto, em slots separados) contam juntas e são consumidas certo; o que sobra fica no inventário.
- [ ] Inventário cheio (sem slot livre) com pilhas maiores que o custo (ex.: 10 tábuas e 10 linhas, de modo que nenhum slot esvazia): o craft consome os ingredientes e o item cai aos pés do jogador.
- [ ] Craftar duas redes avança o desafio `CRAFT_REDE` do battle pass.
- [ ] Com o menu aberto e exatamente 3 tábuas e 2 linhas, rodar no console `/clear <jogador> string 2` e clicar no Gancho: nada é consumido, o chat diz `Falta: 2 Linhas` em vermelho e a lore passa a marcar a linha com `✖`.
- [ ] Com material para exatamente 2 ganchos (6 tábuas e 4 linhas), clicar no Gancho várias vezes bem rápido: nunca sai mais de 2 no total; sobram 0 tábuas e 0 linhas e nenhum material fica negativo ou duplicado.

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

## 10. Gerador, fornalha, nível e missões

### Preparação
- [ ] `server/plugins/ocean-block/drift.yml` sem os torrões e sem areia/cascalho no barril; `gerador.yml` e `nivel.yml` copiados na primeira subida.
- [ ] Depois de reiniciar e rodar `/textura gerar`, o pack sobe com as telas `nivel` e `missoes` e o tema `mare` sem aviso; a coleção `island_research` sumiu do Mongo.

### Gerador
- [ ] Ilha nova (Gerador nível 0): um gerador de pedra (lava + água) forma pedregulho, pedra e às vezes minério de carvão, cobre e ferro.
- [ ] No `/ilha` → upgrades aparece `Gerador` com preço 25.000; comprar sobe para o nível 1 (`Gerador evoluído com sucesso!`) e o gerador passa a formar também lápis e redstone; no nível 2 entram ouro e esmeralda.
- [ ] Comprar Membros ou Borda não mexe no nível nem no preço do Gerador, e vice-versa.
- [ ] No nível 3 aparece minério de diamante de vez em quando; antes disso nunca. No nível 4 os minérios (diamante inclusive) saem com mais frequência.
- [ ] Um gerador fora de qualquer ilha (mundo do spawn) continua formando pedregulho vanilla.
- [ ] Editar `gerador.yml` com um bloco inválido e reiniciar: aviso `[gerador]` no console e o resto da tabela vale.

### Fornalha (sem Fundição)
- [ ] `/receitas` não tem mais Fundição, Fundição rápida nem Fundição com funil; `/cblock give fundicao` não existe.
- [ ] Minério bruto derrete na fornalha vanilla normalmente.
- [ ] Mesa de trabalho vanilla não crafta Alto-forno nem Sensor de luz solar (as receitas somem do livro de receitas).
- [ ] Gerador de energia e Fornalha elétrica pedem 1 Fornalha vanilla na receita, no lugar da Fundição.
- [ ] Missão 4 da trilha: "Derreta 10 minérios brutos" conta os lingotes de ferro, cobre e ouro tirados de uma fornalha vanilla (0/10 no HUD); a missão 11 é "Faça uma Fornalha elétrica".

### Nível (`/nivel`)
- [ ] `/pesquisa` não existe mais.
- [ ] Jogador novo: a sidebar mostra nível 1 e 0%. No `/receitas`, cada receita mostra `Nível N` abaixo da descrição; a Rede de Detritos termina com `Requer nível 3` e o clique diz `Você precisa do nível 3 para craftar isso.`; Gancho e Fundição craftam.
- [ ] Puxar um detrito com o Gancho e pegar outro pelo toque: cada um mostra `+N XP` na região de coleta (somando enquanto a linha vive) e a barra da sidebar anda.
- [ ] Detrito que cai na Rede de Detritos não dá XP; blocos que o Minerador quebra não dão XP.
- [ ] Quebrar à mão pedra/minério formado pelo gerador dá XP (minério raro vale mais); colocar um pedregulho no lugar e quebrar não dá XP.
- [ ] Fabricar uma receita no `/receitas` dá XP (`10 + 3 × nível da receita`).
- [ ] Ao subir de nível: título `NÍVEL N`, som de level-up e aviso `Nível N! Liberado: ...` na região `avisos` quando abriu receita.
- [ ] `/nivel dar <você> 100000` (admin): um título só com o nível final e o aviso lista todas as receitas abertas no caminho. `/nivel definir <você> 50`: barra cheia, `/nivel` mostra `Nível máximo` e XP novo não soma mais.
- [ ] Modo criativo não ganha XP de coleta nem de receita.
- [ ] `/nivel` (e `/level`, `/niveis`) abre o baú com o fundo `NIVEIS` (slots desenhados só no caminho, alinhados com as casas) na página do seu nível: os níveis 1–25 em caminho de serpente (casa verde = alcançado, dourada = atual, cinza = bloqueado, número = quantidade do item), nível que libera receita mostra o item da receita, lore com o estado e `Libera: ...`; setas trocam para 26–50; o frasco de XP mostra nível, XP e como ganhar XP; a barreira fecha.
- [ ] Editar `nivel.yml` com um valor negativo e reiniciar: aviso `[nivel]` no console e o padrão vale.
- [ ] Reiniciar o servidor: nível e XP continuam iguais.

### Missões (HUD e `/missoes`)
- [ ] Jogador novo: o HUD do topo mostra `Missão: Colete 10 detritos usando o gancho 0/10` sobre a placa preta translúcida; pegar detrito pelo toque não conta, pelo Gancho conta.
- [ ] Ao concluir: aviso `Missão concluída: ... +50 XP` com som, XP entra no nível e o HUD passa para `Faça um gerador de pedra`, que conclui quando o gerador forma bloco na ilha (ou quando você quebra um bloco do gerador).
- [ ] Seguir a trilha até a missão 5 (`Chegue ao nível 3`): se já estiver no nível 3, ela conclui na hora e o HUD pula para a 6. Missão 8 soma vendas (`$350/$1.000`); missão 9 conclui ao comprar o upgrade do gerador 1 (ou na hora, se a ilha já tem); missão 10 só conta minério de ferro.
- [ ] Sem ilha, as missões de gerador e upgrade ficam abertas sem erro.
- [ ] `/missoes pular <você>` (admin) até o fim da trilha: aparecem 3 diárias, o HUD mostra `Diária 1/3: ...`; ao terminar as três, `Diárias concluídas. Volte amanhã!`.
- [ ] Depois da meia-noite (Brasília), sem relogar: em até 1 minuto as diárias renovam.
- [ ] `/missoes` (e `/missao`) abre o diálogo sem barra de rolagem: card da trilha com o ícone da missão numa moldura, o texto, `Requisitos` com o contador, a frase do requisito, a barra e `Recompensas: +N XP de nível`; cada diária com ícone, texto, `+N XP`, requisito com o contador (ou `Concluída` em verde) e a barra; `Renovam em HH:MM`, ou antes do fim da trilha `Termine a trilha para liberar as diárias.`
- [ ] Sair e entrar: o HUD volta com a missão atual; reiniciar o servidor mantém o progresso. `/missoes resetar <você>` volta para a missão 1.

- [ ] Sem erros `[gerador]`, `[nivel]` ou `[missao]` no log durante toda a sessão.

## 11. Menu da ilha

### Dialog da ilha (`/ilha`)
- [ ] Com ilha, `/ilha` abre o dialog `Sua Ilha`: moldura cinza-escura com borda clara; no topo o busto pequeno do seu personagem (3/4, bem mais fino que a grade da GUI) à esquerda de uma coluna da mesma altura do busto: título `SUA ILHA` (ou `ILHA DE <DONO>`) com o X no canto, 6 cards pequenos (18×18) o do dono com coroa, depois um por membro (rosto da skin, ponto verde se online ou cinza se offline; o nome aparece só ao passar o mouse) e um card tracejado com `+` em cada vaga livre (tooltip `Convidar jogador`), e a linha de status alinhada com a base do busto; depois o painel `RESUMO DA ILHA` com gerador, configurações, visitas e moedas, os botões MELHORIAS, CONFIGURAÇÕES, NÍVEL, MISSÕES, RECEITAS, o verde IR PARA A ILHA e VISITAS ao lado.
- [ ] O título e os cabeçalhos usam a Minecraft Ten (blocada, grossa) e o resto uma caixa alta fina e espaçada, sem sombra; acentos (Ç, Õ, Í, Ú) aparecem certos; o X do canto é um ícone de traço fino.
- [ ] Clicar no card do dono ou de um membro abre o menu de membros. Membro offline também aparece com a própria skin e o ponto cinza.
- [ ] Como dono, clicar num card `+` abre o dialog `CONVIDAR JOGADOR` na mesma moldura, com o campo `Nome do jogador` embaixo e os botões `Convidar` e `Fechar` no rodapé. Campo vazio, nome inválido (`ab`, com espaço) ou jogador offline mostram o aviso em vermelho na moldura e mantêm o texto. Nome de um jogador online: aviso verde `CONVITE ENVIADO PARA <NOME>.`, o campo limpa e o convidado recebe o `[ACEITAR]` no chat. Membro, bloqueado ou ilha cheia: aviso vermelho e nada é enviado. O X volta para o dialog da ilha.
- [ ] Quem não é dono clica num card `+`: abre o menu de membros, sem convite.
- [ ] X, Esc ou o botão Fechar fecham o dialog.
- [ ] Trocar de skin (SkinsRestorer) e reabrir: o personagem usa a skin nova; skin slim mostra braços finos. Sem skin ou com a Mojang fora do ar, aparece o personagem padrão e o log tem `[ilha] skin de <nome> indisponível`.
- [ ] Membro parado na ilha do amigo: o dialog mostra a ilha do amigo. `/ilha menu` continua abrindo o menu de baú abaixo.

### Hub de baú (`/ilha menu`)
- [ ] `/ilha menu` abre o menu `Ilha de <dono>` (fundo `ilha_menu`, sem vidro): cabeça do dono no topo com membros `n/max`, borda, nível do gerador e visitas; embaixo Ir para a ilha, Membros, Melhorias, Configurações, Visitas, Receitas, Nível e Missões. Não existe mais botão de Fases.
- [ ] `/ilha ir` (ou `/ilha home`) e o botão Ir para a ilha teleportam para o spawn da ilha. O holograma do spawn mostra `(/ilha ir)`.
- [ ] Receitas, Nível e Missões fecham o menu e abrem `/receitas`, `/nivel` e `/missoes`.
- [ ] Membro parado na ilha do amigo: `/ilha` abre o menu da ilha do amigo (sem poder remover, convidar nem mudar nada). Visitante numa ilha alheia: `/ilha` abre o menu da própria ilha.
- [ ] `/ilha <dono>` leva à ilha de quem te adicionou; numa ilha em que você não é membro, a mensagem sugere `/ilha visitar`.

### Membros e convite
- [ ] Membros lista o dono (nome dourado) e os membros; o dono clica num membro para removê-lo (o removido recebe aviso se estiver online). O corante verde mostra `Vagas: n/3` (6 com o upgrade Membros).
- [ ] Convidar membro abre a lista de cabeças de jogadores online (sem você, o dono, membros e bloqueados), com páginas `<` `>` e voltar.
- [ ] Clicar num jogador: o convidado recebe no chat o convite com `[ACEITAR]`; clicar aceita e ele vira membro, o dono e os membros online são avisados. Esperar mais de 60 s antes de aceitar: `Você não tem convite válido`.
- [ ] Convite com a ilha cheia, para quem já é membro ou para um bloqueado: mensagem em vermelho e nada é enviado. Aceitar com a ilha já cheia também é recusado.
- [ ] Sair do servidor com convite pendente: ao voltar, o convite não vale mais.

### Visitas
- [ ] Visitas mostra Ilha pública/privada (corante verde/cinza), Bloquear jogador (barreira) e as cabeças dos bloqueados. O dono alterna pública/privada pelo clique; `/ilha visitar <dono>` só entra se pública.
- [ ] Bloquear jogador abre a lista de jogadores online; o escolhido é bloqueado e, se estiver na ilha, volta para a ilha dele. Clicar na cabeça de um bloqueado desbloqueia.
- [ ] `/ilha publica true|false` só funciona para o dono.

### Configurações e melhorias
- [ ] Configurações (fundo `ilha_config`) mostra as 7 opções numa linha e voltar leva ao hub.
- [ ] Melhorias: a lore mostra `Nível: a ➡ b` de cada upgrade (no máximo, `Máx` e sem preço). Comprar atualiza o menu na hora; Membros evoluído mostra `(3 -> 6 membros)`.

## 12. Máquinas e Picareta fundidora

### Preparação
- [ ] Depois de reiniciar, `server/plugins/Textura/data/block-allocations.yml` ganha `gerador_energia`, `painel_solar`, `fornalha_eletrica` e `minerador`; o pack sobe sem aviso de textura faltando.
- [ ] `/receitas` mostra 4 categorias na coluna da esquerda (a 4ª é `Máquinas`, observador) com moldura própria.
- [ ] Abaixo do nível, cada máquina e a Picareta fundidora dizem `Requer nível N` e não craftam: Picareta 12, Gerador de energia 18, Painel solar 20, Fornalha elétrica 22, Minerador 25.

### Blocos
- [ ] Cada máquina craftada aparece na mão com a textura própria; colocada, o bloco tem a textura (nunca o note block vanilla) e clicar não toca nota.
- [ ] Minerador sobre o gerador de pedra minerando por vários ciclos mantém a própria textura (não vira note block nem outra máquina); `paper-global.yml` tem `disable-noteblock-updates: true`.
- [ ] Quebrar uma máquina dropa um único item (sem note block extra) e ele guarda a carga: recolocar continua com energia, combustível, itens da fornalha ou do minerador.
- [ ] Limites: 4 Geradores, 8 Painéis, 4 Fornalhas, 4 Mineradores por ilha.
- [ ] Visitante não abre nem quebra máquinas de ilha alheia.

### Gerador de energia
- [ ] Clique direito com carvão na mão carrega o combustível; sem combustível na mão abre o menu `Gerador de energia` com o slot de combustível e o status (`Energia: x/10.000 E`, `Queimando`/`Parado`).
- [ ] No menu, clicar num combustível do próprio inventário coloca; clicar no slot de combustível retira. Combustível de outro tipo é recusado (som grave).
- [ ] Queimando, a energia sobe 40 por segundo e sai fumaça em cima; com 10.000 E ele para sem gastar combustível.

### Painel solar
- [ ] De dia com céu aberto, clique direito diz `gerando 6 E/s` e a energia sobe; à noite ou com um bloco em cima diz `parado`.

### Fornalha elétrica
- [ ] Menu com 4 colunas (entrada, progresso, saída) e status de energia à esquerda. Clicar num minério bruto/areia/pedregulho do inventário coloca na primeira entrada livre (ou na que já tem o mesmo item); item que não vai na fornalha é recusado.
- [ ] Com um gerador carregado a até 8 blocos, as 4 colunas derretem juntas, 1 item a cada 2,5 s por coluna, com faíscas em cima; a energia do gerador cai 20 E/s por coluna ativa.
- [ ] Sem fonte no raio (ou com o gerador a 9+ blocos), o progresso mostra `Sem energia` e nada anda.
- [ ] Saída com 64 ou com outro item mostra `Saída cheia` e para só aquela coluna. Clicar na entrada devolve, na saída coleta.
- [ ] Dois jogadores com o menu aberto retirando ao mesmo tempo: nada duplica.

### Minerador
- [ ] Colocado em cima da saída do gerador de pedra, com energia no raio, quebra o bloco de baixo a cada 2 s (partículas e som) e o gerador forma o próximo.
- [ ] Os drops vão para um baú encostado (em cima ou ao lado); sem baú, ficam nos 27 slots do menu `Minerador`, que tem `Coletar tudo` e status (`pronto para minerar`, `Sem energia`, `Armazenamento cheio`).
- [ ] Terra, grama, troncos, blocos custom ou bloco fora da ilha embaixo: não quebra (`nada para minerar`) e não gasta energia.
- [ ] Armazenamento cheio sem baú: para sem gastar energia; colocar um baú encostado esvazia e volta a minerar.

### Picareta fundidora
- [ ] Na mão aparece a picareta laranja `Picareta fundidora`. Minério de ferro, cobre e ouro quebrados dropam lingote (com chamas e um chiado); com Fortuna o número de lingotes sobe como o de brutos; pedregulho dropa pedra; diamante e carvão dropam normal.
- [ ] Durabilidade e encantamentos funcionam como numa picareta de ferro.

- [ ] Sem erros `[custom-items]` de bloco de máquina no log durante toda a sessão.

### Visual estilo Create, rastro e raio
- [ ] As quatro máquinas aparecem no estilo Create (moldura suave de 2 px em relevo e painel rebaixado 1 px em cada face, sem nenhum buraco para o mundo): Gerador em latão com madeira escura e uma porta acesa, tampa de cobre com engrenagem; Fornalha elétrica como um alto-forno de aço com fogo azul; Minerador em andesito com tábuas e a cabeça da broca embaixo; Painel solar em andesito com células azuis em cima. A Picareta fundidora é de latão com fio em brasa.
- [ ] Com uma Fornalha ou Minerador puxando energia, sai uma linha reta e constante de partículas amarelo-claras da tampa do Gerador (ou de baixo do Painel solar) até a tampa da máquina, só para quem está a até 32 blocos; some menos de 2 s depois que a máquina para.
- [ ] `/pref` (ou `/preferencias`) abre `Preferências` com `Rastro de energia` ligado; clicar desliga (o rastro some só para você) e a escolha continua depois de sair e entrar.
- [ ] No menu do Gerador, `Mostrar raio` desenha um círculo ciano de 8 blocos em volta dele, só para você; clicar de novo esconde. Clique direito no Painel solar faz o mesmo. Afastar mais de 48 blocos, sair do servidor ou quebrar a fonte apaga o círculo.

### Minerador estilo drill (barrier + display)
- [ ] O Minerador aparece como a drill do Create: meia carcaça de andesito e madeira em cima e a broca cônica embaixo, sem buraco para o mundo (o bloco de baixo e os vizinhos aparecem inteiros). Na mão, o item mostra a drill.
- [ ] Mineradores colocados antes desta versão viram drill sozinhos quando o chunk carrega, com energia, itens guardados e progresso intactos.
- [ ] Colocar, sair e voltar (ou afastar até descarregar o chunk e voltar): a drill reaparece uma vez só, sem duplicar.
- [ ] Clique direito abre o menu `Minerador`. Em survival, 3 cliques esquerdos rápidos quebram (som de metal, partículas) e dropam o item com o estado; visitante não quebra nem abre. Em criativo quebra com um clique.
- [ ] O menu do Minerador tem uma alavanca Ligado/Desligado. Ligado e com energia no raio, a broca gira sem parar e o gerador perde 20 E/s mesmo sem nada para minerar (a linha de energia fica acesa); desligado ou sem energia, a broca para e não gasta nada.
- [ ] Explosão, pistão e água não mexem no Minerador.

### Brocas do Minerador
- [ ] `/receitas` → `Máquinas` lista Broca de ouro, diamante, obsidiana e netherite com `Nível 28`, `32`, `36` e `40`; cada uma (menos a de ouro) pede a broca anterior. A de netherite pede 2 lingotes de netherite.
- [ ] Na mão, cada broca aparece como a broca 3D na cor do material (dourada, ciano, roxo escuro, preta com friso dourado), centrada no slot.
- [ ] Um Minerador novo (ou de antes desta versão) mostra no menu `Broca de ferro` (picareta de ferro) e quebra um bloco a cada 2 s puxando 20 E/s.
- [ ] Com o menu aberto, clicar numa Broca de ouro do inventário encaixa uma só (a pilha perde 1), toca a bigorna e a broca que gira fica dourada; o Minerador quebra a cada 1,5 s e a alavanca diz 28 E/s.
- [ ] Encaixar a de diamante com a de ouro encaixada devolve a de ouro ao inventário (cai nos pés se estiver cheio); encaixar a mesma broca de novo não faz nada (som grave), assim como clicar em outro item.
- [ ] Clicar no slot da broca devolve a broca e volta para ferro; com o inventário cheio aparece `Seu inventário está cheio.` e nada muda.
- [ ] Obsidiana quebra 2 blocos a cada 1,5 s e netherite um bloco a cada 0,5 s; as rachaduras acompanham o ritmo.
- [ ] Quebrar o Minerador com uma broca encaixada e recolocar: a broca continua encaixada.
- [ ] Visitante não encaixa nem tira broca.

### Fornalha elétrica como alto-forno e Painel solar como sensor de luz
- [ ] A Fornalha elétrica é um alto-forno com textura de aço e fogo azul: a frente vira para quem coloca e acende (fogo azul animado) só enquanto derrete com energia. Funil não põe nem tira nada dela. Fornalhas elétricas antigas viram alto-forno sozinhas com os itens intactos.
- [ ] Todo alto-forno do jogo passa a ter essa textura, com o mesmo relevo das máquinas (moldura levantada e painel rebaixado), aceso ou apagado e em qualquer direção; a fornalha comum continua vanilla.
- [ ] O Painel solar é um sensor de luz (6 px de altura) com células azuis em cima; clicar nele não inverte o sensor, mostra a energia e liga/desliga o círculo do raio. Painéis antigos viram sensor sozinhos com a energia intacta.
- [ ] Agachado com a mão vazia, clicar numa máquina não abre inventário vanilla nenhum.
- [ ] O Gerador de energia é um bloco de aço com cantoneiras, chapas e grelha nas laterais e uma janela de fogo na frente; a frente fica virada para quem colocou. Geradores antigos ficam com a frente para o norte.

### Paxels na bancada

- [ ] Na bancada de trabalho, machado, picareta e pá de ferro na linha de cima e dois gravetos na coluna do meio dão o Paxel de Ferro; o mesmo com diamante e netherita dá o Paxel de Diamante e o de Netherita. Ferramentas gastas também servem.
- [ ] Trocar a picareta de ferro pela Picareta fundidora (ou por um paxel) na grade não mostra resultado.
- [ ] O paxel feito na bancada quebra pedra, madeira e terra como o de `/citem give`.
