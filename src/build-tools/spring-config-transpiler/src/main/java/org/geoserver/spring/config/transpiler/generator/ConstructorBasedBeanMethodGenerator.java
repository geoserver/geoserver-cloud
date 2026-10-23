/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.spring.config.transpiler.generator;

import static org.geoserver.spring.config.transpiler.xml.EnhancedBeanDefinition.sanitizeBeanName;

import com.palantir.javapoet.AnnotationSpec;
import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.CodeBlock;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.ParameterSpec;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.lang.model.element.Modifier;
import org.geoserver.spring.config.transpiler.context.BeanGenerationContext;
import org.geoserver.spring.config.transpiler.context.TranspilationContext;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.config.TypedStringValue;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.beans.factory.support.ManagedMap;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;

/**
 * Generator for generating {@code @Bean} methods for beans with constructor arguments.
 *
 * <p>This generator handles Spring bean definitions that specify constructor arguments for dependency injection. It
 * generates {@code @Bean} methods with proper type resolution and parameter handling.
 *
 * @since 3.0.0
 */
@SuppressWarnings("java:S1192")
public class ConstructorBasedBeanMethodGenerator extends AbstractBeanMethodGenerator {

    @Override
    public boolean canHandle(BeanDefinition beanDefinition) {
        // Handle beans with explicit constructor arguments (but not factory-method beans)
        boolean hasConstructorArgumentValues = beanDefinition.hasConstructorArgumentValues();
        boolean hasClassName = beanDefinition.getBeanClassName() != null;
        boolean isAbstract = beanDefinition.isAbstract();
        boolean hasFactoryMethodName = beanDefinition.getFactoryMethodName() != null;
        if (hasConstructorArgumentValues && hasClassName && !isAbstract && !hasFactoryMethodName) {
            return true;
        }

        // Handle beans with implicit constructor autowiring (no explicit constructor args but class needs injection)
        if (hasClassName && !isAbstract && !hasConstructorArgumentValues && !hasFactoryMethodName) {
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
     * Determine if a bean class requires implicit constructor autowiring. This happens when: 1. The class has no
     * default (no-arg) constructor 2. The class has exactly one constructor that should be autowired
     *
     * <p>Based on the logic from the old BuildTimeXmlImportProcessor.
     */
    @SuppressWarnings("java:S1141")
    private boolean requiresImplicitConstructorAutowiring(String beanClassName) {
        Class<?> beanClass;
        try {
            beanClass = Reflections.convertToRuntimeClass(beanClassName);
        } catch (RuntimeException _) {
            // Cannot load class - let other generators handle it
            return false;
        }
        try {

            // First check for default constructor (any visibility)
            try {
                beanClass.getDeclaredConstructor(); // Try no-args constructor
                return false; // Has default constructor, no implicit autowiring needed
            } catch (NoSuchMethodException _) {
                // No default constructor found
            }

            // Check for single public constructor
            Constructor<?>[] publicConstructors = beanClass.getConstructors();
            if (publicConstructors.length == 1) {
                Constructor<?> constructor = publicConstructors[0];
                // Only autowire if constructor has parameters
                return constructor.getParameterCount() > 0;
            }

            // Check for single declared constructor (including protected/private)
            Constructor<?>[] allConstructors = beanClass.getDeclaredConstructors();
            if (allConstructors.length == 1) {
                Constructor<?> constructor = allConstructors[0];
                // Only autowire if constructor has parameters
                return constructor.getParameterCount() > 0;
            }

            // Multiple constructors or other edge cases - no implicit autowiring
            return false;
        } catch (Exception _) {
            // Any other reflection error - skip implicit autowiring
            return false;
        }
    }

    /** Inner class to encapsulate method generation logic and reduce parameter passing. */
    private class ConstructorBeanMethodGenerator {
        private final BeanGenerationContext beanContext;
        private final TranspilationContext transpilationContext;
        private final BeanDefinition beanDefinition;
        private final String beanName;
        private final String beanClassName;
        private final Class<?> beanClass;
        private final @Nullable Constructor<?> targetConstructor;

        public ConstructorBeanMethodGenerator(BeanGenerationContext beanContext) {
            this.beanContext = beanContext;
            this.transpilationContext = beanContext.getTranspilationContext();
            this.beanDefinition = beanContext.getBeanDefinition();
            this.beanName = beanContext.getBeanName();
            this.beanClassName = beanDefinition.getBeanClassName();
            this.beanClass = beanContext.getResolvedBeanClass();
            this.targetConstructor =
                    Reflections.resolveTargetConstructor(beanClass, beanDefinition.getConstructorArgumentValues());
            BeanAccessibility.requirePublic(transpilationContext, beanName, beanClass, targetConstructor);
        }

        public MethodSpec generate() {
            // Use consolidated EnhancedBeanInfo for proper method name
            String methodName = beanContext.getSanitizedMethodName();

            // For auto-generated bean names, add unique suffix to avoid collisions across configurations
            if (beanContext.isAutoGenerated()) {
                String uniqueSuffix = beanContext.getTranspilationContext().getUniqueMethodSuffix();
                methodName = methodName + "_" + uniqueSuffix;
            }

            // @Bean methods are always package-private per Spring conventions
            // Only @Configuration class visibility is controlled by publicAccess
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
            @Nullable String scope = beanDefinition.getScope();
            if (scope != null && !scope.isEmpty() && !"singleton".equals(scope)) {
                AnnotationSpec scopeAnnotation = AnnotationSpec.builder(Scope.class)
                        .addMember("value", "$S", beanDefinition.getScope())
                        .build();
                methodBuilder.addAnnotation(scopeAnnotation);
            }

            // Add @DependsOn annotation if bean has depends-on attribute
            addDependsOnAnnotation(methodBuilder, beanDefinition.getDependsOn());
        }

        private ClassName getReturnType() {
            return resolveReturnType(beanDefinition, transpilationContext);
        }

        private void addConstructorExceptions(MethodSpec.Builder methodBuilder) {
            addExceptionsFromConstructor(methodBuilder, targetConstructor);
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
            if (hasProperties) {
                List<String> propertyBeanReferences = collectPropertyBeanReferences(beanDefinition.getPropertyValues());
                addPropertyBeanReferenceParameters(methodBuilder, propertyBeanReferences);
            }

            // Collect ManagedList bean references from constructor arguments and add them as method parameters
            List<String> managedListBeanReferences = collectManagedListBeanReferences(constructorParams);
            addManagedListBeanReferenceParameters(methodBuilder, managedListBeanReferences);

            // Generate constructor call
            generateConstructorCall(methodBuilder, constructorParams, hasProperties);

            // Generate property setters if needed
            if (hasProperties) {
                // Use the unified generatePropertySetters method from base class  Pass null for beanReferences to use
                // ConstructorBasedBeanMethodGenerator approach
                generatePropertySetters(
                        methodBuilder, beanDefinition.getPropertyValues(), null, beanClassName, beanName);
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
            Object value = Objects.requireNonNull(valueHolder.getValue());

            return switch (value) {
                case RuntimeBeanReference beanRef -> beanRefConstructorParameter(index, beanRef);
                case TypedStringValue stringValue -> typedStringConstructorParameter(index, stringValue);
                case ManagedList<?> managedList -> managedListConstructorParameter(index, managedList);
                case ManagedMap<?, ?> managedMap -> mangedMapConstructorParameter(index, managedMap);
                default ->
                    throw new UnsupportedOperationException("Unsupported parameter index %d, type: %s"
                            .formatted(index, value.getClass().getSimpleName()));
            };
        }

        private ConstructorParameter mangedMapConstructorParameter(int index, ManagedMap<?, ?> managedMap) {
            // Handle ManagedMap (like <map> in constructor-arg)
            return new ConstructorParameter(
                    index,
                    generateManagedMapCall(managedMap),
                    ClassName.get(java.util.Map.class),
                    ParameterType.MANAGED_MAP);
        }

        private ConstructorParameter managedListConstructorParameter(int index, ManagedList<?> managedList) {
            // Check if the constructor parameter expects an array type
            Class<?> expectedType = getConstructorParameterType(index);
            if (expectedType != null && expectedType.isArray()) {
                // Generate array instead of list
                String arrayCall = generateManagedListAsArray(managedList, expectedType.getComponentType());
                // For array types, use the component type for ClassName and let the arrayCall handle the array syntax
                return new ConstructorParameter(
                        index, arrayCall, ClassName.get(expectedType.getComponentType()), ParameterType.MANAGED_ARRAY);
            } else {
                // Generate List.of() call with bean references - this will be handled specially in constructor
                // generation
                return new ConstructorParameter(
                        index,
                        generateManagedListCall(managedList),
                        ClassName.get(java.util.List.class),
                        ParameterType.MANAGED_LIST);
            }
        }

        private ConstructorParameter typedStringConstructorParameter(
                final int index, final TypedStringValue stringValue) {

            final String rawValue = Objects.requireNonNull(stringValue.getValue(), "null value unexpected here");
            if (rawValue.isEmpty()) {
                throw new IllegalStateException("constructor param %d is empty".formatted(index));
            }
            // Determine the expected parameter type from the constructor
            final Class<?> expectedType = getConstructorParameterType(index);

            String paramValue;
            ClassName paramTypeClassName = Reflections.classToClassName(expectedType);
            ParameterType parameterType;
            String spelExpression = null;
            if (isSpELExpression(rawValue)) {
                // SpEL expression - create parameter with @Value annotation
                paramValue = "spelParam" + index;
                parameterType = ParameterType.SPEL_EXPRESSION;
                spelExpression = rawValue;
            } else if (Class.class.isAssignableFrom(expectedType)) {
                // Constructor expects a Class parameter - convert string to Class literal
                // Convert $ notation to . notation for nested classes (e.g., Outer$Inner -> Outer.Inner)
                paramValue = rawValue.replace('$', '.') + ".class";
                paramTypeClassName = ClassName.get(Class.class);
                parameterType = ParameterType.CLASS_LITERAL;
            } else if (Reflections.isPrimitiveOrBoxedType(expectedType)) {
                // Constructor expects a numeric parameter - convert string to numeric literal
                paramValue = formatNumericLiteral(rawValue, expectedType);
                parameterType = ParameterType.PRIMITIVE_LITERAL;
            } else if (!expectedType.equals(String.class) && Reflections.hasStringConstructor(expectedType)) {
                // Constructor expects a non-String type that can be constructed from a string
                paramValue = "new " + expectedType.getName() + "(\"" + rawValue + "\")";
                parameterType = ParameterType.CONSTRUCTOR_CALL;
            } else {
                // Default to string literal
                paramValue = rawValue;
                paramTypeClassName = ClassName.get(String.class);
                parameterType = ParameterType.STRING_LITERAL;
            }
            return new ConstructorParameter(index, paramValue, paramTypeClassName, parameterType, spelExpression);
        }

        private ConstructorParameter beanRefConstructorParameter(int index, RuntimeBeanReference beanRef) {
            String beanRefName = beanRef.getBeanName();

            // Use enhanced type inference from base class
            ClassName paramType = resolveParameterTypeWithInference(
                    beanRefName,
                    beanClassName,
                    index,
                    null,
                    beanDefinition.getConstructorArgumentValues(),
                    transpilationContext);

            return new ConstructorParameter(
                    index, sanitizeBeanName(beanRefName), paramType, ParameterType.BEAN_REFERENCE, null, beanRefName);
        }

        /**
         * Format a numeric literal with the appropriate Java type suffix. For example, float values need an 'f' suffix
         * and long values need an 'L' suffix.
         */
        private String formatNumericLiteral(String rawValue, Class<?> expectedType) {
            boolean isFloat = expectedType == float.class || expectedType == Float.class;
            boolean isLong = expectedType == long.class || expectedType == Long.class;
            if (isFloat && !rawValue.endsWith("f") && !rawValue.endsWith("F")) {
                return rawValue + "f";
            } else if (isLong && !rawValue.endsWith("L") && !rawValue.endsWith("l")) {
                return rawValue + "L";
            }
            return rawValue;
        }

        /** Generate a {@code Map.of(...)} call for a ManagedMap constructor argument. */
        private String generateManagedMapCall(org.springframework.beans.factory.support.ManagedMap<?, ?> managedMap) {
            StringBuilder mapCall = new StringBuilder("new java.util.HashMap<>(java.util.Map.of(");
            boolean first = true;
            for (Map.Entry<?, ?> entry : managedMap.entrySet()) {
                if (!first) {
                    mapCall.append(", ");
                }
                first = false;
                String key = extractStringValue(entry.getKey());
                String val = extractStringValue(entry.getValue());
                mapCall.append("\"").append(key).append("\", \"").append(val).append("\"");
            }
            mapCall.append("))");
            return mapCall.toString();
        }

        /**
         * Check if a string value is a SpEL (Spring Expression Language) expression. SpEL expressions are always
         * treated as string literals regardless of target parameter type.
         */
        private boolean isSpELExpression(String value) {
            return value != null && value.contains("#{") && value.contains("}");
        }

        /** Get the parameter type for a specific constructor parameter index. */
        private Class<?> getConstructorParameterType(int paramIndex) {
            if (targetConstructor == null || paramIndex >= targetConstructor.getParameterCount()) {
                throw new IllegalStateException(
                        "Cannot resolve constructor parameter %d for %s".formatted(paramIndex, beanClassName));
            }
            return targetConstructor.getParameterTypes()[paramIndex];
        }

        private void addMethodParameters(
                MethodSpec.Builder methodBuilder, List<ConstructorParameter> constructorParams) {
            for (ConstructorParameter param : constructorParams) {
                if (param.type == ParameterType.BEAN_REFERENCE) {
                    // Explicit bean reference with @Qualifier
                    ParameterSpec.Builder paramBuilder = ParameterSpec.builder(param.paramType, param.name)
                            .addAnnotation(AnnotationSpec.builder(Qualifier.class)
                                    .addMember(
                                            "value",
                                            "$S",
                                            param.originalBeanName != null ? param.originalBeanName : param.name)
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
                generateNoArgsConstructor(methodBuilder, hasProperties, returnType);
            } else {
                requireTargetConstructor(constructorParams);
                generateRegularConstructorCall(methodBuilder, constructorParams, hasProperties, returnType);
            }
        }

        private void generateNoArgsConstructor(
                MethodSpec.Builder methodBuilder, boolean hasProperties, ClassName returnType) {
            if (hasProperties) { // No constructor arguments - simple instantiation
                methodBuilder.addStatement("$T bean = new $T()", returnType, returnType);
                // Property setters and return statement will be added later
            } else {
                methodBuilder.addStatement("return new $T()", returnType);
            }
        }

        private void generateRegularConstructorCall(
                MethodSpec.Builder methodBuilder,
                List<ConstructorParameter> constructorParams,
                boolean hasProperties,
                ClassName returnType) {
            List<String> constructorArgs = buildConstructorArgsList(constructorParams);

            // Generate the constructor call using CodeBlock to properly escape parameter names
            if (hasProperties) {
                // Need to create bean variable for property setting
                CodeBlock.Builder constructorCall =
                        CodeBlock.builder().add("$T bean = new $T(", returnType, returnType);
                for (int i = 0; i < constructorArgs.size(); i++) {
                    if (i > 0) {
                        constructorCall.add(", ");
                    }
                    constructorCall.add("$L", constructorArgs.get(i));
                }
                constructorCall.add(")");
                methodBuilder.addStatement(constructorCall.build());
                // Property setters and return statement will be added later
            } else {
                // Direct return from constructor
                CodeBlock.Builder constructorCall = CodeBlock.builder().add("return new $T(", returnType);
                for (int i = 0; i < constructorArgs.size(); i++) {
                    if (i > 0) {
                        constructorCall.add(", ");
                    }
                    constructorCall.add("$L", constructorArgs.get(i));
                }
                constructorCall.add(")");
                methodBuilder.addStatement(constructorCall.build());
            }
        }

        private List<String> buildConstructorArgsList(List<ConstructorParameter> constructorParams) {
            // Build constructor arguments list
            List<String> constructorArgs = new ArrayList<>();
            for (ConstructorParameter param : constructorParams) {
                switch (param.type) {
                    case BEAN_REFERENCE:
                        constructorArgs.add(param.name);
                        break;
                    case STRING_LITERAL:
                        constructorArgs.add("\"" + param.name + "\"");
                        break;
                    case PRIMITIVE_LITERAL:
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
                    case MANAGED_ARRAY:
                        constructorArgs.add(param.name); // The name contains the array initialization
                        break;
                    case MANAGED_MAP:
                        constructorArgs.add(param.name); // The name contains the Map.of() call
                        break;
                    case UNSUPPORTED:
                        constructorArgs.add(param.name); // Already includes comment
                        break;
                    default:
                        throw new UnsupportedOperationException("Unknown parameter type: " + param.type);
                }
            }
            return constructorArgs;
        }

        /**
         * Collect constructor parameters for implicit constructor autowiring using the pre-resolved target constructor.
         */
        private List<ConstructorParameter> collectImplicitConstructorParameters() {
            if (targetConstructor == null) {
                return List.of();
            }

            List<ConstructorParameter> parameters = new ArrayList<>();
            Class<?>[] paramTypes = targetConstructor.getParameterTypes();
            for (int i = 0; i < paramTypes.length; i++) {
                String paramName = generateParameterNameFromType(paramTypes[i]);
                ClassName paramClassName = ClassName.get(paramTypes[i]);
                parameters.add(
                        new ConstructorParameter(i, paramName, paramClassName, ParameterType.IMPLICIT_AUTOWIRED));
            }
            return parameters;
        }

        /**
         * Generate a meaningful parameter name from the parameter type. Converts "GeoServer" -> "geoServer",
         * "GeoServerDataDirectory" -> "geoServerDataDirectory"
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

        /** The constructor call with arguments needs the resolved constructor for its parameter types. */
        private void requireTargetConstructor(List<ConstructorParameter> constructorParams) {
            if (targetConstructor == null) {
                throw new IllegalStateException("No constructor found for %s with %d parameters"
                        .formatted(beanClassName, constructorParams.size()));
            }
        }

        /**
         * Collect bean references from ManagedList constructor parameters. Returns original bean names (not sanitized)
         * for use in @Qualifier annotations.
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
                        if (value instanceof ManagedList<?> managedList) {
                            // Keep original names (base method already returns original names)
                            collectBeanReferencesFromManagedList(managedList).forEach(beanReferences::add);
                        }
                    }
                }
            }

            return beanReferences;
        }

        /**
         * Get all constructor arguments in order (indexed + generic). Based on the logic from the old
         * spring-factory-processor ConstructorGenerator.
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
                    if (genericArgs.isEmpty()) {
                        throw new IllegalStateException("Missing constructor argument at index " + i);
                    } else {
                        allArgs.add(genericArgs.removeFirst());
                    }
                }
            }

            allArgs.addAll(genericArgs);
            return allArgs;
        }

        /** Add method parameters for ManagedList bean references with generic type inference. */
        private void addManagedListBeanReferenceParameters(
                MethodSpec.Builder methodBuilder, List<String> beanReferences) {

            // Map bean references to their inferred types from constructor parameter generic types
            Map<String, ClassName> beanRefToTypeMap = new HashMap<>();
            // Map sanitized bean names to their original bean names for @Qualifier annotations
            Map<String, String> sanitizedToOriginalNameMap = new HashMap<>();

            // Get constructor arguments to analyze generic types
            ConstructorArgumentValues constructorArgs = beanDefinition.getConstructorArgumentValues();

            resolveConstructorArguments(beanRefToTypeMap, sanitizedToOriginalNameMap, constructorArgs);

            addParametersWithInferredTypes(methodBuilder, beanReferences, beanRefToTypeMap);
        }

        private void resolveConstructorArguments(
                Map<String, ClassName> beanRefToTypeMap,
                Map<String, String> sanitizedToOriginalNameMap,
                ConstructorArgumentValues constructorArgs) {

            // Check indexed constructor arguments
            Map<Integer, ConstructorArgumentValues.ValueHolder> indexedArgs =
                    constructorArgs.getIndexedArgumentValues();

            for (Map.Entry<Integer, ConstructorArgumentValues.ValueHolder> entry : indexedArgs.entrySet()) {
                int index = entry.getKey();
                Object value = entry.getValue().getValue();

                collectConstructorArguments(
                        index, value, constructorArgs, beanRefToTypeMap, sanitizedToOriginalNameMap);
            }

            // Also check generic (non-indexed) constructor arguments
            List<ConstructorArgumentValues.ValueHolder> genericArgs = constructorArgs.getGenericArgumentValues();
            for (int index = 0; index < genericArgs.size(); index++) {
                ConstructorArgumentValues.ValueHolder holder = genericArgs.get(index);
                Object value = holder.getValue();

                collectConstructorArguments(
                        index, value, constructorArgs, beanRefToTypeMap, sanitizedToOriginalNameMap);
            }
        }

        private void collectConstructorArguments(
                int index,
                Object value,
                ConstructorArgumentValues constructorArgs,
                Map<String, ClassName> beanRefToTypeMap,
                Map<String, String> sanitizedToOriginalNameMap) {
            if (value instanceof ManagedList<?> managedList) {
                // Get the generic element type for this list parameter
                int argumentCount = constructorArgs.getArgumentCount();
                ClassName elementType = Reflections.getGenericCollectionElementType(index, beanClass, argumentCount);
                if (elementType != null) {
                    // Apply the generic element type to all bean references in this list
                    for (Object listItem : managedList) {
                        if (listItem instanceof RuntimeBeanReference beanRef) {
                            String originalBeanName = beanRef.getBeanName();
                            String sanitizedBeanName = sanitizeBeanName(originalBeanName);
                            // Use sanitized bean name as key to match beanReferences list
                            beanRefToTypeMap.put(sanitizedBeanName, elementType);
                            // Store mapping from sanitized to original name for @Qualifier
                            sanitizedToOriginalNameMap.put(sanitizedBeanName, originalBeanName);
                        }
                    }
                }
            }
        }

        private void addParametersWithInferredTypes(
                MethodSpec.Builder methodBuilder,
                List<String> beanReferences,
                Map<String, ClassName> beanRefToTypeMap) {
            // Add parameters with inferred types
            for (String beanRef : beanReferences) {
                // beanRef is original bean name, sanitize it for map lookup
                String sanitizedBeanRef = sanitizeBeanName(beanRef);

                // Use original bean name for @Qualifier
                AnnotationSpec qualifier = AnnotationSpec.builder(
                                org.springframework.beans.factory.annotation.Qualifier.class)
                        .addMember("value", "$S", beanRef)
                        .build();

                ClassName paramType = resolveParameterType(beanRef, transpilationContext);
                paramType = beanRefToTypeMap.getOrDefault(sanitizedBeanRef, paramType);

                ParameterSpec.Builder paramBuilder =
                        ParameterSpec.builder(paramType, sanitizedBeanRef).addAnnotation(qualifier);
                methodBuilder.addParameter(paramBuilder.build());
            }
        }

        /** Add method parameters for property bean references. */
        private void addPropertyBeanReferenceParameters(MethodSpec.Builder methodBuilder, List<String> beanReferences) {

            for (String beanRef : beanReferences) {
                ClassName paramType = resolveParameterType(beanRef, transpilationContext);
                AnnotationSpec value = AnnotationSpec.builder(
                                org.springframework.beans.factory.annotation.Qualifier.class)
                        .addMember("value", "$S", beanRef)
                        .build();
                ParameterSpec.Builder paramBuilder = ParameterSpec.builder(paramType, sanitizeBeanName(beanRef))
                        .addAnnotation(value);
                methodBuilder.addParameter(paramBuilder.build());
            }
        }
    }

    /** Helper class to represent a constructor parameter with proper typing. */
    private static class ConstructorParameter {
        final int index;
        final String name;
        final ClassName paramType;
        final ParameterType type;
        final String spelExpression; // For SpEL expressions, stores the original expression
        final String originalBeanName; // For bean references, stores the original unsanitized bean name for @Qualifier

        ConstructorParameter(int index, String name, ClassName paramType, ParameterType type) {
            this(index, name, paramType, type, null, null);
        }

        ConstructorParameter(int index, String name, ClassName paramType, ParameterType type, String spelExpression) {
            this(index, name, paramType, type, spelExpression, null);
        }

        ConstructorParameter(
                int index,
                String name,
                ClassName paramType,
                ParameterType type,
                String spelExpression,
                String originalBeanName) {
            this.index = index;
            this.name = name;
            this.paramType = paramType;
            this.type = type;
            this.spelExpression = spelExpression;
            this.originalBeanName = originalBeanName;
        }
    }

    /** Enum to represent different types of constructor parameters. */
    private enum ParameterType {
        PRIMITIVE_LITERAL,
        STRING_LITERAL,
        CLASS_LITERAL,
        BEAN_REFERENCE,
        CONSTRUCTOR_CALL,
        SPEL_EXPRESSION,
        IMPLICIT_AUTOWIRED,
        MANAGED_LIST,
        MANAGED_ARRAY,
        MANAGED_MAP,
        UNSUPPORTED
    }
}
