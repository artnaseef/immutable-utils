package com.artnaseef.immutable.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Utilities for creating mutators to use with MutationUtils.
 */
@SuppressWarnings("unchecked")
public class MutatorUtils {

    private static final Logger DEFAULT_LOGGER = LoggerFactory.getLogger(MutatorUtils.class);

    private Logger log = DEFAULT_LOGGER;

    /**
     * Convenience method for creating a mutator that targets a specific element in an object hierarchy.
     *
     * @param leafValueCalculator function that calculates the new leaf value given a supplier which provides the
     *                           original value, if needed.
     * @param earliestAncestorToCheck first ancestor in the tree to check; may be null to match any.
     * @param earliestPropertyNameToCheck first property name in the tree to check; may be null to match any.
     * @param more additional Class, String combinations that must be matched in the hierarchy, in order from left
     *             to right (aka top to bottom).  Each value may be null to match any one value.
     * @return the mutator that will target and update the specific element in the object hierarchy.
     */
    public Mutator makeAnchoredPathMutator(Function<Supplier<Object>, Object> leafValueCalculator, Class earliestAncestorToCheck, String earliestPropertyNameToCheck, Object... more) {
        return new Mutator() {
            @Override
            public MutationResult mutate(LinkedList<Object> ancestry, LinkedList<String> propertyNames, Supplier valueSupplier) {
                if (MutatorUtils.this.checkPath(ancestry, propertyNames, earliestAncestorToCheck, earliestPropertyNameToCheck, more)) {
                    Object result = leafValueCalculator.apply(valueSupplier);
                    return MutationResult.of(result).build();
                }

                // Do we need to keep walking down from this property?
                if (MutatorUtils.this.isInAnchoredPath(ancestry, propertyNames, earliestAncestorToCheck, earliestPropertyNameToCheck, more)) {
                    return MutationResult.ofType(MutationResultType.WALK_CHILDREN).build();
                }

                return MutationResult.ofType(MutationResultType.UNCHANGED).build();
            }
        };
    }
  
    
    /**
     * Check the given path of ancestor objects and property names against the path to the current entry.  Matches
     *  against the end of the path and not the beginning (unless, of course, the full path is specified).  Don't be
     *  tricked by the more argument - keep alternating Class and String arguments.  For example:
     *
     *      checkPath(ancestry, propertyNames, StorageChest.class, "itemList", List.class, null, Item.class, "itemType")
     *
     *  note that classes and property names can be null to match ANY element at that point in the path.
     *
     * @param ancestry
     * @param propertyNames
     * @param earliestAncestorToCheck
     * @param earliestPropertyNameToCheck
     * @param more
     * @return
     */
    public boolean checkPath(List<Object> ancestry, List<String> propertyNames, Class earliestAncestorToCheck, String earliestPropertyNameToCheck, Object... more) {
        List<Class> wantedClasses = new LinkedList<>();
        List<String> wantedPropertyNames = new LinkedList<>();

        this.prepareForCheckPath(wantedClasses, wantedPropertyNames, earliestAncestorToCheck, earliestPropertyNameToCheck, more);

        int classesOffset = ancestry.size() - wantedClasses.size();
        int propertyNamesOffset = propertyNames.size() - wantedPropertyNames.size();

        // Short circuit if the required lists are longer than the actual ones
        if (( classesOffset < 0 ) || ( propertyNamesOffset < 0)) {
            return false;
        }

        int cur = 0;
        while (cur < wantedClasses.size()) {
            Class oneWanted = wantedClasses.get(cur);
            if ((oneWanted != null) && (! oneWanted.isAssignableFrom(ancestry.get(cur + classesOffset).getClass()))) {
                return false;
            }

            cur++;
        }

        cur = 0;
        while (cur < wantedPropertyNames.size()) {
            String oneWanted = wantedPropertyNames.get(cur);
            if ((oneWanted != null) && (! oneWanted.equals(propertyNames.get(cur + classesOffset)))) {
                return false;
            }

            cur++;
        }

        return true;
    }

    /**
     * Determine if the path specified by the given Classes and Property-Names is at least partially matched by the
     * ancestry and property names of the current node, so we know whether to continue walking down the tree from this
     * node to find a match.  The path matching is anchored from the root node, so the entire match must apply from the
     * root of the tree.
     *
     * @param ancestry actual objects in the hierarchy of the current node.
     * @param propertyNames names of the properties accessed from the root node to the current node being checked.
     *                      propertyNames.get(0) is the name of the field in ancestry.get(0) that give the value
     *                      ancestry.get(1).  The last property name is the current field being checked; it's value is
     *                      not provided.
     * @param earliestAncestorToCheck class to match against the earliest ancestor.  The ancestor must be assignable to
     *                                the class to match.  The null value is treated as a wildcard - all objects match.
     * @param earliestPropertyNameToCheck property name to match against the easiest property.  The names must match.
     *                                    The null value is treated as a wildcard - all property names match.
     * @param more additional, alternating, Class and String arguments used to match subsequent ancestors and property
     *             names, respectively.
     * @return true if the path matches.
     */
    public boolean isInAnchoredPath(List<Object> ancestry, List<String> propertyNames, Class earliestAncestorToCheck, String earliestPropertyNameToCheck, Object... more) {
        List<Class> wantedClasses = new LinkedList<>();
        List<String> wantedPropertyNames = new LinkedList<>();

        this.prepareForCheckPath(wantedClasses, wantedPropertyNames, earliestAncestorToCheck, earliestPropertyNameToCheck, more);

        // To simplify the loop, add a wildcard to the end of the property-names list if it is shorter than the classes one
        if (wantedPropertyNames.size() < wantedClasses.size()) {
            wantedPropertyNames.add(null);
        }

        // Shortcut
        if (ancestry.size() > wantedClasses.size()) {
            return false;   // Cannot match - current path is beyond the one of interest
        }

        //
        // Iterate from the start and check for any mismatches up to the current node.
        //
        Iterator<Class> wantedClassIter = wantedClasses.iterator();
        Iterator<String> wantedPropertyNameIter = wantedPropertyNames.iterator();
        Iterator<Object> ancestryIter = ancestry.iterator();
        Iterator<String> propertyNamesIter = propertyNames.iterator();

        while (ancestryIter.hasNext()) {
            Class wantedClass = wantedClassIter.next();
            String wantedPropertyName = wantedPropertyNameIter.next();

            Object ancestor = ancestryIter.next();
            String propertyName = propertyNamesIter.next();

            if ((wantedClass != null) && (! (wantedClass.isInstance(ancestor)))) {
                return false;
            }

            if ((wantedPropertyName != null) && (! Objects.equals(wantedPropertyName, propertyName))) {
                return false;
            }
        }

        return true;
    }

//========================================
// Internals
//----------------------------------------

    private void prepareForCheckPath(List<Class> wantedClasses, List<String> wantedPropertyNames, Class earliestAncestorToCheck, String earliestPropertyNameToCheck, Object... more) {
        wantedClasses.add(earliestAncestorToCheck);
        wantedPropertyNames.add(earliestPropertyNameToCheck);

        if (more != null) {
            int cur = 0;
            while (cur < more.length) {
                Object classArg = more[cur];
                if (
                        (!(classArg instanceof Class)) &&
                                (classArg != null)) {

                    throw new IllegalArgumentException("VarArgs must be Class, String, Class, String, ...; have " + more[cur].getClass().getName() + " in place of class at pos " + cur);
                }
                wantedClasses.add((Class) classArg);

                if ((cur + 1) < more.length) {
                    Object stringArg = more[cur + 1];
                    if (
                            (!(stringArg instanceof String)) &&
                                    (stringArg != null)) {

                        throw new IllegalArgumentException("VarArgs must be Class, String, Class, String, ...; have " + stringArg.getClass().getName() + " in place of string at pos " + (cur + 1));
                    }
                    wantedPropertyNames.add((String) stringArg);
                }

                cur += 2;
            }
        }
    }
}
