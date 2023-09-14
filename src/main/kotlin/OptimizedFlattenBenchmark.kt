package com.example.benchmark.optimized

import kotlinx.benchmark.*
import kotlin.random.Random

@State(Scope.Benchmark)
open class OptimizedFlattenBenchmark {
    private var list: List<List<Int>> = emptyList()
    private var seq: Sequence<Iterable<Int>> = emptySequence()

    @Param("100", "1000", "10000")
    var size: Int = 0
    @Param("10")
    var sublistSize: Int = 0
    @Param("0.0", "0.25", "0.75", "1.0")
    var emptySubListProbability: Double = 0.0

    @Setup
    fun setupList() {
        list = createListOfList(size, sublistSize, emptySubListProbability)
        seq = list.asSequence()
    }

    @Benchmark
    fun baseline() = seq.flatten().lastOrNull()

    @Benchmark
    fun build() = seq.builtFlatten().lastOrNull()

    @Benchmark
    fun optimized() = seq.optimizedFlatten().lastOrNull()

    @Benchmark
    fun optimizedUsingAbstractIterator() = seq.optimizedFlatten4().lastOrNull()
}

fun createListOfList(count: Int, countInternal: Int, emptySublistProbability: Double): List<List<Int>> {
    val random = Random(count)
    return MutableList(count) {
        if (emptySublistProbability == 0.0) {
            createIntList(countInternal)
        } else if (emptySublistProbability == 1.0) {
            emptyList()
        } else {
            if (random.nextDouble() <= emptySublistProbability) {
                emptyList()
            } else {
                createIntList(countInternal)
            }
        }
    }
}

fun createIntList(count: Int): List<Int> {
    return mutableListOf<Int>().apply {
        (0..count).forEach { add(it) }
        shuffle()
    }
}
