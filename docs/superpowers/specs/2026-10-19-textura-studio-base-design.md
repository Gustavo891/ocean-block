# Textura Studio — Parte 1: base (abrir, prever, editar JSON, salvar)

Data: 2026-10-19
Pedido do usuário: um painel local, no mesmo estilo e com as mesmas funções do resourcepack.ai Studio (editor de dialog com preview), integrado ao Textura: salvar grava direto no servidor e ele dá `/textura reload` para ver no jogo. Tudo roda no PC dele. Ele deixou as decisões comigo e ajusta a cada entrega.

## Partes do projeto (ordem)
1. **Base** (este documento).
2. Edição visual no preview (selecionar, arrastar, redimensionar, editar texto, biblioteca de componentes, propriedades, páginas, desfazer/refazer) cobrindo o que o Textura já suporta.
3. Componentes novos no motor (os do site que faltam), um a um.
4. Outras seções: notificações (`configs/hud/notificacoes.yml`), HUD da bossbar, glyphs/ícones, temas.

## Objetivo da parte 1
Abrir o painel no navegador, ver a lista de dialogs, abrir um, vê-lo desenhado **exatamente como o Textura monta no jogo**, trocar monitor (1080p/1440p/4K) e GUI scale (Auto/1–4), editar o JSON com o preview atualizando, e salvar direto em `server/plugins/Textura/configs/ui/<id>.json`.

## Decisões
- **Onde:** módulo novo `tools/textura-studio` no monorepo `ocean-block` (`:tools:textura-studio`).
- **Backend:** Kotlin/JVM, servidor HTTP do JDK (`com.sun.net.httpserver`) em `127.0.0.1:7070` (só local). Usa as classes do próprio Textura (`ScreenParser`, `ScreenCompiler`, `UiLayout`, `BaseUiKit`, `UiArtCatalog`, `UiTextFontLoader`, `UiThemeRegistry`) do mesmo jeito que os testes do Textura já fazem offline.
- **Preview fiel:** o backend rasteriza o `LaidOut` num PNG com Java2D: imagens do catálogo (nine-slice para superfícies, escala para ícones), textos com as fontes bitmap do `configs/fonts/custom.json` (cor, negrito, sombra como no jogo). Em volta, a moldura vanilla do dialog (fundo escurecido, título no topo, botão do rodapé), com as texturas do client jar que o Textura já baixa (`generated/vanilla/client-<ver>.jar`).
- **Listas dinâmicas (`repeat`):** o preview preenche com entradas de exemplo (o texto `{campo}` aparece literal, como no gerar); opção de valores de exemplo fica para a parte 2.
- **Frontend:** React + TypeScript + Vite em `tools/textura-studio/web`, tema escuro no estilo do site: barra superior (voltar, nome, JSON, Salvar com estado "Salvo"/"Não salvo"), lista de componentes à esquerda (árvore só leitura nesta parte), preview no centro com abas de página, seletor de monitor e GUI scale, painel direito com propriedades do dialog (título, tema, largura do conteúdo, avisos do parser) e editor JSON.
- **Início:** página com todos os dialogs como cartões com miniatura.
- **Salvar:** valida com o `ScreenParser` (avisos mostrados, salvar continua permitido), grava de forma atômica, `Ctrl+S`. Recarregar no jogo continua manual (`/textura reload`).
- **Rodar:** `./gradlew :tools:textura-studio:run` (compila o frontend com npm e sobe o servidor) e abre `http://127.0.0.1:7070`. Pasta de dados padrão `server/plugins/Textura` do repo, sobreponível por `TEXTURA_DATA`.

## API local
- `GET /api/dialogs` → `[{id, title, theme, pages}]`
- `GET /api/dialogs/{id}` → JSON cru
- `PUT /api/dialogs/{id}` → grava; resposta `{warnings}`
- `POST /api/preview` (`{json, page}`) → `{png (base64), width, height, warnings, hotspots}`
- `GET /api/themes` → nomes dos temas

## Erros
- JSON inválido: preview mostra o erro do parser; nada é gravado se o JSON não for legível.
- Arte/fonte ausente: renderiza o resto e lista o aviso.
- Porta ocupada: mensagem clara no console.

## Testes
Kotest no módulo: listar/ler/gravar (pasta temporária), preview de `shop.json` gera PNG com o tamanho do `LaidOut`, JSON quebrado devolve erro sem gravar.

## Fora de escopo (parte 1)
Edição visual, biblioteca de componentes, export PNG/plugin, IA, outras seções, recarregar automático no servidor.

## Parte 2 — edição visual (entregue)
- Motor: `UiNode.tag` + `LaidOut.boxes` (caixa de cada nó com o caminho do parser); o jogo ignora.
- Preview: clique seleciona a peça mais interna, duplo clique edita texto no lugar, arrastar move (linha azul mostra onde entra), alças mudam largura/altura (ícone: tamanho; space: px), botão direito abre ações. Modo "Testar" mostra tooltips.
- Árvore: arrastar para mover (antes/depois/dentro), biblioteca "Adicionar" (clique insere, arrastar para preview ou árvore).
- Ações: duplicar, copiar/colar (entre telas), envolver em Row/Column/Card, subir/descer, apagar, selecionar pai; atalhos Ctrl+D/C/V, Del, Alt+↑/↓, Esc.
- Inspector por tipo de peça: tamanho Fit/Grow/Px, cor, ícones com miniatura e sugestões (tema, ui:, glyph:, item:), opções de segmented/dropdown, rótulo·valor, slots com itens, estados de ícone, onClick (página, fechar, ações `[open] [page] [set] [message] [console] [player] [sound]`), repetir lista, posição livre.
- Dados de exemplo por lista e valores da tela (só no preview, guardados no navegador).
- Páginas: criar (copia a atual), renomear (duplo clique, atualiza `onClick` que apontam para ela), apagar.
- Exportar PNG do preview; novo dialog a partir de cópia de outro.

## Parte 4 — outras seções (entregue)
- **Notificações** (`configs/hud/notificacoes.yml`): tela de jogo com as regiões desenhadas como o `text.vsh` posiciona (âncora + x/y, passo 11 + espaçamento, placa de 11 px, ícone de 8 px, 16 cores legadas). Arrastar uma região muda x/y; inspector com todos os campos; linhas de exemplo por região (só no navegador).
- **HUD bossbar**: teste de texto principal + secundária na segunda bossbar, com o fundo `hud_left/body/right` e a fonte `bossbar` (+3 de ascent). Nada a salvar: a HUD vem do código.
- **Glyphs** (`configs/glyphs/*.yml`): arquivos, grade com miniaturas, editor de todos os campos (fontes nomeadas, folhas), upload de PNG para `resource/assets/oneblock/textures/icons/`, preview no chat com ascent/height, novo arquivo/entrada, apagar.
- **Temas** (`configs/ui/*.yml`): amostra gerada com tudo que o tema declara, renderizada com o tema ainda não salvo; abas Texto, Superfícies, Controles (inclui looks de botão), Ícones e Artes (miniaturas, trocar PNG e editar `.slice`).
- YAML editado com a lib `yaml` (mantém comentários, ordem e CRLF). O servidor só lê/grava em `configs/{hud,glyphs,ui}`, `configs/fonts` (leitura) e `resource/assets`; `config.yml` fica fora.
