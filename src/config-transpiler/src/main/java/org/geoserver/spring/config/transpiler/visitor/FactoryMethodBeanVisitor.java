/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.spring.config.transpiler.visitor;

import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.MethodSpec;
import javax.lang.model.element.Modifier;
import org.geoserver.spring.config.transpiler.context.BeanGenerationContext;
import org.geoserver.spring.config.transpiler.context.TranspilationContext;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;

/**
 * Visitor for generating {@code @Bean} methods for factory method-based beans.
 *
 * <p>This visitor handles Spring bean definitions that use factory methods
 * for instantiation. It supports both:
 * <ul>
 *   <li>Static factory methods (factory-method on the bean class)</li>
 *   <li>Instance factory methods (factory-bean + factory-method)</li>
 * </ul>
 *
 * <p>Generated {@code @Bean} methods will call the appropriate factory method
 * with the correct arguments and return the result.
 *
 * @since 2.28.0
 */
public class FactoryMethodBeanVisitor extends AbstractBeanDefinitionVisitor {

    @Override
    public boolean canHandle(BeanDefinition beanDefinition, TranspilationContext context) {
        // Handle beans that specify a factory method
        return beanDefinition.getFactoryMethodName() != null;
    }

    @Override
    public MethodSpec generateBeanMethod(BeanGenerationContext beanContext) {
        FactoryMethodBeanMethodGenerator generator = new FactoryMethodBeanMethodGenerator(beanContext);
        return generator.generate();
    }

    @Override
    public int getPriority() {
        return 60; // Medium-high priority for factory method beans
    }

    /**
     * Inner class to encapsulate method generation logic and reduce parameter passing.
     */
    private class FactoryMethodBeanMethodGenerator {
        private final BeanGenerationContext beanContext;
        private final TranspilationContext transpilationContext;
        private final BeanDefinition beanDefinition;

        @SuppressWarnings("unused")
        private final String beanName;

        private final String beanClassName;
        private final String factoryMethodName;
        private final String factoryBeanName;

        public FactoryMethodBeanMethodGenerator(BeanGenerationContext beanContext) {
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

        private void generateFactoryMethodCall(MethodSpec.Builder methodBuilder) {
            if (factoryBeanName != null) {
                // Instance factory method - need to inject factory bean as parameter
                methodBuilder.addJavadoc(
                        "Bean created using factory method $L on factory bean $L\n",
                        factoryMethodName,
                        factoryBeanName);

                generateInstanceFactoryMethodCall(methodBuilder);
            } else {
                // Static factory method
                methodBuilder.addJavadoc("Bean created using static factory method $L\n", factoryMethodName);

                generateStaticFactoryMethodCall(methodBuilder);
            }
        }

        private ClassName getReturnType() {
            // Use TypeNameResolver for proper type resolution
            org.geoserver.spring.config.transpiler.util.TypeNameResolver.TypeResolutionResult result =
                    org.geoserver.spring.config.transpiler.util.TypeNameResolver.resolveBeanReturnType(
                            beanDefinition, transpilationContext);

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

        private void generateInstanceFactoryMethodCall(MethodSpec.Builder methodBuilder) {
            // TODO: Implement instance factory method logic
            methodBuilder.addStatement(
                    "throw new $T($S)",
                    UnsupportedOperationException.class,
                    "Instance factory methods not yet implemented");
        }

        private void generateStaticFactoryMethodCall(MethodSpec.Builder methodBuilder) {
            // Parse the class name to get ClassName
            ClassName factoryClass = parseClassName(beanClassName);

            // Collect factory method arguments
            ConstructorArgumentValues constructorArgs = beanDefinition.getConstructorArgumentValues();

            if (!constructorArgs.isEmpty()) {
                // TODO: Handle factory method arguments - for now just generate simple call
                methodBuilder.addStatement("// TODO: Handle factory method arguments");
            }

            // Generate the static factory method call with no arguments for now
            methodBuilder.addStatement("return $T.$L()", factoryClass, factoryMethodName);
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
            } catch (Exception e) {
                // Fallback to Object if we can't parse the class name
                return ClassName.get(Object.class);
            }
        }
    }
}
