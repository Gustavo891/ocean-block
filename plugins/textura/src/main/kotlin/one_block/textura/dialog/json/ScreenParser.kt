package one_block.textura.dialog.json

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import one_block.textura.dialog.DialogValues
import one_block.textura.dialog.kit.UiPixels
import one_block.textura.dialog.node.ContainerKind
import one_block.textura.dialog.node.UiAlign
import one_block.textura.dialog.node.UiPosition
import one_block.textura.dialog.node.UiSize
import one_block.textura.dialog.node.UiTextAlign
import one_block.textura.dialog.theme.UiColors
import one_block.textura.dialog.text.Placeholders
import java.io.File
import java.io.FileReader
import java.util.logging.Logger

class ScreenParser(private val logger: Logger) {

    fun parse(file: File): ScreenDefinition? {
        val id = file.nameWithoutExtension
        val json = runCatching { FileReader(file, Charsets.UTF_8).use { JsonParser.parseReader(it) } }.getOrElse {
            logger.warning("[ui:$id] JSON inválido: ${it.message}")
            return null
        }
        val obj = json.takeIf { it.isJsonObject }?.asJsonObject ?: run {
            logger.warning("[ui:$id] o arquivo precisa ser um objeto JSON")
            return null
        }

        val document = obj.get("document")?.takeIf { it.isJsonObject }?.asJsonObject
        val rootObj = document ?: obj
        val theme = obj.string("theme") ?: document?.string("theme") ?: run {
            logger.warning("[ui:$id] falta 'theme' no raiz ou no document")
            return null
        }
        val reader = Reader(id)
        if (document != null) reader.wrapper(obj)
        val root = reader.node(rootObj, "", 0, root = true) as? DefContainer ?: run {
            logger.warning("[ui:$id] o nó raiz precisa ser row, column, box ou grid")
            return null
        }

        val pages = linkedMapOf<String, DefContainer>()
        var firstPage: String? = null
        obj.get("pages")?.takeIf { it.isJsonArray }?.asJsonArray?.forEachIndexed { index, element ->
            val page = element.takeIf { it.isJsonObject }?.asJsonObject ?: return@forEachIndexed
            val pageId = page.string("id") ?: run {
                logger.warning("[ui:$id] página #$index sem 'id', ignorada")
                return@forEachIndexed
            }
            val pageDocument = page.get("document")?.takeIf { it.isJsonObject }?.asJsonObject
            if (pageDocument == null && firstPage == null) firstPage = pageId
            pages[pageId] = pageDocument?.let { reader.node(it, pageId, index, root = true) as? DefContainer } ?: root
        }
        if (firstPage == null) firstPage = pages.keys.firstOrNull()

        val title = obj.string("title")
        val hideFooter = obj.get("hideFooter")?.let { runCatching { it.asBoolean }.getOrNull() } == true
        val close = when {
            hideFooter -> null
            obj.get("close")?.isJsonNull == true -> null
            else -> obj.string("close") ?: DEFAULT_CLOSE
        }
        val containers = (listOf(root) + pages.values).distinct()

        return ScreenDefinition(
            id = id,
            theme = theme,
            title = title,
            close = close,
            root = root,
            keys = keysOf(containers, title),
            controlBinds = controlBindsOf(id, containers),
            pages = pages,
            firstPage = firstPage,
            escape = obj.get("canCloseWithEscape")?.let { runCatching { it.asBoolean }.getOrNull() } ?: true,
            pause = obj.get("pause")?.let { runCatching { it.asBoolean }.getOrNull() } ?: false,
            defaults = defaultsOf(containers),
            choices = choicesOf(containers),
        )
    }

    private fun controls(containers: List<DefContainer>): List<DefNode> {
        val nodes = mutableListOf<DefNode>()
        fun walk(node: DefNode) {
            when (node) {
                is DefContainer -> node.children.forEach(::walk)
                is DefRepeat -> walk(node.node)
                is DefScoped -> walk(node.node)
                else -> nodes += node
            }
        }
        containers.forEach(::walk)
        return nodes
    }

    private fun defaultsOf(containers: List<DefContainer>): Map<String, String> {
        val defaults = linkedMapOf<String, String>()
        controls(containers).forEach { node ->
            when (node) {
                is DefSwitch -> node.bind?.let { defaults.putIfAbsent(it, node.value.toString()) }
                is DefSegmented -> node.bind?.let { bind ->
                    node.options.getOrNull(node.selected.coerceIn(0, maxOf(0, node.options.lastIndex)))
                        ?.let { defaults.putIfAbsent(bind, DialogValues.optionId(it)) }
                }
                is DefDropdown -> node.bind?.let { bind ->
                    val initial = node.options.firstOrNull { DialogValues.optionId(it) == DialogValues.optionId(node.initial) }
                        ?: node.options.firstOrNull()
                    initial?.let { defaults.putIfAbsent(bind, DialogValues.optionId(it)) }
                }
                is DefSlider -> node.bind?.let { defaults.putIfAbsent(it, node.value.coerceIn(node.min, maxOf(node.min, node.max)).toString()) }
                else -> Unit
            }
        }
        return defaults
    }

    private fun choicesOf(containers: List<DefContainer>): Map<String, List<String>> {
        val choices = linkedMapOf<String, List<String>>()
        controls(containers).forEach { node ->
            when (node) {
                is DefSegmented -> node.bind?.let { choices.putIfAbsent(it, node.options) }
                is DefDropdown -> node.bind?.let { choices.putIfAbsent(it, node.options) }
                else -> Unit
            }
        }
        return choices
    }

    private fun JsonObject.string(key: String): String? =
        get(key)?.takeIf { it.isJsonPrimitive }?.asString

    private fun keysOf(containers: List<DefContainer>, title: String?): Set<String> {
        val keys = mutableSetOf<String>()
        fun placeholders(text: String?) {
            if (text != null) PLACEHOLDER.findAll(text).forEach { keys += it.groupValues[1] }
        }
        fun flag(flag: DefFlag) {
            if (flag is DefFlag.Bound) placeholders(flag.raw)
        }
        fun steps(click: DefClick?) {
            click?.steps?.forEach { step ->
                when (step) {
                    is DefStep.Message -> placeholders(step.text)
                    is DefStep.Command -> placeholders(step.line)
                    is DefStep.Set -> step.pairs.values.forEach(::placeholders)
                    is DefStep.Open -> step.args.values.forEach(::placeholders)
                    else -> Unit
                }
            }
        }
        fun walk(node: DefNode) {
            when (node) {
                is DefContainer -> node.children.forEach(::walk)
                is DefText -> {
                    placeholders(node.text)
                    placeholders(node.style)
                    placeholders(node.icon)
                    placeholders(node.tooltip)
                }
                is DefSwitch -> {
                    node.bind?.let(keys::add)
                    placeholders(node.tooltip)
                }
                is DefSegmented -> {
                    node.bind?.let(keys::add)
                    placeholders(node.tooltip)
                }
                is DefDropdown -> {
                    node.bind?.let(keys::add)
                    placeholders(node.tooltip)
                }
                is DefSlider -> {
                    node.bind?.let(keys::add)
                    placeholders(node.tooltip)
                }
                is DefButton -> {
                    node.bind?.let(keys::add)
                    placeholders(node.label)
                    placeholders(node.icon)
                    placeholders(node.tooltip)
                    flag(node.selected)
                    flag(node.disabled)
                    steps(node.click)
                }
                is DefNav -> {
                    node.bind?.let(keys::add)
                    placeholders(node.label)
                    placeholders(node.icon)
                    placeholders(node.tooltip)
                    flag(node.selected)
                    steps(node.click)
                }
                is DefRepeat -> {
                    keys += node.list
                    walk(node.node)
                }
                is DefScoped -> walk(node.node)
                is DefSlots -> {
                    node.bind?.let(keys::add)
                    node.items.forEach { item ->
                        placeholders(item.item)
                        placeholders(item.count)
                        placeholders(item.name)
                        item.lore.forEach(::placeholders)
                        flag(item.missing)
                    }
                }
                is DefIcon -> {
                    node.bind?.let(keys::add)
                    placeholders(node.id)
                }
                is DefChip -> {
                    placeholders(node.label)
                    placeholders(node.icon)
                    placeholders(node.tooltip)
                }
                is DefPair -> {
                    placeholders(node.label)
                    placeholders(node.value)
                }
                is DefProgress -> {
                    placeholders(node.value)
                    placeholders(node.max)
                }
                is DefPixels -> keys += node.bind
                is DefSpace, is DefImage -> Unit
            }
        }
        containers.forEach(::walk)
        placeholders(title)
        return keys - Placeholders.VALUE_KEY
    }

    private fun controlBindsOf(id: String, containers: List<DefContainer>): Set<String> {
        val (valid, invalid) = collectControlBinds(containers).partition(DialogValues::validKey)
        invalid.forEach { logger.warning("[ui:$id] bind '$it' fora de [a-z0-9_.-]: o valor fica só na sessão, não é salvo no jogador") }
        return valid.toSet()
    }

    private fun collectControlBinds(containers: List<DefContainer>): Set<String> {
        val binds = mutableSetOf<String>()
        fun walk(node: DefNode) {
            when (node) {
                is DefContainer -> node.children.forEach(::walk)
                is DefRepeat -> walk(node.node)
                is DefScoped -> walk(node.node)
                is DefSwitch -> node.bind?.let(binds::add)
                is DefSegmented -> node.bind?.let(binds::add)
                is DefDropdown -> node.bind?.let(binds::add)
                is DefSlider -> node.bind?.let(binds::add)
                is DefIcon -> node.bind?.let(binds::add)
                else -> Unit
            }
        }
        containers.forEach(::walk)
        return binds
    }

    private inner class Reader(private val id: String) {

        fun wrapper(obj: JsonObject) {
            obj.keySet().filterNot { it in WRAPPER || it in IGNORED }.forEach { warn("raiz", "chave desconhecida '$it'") }
        }

        fun node(obj: JsonObject, parent: String, index: Int, root: Boolean = false): DefNode? {
            val shape = CONTAINERS.keys.firstOrNull { obj.has(it) } ?: LEAVES.firstOrNull { obj.has(it) }
            val label = obj.string("name") ?: shape?.let { "$it#$index" } ?: "#$index"
            val path = if (parent.isEmpty()) label else "$parent > $label"

            if (shape == null) {
                warn(path, "nó sem forma reconhecível, descartado")
                return null
            }

            val node = when (shape) {
                "space" -> space(obj, path)
                "text" -> text(obj, path)
                "icon" -> icon(obj, path)
                "image" -> DefImage(obj.string("image").orEmpty(), size(obj, "w", path), position(obj, path), path)
                "pixels" -> pixels(obj, path)
                "switch" -> DefSwitch(obj.bool("switch", path), obj.string("bind"), obj.string("tooltip"), path)
                "segmented" -> segmented(obj, path)
                "button" -> DefButton(
                    singleLine(obj.string("button").orEmpty(), path), obj.string("bind"), size(obj, "w", path),
                    obj.string("icon"), obj.string("tooltip"), path, click(obj, path),
                    obj.string("look"), flag(obj, "selected", path), flag(obj, "disabled", path),
                )
                "nav" -> DefNav(
                    singleLine(obj.string("nav").orEmpty(), path), obj.string("icon"), size(obj, "w", path), click(obj, path), path,
                    flag(obj, "selected", path), obj.string("tooltip"), obj.string("bind"),
                )
                "slots" -> slots(obj, path)
                "chip" -> DefChip(singleLine(obj.string("chip").orEmpty(), path), obj.string("icon"), obj.string("tooltip"), path)
                "pair" -> pair(obj, path)
                "dropdown" -> dropdown(obj, path)
                "slider" -> slider(obj, path)
                "progress" -> progress(obj, path)
                else -> container(obj, CONTAINERS.getValue(shape), shape, path)
            }

            val allowed = KNOWN.getValue(shape) + COMMON + IGNORED + if (root) ROOT else emptySet()
            obj.keySet().filterNot { it in allowed }.forEach { warn(path, "chave desconhecida '$it'") }
            return repeated(obj, node, path, root)
        }

        private fun repeated(obj: JsonObject, node: DefNode, path: String, root: Boolean): DefNode {
            val element = obj.get(REPEAT) ?: return node
            val list = element.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isString }?.asString?.takeIf { it.isNotBlank() }
            return when {
                list == null -> node.also { warn(path, "'$REPEAT' deveria ser o nome de uma lista") }
                root -> node.also { warn(path, "'$REPEAT' no nó raiz é ignorado") }
                else -> DefRepeat(list, perPage(obj, path), node, path)
            }
        }

        private fun perPage(obj: JsonObject, path: String): Int? {
            val element = obj.get(PER_PAGE) ?: return null
            val value = element.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isNumber }?.asInt?.takeIf { it > 0 }
            if (value == null) {
                warn(path, "'$PER_PAGE' deveria ser um número maior que 0; a lista aparece inteira")
                return null
            }
            if (value > MAX_PER_PAGE) warn(path, "'$PER_PAGE' $value passa de $MAX_PER_PAGE, usando $MAX_PER_PAGE")
            return value.coerceAtMost(MAX_PER_PAGE)
        }

        private fun flag(obj: JsonObject, key: String, path: String): DefFlag {
            val element = obj.get(key) ?: return DefFlag.OFF
            val primitive = element.takeIf { it.isJsonPrimitive }?.asJsonPrimitive ?: run {
                warn(path, "'$key' deveria ser true, false ou um {placeholder}")
                return DefFlag.OFF
            }
            if (primitive.isString && PLACEHOLDER.containsMatchIn(primitive.asString)) return DefFlag.Bound(primitive.asString)
            return DefFlag.Fixed(primitive.asBoolean)
        }

        private fun container(obj: JsonObject, kind: ContainerKind, key: String, path: String): DefContainer {
            val children = obj.array(key, path).mapIndexedNotNull { index, element ->
                if (element.isJsonObject) {
                    node(element.asJsonObject, path, index)
                } else {
                    warn(path, "filho #$index não é um objeto, descartado")
                    null
                }
            }
            return DefContainer(
                kind = kind,
                children = children,
                w = size(obj, "w", path),
                h = size(obj, "h", path),
                gap = obj.int("gap", 0, path).coerceAtLeast(0),
                pad = obj.int("pad", 0, path).coerceAtLeast(0),
                align = align(obj, "align", path),
                cross = align(obj, "cross", path),
                snap = obj.int("snap", 0, path).coerceAtLeast(0),
                columns = obj.int("columns", 1, path).coerceAtLeast(1),
                surface = obj.string("surface"),
                position = position(obj, path),
                path = path,
            )
        }

        private fun space(obj: JsonObject, path: String): DefSpace {
            val raw = obj.get("space")
            if (raw?.isJsonPrimitive == true && raw.asJsonPrimitive.isString && raw.asString == "grow") return DefSpace(0, path, grow = true)
            return DefSpace(obj.int("space", 0, path).coerceAtLeast(0), path)
        }

        private fun text(obj: JsonObject, path: String): DefText {
            val scale = obj.get("scale")?.let { runCatching { it.asDouble }.getOrNull() }
            if (scale != null && scale != 1.0) warn(path, "scale $scale não suportado, usando 1")

            val color = obj.string("color")?.let { raw ->
                UiColors.parse(raw) ?: run {
                    warn(path, "cor '$raw' inválida")
                    null
                }
            }
            val bold = obj.get("bold")?.let { runCatching { it.asBoolean }.getOrNull() }

            return DefText(
                text = singleLine(obj.string("text").orEmpty(), path),
                style = obj.string("style"),
                align = textAlign(obj, path),
                w = size(obj, "w", path),
                icon = obj.string("icon"),
                color = color,
                bold = bold,
                tooltip = obj.string("tooltip"),
                position = position(obj, path),
                path = path,
            )
        }

        private fun icon(obj: JsonObject, path: String): DefIcon {
            val states = obj.get("states")?.takeIf { it.isJsonObject }?.asJsonObject?.entrySet()
                ?.mapNotNull { (state, icon) -> runCatching { icon.asString }.getOrNull()?.let { state to it } }
                ?.toMap().orEmpty()
            return DefIcon(obj.string("icon").orEmpty(), iconSize(obj, path), position(obj, path), path, obj.string("bind"), states, obj.bool("frame", path))
        }

        private fun segmented(obj: JsonObject, path: String): DefSegmented {
            val options = obj.array("segmented", path).mapNotNull { runCatching { it.asString }.getOrNull() }
            return DefSegmented(options, obj.int("selected", 0, path), obj.string("bind"), size(obj, "w", path), obj.string("tooltip"), path)
        }

        private fun dropdown(obj: JsonObject, path: String): DefDropdown {
            val options = obj.array("options", path).mapNotNull { runCatching { it.asString }.getOrNull() }
            if (options.isEmpty()) warn(path, "dropdown sem opções")
            return DefDropdown(obj.string("dropdown").orEmpty(), options, obj.string("bind"), size(obj, "w", path), obj.string("tooltip"), path)
        }

        private fun slider(obj: JsonObject, path: String): DefSlider {
            val min = obj.int("min", 0, path)
            val max = obj.int("max", 100, path)
            val step = obj.int("step", 1, path)
            if (max <= min || step <= 0) warn(path, "slider com min/max/step inválidos")
            return DefSlider(
                obj.int("slider", min, path), min, max, step, obj.string("unit").orEmpty(),
                obj.string("bind"), size(obj, "w", path), obj.string("tooltip"), path,
            )
        }

        private fun pixels(obj: JsonObject, path: String): DefPixels {
            val bind = obj.string("pixels").orEmpty()
            if (bind.isBlank()) warn(path, "pixels sem o nome do bind")
            val scale = obj.int("scale", 1, path)
            if (scale !in UiPixels.SCALES) warn(path, "pixels com scale $scale fora de ${UiPixels.SCALES.first}..${UiPixels.SCALES.last}")
            val w = obj.int("w", 0, path)
            val h = obj.int("h", 0, path)
            if (w !in 1..MAX_PIXELS || h !in 1..MAX_PIXELS) warn(path, "pixels precisa de 'w' e 'h' entre 1 e $MAX_PIXELS")
            return DefPixels(
                bind, w.coerceIn(1, MAX_PIXELS), h.coerceIn(1, MAX_PIXELS),
                scale.coerceIn(UiPixels.SCALES.first, UiPixels.SCALES.last), position(obj, path), path,
            )
        }

        private fun progress(obj: JsonObject, path: String): DefProgress {
            val track = obj.string("track").orEmpty()
            val fill = obj.string("fill").orEmpty()
            if (track.isBlank() || fill.isBlank()) warn(path, "progress sem 'track' ou 'fill'")
            return DefProgress(obj.string("progress").orEmpty(), obj.string("max") ?: "100", track, fill, size(obj, "w", path), path)
        }

        private fun pair(obj: JsonObject, path: String): DefPair {
            val parts = obj.array("pair", path).mapNotNull { runCatching { it.asString }.getOrNull() }
            if (parts.size != 2) warn(path, "'pair' deveria ser [rótulo, valor]")
            return DefPair(
                singleLine(parts.getOrElse(0) { "" }, path), singleLine(parts.getOrElse(1) { "" }, path),
                obj.string("style"), size(obj, "w", path), path,
            )
        }

        private fun slots(obj: JsonObject, path: String): DefSlots {
            val shape = obj.array("slots", path).mapNotNull { runCatching { it.asInt }.getOrNull() }
            val columns = shape.getOrElse(0) { 1 }.coerceIn(1, MAX_SLOT_COLUMNS)
            val rows = shape.getOrElse(1) { 1 }.coerceIn(1, MAX_SLOT_ROWS)
            if (shape.size != 2) warn(path, "'slots' deveria ser [colunas, linhas]")
            val items = (obj.get("items")?.takeIf { it.isJsonArray }?.asJsonArray ?: JsonArray()).mapIndexedNotNull { index, element ->
                val item = element.takeIf { it.isJsonObject }?.asJsonObject ?: return@mapIndexedNotNull null
                val at = item.int("at", index, path)
                val material = item.string("item")
                when {
                    material.isNullOrBlank() -> {
                        warn(path, "item #$index sem 'item'")
                        null
                    }
                    at !in 0 until columns * rows -> {
                        warn(path, "item '$material' em 'at' $at fora da grade ${columns}x$rows")
                        null
                    }
                    else -> DefSlotItem(at, material, count(item, path), item.string("name"), lore(item, path), flag(item, "missing", path))
                }
            }
            return DefSlots(columns, rows, items, align(obj, "align", path), obj.bool("hideEmpty", path), path, obj.string("bind"))
        }

        private fun count(item: JsonObject, path: String): String {
            val primitive = item.get("count")?.takeIf { it.isJsonPrimitive }?.asJsonPrimitive ?: return DEFAULT_COUNT
            val raw = primitive.asString.trim()
            if (PLACEHOLDER.containsMatchIn(raw)) return raw
            val value = raw.toIntOrNull() ?: run {
                warn(path, "'count' '$raw' deveria ser número ou {placeholder}, usando 1")
                return DEFAULT_COUNT
            }
            return value.coerceAtLeast(1).toString()
        }

        private fun lore(item: JsonObject, path: String): List<String> {
            val element = item.get("lore") ?: return emptyList()
            return when {
                element.isJsonArray -> element.asJsonArray.mapNotNull { line -> line.takeIf { it.isJsonPrimitive }?.asString }
                element.isJsonPrimitive -> element.asString.split('\n')
                else -> emptyList<String>().also { warn(path, "'lore' deveria ser uma lista de textos") }
            }
        }

        private fun click(obj: JsonObject, path: String): DefClick? {
            val click = obj.get("onClick")?.takeIf { it.isJsonObject }?.asJsonObject ?: return null
            val value = click.string("value").orEmpty()
            val steps = when (val action = click.string("action")) {
                "page" -> listOf(DefStep.Page(value))
                "close" -> listOf(DefStep.Close)
                "run_command" -> value.lines().mapNotNull { step(it.trim(), path) }
                else -> {
                    warn(path, "onClick.action '$action' desconhecida")
                    return null
                }
            }
            return DefClick(steps)
        }

        private fun step(line: String, path: String): DefStep? {
            if (line.isEmpty()) return null
            val tagged = TAG.matchEntire(line) ?: return DefStep.Command(console = false, line = line.removePrefix("/"))
            val name = tagged.groupValues[1]
            val rest = tagged.groupValues[2].trim()
            val words = rest.split(' ').filter { it.isNotBlank() }
            return when (name) {
                "open" -> DefStep.Open(words.firstOrNull().orEmpty(), args(words.drop(1)))
                "page" -> DefStep.Page(rest)
                "close" -> DefStep.Close
                "refresh" -> DefStep.Refresh
                "set" -> DefStep.Set(args(words))
                "message" -> DefStep.Message(rest)
                "console" -> DefStep.Command(console = true, line = rest.removePrefix("/"))
                "player" -> DefStep.Command(console = false, line = rest.removePrefix("/"))
                "sound" -> DefStep.Sound(
                    words.getOrElse(0) { "" },
                    words.getOrNull(1)?.toFloatOrNull() ?: 1f,
                    words.getOrNull(2)?.toFloatOrNull() ?: 1f,
                )
                else -> {
                    warn(path, "ação '[$name]' desconhecida, ignorada")
                    null
                }
            }
        }

        private fun args(words: List<String>): Map<String, String> = words.mapNotNull { word ->
            val parts = word.split('=', limit = 2)
            if (parts.size == 2 && parts[0].isNotBlank()) parts[0] to parts[1] else null
        }.toMap()

        private fun size(obj: JsonObject, key: String, path: String): UiSize {
            val element = obj.get(key) ?: return UiSize.Fit
            val primitive = element.takeIf { it.isJsonPrimitive }?.asJsonPrimitive
            return when {
                primitive?.isNumber == true -> UiSize.Px(primitive.asInt.coerceAtLeast(0))
                primitive?.isString == true && primitive.asString == "grow" -> UiSize.Grow
                primitive?.isString == true && primitive.asString == "fit" -> UiSize.Fit
                else -> {
                    warn(path, "'$key' inválido ($element), usando fit")
                    UiSize.Fit
                }
            }
        }

        private fun align(obj: JsonObject, key: String, path: String): UiAlign = when (val value = obj.string(key)) {
            null, "start" -> UiAlign.Start
            "center" -> UiAlign.Center
            "end" -> UiAlign.End
            else -> {
                warn(path, "'$key' inválido ($value), usando start")
                UiAlign.Start
            }
        }

        private fun textAlign(obj: JsonObject, path: String): UiTextAlign = when (val value = obj.string("align")) {
            null, "left" -> UiTextAlign.Left
            "center" -> UiTextAlign.Center
            "right" -> UiTextAlign.Right
            else -> {
                warn(path, "'align' inválido ($value), usando left")
                UiTextAlign.Left
            }
        }

        private fun singleLine(text: String, path: String): String {
            if (!text.contains('\n')) return text
            warn(path, "texto com quebra de linha; cada texto ocupa uma linha só, quebras viraram espaço")
            return text.replace('\n', ' ')
        }

        private fun iconSize(obj: JsonObject, path: String): Int {
            val size = obj.int("size", DEFAULT_ICON_SIZE, path)
            if (size in 1..MAX_ICON_SIZE) return size
            warn(path, "'size' $size fora de 1..$MAX_ICON_SIZE, usando ${size.coerceIn(1, MAX_ICON_SIZE)}")
            return size.coerceIn(1, MAX_ICON_SIZE)
        }

        private fun position(obj: JsonObject, path: String): UiPosition? {
            if (!obj.has("x") && !obj.has("y")) return null
            return UiPosition(obj.int("x", 0, path).coerceAtLeast(0), obj.int("y", 0, path).coerceAtLeast(0))
        }

        private fun JsonObject.int(key: String, default: Int, path: String): Int {
            val element = get(key) ?: return default
            return runCatching { element.asInt }.getOrElse {
                warn(path, "'$key' deveria ser número")
                default
            }
        }

        private fun JsonObject.bool(key: String, path: String): Boolean {
            val element = get(key) ?: return false
            return runCatching { element.asBoolean }.getOrElse {
                warn(path, "'$key' deveria ser true ou false")
                false
            }
        }

        private fun JsonObject.array(key: String, path: String): JsonArray =
            get(key)?.takeIf { it.isJsonArray }?.asJsonArray ?: JsonArray().also { warn(path, "'$key' deveria ser uma lista") }

        private fun warn(path: String, message: String) {
            logger.warning("[ui:$id] $path: $message")
        }
    }

    companion object {
        const val DEFAULT_CLOSE = "Fechar"
        const val DEFAULT_ICON_SIZE = 8
        const val MAX_ICON_SIZE = 256
        const val MAX_SLOT_COLUMNS = 21
        const val MAX_SLOT_ROWS = 14
        const val REPEAT = "repeat"
        const val PER_PAGE = "perPage"
        const val MAX_PER_PAGE = 64
        private const val MAX_PIXELS = 256
        private const val DEFAULT_COUNT = "1"
        val PLACEHOLDER = Regex("""\{([A-Za-z0-9_.-]+)""")

        private val CONTAINERS = linkedMapOf(
            "column" to ContainerKind.Column,
            "row" to ContainerKind.Row,
            "box" to ContainerKind.Box,
            "grid" to ContainerKind.Grid,
        )
        private val LEAVES = listOf("text", "button", "nav", "dropdown", "slider", "progress", "segmented", "switch", "chip", "pair", "slots", "image", "pixels", "icon", "space")
        private val COMMON = setOf("name", REPEAT)
        private val ROOT = setOf("theme", "title", "close")
        private val WRAPPER = setOf("title", "theme", "document", "pages", "hideFooter", "canCloseWithEscape", "pause", "close")
        private val IGNORED = setOf(
            "nudge", "drawButtons", "contentWidth", "showWarning", "look", "compact", "accent", "face", "frame",
            "style", "buttons", "inputs", "afterAction", "command", "commandPermission", "perPage", "scale", "columns",
        )
        private val TAG = Regex("""^\[([a-z_:]+)]\s*(.*)$""")
        private val CONTAINER_KEYS = setOf("w", "h", "gap", "pad", "align", "cross", "snap", "columns", "surface", "x", "y")
        private val KNOWN: Map<String, Set<String>> = CONTAINERS.keys.associateWith { CONTAINER_KEYS + it } + mapOf(
            "space" to setOf("space"),
            "text" to setOf("text", "style", "align", "w", "icon", "color", "bold", "scale", "tooltip", "x", "y"),
            "icon" to setOf("icon", "size", "x", "y", "bind", "states", "frame"),
            "image" to setOf("image", "w", "x", "y"),
            "pixels" to setOf("pixels", "w", "h", "scale", "x", "y"),
            "switch" to setOf("switch", "bind", "tooltip"),
            "segmented" to setOf("segmented", "selected", "bind", "w", "tooltip"),
            "button" to setOf("button", "bind", "w", "icon", "tooltip", "onClick", "look", "selected", "disabled"),
            "chip" to setOf("chip", "icon", "tooltip"),
            "pair" to setOf("pair", "style", "w"),
            "nav" to setOf("nav", "icon", "w", "onClick", "selected", "tooltip", "bind"),
            "slots" to setOf("slots", "items", "align", "hideEmpty", "bind"),
            "dropdown" to setOf("dropdown", "options", "bind", "w", "tooltip"),
            "slider" to setOf("slider", "min", "max", "step", "unit", "bind", "w", "tooltip"),
            "progress" to setOf("progress", "max", "track", "fill", "w"),
        )
    }
}
