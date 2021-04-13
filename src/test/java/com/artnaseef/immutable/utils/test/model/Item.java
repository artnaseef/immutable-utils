package com.artnaseef.immutable.utils.test.model;

import com.artnaseef.immutable.utils.MutationUtilsImmutableProperties;

@MutationUtilsImmutableProperties(properties = {"id", "name"})
public class Item {
    private final long id;
    private final String name;

    public Item(long id, String name) {
        this.id = id;
        this.name = name;
    }

    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}
