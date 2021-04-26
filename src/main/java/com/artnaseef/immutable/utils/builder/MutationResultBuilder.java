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
