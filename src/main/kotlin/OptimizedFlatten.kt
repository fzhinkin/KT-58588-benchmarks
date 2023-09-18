package com.example.benchmark.optimized
// Implementation is based on https://github.com/maxssoft/optimized-sequences
/**
 * Optimized flatten sequence function
 *
 * Optimization 1 - removed nullable type for variable [itemIterator]
 * Kotlin add extended read/write operations for every read/write nullable variable
 * I added singleton object [EmptyIterator] and it's replaced case for null value
 *
 * Optimization 2 - I added state of calculation itemIterator variable
 * It's reduced the count of calls heavy function [ensureItemIterator]
 *
 * Optimized function works on 35-40% faster than standard implementation of flatten function
 * Look microbenchmark test [com.example.benchmark.OptimizedFlattenTest]
 *
 * @author Max Sidorov on 21.04.2023
 */

fun <T> Sequence<Iterable<T>>.optimizedFlatten(): Sequence<T> = optimizedFlatten { it.iterator() }

private fun <T, R> Sequence<T>.optimizedFlatten(iterator: (T) -> Iterator<R>): Sequence<R> {
    return OptimizedFlatteningSequence(this, { it }, iterator)
}

fun <T> Sequence<Iterable<T>>.builtFlatten(): Sequence<T> = sequence {
    this@builtFlatten.forEach {
        yieldAll(it)
    }
}

// Empty iterator for cause when we haven't next element
private object EmptyIterator : Iterator<Nothing> {
    override fun hasNext(): Boolean = false
    override fun next(): Nothing = throw NoSuchElementException()
}

internal class OptimizedFlatteningSequence<T, R, E>
constructor(
    private val sequence: Sequence<T>,
    private val transformer: (T) -> R,
    private val iterator: (R) -> Iterator<E>
) : Sequence<E> {

    override fun iterator(): Iterator<E> = object : Iterator<E> {
        private val iterator = sequence.iterator()
        private var itemIterator: Iterator<E> = EmptyIterator // optimization for exclude nullable variable
        private var state: Int = UNDEFINED_STATE // { UNDEFINED_STATE, HAS_NEXT_ITEM, HAS_FINISHED }

        override fun next(): E {
            if (state == UNDEFINED_STATE) { // optimized typical cause hasNext() + next()
                ensureItemIterator()
            }
            state = UNDEFINED_STATE
            return itemIterator.next()
        }

        override fun hasNext(): Boolean {
            return when (state) { // optimized cause for multiple call hasNext()
                HAS_NEXT_ITEM -> true
                HAS_FINISHED -> false
                else -> ensureItemIterator()
            }
        }

        private fun ensureItemIterator(): Boolean {
            if (itemIterator.hasNext()) {
                state = HAS_NEXT_ITEM
                return true
            } else {
                while (iterator.hasNext()) {
                    val nextItemIterator = iterator(transformer(iterator.next()))
                    if (nextItemIterator.hasNext()) {
                        itemIterator = nextItemIterator
                        state = HAS_NEXT_ITEM
                        return true
                    }
                }
                state = HAS_FINISHED
                itemIterator = EmptyIterator
                return false
            }
        }
    }
}

private const val UNDEFINED_STATE = -1 // next item undefined
private const val HAS_NEXT_ITEM = 0 // has next item
private const val HAS_FINISHED = 1 // has finished iteration

internal class OptimizedFlatteningSequenceUsingAbstractIterator<T, R, E>
constructor(
    private val sequence: Sequence<T>,
    private val transformer: (T) -> R,
    private val iterator: (R) -> Iterator<E>
) : Sequence<E> {
    override fun iterator(): Iterator<E> = object : AbstractIteratorOpt<E>() {
        private val seqIter = sequence.iterator()
        private var iter: Iterator<E>? = null

        override fun computeNext() {
            val it = iter
            if (it != null && it.hasNext()) {
                setNext(it.next())
                return
            }
            val sq = seqIter
            while (sq.hasNext()) {
                val next = iterator(transformer(sq.next()))
                if (next.hasNext()) {
                    iter = next
                    setNext(next.next())
                    return
                }
            }
            done()
        }
    }

}

fun <T> Sequence<Iterable<T>>.optimizedFlatten4(): Sequence<T> = optimizedFlatten4 { it.iterator() }

private fun <T, R> Sequence<T>.optimizedFlatten4(iterator: (T) -> Iterator<R>): Sequence<R> {
    return OptimizedFlatteningSequenceUsingAbstractIterator(this, { it }, iterator)
}

internal class FlatteningSequenceOpt<T, R, E>
constructor(
    private val sequence: Sequence<T>,
    private val transformer: (T) -> R,
    private val iterator: (R) -> Iterator<E>
) : Sequence<E> {
    private object State {
        const val UNKNOWN = 0
        const val READY = 1
        const val DONE = 2
    }

    override fun iterator(): Iterator<E> = object : Iterator<E> {
        val iterator = sequence.iterator()
        var itemIterator: Iterator<E>? = null
        private var state = State.UNKNOWN

        override fun next(): E {
            if (!ensureItemIterator())
                throw NoSuchElementException()
            state = State.UNKNOWN
            return itemIterator!!.next()
        }

        override fun hasNext(): Boolean {
            return ensureItemIterator()
        }

        private fun ensureItemIterator(): Boolean {
            if (state == State.DONE) return false
            if (state == State.READY) return true

            val ii = itemIterator
            if (ii != null && ii.hasNext()) {
                state = State.READY
                return true
            }

            while (iterator.hasNext()) {
                val element = iterator.next()
                val nextItemIterator = iterator(transformer(element))
                if (nextItemIterator.hasNext()) {
                    itemIterator = nextItemIterator
                    state = State.READY
                    return true
                }
            }

            state = State.DONE
            return false
        }
    }
}

fun <T> Sequence<Iterable<T>>.optimizedFlatten5(): Sequence<T> = optimizedFlatten5 { it.iterator() }

private fun <T, R> Sequence<T>.optimizedFlatten5(iterator: (T) -> Iterator<R>): Sequence<R> {
    return FlatteningSequenceOpt(this, { it }, iterator)
}
