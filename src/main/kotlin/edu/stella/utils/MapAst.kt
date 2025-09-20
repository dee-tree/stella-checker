package edu.stella.utils

import org.antlr.v4.kotlinruntime.misc.Interval
import org.antlr.v4.kotlinruntime.tree.ParseTree

class MapAst<V> : Map<ParseTree, V> {
    private val map = hashMapOf<Interval, V>()
    private val converter = hashMapOf<Interval, ParseTree>()

    override fun containsKey(key: ParseTree): Boolean = map.containsKey(key.sourceInterval)

    override fun containsValue(value: V): Boolean = map.containsValue(value)

    override fun get(key: ParseTree): V? = map.get(key.sourceInterval)

    override fun isEmpty(): Boolean = map.isEmpty()

    override val entries: Set<Map.Entry<ParseTree, V>>
        get() = map.entries.mapTo(mutableSetOf()) { entry -> object : Map.Entry<ParseTree, V> {
            override val key: ParseTree
                get() = converter[entry.key]!!

            override val value: V
                get() = entry.value
        } }

    override val keys: Set<ParseTree>
        get() = converter.values.toSet()

    override val size: Int
        get() = map.size

    override val values: Collection<V>
        get() = map.values

    operator fun set(key: ParseTree, value: V) = map.set(key.sourceInterval, value).also {
        converter[key.sourceInterval] = key
    }

    fun getOrPut(key: ParseTree, defaultValue: V): V {
        return this[key] ?: run { this[key] = defaultValue; defaultValue }
    }

    fun remove(key: ParseTree): V? = map.remove(key.sourceInterval).also {
        converter.remove(key.sourceInterval)
    }

    fun clear() {
        map.clear()
        converter.clear()
    }
}