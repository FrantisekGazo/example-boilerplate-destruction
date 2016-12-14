package eu.f3rog.log

import com.google.common.truth.Truth
import com.google.testing.compile.JavaFileObjects
import com.google.testing.compile.JavaSourcesSubjectFactory
import spock.lang.Specification
import spock.lang.Unroll

public final class LoggedSpecification extends Specification {

    @Unroll
    def "class with 1 logged method with #paramType param"() {
        given:
        def input = JavaFileObjects.forSourceString(
                "com.example.A",
                """
                package com.example;

                import eu.f3rog.log.Logged;

                public class A {
                    @Logged
                    public void b($paramType num) {
                    }
                }
                """
        )
        def output = JavaFileObjects.forSourceString(
                "com.example.A_Logger",
                """
                package com.example;

                import android.util.Log;

                public final class A_Logger {

                    public static void b(A instance, $paramType num) {
                        Log.d("com.example.A.b", "called" + "on " + instance + " with " + "num:" + num);
                    }
                }
                """
        )

        expect:
        Truth.assert_()
                .about(JavaSourcesSubjectFactory.javaSources())
                .that([input])
                .processedWith(new LoggedAnnotationProcessor())
                .compilesWithoutError()
                .and()
                .generatesSources(output)

        where:
        paramType << ["boolean", "byte", "int", "long", "double", "float"]
    }

    def "class with 1 logged static method"() {
        given:
        def input = JavaFileObjects.forSourceString(
                "com.example.A",
                """
                package com.example;

                import eu.f3rog.log.Logged;

                public class A {
                    @Logged
                    public static void b(int num) {
                    }
                }
                """
        )
        def output = JavaFileObjects.forSourceString(
                "com.example.A_Logger",
                """
                package com.example;

                import android.util.Log;

                public final class A_Logger {

                    public static void b(int num) {
                        Log.d("com.example.A.b", "called" + " with " + "num:" + num);
                    }
                }
                """
        )

        expect:
        Truth.assert_()
                .about(JavaSourcesSubjectFactory.javaSources())
                .that([input])
                .processedWith(new LoggedAnnotationProcessor())
                .compilesWithoutError()
                .and()
                .generatesSources(output)
    }

    def "class with invalid usage of @Logged"() {
        given:
        def input = JavaFileObjects.forSourceString(
                "com.example.A",
                """
                package com.example;

                import eu.f3rog.log.Logged;

                @Logged
                public class A {
                }
                """
        )

        expect:
        Truth.assert_()
                .about(JavaSourcesSubjectFactory.javaSources())
                .that([input])
                .processedWith(new LoggedAnnotationProcessor())
                .failsToCompile()
                .withErrorContaining("annotation type not applicable to this kind of declaration")
    }
}
