package textura_studio

import io.papermc.paper.registry.RegistryAccess
import io.papermc.paper.registry.RegistryKey
import org.bukkit.Keyed
import org.bukkit.Registry
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Proxy
import java.util.stream.Stream

/**
 * O compilador do Textura toca em `org.bukkit.Material` (tooltips de slot), que exige
 * um RegistryAccess do servidor. Fora do servidor os registros ficam vazios.
 */
class OfflineRegistryAccess : RegistryAccess {

    override fun <T : Keyed> getRegistry(type: Class<T>): Registry<T> = empty()

    override fun <T : Keyed> getRegistry(key: RegistryKey<T>): Registry<T> = empty()

    @Suppress("UNCHECKED_CAST")
    private fun <T : Keyed> empty(): Registry<T> = EMPTY as Registry<T>

    private companion object {
        val EMPTY: Registry<*> = Proxy.newProxyInstance(
            Registry::class.java.classLoader,
            arrayOf(Registry::class.java),
            InvocationHandler { proxy, method, args ->
                when {
                    method.isDefault -> InvocationHandler.invokeDefault(proxy, method, *(args ?: emptyArray()))
                    method.name == "toString" -> "OfflineRegistry"
                    method.name == "hashCode" -> 0
                    method.name == "equals" -> proxy === args?.firstOrNull()
                    method.returnType == Stream::class.java -> Stream.empty<Any>()
                    method.returnType == Iterator::class.java -> emptyList<Any>().iterator()
                    method.returnType == Int::class.javaPrimitiveType -> 0
                    method.returnType == Boolean::class.javaPrimitiveType -> false
                    else -> null
                }
            },
        ) as Registry<*>
    }
}
