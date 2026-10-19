# OceanBlock — Tela de receitas em dialog

Data: 2026-10-18
Pedido do usuário: sistema de receita/craft usando o dialog do Textura, no estilo da tela de loja enviada (navegação lateral, chips no topo, grid de cards com item, nome, custo e botão). Receitas **totalmente novas do OceanBlock** (gancho, rede de detritos e as próximas); nada das receitas do one-block.

## 1. Decisões

| Tema | Decisão |
|---|---|
| Receitas | Só OceanBlock. Iniciais: **Gancho** (categoria Ferramentas) e **Rede de Detritos** (categoria Coleta). As receitas do one-block saem (couro, platina, paxels) junto com `/forja`. |
| Comando | `/receitas` (aliases `/craft`, `/receita`). |
| Bancada vanilla | As receitas vanilla do gancho (`oceanblock:gancho`) e da rede (`oceanblock:rede`) deixam de existir; os dois só saem pela tela. |
| Ingredientes | Gancho: 3 tábuas (qualquer, `Tag.PLANKS`) + 2 linhas. Rede: 4 tábuas + 5 linhas. |
| Layout | O JSON do usuário, adaptado: português, tema `stone`, categorias na lateral, chips no topo, grid 2 colunas × 3 linhas (6 cards por página), setas de página quando houver mais de 6, rodapé "Os itens vão para o seu inventário." |
| Chips | Texto apenas (sem ícone): quantidade que o jogador tem de cada ingrediente usado na categoria aberta, até 4 (ex.: `12 Linha`). |
| Card | Slot com o item resultado (item real do dialog), nome, linha de custo (`3 Tábuas · 2 Linhas`), botão **Craftar** (`ui:cart`, `look: light`); desabilitado quando falta material, com tooltip listando o que falta. |
| Ícones | Categoria Ferramentas `ui:pickaxe`, Coleta `ui:chest`. `ui:grid` não existe no tema e não é usado. |

## 2. Extensão do Textura (dialog)

O dialog hoje só troca textos `{chave}`; não repete nós nem troca itens de slot por código. Extensões:

1. **Repetição:** qualquer nó pode ter `"repeat": "<lista>"` e opcionalmente `"perPage": N`. O código fornece a lista via `DialogBinds.list(key) { List<Map<String, String>> }`. O nó é renderizado uma vez por entrada (no máximo `perPage`, a partir de `page * perPage`); dentro dele `{campo}` resolve primeiro na entrada, depois nos valores globais.
2. **Slots dinâmicos:** em `slots.items[]`, `item` e `count` aceitam `{campo}`; `name` e `lore` passam a ser lidos e aplicados (MiniMessage via `Mini`/tags do Textura) em vez de ignorados.
3. **Botões e nav em lista:** um `button` ou `nav` dentro de um nó repetido com `"bind": "<chave>"` chama `DialogBinds.listButton(key) { click, index -> }` com o índice da entrada. `disabled` e `selected` aceitam `{campo}` (`"true"`/`"false"`).
4. **Página da lista:** `DialogBinds.listPage(key) { Int }` define a página atual; o código troca de página reabrindo com outro valor.
5. **Pré-renderização:** como superfícies são assadas por altura no `/textura gerar`, o gerar compila cada nó com `repeat` + `perPage` para **todas** as contagens de 0 a `perPage`, para que qualquer página parcial tenha altura pronta. Sem `perPage`, o gerar avisa e usa 1.
6. Compatibilidade: telas existentes (`shop`, `kits`, `settings`, `social`) continuam iguais; chaves desconhecidas seguem só avisando.

Testes Kotest no módulo Textura para: expansão de `repeat` com página e `perPage`, placeholders por entrada, `disabled`/`selected` por placeholder, `item`/`count` dinâmicos, `name`/`lore` lidos, cálculo das contagens a pré-renderizar.

## 3. Sistema de receitas (`system/craft` reescrito)

- Remove o conteúdo do one-block: `forge/*`, `view/*`, `CraftCategory`, `command/ForgeCommand`, `TexturaIngredients`, receitas de couro/platina e o registro de receitas de paxel em `custom-items` (os itens paxel continuam existindo, só sem receita).
- Mantém `events/CustomCraftEvent` (usado pelo battle-pass) com a mesma assinatura.
- Modelo novo:
  - `RecipeCategory` enum: `FERRAMENTAS("Ferramentas", "ui:pickaxe", descrição)`, `COLETA("Coleta", "ui:chest", descrição)`.
  - `Ingredient(label: String, amount: Int, matches: (ItemStack) -> Boolean)` com fábricas `Ingredient.material(Material, label, amount)` e `Ingredient.tag(Tag<Material>, label, amount)`.
  - `OceanRecipe(id, name, description, category, result: () -> ItemStack, ingredients: List<Ingredient>)`.
  - `Recipes.register(recipe)`, `Recipes.byCategory()`, `Recipes.get(id)`.
- `RecipeCrafter.craft(player, recipe): CraftResult` (sealed: `Crafted`, `Missing(List<Ingredient + falta>)`): recontagem no momento do clique, consumo, entrega no inventário com sobra caindo aos pés do jogador, `CustomCraftEvent`, som de sucesso. Lógica de contagem/consumo pura sobre listas de `ItemStack` para teste.
- `RecipesScreen`: abre `receitas` via `DialogApi.open` com binds de categoria (lista `categorias`), chips (lista `chips`), cards (lista `receitas`, página), botões `craftar` (índice → receita da página) e setas de página. Estado (categoria, página) por jogador em memória.
- Comando `ReceitasCommand` (`@CommandAlias("receitas|craft|receita")`).
- `drift` registra Gancho e Rede via `Recipes.register` (compileOnly em `craft`), e remove `GanchoRecipe`/`NetRecipe` vanilla.

## 4. Tela (`server/plugins/Textura/configs/ui/receitas.json`)

Baseada no JSON do usuário: coluna de `nav` repetida sobre `categorias` (`selected: "{ativa}"`), coluna principal com linha de `chip` repetida sobre `chips`, cabeçalho com o nome da categoria `{categoria}`, `grid` de 2 colunas com o card repetido sobre `receitas` (`perPage: 6`), linha de paginação (`◀ {pagina}/{paginas} ▶`) e rodapé. Precisa de `/textura gerar` depois de instalar.

## 5. Erros
- Tela ausente ou Textura sem o `receitas`: mensagem ao jogador "A tela de receitas não está disponível." e log `warning`.
- Clique em receita sem material: nada é consumido; reabre a tela (o botão já aparece desabilitado).
- Inventário cheio: entrega o que couber, o resto cai aos pés.

## 6. Fora de escopo
- Estação física de craft (bloco); receitas por fase; ícones de item nos chips.
