package com.gabrielittner.auto.value.contentvalues;

import com.google.auto.value.processor.AutoValueProcessor;
import com.google.testing.compile.JavaFileObjects;
import java.util.Arrays;
import java.util.Collections;
import javax.tools.JavaFileObject;
import org.junit.Test;

import static com.google.common.truth.Truth.assertAbout;
import static com.google.testing.compile.JavaSourcesSubjectFactory.javaSources;

public class AutoValueContentValuesExtensionTest {

    @Test public void simple() {
        JavaFileObject source = JavaFileObjects.forSourceString("test.Test", ""
                + "package test;\n"
                + "import com.google.auto.value.AutoValue;\n"
                + "import android.content.ContentValues;\n"
                + "@AutoValue public abstract class Test {\n"
                + "  public abstract int a();\n"
                + "  public abstract String b();\n"
                + "  public abstract ContentValues toContentValues();\n"
                + "}\n"
        );

        JavaFileObject expected = JavaFileObjects.forSourceString("test.AutoValue_Test", ""
                + "package test;\n"
                + "\n"
                + "import android.content.ContentValues;\n"
                + "import java.lang.String;\n"
                + "\n"
                + "final class AutoValue_Test extends $AutoValue_Test {\n"
                + "  AutoValue_Test(int a, String b) {\n"
                + "    super(a, b);\n"
                + "  }\n"
                + "\n"
                + "  public ContentValues toContentValues() {\n"
                + "    ContentValues values = new ContentValues(2);\n"
                + "    values.put(\"a\", a());\n"
                + "    values.put(\"b\", b());\n"
                + "    return values;\n"
                + "  }\n"
                + "}"
        );

        assertAbout(javaSources()).that(Collections.singletonList(source))
                .processedWith(new AutoValueProcessor())
                .compilesWithoutError()
                .and()
                .generatesSources(expected);
    }

    @Test public void columnName() {
        JavaFileObject source = JavaFileObjects.forSourceString("test.Test", ""
                + "package test;\n"
                + "import com.gabrielittner.auto.value.cursor.ColumnName;\n"
                + "import com.google.auto.value.AutoValue;\n"
                + "import android.content.ContentValues;\n"
                + "@AutoValue public abstract class Test {\n"
                + "  public abstract int a();\n"
                + "  @ColumnName(\"column_b\") public abstract String b();\n"
                + "  public abstract ContentValues toContentValues();\n"
                + "}\n"
        );

        JavaFileObject expected = JavaFileObjects.forSourceString("test.AutoValue_Test", ""
                + "package test;\n"
                + "\n"
                + "import android.content.ContentValues;\n"
                + "import java.lang.String;\n"
                + "\n"
                + "final class AutoValue_Test extends $AutoValue_Test {\n"
                + "  AutoValue_Test(int a, String b) {\n"
                + "    super(a, b);\n"
                + "  }\n"
                + "\n"
                + "  public ContentValues toContentValues() {\n"
                + "    ContentValues values = new ContentValues(2);\n"
                + "    values.put(\"a\", a());\n"
                + "    values.put(\"column_b\", b());\n"
                + "    return values;\n"
                + "  }\n"
                + "}"
        );

        assertAbout(javaSources()).that(Collections.singletonList(source))
                .processedWith(new AutoValueProcessor())
                .compilesWithoutError()
                .and()
                .generatesSources(expected);
    }

    @Test public void unsupported() {
        JavaFileObject source = JavaFileObjects.forSourceString("test.Test", ""
                + "package test;\n"
                + "import com.google.auto.value.AutoValue;\n"
                + "import android.content.ContentValues;\n"
                + "import javax.annotation.Nullable;\n"
                + "@AutoValue public abstract class Test {\n"
                + "  @Nullable public abstract int[] a();\n"
                + "  public abstract String b();\n"
                + "  public abstract ContentValues toContentValues();\n"
                + "}\n"
        );

        assertAbout(javaSources())
                .that(Collections.singletonList(source))
                .processedWith(new AutoValueProcessor())
                .failsToCompile()
                .withErrorContaining("Property \"a\" has type \"int[]\" that can't be put"
                        + " into ContentValues.");
    }

    @Test public void allContentValuesTypes() {
        JavaFileObject source = JavaFileObjects.forSourceString("test.Test", ""
                + "package test;\n"
                + "import com.google.auto.value.AutoValue;\n"
                + "import android.content.ContentValues;\n"
                + "@AutoValue public abstract class Test {\n"
                + "  public abstract String a();\n"
                + "  public abstract int b();\n"
                + "  public abstract Integer c();\n"
                + "  public abstract long d();\n"
                + "  public abstract Long e();\n"
                + "  public abstract short f();\n"
                + "  public abstract Short g();\n"
                + "  public abstract double h();\n"
                + "  public abstract Double i();\n"
                + "  public abstract float j();\n"
                + "  public abstract Float k();\n"
                + "  public abstract boolean l();\n"
                + "  public abstract Boolean m();\n"
                + "  public abstract byte[] n();\n"
                + "  public abstract ContentValues contentValues();\n"
                + "}\n"
        );

        JavaFileObject expected = JavaFileObjects.forSourceString("test.AutoValue_Test", ""
                + "package test;\n"
                + "\n"
                + "import android.content.ContentValues;\n"
                + "import java.lang.Boolean;\n"
                + "import java.lang.Double;\n"
                + "import java.lang.Float;\n"
                + "import java.lang.Integer;\n"
                + "import java.lang.Long;\n"
                + "import java.lang.Short;\n"
                + "import java.lang.String;\n"
                + "\n"
                + "final class AutoValue_Test extends $AutoValue_Test {\n"
                + "  AutoValue_Test(String a, int b, Integer c, long d, Long e, short f, Short g, double h, Double i, float j, Float k, boolean l, Boolean m, byte[] n) {\n"
                + "    super(a, b, c, d, e, f, g, h, i, j, k, l, m, n);\n"
                + "  }\n"
                + "\n"
                + "  public ContentValues contentValues() {\n"
                + "    ContentValues values = new ContentValues(14);\n"
                + "    values.put(\"a\", a());\n"
                + "    values.put(\"b\", b());\n"
                + "    values.put(\"c\", c());\n"
                + "    values.put(\"d\", d());\n"
                + "    values.put(\"e\", e());\n"
                + "    values.put(\"f\", f());\n"
                + "    values.put(\"g\", g());\n"
                + "    values.put(\"h\", h());\n"
                + "    values.put(\"i\", i());\n"
                + "    values.put(\"j\", j());\n"
                + "    values.put(\"k\", k());\n"
                + "    values.put(\"l\", l());\n"
                + "    values.put(\"m\", m());\n"
                + "    values.put(\"n\", n());\n"
                + "    return values;\n"
                + "  }"
                + "\n"
                + "}"
        );

        assertAbout(javaSources()).that(Collections.singletonList(source))
                .processedWith(new AutoValueProcessor())
                .compilesWithoutError()
                .and()
                .generatesSources(expected);
    }

    @Test public void customTypes() {
        JavaFileObject fooClass = JavaFileObjects.forSourceString("test.Foo", ""
                + "package test;\n"
                + "public class Foo {\n"
                + "}"
        );
        JavaFileObject source = JavaFileObjects.forSourceString("test.Test", ""
                + "package test;\n"
                + "import com.gabrielittner.auto.value.cursor.ColumnName;\n"
                + "import com.gabrielitter.auto.value.contentvalues.ValuesAdapter;\n"
                + "import com.google.auto.value.AutoValue;\n"
                + "import javax.annotation.Nullable;\n"
                + "import android.content.ContentValues;\n"
                + "@AutoValue public abstract class Test {\n"
                + "  @ValuesAdapter(FooFactory.class) public abstract Foo foo();\n"
                + "  public abstract ContentValues toContentValues();"
                + "}\n"
        );
        JavaFileObject fooFactorySource = JavaFileObjects.forSourceString("test.FooFactory", ""
                + "package test;\n"
                + "\n"
                + "import android.content.ContentValues;\n"
                + "\n"
                + "public class FooFactory {\n"
                + "  public static ContentValues createContentValues(Foo foo) { "
                + "    return new ContentValues();\n"
                + "  }\n"
                + "}\n"
        );
        JavaFileObject expected = JavaFileObjects.forSourceString("test.AutoValue_Test", ""
                + "package test;\n"
                + "\n"
                + "import android.content.ContentValues;\n"
                + "\n"
                + "final class AutoValue_Test extends $AutoValue_Test {\n"
                + "  AutoValue_Test(Foo foo) {\n"
                + "    super(foo);\n"
                + "  }\n"
                + "\n"
                + "  public ContentValues toContentValues() {\n"
                + "    ContentValues values = new ContentValues(1);\n"
                + "    ContentValues fooValues = FooFactory.createContentValues(foo());\n"
                + "    if (fooValues != null) values.putAll(fooValues);\n"
                + "    return values;\n"
                + "  }\n"
                + "}"
        );

        assertAbout(javaSources()).that(Arrays.asList(fooClass, fooFactorySource, source))
                .processedWith(new AutoValueProcessor())
                .compilesWithoutError()
                .and()
                .generatesSources(expected);
    }

    @Test public void unsupportedValuesFactory() {
        JavaFileObject fooClass = JavaFileObjects.forSourceString("test.Foo", ""
                + "package test;\n"
                + "public class Foo {\n"
                + "}"
        );
        JavaFileObject source = JavaFileObjects.forSourceString("test.Test", ""
                + "package test;\n"
                + "import com.gabrielittner.auto.value.cursor.ColumnName;\n"
                + "import com.gabrielitter.auto.value.contentvalues.ValuesAdapter;\n"
                + "import com.google.auto.value.AutoValue;\n"
                + "import javax.annotation.Nullable;\n"
                + "import android.content.ContentValues;\n"
                + "@AutoValue public abstract class Test {\n"
                + "  @ValuesAdapter(FooFactory.class) public abstract Foo foo();\n"
                + "  public abstract ContentValues toContentValues();"
                + "}\n"
        );
        JavaFileObject fooFactorySource = JavaFileObjects.forSourceString("test.FooFactory", ""
                + "package test;\n"
                + "\n"
                + "public class FooFactory {\n"
                + "}\n"
        );

        assertAbout(javaSources()).that(Arrays.asList(fooClass, fooFactorySource, source))
                .processedWith(new AutoValueProcessor())
                .failsToCompile()
                .withErrorContaining("Class \"test.FooFactory\" needs to define a public static"
                        + " method taking \"test.Foo\" and returning \"ContentValues\"");
    }
}
