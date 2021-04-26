/*
 * Copyright (c) 2021 Arthur Naseef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
