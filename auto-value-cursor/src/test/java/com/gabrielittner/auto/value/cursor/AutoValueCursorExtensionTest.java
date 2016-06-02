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

    @Test
    public void simple() {
        JavaFileObject source = JavaFileObjects.forSourceString("test.Test", ""
                + "package test;\n"
                + "import com.google.auto.value.AutoValue;\n"
                + "import android.database.Cursor;\n"
                + "@AutoValue public abstract class Test {\n"
                + "  public static Test blah(Cursor cursor) { return null; }\n"
                + "  public abstract int a();\n"
                + "  public abstract String b();\n"
                + "}\n");

        JavaFileObject expected = JavaFileObjects.forSourceString("test.AutoValue_Test", ""
                + "package test;\n"
                + "import android.database.Cursor;\n"
                + "import java.lang.String;\n"
                + "final class AutoValue_Test extends $AutoValue_Test {\n"
                + "  AutoValue_Test(int a, String b) {\n"
                + "    super(a, b);\n"
                + "  }\n"
                + "  static AutoValue_Test createFromCursor(Cursor cursor) {\n"
                + "    int a = cursor.getInt(cursor.getColumnIndexOrThrow(\"a\"));\n"
                + "    String b = cursor.getString(cursor.getColumnIndexOrThrow(\"b\"));\n"
                + "    return new AutoValue_Test(a, b);\n"
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
                + "import android.database.Cursor;\n"
                + "@AutoValue public abstract class Test {\n"
                + "  public static Test blah(Cursor cursor) { return null; }\n"
                + "  public abstract int a();\n"
                + "  @ColumnName(\"column_b\") public abstract String b();\n"
                + "}\n");

        JavaFileObject expected = JavaFileObjects.forSourceString("test.AutoValue_Test", ""
                + "package test;\n"
                + "import android.database.Cursor;\n"
                + "import java.lang.String;\n"
                + "final class AutoValue_Test extends $AutoValue_Test {\n"
                + "  AutoValue_Test(int a, String b) {\n"
                + "    super(a, b);\n"
                + "  }\n"
                + "  static AutoValue_Test createFromCursor(Cursor cursor) {\n"
                + "    int a = cursor.getInt(cursor.getColumnIndexOrThrow(\"a\"));\n"
                + "    String b = cursor.getString(cursor.getColumnIndexOrThrow(\"column_b\"));\n"
                + "    return new AutoValue_Test(a, b);\n"
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
    public void nullable() {
        JavaFileObject source = JavaFileObjects.forSourceString("test.Test", ""
                + "package test;\n"
                + "import com.google.auto.value.AutoValue;\n"
                + "import android.database.Cursor;\n"
                + "import javax.annotation.Nullable;\n"
                + "@AutoValue public abstract class Test {\n"
                + "  public static Test blah(Cursor cursor) { return null; }\n"
                + "  public abstract int a();\n"
                + "  @Nullable public abstract String b();\n"
                + "}\n");

        JavaFileObject expected = JavaFileObjects.forSourceString("test.AutoValue_Test", ""
                + "package test;\n"
                + "import android.database.Cursor;\n"
                + "import java.lang.String;\n"
                + "final class AutoValue_Test extends $AutoValue_Test {\n"
                + "  AutoValue_Test(int a, String b) {\n"
                + "    super(a, b);\n"
                + "  }\n"
                + "  static AutoValue_Test createFromCursor(Cursor cursor) {\n"
                + "    int a = cursor.getInt(cursor.getColumnIndexOrThrow(\"a\"));\n"
                + "    int bColumnIndex = cursor.getColumnIndexOrThrow(\"b\");"
                + "    String b = cursor.isNull(bColumnIndex) ? null : cursor.getString(bColumnIndex);\n"
                + "    return new AutoValue_Test(a, b);\n"
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
    public void nullableColumnName() {
        JavaFileObject source = JavaFileObjects.forSourceString("test.Test", ""
                + "package test;\n"
                + "import com.gabrielittner.auto.value.cursor.ColumnName;\n"
                + "import com.google.auto.value.AutoValue;\n"
                + "import android.database.Cursor;\n"
                + "import javax.annotation.Nullable;\n"
                + "@AutoValue public abstract class Test {\n"
                + "  public static Test blah(Cursor cursor) { return null; }\n"
                + "  public abstract int a();\n"
                + "  @Nullable @ColumnName(\"column_b\") public abstract String b();\n"
                + "}\n");

        JavaFileObject expected = JavaFileObjects.forSourceString("test.AutoValue_Test", ""
                + "package test;\n"
                + "import android.database.Cursor;\n"
                + "import java.lang.String;\n"
                + "final class AutoValue_Test extends $AutoValue_Test {\n"
                + "  AutoValue_Test(int a, String b) {\n"
                + "    super(a, b);\n"
                + "  }\n"
                + "  static AutoValue_Test createFromCursor(Cursor cursor) {\n"
                + "    int a = cursor.getInt(cursor.getColumnIndexOrThrow(\"a\"));\n"
                + "    int bColumnIndex = cursor.getColumnIndexOrThrow(\"column_b\");"
                + "    String b = cursor.isNull(bColumnIndex) ? null : cursor.getString(bColumnIndex);\n"
                + "    return new AutoValue_Test(a, b);\n"
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
                + "import android.database.Cursor;\n"
                + "@AutoValue public abstract class Test {\n"
                + "  public static Test blah(Cursor cursor) { return null; }\n"
                + "  public abstract int[] a();\n"
                + "  public abstract String b();\n"
                + "}\n");

        assertAbout(javaSources())
                .that(Collections.singletonList(source))
                .processedWith(new AutoValueProcessor())
                .failsToCompile()
                .withErrorContaining("Property has type that can't be read from Cursor.");
    }

    @Test
    public void unsupportedWithNullable() {
        JavaFileObject source = JavaFileObjects.forSourceString("test.Test", ""
                + "package test;\n"
                + "import com.google.auto.value.AutoValue;\n"
                + "import android.database.Cursor;\n"
                + "import javax.annotation.Nullable;\n"
                + "@AutoValue public abstract class Test {\n"
                + "  public static Test blah(Cursor cursor) { return null; }\n"
                + "  @Nullable public abstract int[] a();\n"
                + "  public abstract String b();\n"
                + "}\n");

        JavaFileObject expected = JavaFileObjects.forSourceString("test.AutoValue_Test", ""
                + "package test;\n"
                + "import android.database.Cursor;\n"
                + "import java.lang.String;\n"
                + "final class AutoValue_Test extends $AutoValue_Test {\n"
                + "  AutoValue_Test(int[] a, String b) {\n"
                + "    super(a, b);\n"
                + "  }\n"
                + "  static AutoValue_Test createFromCursor(Cursor cursor) {\n"
                + "    int[] a = null; // can't be read from cursor\n"
                + "    String b = cursor.getString(cursor.getColumnIndexOrThrow(\"b\"));\n"
                + "    return new AutoValue_Test(a, b);\n"
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
    public void allCursorTypes() {
        JavaFileObject source = JavaFileObjects.forSourceString("test.Test", ""
                + "package test;\n"
                + "import com.google.auto.value.AutoValue;\n"
                + "import android.database.Cursor;\n"
                + "@AutoValue public abstract class Test {\n"
                + "  public static Test blah(Cursor cursor) { return null; }\n"
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
                + "}\n");

        JavaFileObject expected = JavaFileObjects.forSourceString("test.AutoValue_Test", ""
                + "package test;\n"
                + "import android.database.Cursor;\n"
                + "import java.lang.Boolean;\n"
                + "import java.lang.Double;\n"
                + "import java.lang.Float;\n"
                + "import java.lang.Integer;\n"
                + "import java.lang.Long;\n"
                + "import java.lang.Short;\n"
                + "import java.lang.String;\n"
                + "final class AutoValue_Test extends $AutoValue_Test {\n"
                + "  AutoValue_Test(String a, int b, Integer c, long d, Long e, short f, Short g, double h, Double i, float j, Float k, boolean l, Boolean m, byte[] n) {\n"
                + "    super(a, b, c, d, e, f, g, h, i, j, k, l, m, n);\n"
                + "  }\n"
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
                + "}\n");

        assertAbout(javaSources())
                .that(Collections.singletonList(source))
                .processedWith(new AutoValueProcessor())
                .compilesWithoutError()
                .and()
                .generatesSources(expected);
    }

    @Test
    public void cursorAdapter() {
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
                + "  }\n"
                + "}\n");
        JavaFileObject source = JavaFileObjects.forSourceString("test.Test", ""
                + "package test;\n"
                + "import com.gabrielittner.auto.value.cursor.ColumnName;\n"
                + "import com.gabrielittner.auto.value.cursor.ColumnAdapter;\n"
                + "import com.google.auto.value.AutoValue;\n"
                + "import javax.annotation.Nullable;\n"
                + "import android.database.Cursor;\n"
                + "@AutoValue public abstract class Test {\n"
                + "  public static Test blah(Cursor cursor) { return null; }\n"
                + "  @ColumnAdapter(FooAdapter.class) public abstract Foo foo();\n"
                + "  @ColumnAdapter(StringAdapter.class) public abstract String bar();\n"
                + "  @ColumnAdapter(StringAdapter.class) @ColumnName(\"column\") public abstract String columnName();\n"
                + "}\n");
        JavaFileObject expected = JavaFileObjects.forSourceString("test.AutoValue_Test", ""
                + "package test;\n"
                + "import android.database.Cursor;\n"
                + "import java.lang.String;\n"
                + "final class AutoValue_Test extends $AutoValue_Test {\n"
                + "  AutoValue_Test(Foo foo, String bar, String columnName) {\n"
                + "    super(foo, bar, columnName);\n"
                + "  }\n"
                + "  static AutoValue_Test createFromCursor(Cursor cursor) {\n"
                + "    FooAdapter fooAdapter = new FooAdapter();\n"
                + "    StringAdapter stringAdapter = new StringAdapter();\n"
                + "    Foo foo = fooAdapter.fromCursor(cursor, \"foo\");\n"
                + "    String bar = stringAdapter.fromCursor(cursor, \"bar\");\n"
                + "    String columnName = stringAdapter.fromCursor(cursor, \"column\");\n"
                + "    return new AutoValue_Test(foo, bar, columnName);\n"
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
    public void rxjava() {
        JavaFileObject source = JavaFileObjects.forSourceString("test.Test", ""
                + "package test;\n"
                + "import com.google.auto.value.AutoValue;\n"
                + "import android.database.Cursor;\n"
                + "@AutoValue public abstract class Test {\n"
                + "  public static Test blah(Cursor cursor) { return null; }\n"
                + "  public abstract int a();\n"
                + "  public abstract String b();\n"
                + "}\n");

        JavaFileObject expected = JavaFileObjects.forSourceString("test.AutoValue_Test", ""
                + "package test;\n"
                + "import android.database.Cursor;\n"
                + "import java.lang.Override;\n"
                + "import java.lang.String;\n"
                + "import rx.functions.Func1;\n"
                + "final class AutoValue_Test extends $AutoValue_Test {\n"
                + "  static final Func1<Cursor, Test> MAPPER = new Func1<Cursor, Test>() {\n"
                + "    @Override\n"
                + "    public AutoValue_Test call(Cursor c) {\n"
                + "      return createFromCursor(c);\n"
                + "    }\n"
                + "  };\n"
                + "  AutoValue_Test(int a, String b) {\n"
                + "    super(a, b);\n"
                + "  }\n"
                + "  static AutoValue_Test createFromCursor(Cursor cursor) {\n"
                + "    int a = cursor.getInt(cursor.getColumnIndexOrThrow(\"a\"));\n"
                + "    String b = cursor.getString(cursor.getColumnIndexOrThrow(\"b\"));\n"
                + "    return new AutoValue_Test(a, b);\n"
                + "  }\n"
                + "}\n");

        assertAbout(javaSources())
                .that(Arrays.asList(func1(), source))
                .processedWith(new AutoValueProcessor())
                .compilesWithoutError()
                .and()
                .generatesSources(expected);
    }

    @Test
    public void rxjavaOptIn() {
        JavaFileObject source = JavaFileObjects.forSourceString("test.Test", ""
                + "package test;\n"
                + "import com.google.auto.value.AutoValue;\n"
                + "import android.database.Cursor;\n"
                + "import rx.functions.Func1;\n"
                + "@AutoValue public abstract class Test {\n"
                + "  public static Func1<Cursor, Test> blahMap() { return null; }\n"
                + "  public abstract int a();\n"
                + "  public abstract String b();\n"
                + "}\n");

        JavaFileObject expected = JavaFileObjects.forSourceString("test.AutoValue_Test", ""
                + "package test;\n"
                + "import android.database.Cursor;\n"
                + "import java.lang.Override;\n"
                + "import java.lang.String;\n"
                + "import rx.functions.Func1;\n"
                + "final class AutoValue_Test extends $AutoValue_Test {\n"
                + "  static final Func1<Cursor, Test> MAPPER = new Func1<Cursor, Test>() {\n"
                + "    @Override\n"
                + "    public AutoValue_Test call(Cursor c) {\n"
                + "      return createFromCursor(c);\n"
                + "    }\n"
                + "  };\n"
                + "  AutoValue_Test(int a, String b) {\n"
                + "    super(a, b);\n"
                + "  }\n"
                + "  static AutoValue_Test createFromCursor(Cursor cursor) {\n"
                + "    int a = cursor.getInt(cursor.getColumnIndexOrThrow(\"a\"));\n"
                + "    String b = cursor.getString(cursor.getColumnIndexOrThrow(\"b\"));\n"
                + "    return new AutoValue_Test(a, b);\n"
                + "  }\n"
                + "}\n");

        assertAbout(javaSources())
                .that(Arrays.asList(func1(), source))
                .processedWith(new AutoValueProcessor())
                .compilesWithoutError()
                .and()
                .generatesSources(expected);
    }

    private JavaFileObject func1() {
        return JavaFileObjects.forSourceString(
                "rx.functions.Func1", ""
                        + "package rx.functions;\n"
                        + "public interface Func1<T, R> {\n"
                        + "  R call(T t);\n"
                        + "}\n");
    }

    @Test
    public void generatesNothingWithoutOptIn() {
        JavaFileObject source = JavaFileObjects.forSourceString("test.Test", ""
                + "package test;\n"
                + "import com.google.auto.value.AutoValue;\n"
                + "import android.database.Cursor;\n"
                + "@AutoValue public abstract class Test {\n"
                + "  public abstract int a();\n"
                + "  public abstract String b();\n"
                + "}\n");

        JavaFileObject expected = JavaFileObjects.forSourceString("test.AutoValue_Test", ""
                + "package test;\n"
                + "import javax.annotation.Generated;\n"
                + "@Generated(\"com.google.auto.value.processor.AutoValueProcessor\")\n"
                + "final class AutoValue_Test extends Test {\n"
                + "  private final int a;\n"
                + "  private final String b;\n"
                + "  AutoValue_Test(\n"
                + "      int a,\n"
                + "      String b) {\n"
                + "    this.a = a;\n"
                + "    if (b == null) {\n"
                + "      throw new NullPointerException(\"Null b\");\n"
                + "    }\n"
                + "    this.b = b;\n"
                + "  }\n"
                + "  @Override\n"
                + "  public int a() {\n"
                + "    return a;\n"
                + "  }\n"
                + "  @Override\n"
                + "  public String b() {\n"
                + "    return b;\n"
                + "  }\n"
                + "  @Override\n"
                + "  public String toString() {\n"
                + "    return \"Test{\"\n"
                + "        + \"a=\" + a + \", \"\n"
                + "        + \"b=\" + b\n"
                + "        + \"}\";\n"
                + "  }\n"
                + "  @Override\n"
                + "  public boolean equals(Object o) {\n"
                + "    if (o == this) {\n"
                + "      return true;\n"
                + "    }\n"
                + "    if (o instanceof Test) {\n"
                + "      Test that = (Test) o;\n"
                + "      return (this.a == that.a())\n"
                + "           && (this.b.equals(that.b()));\n"
                + "    }\n"
                + "    return false;\n"
                + "  }\n"
                + "  @Override\n"
                + "  public int hashCode() {\n"
                + "    int h = 1;\n"
                + "    h *= 1000003;\n"
                + "    h ^= this.a;\n"
                + "    h *= 1000003;\n"
                + "    h ^= this.b.hashCode();\n"
                + "    return h;\n"
                + "  }\n"
                + "}\n");

        assertAbout(javaSources())
                .that(Collections.singleton(source))
                .processedWith(new AutoValueProcessor())
                .compilesWithoutError()
                .and()
                .generatesSources(expected);
    }
}
