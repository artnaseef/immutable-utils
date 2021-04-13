package com.artnaseef.immutable.utils.test.model;

import com.artnaseef.immutable.utils.MutationUtilsImmutableProperties;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

@MutationUtilsImmutableProperties(properties = {"x", "y", "itemList"})
public class MissingGetter {
    private final int x;
    private final int y;
    private final List<Item> itemList;

    public MissingGetter(int x, int y, List<Item> itemList) {
        this.x = x;
        this.y = y;

        // Copy the list and wrap it in "unmodifiable" and throw away the "key" (i.e. forget the reference to the
        //  underlying, modifiable version of the list.
        this.itemList = Collections.unmodifiableList(new LinkedList<>(itemList));
    }

    public int getX() {
        return x;
    }

    public List<Item> getItemList() {
        return itemList;
    }
}
