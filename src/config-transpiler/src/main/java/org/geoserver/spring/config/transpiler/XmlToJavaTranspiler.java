/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.spring.config.transpiler;

import com.squareup.javapoet.JavaFile;
import java.io.IOException;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import org.geoserver.spring.config.annotations.TranspileXmlConfig;
import org.geoserver.spring.config.transpiler.context.TranspilationContext;
import org.geoserver.spring.config.transpiler.visitor.JavaFileGeneratorVisitor;

/**
 * Core transpiler that converts XML Spring configurations to Java {@code @Configuration} classes.
 *
 * <p>This class orchestrates the transpilation process by:
 * <ul>
 *   <li>Creating a transpilation context from the annotation parameters</li>
 *   <li>Loading and parsing XML resources</li>
 *   <li>Using visitors to generate Java code</li>
 *   <li>Writing the generated files using the annotation processing API</li>
 * </ul>
 *
 * <p>The transpiler follows a visitor pattern where different aspects of code generation
 * are handled by specialized visitors, making the system extensible and maintainable.
 *
 * @since 2.28.0
 * @see TranspileXmlConfig
 * @see TranspilationContext
 * @see JavaFileGeneratorVisitor
 */
public class XmlToJavaTranspiler {

    private final ProcessingEnvironment processingEnv;
    private final Messager messager;
    private final Filer filer;

    private final JavaFileGeneratorVisitor javaFileGenerator;

    /**
     * Create a new transpiler with the given processing environment.
     *
     * @param processingEnv the annotation processing environment
     */
    public XmlToJavaTranspiler(ProcessingEnvironment processingEnv) {
        this.processingEnv = processingEnv;
        this.messager = processingEnv.getMessager();
        this.filer = processingEnv.getFiler();

        // Initialize the visitor hierarchy
        this.javaFileGenerator = new JavaFileGeneratorVisitor();
    }

    /**
     * Transpile XML configuration to Java code for the given annotated element.
     *
     * @param annotatedElement the class annotated with @TranspileXmlConfig
     * @param annotation the annotation instance containing configuration
     */
    public void transpile(TypeElement annotatedElement, TranspileXmlConfig annotation) {
        try {
            // Create transpilation context
            TranspilationContext context = createTranspilationContext(annotatedElement, annotation);

            // Generate Java file using visitor pattern
            JavaFile javaFile = javaFileGenerator.generateJavaFile(context);

            // Write the generated file
            writeJavaFile(javaFile);

            messager.printMessage(
                    Diagnostic.Kind.NOTE,
                    "Generated " + javaFile.packageName + "." + javaFile.typeSpec.name
                            + " from XML configuration transpilation");

        } catch (Exception e) {
            messager.printMessage(Diagnostic.Kind.ERROR, "Transpilation failed: " + e.getMessage(), annotatedElement);
            // Log stack trace for debugging in verbose mode
            e.printStackTrace();
        }
    }

    /**
     * Create a transpilation context from the annotation and annotated element.
     */
    private TranspilationContext createTranspilationContext(
            TypeElement annotatedElement, TranspileXmlConfig annotation) {

        return TranspilationContext.builder()
                .annotatedElement(annotatedElement)
                .annotation(annotation)
                .processingEnvironment(processingEnv)
                .build();
    }

    /**
     * Write the generated Java file using the annotation processing API.
     */
    private void writeJavaFile(JavaFile javaFile) throws IOException {
        javaFile.writeTo(filer);
    }
}
