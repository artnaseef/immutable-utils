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

package com.artnaseef.immutable.utils.test.model;

import com.artnaseef.immutable.utils.MutationUtilsImmutableProperties;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

@MutationUtilsImmutableProperties(properties = {"x", "y", "itemList"})
public class RootState {
    private final int x;
    private final int y;
    private final List<Item> itemList;

    public RootState(int x, int y, List<Item> itemList) {
        this.x = x;
        this.y = y;

        // Copy the list and wrap it in "unmodifiable" and throw away the "key" (i.e. forget the reference to the
        //  underlying, modifiable version of the list.
        this.itemList = Collections.unmodifiableList(new LinkedList<>(itemList));
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public List<Item> getItemList() {
        return itemList;
    }
}
