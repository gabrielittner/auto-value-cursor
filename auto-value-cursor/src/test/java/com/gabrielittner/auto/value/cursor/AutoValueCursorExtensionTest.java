package com.gabrielittner.auto.value.cursor;

import com.google.auto.value.processor.AutoValueProcessor;
import com.google.testing.compile.JavaFileObjects;
import java.util.Collections;
import javax.tools.JavaFileObject;
import org.junit.Test;

import static com.google.common.truth.Truth.assertAbout;
import static com.google.testing.compile.JavaSourcesSubjectFactory.javaSources;

public class AutoValueCursorExtensionTest {

    @Test public void simple() {
        simpleTypeTest("", "String", "cursor.getString(cursor.getColumnIndexOrThrow(\"a\");");
    }

    @Test public void simpleWithNullable() {
        simpleTypeTest("@Nullable ", "String", "cursor.getString(cursor.getColumnIndexOrThrow(\"a\");");
    }

    @Test public void simpleWithColumnName() {
        simpleTypeTest("@ColumnName(\"column_a\") ", "String", "cursor.getString(cursor.getColumnIndexOrThrow(\"column_a\");");
    }

    @Test public void simpleWithColumnNameAndNullable() {
        simpleTypeTest("@ColumnName(\"column_a\") @Nullable ", "String",
                "cursor.getString(cursor.getColumnIndexOrThrow(\"column_a\");");
    }

    @Test public void unsupported() {
        JavaFileObject source = getSource("public abstract int[] a();\n");

        assertAbout(javaSources())
                .that(Collections.singletonList(source))
                .processedWith(new AutoValueProcessor())
                .failsToCompile();
    }

    @Test public void unsupportedWithNullable() {
        simpleTypeTest("@Nullable ", "int[]", "null; // type can't be read from cursor");
    }

    @Test public void unsupportedWithColumnName() {
        JavaFileObject source = getSource("@ColumnName(\"column_i\") public abstract int[] a();\n");

        assertAbout(javaSources())
                .that(Collections.singletonList(source))
                .processedWith(new AutoValueProcessor())
                .failsToCompile();
    }

    @Test public void unsupportedWithColumnNameAndNullable() {
        simpleTypeTest("@Nullable @ColumnName(\"column_i\")", "int[]", "null; // type can't be read from cursor");
    }

    @Test public void types() {
        String[] types = {"byte[]", "double", "float", "int", "long", "short", "String", "boolean",
                "byte[]", "double", "float", "int", "long", "short", "Boolean"};
        String[] gets = {"Blob", "Double", "Float", "Int", "Long", "Short", "String", "Int",
                "Blob", "Double", "Float", "Int", "Long", "Short", "Int"};
        String[] suffixe = {"", "", "", "", "", "", "", " == 1", "", "", "", "", "", "", " == 1" };
        for (int i = 0; i < types.length; i++) {
            String type = types[i];
            String get = gets[i];
            String suffix = suffixe[i];
            simpleTypeTest("", type, "cursor.get" + get + "(cursor.getColumnIndexOrThrow(\"a\"))" + suffix + ";");
            simpleTypeTest("@Nullable ", type, "cursor.get" + get + "(cursor.getColumnIndexOrThrow(\"a\"))" + suffix + ";");
            simpleTypeTest("@ColumnName(\"column_a\") ", type, "cursor.get" + get + "(cursor.getColumnIndexOrThrow(\"column_a\"))" + suffix + ";");
            simpleTypeTest("@ColumnName(\"column_a\") @Nullable ", type, "cursor.get" + get + "(cursor.getColumnIndexOrThrow(\"column_a\"))" + suffix + ";");
        }
    }

    private static void simpleTypeTest(String annotation, String type, String cursorGet) {
        String fields = annotation + "public abstract " + type + " a();\n";
        JavaFileObject source = getSource(fields);

        String constructorArgs = type + " a";
        String constructorSuperArgs = "a";
        String mapping = type + " a = " + cursorGet;
        JavaFileObject expected = getExpected(constructorArgs, constructorSuperArgs, mapping);

        assertAbout(javaSources()).that(Collections.singletonList(source))
                .processedWith(new AutoValueProcessor())
                .compilesWithoutError()
                .and()
                .generatesSources(expected);
    }

    private static JavaFileObject getSource(String fields) {
        return JavaFileObjects.forSourceString("test.Test", ""
                        + "package test;\n"
                        + "import com.gabrielittner.auto.value.cursor.ColumnName;\n"
                        + "import com.google.auto.value.AutoValue;\n"
                        + "import javax.annotation.Nullable;\n"
                        + "import android.database.Cursor;\n"
                        + "@AutoValue public abstract class Test {\n"
                        + "  public static Test blah(Cursor cursor) { return null; }\n"
                        + "  " + fields
                        + "}\n"
        );
    }

    private static JavaFileObject getExpected(String constructorArgs, String constructorSuperArgs,
            String mapping) {
        return JavaFileObjects.forSourceString("test/AutoValue_Test", ""
                        + "package test;\n"
                        + "\n"
                        + "import android.database.Cursor;\n"
                        + "\n"
                        + "final class AutoValue_Test extends $AutoValue_Test {\n"
                        + "  AutoValue_Test(" + constructorArgs + ") {\n"
                        + "    super(" + constructorSuperArgs + ");\n"
                        + "  }\n"
                        + "\n"
                        + "  static AutoValue_Test createFromCursor(Cursor cursor) {\n"
                        + "    " + mapping + "\n"
                        + "    return new AutoValue_Test(" + constructorSuperArgs + ");\n"
                        + "  }\n"
                        + "}"
        );
    };
}
