package com.artnaseef.immutable.utils.builder;

import com.artnaseef.immutable.utils.MutationResult;
import com.artnaseef.immutable.utils.MutationResultType;

public class MutationResultBuilder {

    private MutationResultType mutationResultType = MutationResultType.WALK_CHILDREN;
    private Object replacementValue = null;

    public MutationResultBuilder replacementValue(Object replacementValue) {
        this.replacementValue = replacementValue;
        this.mutationResultType = MutationResultType.CHANGED;
        return this;
    }

    // Alias
    public MutationResultBuilder of(Object replacementValue) {
        return this.replacementValue(replacementValue);
    }

    public MutationResultBuilder ofType(MutationResultType mutationResultType) {
        this.mutationResultType = mutationResultType;

        return this;
    }

    public MutationResult build() {
        return new MutationResult(this.mutationResultType, this.replacementValue);
    }
}
