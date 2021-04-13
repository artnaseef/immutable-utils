package com.artnaseef.immutable.utils;

import java.util.LinkedList;
import java.util.function.Supplier;

public interface Mutator {

    /**
     *
     * @param ancestry list of ancestors of the value to (possibly) mutate.
     * @param propertyNames
     * @param valueSupplier
     * @return
     */
    MutationResult mutate(LinkedList<Object> ancestry, LinkedList<String> propertyNames, Supplier valueSupplier);
}
