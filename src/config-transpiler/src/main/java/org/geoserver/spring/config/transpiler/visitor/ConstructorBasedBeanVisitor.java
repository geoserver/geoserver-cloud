/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.spring.config.transpiler.visitor;

import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.lang.model.element.Modifier;
import org.geoserver.spring.config.transpiler.context.BeanGenerationContext;
import org.geoserver.spring.config.transpiler.context.TranspilationContext;
import org.geoserver.spring.config.transpiler.util.TypeNameResolver;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.config.TypedStringValue;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;

/**
 * Visitor for generating {@code @Bean} methods for beans with constructor arguments.
 *
 * <p>This visitor handles Spring bean definitions that specify constructor arguments
 * for dependency injection. It generates {@code @Bean} methods with proper type resolution
 * and parameter handling.
 *
 * @since 2.28.0
 */
public class ConstructorBasedBeanVisitor extends AbstractBeanDefinitionVisitor {

    @Override
    public boolean canHandle(BeanDefinition beanDefinition, TranspilationContext context) {
        // Handle beans with explicit constructor arguments
        if (beanDefinition.hasConstructorArgumentValues()
                && beanDefinition.getBeanClassName() != null
                && !beanDefinition.isAbstract()) {
            return true;
        }

        // Handle beans with implicit constructor autowiring (no explicit constructor args but class needs injection)
        if (beanDefinition.getBeanClassName() != null
                && !beanDefinition.isAbstract()
                && !beanDefinition.hasConstructorArgumentValues()) {
            return requiresImplicitConstructorAutowiring(beanDefinition.getBeanClassName());
        }

        return false;
    }

    @Override
    public MethodSpec generateBeanMethod(BeanGenerationContext beanContext) {
        ConstructorBeanMethodGenerator generator = new ConstructorBeanMethodGenerator(beanContext);
        return generator.generate();
    }

    @Override
    public int getPriority() {
        return 50; // High priority for constructor-based beans
    }

    /**
     * Determine if a bean class requires implicit constructor autowiring.
     * This happens when:
     * 1. The class has no default (no-arg) constructor
     * 2. The class has exactly one constructor that should be autowired
     *
     * Based on the logic from the old BuildTimeXmlImportProcessor.
     */
    private boolean requiresImplicitConstructorAutowiring(String beanClassName) {
        try {
            Class<?> beanClass = Class.forName(beanClassName);

            // First check for default constructor (any visibility)
            try {
                beanClass.getDeclaredConstructor(); // Try no-args constructor
                return false; // Has default constructor, no implicit autowiring needed
            } catch (NoSuchMethodException e) {
                // No default constructor found
            }

            // Check for single public constructor
            java.lang.reflect.Constructor<?>[] publicConstructors = beanClass.getConstructors();
            if (publicConstructors.length == 1) {
                java.lang.reflect.Constructor<?> constructor = publicConstructors[0];
                // Only autowire if constructor has parameters
                return constructor.getParameterCount() > 0;
            }

            // Check for single declared constructor (including protected/private)
            java.lang.reflect.Constructor<?>[] allConstructors = beanClass.getDeclaredConstructors();
            if (allConstructors.length == 1) {
                java.lang.reflect.Constructor<?> constructor = allConstructors[0];
                // Only autowire if constructor has parameters
                return constructor.getParameterCount() > 0;
            }

            // Multiple constructors or other edge cases - no implicit autowiring
            return false;

        } catch (ClassNotFoundException e) {
            // Cannot load class - let other visitors handle it
            return false;
        } catch (Exception e) {
            // Any other reflection error - skip implicit autowiring
            return false;
        }
    }

    /**
     * Inner class to encapsulate method generation logic and reduce parameter passing.
     */
    private class ConstructorBeanMethodGenerator {
        private final BeanGenerationContext beanContext;
        private final TranspilationContext transpilationContext;
        private final BeanDefinition beanDefinition;
        private final String beanName;
        private final String beanClassName;

        public ConstructorBeanMethodGenerator(BeanGenerationContext beanContext) {
            this.beanContext = beanContext;
            this.transpilationContext = beanContext.getTranspilationContext();
            this.beanDefinition = beanContext.getBeanDefinition();
            this.beanName = beanContext.getBeanName();
            this.beanClassName = beanDefinition.getBeanClassName();
        }

        public MethodSpec generate() {
            // Use consolidated EnhancedBeanInfo for proper method name
            String methodName = beanContext.getSanitizedMethodName();

            // For auto-generated bean names, add unique suffix to avoid collisions across configurations
            if (beanContext.isAutoGenerated()) {
                String uniqueSuffix = beanContext.getTranspilationContext().getUniqueMethodSuffix();
                methodName = methodName + "_" + uniqueSuffix;
            }

            // {@code @Bean} methods are always package-private per Spring conventions
            // Only {@code @Configuration} class visibility is controlled by publicAccess
            Modifier[] methodModifiers = new Modifier[0]; // Package-private

            // Get return type using TypeNameResolver
            ClassName returnType = getReturnType();

            // Create method builder
            MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder(methodName)
                    .addModifiers(methodModifiers)
                    .returns(returnType);

            // Add Javadoc
            addJavadoc(methodBuilder);

            // Add annotations
            addBeanAnnotations(methodBuilder);

            // Add constructor exceptions if needed
            addConstructorExceptions(methodBuilder);

            // Generate method body
            generateMethodBody(methodBuilder);

            return methodBuilder.build();
        }

        private void addJavadoc(MethodSpec.Builder methodBuilder) {
            // Use the enhanced bean info's Javadoc generation (includes original XML if available)
            String javadocContent = beanContext.generateJavadoc();
            methodBuilder.addJavadoc(javadocContent);
        }

        private void addBeanAnnotations(MethodSpec.Builder methodBuilder) {
            // Add {@code @Bean} annotation using BeanNameResolver logic
            methodBuilder.addAnnotation(createBeanAnnotation(beanContext));

            // Add @SuppressWarnings if needed for managed collections
            addSuppressWarningsIfNeeded(methodBuilder, beanDefinition);

            // Add @Lazy annotation if bean has lazy-init="true"
            if (beanDefinition.isLazyInit()) {
                methodBuilder.addAnnotation(Lazy.class);
            }

            // Add @Scope annotation if bean has non-default scope
            if (beanDefinition.getScope() != null
                    && !beanDefinition.getScope().isEmpty()
                    && !"singleton".equals(beanDefinition.getScope())) {
                AnnotationSpec scopeAnnotation = AnnotationSpec.builder(Scope.class)
                        .addMember("value", "$S", beanDefinition.getScope())
                        .build();
                methodBuilder.addAnnotation(scopeAnnotation);
            }

            // Add @DependsOn annotation if bean has depends-on attribute
            String[] dependsOn = beanDefinition.getDependsOn();
            if (dependsOn != null && dependsOn.length > 0) {
                AnnotationSpec.Builder dependsOnBuilder = AnnotationSpec.builder(DependsOn.class);
                if (dependsOn.length == 1) {
                    dependsOnBuilder.addMember("value", "$S", dependsOn[0]);
                } else {
                    CodeBlock.Builder arrayBuilder = CodeBlock.builder().add("{");
                    for (int i = 0; i < dependsOn.length; i++) {
                        if (i > 0) {
                            arrayBuilder.add(", ");
                        }
                        arrayBuilder.add("$S", dependsOn[i]);
                    }
                    arrayBuilder.add("}");
                    dependsOnBuilder.addMember("value", arrayBuilder.build());
                }
                methodBuilder.addAnnotation(dependsOnBuilder.build());
            }
        }

        private ClassName getReturnType() {
            // Use TypeNameResolver for proper type resolution
            TypeNameResolver.TypeResolutionResult result =
                    TypeNameResolver.resolveBeanReturnType(beanDefinition, transpilationContext);

            if (!result.isResolved()) {
                return ClassName.get(Object.class);
            }

            String typeName = result.getResolvedTypeName();
            try {
                if (typeName.contains(".")) {
                    int lastDot = typeName.lastIndexOf('.');
                    String packageName = typeName.substring(0, lastDot);
                    String simpleName = typeName.substring(lastDot + 1);
                    return ClassName.get(packageName, simpleName);
                } else {
                    return ClassName.get("java.lang", typeName);
                }
            } catch (Exception e) {
                return ClassName.get(Object.class);
            }
        }

        private void addConstructorExceptions(MethodSpec.Builder methodBuilder) {
            if (beanClassName == null) {
                return;
            }

            try {
                // Use runtime reflection to analyze constructor exceptions
                Class<?> beanClass = Class.forName(beanClassName);

                // Determine the constructor we'll be using
                java.lang.reflect.Constructor<?> targetConstructor = null;
                ConstructorArgumentValues constructorArgs = beanDefinition.getConstructorArgumentValues();

                if (constructorArgs.isEmpty()) {
                    // Handle implicit constructor autowiring
                    targetConstructor = findConstructorForAutowiring(beanClass);
                } else {
                    // Handle explicit constructor arguments
                    int argCount = constructorArgs.getArgumentCount();
                    java.lang.reflect.Constructor<?>[] constructors = beanClass.getDeclaredConstructors();

                    // First try to find a public constructor with matching parameter count
                    for (java.lang.reflect.Constructor<?> constructor : constructors) {
                        if (constructor.getParameterCount() == argCount
                                && java.lang.reflect.Modifier.isPublic(constructor.getModifiers())) {
                            targetConstructor = constructor;
                            break;
                        }
                    }

                    // If no public constructor found, fall back to any constructor with matching count
                    if (targetConstructor == null) {
                        for (java.lang.reflect.Constructor<?> constructor : constructors) {
                            if (constructor.getParameterCount() == argCount) {
                                targetConstructor = constructor;
                                break;
                            }
                        }
                    }
                }

                if (targetConstructor != null) {
                    // If constructor is not public, we need reflection which can throw Exception
                    if (!java.lang.reflect.Modifier.isPublic(targetConstructor.getModifiers())) {
                        // For reflection-based instantiation, just add generic Exception
                        methodBuilder.addException(ClassName.get(Exception.class));
                    } else {
                        // For direct instantiation, add declared exceptions from the constructor
                        for (Class<?> exceptionType : targetConstructor.getExceptionTypes()) {
                            // Only add checked exceptions (not RuntimeException or Error)
                            if (Exception.class.isAssignableFrom(exceptionType)
                                    && !RuntimeException.class.isAssignableFrom(exceptionType)) {
                                methodBuilder.addException(ClassName.get(exceptionType));
                            }
                        }
                    }
                }

            } catch (ClassNotFoundException e) {
                // If we can't load the class, assume we might need reflection for non-empty constructors
                if (beanDefinition.hasConstructorArgumentValues()
                        || (!beanDefinition.hasConstructorArgumentValues() && beanClassName != null)) {
                    methodBuilder.addException(ClassName.get(Exception.class));
                }
            }
        }

        private void generateMethodBody(MethodSpec.Builder methodBuilder) {
            ConstructorArgumentValues constructorArgs = beanDefinition.getConstructorArgumentValues();
            boolean hasProperties = beanContext.hasPropertyValues();

            List<ConstructorParameter> constructorParams;

            if (constructorArgs.isEmpty()) {
                // Handle implicit constructor autowiring - infer parameters from class constructor
                constructorParams = collectImplicitConstructorParameters();
            } else {
                // Handle explicit constructor arguments from XML
                constructorParams = collectConstructorParameters(constructorArgs);
            }

            // Add method parameters for each constructor argument
            addMethodParameters(methodBuilder, constructorParams);

            // Collect property bean references and add them as method parameters if needed
            List<String> propertyBeanReferences = new ArrayList<>();
            if (hasProperties) {
                propertyBeanReferences = collectPropertyBeanReferences();
                addPropertyBeanReferenceParameters(methodBuilder, propertyBeanReferences);
            }

            // Collect ManagedList bean references from constructor arguments and add them as method parameters
            List<String> managedListBeanReferences = collectManagedListBeanReferences(constructorParams);
            addManagedListBeanReferenceParameters(methodBuilder, managedListBeanReferences);

            // Generate constructor call
            generateConstructorCall(methodBuilder, constructorParams, hasProperties);

            // Generate property setters if needed
            if (hasProperties) {
                generatePropertySetters(methodBuilder);
                // Add the return statement after property setters
                methodBuilder.addStatement("return bean");
            }
        }

        private List<ConstructorParameter> collectConstructorParameters(ConstructorArgumentValues constructorArgs) {
            List<ConstructorParameter> parameters = new ArrayList<>();

            // Process indexed arguments
            Map<Integer, ConstructorArgumentValues.ValueHolder> indexedArgs =
                    constructorArgs.getIndexedArgumentValues();
            for (Map.Entry<Integer, ConstructorArgumentValues.ValueHolder> entry : indexedArgs.entrySet()) {
                int index = entry.getKey();
                ConstructorArgumentValues.ValueHolder valueHolder = entry.getValue();
                parameters.add(createConstructorParameter(index, valueHolder));
            }

            // Process generic arguments
            List<ConstructorArgumentValues.ValueHolder> genericArgs = constructorArgs.getGenericArgumentValues();
            for (int i = 0; i < genericArgs.size(); i++) {
                ConstructorArgumentValues.ValueHolder valueHolder = genericArgs.get(i);
                // Generic args get placed after indexed args
                int index = indexedArgs.size() + i;
                parameters.add(createConstructorParameter(index, valueHolder));
            }

            // Sort by index to ensure proper order
            parameters.sort((p1, p2) -> Integer.compare(p1.index, p2.index));

            return parameters;
        }

        private ConstructorParameter createConstructorParameter(
                int index, ConstructorArgumentValues.ValueHolder valueHolder) {
            Object value = valueHolder.getValue();

            if (value instanceof RuntimeBeanReference beanRef) {
                String beanName = beanRef.getBeanName();

                // Use enhanced type inference from base class
                ClassName paramType = resolveParameterTypeWithInference(
                        beanName,
                        beanClassName,
                        index,
                        null,
                        beanDefinition.getConstructorArgumentValues(),
                        transpilationContext);

                return new ConstructorParameter(index, beanName, paramType, ParameterType.BEAN_REFERENCE);
            } else if (value instanceof TypedStringValue stringValue) {
                String rawValue = stringValue.getValue();

                // Determine the expected parameter type from the constructor
                Class<?> expectedType = getConstructorParameterType(index);

                if (isSpELExpression(rawValue)) {
                    // SpEL expression - create parameter with @Value annotation
                    String paramName = "spelParam" + index;
                    ClassName paramTypeClassName = expectedType != null
                            ? (expectedType.isPrimitive() ? getBoxedType(expectedType) : ClassName.get(expectedType))
                            : ClassName.get(Object.class);
                    return new ConstructorParameter(
                            index, paramName, paramTypeClassName, ParameterType.SPEL_EXPRESSION, rawValue);
                } else if (expectedType != null
                        && (expectedType == Class.class
                                || expectedType.getName().equals("java.lang.Class"))) {
                    // Constructor expects a Class parameter - convert string to Class literal
                    return new ConstructorParameter(
                            index, rawValue + ".class", ClassName.get(Class.class), ParameterType.CLASS_LITERAL);
                } else if (expectedType != null && isNumericType(expectedType)) {
                    // Constructor expects a numeric parameter - convert string to numeric literal
                    ClassName paramTypeClassName =
                            expectedType.isPrimitive() ? getBoxedType(expectedType) : ClassName.get(expectedType);
                    return new ConstructorParameter(index, rawValue, paramTypeClassName, ParameterType.NUMERIC_LITERAL);
                } else if (expectedType != null
                        && !expectedType.equals(String.class)
                        && hasStringConstructor(expectedType)) {
                    // Constructor expects a non-String type that can be constructed from a string
                    ClassName paramTypeClassName = ClassName.get(expectedType);
                    String constructorCall = "new " + expectedType.getName() + "(\"" + rawValue + "\")";
                    return new ConstructorParameter(
                            index, constructorCall, paramTypeClassName, ParameterType.CONSTRUCTOR_CALL);
                } else {
                    // Default to string literal
                    return new ConstructorParameter(
                            index, rawValue, ClassName.get(String.class), ParameterType.LITERAL_VALUE);
                }
            } else if (value instanceof org.springframework.beans.factory.support.ManagedList<?> managedList) {

                // Generate List.of() call with bean references - this will be handled specially in constructor
                // generation
                return new ConstructorParameter(
                        index,
                        generateManagedListCall(managedList),
                        ClassName.get(java.util.List.class),
                        ParameterType.MANAGED_LIST);
            } else {
                // Handle other types
                return new ConstructorParameter(
                        index,
                        "null /* Unsupported: " + value.getClass().getSimpleName() + " */",
                        ClassName.get(Object.class),
                        ParameterType.UNSUPPORTED);
            }
        }

        /**
         * Check if a class represents a numeric type that should be rendered as a literal.
         */
        private boolean isNumericType(Class<?> type) {
            return type == int.class
                    || type == Integer.class
                    || type == long.class
                    || type == Long.class
                    || type == short.class
                    || type == Short.class
                    || type == byte.class
                    || type == Byte.class
                    || type == float.class
                    || type == Float.class
                    || type == double.class
                    || type == Double.class
                    || type == boolean.class
                    || type == Boolean.class;
        }

        /**
         * Check if a string value is a SpEL (Spring Expression Language) expression.
         * SpEL expressions are always treated as string literals regardless of target parameter type.
         */
        private boolean isSpELExpression(String value) {
            return value != null && value.contains("#{") && value.contains("}");
        }

        /**
         * Check if a type has a constructor that accepts a single String parameter.
         */
        private boolean hasStringConstructor(Class<?> type) {
            try {
                // Check for common types that have String constructors
                if (type.getName().equals("org.geotools.util.Version")) {
                    return true;
                }

                // Use reflection to check for String constructor
                type.getConstructor(String.class);
                return true;
            } catch (NoSuchMethodException e) {
                return false;
            }
        }

        /**
         * Get the parameter type for a specific constructor parameter index using reflection.
         */
        private Class<?> getConstructorParameterType(int paramIndex) {
            if (beanClassName == null) {
                return null;
            }

            try {
                Class<?> beanClass = Class.forName(beanClassName);
                ConstructorArgumentValues constructorArgs = beanDefinition.getConstructorArgumentValues();

                if (constructorArgs.isEmpty()) {
                    // For implicit constructor autowiring, find the constructor
                    java.lang.reflect.Constructor<?> constructor = findConstructorForAutowiring(beanClass);
                    if (constructor != null && paramIndex < constructor.getParameterCount()) {
                        return constructor.getParameterTypes()[paramIndex];
                    }
                } else {
                    // For explicit constructor arguments, find constructor by argument count
                    // Prefer public constructors over non-public ones
                    int argCount = constructorArgs.getArgumentCount();
                    java.lang.reflect.Constructor<?>[] constructors = beanClass.getDeclaredConstructors();

                    // First try to find a public constructor with matching parameter count
                    for (java.lang.reflect.Constructor<?> constructor : constructors) {
                        if (constructor.getParameterCount() == argCount
                                && paramIndex < constructor.getParameterCount()
                                && java.lang.reflect.Modifier.isPublic(constructor.getModifiers())) {
                            return constructor.getParameterTypes()[paramIndex];
                        }
                    }

                    // If no public constructor found, fall back to any constructor with matching count
                    for (java.lang.reflect.Constructor<?> constructor : constructors) {
                        if (constructor.getParameterCount() == argCount
                                && paramIndex < constructor.getParameterCount()) {
                            return constructor.getParameterTypes()[paramIndex];
                        }
                    }
                }
            } catch (Exception e) {
                // Fall back to null if we can't determine the type
            }

            return null;
        }

        private void addMethodParameters(
                MethodSpec.Builder methodBuilder, List<ConstructorParameter> constructorParams) {
            for (ConstructorParameter param : constructorParams) {
                if (param.type == ParameterType.BEAN_REFERENCE) {
                    // Explicit bean reference with @Qualifier
                    ParameterSpec.Builder paramBuilder = ParameterSpec.builder(param.paramType, param.name)
                            .addAnnotation(AnnotationSpec.builder(Qualifier.class)
                                    .addMember("value", "$S", param.name)
                                    .build());
                    methodBuilder.addParameter(paramBuilder.build());
                } else if (param.type == ParameterType.SPEL_EXPRESSION) {
                    // SpEL expression with @Value annotation
                    ParameterSpec.Builder paramBuilder = ParameterSpec.builder(param.paramType, param.name)
                            .addAnnotation(
                                    AnnotationSpec.builder(org.springframework.beans.factory.annotation.Value.class)
                                            .addMember("value", "$S", param.spelExpression)
                                            .build());
                    methodBuilder.addParameter(paramBuilder.build());
                } else if (param.type == ParameterType.IMPLICIT_AUTOWIRED) {
                    // Implicit autowiring by type - no @Qualifier annotation
                    ParameterSpec paramSpec =
                            ParameterSpec.builder(param.paramType, param.name).build();
                    methodBuilder.addParameter(paramSpec);
                }
            }
        }

        private void generateConstructorCall(
                MethodSpec.Builder methodBuilder, List<ConstructorParameter> constructorParams, boolean hasProperties) {
            ClassName returnType = getReturnType();

            if (constructorParams.isEmpty()) {
                // No constructor arguments - simple instantiation
                if (hasProperties) {
                    methodBuilder.addStatement("$T bean = new $T()", returnType, returnType);
                    // Property setters and return statement will be added later
                } else {
                    methodBuilder.addStatement("return new $T()", returnType);
                }
                return;
            }

            // Check if we need reflection-based instantiation for protected/private constructors
            if (requiresReflectionBasedInstantiation(constructorParams)) {
                generateReflectionBasedConstructorCall(methodBuilder, constructorParams, hasProperties);
                return;
            }

            // Build constructor arguments list
            List<String> constructorArgs = new ArrayList<>();
            for (ConstructorParameter param : constructorParams) {
                switch (param.type) {
                    case BEAN_REFERENCE:
                        constructorArgs.add(param.name);
                        break;
                    case LITERAL_VALUE:
                        constructorArgs.add("\"" + param.name + "\"");
                        break;
                    case NUMERIC_LITERAL:
                        constructorArgs.add(param.name); // Numeric literals don't need quotes
                        break;
                    case CLASS_LITERAL:
                        constructorArgs.add(param.name); // Already includes ".class" suffix
                        break;
                    case CONSTRUCTOR_CALL:
                        constructorArgs.add(param.name); // The name contains the constructor call
                        break;
                    case SPEL_EXPRESSION:
                        // SpEL expressions need casting to the expected type
                        if (param.paramType.equals(ClassName.get(Object.class))) {
                            constructorArgs.add(param.name);
                        } else {
                            constructorArgs.add("(" + param.paramType.simpleName() + ") " + param.name);
                        }
                        break;
                    case IMPLICIT_AUTOWIRED:
                        constructorArgs.add(param.name);
                        break;
                    case MANAGED_LIST:
                        constructorArgs.add(param.name); // The name contains the List.of() call
                        break;
                    case UNSUPPORTED:
                        constructorArgs.add(param.name); // Already includes comment
                        break;
                    default:
                        throw new UnsupportedOperationException("Unknown parameter type: " + param.type);
                }
            }

            // Generate the constructor call
            if (hasProperties) {
                // Need to create bean variable for property setting
                StringBuilder constructorCall = new StringBuilder("$T bean = new $T(");
                for (int i = 0; i < constructorArgs.size(); i++) {
                    if (i > 0) {
                        constructorCall.append(", ");
                    }
                    constructorCall.append(constructorArgs.get(i));
                }
                constructorCall.append(")");
                methodBuilder.addStatement(constructorCall.toString(), returnType, returnType);
                // Property setters and return statement will be added later
            } else {
                // Direct return from constructor
                StringBuilder constructorCall = new StringBuilder("return new $T(");
                for (int i = 0; i < constructorArgs.size(); i++) {
                    if (i > 0) {
                        constructorCall.append(", ");
                    }
                    constructorCall.append(constructorArgs.get(i));
                }
                constructorCall.append(")");
                methodBuilder.addStatement(constructorCall.toString(), returnType);
            }
        }

        /**
         * Collect constructor parameters for implicit constructor autowiring.
         * Analyzes the bean class constructor using reflection to determine required parameters.
         */
        private List<ConstructorParameter> collectImplicitConstructorParameters() {
            List<ConstructorParameter> parameters = new ArrayList<>();

            try {
                Class<?> beanClass = Class.forName(beanClassName);
                java.lang.reflect.Constructor<?> constructor = findConstructorForAutowiring(beanClass);

                if (constructor == null) {
                    return parameters; // No autowirable constructor found
                }

                // Generate parameters from constructor parameter types
                Class<?>[] paramTypes = constructor.getParameterTypes();
                for (int i = 0; i < paramTypes.length; i++) {
                    Class<?> paramType = paramTypes[i];
                    String paramName = generateParameterNameFromType(paramType);
                    ClassName paramClassName = ClassName.get(paramType);

                    parameters.add(
                            new ConstructorParameter(i, paramName, paramClassName, ParameterType.IMPLICIT_AUTOWIRED));
                }

            } catch (ClassNotFoundException e) {
                // Cannot analyze class - return empty parameters
            }

            return parameters;
        }

        /**
         * Find the constructor to use for implicit autowiring.
         * Follows the same logic as requiresImplicitConstructorAutowiring().
         */
        private java.lang.reflect.Constructor<?> findConstructorForAutowiring(Class<?> beanClass) {
            // Check for single public constructor
            java.lang.reflect.Constructor<?>[] publicConstructors = beanClass.getConstructors();
            if (publicConstructors.length == 1) {
                java.lang.reflect.Constructor<?> constructor = publicConstructors[0];
                if (constructor.getParameterCount() > 0) {
                    return constructor;
                }
            }

            // Check for single declared constructor (including protected/private)
            java.lang.reflect.Constructor<?>[] allConstructors = beanClass.getDeclaredConstructors();
            if (allConstructors.length == 1) {
                java.lang.reflect.Constructor<?> constructor = allConstructors[0];
                if (constructor.getParameterCount() > 0) {
                    return constructor;
                }
            }

            return null; // No suitable constructor found
        }

        /**
         * Generate a meaningful parameter name from the parameter type.
         * Converts "GeoServer" -> "geoServer", "GeoServerDataDirectory" -> "geoServerDataDirectory"
         */
        private String generateParameterNameFromType(Class<?> paramType) {
            String typeName = paramType.getSimpleName();

            // Handle special cases
            if (typeName.equals("GeoServerDataDirectory")) {
                return "dataDirectory"; // Standard Spring naming for this type
            }
            if (typeName.equals("GeoServer")) {
                return "geoServer"; // Standard naming
            }

            // Convert first letter to lowercase
            return Character.toLowerCase(typeName.charAt(0)) + typeName.substring(1);
        }

        /**
         * Check if the constructor requires reflection-based instantiation.
         * This is needed for protected or private constructors.
         */
        private boolean requiresReflectionBasedInstantiation(List<ConstructorParameter> constructorParams) {
            try {
                Class<?> beanClass = Class.forName(beanClassName);
                java.lang.reflect.Constructor<?> constructor;

                // Check for implicit autowired parameters first
                boolean hasImplicitParams =
                        constructorParams.stream().anyMatch(param -> param.type == ParameterType.IMPLICIT_AUTOWIRED);

                if (hasImplicitParams) {
                    // For implicit autowiring, find the constructor for autowiring
                    constructor = findConstructorForAutowiring(beanClass);
                } else {
                    // For explicit parameters, find constructor with matching parameter count
                    // Prefer public constructors over non-public ones
                    int paramCount = constructorParams.size();
                    java.lang.reflect.Constructor<?>[] constructors = beanClass.getDeclaredConstructors();
                    constructor = null;

                    // First try to find a public constructor with matching parameter count
                    for (java.lang.reflect.Constructor<?> c : constructors) {
                        if (c.getParameterCount() == paramCount
                                && java.lang.reflect.Modifier.isPublic(c.getModifiers())) {
                            constructor = c;
                            break;
                        }
                    }

                    // If no public constructor found, fall back to any constructor with matching count
                    if (constructor == null) {
                        for (java.lang.reflect.Constructor<?> c : constructors) {
                            if (c.getParameterCount() == paramCount) {
                                constructor = c;
                                break;
                            }
                        }
                    }
                }

                if (constructor == null) {
                    return false;
                }

                // Check if constructor is public
                return !java.lang.reflect.Modifier.isPublic(constructor.getModifiers());

            } catch (Exception e) {
                return false; // Default to regular instantiation if we can't determine
            }
        }

        /**
         * Generate reflection-based constructor call for protected/private constructors.
         */
        private void generateReflectionBasedConstructorCall(
                MethodSpec.Builder methodBuilder, List<ConstructorParameter> constructorParams, boolean hasProperties) {
            ClassName returnType = getReturnType();

            // Build parameter types array for getDeclaredConstructor
            StringBuilder parameterTypes = new StringBuilder();
            for (int i = 0; i < constructorParams.size(); i++) {
                if (i > 0) {
                    parameterTypes.append(", ");
                }
                parameterTypes.append(constructorParams.get(i).paramType).append(".class");
            }

            // Build parameter values for newInstance
            StringBuilder parameterValues = new StringBuilder();
            for (int i = 0; i < constructorParams.size(); i++) {
                if (i > 0) {
                    parameterValues.append(", ");
                }
                parameterValues.append(constructorParams.get(i).name);
            }

            // Generate reflection-based instantiation
            methodBuilder.addStatement(
                    "java.lang.reflect.Constructor constructor = java.lang.Class.forName($S).getDeclaredConstructor($L)",
                    beanClassName,
                    parameterTypes.toString());
            methodBuilder.addStatement("constructor.setAccessible(true)");

            if (hasProperties) {
                methodBuilder.addStatement(
                        "$T bean = ($T) constructor.newInstance($L)",
                        returnType,
                        returnType,
                        parameterValues.toString());
                // Property setters and return statement will be added later
            } else {
                methodBuilder.addStatement(
                        "return ($T) constructor.newInstance($L)", returnType, parameterValues.toString());
            }
        }

        /**
         * Collect bean references from property values.
         * Similar to SimpleBeanVisitor.collectPropertyBeanReferences().
         */
        private List<String> collectPropertyBeanReferences() {
            List<String> beanReferences = new ArrayList<>();

            for (org.springframework.beans.PropertyValue pv :
                    beanDefinition.getPropertyValues().getPropertyValues()) {
                Object value = pv.getValue();
                if (value instanceof org.springframework.beans.factory.config.RuntimeBeanReference beanRef) {
                    beanReferences.add(beanRef.getBeanName());
                } else if (value instanceof org.springframework.beans.factory.support.ManagedProperties managedProps) {
                    for (Object propVal : managedProps.values()) {
                        String propValStr = extractStringValue(propVal);
                        String simpleBeanRef = extractSimpleSpelBeanReference(propValStr);
                        if (simpleBeanRef != null && !beanReferences.contains(simpleBeanRef)) {
                            beanReferences.add(simpleBeanRef);
                        }
                    }
                } else if (value instanceof org.springframework.beans.factory.support.ManagedList<?> managedList) {
                    List<String> listBeanRefs = collectBeanReferencesFromManagedList(managedList);
                    for (String beanRef : listBeanRefs) {
                        if (!beanReferences.contains(beanRef)) {
                            beanReferences.add(beanRef);
                        }
                    }
                }
            }

            return beanReferences;
        }

        /**
         * Collect bean references from ManagedList constructor parameters.
         */
        private List<String> collectManagedListBeanReferences(List<ConstructorParameter> constructorParams) {
            List<String> beanReferences = new ArrayList<>();

            for (ConstructorParameter param : constructorParams) {
                if (param.type == ParameterType.MANAGED_LIST) {
                    // Extract the original ManagedList from the constructor arguments to get bean references
                    ConstructorArgumentValues constructorArgs = beanDefinition.getConstructorArgumentValues();
                    List<ConstructorArgumentValues.ValueHolder> allArgs = getAllConstructorArguments(constructorArgs);

                    if (param.index < allArgs.size()) {
                        Object value = allArgs.get(param.index).getValue();
                        if (value instanceof org.springframework.beans.factory.support.ManagedList<?> managedList) {
                            beanReferences.addAll(collectBeanReferencesFromManagedList(managedList));
                        }
                    }
                }
            }

            return beanReferences;
        }

        /**
         * Get all constructor arguments in order (indexed + generic).
         * Based on the logic from the old spring-factory-processor ConstructorGenerator.
         */
        private List<ConstructorArgumentValues.ValueHolder> getAllConstructorArguments(
                ConstructorArgumentValues constructorArgs) {
            Map<Integer, ConstructorArgumentValues.ValueHolder> indexedArgs =
                    constructorArgs.getIndexedArgumentValues();
            List<ConstructorArgumentValues.ValueHolder> genericArgs =
                    new ArrayList<>(constructorArgs.getGenericArgumentValues());

            List<ConstructorArgumentValues.ValueHolder> allArgs = new ArrayList<>();

            // First add indexed arguments in order
            int maxIndex = indexedArgs.keySet().stream()
                    .mapToInt(Integer::intValue)
                    .max()
                    .orElse(-1);
            for (int i = 0; i <= maxIndex; i++) {
                if (indexedArgs.containsKey(i)) {
                    allArgs.add(indexedArgs.get(i));
                } else {
                    if (!genericArgs.isEmpty()) {
                        allArgs.add(genericArgs.remove(0));
                    } else {
                        throw new IllegalStateException("Missing constructor argument at index " + i);
                    }
                }
            }

            allArgs.addAll(genericArgs);
            return allArgs;
        }

        /**
         * Add method parameters for ManagedList bean references.
         */
        private void addManagedListBeanReferenceParameters(
                MethodSpec.Builder methodBuilder, List<String> beanReferences) {
            for (String beanRef : beanReferences) {
                ClassName paramType = resolveParameterType(beanRef, transpilationContext);
                ParameterSpec.Builder paramBuilder = ParameterSpec.builder(paramType, beanRef)
                        .addAnnotation(
                                AnnotationSpec.builder(org.springframework.beans.factory.annotation.Qualifier.class)
                                        .addMember("value", "$S", beanRef)
                                        .build());
                methodBuilder.addParameter(paramBuilder.build());
            }
        }

        /**
         * Add method parameters for property bean references.
         */
        private void addPropertyBeanReferenceParameters(MethodSpec.Builder methodBuilder, List<String> beanReferences) {

            for (String beanRef : beanReferences) {
                ClassName paramType = resolveParameterType(beanRef, transpilationContext);
                ParameterSpec.Builder paramBuilder = ParameterSpec.builder(paramType, beanRef)
                        .addAnnotation(
                                AnnotationSpec.builder(org.springframework.beans.factory.annotation.Qualifier.class)
                                        .addMember("value", "$S", beanRef)
                                        .build());
                methodBuilder.addParameter(paramBuilder.build());
            }
        }

        /**
         * Generate property setter method calls for bean properties.
         */
        private void generatePropertySetters(MethodSpec.Builder methodBuilder) {
            if (!beanDefinition.hasPropertyValues()) {
                return;
            }

            for (org.springframework.beans.PropertyValue pv :
                    beanDefinition.getPropertyValues().getPropertyValues()) {
                String propertyName = pv.getName();
                Object value = pv.getValue();

                // Generate setter method name
                String setterName = "set" + Character.toUpperCase(propertyName.charAt(0)) + propertyName.substring(1);

                if (value instanceof org.springframework.beans.factory.config.RuntimeBeanReference beanRef) {
                    String beanName = beanRef.getBeanName();
                    methodBuilder.addStatement("bean.$L($N)", setterName, beanName);
                } else if (value instanceof org.springframework.beans.factory.config.TypedStringValue stringValue) {
                    String rawValue = stringValue.getValue();

                    // Use Spring BeanWrapper-based type conversion like SimpleBeanVisitor
                    generateTypedPropertySetterCall(methodBuilder, setterName, propertyName, rawValue, beanClassName);
                } else if (value instanceof org.springframework.beans.factory.support.ManagedProperties managedProps) {
                    // Handle ManagedProperties (like <props>)
                    methodBuilder.addComment("// Property '" + propertyName + "' uses ManagedProperties");
                    methodBuilder.addStatement(
                            "java.util.Properties $L = new java.util.Properties()", propertyName + "Props");

                    for (Object key : managedProps.keySet()) {
                        Object val = managedProps.get(key);
                        String keyStr = extractStringValue(key);
                        String valStr = extractStringValue(val);

                        // Check if the value is a simple SpEL bean reference
                        String simpleBeanRef = extractSimpleSpelBeanReference(valStr);
                        if (simpleBeanRef != null) {
                            // Use bean reference parameter variable instead of literal SpEL
                            methodBuilder.addStatement(
                                    "$L.setProperty($S, $N)", propertyName + "Props", keyStr, simpleBeanRef);
                        } else {
                            // Use literal string value
                            methodBuilder.addStatement(
                                    "$L.setProperty($S, $S)", propertyName + "Props", keyStr, valStr);
                        }
                    }

                    methodBuilder.addStatement("bean.$L($L)", setterName, propertyName + "Props");
                } else if (value instanceof org.springframework.beans.factory.support.ManagedMap<?, ?> managedMap) {
                    // Handle ManagedMap (like <map>)
                    methodBuilder.addComment("// Property '" + propertyName + "' uses ManagedMap");
                    methodBuilder.addStatement(
                            "java.util.Map<String, Object> $L = new java.util.HashMap<>()", propertyName + "Map");

                    for (Map.Entry<?, ?> entry : managedMap.entrySet()) {
                        String keyStr = extractStringValue(entry.getKey());
                        String valStr = extractStringValue(entry.getValue());
                        methodBuilder.addStatement("$L.put($S, $S)", propertyName + "Map", keyStr, valStr);
                    }

                    methodBuilder.addStatement("bean.$L($L)", setterName, propertyName + "Map");
                } else if (value instanceof org.springframework.beans.factory.support.ManagedList<?> managedList) {
                    // Handle ManagedList (like <list> with bean references for method arguments)
                    methodBuilder.addComment("// Property '" + propertyName + "' uses ManagedList");

                    String listCall = generateManagedListCall(managedList);
                    methodBuilder.addStatement("bean.$L($L)", setterName, listCall);
                } else if (value instanceof org.springframework.beans.factory.config.BeanDefinitionHolder) {
                    // Nested bean definitions are not supported
                    throw new UnsupportedOperationException(
                            "Nested bean definitions are not supported. Found nested bean in property '" + propertyName
                                    + "' of bean '" + beanName + "'");
                } else {
                    // Fallback for other value types
                    methodBuilder.addComment("TODO: Handle property '" + propertyName + "' of type "
                            + value.getClass().getSimpleName());
                }
            }
        }
    }

    /**
     * Helper class to represent a constructor parameter with proper typing.
     */
    private static class ConstructorParameter {
        final int index;
        final String name;
        final ClassName paramType;
        final ParameterType type;
        final String spelExpression; // For SpEL expressions, stores the original expression

        ConstructorParameter(int index, String name, ClassName paramType, ParameterType type) {
            this(index, name, paramType, type, null);
        }

        ConstructorParameter(int index, String name, ClassName paramType, ParameterType type, String spelExpression) {
            this.index = index;
            this.name = name;
            this.paramType = paramType;
            this.type = type;
            this.spelExpression = spelExpression;
        }
    }

    /**
     * Enum to represent different types of constructor parameters.
     */
    private enum ParameterType {
        BEAN_REFERENCE,
        LITERAL_VALUE,
        NUMERIC_LITERAL,
        CLASS_LITERAL,
        CONSTRUCTOR_CALL,
        SPEL_EXPRESSION,
        IMPLICIT_AUTOWIRED,
        MANAGED_LIST,
        UNSUPPORTED
    }
}
