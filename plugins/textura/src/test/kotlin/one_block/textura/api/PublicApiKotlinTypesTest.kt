package one_block.textura.api

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.collections.shouldNotBeEmpty
import java.io.File
import java.lang.reflect.Executable
import java.lang.reflect.GenericArrayType
import java.lang.reflect.Modifier
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.lang.reflect.TypeVariable
import java.lang.reflect.WildcardType

class PublicApiKotlinTypesTest : StringSpec({

    val packages = listOf("one_block.textura.api", "one_block.textura.hud", "one_block.textura.notify")

    fun classesIn(pkg: String): List<Class<*>> {
        val path = pkg.replace('.', '/')
        return javaClass.classLoader.getResources(path).toList()
            .filter { it.protocol == "file" }
            .flatMap { url -> File(url.toURI()).listFiles().orEmpty().filter { it.name.endsWith(".class") } }
            .map { Class.forName("$pkg.${it.name.removeSuffix(".class")}", false, javaClass.classLoader) }
    }

    fun kotlinTypes(type: Type, seen: MutableSet<Type> = mutableSetOf()): List<String> {
        if (!seen.add(type)) return emptyList()
        return when (type) {
            is Class<*> -> when {
                type.isArray -> kotlinTypes(type.componentType, seen)
                type.name.startsWith("kotlin.") -> listOf(type.name)
                else -> emptyList()
            }
            is ParameterizedType -> kotlinTypes(type.rawType, seen) + type.actualTypeArguments.flatMap { kotlinTypes(it, seen) }
            is WildcardType -> (type.upperBounds + type.lowerBounds).flatMap { kotlinTypes(it, seen) }
            is GenericArrayType -> kotlinTypes(type.genericComponentType, seen)
            is TypeVariable<*> -> type.bounds.flatMap { kotlinTypes(it, seen) }
            else -> emptyList()
        }
    }

    fun signatureTypes(member: Executable): List<Type> =
        member.genericParameterTypes.toList() + member.parameterTypes.toList() +
            ((member as? java.lang.reflect.Method)?.let { listOf(it.genericReturnType, it.returnType) } ?: emptyList())

    val reachable = listOf(one_block.textura.registry.TitleFontSource::class.java)

    fun exposed(member: Executable): Boolean =
        Modifier.isPublic(member.modifiers) && (member !is java.lang.reflect.Method || INTERNAL_MARK !in member.name)

    fun texturaClasses(type: Type, seen: MutableSet<Type> = mutableSetOf()): List<Class<*>> {
        if (!seen.add(type)) return emptyList()
        return when (type) {
            is Class<*> -> when {
                type.isArray -> texturaClasses(type.componentType, seen)
                type.name.startsWith(TEXTURA) -> listOf(type)
                else -> emptyList()
            }
            is ParameterizedType -> texturaClasses(type.rawType, seen) + type.actualTypeArguments.flatMap { texturaClasses(it, seen) }
            is WildcardType -> (type.upperBounds + type.lowerBounds).flatMap { texturaClasses(it, seen) }
            is GenericArrayType -> texturaClasses(type.genericComponentType, seen)
            is TypeVariable<*> -> type.bounds.flatMap { texturaClasses(it, seen) }
            else -> emptyList()
        }
    }

    fun exposedTypes(type: Class<*>): List<Type> =
        listOfNotNull(type.genericSuperclass) + type.genericInterfaces +
            (type.constructors.toList() + type.methods.filter { it.declaringClass == type }).filter(::exposed).flatMap(::signatureTypes) +
            type.fields.filter { it.declaringClass == type }.map { it.genericType }

    fun publicClasses(): List<Class<*>> {
        val found = linkedSetOf<Class<*>>()
        val queue = ArrayDeque((packages.flatMap(::classesIn) + reachable).filter { Modifier.isPublic(it.modifiers) })
        while (queue.isNotEmpty()) {
            val type = queue.removeFirst()
            if (!found.add(type)) continue
            exposedTypes(type).flatMap { texturaClasses(it) }.filter { Modifier.isPublic(it.modifiers) && it !in found }.forEach(queue::addLast)
        }
        return found.toList()
    }

    fun checkedClasses(): List<Class<*>> = publicClasses().filterNot { it.name in PRE_EXISTING_REACHABLE }

    fun violations(): List<String> = checkedClasses()
        .flatMap { type ->
            val supertypes = listOfNotNull(type.genericSuperclass) + type.genericInterfaces
            val members = (type.constructors.toList() + type.methods.filter { it.declaringClass == type })
                .filter(::exposed)
                .flatMap { member -> signatureTypes(member).flatMap { kotlinTypes(it) }.map { "${type.name}: $member -> $it" } }
            val fields = type.fields.filter { it.declaringClass == type }
                .flatMap { field -> kotlinTypes(field.genericType).map { "${type.name}.${field.name} -> $it" } }
            supertypes.flatMap { kotlinTypes(it) }.map { "${type.name} extends $it" } + members + fields
        }
        .distinct()

    "nenhuma função pública tem parâmetro com valor padrão" {
        checkedClasses()
            .flatMap { type ->
                type.methods
                    .filter { it.declaringClass == type && it.name.endsWith(DEFAULTS) && it.name != DATA_COPY }
                    .map { "${type.name}.${it.name}" }
            }
            .joinToString("\n") shouldBe ""
    }

    "a varredura segue as assinaturas públicas até outras classes do Textura" {
        val reached = publicClasses()
        reached.contains(one_block.textura.notify.NotificationLine::class.java) shouldBe true
        reached.any { it.name.startsWith("one_block.textura.notify.service.") } shouldBe false
        PRE_EXISTING_REACHABLE.forEach { name -> reached.any { it.name == name } shouldBe true }
    }

    "a varredura enxerga as classes da API" {
        classesIn("one_block.textura.api").shouldNotBeEmpty()
        classesIn("one_block.textura.hud").shouldNotBeEmpty()
        classesIn("one_block.textura.notify").shouldNotBeEmpty()
    }

    "nenhuma assinatura pública da API ou do HUD expõe tipos do Kotlin" {
        violations().joinToString("\n") shouldBe ""
    }
})

private const val DEFAULTS = "\$default"
private const val DATA_COPY = "copy\$default"
private const val INTERNAL_MARK = "$"
private const val TEXTURA = "one_block.textura."
private val PRE_EXISTING_REACHABLE = setOf(
    "one_block.textura.model.BlockDefinition",
    "one_block.textura.model.BlockDefinition\$Companion",
    "one_block.textura.model.BlockEntryType",
    "one_block.textura.model.BlockBase",
    "one_block.textura.model.ItemData",
    "one_block.textura.model.ItemType",
    "one_block.textura.model.DisplayTransform",
    "one_block.textura.registry.TitleFont",
    "one_block.textura.registry.TitleFontRegistry",
    "one_block.textura.registry.TitleFontRegistry\$Companion",
)
