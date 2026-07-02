package com.example.wearshooter

/**
 * Zero-GC entity lifecycle. The game spawns hundreds of bullets and particles per
 * second; allocating them fresh makes the GC hitch mid-dodge on a watch. Instead
 * every high-churn entity is [Poolable]: update/collision code only flips `alive`,
 * and once per frame [sweep] swap-removes the dead and hands them back to a [Pool].
 */
internal interface Poolable {
    var alive: Boolean
}

internal class Pool<T : Poolable>(warm: Int, private val factory: () -> T) {
    private val free = ArrayList<T>(warm * 2)

    init {
        repeat(warm) { free.add(factory()) }
    }

    fun obtain(): T = if (free.isEmpty()) factory() else free.removeAt(free.size - 1)

    fun release(t: T) {
        free.add(t)
    }
}

/**
 * Swap-remove every dead entity (order isn't meaningful for any of these lists, so
 * the O(1) swap beats ArrayList's shifting removal). Iterates downward so the live
 * element swapped in from the tail has already been examined.
 */
internal fun <T : Poolable> sweep(list: ArrayList<T>, pool: Pool<T>? = null) {
    var i = list.size - 1
    while (i >= 0) {
        val t = list[i]
        if (!t.alive) {
            val last = list.size - 1
            list[i] = list[last]
            list.removeAt(last)
            pool?.release(t)
        }
        i--
    }
}

/** Empty the list, returning everything (alive or not) to its pool. */
internal fun <T : Poolable> releaseAll(list: ArrayList<T>, pool: Pool<T>? = null) {
    if (pool != null) for (i in list.indices) pool.release(list[i])
    list.clear()
}
