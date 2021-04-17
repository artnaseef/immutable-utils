package com.artnaseef.immutable.utils;

import com.artnaseef.immutable.utils.builder.MutationResultBuilder;

public class MutationResult {

    public static final MutationResult UNCHANGED_MUTATION_RESULT = new MutationResult(MutationResultType.UNCHANGED, null);

    private final MutationResultType resultType;
    private final Object replacementValue;

    public MutationResult(MutationResultType resultType, Object replacementValue) {
        this.resultType = resultType;
        this.replacementValue = replacementValue;
    }

    public MutationResultType getResultType() {
        return resultType;
    }

    public Object getReplacementValue() {
        return this.replacementValue;
    }

//========================================
// Builder Pattern Helpers
//----------------------------------------

    public static MutationResultBuilder of(Object replacementValue) {
        return new MutationResultBuilder().of(replacementValue);
    }

    public static MutationResultBuilder ofType(MutationResultType mutationResultType) {
        return new MutationResultBuilder().ofType(mutationResultType);
    }
}
