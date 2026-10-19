# OceanBlock — Tela de receitas em dialog — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: superpowers:subagent-driven-development. Each task's requirements are the referenced spec sections; values there are binding.

**Goal:** Receitas do OceanBlock (Gancho, Rede) craftadas por uma tela em dialog do Textura no estilo da loja enviada pelo usuário.

**Spec:** `docs/superpowers/specs/2026-10-18-oceanblock-receitas-dialog-design.md`

## Global Constraints
- Sem comentários em código/Gradle/YAML/JSON gerado. Strings ao jogador em português.
- Commits `type(scope): description` + trailer `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`, em comando separado (hook bloqueia `-n`/`--no-verify`). Sem arquivos de `server/` exceto os listados na task.
- Build `./gradlew :plugins:oceanblock:plugin:localJar` e `./gradlew :plugins:textura:localJar`; testes Textura `./gradlew :plugins:textura:localTest`, craft/drift com `-Penv=local`.
- Não parar o servidor nem fazer deploy.
- Telas existentes do Textura (`shop`, `kits`, `settings`, `social`) não podem mudar de aparência.

## Task 1: Textura — repetição, slots dinâmicos e binds de lista
Spec §2 inteiro. Files: `plugins/textura/src/main/kotlin/one_block/textura/dialog/**`, `api/DialogApi.kt`, `api/DialogBinds.kt`, `api/DialogClick.kt`, testes em `plugins/textura/src/test/kotlin/...`, `server/plugins/Textura/README.md` (documentar `repeat`, `perPage`, `list`, `listButton`, `listPage`, placeholders em slots/botões). TDD nos pontos listados em §2.
Produces: `DialogBinds.list(key: String, entries: () -> List<Map<String, String>>)`, `DialogBinds.listPage(key: String, page: () -> Int)`, `DialogBinds.listButton(key: String, action: (DialogClick, Int) -> Unit)`.

## Task 2: Craft reescrito + receitas do mar
Spec §1 (receitas, ingredientes, bancada vanilla), §3. Files: `plugins/oceanblock/system/craft/**` (remove o conteúdo antigo, mantém `CustomCraftEvent`), `plugins/oceanblock/system/custom-items/**` (remove o registro de receita de paxel), `plugins/oceanblock/system/drift/**` (registra Gancho/Rede via `Recipes.register`, remove `GanchoRecipe` e `NetRecipe` vanilla), `plugins/oceanblock/plugin/.../OceanBlockPlugin.kt` (comando antigo `/forja`/`/craft` sai). Testes Kotest para contagem/consumo puros e `Recipes` (registro, por categoria, id duplicado).
Produces: `Recipes`, `OceanRecipe`, `RecipeCategory`, `Ingredient`, `RecipeCrafter.craft(player, recipe): CraftResult`.

## Task 3: Tela `receitas` + comando
Spec §1 (layout, chips, card, ícones), §3 (`RecipesScreen`, `ReceitasCommand`), §4, §5. Files: `server/plugins/Textura/configs/ui/receitas.json` (commit), `plugins/oceanblock/system/craft/**` (screen, command, estado por jogador), `build.gradle.kts` do craft (`compileOnly(projects.plugins.textura)`), CLAUDE.md (seção do craft reescrita), roteiro de aceite (seção nova "8. Receitas").
Consumes: Task 1 API, Task 2 API.
