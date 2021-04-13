package com.artnaseef.immutable.utils;

import com.artnaseef.immutable.exception.CannotConstructException;
import com.artnaseef.immutable.exception.CannotMutateException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Utilities for mutating the state of complex immutable objects by copying existing objects where feasible, and
 * constructing new ones in the hierarchy, as-needed, to implement desired mutations.
 *
 * LIMITATIONS:
 *  - All objects processed here are expected to be immutable and use immutable fields.
 *      - Fields in immutable objects are always declared final.
 *      - Fields in immutable objects are always immutable themselves.
 *  - Lists MUST be declared as List type (e.g. not ArrayList nor LinkedList)
 *  - The class declaration of all objects to be processed MUST include the MutationUtilsImmutableProperties.
 *      - The ORDER of the properties in the annotation MUST match the order they are provided to the Constructor.
 *
 * TODO:
 *  - Removal of list entries as a MutationResultType?  REMOVE - sets fields to null, or drops from collections
 *  - Support for Map and Set built-ins
 *  - Use model object to carry around state of the tree walk
 */
@SuppressWarnings("unchecked")
public class MutationUtils {

    private static final Logger DEFAULT_LOGGER = LoggerFactory.getLogger(MutationUtils.class);

    private Logger log = DEFAULT_LOGGER;

    private MutatorUtils mutatorUtils = new MutatorUtils();


    /**
     * Mutate the complex data structure given using the provided Mutator to choose the paths through the structure's
     * hierarchy to visit and modify.  Produces a copy of the original structure with minimal changes needed to produce
     * the mutations, thereby minimizing the additional memory needed.
     *
     * For example, if a list with 1000 entries changes the element at position 3, a new list is created using the
     * same entries for all 999 elements that didn't change, and the 1 new element that did change.
     *
     * @param root
     * @param mutator
     * @param <T>
     * @return
     */
    public <T> T mutateDeep(T root, Mutator mutator) {
        return this.mutateDeep(new LinkedList<>(), new LinkedList<>(), root, mutator);
    }

    /**
     * Mutate the complex data structure given using the provided Mutator to choose the paths through the structure's
     * hierarchy to visit and modify.  Produces a copy of the original structure with minimal changes needed to produce
     * the mutations, thereby minimizing the additional memory needed.  Takes the ancestry and property names accessed
     * to get to the current node.
     *
     * For example, if a list with 1000 entries changes the element at position 3, a new list is created using the
     * same entries for all 999 elements that didn't change, and the 1 new element that did change.
     *
     * The convenience version of this method is recommended unless there is a real reason to get in deep.
     *
     * @see #mutateDeep(Object, Mutator)
     *
     * @param ancestry list of ancestor objects (from the root) to the current node's immediate parent.  Empty list means
     *                 this node is the root node.
     * @param propertyNames list of property names accessed to get to the current node.  propertyNames.get(0) is the
     *                      name of the field in ancestry.get(0) to get to ancestry.get(1).  Empty list means this is the
     *                      root node.
     * @param node the current node being processed with the mutator.
     * @param mutator node visitor that decides whether to mutate the current node, and whether to continue walking the
     *                children of this node.
     * @param <T> the type of the current node being processed.
     * @return either the original node, or a copy of the node with mutated values populated.  Either way, the result
     *         is an immutable object (assuming no "cheats").
     */
    public <T> T mutateDeep(LinkedList<Object> ancestry, LinkedList<String> propertyNames, T node, Mutator mutator) {
        if (node == null) {
            return null;
        }

        MutationUtilsImmutableProperties mutationUtilsImmutableProperties = node.getClass().getAnnotation(MutationUtilsImmutableProperties.class);

        //
        // Class not marked with ImmutableProperties.  Try built-in classes that are supported (e.g. List).
        //
        if (mutationUtilsImmutableProperties == null) {
            return this.mutateDeepBuiltIn(ancestry, propertyNames, node, mutator);
        }

        //
        // Walk the properties and check for mutation on each of them.
        //
        boolean changed = false;
        Map<String, Supplier<Object>> updateMapper = new HashMap<>();
        Map<String, Supplier> childrenToWalkMap = new HashMap<>();

        for (String propertyName : mutationUtilsImmutableProperties.properties()) {
            boolean propertyChanged =
                    this.mutateProperty(
                            ancestry,
                            propertyNames,
                            node,
                            mutator,
                            updateMapper,
                            childrenToWalkMap,
                            propertyName);

            if (propertyChanged) {
                changed = true;
            }
        }

        //
        // Walk all of the children requested now.
        //
        for (Map.Entry<String, Supplier> childPropertyToWalk : childrenToWalkMap.entrySet()) {
            boolean childChanged =
                this.walkChildProperty(ancestry, propertyNames, node, mutator, changed, updateMapper, childPropertyToWalk);

            if (childChanged) {
                changed = true;
            }
        }

        //
        // If anything changed, construct a new instance with the updated values.
        //
        if (changed) {
            return this.constructNewInstanceWithProperties((Class<T>) node.getClass(), updateMapper);
        }

        // No Changes
        return node;
    }

    /**
     * Mutate a built-in node type using the given mutator.  This method can be called directly, but generally calling
     * mutateDeep is the right starting point.
     *
     * Note: currently only supports List types.
     *
     * @see #mutateDeep(Object, Mutator)
     *
     * @param ancestry list of ancestor objects (from the root) to the current node's immediate parent.  Empty list means
     *                 this node is the root node.
     * @param propertyNames list of property names accessed to get to the current node.  propertyNames.get(0) is the
     *                      name of the field in ancestry.get(0) to get to ancestry.get(1).  Empty list means this is the
     *                      root node.
     * @param node the current node being processed with the mutator.
     * @param mutator node visitor that decides whether to mutate the current node, and whether to continue walking the
     *                children of this node.
     * @param <T> the type of the current node being processed.
     * @return either the original node, or a copy of the node with mutated values populated.  Either way, the result
     *         is an immutable object (assuming no "cheats").
     */
    public <T> T mutateDeepBuiltIn(LinkedList<Object> ancestry, LinkedList<String> propertyNames, T node, Mutator mutator) {
        if (node instanceof List) {
            return (T) this.mutateDeepList(ancestry, propertyNames, (List) node, mutator);
        }

        this.log.error("Request to mutateDeep() on a class lacking ImmutableProperties and lacking built-in support; keeping existing value: class={}", node.getClass().getName());
        return node;
    }

    /**
     * Mutate a List node using the given mutator.  This method can be called directly, but generally calling
     * mutateDeep is the right starting point.
     *
     * NOTE: only List type elements are supported as the result is wrapped with Collection.unmodifiableList().  Model
     *       object list fields must support List to use these tools.  Other forms of immutable lists are not supported.
     *
     * @see #mutateDeep(Object, Mutator)
     *
     * @param ancestry
     * @param propertyNames
     * @param node
     * @param mutator
     * @param <T>
     * @return
     */
    public <T> List<T> mutateDeepList(LinkedList<Object> ancestry, LinkedList<String> propertyNames, List<T> node, Mutator mutator) {
        boolean changed = false;
        Map<String, Supplier<Object>> updateMapper = new LinkedHashMap<>();
        Map<String, Supplier> childrenToWalkMap = new LinkedHashMap<>();

        int cur = 0;
        while (cur < node.size()) {
            T child = node.get(cur);
            boolean elementChanged =
                    this.mutatePropertyWithSupplier(
                            ancestry,
                            propertyNames,
                            node,
                            mutator,
                            updateMapper,
                            childrenToWalkMap,
                            Integer.toString(cur),
                            () -> child
                    );

            if (elementChanged) {
                changed = true;
            }

            cur++;
        }

        //
        // Walk all of the children requested now.
        //
        for (Map.Entry<String, Supplier> childPropertyToWalk : childrenToWalkMap.entrySet()) {
            boolean childChanged =
                    this.walkChildProperty(ancestry, propertyNames, node, mutator, changed, updateMapper, childPropertyToWalk);

            if (childChanged) {
                changed = true;
            }
        }

        if (changed) {
            List<T> resultList =
                    updateMapper.entrySet().stream()
                            .map((entry) -> (T) entry.getValue().get())
                            .collect(Collectors.toList())
            ;

            return Collections.unmodifiableList(resultList);
        }

        return node;
    }

    /**
     * Check the given path of ancestor objects and property names against the path to the current entry.  Matches
     *  against the end of the path and not the beginning (unless, of course, the full path is specified).  Don't be
     *  tricked by the more argument - keep alternating Class and String arguments.  For example:
     *
     *      checkPath(ancestry, propertyNames, StorageChest.class, "itemList", List.class, null, Item.class, "itemType")
     *
     *
     * Passes through to the MutatorUtils method; see there for more information.
     *
     * @see MutatorUtils#checkPath(List, List, Class, String, Object...)
     */
    public boolean checkPath(List<Object> ancestry, List<String> propertyNames, Class earliestAncestorToCheck, String earliestPropertyNameToCheck, Object... more) {
        return this.mutatorUtils.checkPath(ancestry, propertyNames, earliestAncestorToCheck, earliestPropertyNameToCheck, more);
    }

    /**
     * Determine if the path specified by the given Classes and Property-Names is at least partially matched by the
     * ancestry and property names of the current node, so we know whether to continue walking down the tree from this
     * node to find a match.  The path matching is anchored from the root node, so the entire match must apply from the
     * root of the tree.
     *
     * Passes through to the MutatorUtils method; see there for more information.
     *
     * @see MutatorUtils#isInAnchoredPath(List, List, Class, String, Object...)
     */
    public boolean isInAnchoredPath(List<Object> ancestry, List<String> propertyNames, Class earliestAncestorToCheck, String earliestPropertyNameToCheck, Object... more) {
        return this.mutatorUtils.isInAnchoredPath(ancestry, propertyNames, earliestAncestorToCheck, earliestPropertyNameToCheck, more);
    }

    /**
     * Convenience method for creating a mutator that targets a specific element in an object hierarchy.
     *
     * Passes through to the MutatorUtils method; see there for more information.
     *
     * @see MutatorUtils#makeAnchoredPathMutator(Function, Class, String, Object...)
     */
    public Mutator makeAnchoredPathMutator(Function<Supplier<Object>, Object> leafValueCalculator, Class earliestAncestorToCheck, String earliestPropertyNameToCheck, Object... more) {
        return this.mutatorUtils.makeAnchoredPathMutator(leafValueCalculator, earliestAncestorToCheck, earliestPropertyNameToCheck, more);
    }

//========================================
// Internals
//----------------------------------------

    // TBD: consider using a model object to track the state of the mutation tree walk
    private <T>
    boolean
    mutateProperty(
            LinkedList<Object> ancestry,
            LinkedList<String> propertyNames,
            T parentNode,
            Mutator mutator,
            Map<String, Supplier<Object>> updateMapper,
            Map<String, Supplier> childrenToWalkMap,
            String propertyName) {

        Method getter = this.locateGetter(parentNode.getClass(), propertyName);

        if (getter == null) {
            throw new IllegalArgumentException("missing getter: property=" + propertyName + "; class=" + parentNode.getClass().getName());
        }

        return this.mutatePropertyWithSupplier(
                ancestry,
                propertyNames,
                parentNode,
                mutator,
                updateMapper,
                childrenToWalkMap,
                propertyName,
                () -> this.readFieldWithGetter(parentNode, getter, propertyName)
        );
    }

    private <T>
    boolean
    mutatePropertyWithSupplier(
            LinkedList<Object> ancestry,
            LinkedList<String> propertyNames,
            T parentNode,
            Mutator mutator,
            Map<String, Supplier<Object>> updateMapper,
            Map<String, Supplier> childrenToWalkMap,
            String propertyName,
            Supplier propertySupplier
    ) {


        //
        // Prepare the information for the mutator.
        //
        LinkedList<Object> newAncestry = this.newLinkedListWithAdd(ancestry, parentNode);
        LinkedList<String> newPropertyNames = this.newLinkedListWithAdd(propertyNames, propertyName);

        MemoizeSupplier memoizeSupplier = new MemoizeSupplier(propertySupplier);


        //
        // Always add every property to the updater because it will be used to construct new parent object instances
        //  and needs all of the properties.
        //
        updateMapper.put(propertyName, memoizeSupplier);


        //
        // Apply the mutator now.
        //
        MutationResult mutationResult = mutator.mutate(newAncestry, newPropertyNames, memoizeSupplier);

        //
        // Handle the outcome.
        //
        boolean changed = processMutatorOutcome(updateMapper, childrenToWalkMap, propertyName, memoizeSupplier, mutationResult);

        return changed;
    }


    /**
     * Walk the properties of the child property given.  This method recursively calls mutateDeep() and handles the
     * result.
     *
     * @param ancestry
     * @param propertyNames
     * @param node
     * @param mutator
     * @param changed
     * @param updateMapper
     * @param childPropertyToWalk
     * @param <T>
     * @return
     */
    private <T>
    boolean
    walkChildProperty(
            LinkedList<Object> ancestry,
            LinkedList<String> propertyNames,
            T node,
            Mutator mutator,
            boolean changed,
            Map<String, Supplier<Object>> updateMapper,
            Map.Entry<String, Supplier> childPropertyToWalk) {

        String propertyName = childPropertyToWalk.getKey();
        Supplier valueSupplier = childPropertyToWalk.getValue();

        LinkedList<Object> newAncestry = this.newLinkedListWithAdd(ancestry, node);
        LinkedList<String> newPropertyNames = this.newLinkedListWithAdd(propertyNames, propertyName);

        Object child = valueSupplier.get();
        Object updatedChild = this.mutateDeep(newAncestry, newPropertyNames, child, mutator);

        // If the child changed, record the change.  Using != here works perfectly because the child is immutable;
        //  the same instance will, therefore, surely be unchanged.  A new instance could actually contain the same
        //  contents as well - but that's up to the mutator to avoid as appropriate.
        if (updatedChild != child) {
            changed = true;
            updateMapper.put(propertyName, () -> updatedChild);
        }
        return changed;
    }

    /**
     * Given the mutator was run against a property, apply the results to the mutation process.
     *
     * @param updateMapper map keyed by property name that holds suppliers of the property value (either original or
     *                    mutated)
     * @param childrenToWalkMap map of children that will be walked later keyed by the property name for the child.
     * @param propertyName name of the property that finished being processed by the mutator.
     * @param memoizeSupplier supplier of the value for this property.
     * @param mutationResult result indicating how to handle the property.
     * @return true => if the value changed; false => if the value did not change yet; when the outcome is
     * "WALK_CHILDREN", false is returned.
     */
    private boolean processMutatorOutcome(Map<String, Supplier<Object>> updateMapper, Map<String, Supplier> childrenToWalkMap, String propertyName, MemoizeSupplier memoizeSupplier, MutationResult mutationResult) {
        boolean changed = false;

        switch (mutationResult.getResultType()) {
            case UNCHANGED:
                // The property remains unchanged.
                break;

            case CHANGED:
                // The property was changed; record the update.  Don't walk the children of the value as a new
                //  replacement has been given.
                changed = true;
                updateMapper.put(propertyName, mutationResult::getReplacementValue);
                break;

            case WALK_CHILDREN:
                // Walk the children of the property to update.  The result of that walk will be used as the
                //  replacement, if a change occurs.
                childrenToWalkMap.put(propertyName, memoizeSupplier);
                break;
        }

        return changed;
    }

    /**
     * Create a new linked list with the contents of the original list, and the given element added to the end.
     *
     * @param original list whose contents will be copied.
     * @param toAdd additional element that willl be added to the end of the resulting list.
     * @param <T> type of elements in the list.
     * @return new list with the contents of the original list plus the added element specified.
     */
    private <T> LinkedList<T> newLinkedListWithAdd(LinkedList<T> original, T toAdd) {
        LinkedList<T> result = new LinkedList<>(original);
        result.add(toAdd);

        return result;
    }

    /**
     * Safely read a field using its getter.
     *
     * @param instance instance of the object from which to read the field.
     * @param getter method that gets the field value from the object.
     * @param propertyName name of the field being read.
     * @param <T> type of the field being read.
     * @return the value of the field returned by the getter.
     * @throws CannotMutateException if an exception is thrown on invoking the getter.
     */
    private <T> T readFieldWithGetter(Object instance, Method getter, String propertyName) {
        try {
            return (T) getter.invoke(instance);
        } catch (Exception exc) {
            this.log.info("Problem accessing property: property={}; readMethod={}", propertyName, getter.getName());
            throw new CannotMutateException("property name = " + propertyName, exc);
        }
    }

//========================================
// Deep Internals: Reflection
//----------------------------------------

    // TODO: consider caching the results of reflection

    /**
     *
     * @param clazz
     * @param propertySuppliers
     * @param <T>
     * @return
     */
    private <T> T constructNewInstanceWithProperties(Class<T> clazz, Map<String, Supplier<Object>> propertySuppliers) {
        try {
            Constructor constructor = this.findConstructor(clazz);
            if (constructor == null) {
                throw new CannotConstructException("Cannot construct instance; failed to find a suitable constructor: class=" + clazz.getName());
            }

            MutationUtilsImmutableProperties mutationUtilsImmutableProperties = clazz.getAnnotation(MutationUtilsImmutableProperties.class);

            //
            Object[] arguments = prepareConstructorArguments(propertySuppliers, mutationUtilsImmutableProperties);

            T instance = (T) constructor.newInstance(arguments);
            return instance;
        } catch (Exception exc) {
            throw new CannotConstructException("failed to construct new instance of class " + clazz.getName(), exc);
        }
    }

    /**
     * Prepare the arguments for the constructor using the class MutationUtilsImmutableProperties annotation which
     * specifies the fields and order.
     *
     * @param propertySuppliers
     * @param mutationUtilsImmutableProperties
     * @return
     */
    private Object[] prepareConstructorArguments(Map<String, Supplier<Object>> propertySuppliers, MutationUtilsImmutableProperties mutationUtilsImmutableProperties) {
        Object[] arguments = new Object[mutationUtilsImmutableProperties.properties().length];
        int cur = 0;

        for (String onePropertyName : mutationUtilsImmutableProperties.properties()) {
            Supplier<Object> newValueSupplier = propertySuppliers.get(onePropertyName);

            arguments[cur] = newValueSupplier.get();
            cur++;
        }

        return arguments;
    }

    /**
     * Find the constructor.  Expecting to only have 1.  If more than 1, we take the first one.  Not great logic, but
     * the expectation is that the model objects will be built to match this library.
     *
     * @param clazz
     * @return
     */
    private Constructor findConstructor(Class clazz) {
        Constructor[] constructors = clazz.getConstructors();

        // Really shouldn't do much here - the entire intent of this library is to work with classes that are built
        //  for its use, so a single constructor is expected at all times.
        if (constructors.length > 0) {
            return constructors[0];
        }

        return null;
    }

    private Method locateGetter(Class clazz, String fieldname) {
        String getterName = "get" + fieldname.substring(0, 1).toUpperCase() + fieldname.substring(1);

        Method method = this.getNamedMethodInHierarchy(clazz, getterName);

        return method;
    }

    private Method getNamedMethodInHierarchy(Class clazz, String methodName) {
        try {
            Method result = clazz.getDeclaredMethod(methodName);
            if (result != null) {
                return result;
            }
        } catch (NoSuchMethodException e) {
        }

        Class superClass = clazz.getSuperclass();
        if ((superClass != clazz) && (superClass != null)) {
            return this.getNamedMethodInHierarchy(superClass, methodName);
        }

        return null;
    }
}
