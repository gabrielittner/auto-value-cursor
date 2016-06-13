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

    @Test
    public void simple() {
        JavaFileObject source = JavaFileObjects.forSourceString("test.Test", ""
                + "package test;\n"
                + "import com.google.auto.value.AutoValue;\n"
                + "import android.content.ContentValues;\n"
                + "@AutoValue public abstract class Test {\n"
                + "  public abstract int a();\n"
                + "  public abstract String b();\n"
                + "  public abstract ContentValues toContentValues();\n"
                + "}\n");

        JavaFileObject expected = JavaFileObjects.forSourceString("test.AutoValue_Test", ""
                + "package test;\n"
                + "import android.content.ContentValues;\n"
                + "import java.lang.Override;\n"
                + "import java.lang.String;\n"
                + "final class AutoValue_Test extends $AutoValue_Test {\n"
                + "  AutoValue_Test(int a, String b) {\n"
                + "    super(a, b);\n"
                + "  }\n"
                + "  @Override\n"
                + "  public ContentValues toContentValues() {\n"
                + "    ContentValues values = new ContentValues(2);\n"
                + "    values.put(\"a\", a());\n"
                + "    values.put(\"b\", b());\n"
                + "    return values;\n"
                + "  }\n"
                + "}\n");

        assertAbout(javaSources())
                .that(Collections.singletonList(source))
                .processedWith(new AutoValueProcessor())
                .compilesWithoutError()
                .and()
                .generatesSources(expected);
    }

    @Test
    public void columnName() {
        JavaFileObject source = JavaFileObjects.forSourceString("test.Test", ""
                + "package test;\n"
                + "import com.gabrielittner.auto.value.cursor.ColumnName;\n"
                + "import com.google.auto.value.AutoValue;\n"
                + "import android.content.ContentValues;\n"
                + "@AutoValue public abstract class Test {\n"
                + "  public abstract int a();\n"
                + "  @ColumnName(\"column_b\") public abstract String b();\n"
                + "  public abstract ContentValues toContentValues();\n"
                + "}\n");

        JavaFileObject expected = JavaFileObjects.forSourceString("test.AutoValue_Test", ""
                + "package test;\n"
                + "import android.content.ContentValues;\n"
                + "import java.lang.Override;\n"
                + "import java.lang.String;\n"
                + "final class AutoValue_Test extends $AutoValue_Test {\n"
                + "  AutoValue_Test(int a, String b) {\n"
                + "    super(a, b);\n"
                + "  }\n"
                + "  @Override\n"
                + "  public ContentValues toContentValues() {\n"
                + "    ContentValues values = new ContentValues(2);\n"
                + "    values.put(\"a\", a());\n"
                + "    values.put(\"column_b\", b());\n"
                + "    return values;\n"
                + "  }\n"
                + "}\n");

        assertAbout(javaSources())
                .that(Collections.singletonList(source))
                .processedWith(new AutoValueProcessor())
                .compilesWithoutError()
                .and()
                .generatesSources(expected);
    }

    @Test
    public void unsupported() {
        JavaFileObject source = JavaFileObjects.forSourceString("test.Test", ""
                + "package test;\n"
                + "import com.google.auto.value.AutoValue;\n"
                + "import android.content.ContentValues;\n"
                + "import javax.annotation.Nullable;\n"
                + "@AutoValue public abstract class Test {\n"
                + "  @Nullable public abstract int[] a();\n"
                + "  public abstract String b();\n"
                + "  public abstract ContentValues toContentValues();\n"
                + "}\n");

        assertAbout(javaSources())
                .that(Collections.singletonList(source))
                .processedWith(new AutoValueProcessor())
                .failsToCompile()
                .withErrorContaining("Property has type that can't be put into ContentValues.");
    }

    @Test
    public void allContentValuesTypes() {
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
                + "}\n");

        JavaFileObject expected = JavaFileObjects.forSourceString("test.AutoValue_Test", ""
                + "package test;\n"
                + "import android.content.ContentValues;\n"
                + "import java.lang.Boolean;\n"
                + "import java.lang.Double;\n"
                + "import java.lang.Float;\n"
                + "import java.lang.Integer;\n"
                + "import java.lang.Long;\n"
                + "import java.lang.Override;\n"
                + "import java.lang.Short;\n"
                + "import java.lang.String;\n"
                + "final class AutoValue_Test extends $AutoValue_Test {\n"
                + "  AutoValue_Test(String a, int b, Integer c, long d, Long e, short f, Short g, double h, Double i, float j, Float k, boolean l, Boolean m, byte[] n) {\n"
                + "    super(a, b, c, d, e, f, g, h, i, j, k, l, m, n);\n"
                + "  }\n"
                + "  @Override\n"
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
                + "  }\n"
                + "}\n");

        assertAbout(javaSources())
                .that(Collections.singletonList(source))
                .processedWith(new AutoValueProcessor())
                .compilesWithoutError()
                .and()
                .generatesSources(expected);
    }

    @Test
    public void valuesAdapter() {
        JavaFileObject fooClass = JavaFileObjects.forSourceString("test.Foo", ""
                + "package test;\n"
                + "public class Foo {\n"
                + "  public final String data;\n"
                + "  public Foo(String data) {\n"
                + "    this.data = data;\n"
                + "  }\n"
                + "}\n");
        JavaFileObject fooFactorySource = JavaFileObjects.forSourceString("test.FooAdapter", ""
                + "package test;\n"
                + "import android.content.ContentValues;\n"
                + "import android.database.Cursor;\n"
                + "import com.gabrielittner.auto.value.cursor.ColumnTypeAdapter;\n"
                + "public class FooAdapter implements ColumnTypeAdapter<Foo> {\n"
                + "  public Foo fromCursor(Cursor cursor, String columnName) {\n"
                + "    return new Foo(cursor.getString(cursor.getColumnIndex(columnName)));\n"
                + "  }\n"
                + "  public void toContentValues(ContentValues values, String columnName, Foo value) {\n"
                + "    values.put(columnName, value.data);\n"
                + "  }\n"
                + "}\n");
        JavaFileObject stringFactorySource = JavaFileObjects.forSourceString("test.StringAdapter", ""
                + "package test;\n"
                + "import android.content.ContentValues;\n"
                + "import android.database.Cursor;\n"
                + "import com.gabrielittner.auto.value.cursor.ColumnTypeAdapter;\n"
                + "public class StringAdapter implements ColumnTypeAdapter<String> {\n"
                + "  public String fromCursor(Cursor cursor, String columnName) {\n"
                + "    return cursor.getString(cursor.getColumnIndex(columnName));\n"
                + "  }\n"
                + "  public void toContentValues(ContentValues values, String columnName, String value) {\n"
                + "    values.put(columnName, value);\n"
                + "  }\n"
                + "}\n");
        JavaFileObject source = JavaFileObjects.forSourceString("test.Test", ""
                + "package test;\n"
                + "import android.content.ContentValues;\n"
                + "import com.gabrielittner.auto.value.cursor.ColumnName;\n"
                + "import com.gabrielittner.auto.value.cursor.ColumnAdapter;\n"
                + "import com.google.auto.value.AutoValue;\n"
                + "import javax.annotation.Nullable;\n"
                + "@AutoValue public abstract class Test {\n"
                + "  public abstract ContentValues toContentValues();\n"
                + "  @ColumnAdapter(FooAdapter.class) public abstract Foo foo();\n"
                + "  @ColumnAdapter(StringAdapter.class) public abstract String bar();\n"
                + "  @ColumnAdapter(StringAdapter.class) @ColumnName(\"column\") public abstract String columnName();\n"
                + "}\n");
        JavaFileObject expected = JavaFileObjects.forSourceString("test.AutoValue_Test", ""
                + "package test;\n"
                + "import android.content.ContentValues;\n"
                + "import java.lang.Override;\n"
                + "import java.lang.String;\n"
                + "final class AutoValue_Test extends $AutoValue_Test {\n"
                + "  AutoValue_Test(Foo foo, String bar, String columnName) {\n"
                + "    super(foo, bar, columnName);\n"
                + "  }\n"
                + "  @Override\n"
                + "  public ContentValues toContentValues() {\n"
                + "    ContentValues values = new ContentValues(3);\n"
                + "    FooAdapter fooAdapter = new FooAdapter();\n"
                + "    StringAdapter stringAdapter = new StringAdapter();\n"
                + "    fooAdapter.toContentValues(values, \"foo\", foo());\n"
                + "    stringAdapter.toContentValues(values,  \"bar\", bar());\n"
                + "    stringAdapter.toContentValues(values, \"column\", columnName());\n"
                + "    return values;\n"
                + "  }\n"
                + "}\n");

        assertAbout(javaSources())
                .that(Arrays.asList(fooClass, stringFactorySource, fooFactorySource, source))
                .processedWith(new AutoValueProcessor())
                .compilesWithoutError()
                .and()
                .generatesSources(expected);
    }

    @Test
    public void baseClass() {
        JavaFileObject source = JavaFileObjects.forSourceString("test.BaseTest", ""
                + "package test;\n"
                + "import android.content.ContentValues;\n"
                + "public abstract class BaseTest {\n"
                + "  public abstract ContentValues toContentValues();\n"
                + "}\n");
        JavaFileObject source2 = JavaFileObjects.forSourceString("test.Test", ""
                + "package test;\n"
                + "import com.google.auto.value.AutoValue;\n"
                + "@AutoValue public abstract class Test extends BaseTest {\n"
                + "  public abstract int a();\n"
                + "  public abstract String b();\n"
                + "}\n");

        JavaFileObject expected = JavaFileObjects.forSourceString("test.AutoValue_Test", ""
                + "package test;\n"
                + "import android.content.ContentValues;\n"
                + "import java.lang.Override;\n"
                + "import java.lang.String;\n"
                + "final class AutoValue_Test extends $AutoValue_Test {\n"
                + "  AutoValue_Test(int a, String b) {\n"
                + "    super(a, b);\n"
                + "  }\n"
                + "  @Override\n"
                + "  public ContentValues toContentValues() {\n"
                + "    ContentValues values = new ContentValues(2);\n"
                + "    values.put(\"a\", a());\n"
                + "    values.put(\"b\", b());\n"
                + "    return values;\n"
                + "  }\n"
                + "}\n");

        assertAbout(javaSources())
                .that(Arrays.asList(source, source2))
                .processedWith(new AutoValueProcessor())
                .compilesWithoutError()
                .and()
                .generatesSources(expected);
    }

    @Test
    public void prefixedMethods() {
        JavaFileObject source = JavaFileObjects.forSourceString("test.Test", ""
                + "package test;\n"
                + "import com.google.auto.value.AutoValue;\n"
                + "import android.content.ContentValues;\n"
                + "@AutoValue public abstract class Test {\n"
                + "  public abstract int getA();\n"
                + "  public abstract String getB();\n"
                + "  public abstract ContentValues toContentValues();\n"
                + "}\n");

        JavaFileObject expected = JavaFileObjects.forSourceString("test.AutoValue_Test", ""
                + "package test;\n"
                + "import android.content.ContentValues;\n"
                + "import java.lang.Override;\n"
                + "import java.lang.String;\n"
                + "final class AutoValue_Test extends $AutoValue_Test {\n"
                + "  AutoValue_Test(int a, String b) {\n"
                + "    super(a, b);\n"
                + "  }\n"
                + "  @Override\n"
                + "  public ContentValues toContentValues() {\n"
                + "    ContentValues values = new ContentValues(2);\n"
                + "    values.put(\"a\", getA());\n"
                + "    values.put(\"b\", getB());\n"
                + "    return values;\n"
                + "  }\n"
                + "}\n");

        assertAbout(javaSources())
                .that(Collections.singletonList(source))
                .processedWith(new AutoValueProcessor())
                .compilesWithoutError()
                .and()
                .generatesSources(expected);
    }
}
