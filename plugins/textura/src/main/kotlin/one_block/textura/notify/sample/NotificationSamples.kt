package one_block.textura.notify.sample

import java.util.concurrent.ConcurrentHashMap
import java.util.function.Consumer

internal class NotificationSamples<T> {

    private val samples = ConcurrentHashMap<String, Consumer<T>>()

    fun register(id: String, sample: Consumer<T>) {
        samples[id.lowercase()] = sample
    }

    fun ids(): List<String> = samples.keys.sorted()

    fun send(id: String, target: T): Boolean {
        val sample = samples[id.lowercase()] ?: return false
        sample.accept(target)
        return true
    }
}
