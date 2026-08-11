/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.spring.config.transpiler.generator;

import static org.geoserver.spring.config.transpiler.xml.EnhancedBeanDefinition.sanitizeBeanName;

import com.palantir.javapoet.AnnotationSpec;
import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.ParameterSpec;
import java.lang.reflect.Constructor;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.geoserver.spring.config.transpiler.context.BeanGenerationContext;
import org.geoserver.spring.config.transpiler.context.TranspilationContext;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.PropertyValue;
import org.springframework.beans.PropertyValues;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.config.TypedStringValue;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;
import org.springframework.core.ResolvableType;

/**
 * Generator for generating {@code @Bean} methods from Spring XML bean definitions that use abstract bean inheritance.
 *
 * <h2>Problem Solved</h2>
 *
 * <p>Spring XML supports a powerful inheritance mechanism where child beans can inherit configuration from abstract
 * parent beans using the {@code parent} attribute. This pattern is commonly used in complex applications like GeoServer
 * to reduce XML duplication and establish configuration templates.
 *
 * <p><strong>Example Spring XML pattern:</strong>
 *
 * <pre>{@code
 * <!-- Abstract parent bean defines common configuration -->
 * <bean id="xmlReader-1.0.0" class="org.geoserver.wfs.xml.v1_0_0.WfsXmlReader" abstract="true">
 *     <constructor-arg index="1" ref="xmlConfiguration-1.0"/>
 *     <constructor-arg index="2" ref="geoServer"/>
 * </bean>
 *
 * <!-- Child beans inherit parent configuration and add their own -->
 * <bean id="wfsGetCapabilitiesXmlReader"
 *       class="org.geoserver.wfs.xml.v1_0_0.WfsXmlReader"
 *       parent="xmlReader-1.0.0">
 *     <constructor-arg value="GetCapabilities"/>
 * </bean>
 * }</pre>
 *
 * <h2>Spring's Inheritance Behavior</h2>
 *
 * <p>In the above example, Spring merges the child bean's configuration with its parent:
 *
 * <ul>
 *   <li><strong>Constructor arguments:</strong> Child args are merged with parent args based on index positions
 *   <li><strong>Property values:</strong> Child properties override parent properties with the same name
 *   <li><strong>Bean metadata:</strong> Child inherits scope, lazy-init, depends-on, etc. from parent
 *   <li><strong>Abstract beans:</strong> Parent beans marked {@code abstract="true"} are not instantiated
 * </ul>
 *
 * <p>The effective constructor call becomes: {@code new WfsXmlReader("GetCapabilities", xmlConfiguration-1.0,
 * geoServer)}
 *
 * <h2>Generated Java Output</h2>
 *
 * <p>This generator generates equivalent Java {@code @Bean} methods that replicate Spring's inheritance behavior:
 *
 * <pre>{@code
 * @Bean
 * org.geoserver.wfs.xml.v1_0_0.WfsXmlReader wfsGetCapabilitiesXmlReader(
 *     @Qualifier("xmlConfiguration-1.0") org.geotools.xsd.Configuration xmlConfiguration_1_0,
 *     @Qualifier("geoServer") org.geoserver.config.GeoServer geoServer) {
 *     return new org.geoserver.wfs.xml.v1_0_0.WfsXmlReader("GetCapabilities", xmlConfiguration_1_0, geoServer);
 * }
 * }</pre>
 *
 * <h2>Implementation Details</h2>
 *
 * <p>This generator handles the complex inheritance merging process:
 *
 * <ol>
 *   <li><strong>Parent Resolution:</strong> Locates and validates the abstract parent bean definition
 *   <li><strong>Constructor Argument Merging:</strong> Combines parent and child constructor args respecting explicit
 *       indexes
 *   <li><strong>Property Merging:</strong> Merges property values with child properties overriding parent properties
 *   <li><strong>Dependency Collection:</strong> Gathers all bean references from merged configuration for method
 *       parameters
 *   <li><strong>Code Generation:</strong> Produces {@code @Bean} methods with properly merged constructor calls and
 *       property setters
 * </ol>
 *
 * <h2>Generator Priority</h2>
 *
 * <p>This generator operates at <strong>priority 50</strong> (higher than {@link ConstructorBasedBeanMethodGenerator}
 * at 100) to ensure beans with inheritance are handled before falling back to standard constructor injection patterns.
 * Only processes beans that have a {@code parent} attribute - abstract parent beans themselves are skipped since they
 * are not meant to be instantiated.
 *
 * <h2>Error Handling</h2>
 *
 * <p>The generator performs validation to ensure:
 *
 * <ul>
 *   <li>Parent bean exists and is properly defined
 *   <li>Parent bean is marked as abstract (recommended practice)
 *   <li>Constructor argument indexes don't conflict during merging
 *   <li>All referenced beans are available for dependency injection
 * </ul>
 *
 * @since 3.0.0
 * @see ConstructorBasedBeanMethodGenerator
 * @see AbstractBeanMethodGenerator
 */
public class AbstractBeanInheritanceMethodGenerator extends AbstractBeanMethodGenerator {

    @Override
    public boolean canHandle(BeanDefinition beanDefinition) {
        // Handle beans that inherit from abstract parent beans
        return beanDefinition.getParentName() != null && !beanDefinition.isAbstract();
    }

    @Override
    public int getPriority() {
        return 50; // Higher priority than ConstructorBasedBeanMethodGenerator (100)
    }

    @Override
    public MethodSpec generateBeanMethod(BeanGenerationContext beanContext) {
        TranspilationContext transpilationContext = beanContext.getTranspilationContext();
        BeanDefinition childBean = beanContext.getBeanDefinition();
        String beanName = beanContext.getBeanName();
        String beanClassName = beanContext.getBeanClassName();

        // Resolve the parent bean definition
        BeanDefinition parentBean = resolveParentBean(childBean, transpilationContext);
        if (parentBean == null) {
            throw new IllegalStateException(
                    "Parent bean '" + childBean.getParentName() + "' not found for bean '" + beanName + "'");
        }

        // Merge constructor arguments from parent and child
        ConstructorArgumentValues mergedConstructorArgs = mergeConstructorArguments(parentBean, childBean);

        // Merge property values from parent and child (child overrides parent)
        PropertyValues mergedPropertyValues = mergePropertyValues(parentBean, childBean);

        // Sanitize bean name for method name
        String methodName = sanitizeBeanName(beanName);

        // For auto-generated bean names, add unique suffix to avoid collisions across configurations
        if (beanContext.isAutoGenerated()) {
            String uniqueSuffix = transpilationContext.getUniqueMethodSuffix();
            methodName = methodName + "_" + uniqueSuffix;
        }

        // Get return type using TypeNameResolver
        ClassName returnType = getReturnType(beanContext, transpilationContext);

        // Create method builder
        MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder(methodName)
                .addModifiers() // Package-private
                .returns(returnType);

        // Add {@code @Bean} annotation with proper name handling
        methodBuilder.addAnnotation(createBeanAnnotation(beanContext));

        // Add inherited bean lifecycle annotations
        addInheritedAnnotations(methodBuilder, parentBean, childBean);

        // Resolve the target constructor once for the merged args
        Constructor<?> targetConstructor =
                Reflections.resolveTargetConstructor(beanContext.getResolvedBeanClass(), mergedConstructorArgs);

        // Collect all bean references for method parameters
        List<String> beanReferences = collectAllBeanReferences(mergedConstructorArgs);

        // Add method parameters for bean references with constructor-based type inference
        addMethodParametersWithConstructorTypeInference(
                methodBuilder,
                mergedConstructorArgs,
                beanReferences,
                targetConstructor,
                beanClassName,
                transpilationContext);

        // Add javadoc using enhanced bean info
        String javadocContent = beanContext.generateJavadoc();
        methodBuilder.addJavadoc(javadocContent);

        // Generate method body with inheritance-aware logic
        generateInheritanceMethodBody(
                methodBuilder, targetConstructor, mergedConstructorArgs, mergedPropertyValues, returnType);

        return methodBuilder.build();
    }

    /** Resolves the parent bean definition from the transpilation context. */
    private BeanDefinition resolveParentBean(BeanDefinition childBean, TranspilationContext context) {
        String parentName = childBean.getParentName();
        if (parentName == null) {
            return null;
        }
        return context.getBeanDefinition(parentName);
    }

    /**
     * Merges constructor arguments from parent and child beans following Spring's inheritance rules. Child arguments
     * override parent arguments at the same index.
     */
    private ConstructorArgumentValues mergeConstructorArguments(BeanDefinition parentBean, BeanDefinition childBean) {
        ConstructorArgumentValues merged = new ConstructorArgumentValues();

        // Start with parent constructor arguments
        if (parentBean.hasConstructorArgumentValues()) {
            ConstructorArgumentValues parentArgs = parentBean.getConstructorArgumentValues();

            // Copy indexed arguments from parent
            for (Map.Entry<Integer, ConstructorArgumentValues.ValueHolder> entry :
                    parentArgs.getIndexedArgumentValues().entrySet()) {
                merged.addIndexedArgumentValue(entry.getKey(), entry.getValue().getValue());
            }

            // Copy generic arguments from parent
            for (ConstructorArgumentValues.ValueHolder holder : parentArgs.getGenericArgumentValues()) {
                merged.addGenericArgumentValue(holder.getValue());
            }
        }

        // Overlay child constructor arguments (child overrides parent at same index)
        if (childBean.hasConstructorArgumentValues()) {
            ConstructorArgumentValues childArgs = childBean.getConstructorArgumentValues();

            // Child indexed arguments override parent at same index
            for (Map.Entry<Integer, ConstructorArgumentValues.ValueHolder> entry :
                    childArgs.getIndexedArgumentValues().entrySet()) {
                merged.addIndexedArgumentValue(entry.getKey(), entry.getValue().getValue());
            }

            // Handle child generic arguments by finding the next available index
            // Generic args in child should fill gaps or extend beyond parent indexed args
            for (ConstructorArgumentValues.ValueHolder holder : childArgs.getGenericArgumentValues()) {
                // Find the lowest available index starting from 0
                int nextIndex = 0;
                while (merged.hasIndexedArgumentValue(nextIndex)) {
                    nextIndex++;
                }
                merged.addIndexedArgumentValue(nextIndex, holder.getValue());
            }
        }

        return merged;
    }

    /**
     * Merges property values from parent and child beans. Child properties override parent properties with the same
     * name.
     */
    private PropertyValues mergePropertyValues(BeanDefinition parentBean, BeanDefinition childBean) {
        Map<String, PropertyValue> mergedProps = new LinkedHashMap<>();

        // Start with parent properties
        if (parentBean.hasPropertyValues()) {
            for (PropertyValue pv : parentBean.getPropertyValues().getPropertyValues()) {
                mergedProps.put(pv.getName(), pv);
            }
        }

        // Overlay child properties (child overrides parent)
        if (childBean.hasPropertyValues()) {
            for (PropertyValue pv : childBean.getPropertyValues().getPropertyValues()) {
                mergedProps.put(pv.getName(), pv);
            }
        }

        // Convert back to PropertyValues
        org.springframework.beans.MutablePropertyValues result = new org.springframework.beans.MutablePropertyValues();
        for (PropertyValue pv : mergedProps.values()) {
            result.addPropertyValue(pv);
        }

        return result;
    }

    /**
     * Adds inherited bean lifecycle annotations from parent and child beans. Child annotations override parent
     * annotations.
     */
    private void addInheritedAnnotations(
            MethodSpec.Builder methodBuilder, BeanDefinition parentBean, BeanDefinition childBean) {
        // Determine effective lazy-init (child overrides parent)
        boolean isLazy = childBean.isLazyInit() || parentBean.isLazyInit();
        if (isLazy) {
            methodBuilder.addAnnotation(Lazy.class);
        }

        // Determine effective scope (child overrides parent)
        String scope = childBean.getScope();
        if (scope == null || scope.isEmpty()) {
            scope = parentBean.getScope();
        }
        if (scope != null && !scope.isEmpty() && !"singleton".equals(scope)) {
            AnnotationSpec scopeAnnotation = AnnotationSpec.builder(Scope.class)
                    .addMember("value", "$S", scope)
                    .build();
            methodBuilder.addAnnotation(scopeAnnotation);
        }

        // Merge depends-on attributes (child dependencies added to parent dependencies)
        LinkedHashSet<String> allDependencies = new LinkedHashSet<>();
        if (parentBean.getDependsOn() != null) {
            allDependencies.addAll(Arrays.asList(parentBean.getDependsOn()));
        }
        if (childBean.getDependsOn() != null) {
            allDependencies.addAll(Arrays.asList(childBean.getDependsOn()));
        }

        if (!allDependencies.isEmpty()) {
            addDependsOnAnnotation(methodBuilder, allDependencies.toArray(String[]::new));
        }
    }

    /** Collects all bean references from merged constructor arguments and property values. */
    private List<String> collectAllBeanReferences(ConstructorArgumentValues constructorArgs) {
        List<String> beanReferences = new ArrayList<>();

        // Collect from constructor arguments
        if (constructorArgs != null) {
            // From indexed arguments
            for (ConstructorArgumentValues.ValueHolder holder :
                    constructorArgs.getIndexedArgumentValues().values()) {
                String beanRef = extractBeanReference(holder.getValue());
                if (beanRef != null && !beanReferences.contains(beanRef)) {
                    beanReferences.add(beanRef);
                }
            }

            // From generic arguments
            for (ConstructorArgumentValues.ValueHolder holder : constructorArgs.getGenericArgumentValues()) {
                String beanRef = extractBeanReference(holder.getValue());
                if (beanRef != null && !beanReferences.contains(beanRef)) {
                    beanReferences.add(beanRef);
                }
            }
        }

        return beanReferences;
    }

    /** Extracts a bean reference name from a constructor argument value. */
    private String extractBeanReference(Object value) {
        if (value instanceof RuntimeBeanReference reference) {
            return reference.getBeanName();
        }
        return null;
    }

    /** Generates the method body for inheritance-aware bean creation. */
    private void generateInheritanceMethodBody(
            MethodSpec.Builder methodBuilder,
            @Nullable Constructor<?> targetConstructor,
            ConstructorArgumentValues constructorArgs,
            PropertyValues propertyValues,
            ClassName returnType) {

        boolean hasProperties = propertyValues != null && propertyValues.getPropertyValues().length > 0;
        boolean hasConstructorArgs = constructorArgs != null
                && (!constructorArgs.getIndexedArgumentValues().isEmpty()
                        || !constructorArgs.getGenericArgumentValues().isEmpty());

        // Add constructor exceptions to method signature
        addExceptionsFromConstructor(methodBuilder, targetConstructor, false);

        if (hasConstructorArgs) {
            // Generate constructor call with merged arguments
            generateInheritanceConstructorCall(methodBuilder, constructorArgs, returnType);
        } else {
            // Simple no-arg constructor
            methodBuilder.addStatement("$T bean = new $T()", returnType, returnType);
        }

        if (hasProperties) {
            // Set merged properties
            generateInheritancePropertySetters(methodBuilder, propertyValues);
            methodBuilder.addStatement("return bean");
        } else if (hasConstructorArgs) {
            // Constructor call already returns the instance
        } else {
            methodBuilder.addStatement("return bean");
        }
    }

    /** Generates constructor call with merged arguments from inheritance. */
    private void generateInheritanceConstructorCall(
            MethodSpec.Builder methodBuilder, ConstructorArgumentValues constructorArgs, ClassName returnType) {

        Map<Integer, ConstructorArgumentValues.ValueHolder> indexedArgs = constructorArgs.getIndexedArgumentValues();

        // Find the highest index to determine array size
        int maxIndex =
                indexedArgs.keySet().stream().mapToInt(Integer::intValue).max().orElse(-1);

        // Create array to hold arguments in correct order
        String[] orderedArgs = new String[maxIndex + 1];

        // Fill in indexed arguments at their proper positions
        for (Map.Entry<Integer, ConstructorArgumentValues.ValueHolder> entry : indexedArgs.entrySet()) {
            int index = entry.getKey();
            Object value = entry.getValue().getValue();
            orderedArgs[index] = generateConstructorArgExpression(value);
        }

        // Add any generic arguments at the end
        List<String> allArgs = new ArrayList<>();
        for (String arg : orderedArgs) {
            if (arg != null) {
                allArgs.add(arg);
            }
        }

        for (ConstructorArgumentValues.ValueHolder holder : constructorArgs.getGenericArgumentValues()) {
            String argExpr = generateConstructorArgExpression(holder.getValue());
            allArgs.add(argExpr);
        }

        // Generate the constructor call
        if (allArgs.isEmpty()) {
            methodBuilder.addStatement("return new $T()", returnType);
        } else {
            String argList = String.join(", ", allArgs);
            methodBuilder.addStatement("return new $T($L)", returnType, argList);
        }
    }

    /** Generates expression for a constructor argument value. */
    private String generateConstructorArgExpression(Object value) {
        Objects.requireNonNull(value, "null value is unhandled");

        return switch (value) {
            case RuntimeBeanReference beanRef -> sanitizeBeanName(beanRef.getBeanName());
            case TypedStringValue stringValue -> "\"" + stringValue.getValue() + "\"";
            // fall back to string value?
            default -> "\"" + value.toString() + "\"";
        };
    }

    /** Generates property setter calls for merged properties. */
    private void generateInheritancePropertySetters(MethodSpec.Builder methodBuilder, PropertyValues propertyValues) {

        for (PropertyValue pv : propertyValues.getPropertyValues()) {
            final String propertyName = pv.getName();
            final Object value = Objects.requireNonNull(pv.getValue(), "null PropertyValue.getValue() is unhandled");

            final String setterName = Reflections.buildSetterName(propertyName);

            switch (value) {
                case RuntimeBeanReference beanRef -> {
                    String sanitizeBeanName = sanitizeBeanName(beanRef.getBeanName());
                    methodBuilder.addStatement("bean.$L($N)", setterName, sanitizeBeanName);
                }
                case TypedStringValue stringValue ->
                    methodBuilder.addStatement("bean.$L($S)", setterName, stringValue.getValue());
                default -> {
                    String simpleName = value.getClass().getSimpleName();
                    String msg = "Handle property '" + propertyName + "' of type " + simpleName;
                    throw new UnsupportedOperationException(msg);
                }
            }
        }
    }

    /**
     * Add method parameters with constructor-based type inference. Uses reflection to determine the actual constructor
     * parameter types.
     */
    @SuppressWarnings("java:S3776")
    private void addMethodParametersWithConstructorTypeInference(
            MethodSpec.Builder methodBuilder,
            ConstructorArgumentValues constructorArgs,
            List<String> beanReferences,
            @Nullable Constructor<?> targetConstructor,
            String beanClassName,
            TranspilationContext context) {

        // Map bean references to their constructor parameter positions and infer types
        Map<String, ClassName> beanRefToTypeMap = new HashMap<>();

        if (targetConstructor != null) {
            Class<?>[] paramTypes = targetConstructor.getParameterTypes();

            // Process indexed arguments to map bean references to constructor parameter types
            Map<Integer, ConstructorArgumentValues.ValueHolder> indexedArgs =
                    constructorArgs.getIndexedArgumentValues();
            for (Map.Entry<Integer, ConstructorArgumentValues.ValueHolder> entry : indexedArgs.entrySet()) {
                int index = entry.getKey();
                Object value = entry.getValue().getValue();

                if (index < paramTypes.length && value instanceof RuntimeBeanReference beanRef) {
                    beanRefToTypeMap.put(beanRef.getBeanName(), Reflections.classToClassName(paramTypes[index]));
                } else if (value instanceof ManagedList<?> managedList) {
                    // Handle ManagedList - extract generic type from constructor parameter
                    Type genericType = targetConstructor.getGenericParameterTypes()[index];
                    if (genericType instanceof ParameterizedType pt) {
                        Type[] typeArgs = pt.getActualTypeArguments();
                        if (typeArgs.length > 0 && typeArgs[0] instanceof Class<?> elementClass) {
                            ClassName elementType = ClassName.get(elementClass);
                            for (Object listItem : managedList) {
                                if (listItem instanceof RuntimeBeanReference beanRef) {
                                    beanRefToTypeMap.put(beanRef.getBeanName(), elementType);
                                }
                            }
                        }
                    }
                }
            }

            // Process generic (non-indexed) arguments
            List<ConstructorArgumentValues.ValueHolder> genericArgs = constructorArgs.getGenericArgumentValues();
            int genericStartIndex = indexedArgs.size();
            for (int i = 0; i < genericArgs.size(); i++) {
                Object value = genericArgs.get(i).getValue();
                int paramIndex = genericStartIndex + i;
                if (paramIndex < paramTypes.length && value instanceof RuntimeBeanReference beanRef) {
                    beanRefToTypeMap.put(beanRef.getBeanName(), Reflections.classToClassName(paramTypes[paramIndex]));
                }
            }
        }

        // Add method parameters for each bean reference with inferred types
        for (String beanRef : beanReferences) {
            ClassName paramType =
                    resolveParameterTypeWithInference(beanRef, beanClassName, null, null, constructorArgs, context);
            paramType = beanRefToTypeMap.getOrDefault(beanRef, paramType);

            AnnotationSpec value = AnnotationSpec.builder(Qualifier.class)
                    .addMember("value", "$S", beanRef)
                    .build();
            ParameterSpec.Builder paramBuilder =
                    ParameterSpec.builder(paramType, sanitizeBeanName(beanRef)).addAnnotation(value);
            methodBuilder.addParameter(paramBuilder.build());
        }
    }

    /**
     * Resolve class name from parent bean, handling chained inheritance. This method recursively walks up the parent
     * chain to find the first bean with a class name.
     */
    private String resolveClassNameFromParent(BeanDefinition parentBean, TranspilationContext context) {
        if (parentBean == null) {
            return null;
        }

        // Check if parent has explicit class name
        String parentClassName = parentBean.getBeanClassName();
        if (parentClassName != null) {
            return parentClassName;
        }

        // If parent doesn't have class, check its parent (chained inheritance)
        if (parentBean.getParentName() != null) {
            BeanDefinition grandParentBean = context.getBeanDefinition(parentBean.getParentName());
            return resolveClassNameFromParent(grandParentBean, context);
        }

        return null;
    }

    /** Get return type using Spring's ResolvableType with TypeNameResolver fallback. */
    private ClassName getReturnType(BeanGenerationContext beanContext, TranspilationContext context) {
        BeanDefinition beanDefinition = beanContext.getBeanDefinition();

        // First try Spring's ResolvableType which handles inheritance properly
        ClassName returnType = getReturnType(beanDefinition.getResolvableType());
        if (returnType == null) {
            // Fallback to TypeNameResolver
            TypeNameResolver.TypeResolutionResult result =
                    TypeNameResolver.resolveBeanReturnType(beanDefinition, context);

            returnType = getReturnType(beanContext, context, beanDefinition, result);
        }

        return returnType;
    }

    private ClassName getReturnType(
            BeanGenerationContext beanContext,
            TranspilationContext context,
            BeanDefinition beanDefinition,
            TypeNameResolver.TypeResolutionResult result) {

        if (result.isResolved()) {
            return typeNameToClassName(result.getResolvedTypeName());
        }

        // Try to resolve from parent bean if TypeNameResolver failed
        String resolvedClassName = beanContext.getBeanClassName();
        if (resolvedClassName == null) {
            BeanDefinition parentBean = resolveParentBean(beanDefinition, context);
            if (parentBean != null) {
                resolvedClassName = resolveClassNameFromParent(parentBean, context);
            }
        }
        if (resolvedClassName == null) {
            throw new IllegalStateException("unable to resolve return type");
        }
        return typeNameToClassName(resolvedClassName);
    }

    private ClassName getReturnType(ResolvableType resolvableType) {
        if (resolvableType != ResolvableType.NONE) {
            Class<?> resolvedClass = resolvableType.resolve();
            if (resolvedClass != null) {
                return ClassName.get(resolvedClass);
            }
        }
        return null;
    }
}
