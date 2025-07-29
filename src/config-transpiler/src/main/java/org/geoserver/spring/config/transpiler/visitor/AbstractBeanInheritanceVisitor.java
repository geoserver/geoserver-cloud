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
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.geoserver.spring.config.transpiler.context.BeanGenerationContext;
import org.geoserver.spring.config.transpiler.context.TranspilationContext;
import org.geoserver.spring.config.transpiler.util.TypeNameResolver;
import org.springframework.beans.PropertyValue;
import org.springframework.beans.PropertyValues;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.config.TypedStringValue;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;

/**
 * Visitor for generating {@code @Bean} methods from Spring XML bean definitions that use abstract bean inheritance.
 *
 * <h3>Problem Solved</h3>
 * <p>Spring XML supports a powerful inheritance mechanism where child beans can inherit configuration
 * from abstract parent beans using the {@code parent} attribute. This pattern is commonly used in
 * complex applications like GeoServer to reduce XML duplication and establish configuration templates.
 *
 * <p><strong>Example Spring XML pattern:</strong>
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
 * <h3>Spring's Inheritance Behavior</h3>
 * <p>In the above example, Spring merges the child bean's configuration with its parent:
 * <ul>
 *   <li><strong>Constructor arguments:</strong> Child args are merged with parent args based on index positions</li>
 *   <li><strong>Property values:</strong> Child properties override parent properties with the same name</li>
 *   <li><strong>Bean metadata:</strong> Child inherits scope, lazy-init, depends-on, etc. from parent</li>
 *   <li><strong>Abstract beans:</strong> Parent beans marked {@code abstract="true"} are not instantiated</li>
 * </ul>
 *
 * <p>The effective constructor call becomes:
 * {@code new WfsXmlReader("GetCapabilities", xmlConfiguration-1.0, geoServer)}
 *
 * <h3>Generated Java Output</h3>
 * <p>This visitor generates equivalent Java {@code @Bean} methods that replicate Spring's inheritance behavior:
 * <pre>{@code
 * @Bean
 * org.geoserver.wfs.xml.v1_0_0.WfsXmlReader wfsGetCapabilitiesXmlReader(
 *     @Qualifier("xmlConfiguration-1.0") org.geotools.xsd.Configuration xmlConfiguration_1_0,
 *     @Qualifier("geoServer") org.geoserver.config.GeoServer geoServer) {
 *     return new org.geoserver.wfs.xml.v1_0_0.WfsXmlReader("GetCapabilities", xmlConfiguration_1_0, geoServer);
 * }
 * }</pre>
 *
 * <h3>Implementation Details</h3>
 * <p>This visitor handles the complex inheritance merging process:
 * <ol>
 *   <li><strong>Parent Resolution:</strong> Locates and validates the abstract parent bean definition</li>
 *   <li><strong>Constructor Argument Merging:</strong> Combines parent and child constructor args respecting explicit indexes</li>
 *   <li><strong>Property Merging:</strong> Merges property values with child properties overriding parent properties</li>
 *   <li><strong>Dependency Collection:</strong> Gathers all bean references from merged configuration for method parameters</li>
 *   <li><strong>Code Generation:</strong> Produces {@code @Bean} methods with properly merged constructor calls and property setters</li>
 * </ol>
 *
 * <h3>Visitor Priority</h3>
 * <p>This visitor operates at <strong>priority 50</strong> (higher than {@link ConstructorBasedBeanVisitor} at 100)
 * to ensure beans with inheritance are handled before falling back to standard constructor injection patterns.
 * Only processes beans that have a {@code parent} attribute - abstract parent beans themselves are skipped
 * since they are not meant to be instantiated.
 *
 * <h3>Error Handling</h3>
 * <p>The visitor performs validation to ensure:
 * <ul>
 *   <li>Parent bean exists and is properly defined</li>
 *   <li>Parent bean is marked as abstract (recommended practice)</li>
 *   <li>Constructor argument indexes don't conflict during merging</li>
 *   <li>All referenced beans are available for dependency injection</li>
 * </ul>
 *
 * @since 2.28.0
 * @see ConstructorBasedBeanVisitor
 * @see AbstractBeanDefinitionVisitor
 */
public class AbstractBeanInheritanceVisitor extends AbstractBeanDefinitionVisitor {

    @Override
    public boolean canHandle(BeanDefinition beanDefinition, TranspilationContext context) {
        // Handle beans that inherit from abstract parent beans
        return beanDefinition.getParentName() != null && !beanDefinition.isAbstract();
    }

    @Override
    public int getPriority() {
        return 50; // Higher priority than ConstructorBasedBeanVisitor (100)
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

        // Collect all bean references for method parameters
        List<String> beanReferences = collectAllBeanReferences(mergedConstructorArgs, mergedPropertyValues);

        // Add method parameters for bean references with constructor-based type inference
        addMethodParametersWithConstructorTypeInference(
                methodBuilder, mergedConstructorArgs, beanReferences, beanClassName, transpilationContext);

        // Add javadoc using enhanced bean info
        String javadocContent = beanContext.generateJavadoc();
        methodBuilder.addJavadoc(javadocContent);

        // Generate method body with inheritance-aware logic
        generateInheritanceMethodBody(
                methodBuilder,
                beanContext,
                mergedConstructorArgs,
                mergedPropertyValues,
                beanReferences,
                returnType,
                transpilationContext);

        return methodBuilder.build();
    }

    /**
     * Resolves the parent bean definition from the transpilation context.
     */
    private BeanDefinition resolveParentBean(BeanDefinition childBean, TranspilationContext context) {
        String parentName = childBean.getParentName();
        if (parentName == null) {
            return null;
        }
        return context.getBeanDefinition(parentName);
    }

    /**
     * Merges constructor arguments from parent and child beans following Spring's inheritance rules.
     * Child arguments override parent arguments at the same index.
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
     * Merges property values from parent and child beans.
     * Child properties override parent properties with the same name.
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
     * Adds inherited bean lifecycle annotations from parent and child beans.
     * Child annotations override parent annotations.
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
        List<String> allDependencies = new ArrayList<>();
        if (parentBean.getDependsOn() != null) {
            for (String dep : parentBean.getDependsOn()) {
                allDependencies.add(dep);
            }
        }
        if (childBean.getDependsOn() != null) {
            for (String dep : childBean.getDependsOn()) {
                if (!allDependencies.contains(dep)) {
                    allDependencies.add(dep);
                }
            }
        }

        if (!allDependencies.isEmpty()) {
            AnnotationSpec.Builder dependsOnBuilder = AnnotationSpec.builder(DependsOn.class);
            if (allDependencies.size() == 1) {
                dependsOnBuilder.addMember("value", "$S", allDependencies.get(0));
            } else {
                CodeBlock.Builder arrayBuilder = CodeBlock.builder().add("{");
                for (int i = 0; i < allDependencies.size(); i++) {
                    if (i > 0) {
                        arrayBuilder.add(", ");
                    }
                    arrayBuilder.add("$S", allDependencies.get(i));
                }
                arrayBuilder.add("}");
                dependsOnBuilder.addMember("value", arrayBuilder.build());
            }
            methodBuilder.addAnnotation(dependsOnBuilder.build());
        }
    }

    /**
     * Collects all bean references from merged constructor arguments and property values.
     */
    private List<String> collectAllBeanReferences(
            ConstructorArgumentValues constructorArgs, PropertyValues propertyValues) {
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

        // Collect from property values (reuse existing logic from base class)
        if (propertyValues != null) {
            for (PropertyValue pv : propertyValues.getPropertyValues()) {
                Object value = pv.getValue();
                if (value instanceof RuntimeBeanReference beanRef) {
                    if (!beanReferences.contains(beanRef.getBeanName())) {
                        beanReferences.add(beanRef.getBeanName());
                    }
                }
                // TODO: Add support for managed collections in properties if needed
            }
        }

        return beanReferences;
    }

    /**
     * Extracts a bean reference name from a constructor argument value.
     */
    private String extractBeanReference(Object value) {
        if (value instanceof RuntimeBeanReference reference) {
            return reference.getBeanName();
        }
        return null;
    }

    /**
     * Generates the method body for inheritance-aware bean creation.
     */
    private void generateInheritanceMethodBody(
            MethodSpec.Builder methodBuilder,
            BeanGenerationContext beanContext,
            ConstructorArgumentValues constructorArgs,
            PropertyValues propertyValues,
            List<String> beanReferences,
            ClassName returnType,
            TranspilationContext context) {

        String beanClassName = beanContext.getBeanClassName();

        // If child bean doesn't have explicit class, try Spring's ResolvableType first
        if (beanClassName == null) {
            try {
                org.springframework.core.ResolvableType resolvableType =
                        beanContext.getBeanDefinition().getResolvableType();
                if (resolvableType != null && resolvableType != org.springframework.core.ResolvableType.NONE) {
                    Class<?> resolvedClass = resolvableType.resolve();
                    if (resolvedClass != null) {
                        beanClassName = resolvedClass.getName();
                    }
                }
            } catch (Exception e) {
                // Continue to manual parent resolution
            }
        }

        // If still null, try manual parent resolution as fallback
        if (beanClassName == null) {
            BeanDefinition parentBean =
                    resolveParentBean(beanContext.getBeanDefinition(), beanContext.getTranspilationContext());
            if (parentBean != null) {
                beanClassName = resolveClassNameFromParent(parentBean, beanContext.getTranspilationContext());
            }
        }

        if (beanClassName == null) {
            methodBuilder.addStatement(
                    "throw new $T($S)", UnsupportedOperationException.class, "No bean class name specified");
            return;
        }

        boolean hasProperties = propertyValues != null && propertyValues.getPropertyValues().length > 0;
        boolean hasConstructorArgs = constructorArgs != null
                && (!constructorArgs.getIndexedArgumentValues().isEmpty()
                        || !constructorArgs.getGenericArgumentValues().isEmpty());

        if (hasConstructorArgs) {
            // Generate constructor call with merged arguments
            generateInheritanceConstructorCall(methodBuilder, constructorArgs, beanReferences, returnType, context);
        } else {
            // Simple no-arg constructor
            methodBuilder.addStatement("$T bean = new $T()", returnType, returnType);
        }

        if (hasProperties) {
            // Set merged properties
            generateInheritancePropertySetters(methodBuilder, propertyValues, beanReferences);
            methodBuilder.addStatement("return bean");
        } else if (hasConstructorArgs) {
            // Constructor call already returns the instance
        } else {
            methodBuilder.addStatement("return bean");
        }
    }

    /**
     * Generates constructor call with merged arguments from inheritance.
     */
    private void generateInheritanceConstructorCall(
            MethodSpec.Builder methodBuilder,
            ConstructorArgumentValues constructorArgs,
            List<String> beanReferences,
            ClassName returnType,
            TranspilationContext context) {

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
            orderedArgs[index] = generateConstructorArgExpression(value, beanReferences, 0);
        }

        // Add any generic arguments at the end
        List<String> allArgs = new ArrayList<>();
        for (String arg : orderedArgs) {
            if (arg != null) {
                allArgs.add(arg);
            }
        }

        for (ConstructorArgumentValues.ValueHolder holder : constructorArgs.getGenericArgumentValues()) {
            String argExpr = generateConstructorArgExpression(holder.getValue(), beanReferences, 0);
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

    /**
     * Generates expression for a constructor argument value.
     */
    private String generateConstructorArgExpression(Object value, List<String> beanReferences, int beanRefIndex) {
        if (value instanceof RuntimeBeanReference beanRef) {
            return sanitizeBeanName(beanRef.getBeanName());
        } else if (value instanceof TypedStringValue stringValue) {
            return "\"" + stringValue.getValue() + "\"";
        } else {
            // Fallback to string representation
            return "\"" + value.toString() + "\"";
        }
    }

    /**
     * Generates property setter calls for merged properties.
     */
    private void generateInheritancePropertySetters(
            MethodSpec.Builder methodBuilder, PropertyValues propertyValues, List<String> beanReferences) {

        for (PropertyValue pv : propertyValues.getPropertyValues()) {
            String propertyName = pv.getName();
            Object value = pv.getValue();

            // Generate setter method name
            String setterName = "set" + Character.toUpperCase(propertyName.charAt(0)) + propertyName.substring(1);

            if (value instanceof RuntimeBeanReference beanRef) {
                methodBuilder.addStatement("bean.$L($N)", setterName, sanitizeBeanName(beanRef.getBeanName()));
            } else if (value instanceof TypedStringValue stringValue) {
                methodBuilder.addStatement("bean.$L($S)", setterName, stringValue.getValue());
            } else {
                // TODO: Handle other property value types as needed
                methodBuilder.addComment("TODO: Handle property '" + propertyName + "' of type "
                        + value.getClass().getSimpleName());
            }
        }
    }

    /**
     * Add method parameters with constructor-based type inference.
     * Uses reflection to determine the actual constructor parameter types.
     */
    private void addMethodParametersWithConstructorTypeInference(
            MethodSpec.Builder methodBuilder,
            ConstructorArgumentValues constructorArgs,
            List<String> beanReferences,
            String beanClassName,
            TranspilationContext context) {

        // Map bean references to their constructor parameter positions and infer types
        Map<String, ClassName> beanRefToTypeMap = new java.util.HashMap<>();

        // Process indexed arguments to map bean references to constructor parameter types
        Map<Integer, ConstructorArgumentValues.ValueHolder> indexedArgs = constructorArgs.getIndexedArgumentValues();
        for (Map.Entry<Integer, ConstructorArgumentValues.ValueHolder> entry : indexedArgs.entrySet()) {
            int index = entry.getKey();
            Object value = entry.getValue().getValue();

            if (value instanceof RuntimeBeanReference beanRef) {
                String beanName = beanRef.getBeanName();

                // Use constructor-based type inference for this parameter position
                ClassName paramType = getConstructorParameterType(index, beanClassName, constructorArgs);
                if (paramType != null) {
                    beanRefToTypeMap.put(beanName, paramType);
                }
            } else if (value instanceof org.springframework.beans.factory.support.ManagedList<?> managedList) {
                // Handle ManagedList - extract generic type from constructor parameter
                ClassName elementType = getGenericCollectionElementType(index, beanClassName, constructorArgs);
                if (elementType != null) {
                    // Apply the generic element type to all bean references in the list
                    for (Object listItem : managedList) {
                        if (listItem instanceof RuntimeBeanReference beanRef) {
                            beanRefToTypeMap.put(beanRef.getBeanName(), elementType);
                        }
                    }
                }
            }
        }

        // Process generic (non-indexed) arguments - these get mapped to sequential parameter positions
        List<ConstructorArgumentValues.ValueHolder> genericArgs = constructorArgs.getGenericArgumentValues();
        int genericStartIndex = indexedArgs.size(); // Generic args start after indexed args

        for (int i = 0; i < genericArgs.size(); i++) {
            ConstructorArgumentValues.ValueHolder holder = genericArgs.get(i);
            Object value = holder.getValue();

            if (value instanceof RuntimeBeanReference beanRef) {
                String beanName = beanRef.getBeanName();

                // Map this generic arg to its sequential parameter position
                int paramIndex = genericStartIndex + i;
                ClassName paramType = getConstructorParameterType(paramIndex, beanClassName, constructorArgs);
                if (paramType != null) {
                    beanRefToTypeMap.put(beanName, paramType);
                }
            }
        }

        // Add method parameters for each bean reference with inferred types
        for (String beanRef : beanReferences) {
            ClassName paramType = beanRefToTypeMap.getOrDefault(
                    beanRef,
                    resolveParameterTypeWithInference(
                            beanRef, beanClassName, null, null, constructorArgs, context)); // enhanced fallback

            ParameterSpec.Builder paramBuilder = ParameterSpec.builder(paramType, sanitizeBeanName(beanRef))
                    .addAnnotation(AnnotationSpec.builder(Qualifier.class)
                            .addMember("value", "$S", beanRef)
                            .build());
            methodBuilder.addParameter(paramBuilder.build());
        }
    }

    /**
     * Get the parameter type for a specific constructor parameter index using reflection.
     * Based on ConstructorBasedBeanVisitor.getConstructorParameterType().
     */
    private ClassName getConstructorParameterType(
            int paramIndex, String beanClassName, ConstructorArgumentValues constructorArgs) {
        if (beanClassName == null) {
            return null;
        }

        try {
            Class<?> beanClass = Class.forName(beanClassName);

            // For explicit constructor arguments, find constructor by argument count
            int argCount = constructorArgs.getArgumentCount();
            java.lang.reflect.Constructor<?>[] constructors = beanClass.getDeclaredConstructors();

            // First try to find a public constructor with matching parameter count
            for (java.lang.reflect.Constructor<?> constructor : constructors) {
                if (constructor.getParameterCount() == argCount
                        && paramIndex < constructor.getParameterCount()
                        && Modifier.isPublic(constructor.getModifiers())) {
                    Class<?> paramType = constructor.getParameterTypes()[paramIndex];
                    return classToClassName(paramType);
                }
            }

            // If no public constructor found, fall back to any constructor with matching count
            for (java.lang.reflect.Constructor<?> constructor : constructors) {
                if (constructor.getParameterCount() == argCount && paramIndex < constructor.getParameterCount()) {
                    Class<?> paramType = constructor.getParameterTypes()[paramIndex];
                    return classToClassName(paramType);
                }
            }
        } catch (Exception e) {
            // Fall back to null if we can't determine the type
        }

        return null;
    }

    /**
     * Convert a Class to ClassName for JavaPoet.
     */
    private ClassName classToClassName(Class<?> clazz) {
        if (clazz.isPrimitive()) {
            // Convert primitive to boxed type for consistency with method parameters
            return getBoxedType(clazz);
        }

        return ClassName.get(clazz);
    }

    /**
     * Resolve class name from parent bean, handling chained inheritance.
     * This method recursively walks up the parent chain to find the first bean with a class name.
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

    /**
     * Get return type using Spring's ResolvableType with TypeNameResolver fallback.
     */
    private ClassName getReturnType(BeanGenerationContext beanContext, TranspilationContext context) {
        BeanDefinition beanDefinition = beanContext.getBeanDefinition();

        // First try Spring's ResolvableType which handles inheritance properly
        try {
            org.springframework.core.ResolvableType resolvableType = beanDefinition.getResolvableType();
            if (resolvableType != null && resolvableType != org.springframework.core.ResolvableType.NONE) {
                Class<?> resolvedClass = resolvableType.resolve();
                if (resolvedClass != null) {
                    return ClassName.get(resolvedClass);
                }
            }
        } catch (Exception e) {
            // Continue to fallback approaches
        }

        // Fallback to TypeNameResolver
        TypeNameResolver.TypeResolutionResult result = TypeNameResolver.resolveBeanReturnType(beanDefinition, context);

        if (!result.isResolved()) {
            // Try to resolve from parent bean if TypeNameResolver failed
            String resolvedClassName = beanContext.getBeanClassName();
            if (resolvedClassName == null) {
                BeanDefinition parentBean = resolveParentBean(beanDefinition, context);
                if (parentBean != null) {
                    resolvedClassName = resolveClassNameFromParent(parentBean, context);
                }
            }

            if (resolvedClassName != null) {
                try {
                    if (resolvedClassName.contains(".")) {
                        int lastDot = resolvedClassName.lastIndexOf('.');
                        String packageName = resolvedClassName.substring(0, lastDot);
                        String simpleName = resolvedClassName.substring(lastDot + 1);
                        return ClassName.get(packageName, simpleName);
                    } else {
                        return ClassName.get("java.lang", resolvedClassName);
                    }
                } catch (Exception e) {
                    // Fall through to Object fallback
                }
            }

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
}
