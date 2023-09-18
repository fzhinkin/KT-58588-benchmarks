package com.example.benchmark.optimized

import kotlinx.benchmark.*

@State(Scope.Benchmark)
open class OptimizedFlattenBenchmarkWithTypePollution {
    @Param("1", "2", "3")
    var types: Int = 0

    @Param("32")
    var size: Int = 0

    private var seq: Sequence<Iterable<Int>> = emptySequence()

    @Setup
    fun setup() {
        val list = when(types) {
            1 -> listOf(Iterable0(size), Iterable0(size), Iterable0(size))
            2 -> listOf(Iterable0(size), Iterable1(size), Iterable0(size))
            3 -> listOf(Iterable0(size), Iterable1(size), Iterable2(size))
            else -> throw IllegalArgumentException()
        }
        seq = list.asSequence()
    }

    @Benchmark
    fun baseline() = seq.flatten().lastOrNull()

    @Benchmark
    fun optimized() = seq.optimizedFlatten().lastOrNull()


    @Benchmark
    fun optimizedUsingAbstractIterator() = seq.optimizedFlatten4().lastOrNull()

    @Benchmark
    fun optimizedReworkedIterator() = seq.optimizedFlatten5().lastOrNull()
}

class Iterable0(private val size: Int) : Iterable<Int> {
    override fun iterator(): Iterator<Int> = object : Iterator<Int> {
        private var count = 0

        override fun hasNext(): Boolean = count < size

        override fun next(): Int {
            return count++
        }
    }
}

class Iterable1(private val size: Int) : Iterable<Int> {
    override fun iterator(): Iterator<Int> = object : Iterator<Int> {
        private var count = 0

        override fun hasNext(): Boolean = count < size

        override fun next(): Int {
            return count++
        }
    }
}

class Iterable2(private val size: Int) : Iterable<Int> {
    override fun iterator(): Iterator<Int> = object : Iterator<Int> {
        private var count = 0

        override fun hasNext(): Boolean = count < size

        override fun next(): Int {
            return count++
        }
    }
}

class Iterable3(private val size: Int) : Iterable<Int> {
    override fun iterator(): Iterator<Int> = object : Iterator<Int> {
        private var count = 0

        override fun hasNext(): Boolean = count < size

        override fun next(): Int {
            return count++
        }
    }
}
