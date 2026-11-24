/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.spring.config.transpiler.generator;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Collectors;
import org.geoserver.spring.config.transpiler.context.TranspilationContext;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.beans.factory.config.ConstructorArgumentValues.ValueHolder;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.config.TypedStringValue;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.beans.factory.support.ManagedMap;
import org.springframework.core.ResolvableType;
import org.springframework.util.ClassUtils;

/**
 * Chooses the constructor called for a bean of the XML, the way Spring chooses it when it reads the XML.
 *
 * <p>The candidates are the public constructors with as many parameters as the bean has arguments. A single candidate
 * is taken as it is. Among several, the ones accepting the arguments are ranked as in Spring's
 * {@code ConstructorResolver}: an argument passed without conversion beats a converted one, and a nearer type beats a
 * distant one. The order of the candidates never counts: the JVM does not define it, and it changes between builds. A
 * tie fails the transpilation, because Spring itself would call the first candidate in that order.
 *
 * <p>The resolution also tells which bean references go into a list. Spring passes a single bean to a collection
 * parameter inside a list, unless the bean is a collection itself.
 */
public class TargetConstructorResolver {

    /** The declared types of the beans given as constructor arguments. */
    @FunctionalInterface
    public interface BeanTypes {

        /** The type of a bean, empty when unknown. */
        Optional<Class<?>> declaredType(String beanName);
    }

    /**
     * The constructor chosen for a bean, and the bean references passed to it inside a list, by argument position. The
     * constructor is null for a bean without arguments and without a constructor to call.
     */
    public record ConstructorResolution(@Nullable Constructor<?> constructor, Map<Integer, Class<?>> wrappedBeanTypes) {

        /** The type of the bean passed inside a list at this argument position, if any. */
        public Optional<Class<?>> wrappedBeanType(int position) {
            return Optional.ofNullable(wrappedBeanTypes.get(position));
        }
    }

    /** Spring favors a constructor taking all the arguments without conversion by this much. */
    private static final long RAW_MATCH_BONUS = 1024;

    private final BeanTypes beanTypes;

    public TargetConstructorResolver(BeanTypes beanTypes) {
        this.beanTypes = beanTypes;
    }

    /** A resolver knowing the types of the beans declared in the transpiled XML. */
    public static TargetConstructorResolver of(TranspilationContext context) {
        Map<String, BeanDefinition> beanDefinitions = context.getAllBeanDefinitions();
        return new TargetConstructorResolver(
                beanName -> TypeNameResolver.resolveDeclaredBeanClass(beanName, beanDefinitions, context));
    }

    /**
     * Resolve the constructor of a bean. Without arguments, it is the no-arg constructor or the single one to autowire.
     */
    public ConstructorResolution resolve(
            String beanName, Class<?> beanClass, @Nullable ConstructorArgumentValues args) {
        if (args == null || args.isEmpty()) {
            return new ConstructorResolution(Reflections.resolveImplicitConstructor(beanClass), Map.of());
        }
        List<Constructor<?>> candidates = candidates(beanClass, args.getArgumentCount());
        return resolve(beanName, candidates, args);
    }

    /** Resolve the constructor of a bean among candidates with the argument count, given in any order. */
    ConstructorResolution resolve(String beanName, List<Constructor<?>> candidates, ConstructorArgumentValues args) {
        SortedMap<Integer, @Nullable Object> arguments = argumentsByPosition(args);
        Constructor<?> constructor = choose(beanName, sortedBySignature(candidates), arguments);
        Map<Integer, Class<?>> wrappedBeanTypes = wrappedBeanTypes(beanName, constructor, arguments);
        return new ConstructorResolution(constructor, wrappedBeanTypes);
    }

    /**
     * The public constructors with the argument count. Without any, the non-public ones: the accessibility check names
     * the problem better than a missing constructor.
     */
    private static List<Constructor<?>> candidates(Class<?> beanClass, int argumentCount) {
        List<Constructor<?>> withArgumentCount = Arrays.stream(beanClass.getDeclaredConstructors())
                .filter(constructor -> constructor.getParameterCount() == argumentCount)
                .toList();
        List<Constructor<?>> publicOnes =
                withArgumentCount.stream().filter(Reflections::isPublic).toList();
        if (!publicOnes.isEmpty()) {
            return publicOnes;
        }
        if (withArgumentCount.isEmpty()) {
            throw new IllegalStateException(
                    "No constructor with %d args found in %s".formatted(argumentCount, beanClass.getName()));
        }
        return withArgumentCount;
    }

    /** The arguments by position, numbered as the generators number them: indexed ones first, then generic ones. */
    private static SortedMap<Integer, @Nullable Object> argumentsByPosition(ConstructorArgumentValues args) {
        Map<Integer, ValueHolder> indexed = args.getIndexedArgumentValues();
        List<ValueHolder> generic = args.getGenericArgumentValues();
        SortedMap<Integer, @Nullable Object> byPosition = new TreeMap<>();
        for (Map.Entry<Integer, ValueHolder> entry : indexed.entrySet()) {
            byPosition.put(entry.getKey(), entry.getValue().getValue());
        }
        for (int i = 0; i < generic.size(); i++) {
            byPosition.put(indexed.size() + i, generic.get(i).getValue());
        }
        return byPosition;
    }

    /** A stable order keeps the error messages the same from one build to the next. */
    private static List<Constructor<?>> sortedBySignature(List<Constructor<?>> candidates) {
        return candidates.stream()
                .sorted(Comparator.comparing(Constructor::toGenericString))
                .toList();
    }

    private Constructor<?> choose(
            String beanName, List<Constructor<?>> candidates, SortedMap<Integer, @Nullable Object> arguments) {
        if (candidates.size() == 1) {
            return candidates.get(0);
        }
        List<Candidate> accepting = accepting(candidates, arguments);
        if (accepting.isEmpty()) {
            throw new IllegalStateException(
                    "No constructor among %s accepts the arguments of bean '%s'. Exclude '%s' and declare it by hand."
                            .formatted(describeAll(candidates), beanName, beanName));
        }
        requireAgreementOnUnknownTypes(beanName, accepting, arguments);
        return closestMatch(beanName, accepting);
    }

    private List<Candidate> accepting(List<Constructor<?>> candidates, SortedMap<Integer, @Nullable Object> arguments) {
        List<Candidate> accepting = new ArrayList<>();
        for (Constructor<?> constructor : candidates) {
            Optional<Candidate> candidate = acceptingCandidate(constructor, arguments);
            candidate.ifPresent(accepting::add);
        }
        return accepting;
    }

    private Optional<Candidate> acceptingCandidate(
            Constructor<?> constructor, SortedMap<Integer, @Nullable Object> arguments) {
        Map<Integer, Fit> fits = new HashMap<>();
        for (Map.Entry<Integer, @Nullable Object> argument : arguments.entrySet()) {
            int position = argument.getKey();
            Optional<Fit> fit = fit(argument.getValue(), constructor, position);
            if (fit.isEmpty()) {
                return Optional.empty();
            }
            fits.put(position, fit.get());
        }
        return Optional.of(new Candidate(constructor, fits));
    }

    /** How a value of the XML fits a parameter. Empty when the generated code cannot pass it. */
    private Optional<Fit> fit(@Nullable Object value, Constructor<?> constructor, int position) {
        if (position >= constructor.getParameterCount()) {
            return Optional.empty();
        }
        Class<?> parameterType = constructor.getParameterTypes()[position];
        return switch (value) {
            case RuntimeBeanReference reference -> referenceFit(reference, constructor, position);
            case TypedStringValue string -> stringFit(string, parameterType);
            case ManagedList<?> _ -> listFit(parameterType);
            case ManagedMap<?, ?> _ -> mapFit(parameterType);
            case null, default -> Optional.of(Fit.unknown());
        };
    }

    private Optional<Fit> referenceFit(RuntimeBeanReference reference, Constructor<?> constructor, int position) {
        Optional<Class<?>> declaredType = beanTypes.declaredType(reference.getBeanName());
        if (declaredType.isEmpty()) {
            return Optional.of(Fit.unknown());
        }
        Class<?> beanType = declaredType.get();
        Class<?> parameterType = constructor.getParameterTypes()[position];
        if (ClassUtils.isAssignable(parameterType, beanType)) {
            return Optional.of(Fit.asIs(beanType));
        }
        if (acceptsInList(constructor, position, beanType)) {
            return Optional.of(Fit.converted(ArrayList.class));
        }
        return Optional.empty();
    }

    /** Whether Spring passes the bean inside a list, as an {@code ArrayList} created by the generated code. */
    private static boolean acceptsInList(Constructor<?> constructor, int position, Class<?> beanType) {
        Class<?> parameterType = constructor.getParameterTypes()[position];
        boolean takesArrayList =
                Collection.class.isAssignableFrom(parameterType) && parameterType.isAssignableFrom(ArrayList.class);
        if (!takesArrayList || Collection.class.isAssignableFrom(beanType)) {
            return false;
        }
        ResolvableType collectionType = ResolvableType.forConstructorParameter(constructor, position);
        Class<?> elementType = collectionType.asCollection().resolveGeneric(0);
        return elementType == null || ClassUtils.isAssignable(elementType, beanType);
    }

    /** The conversions of a string value done by the generated code, as its value rendering does them. */
    private static Optional<Fit> stringFit(TypedStringValue string, Class<?> parameterType) {
        String value = string.getValue();
        if (value != null && isSpelExpression(value)) {
            return Optional.of(expressionFit(parameterType));
        }
        if (parameterType.isAssignableFrom(String.class)) {
            return Optional.of(Fit.asIs(String.class));
        }
        if (Class.class.isAssignableFrom(parameterType)) {
            return Optional.of(Fit.converted(Class.class));
        }
        if (Reflections.isPrimitiveOrBoxedType(parameterType)) {
            return Optional.of(Fit.converted(ClassUtils.resolvePrimitiveIfNecessary(parameterType)));
        }
        if (Reflections.hasStringConstructor(parameterType)) {
            return Optional.of(Fit.converted(parameterType));
        }
        return Optional.empty();
    }

    private static boolean isSpelExpression(String value) {
        return value.contains("#{") && value.contains("}");
    }

    /**
     * Spring evaluates an expression when it creates the bean, and converts the result to the parameter type. The
     * generated code takes the result as a parameter of that type. The ranking counts the expression as a string, like
     * Spring's build-time resolver does.
     */
    private static Fit expressionFit(Class<?> parameterType) {
        if (parameterType.isAssignableFrom(String.class)) {
            return Fit.asIs(String.class);
        }
        return Fit.converted(ClassUtils.resolvePrimitiveIfNecessary(parameterType));
    }

    /** Spring resolves a {@code <list>} to an {@code ArrayList}, and converts it for an array parameter. */
    private static Optional<Fit> listFit(Class<?> parameterType) {
        if (parameterType.isArray()) {
            return Optional.of(Fit.converted(parameterType));
        }
        if (parameterType.isAssignableFrom(ArrayList.class)) {
            return Optional.of(Fit.asIs(ArrayList.class));
        }
        return Optional.empty();
    }

    /** The generated code passes a {@code <map>} as a {@code HashMap}. */
    private static Optional<Fit> mapFit(Class<?> parameterType) {
        if (parameterType.isAssignableFrom(HashMap.class)) {
            return Optional.of(Fit.asIs(HashMap.class));
        }
        return Optional.empty();
    }

    /**
     * An argument of unknown type cannot rank the candidates. It is harmless when the candidates take the same type at
     * its position. Otherwise the choice would depend on the type of the argument.
     */
    private static void requireAgreementOnUnknownTypes(
            String beanName, List<Candidate> candidates, SortedMap<Integer, @Nullable Object> arguments) {
        for (Map.Entry<Integer, @Nullable Object> argument : arguments.entrySet()) {
            int position = argument.getKey();
            Set<Class<?>> parameterTypes = parameterTypesForUnknownArgument(candidates, position);
            if (parameterTypes.size() > 1) {
                throw new IllegalStateException(
                        "Bean '%s' gives %s to constructor parameter %d, and the constructors %s take different types there. Exclude '%s' and declare it by hand."
                                .formatted(
                                        beanName,
                                        describeUnknown(argument.getValue()),
                                        position,
                                        describeAll(constructors(candidates)),
                                        beanName));
            }
        }
    }

    private static Set<Class<?>> parameterTypesForUnknownArgument(List<Candidate> candidates, int position) {
        Set<Class<?>> parameterTypes = new LinkedHashSet<>();
        for (Candidate candidate : candidates) {
            if (candidate.fits().get(position).isUnknown()) {
                parameterTypes.add(candidate.parameterType(position));
            }
        }
        return parameterTypes;
    }

    private static String describeUnknown(@Nullable Object value) {
        if (value instanceof RuntimeBeanReference reference) {
            return "bean '%s', of unknown type,".formatted(reference.getBeanName());
        }
        return "an argument of unknown type";
    }

    private static Constructor<?> closestMatch(String beanName, List<Candidate> candidates) {
        long lowestWeight =
                candidates.stream().mapToLong(Candidate::weight).min().orElseThrow();
        List<Constructor<?>> closest = candidates.stream()
                .filter(candidate -> candidate.weight() == lowestWeight)
                .map(Candidate::constructor)
                .toList();
        if (closest.size() > 1) {
            throw new IllegalStateException(
                    "Bean '%s' matches the constructors %s equally. Spring would call the first of them in reflection order, and that order changes between builds. Exclude '%s' and declare it by hand."
                            .formatted(beanName, describeAll(closest), beanName));
        }
        return closest.get(0);
    }

    private Map<Integer, Class<?>> wrappedBeanTypes(
            String beanName, Constructor<?> constructor, SortedMap<Integer, @Nullable Object> arguments) {
        Map<Integer, Class<?>> wrapped = new HashMap<>();
        for (Map.Entry<Integer, @Nullable Object> argument : arguments.entrySet()) {
            int position = argument.getKey();
            boolean hasParameter = position < constructor.getParameterCount();
            if (hasParameter && argument.getValue() instanceof RuntimeBeanReference reference) {
                Optional<Class<?>> wrappedType = wrappedBeanType(beanName, constructor, position, reference);
                wrappedType.ifPresent(type -> wrapped.put(position, type));
            }
        }
        return Map.copyOf(wrapped);
    }

    /**
     * The type of a bean passed to a collection parameter inside a list. Spring puts the bean into a list unless the
     * bean is a collection, and only its type tells which. An unknown type fails the transpilation.
     */
    private Optional<Class<?>> wrappedBeanType(
            String beanName, Constructor<?> constructor, int position, RuntimeBeanReference reference) {
        Class<?> parameterType = constructor.getParameterTypes()[position];
        if (!Collection.class.isAssignableFrom(parameterType)) {
            return Optional.empty();
        }
        String referencedName = reference.getBeanName();
        Class<?> beanType = beanTypes
                .declaredType(referencedName)
                .orElseThrow(() -> unknownCollectionArgument(beanName, referencedName, position));
        if (parameterType.isAssignableFrom(beanType)) {
            return Optional.empty();
        }
        return Optional.of(beanType);
    }

    private static IllegalStateException unknownCollectionArgument(
            String beanName, String referencedName, int position) {
        return new IllegalStateException(
                "Bean '%s' gives bean '%s' to the collection parameter %d, and the type of '%s' is unknown. Spring puts a bean into a list unless the bean is a collection. Exclude '%s' and declare it by hand."
                        .formatted(beanName, referencedName, position, referencedName, beanName));
    }

    private static List<Constructor<?>> constructors(List<Candidate> candidates) {
        return candidates.stream().map(Candidate::constructor).toList();
    }

    private static String describeAll(List<Constructor<?>> constructors) {
        return constructors.stream().map(TargetConstructorResolver::describe).collect(Collectors.joining(", "));
    }

    private static String describe(Constructor<?> constructor) {
        String parameters = Arrays.stream(constructor.getParameterTypes())
                .map(Class::getSimpleName)
                .collect(Collectors.joining(", "));
        return "%s(%s)".formatted(constructor.getDeclaringClass().getSimpleName(), parameters);
    }

    /**
     * Spring's distance between a parameter type and the type of a value, as in
     * {@code MethodInvoker.getTypeDifferenceWeight}: 2 for each superclass of the value type assignable to the
     * parameter type, and 1 for an interface parameter.
     */
    static int typeDifference(Class<?> parameterType, Class<?> valueType) {
        int difference = 0;
        Class<?> superClass = valueType.getSuperclass();
        while (superClass != null) {
            if (parameterType.equals(superClass)) {
                difference += 2;
                superClass = null;
            } else if (ClassUtils.isAssignable(parameterType, superClass)) {
                difference += 2;
                superClass = superClass.getSuperclass();
            } else {
                superClass = null;
            }
        }
        if (parameterType.isInterface()) {
            difference += 1;
        }
        return difference;
    }

    /** A constructor accepting the arguments, with the fit of each argument by position. */
    private record Candidate(Constructor<?> constructor, Map<Integer, Fit> fits) {

        Class<?> parameterType(int position) {
            return constructor.getParameterTypes()[position];
        }

        /** Spring's weight: the lower of the converted weight and the raw one, favored by {@link #RAW_MATCH_BONUS}. */
        long weight() {
            return Math.min(rawWeight(), convertedWeight());
        }

        private long rawWeight() {
            long weight = 0;
            for (Map.Entry<Integer, Fit> entry : fits.entrySet()) {
                Fit fit = entry.getValue();
                Class<?> rawType = fit.rawType();
                if (rawType != null) {
                    weight += typeDifference(parameterType(entry.getKey()), rawType);
                } else if (!fit.isUnknown()) {
                    return Long.MAX_VALUE;
                }
            }
            return weight - RAW_MATCH_BONUS;
        }

        private long convertedWeight() {
            long weight = 0;
            for (Map.Entry<Integer, Fit> entry : fits.entrySet()) {
                Class<?> convertedType = entry.getValue().convertedType();
                if (convertedType != null) {
                    weight += typeDifference(parameterType(entry.getKey()), convertedType);
                }
            }
            return weight;
        }
    }

    /**
     * How an argument fits a parameter, in the terms of Spring's weight. The raw type is the type of the value before
     * conversion, null when the parameter needs a conversion. The converted type is the type after conversion. Both are
     * null for a value of unknown type: it fits any parameter and weighs nothing.
     */
    private record Fit(@Nullable Class<?> rawType, @Nullable Class<?> convertedType) {

        static Fit asIs(Class<?> type) {
            return new Fit(type, type);
        }

        static Fit converted(Class<?> type) {
            return new Fit(null, type);
        }

        static Fit unknown() {
            return new Fit(null, null);
        }

        boolean isUnknown() {
            return convertedType == null;
        }
    }
}
