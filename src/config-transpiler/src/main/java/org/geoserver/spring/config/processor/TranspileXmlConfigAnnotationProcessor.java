/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.spring.config.processor;

import java.util.Set;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import org.geoserver.spring.config.annotations.TranspileXmlConfig;
import org.geoserver.spring.config.transpiler.XmlToJavaTranspiler;

/**
 * Annotation processor for {@link TranspileXmlConfig} that generates Spring {@code @Configuration} classes
 * from XML bean definitions at compile time.
 *
 * <p>This processor serves as the JSR 269 entry point and delegates the actual transpilation
 * work to {@link XmlToJavaTranspiler}. It handles:
 * <ul>
 *   <li>Processing the annotation and extracting configuration parameters</li>
 *   <li>Validating the annotated elements</li>
 *   <li>Coordinating the transpilation process</li>
 *   <li>Error reporting and diagnostics</li>
 * </ul>
 *
 * <p>The separation of concerns allows the transpiler to be tested and used independently
 * of the annotation processing infrastructure.
 *
 * @since 2.28.0
 * @see TranspileXmlConfig
 * @see XmlToJavaTranspiler
 */
@SupportedAnnotationTypes({
    "org.geoserver.spring.config.annotations.TranspileXmlConfig",
    "org.geoserver.spring.config.annotations.TranspileXmlConfig.List"
})
@SupportedSourceVersion(SourceVersion.RELEASE_17)
public class TranspileXmlConfigAnnotationProcessor extends AbstractProcessor {

    private XmlToJavaTranspiler transpiler;
    private Messager messager;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        this.messager = processingEnv.getMessager();
        this.transpiler = new XmlToJavaTranspiler(processingEnv);
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (roundEnv.processingOver()) {
            return false;
        }

        // Find all elements annotated with @TranspileXmlConfig (individual annotations)
        Set<? extends Element> annotatedElements = roundEnv.getElementsAnnotatedWith(TranspileXmlConfig.class);

        // Find all elements annotated with @TranspileXmlConfig.List (multiple annotations)
        Set<? extends Element> listAnnotatedElements = roundEnv.getElementsAnnotatedWith(TranspileXmlConfig.List.class);

        messager.printMessage(
                Diagnostic.Kind.NOTE,
                "Found " + annotatedElements.size() + " @TranspileXmlConfig, " + listAnnotatedElements.size()
                        + " @TranspileXmlConfig.List");

        // Process individual annotations
        for (Element element : annotatedElements) {
            if (!validateAnnotatedElement(element)) {
                continue; // Skip invalid elements
            }

            try {
                processAnnotatedElement((TypeElement) element);
            } catch (Exception e) {
                messager.printMessage(
                        Diagnostic.Kind.ERROR,
                        "Failed to process @TranspileXmlConfig annotation: " + e.getMessage(),
                        element);
            }
        }

        // Process List annotations (multiple @TranspileXmlConfig on same element)
        for (Element element : listAnnotatedElements) {
            if (!validateAnnotatedElement(element)) {
                continue; // Skip invalid elements
            }

            try {
                processAnnotatedElement((TypeElement) element);
            } catch (Exception e) {
                messager.printMessage(
                        Diagnostic.Kind.ERROR,
                        "Failed to process @TranspileXmlConfig.List annotation: " + e.getMessage(),
                        element);
            }
        }

        return true; // Claim the annotations
    }

    /**
     * Validate that the annotated element is appropriate for processing.
     */
    private boolean validateAnnotatedElement(Element element) {
        if (element.getKind() != ElementKind.CLASS) {
            messager.printMessage(Diagnostic.Kind.ERROR, "@TranspileXmlConfig can only be applied to classes", element);
            return false;
        }

        // TypeElement typeElement = (TypeElement) element;

        // Additional validation could be added here:
        // - Check that it's a {@code @Configuration} class
        // - Verify package accessibility
        // - Validate annotation parameters

        return true;
    }

    /**
     * Process a single annotated element by delegating to the transpiler.
     */
    private void processAnnotatedElement(TypeElement typeElement) {
        // Handle single @TranspileXmlConfig annotation
        TranspileXmlConfig annotation = typeElement.getAnnotation(TranspileXmlConfig.class);
        if (annotation != null) {
            transpiler.transpile(typeElement, annotation);
        }

        // Handle multiple @TranspileXmlConfig annotations via @TranspileXmlConfig.List
        TranspileXmlConfig.List annotationList = typeElement.getAnnotation(TranspileXmlConfig.List.class);
        if (annotationList != null) {
            for (TranspileXmlConfig config : annotationList.value()) {
                transpiler.transpile(typeElement, config);
            }
        }
    }
}
