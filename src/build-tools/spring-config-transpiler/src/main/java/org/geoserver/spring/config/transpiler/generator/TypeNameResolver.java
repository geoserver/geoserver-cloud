/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.spring.config.transpiler.generator;

import static javax.tools.Diagnostic.Kind.NOTE;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import lombok.experimental.UtilityClass;
import org.geoserver.spring.config.transpiler.context.TranspilationContext;
import org.springframework.beans.PropertyValue;
import org.springframework.beans.PropertyValues;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.beans.factory.config.RuntimeBeanReference;

/**
 * Utility class for resolving type names during transpilation.
 *
 * <p>This class handles the critical task of converting Spring BeanDefinition type information into proper Java type
 * names for code generation. It uses analysis of bean usage patterns and annotation processor APIs to infer types
 * dynamically rather than hardcoding them.
 *
 * @since 3.0.0
 */
@UtilityClass
class TypeNameResolver {

    private static final String JAVA_LANG_OBJECT = "java.lang.Object";

    /** Cache for resolved type names to avoid repeated resolution attempts. */
    private static final Map<String, String> typeNameCache = new ConcurrentHashMap<>();

    /** Result of type resolution containing the resolved type and metadata. */
    public static class TypeResolutionResult {
        private final String resolvedTypeName;
        private final boolean isResolved;
        private final boolean requiresImport;
        private final String packageName;
        private final String simpleClassName;

        private TypeResolutionResult(
                String resolvedTypeName,
                boolean isResolved,
                boolean requiresImport,
                String packageName,
                String simpleClassName) {
            this.resolvedTypeName = resolvedTypeName;
            this.isResolved = isResolved;
            this.requiresImport = requiresImport;
            this.packageName = packageName;
            this.simpleClassName = simpleClassName;
        }

        public String getResolvedTypeName() {
            return resolvedTypeName;
        }

        public boolean isResolved() {
            return isResolved;
        }

        public boolean requiresImport() {
            return requiresImport;
        }

        public String getPackageName() {
            return packageName;
        }

        public String getSimpleClassName() {
            return simpleClassName;
        }

        public static TypeResolutionResult resolved(String typeName) {
            String packageName = "";
            String simpleClassName = typeName;
            boolean requiresImport = false;

            if (typeName.contains(".")) {
                int lastDot = typeName.lastIndexOf(".");
                packageName = typeName.substring(0, lastDot);
                simpleClassName = typeName.substring(lastDot + 1);
                requiresImport = !isJavaLangType(typeName);
            }

            return new TypeResolutionResult(typeName, true, requiresImport, packageName, simpleClassName);
        }

        public static TypeResolutionResult unresolved(String fallbackType) {
            return new TypeResolutionResult(fallbackType, false, false, "", fallbackType);
        }

        private static boolean isJavaLangType(String typeName) {
            return typeName.startsWith("java.lang.") && !typeName.substring(10).contains(".");
        }
    }

    /**
     * Resolve the type name for a bean definition's return type.
     *
     * @param beanDefinition the bean definition
     * @param context the transpilation context
     * @return resolved type information
     */
    public static TypeResolutionResult resolveBeanReturnType(
            BeanDefinition beanDefinition, TranspilationContext context) {
        String beanClassName = beanDefinition.getBeanClassName();

        // Check for static factory method FIRST (before plain beanClassName resolution)
        if (beanDefinition.getFactoryMethodName() != null && beanClassName != null) {
            return resolveStaticFactoryMethodReturnType(beanClassName, beanDefinition.getFactoryMethodName(), context);
        }

        // Try direct bean class name
        if (beanClassName != null && !beanClassName.trim().isEmpty()) {
            return resolveTypeName(beanClassName, context);
        }

        // Handle factory bean cases
        if (beanDefinition.getFactoryBeanName() != null) {
            String factoryMethod = beanDefinition.getFactoryMethodName();
            if (factoryMethod != null) {
                // For factory beans, we'd need the factory bean's definition to determine return type
                // This is complex and would require cross-reference analysis
                context.printMessage(
                        NOTE,
                        "Factory bean return type resolution not implemented for: "
                                + beanDefinition.getFactoryBeanName());
            }
        }

        // Fallback to Object if we can't determine the type
        return TypeResolutionResult.unresolved(JAVA_LANG_OBJECT);
    }

    /**
     * Resolve the type name for a dependency (constructor argument or property reference).
     *
     * @param dependencyName the name of the dependency bean
     * @param allBeanDefinitions map of all bean definitions available
     * @param context the transpilation context
     * @return resolved type information
     */
    public static TypeResolutionResult resolveDependencyType(
            String dependencyName, Map<String, BeanDefinition> allBeanDefinitions, TranspilationContext context) {
        // Look up the dependency bean definition in our current set
        BeanDefinition dependencyBean = allBeanDefinitions.get(dependencyName);
        if (dependencyBean != null) {
            return resolveBeanReturnType(dependencyBean, context);
        }

        // For external dependencies, try to infer the type from usage context
        TypeResolutionResult inferredType = inferTypeFromUsageContext(dependencyName, allBeanDefinitions, context);
        if (inferredType.isResolved()) {
            return inferredType;
        }

        // Log external dependency
        context.printMessage(
                NOTE,
                "External dependency '" + dependencyName
                        + "' not found in current definitions, using Object parameter type");

        // Final fallback to Object - this is the safest approach for external dependencies
        return TypeResolutionResult.unresolved(JAVA_LANG_OBJECT);
    }

    /**
     * Resolve a type name using the annotation processor's type system.
     *
     * @param typeName the fully qualified type name
     * @param context the transpilation context
     * @return resolved type information
     */
    public static TypeResolutionResult resolveTypeName(String typeName, TranspilationContext context) {
        if (typeName == null || typeName.trim().isEmpty()) {
            return TypeResolutionResult.unresolved(JAVA_LANG_OBJECT);
        }

        // Check cache first
        String cached = typeNameCache.get(typeName);
        if (cached != null) {
            return TypeResolutionResult.resolved(cached);
        }

        try {
            // Use the annotation processor's Elements utility to resolve the type
            Elements elementUtils = context.getProcessingEnvironment().getElementUtils();
            TypeElement typeElement = elementUtils.getTypeElement(typeName);

            if (typeElement != null) {
                String qualifiedName = typeElement.getQualifiedName().toString();
                typeNameCache.put(typeName, qualifiedName);
                return TypeResolutionResult.resolved(qualifiedName);
            }
        } catch (Exception e) {
            // Type resolution failed - log and continue with fallback
            if (context.getProcessingEnvironment() != null) {
                context.getProcessingEnvironment()
                        .getMessager()
                        .printMessage(NOTE, "Failed to resolve type '" + typeName + "': " + e.getMessage());
            }
        }

        // Try common type name transformations
        String transformedType = tryCommonTypeTransformations(typeName);
        if (transformedType != null && !transformedType.equals(typeName)) {
            TypeResolutionResult transformed = resolveTypeName(transformedType, context);
            if (transformed.isResolved()) {
                typeNameCache.put(typeName, transformed.getResolvedTypeName());
                return transformed;
            }
        }

        // If we can't resolve it, but it looks like a valid class name, use it as-is
        if (isValidClassName(typeName)) {
            typeNameCache.put(typeName, typeName);
            return TypeResolutionResult.resolved(typeName);
        }

        // Final fallback
        return TypeResolutionResult.unresolved(JAVA_LANG_OBJECT);
    }

    /** Resolve the return type of a static factory method. */
    private static TypeResolutionResult resolveStaticFactoryMethodReturnType(
            String className, String factoryMethod, TranspilationContext context) {
        // Strategy 1: Use annotation processor APIs
        try {
            Elements elementUtils = context.getProcessingEnvironment().getElementUtils();
            TypeElement classElement = elementUtils.getTypeElement(className);
            if (classElement != null) {
                TypeResolutionResult result = classElement.getEnclosedElements().stream()
                        .filter(element -> element.getKind() == javax.lang.model.element.ElementKind.METHOD)
                        .filter(element -> element.getSimpleName().toString().equals(factoryMethod))
                        .filter(element -> element.getModifiers().contains(javax.lang.model.element.Modifier.STATIC))
                        .map(element -> (javax.lang.model.element.ExecutableElement) element)
                        .findFirst()
                        .map(method -> {
                            TypeMirror returnType = method.getReturnType();
                            String returnTypeName = returnType.toString();
                            return TypeResolutionResult.resolved(returnTypeName);
                        })
                        .orElse(null);
                if (result != null) {
                    return result;
                }
            }
        } catch (Exception _) {
            // Fall through to reflection-based strategy
        }

        // Strategy 2: Use runtime reflection
        try {
            Class<?> factoryClass = Class.forName(className);
            for (java.lang.reflect.Method method : factoryClass.getDeclaredMethods()) {
                if (method.getName().equals(factoryMethod) && Modifier.isStatic(method.getModifiers())) {
                    Class<?> returnType = method.getReturnType();
                    return TypeResolutionResult.resolved(returnType.getName());
                }
            }
        } catch (ClassNotFoundException e) {
            context.printMessage(
                    NOTE,
                    "Failed to resolve static factory method return type for " + className + "." + factoryMethod + ": "
                            + e.getMessage());
        }

        return TypeResolutionResult.unresolved(JAVA_LANG_OBJECT);
    }

    /** Infer type from usage context by analyzing how the dependency is used. */
    private static TypeResolutionResult inferTypeFromUsageContext(
            String dependencyName, Map<String, BeanDefinition> allBeanDefinitions, TranspilationContext context) {
        context.printMessage(
                NOTE,
                "Analyzing usage context for dependency: %s across %d bean definitions"
                        .formatted(dependencyName, allBeanDefinitions.size()));

        // Analyze constructor arguments to infer types from actual constructor signatures
        for (BeanDefinition beanDef : allBeanDefinitions.values()) {
            if (beanDef.hasConstructorArgumentValues()) {
                TypeResolutionResult constructorInference = inferFromConstructorUsage(dependencyName, beanDef, context);
                if (constructorInference.isResolved()) {
                    return constructorInference;
                }
            }

            // Check property setters - this is also a reliable way to infer types
            TypeResolutionResult propertyInference = inferFromPropertyUsage(dependencyName, beanDef, context);
            if (propertyInference.isResolved()) {
                return propertyInference;
            }
        }

        return TypeResolutionResult.unresolved(JAVA_LANG_OBJECT);
    }

    /** Infer type from property usage by analyzing setter method signatures. */
    private static TypeResolutionResult inferFromPropertyUsage(
            String dependencyName, BeanDefinition beanDef, TranspilationContext context) {
        if (!beanDef.hasPropertyValues()) {
            return TypeResolutionResult.unresolved(JAVA_LANG_OBJECT);
        }

        PropertyValues propertyValues = beanDef.getPropertyValues();
        context.printMessage(
                NOTE,
                "Bean " + beanDef.getBeanClassName() + " has " + propertyValues.getPropertyValues().length
                        + " property values");

        for (PropertyValue pv : propertyValues.getPropertyValues()) {
            Object value = pv.getValue();
            context.printMessage(
                    NOTE,
                    "Checking property '" + pv.getName() + "' with value type: "
                            + (value != null ? value.getClass().getSimpleName() : "null"));

            if (value instanceof RuntimeBeanReference beanRef) {
                context.printMessage(
                        NOTE,
                        "Property '" + pv.getName() + "' references bean: '" + beanRef.getBeanName()
                                + "', looking for: '" + dependencyName + "'");

                if (dependencyName.equals(beanRef.getBeanName())) {
                    context.printMessage(
                            NOTE,
                            "Found matching bean reference! Inferring type from setter method for property: "
                                    + pv.getName());
                    // Found usage - try to infer type from setter method
                    return inferTypeFromSetterMethod(beanDef, pv.getName(), context);
                }
            }
        }

        return TypeResolutionResult.unresolved(JAVA_LANG_OBJECT);
    }

    /** Infer type from constructor usage by analyzing actual constructor signatures. */
    private static TypeResolutionResult inferFromConstructorUsage(
            String dependencyName, BeanDefinition beanDef, TranspilationContext context) {

        String beanClassName = beanDef.getBeanClassName();
        if (beanClassName == null) {
            return TypeResolutionResult.unresolved(JAVA_LANG_OBJECT);
        }

        // Use runtime reflection to load the class
        Class<?> beanClass = Reflections.convertToRuntimeClass(beanClassName);

        // Get constructor arguments from bean definition
        ConstructorArgumentValues constructorArgs = beanDef.getConstructorArgumentValues();
        int argCount = constructorArgs.getArgumentCount();

        // Find constructor with matching argument count, preferring public constructors
        List<Constructor<?>> constructors = Arrays.asList(beanClass.getDeclaredConstructors());
        Constructor<?> selectedConstructor = null;

        // First try to find a public constructor with matching argument count
        selectedConstructor = Reflections.findPublicConstructorWithArgCount(constructors, argCount)
                .orElse(null);

        // If no public constructor found, fall back to any constructor with matching count
        if (selectedConstructor == null) {
            for (Constructor<?> constructor : constructors) {
                if (constructor.getParameterCount() == argCount) {
                    selectedConstructor = constructor;
                    break;
                }
            }
        }

        if (selectedConstructor != null) {
            Class<?>[] parameterTypes = selectedConstructor.getParameterTypes();

            // Find which parameter position uses our dependency
            int dependencyIndex = findDependencyIndexInConstructor(dependencyName, constructorArgs);
            if (dependencyIndex >= 0 && dependencyIndex < parameterTypes.length) {
                Class<?> paramType = parameterTypes[dependencyIndex];
                context.printMessage(
                        NOTE,
                        "SUCCESS: Inferred type for '%s' from constructor of %s: %s"
                                .formatted(dependencyName, beanClassName, paramType.getName()));
                return TypeResolutionResult.resolved(paramType.getName());
            }
        }
        return TypeResolutionResult.unresolved(JAVA_LANG_OBJECT);
    }

    /** Find the index position of a dependency in constructor arguments. */
    private static int findDependencyIndexInConstructor(
            String dependencyName, org.springframework.beans.factory.config.ConstructorArgumentValues constructorArgs) {
        // Check indexed arguments first
        Map<Integer, ConstructorArgumentValues.ValueHolder> indexedArgs = constructorArgs.getIndexedArgumentValues();
        for (Map.Entry<Integer, ConstructorArgumentValues.ValueHolder> entry : indexedArgs.entrySet()) {
            Object value = entry.getValue().getValue();
            if (value instanceof RuntimeBeanReference beanRef && dependencyName.equals(beanRef.getBeanName())) {
                return entry.getKey();
            }
        }

        // Check generic arguments
        List<ConstructorArgumentValues.ValueHolder> genericArgs = constructorArgs.getGenericArgumentValues();
        for (int i = 0; i < genericArgs.size(); i++) {
            Object value = genericArgs.get(i).getValue();
            if (value instanceof RuntimeBeanReference beanRef && dependencyName.equals(beanRef.getBeanName())) {
                // Generic args come after indexed args
                return indexedArgs.size() + i;
            }
        }

        return -1; // Not found
    }

    /** Infer type from setter method by examining the target class using runtime reflection. */
    private static TypeResolutionResult inferTypeFromSetterMethod(
            BeanDefinition beanDef, String propertyName, TranspilationContext context) {
        String beanClassName = beanDef.getBeanClassName();
        if (beanClassName == null) {
            context.printMessage(NOTE, "Cannot infer setter type: bean class name is null");
            return TypeResolutionResult.unresolved(JAVA_LANG_OBJECT);
        }

        context.printMessage(
                NOTE,
                "Attempting to infer type from setter method for property '" + propertyName + "' in class "
                        + beanClassName);

        try {
            // Use runtime reflection to load the class and analyze setter methods
            Class<?> beanClass = Class.forName(beanClassName);
            String setterName = Reflections.buildSetterName(propertyName);

            context.printMessage(NOTE, "Looking for setter method: " + setterName + " in class " + beanClassName);

            // Search through the class hierarchy for the setter method
            Class<?> currentClass = beanClass;
            while (currentClass != null && currentClass != Object.class) {
                java.lang.reflect.Method[] methods = currentClass.getDeclaredMethods();
                for (java.lang.reflect.Method method : methods) {
                    if (method.getName().equals(setterName) && method.getParameterCount() == 1) {
                        Class<?> paramType = method.getParameterTypes()[0];
                        context.printMessage(
                                NOTE,
                                "SUCCESS: Found setter " + setterName + " in " + currentClass.getName()
                                        + " with parameter type: " + paramType.getName());
                        return TypeResolutionResult.resolved(paramType.getName());
                    }
                }
                currentClass = currentClass.getSuperclass();
            }

            context.printMessage(
                    NOTE, "No setter method found: " + setterName + " in class hierarchy of " + beanClassName);

        } catch (ClassNotFoundException _) {
            context.printMessage(NOTE, "Cannot load class for setter analysis: " + beanClassName);
        } catch (Exception e) {
            context.printMessage(NOTE, "Exception while inferring type from setter method: " + e.getMessage());
        }

        return TypeResolutionResult.unresolved(JAVA_LANG_OBJECT);
    }

    /** Try common type name transformations. */
    private static String tryCommonTypeTransformations(String typeName) {
        // Handle inner classes ($ to .)
        if (typeName.contains("$")) {
            return typeName.replace("$", ".");
        }

        // Handle array types
        if (typeName.endsWith("[]")) {
            return typeName; // Keep as-is for now
        }

        return null; // No transformation found
    }

    /** Check if a string looks like a valid Java class name. */
    private static boolean isValidClassName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return false;
        }

        // Basic validation - should contain at least one dot and valid Java identifier characters
        return name.matches("^\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}*+"
                + "(\\.\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}*+)++$");
    }
}
