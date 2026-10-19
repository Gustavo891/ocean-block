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

    fun publicClasses(): List<Class<*>> = (packages.flatMap(::classesIn) + reachable).filter { Modifier.isPublic(it.modifiers) }

    fun violations(): List<String> = publicClasses()
        .flatMap { type ->
            val supertypes = listOfNotNull(type.genericSuperclass) + type.genericInterfaces
            val members = (type.constructors.toList() + type.methods.filter { it.declaringClass == type })
                .filter { Modifier.isPublic(it.modifiers) }
                .flatMap { member -> signatureTypes(member).flatMap { kotlinTypes(it) }.map { "${type.name}: $member -> $it" } }
            val fields = type.fields.filter { it.declaringClass == type }
                .flatMap { field -> kotlinTypes(field.genericType).map { "${type.name}.${field.name} -> $it" } }
            supertypes.flatMap { kotlinTypes(it) }.map { "${type.name} extends $it" } + members + fields
        }
        .distinct()

    "nenhuma função pública tem parâmetro com valor padrão" {
        publicClasses()
            .flatMap { type ->
                type.methods
                    .filter { it.declaringClass == type && it.name.endsWith(DEFAULTS) && it.name != DATA_COPY }
                    .map { "${type.name}.${it.name}" }
            }
            .joinToString("\n") shouldBe ""
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
