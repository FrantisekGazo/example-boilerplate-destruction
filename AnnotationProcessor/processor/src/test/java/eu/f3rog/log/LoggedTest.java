package eu.f3rog.log;

import com.google.common.base.Joiner;
import com.google.common.truth.Truth;
import com.google.testing.compile.JavaFileObjects;
import com.google.testing.compile.JavaSourcesSubjectFactory;

import org.junit.Test;

import java.util.Arrays;

import javax.tools.JavaFileObject;


public class LoggedTest {

    private static final String NEW_LINE = "\n";

    @Test
    public void classWithOneLoggedMethod() {
        final JavaFileObject input = JavaFileObjects.forSourceString(
                "com.example.A",
                Joiner.on(NEW_LINE).join(
                        "package com.example;",
                        "",
                        "import eu.f3rog.log.Logged;",
                        "",
                        "public class A {",
                        "",
                        "   @Logged",
                        "   public void b(int num) {",
                        "   }",
                        "}"
                )
        );
        final JavaFileObject output = JavaFileObjects.forSourceString(
                "com.example.A_Logger",
                Joiner.on(NEW_LINE).join(
                        "package com.example;",
                        "",
                        "import android.util.Log;",
                        "",
                        "public final class A_Logger {",
                        "",
                        "   public static void b(A instance, int num) {",
                        "       Log.d(\"com.example.A.b\", \"called\" + \"on \" + instance + \" with \" + \"num:\" + num);",
                        "   }",
                        "}"
                )
        );

        Truth.assert_()
                .about(JavaSourcesSubjectFactory.javaSources())
                .that(Arrays.asList(input))
                .processedWith(new LoggedAnnotationProcessor())
                .compilesWithoutError()
                .and()
                .generatesSources(output);
    }

    @Test
    public void classWithOneLoggedStaticMethod() {
        final JavaFileObject input = JavaFileObjects.forSourceString(
                "com.example.A",
                Joiner.on(NEW_LINE).join(
                        "package com.example;",
                        "",
                        "import eu.f3rog.log.Logged;",
                        "",
                        "public class A {",
                        "",
                        "   @Logged",
                        "   public static void b(int num) {",
                        "   }",
                        "}"
                )
        );
        final JavaFileObject output = JavaFileObjects.forSourceString(
                "com.example.A_Logger",
                Joiner.on(NEW_LINE).join(
                        "package com.example;",
                        "",
                        "import android.util.Log;",
                        "",
                        "public final class  A_Logger {",
                        "",
                        "   public static void b(int num) {",
                        "       Log.d(\"com.example.A.b\", \"called\" + \" with \" + \"num:\" + num);",
                        "   }",
                        "}"
                )
        );

        Truth.assert_()
                .about(JavaSourcesSubjectFactory.javaSources())
                .that(Arrays.asList(input))
                .processedWith(new LoggedAnnotationProcessor())
                .compilesWithoutError()
                .and()
                .generatesSources(output);
    }

    @Test
    public void invalidAnnotationUsage() {
        final JavaFileObject input = JavaFileObjects.forSourceString(
                "com.example.A",
                Joiner.on(NEW_LINE).join(
                        "package com.example;",
                        "",
                        "import eu.f3rog.log.Logged;",
                        "",
                        "@Logged",
                        "public class A {",
                        "}"
                )
        );

        Truth.assert_()
                .about(JavaSourcesSubjectFactory.javaSources())
                .that(Arrays.asList(input))
                .processedWith(new LoggedAnnotationProcessor())
                .failsToCompile()
                .withErrorContaining("annotation type not applicable to this kind of declaration");
    }
}
