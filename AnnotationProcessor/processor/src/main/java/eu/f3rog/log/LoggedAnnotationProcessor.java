package eu.f3rog.log;

import com.google.auto.service.AutoService;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeSpec;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.element.VariableElement;
import javax.tools.Diagnostic;

@AutoService(Processor.class)
public class LoggedAnnotationProcessor extends AbstractProcessor {

    private Messager mMessager;
    private Filer mFiler;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        mMessager = processingEnv.getMessager();
        mFiler = processingEnv.getFiler();
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        Set<String> annotations = new HashSet<>();
        annotations.add(Logged.class.getCanonicalName());
        return annotations;
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        final Map<TypeElement, List<ExecutableElement>> annotatedMethods = findMethods(roundEnv);

        final Set<TypeElement> classes = annotatedMethods.keySet();
        for (final TypeElement classElement : classes) {
            try {
                generateLoggedClass(classElement, annotatedMethods.get(classElement));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return false;
    }

    private Map<TypeElement, List<ExecutableElement>> findMethods(RoundEnvironment roundEnv) {
        final Map<TypeElement, List<ExecutableElement>> annotatedMethods = new HashMap<>();

        final Set<? extends Element> elements = roundEnv.getElementsAnnotatedWith(Logged.class);

        for (final Element e : elements) {
            if (e instanceof ExecutableElement) {
                final ExecutableElement methodElement = (ExecutableElement) e;
                final TypeElement classElement = (TypeElement) methodElement.getEnclosingElement();
                if (annotatedMethods.containsKey(classElement)) {
                    final List<ExecutableElement> methods = annotatedMethods.get(classElement);
                    methods.add(methodElement);
                } else {
                    final List<ExecutableElement> methods = new ArrayList<>();
                    methods.add(methodElement);
                    annotatedMethods.put(classElement, methods);
                }
            }
        }

        return annotatedMethods;
    }

    private MethodSpec generateLoggedMethod(TypeElement classElement, ExecutableElement methodElement) {
        final MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder(methodElement.getSimpleName().toString())
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC);

        methodBuilder.addCode("$T.d(", ClassName.get("android.util", "Log"));
        methodBuilder.addCode("$S, $S", classElement.getQualifiedName() + "." + methodElement.getSimpleName(), "called");

        final boolean isStatic = methodElement.getModifiers().contains(Modifier.STATIC);
        if (!isStatic) {
            methodBuilder.addParameter(
                    ParameterSpec.builder(ClassName.get(classElement), "instance").build()
            );
            methodBuilder.addCode(" + $S + $N", "on ", "instance");
        }

        methodBuilder.addCode(" + $S", " with ");

        boolean isFirst = true;
        for (final VariableElement paramElement : methodElement.getParameters()) {
            methodBuilder.addParameter(
                    ParameterSpec.get(paramElement)
            );

            if (isFirst) {
                isFirst = false;
            } else {
                methodBuilder.addCode(" + $S", ", ");
            }
            methodBuilder.addCode(" + $S + $N", paramElement.getSimpleName() + ":", paramElement.getSimpleName());
        }

        methodBuilder.addCode(");\n");

        return methodBuilder.build();
    }

    private void generateLoggedClass(TypeElement classElement, List<ExecutableElement> methodElements) throws IOException {
        // prepare generated class
        final String className = String.format("%s_Logger", classElement.getSimpleName());
        final TypeSpec.Builder generatedClassBuilder = TypeSpec.classBuilder(className)
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL);

        for (final ExecutableElement methodElement : methodElements) {
            generatedClassBuilder.addMethod(generateLoggedMethod(classElement, methodElement));
        }

        // create generated class to a file
        final String qualifiedName = classElement.getQualifiedName().toString();
        final String packageName = qualifiedName.substring(0, qualifiedName.lastIndexOf("."));
        final TypeSpec generatedClass = generatedClassBuilder.build();

        JavaFile.builder(packageName, generatedClass)
                .build()
                .writeTo(mFiler);
    }

    private void logError(Element e, final String msg, Object... args) {
        mMessager.printMessage(Diagnostic.Kind.ERROR, String.format(msg, args), e);
    }
}
