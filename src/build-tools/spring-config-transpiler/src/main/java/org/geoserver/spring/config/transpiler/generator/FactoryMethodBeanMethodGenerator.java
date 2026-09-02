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
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.lang.model.element.Modifier;
import org.geoserver.spring.config.transpiler.context.BeanGenerationContext;
import org.geoserver.spring.config.transpiler.context.TranspilationContext;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.config.TypedStringValue;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;

/**
 * Generator for generating {@code @Bean} methods for factory method-based beans.
 *
 * <p>This generator handles Spring bean definitions that use factory methods for instantiation. It supports both:
 *
 * <ul>
 *   <li>Static factory methods (factory-method on the bean class)
 *   <li>Instance factory methods (factory-bean + factory-method)
 * </ul>
 *
 * <p>Generated {@code @Bean} methods will call the appropriate factory method with the correct arguments and return the
 * result.
 *
 * @since 3.0.0
 */
public class FactoryMethodBeanMethodGenerator extends AbstractBeanMethodGenerator {

    @Override
    public boolean canHandle(BeanDefinition beanDefinition) {
        // Handle beans that specify a factory method
        return beanDefinition.getFactoryMethodName() != null;
    }

    @Override
    public MethodSpec generateBeanMethod(BeanGenerationContext beanContext) {
        FactoryMethodBeanMethodBuilder generator = new FactoryMethodBeanMethodBuilder(beanContext);
        return generator.generate();
    }

    @Override
    public int getPriority() {
        return 60; // Medium-high priority for factory method beans
    }

    /** Inner class to encapsulate method generation logic and reduce parameter passing. */
    private class FactoryMethodBeanMethodBuilder {
        private final BeanGenerationContext beanContext;
        private final TranspilationContext transpilationContext;
        private final BeanDefinition beanDefinition;

        @SuppressWarnings("unused")
        private final String beanName;

        private final String beanClassName;
        private final String factoryMethodName;
        private final String factoryBeanName;

        public FactoryMethodBeanMethodBuilder(BeanGenerationContext beanContext) {
            this.beanContext = beanContext;
            this.transpilationContext = beanContext.getTranspilationContext();
            this.beanDefinition = beanContext.getBeanDefinition();
            this.beanName = beanContext.getBeanName();
            this.beanClassName = beanDefinition.getBeanClassName();
            this.factoryMethodName = beanDefinition.getFactoryMethodName();
            this.factoryBeanName = beanDefinition.getFactoryBeanName();
        }

        public MethodSpec generate() {
            // Use consolidated EnhancedBeanInfo for proper method name
            String methodName = beanContext.getSanitizedMethodName();

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
            String javadocContent = beanContext.generateJavadoc();
            methodBuilder.addJavadoc(javadocContent);

            // Add annotations
            addBeanAnnotations(methodBuilder);

            // Generate factory method call
            generateFactoryMethodCall(methodBuilder);

            return methodBuilder.build();
        }

        private void addBeanAnnotations(MethodSpec.Builder methodBuilder) {
            // Add {@code @Bean} annotation using BeanNameResolver logic
            methodBuilder.addAnnotation(createBeanAnnotation(beanContext));

            // Add @Lazy annotation if bean has lazy-init="true"
            if (beanDefinition.isLazyInit()) {
                methodBuilder.addAnnotation(Lazy.class);
            }

            // Add @Scope annotation if bean has non-default scope
            @Nullable String scope = beanDefinition.getScope();
            if (scope != null && !scope.isEmpty() && !"singleton".equals(scope)) {
                AnnotationSpec scopeAnnotation = AnnotationSpec.builder(Scope.class)
                        .addMember("value", "$S", scope)
                        .build();
                methodBuilder.addAnnotation(scopeAnnotation);
            }

            // Add @DependsOn annotation if bean has depends-on attribute
            addDependsOnAnnotation(methodBuilder, beanDefinition.getDependsOn());
        }

        private void generateFactoryMethodCall(MethodSpec.Builder methodBuilder) {
            if (factoryBeanName != null) {
                throw new UnsupportedOperationException("Instance factory methods not yet implemented");
            } else {
                generateStaticFactoryMethodCall(methodBuilder);
            }
        }

        private ClassName getReturnType() {
            // Use TypeNameResolver for proper type resolution
            org.geoserver.spring.config.transpiler.generator.TypeNameResolver.TypeResolutionResult result =
                    org.geoserver.spring.config.transpiler.generator.TypeNameResolver.resolveBeanReturnType(
                            beanDefinition, transpilationContext);

            if (!result.isResolved()) {
                return ClassName.get(Object.class);
            }

            String typeName = result.getResolvedTypeName();
            try {
                // Try to load the class via reflection to properly handle inner classes
                Class<?> clazz = Reflections.loadForInspection(typeName.replace('.', '$'));
                return ClassName.get(clazz);
            } catch (ClassNotFoundException _) {
                // Not found with $ replacement, try as-is
            }

            try {
                // Try loading directly (works for top-level classes)
                Class<?> clazz = Reflections.loadForInspection(typeName);
                return ClassName.get(clazz);
            } catch (ClassNotFoundException _) {
                // Fall back to string parsing
            }

            // Try to handle inner classes by probing with $ separator
            // e.g. "org.example.Outer.Inner" -> try "org.example.Outer$Inner"
            if (typeName.contains(".")) {
                String candidate = typeName;
                // Walk backwards through dots, trying $ instead
                int dotIndex = candidate.lastIndexOf('.');
                while (dotIndex > 0) {
                    candidate = candidate.substring(0, dotIndex) + "$" + candidate.substring(dotIndex + 1);
                    try {
                        Class<?> clazz = Reflections.loadForInspection(candidate);
                        return ClassName.get(clazz);
                    } catch (ClassNotFoundException _) {
                        // Try the next dot
                        dotIndex = candidate.lastIndexOf('.', dotIndex - 1);
                    }
                }

                // Last resort: simple string-based parsing
                int lastDot = typeName.lastIndexOf('.');
                String packageName = typeName.substring(0, lastDot);
                String simpleName = typeName.substring(lastDot + 1);
                return ClassName.get(packageName, simpleName);
            } else {
                return ClassName.get("java.lang", typeName);
            }
        }

        private void generateStaticFactoryMethodCall(MethodSpec.Builder methodBuilder) {
            // Parse the class name to get ClassName for the factory class
            ClassName factoryClass = parseClassName(beanClassName);

            // Collect factory method arguments
            ConstructorArgumentValues constructorArgs = beanDefinition.getConstructorArgumentValues();

            if (constructorArgs.isEmpty()) {
                // No arguments - simple static factory call
                methodBuilder.addStatement("return $T.$L()", factoryClass, factoryMethodName);
                return;
            }

            // Resolve factory method parameter types via reflection
            Class<?>[] factoryParamTypes = resolveFactoryMethodParameterTypes();

            // Collect arguments in order
            List<FactoryMethodArg> args = collectFactoryMethodArgs(constructorArgs, factoryParamTypes);

            // Add method parameters for bean references
            for (FactoryMethodArg arg : args) {
                if (arg.isBeanReference) {
                    ParameterSpec.Builder paramBuilder = ParameterSpec.builder(arg.paramType, arg.paramName)
                            .addAnnotation(AnnotationSpec.builder(Qualifier.class)
                                    .addMember("value", "$S", arg.qualifierName)
                                    .build());
                    methodBuilder.addParameter(paramBuilder.build());
                }
            }

            // Build the argument list for the factory method call
            List<String> argNames = new ArrayList<>();
            for (FactoryMethodArg arg : args) {
                argNames.add(arg.callExpression);
            }

            String argsJoined = String.join(", ", argNames);
            methodBuilder.addStatement("return $T.$L($L)", factoryClass, factoryMethodName, argsJoined);
        }

        /** Resolve parameter types of the static factory method via reflection. */
        private Class<?>[] resolveFactoryMethodParameterTypes() {
            Class<?> factoryClazz = Reflections.convertToRuntimeClass(beanClassName);
            // Find the factory method by name
            for (Method method : factoryClazz.getDeclaredMethods()) {
                if (method.getName().equals(factoryMethodName)
                        && java.lang.reflect.Modifier.isStatic(method.getModifiers())) {
                    return method.getParameterTypes();
                }
            }
            return new Class<?>[0];
        }

        /** Collect factory method arguments from constructor-arg values. */
        private List<FactoryMethodArg> collectFactoryMethodArgs(
                ConstructorArgumentValues constructorArgs, Class<?>[] factoryParamTypes) {
            List<FactoryMethodArg> args = new ArrayList<>();

            // Process indexed arguments
            Map<Integer, ConstructorArgumentValues.ValueHolder> indexedArgs =
                    constructorArgs.getIndexedArgumentValues();
            for (Map.Entry<Integer, ConstructorArgumentValues.ValueHolder> entry : indexedArgs.entrySet()) {
                int index = entry.getKey();
                args.add(createFactoryMethodArg(index, entry.getValue(), factoryParamTypes));
            }

            // Process generic arguments (come after indexed args)
            List<ConstructorArgumentValues.ValueHolder> genericArgs = constructorArgs.getGenericArgumentValues();
            for (int i = 0; i < genericArgs.size(); i++) {
                int index = indexedArgs.size() + i;
                args.add(createFactoryMethodArg(index, genericArgs.get(i), factoryParamTypes));
            }

            // Sort by index
            args.sort((a, b) -> Integer.compare(a.index, b.index));
            return args;
        }

        private FactoryMethodArg createFactoryMethodArg(
                int index, ConstructorArgumentValues.ValueHolder valueHolder, Class<?>[] factoryParamTypes) {
            Object value = valueHolder.getValue();

            return switch (value) {
                case RuntimeBeanReference beanRef -> {
                    String refBeanName = beanRef.getBeanName();
                    String paramName = sanitizeBeanName(refBeanName);

                    // Resolve parameter type from factory method signature if available
                    ClassName paramType;
                    if (index < factoryParamTypes.length) {
                        paramType = ClassName.get(factoryParamTypes[index]);
                    } else {
                        paramType = resolveParameterType(refBeanName, transpilationContext);
                    }

                    yield new FactoryMethodArg(index, paramName, paramType, true, refBeanName, paramName);
                }
                case TypedStringValue stringValue -> {
                    String rawValue = stringValue.getValue();
                    String paramName = "\"" + rawValue + "\"";
                    ClassName paramType = ClassName.get(String.class);
                    boolean isBeanReference = false;
                    String callExpression = "\"" + rawValue + "\"";
                    yield new FactoryMethodArg(index, paramName, paramType, isBeanReference, null, callExpression);
                }
                default -> new FactoryMethodArg(index, "null", ClassName.get(Object.class), false, null, "null");
            };
        }

        private ClassName parseClassName(String className) {
            if (className == null || className.isEmpty()) {
                return ClassName.get(Object.class);
            }

            try {
                if (className.contains(".")) {
                    int lastDot = className.lastIndexOf('.');
                    String packageName = className.substring(0, lastDot);
                    String simpleName = className.substring(lastDot + 1);
                    return ClassName.get(packageName, simpleName);
                } else {
                    // Simple name - assume java.lang package
                    return ClassName.get("java.lang", className);
                }
            } catch (Exception _) {
                // Fallback to Object if we can't parse the class name
                return ClassName.get(Object.class);
            }
        }

        /** Holder for factory method argument info. */
        private static class FactoryMethodArg {
            final int index;
            final String paramName;
            final ClassName paramType;
            final boolean isBeanReference;
            final String qualifierName;
            final String callExpression;

            FactoryMethodArg(
                    int index,
                    String paramName,
                    ClassName paramType,
                    boolean isBeanReference,
                    String qualifierName,
                    String callExpression) {
                this.index = index;
                this.paramName = paramName;
                this.paramType = paramType;
                this.isBeanReference = isBeanReference;
                this.qualifierName = qualifierName;
                this.callExpression = callExpression;
            }
        }
    }
}
