@file:Suppress("UNCHECKED_CAST")

package net.mcbrawls.fracture.polar.event

import kotlin.reflect.KClass

object PolarEvents {
    private val callbacks: MutableMap<KClass<out PolarEvent>, MutableSet<(PolarEvent) -> Unit>> = mutableMapOf()

    fun <T : PolarEvent> listen(clazz: KClass<T>, callback: (T) -> Unit) {
        callbacks.getOrPut(clazz) { mutableSetOf() }.add { event -> callback(event as T) }
    }

    fun <T : PolarEvent> emit(clazz: KClass<T>, event: T) {
        callbacks[clazz]?.forEach { callback ->
            runCatching {
                callback.invoke(event)
            }.exceptionOrNull()?.printStackTrace()
        }
    }
}
