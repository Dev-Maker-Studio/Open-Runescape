package org.darkan.migration

import java.util.TreeMap

/**
 * Accumulates what a transform did to a collection so the run can be judged before anything is
 * trusted: how many documents went through, how many were changed by each rule, and every
 * document that could not be migrated, by key.
 */
class Report(val collection: String) {
    var scanned = 0
    var written = 0
    private val counters = TreeMap<String, Int>()
    private val problems = TreeMap<String, MutableList<String>>()

    fun count(rule: String, by: Int = 1) {
        if (by == 0) return
        counters[rule] = (counters[rule] ?: 0) + by
    }

    fun problem(rule: String, key: String) {
        problems.getOrPut(rule) { mutableListOf() }.add(key)
    }

    val hasProblems get() = problems.isNotEmpty()

    fun print() {
        println("== $collection: scanned $scanned, written $written")
        for ((rule, n) in counters)
            println("   $rule: $n")
        for ((rule, keys) in problems) {
            println("   !! $rule: ${keys.size}")
            keys.take(MAX_LISTED).forEach { println("        $it") }
            if (keys.size > MAX_LISTED) println("        ... ${keys.size - MAX_LISTED} more")
        }
    }

    companion object {
        private const val MAX_LISTED = 25
    }
}
