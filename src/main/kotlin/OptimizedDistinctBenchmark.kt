package com.example.benchmark.optimized

import kotlinx.benchmark.*
import kotlin.random.Random

@State(Scope.Benchmark)
open class OptimizedDistinctBenchmark {
    private var list: List<Int> = emptyList()

    @Param("100", "1000", "10000", "50000", "100000")
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
    }

    @Benchmark
    fun stdlib() = list.asSequence().distinctBy { it }.last()

    @Benchmark
    fun custom() = list.asSequence().optimizedDistinctBy { it }.last()
}


