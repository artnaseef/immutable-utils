package com.artnaseef.immutable.utils;

import java.util.function.Supplier;

/**
 * A supplier that wraps another supplier and caches the result of the delegate, ensuring it is called only once
 * regardless of the number of times this supplier is called.
 *
 * @param <T>
 */
class MemoizeSupplier<T> implements Supplier<T> {

    private final Object lock = new Object();

    private final Supplier<T> computation;
    private T value;
    private boolean haveValue;

    public MemoizeSupplier(Supplier<T> computation) {
        this.computation = computation;
    }

    public T get() {
        synchronized (this.lock) {
            if (!this.haveValue) {
                this.value = this.computation.get();
                this.haveValue = true;
            }
        }

        return this.value;
    }
}
