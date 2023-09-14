package com.example.benchmark.optimized

import kotlinx.benchmark.*
import kotlin.random.Random

@State(Scope.Benchmark)
open class OptimizedDistinctBenchmark {
    private var list: List<Int> = emptyList()
    private var seq: Sequence<Int> = list.asSequence()

    @Param("10", "100", "1000", "10000")
    var size: Int = 0

    @Param("same", "distinct", "mixed")
    var uniqueness: String = ""

    @Setup
    fun setupList() {
        val random = Random(size)
        list = when (uniqueness) {
            "same" -> {
                MutableList(size) { 42 }
            }
            "distinct" -> {
                MutableList(size) { it }.shuffled(random)
            }
            "mixed" -> {
                require(size >= 2)
                MutableList(size) { random.nextInt(size / 2) }
            }
            else -> {
                throw IllegalArgumentException(uniqueness)
            }
        }
        seq = list.asSequence()
    }

    @Benchmark
    fun stdlib() = seq.distinctBy { it }.last()

    @Benchmark
    fun optimized() = seq.optimizedDistinctBy { it }.last()

    @Benchmark
    fun modifiedStdlib() = seq.stdlibOptDistinctBy { it }.last()
}


