package basewidgets

import java.util.*

internal class MultiMapList<K, V> {
    private val map: MutableMap<K, MutableList<V>?> =
        HashMap()

    fun size() = map.size

    val isEmpty: Boolean
        get() = map.isEmpty()

    fun containsKey(key: K) = map.containsKey(key)

    operator fun get(key: K): List<V>? {
        return map[key]
    }

    fun put(key: K, value: V) {
        var list = map[key]
        if (list == null) {
            list = ArrayList()
            map[key] = list
        }
        list.add(value)
    }

    fun remove(key: K) {
        map.remove(key)
    }

    fun clear() {
        map.clear()
    }
}