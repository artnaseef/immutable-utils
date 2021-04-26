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
