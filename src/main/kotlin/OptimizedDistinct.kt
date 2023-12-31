package com.example.benchmark.optimized
// Implementation is based on https://github.com/maxssoft/optimized-sequences
/**
 * Optimized distinct sequence function
 *
 * Excluded AbstractIterator with virtual methods
 *
 * Working with state over Enum class replaced on Int
 * Using "when" condition with Enum class is slower on 10% as Int
 * article of Jake Wharton https://jakewharton.com/r8-optimization-enum-switch-maps/
 * Look microbenchmark test [com.example.benchmark.EnumTest]
 *
 * I developed this function such as implementation of FilteringSequence
 * and this implementation works on 15-18% faster than standard implementation
 * Look microbenchmark test [com.example.benchmark.OptimizedDistinctTest]
 *
 * @author Max Sidorov on 21.04.2023
 */

fun <T, K> Sequence<T>.optimizedDistinctBy(selector: (T) -> K): Sequence<T> {
    return OptimizedDistinctSequence(this, selector)
}

internal class OptimizedDistinctSequence<T, K>(private val source: Sequence<T>, private val keySelector: (T) -> K) : Sequence<T> {
    override fun iterator(): Iterator<T> = OptimizedDistinctIterator(source.iterator(), keySelector)
}

private class OptimizedDistinctIterator<T, K>(
    private val source: Iterator<T>, private val keySelector: (T) -> K
) : Iterator<T>{
    private val observed = HashSet<K>()
    // { UNDEFINED_STATE, HAS_NEXT_ITEM, HAS_FINISHED }
    private var nextState: Int = UNDEFINED_STATE
    private var nextItem: T? = null

    override fun hasNext(): Boolean {
        require(nextState != INVALID_STATE)
        if (nextState == UNDEFINED_STATE) {
            calcNext()
        }
        return nextState == HAS_NEXT_ITEM
    }

    override fun next(): T {
        require(nextState != INVALID_STATE)
        if (nextState == UNDEFINED_STATE) {
            calcNext()
        }
        if (nextState == HAS_FINISHED)
            throw NoSuchElementException()
        nextState = UNDEFINED_STATE
        return nextItem as T
    }

    private fun calcNext() {
        nextState = INVALID_STATE
        while (source.hasNext()) {
            val next = source.next()
            val key = keySelector(next)

            if (observed.add(key)) {
                nextItem = next
                nextState = HAS_NEXT_ITEM // found next item
                return
            }
        }
        nextState = HAS_FINISHED // end of iterator
    }
}

private const val UNDEFINED_STATE = -1 // next item undefined
private const val HAS_NEXT_ITEM = 0 // has next item
private const val HAS_FINISHED = 1 // has finished iteration
private const val INVALID_STATE = 2

private object State {
    const val READY: Int = 0
    const val NOT_READY: Int = 1
    const val DONE: Int = 2
    const val FAILED: Int = 3
}

public abstract class AbstractIteratorOpt<T> : Iterator<T> {
    private var state = State.NOT_READY
    private var nextValue: T? = null

    override fun hasNext(): Boolean {
        require(state != State.FAILED)
        return when (state) {
            State.DONE -> false
            State.READY -> true
            else -> tryToComputeNext()
        }
    }

    override fun next(): T {
        if (state == State.READY) {
            state = State.NOT_READY
            @Suppress("UNCHECKED_CAST")
            return nextValue as T
        }
        if (state == State.DONE || !tryToComputeNext()) {
            throw NoSuchElementException()
        }
        state = State.NOT_READY
        @Suppress("UNCHECKED_CAST")
        return nextValue as T
    }

    private fun tryToComputeNext(): Boolean {
        state = State.FAILED
        computeNext()
        return state == State.READY
    }

    protected abstract fun computeNext(): Unit

    protected fun setNext(value: T): Unit {
        nextValue = value
        state = State.READY
    }

    protected fun done() {
        state = State.DONE
    }
}

internal class DistinctSequenceOpt<T, K>(private val source: Sequence<T>, private val keySelector: (T) -> K) : Sequence<T> {
    override fun iterator(): Iterator<T> = DistinctIteratorOpt(source.iterator(), keySelector)
}


fun <T, K> Sequence<T>.stdlibOptDistinctBy(selector: (T) -> K): Sequence<T> {
    return DistinctSequenceOpt(this, selector)
}


private class DistinctIteratorOpt<T, K>(private val source: Iterator<T>, private val keySelector: (T) -> K) : AbstractIteratorOpt<T>() {
    private val observed = HashSet<K>()

    override fun computeNext() {
        while (source.hasNext()) {
            val next = source.next()
            val key = keySelector(next)

            if (observed.add(key)) {
                setNext(next)
                return
            }
        }

        done()
    }
}
