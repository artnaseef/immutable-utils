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

import com.artnaseef.immutable.utils.test.model.Item;
import com.artnaseef.immutable.utils.test.model.MissingGetter;
import com.artnaseef.immutable.utils.test.model.RootState;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class MutationUtilsTest {

    private MutationUtils mutationUtils;

    private RootState testRootState;
    private Mutator myMutator;

    @Before
    public void setUp() throws Exception {
        this.mutationUtils = new MutationUtils();

        this.testRootState = new RootState(2, 5, Arrays.asList(
                new Item(13, "thirteen"),
                new Item(23, "twenty three")
        ));

        this.myMutator = (ancestry, propertyNames, valueSupplier) -> {
            if (mutationUtils.checkPath(ancestry, propertyNames, RootState.class, "itemList", List.class)) {
                Item item = (Item) valueSupplier.get();

                if (item.getId() == 13) {
                    return MutationResult.of(new Item(14, "fourteen")).build();
                }
            }

            if ((ancestry.getLast() instanceof RootState) && (propertyNames.getLast().equals("itemList"))) {
                return MutationResult.ofType(MutationResultType.WALK_CHILDREN).build();
            }

            if (ancestry.getLast() instanceof List) {
                return MutationResult.ofType(MutationResultType.WALK_CHILDREN).build();
            }

            return MutationResult.ofType(MutationResultType.UNCHANGED).build();
        };
    }

    @Test
    public void mutateDeep() {
        //
        // Setup Test Data and Interactions
        //

        //
        // Execute
        //
        RootState variant1 = this.mutationUtils.mutateDeep(this.testRootState, this.myMutator);
        RootState variant2 = this.mutationUtils.mutateDeep(variant1, this.myMutator);

        //
        // Verify the Results
        //
        assertNotEquals(System.identityHashCode(variant1), System.identityHashCode(this.testRootState));
        assertEquals(13, this.testRootState.getItemList().get(0).getId());
        assertEquals(14, variant1.getItemList().get(0).getId());

        assertEquals(System.identityHashCode(variant1), System.identityHashCode(variant2));
    }

    @Test
    public void testMutateNull() {
        Object result = this.mutationUtils.mutateDeep(null, this.myMutator);
        assertNull(result);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testMutateMissingGetter() {
        //
        // Setup Test Data and Interactions
        //
        MissingGetter missingGetter = new MissingGetter(1, 2, Collections.EMPTY_LIST);

        //
        // Execute
        //
        this.mutationUtils.mutateDeep(missingGetter, this.myMutator);
        fail("missing expected exception");
    }


    /**
     * List with multiple entries.  Most unchanged.  One changed directly, one using "child walk".
     *
     * Initial Sequence
     *      U U C U U WC WU U U U WC
     * U = unchanged
     * C = changed immediate (no child-walk)
     * WC = child-walk + change
     * WU = child-walk + unchanged
     */
    @Test
    public void testListOrderUnchanged() {
        //
        // Setup Test Data and Interactions
        //
        RootState rootState = new RootState(3, 5, Arrays.asList(
                new Item( 1, "U.1"),
                new Item( 2, "U.2"),
                new Item( 3, "C.1"),
                new Item( 4, "U.3"),
                new Item( 5, "U.4"),
                new Item( 6, "WC.1"),
                new Item( 7, "WU.1"),
                new Item( 8, "U.5"),
                new Item( 9, "U.6"),
                new Item(10, "U.7"),
                new Item(11, "WC.2")
        ));


        Mutator mutateMyList = (ancestry, propertyNames, valueSupplier) -> {
            if (mutationUtils.checkPath(ancestry, propertyNames, RootState.class, "itemList", List.class)) {
                Item item = (Item) valueSupplier.get();

                if (item.getName().startsWith("U.")) {
                    return MutationResult.ofType(MutationResultType.UNCHANGED).build();
                } else if (item.getName().startsWith("C.")) {
                    Item replacement = new Item(item.getId() + 100, item.getName() + "*");
                    return MutationResult.ofType(MutationResultType.CHANGED).of(replacement).build();
                } else {
                    return MutationResult.ofType(MutationResultType.WALK_CHILDREN).build();
                }
            } else if (ancestry.getLast() instanceof Item) {
                Item parentItem = (Item) ancestry.getLast();
                if (parentItem.getName().startsWith("WU")) {
                    return MutationResult.ofType(MutationResultType.UNCHANGED).build();
                } else {
                    if (propertyNames.getLast().equals("id")) {
                        long replacement = parentItem.getId() + 100;
                        return MutationResult.ofType(MutationResultType.CHANGED).of(replacement).build();
                    } else if (propertyNames.getLast().equals("name")) {
                        String replacement = parentItem.getName() + "*";
                        return MutationResult.ofType(MutationResultType.CHANGED).of(replacement).build();
                    }
                }
            }

            if ((ancestry.getLast() instanceof RootState) && (propertyNames.getLast().equals("itemList"))) {
                return MutationResult.ofType(MutationResultType.WALK_CHILDREN).build();
            }

            if (ancestry.getLast() instanceof List) {
                return MutationResult.ofType(MutationResultType.WALK_CHILDREN).build();
            }

            return MutationResult.ofType(MutationResultType.UNCHANGED).build();
        };


        //
        // Execute
        //
        RootState variant = this.mutationUtils.mutateDeep(rootState, mutateMyList);

        //
        // Verify the Results
        //
        assertEquals(  1, variant.getItemList().get(0).getId());
        assertEquals(  2, variant.getItemList().get(1).getId());
        assertEquals(103, variant.getItemList().get(2).getId());
        assertEquals(  4, variant.getItemList().get(3).getId());
        assertEquals(  5, variant.getItemList().get(4).getId());
        assertEquals(106, variant.getItemList().get(5).getId());
        assertEquals(  7, variant.getItemList().get(6).getId());
        assertEquals(  8, variant.getItemList().get(7).getId());
        assertEquals(  9, variant.getItemList().get(8).getId());
        assertEquals( 10, variant.getItemList().get(9).getId());
        assertEquals(111, variant.getItemList().get(10).getId());

        assertEquals("U.1",   variant.getItemList().get(0).getName());
        assertEquals("U.2",   variant.getItemList().get(1).getName());
        assertEquals("C.1*",  variant.getItemList().get(2).getName());
        assertEquals("U.3",   variant.getItemList().get(3).getName());
        assertEquals("U.4",   variant.getItemList().get(4).getName());
        assertEquals("WC.1*", variant.getItemList().get(5).getName());
        assertEquals("WU.1",  variant.getItemList().get(6).getName());
        assertEquals("U.5",   variant.getItemList().get(7).getName());
        assertEquals("U.6",   variant.getItemList().get(8).getName());
        assertEquals("U.7",   variant.getItemList().get(9).getName());
        assertEquals("WC.2*", variant.getItemList().get(10).getName());

        assertSame(variant.getItemList().get(0), rootState.getItemList().get(0));
        assertSame(variant.getItemList().get(1), rootState.getItemList().get(1));
        assertSame(variant.getItemList().get(3), rootState.getItemList().get(3));
        assertSame(variant.getItemList().get(4), rootState.getItemList().get(4));
        assertSame(variant.getItemList().get(6), rootState.getItemList().get(6));
        assertSame(variant.getItemList().get(7), rootState.getItemList().get(7));
        assertSame(variant.getItemList().get(8), rootState.getItemList().get(8));
        assertSame(variant.getItemList().get(9), rootState.getItemList().get(9));

        assertNotSame(variant.getItemList().get(2),  rootState.getItemList().get(2));
        assertNotSame(variant.getItemList().get(5),  rootState.getItemList().get(5));
        assertNotSame(variant.getItemList().get(10), rootState.getItemList().get(10));
    }

    @Test
    public void testCheckPathSingles() {
        //
        // Setup Test Data and Interactions
        //
        RootState rootState = new RootState(3, 5, Arrays.asList(
                new Item( 1, "U.1"),
                new Item( 2, "C.1")
        ));
        List<Object> ancestry1 = Arrays.asList(rootState, rootState.getItemList());
        List<String> propertyNames1 = Arrays.asList("itemList", "0");

        //
        // Execute
        //
        boolean result1 = this.mutationUtils.checkPath(ancestry1, propertyNames1, List.class, "0");
        boolean result2 = this.mutationUtils.checkPath(ancestry1, propertyNames1, List.class, "no_such_thing");
        boolean result3 = this.mutationUtils.checkPath(ancestry1, propertyNames1, String.class, "0");

        //
        // Verify the Results
        //
        assertTrue(result1);
        assertFalse(result2);
        assertFalse(result3);
    }

    @Test
    public void testCheckPathWildcards() {
        //
        // Setup Test Data and Interactions
        //
        RootState rootState = new RootState(3, 5, Arrays.asList(
                new Item( 1, "U.1"),
                new Item( 2, "C.1")
        ));
        List<Object> ancestry1 = Arrays.asList(rootState, rootState.getItemList());
        List<String> propertyNames1 = Arrays.asList("itemList", "0");

        //
        // Execute
        //
        boolean result1 = this.mutationUtils.checkPath(ancestry1, propertyNames1, null, null);
        boolean result2 = this.mutationUtils.checkPath(ancestry1, propertyNames1, List.class, null);
        boolean result3 = this.mutationUtils.checkPath(ancestry1, propertyNames1, null, "0");
        boolean result4 = this.mutationUtils.checkPath(ancestry1, propertyNames1, null, null, null, null);
        boolean result5 = this.mutationUtils.checkPath(ancestry1, propertyNames1, RootState.class, null, List.class, null);
        boolean result6 = this.mutationUtils.checkPath(ancestry1, propertyNames1, null, "itemList", null, "0");

        //
        // Verify the Results
        //
        assertTrue(result1);
        assertTrue(result2);
        assertTrue(result3);
        assertTrue(result4);
        assertTrue(result5);
        assertTrue(result6);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCheckPathNonClassArg() {
        //
        // Setup Test Data and Interactions
        //
        List<Object> ancestry1 = Arrays.asList("first", "second");
        List<String> propertyNames1 = Arrays.asList("1st", "2nd");

        //
        // Execute
        //
        this.mutationUtils.checkPath(ancestry1, propertyNames1, Object.class, null, "not-a-class", null);

        //
        // Verify the Results
        //
        fail("Expected an exception to be thrown");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCheckPathNonStringArg() {
        //
        // Setup Test Data and Interactions
        //
        List<Object> ancestry1 = Arrays.asList("first", "second");
        List<String> propertyNames1 = Arrays.asList("1st", "2nd");

        //
        // Execute
        //
        this.mutationUtils.checkPath(ancestry1, propertyNames1, null, "1st", null, 123);

        //
        // Verify the Results
        //
        fail("Expected an exception to be thrown");
    }

    @Test
    public void testWalkPathMutator() {
        //
        // Setup Test Data and Interactions
        //

        //
        // Execute
        //
        Mutator walkPathMutator = this.mutationUtils.makeAnchoredPathMutator((getExisting) -> getExisting.get() + "*", RootState.class, "itemList", List.class, "0", Item.class, "name");
        RootState variant = this.mutationUtils.mutateDeep(this.testRootState, walkPathMutator);

        //
        // Verify the Results
        //
        assertEquals(13, variant.getItemList().get(0).getId());
        assertEquals("thirteen*", variant.getItemList().get(0).getName());
        assertEquals(23, variant.getItemList().get(1).getId());
        assertEquals("twenty three", variant.getItemList().get(1).getName());

        assertSame(this.testRootState.getItemList().get(1), variant.getItemList().get(1));
    }
}