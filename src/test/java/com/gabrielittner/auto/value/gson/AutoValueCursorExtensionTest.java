package com.gabrielittner.auto.value.gson;

import com.google.auto.value.processor.AutoValueProcessor;
import com.google.testing.compile.JavaFileObjects;
import java.util.Collections;
import org.junit.Test;

import javax.tools.JavaFileObject;

import static com.google.common.truth.Truth.assertAbout;
import static com.google.testing.compile.JavaSourcesSubjectFactory.javaSources;

public class AutoValueCursorExtensionTest {

  @Test public void simple() {
    JavaFileObject source = JavaFileObjects.forSourceString("test.Test", ""
            + "package test;\n"
            + "import com.gabrielittner.auto.value.cursor.ColumnName;\n"
            + "import com.google.auto.value.AutoValue;\n"
            + "import javax.annotation.Nullable;\n"
            + "@AutoValue public abstract class Test {\n"
            // byte[] type
            + "public abstract byte[] a();\n"
            // double type
            + "public abstract double b();\n"
            // float type
            + "public abstract float c();\n"
            // int type
            + "public abstract int d();\n"
            // long type
            + "public abstract long e();\n"
            // short type
            + "public abstract short f();\n"
            // boolean type
            + "public abstract boolean g();\n"
            // String type
            + "public abstract String h();\n"
            // ColumnName
            + "@ColumnName(\"column_i\") public abstract String i();\n"
            // Nullable unsupported value
            + "@Nullable public abstract int[] j();\n"
            + "}\n"
    );

    JavaFileObject expected = JavaFileObjects.forSourceString("test/AutoValue_Test", ""
            + "package test;\n"
            + "\n"
            + "import android.database.Cursor;\n"
            + "import java.lang.String;\n"
            + "\n"
            + "final class AutoValue_Test extends $AutoValue_Test {\n"
            + "  AutoValue_Test(byte[] a, double b, float c, int d, long e, short f, boolean g, String h, String i, int[] j) {\n"
            + "    super(a, b, c, d, e, f, g, h, i, j);\n"
            + "  }\n"
            + "\n"
            + "  static Test createFromCursor(Cursor cursor) {\n"
            + "    byte[] a = cursor.getBlob(cursor.getColumnIndexOrThrow(\"a\"));\n"
            + "    double b = cursor.getDouble(cursor.getColumnIndexOrThrow(\"b\"));\n"
            + "    float c = cursor.getFloat(cursor.getColumnIndexOrThrow(\"c\"));\n"
            + "    int d = cursor.getInt(cursor.getColumnIndexOrThrow(\"d\"));\n"
            + "    long e = cursor.getLong(cursor.getColumnIndexOrThrow(\"e\"));\n"
            + "    short f = cursor.getShort(cursor.getColumnIndexOrThrow(\"f\"));\n"
            + "    boolean g = cursor.getInt(cursor.getColumnIndexOrThrow(\"g\")) == 1;\n"
            + "    String h = cursor.getString(cursor.getColumnIndexOrThrow(\"h\"));\n"
            + "    String i = cursor.getString(cursor.getColumnIndexOrThrow(\"column_i\"));\n"
            + "    int[] j = null;\n"
            + "    return new AutoValue_Test(a, b, c, d, e, f, g, h, i, j);\n"
            + "  }\n"
            + "}"
    );

    assertAbout(javaSources())
        .that(Collections.singletonList(source))
        .processedWith(new AutoValueProcessor())
        .compilesWithoutError()
        .and()
        .generatesSources(expected);
  }

  @Test public void failTest() {
    JavaFileObject source = JavaFileObjects.forSourceString("test.Test", ""
            + "package test;\n"
            + "import com.gabrielittner.auto.value.cursor.ColumnName;\n"
            + "import com.google.auto.value.AutoValue;\n"
            + "@AutoValue public abstract class Test {\n"
            // byte[] type
            + "public abstract int[] a();\n"
            // ColumnName
            + "@ColumnName(\"column_i\") public abstract String b();\n"
            + "}\n"
    );

    assertAbout(javaSources())
            .that(Collections.singletonList(source))
            .processedWith(new AutoValueProcessor())
            .failsToCompile();
}
}
