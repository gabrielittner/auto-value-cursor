package com.gabrielittner.auto.value.cursor;

import com.google.auto.value.processor.AutoValueProcessor;
import com.google.testing.compile.JavaFileObjects;
import java.util.Arrays;
import java.util.Collections;
import javax.tools.JavaFileObject;
import org.junit.Test;

import static com.google.common.truth.Truth.assertAbout;
import static com.google.testing.compile.JavaSourcesSubjectFactory.javaSources;

public class AutoValueCursorExtensionTest {

    @Test public void simple() {
        JavaFileObject source = JavaFileObjects.forSourceString("test.Test", ""
                + "package test;\n"
                + "import com.google.auto.value.AutoValue;\n"
                + "import android.database.Cursor;\n"
                + "@AutoValue public abstract class Test {\n"
                + "  public static Test blah(Cursor cursor) { return null; }\n"
                + "  public abstract int a();"
                + "  public abstract String b();"
                + "}\n"
        );

        JavaFileObject expected = JavaFileObjects.forSourceString("test.AutoValue_Test", ""
                + "package test;\n"
                + "\n"
                + "import android.database.Cursor;\n"
                + "import java.lang.String;\n"
                + "\n"
                + "final class AutoValue_Test extends $AutoValue_Test {\n"
                + "  AutoValue_Test(int a, String b) {\n"
                + "    super(a, b);\n"
                + "  }\n"
                + "\n"
                + "  static AutoValue_Test createFromCursor(Cursor cursor) {\n"
                + "    int a = cursor.getInt(cursor.getColumnIndexOrThrow(\"a\"));\n"
                + "    String b = cursor.getString(cursor.getColumnIndexOrThrow(\"b\"));\n"
                + "    return new AutoValue_Test(a, b);\n"
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
                + "import android.database.Cursor;\n"
                + "@AutoValue public abstract class Test {\n"
                + "  public static Test blah(Cursor cursor) { return null; }\n"
                + "  public abstract int a();"
                + "  @ColumnName(\"column_b\") public abstract String b();"
                + "}\n"
        );

        JavaFileObject expected = JavaFileObjects.forSourceString("test.AutoValue_Test", ""
                + "package test;\n"
                + "\n"
                + "import android.database.Cursor;\n"
                + "import java.lang.String;\n"
                + "\n"
                + "final class AutoValue_Test extends $AutoValue_Test {\n"
                + "  AutoValue_Test(int a, String b) {\n"
                + "    super(a, b);\n"
                + "  }\n"
                + "\n"
                + "  static AutoValue_Test createFromCursor(Cursor cursor) {\n"
                + "    int a = cursor.getInt(cursor.getColumnIndexOrThrow(\"a\"));\n"
                + "    String b = cursor.getString(cursor.getColumnIndexOrThrow(\"column_b\"));\n"
                + "    return new AutoValue_Test(a, b);\n"
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
                + "import android.database.Cursor;\n"
                + "@AutoValue public abstract class Test {\n"
                + "  public static Test blah(Cursor cursor) { return null; }\n"
                + "  public abstract int[] a();"
                + "  public abstract String b();"
                + "}\n"
        );

        assertAbout(javaSources())
                .that(Collections.singletonList(source))
                .processedWith(new AutoValueProcessor())
                .failsToCompile()
                .withErrorContaining("Property \"a\" has type \"int[]\" that can't be read"
                        + " from Cursor.");
    }

    @Test public void unsupportedWithNullable() {
        JavaFileObject source = JavaFileObjects.forSourceString("test.Test", ""
                + "package test;\n"
                + "import com.google.auto.value.AutoValue;\n"
                + "import android.database.Cursor;\n"
                + "import javax.annotation.Nullable;\n"
                + "@AutoValue public abstract class Test {\n"
                + "  public static Test blah(Cursor cursor) { return null; }\n"
                + "  @Nullable public abstract int[] a();"
                + "  public abstract String b();"
                + "}\n"
        );

        JavaFileObject expected = JavaFileObjects.forSourceString("test.AutoValue_Test", ""
                + "package test;\n"
                + "\n"
                + "import android.database.Cursor;\n"
                + "import java.lang.String;\n"
                + "\n"
                + "final class AutoValue_Test extends $AutoValue_Test {\n"
                + "  AutoValue_Test(int[] a, String b) {\n"
                + "    super(a, b);\n"
                + "  }\n"
                + "\n"
                + "  static AutoValue_Test createFromCursor(Cursor cursor) {\n"
                + "    int[] a = null; // can't be read from cursor\n"
                + "    String b = cursor.getString(cursor.getColumnIndexOrThrow(\"b\"));\n"
                + "    return new AutoValue_Test(a, b);\n"
                + "  }\n"
                + "}"
        );

        assertAbout(javaSources()).that(Collections.singletonList(source))
                .processedWith(new AutoValueProcessor())
                .compilesWithoutError()
                .and()
                .generatesSources(expected);
    }

    @Test public void allCursorTypes() {
        JavaFileObject source = JavaFileObjects.forSourceString("test.Test", ""
                + "package test;\n"
                + "import com.google.auto.value.AutoValue;\n"
                + "import android.database.Cursor;\n"
                + "@AutoValue public abstract class Test {\n"
                + "  public static Test blah(Cursor cursor) { return null; }\n"
                + "  public abstract String a();"
                + "  public abstract int b();"
                + "  public abstract Integer c();"
                + "  public abstract long d();"
                + "  public abstract Long e();"
                + "  public abstract short f();"
                + "  public abstract Short g();"
                + "  public abstract double h();"
                + "  public abstract Double i();"
                + "  public abstract float j();"
                + "  public abstract Float k();"
                + "  public abstract boolean l();"
                + "  public abstract Boolean m();"
                + "  public abstract byte[] n();"
                + "}\n"
        );

        JavaFileObject expected = JavaFileObjects.forSourceString("test.AutoValue_Test", ""
                + "package test;\n"
                + "\n"
                + "import android.database.Cursor;\n"
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
                + "  static AutoValue_Test createFromCursor(Cursor cursor) {\n"
                + "    String a = cursor.getString(cursor.getColumnIndexOrThrow(\"a\"));\n"
                + "    int b = cursor.getInt(cursor.getColumnIndexOrThrow(\"b\"));\n"
                + "    Integer c = cursor.getInt(cursor.getColumnIndexOrThrow(\"c\"));\n"
                + "    long d = cursor.getLong(cursor.getColumnIndexOrThrow(\"d\"));\n"
                + "    Long e = cursor.getLong(cursor.getColumnIndexOrThrow(\"e\"));\n"
                + "    short f = cursor.getShort(cursor.getColumnIndexOrThrow(\"f\"));\n"
                + "    Short g = cursor.getShort(cursor.getColumnIndexOrThrow(\"g\"));\n"
                + "    double h = cursor.getDouble(cursor.getColumnIndexOrThrow(\"h\"));\n"
                + "    Double i = cursor.getDouble(cursor.getColumnIndexOrThrow(\"i\"));\n"
                + "    float j = cursor.getFloat(cursor.getColumnIndexOrThrow(\"j\"));\n"
                + "    Float k = cursor.getFloat(cursor.getColumnIndexOrThrow(\"k\"));\n"
                + "    boolean l = cursor.getInt(cursor.getColumnIndexOrThrow(\"l\")) == 1;\n"
                + "    Boolean m = cursor.getInt(cursor.getColumnIndexOrThrow(\"m\")) == 1;\n"
                + "    byte[] n = cursor.getBlob(cursor.getColumnIndexOrThrow(\"n\"));\n"
                + "    return new AutoValue_Test(a, b, c, d, e, f, g, h, i, j, k, l, m, n);\n"
                + "  }\n"
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
                + "import android.database.Cursor;\n"
                + "public class Foo {\n"
                + "  private Foo(String data) {\n"
                + "  }\n"
                + "  public static Foo createFromCursor(Cursor cursor) {\n"
                + "    return new Foo(cursor.getString(cursor.getColumnIndexOrThrow(\"foo_column\")));\n"
                + "  }\n"
                + "}"
        );
        JavaFileObject source = JavaFileObjects.forSourceString("test.Test", ""
                + "package test;\n"
                + "import com.gabrielittner.auto.value.cursor.ColumnName;\n"
                + "import com.gabrielittner.auto.value.cursor.CursorAdapter;\n"
                + "import com.google.auto.value.AutoValue;\n"
                + "import javax.annotation.Nullable;\n"
                + "import android.database.Cursor;\n"
                + "@AutoValue public abstract class Test {\n"
                + "  public static Test blah(Cursor cursor) { return null; }\n"
                + "  @CursorAdapter(FooFactory.class) public abstract Foo foo();\n"
                + "}\n"
        );
        JavaFileObject fooFactorySource = JavaFileObjects.forSourceString("test.FooFactory", ""
                + "package test;\n"
                + "\n"
                + "import android.database.Cursor;\n"
                + "\n"
                + "public class FooFactory {\n"
                + "  public static Foo createFromCursor(Cursor cursor) { "
                + "    return Foo.createFromCursor(cursor);\n"
                + "  }\n"
                + "}\n"
        );
        JavaFileObject expected = JavaFileObjects.forSourceString("test.AutoValue_Test", ""
                + "package test;\n"
                + "\n"
                + "import android.database.Cursor;\n"
                + "\n"
                + "final class AutoValue_Test extends $AutoValue_Test {\n"
                + "  AutoValue_Test(Foo foo) {\n"
                + "    super(foo);\n"
                + "  }\n"
                + "\n"
                + "  static AutoValue_Test createFromCursor(Cursor cursor) {\n"
                + "    Foo foo = FooFactory.createFromCursor(cursor);\n"
                + "    return new AutoValue_Test(foo);\n"
                + "  }\n"
                + "}"
        );

        assertAbout(javaSources()).that(Arrays.asList(fooClass, fooFactorySource, source))
                .processedWith(new AutoValueProcessor())
                .compilesWithoutError()
                .and()
                .generatesSources(expected);
    }

    @Test public void unsupportedCursorFactory() {
        JavaFileObject fooClass = JavaFileObjects.forSourceString("test.Foo", ""
                + "package test;\n"
                + "import android.database.Cursor;\n"
                + "public class Foo {\n"
                + "  private Foo(String data) {\n"
                + "  }\n"
                + "  public static Foo create(Cursor cursor) {\n"
                + "    return new Foo(cursor.getString(cursor.getColumnIndexOrThrow(\"foo_column\")));\n"
                + "  }\n"
                + "}"
        );
        JavaFileObject source = JavaFileObjects.forSourceString("test.Test", ""
                + "package test;\n"
                + "import com.gabrielittner.auto.value.cursor.ColumnName;\n"
                + "import com.gabrielittner.auto.value.cursor.CursorAdapter;\n"
                + "import com.google.auto.value.AutoValue;\n"
                + "import javax.annotation.Nullable;\n"
                + "import android.database.Cursor;\n"
                + "@AutoValue public abstract class Test {\n"
                + "  public static Test blah(Cursor cursor) { return null; }\n"
                + "  @CursorAdapter(FooFactory.class) public abstract Foo foo();\n"
                + "}\n"
        );
        JavaFileObject fooFactorySource = JavaFileObjects.forSourceString("test.FooFactory", ""
                + "package test;\n"
                + "\n"
                + "import android.database.Cursor;\n"
                + "\n"
                + "public class FooFactory {\n"
                + "}\n"
        );

        assertAbout(javaSources()).that(Arrays.asList(fooClass, fooFactorySource, source))
                .processedWith(new AutoValueProcessor())
                .failsToCompile()
                .withErrorContaining("Class \"FooFactory\" needs to define a public static method "
                        + "taking a \"Cursor\" and returning \"test.Foo\"");
    }

    @Test public void contentValues() {
        JavaFileObject source = JavaFileObjects.forSourceString("test.Test", ""
                + "package test;\n"
                + "import com.google.auto.value.AutoValue;\n"
                + "import android.content.ContentValues;\n"
                + "import android.database.Cursor;\n"
                + "@AutoValue public abstract class Test {\n"
                + "  public static Test blah(Cursor cursor) { return null; }\n"
                + "  public abstract int a();"
                + "  public abstract String b();"
                + "  public abstract ContentValues toContentValues();"
                + "}\n"
        );

        JavaFileObject expected = JavaFileObjects.forSourceString("test.AutoValue_Test", ""
                + "package test;\n"
                + "\n"
                + "import android.content.ContentValues;\n"
                + "import android.database.Cursor;\n"
                + "import java.lang.String;\n"
                + "\n"
                + "final class AutoValue_Test extends $AutoValue_Test {\n"
                + "  AutoValue_Test(int a, String b) {\n"
                + "    super(a, b);\n"
                + "  }\n"
                + "\n"
                + "  static AutoValue_Test createFromCursor(Cursor cursor) {\n"
                + "    int a = cursor.getInt(cursor.getColumnIndexOrThrow(\"a\"));\n"
                + "    String b = cursor.getString(cursor.getColumnIndexOrThrow(\"b\"));\n"
                + "    return new AutoValue_Test(a, b);\n"
                + "  }\n"
                + "\n"
                + "  public ContentValues toContentValues() {\n"
                + "    ContentValues values = new ContentValues(2);"
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

    @Test public void contentValuesColumnName() {
        JavaFileObject source = JavaFileObjects.forSourceString("test.Test", ""
                + "package test;\n"
                + "import com.gabrielittner.auto.value.cursor.ColumnName;\n"
                + "import com.google.auto.value.AutoValue;\n"
                + "import android.content.ContentValues;\n"
                + "import android.database.Cursor;\n"
                + "@AutoValue public abstract class Test {\n"
                + "  public static Test blah(Cursor cursor) { return null; }\n"
                + "  public abstract int a();"
                + "  @ColumnName(\"column_b\") public abstract String b();"
                + "  public abstract ContentValues toContentValues();"
                + "}\n"
        );

        JavaFileObject expected = JavaFileObjects.forSourceString("test.AutoValue_Test", ""
                + "package test;\n"
                + "\n"
                + "import android.content.ContentValues;\n"
                + "import android.database.Cursor;\n"
                + "import java.lang.String;\n"
                + "\n"
                + "final class AutoValue_Test extends $AutoValue_Test {\n"
                + "  AutoValue_Test(int a, String b) {\n"
                + "    super(a, b);\n"
                + "  }\n"
                + "\n"
                + "  static AutoValue_Test createFromCursor(Cursor cursor) {\n"
                + "    int a = cursor.getInt(cursor.getColumnIndexOrThrow(\"a\"));\n"
                + "    String b = cursor.getString(cursor.getColumnIndexOrThrow(\"column_b\"));\n"
                + "    return new AutoValue_Test(a, b);\n"
                + "  }\n"
                + "\n"
                + "  public ContentValues toContentValues() {\n"
                + "    ContentValues values = new ContentValues(2);"
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

    @Test public void unsupportedColumnName() {
        JavaFileObject source = JavaFileObjects.forSourceString("test.Test", ""
                + "package test;\n"
                + "import com.google.auto.value.AutoValue;\n"
                + "import android.content.ContentValues;\n"
                + "import android.database.Cursor;\n"
                + "import javax.annotation.Nullable;\n"
                + "@AutoValue public abstract class Test {\n"
                + "  public static Test blah(Cursor cursor) { return null; }\n"
                + "  @Nullable public abstract int[] a();"
                + "  public abstract String b();"
                + "  public abstract ContentValues toContentValues();"
                + "}\n"
        );

        assertAbout(javaSources())
                .that(Collections.singletonList(source))
                .processedWith(new AutoValueProcessor())
                .failsToCompile()
                .withErrorContaining("Property \"a\" has type \"int[]\" that can't be put"
                        + " into ContentValues.");
    }

    @Test public void allContentValueTypes() {
        JavaFileObject source = JavaFileObjects.forSourceString("test.Test", ""
                + "package test;\n"
                + "import com.google.auto.value.AutoValue;\n"
                + "import android.content.ContentValues;\n"
                + "import android.database.Cursor;\n"
                + "@AutoValue public abstract class Test {\n"
                + "  public static Test blah(Cursor cursor) { return null; }\n"
                + "  public abstract String a();"
                + "  public abstract int b();"
                + "  public abstract Integer c();"
                + "  public abstract long d();"
                + "  public abstract Long e();"
                + "  public abstract short f();"
                + "  public abstract Short g();"
                + "  public abstract double h();"
                + "  public abstract Double i();"
                + "  public abstract float j();"
                + "  public abstract Float k();"
                + "  public abstract boolean l();"
                + "  public abstract Boolean m();"
                + "  public abstract byte[] n();"
                + "  public abstract ContentValues contentValues();"
                + "}\n"
        );

        JavaFileObject expected = JavaFileObjects.forSourceString("test.AutoValue_Test", ""
                + "package test;\n"
                + "\n"
                + "import android.content.ContentValues;\n"
                + "import android.database.Cursor;\n"
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
                + "  static AutoValue_Test createFromCursor(Cursor cursor) {\n"
                + "    String a = cursor.getString(cursor.getColumnIndexOrThrow(\"a\"));\n"
                + "    int b = cursor.getInt(cursor.getColumnIndexOrThrow(\"b\"));\n"
                + "    Integer c = cursor.getInt(cursor.getColumnIndexOrThrow(\"c\"));\n"
                + "    long d = cursor.getLong(cursor.getColumnIndexOrThrow(\"d\"));\n"
                + "    Long e = cursor.getLong(cursor.getColumnIndexOrThrow(\"e\"));\n"
                + "    short f = cursor.getShort(cursor.getColumnIndexOrThrow(\"f\"));\n"
                + "    Short g = cursor.getShort(cursor.getColumnIndexOrThrow(\"g\"));\n"
                + "    double h = cursor.getDouble(cursor.getColumnIndexOrThrow(\"h\"));\n"
                + "    Double i = cursor.getDouble(cursor.getColumnIndexOrThrow(\"i\"));\n"
                + "    float j = cursor.getFloat(cursor.getColumnIndexOrThrow(\"j\"));\n"
                + "    Float k = cursor.getFloat(cursor.getColumnIndexOrThrow(\"k\"));\n"
                + "    boolean l = cursor.getInt(cursor.getColumnIndexOrThrow(\"l\")) == 1;\n"
                + "    Boolean m = cursor.getInt(cursor.getColumnIndexOrThrow(\"m\")) == 1;\n"
                + "    byte[] n = cursor.getBlob(cursor.getColumnIndexOrThrow(\"n\"));\n"
                + "    return new AutoValue_Test(a, b, c, d, e, f, g, h, i, j, k, l, m, n);\n"
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
}
