package com.gabrielittner.auto.value;

import com.gabrielittner.auto.value.cursor.ColumnAdapter;
import com.gabrielittner.auto.value.cursor.ColumnName;
import com.gabrielittner.auto.value.util.Property;
import com.google.auto.value.extension.AutoValueExtension;
import com.google.common.collect.ImmutableList;
import com.squareup.javapoet.TypeName;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.type.TypeMirror;

import static com.gabrielittner.auto.value.util.ElementUtil.getAnnotationValue;

public final class ColumnProperty extends Property {

    public static ImmutableList<ColumnProperty> from(AutoValueExtension.Context context) {
        ImmutableList.Builder<ColumnProperty> values = ImmutableList.builder();
        for (Map.Entry<String, ExecutableElement> entry : context.properties().entrySet()) {
            values.add(new ColumnProperty(entry.getKey(), entry.getValue()));
        }
        return values.build();
    }

    private static final List<TypeName> SUPPORTED_TYPES =
            Arrays.asList(
                    TypeName.get(String.class),
                    TypeName.get(byte[].class),
                    TypeName.get(Byte[].class),
                    TypeName.DOUBLE,
                    TypeName.DOUBLE.box(),
                    TypeName.FLOAT,
                    TypeName.FLOAT.box(),
                    TypeName.INT,
                    TypeName.INT.box(),
                    TypeName.LONG,
                    TypeName.LONG.box(),
                    TypeName.SHORT,
                    TypeName.SHORT.box(),
                    TypeName.BOOLEAN,
                    TypeName.BOOLEAN.box());

    private final String columnName;
    private final boolean supportedType;

    private ColumnProperty(String humanName, ExecutableElement element) {
        super(humanName, element);
        columnName = (String) getAnnotationValue(element, ColumnName.class, "value");
        supportedType = SUPPORTED_TYPES.contains(type());
    }

    public boolean supportedType() {
        return supportedType;
    }

    public String columnName() {
        return columnName != null ? columnName : humanName();
    }

    public TypeMirror columnAdapter() {
        return (TypeMirror) getAnnotationValue(element(), ColumnAdapter.class, "value");
    }

    public String cursorMethod() {
        if (!supportedType) {
            return null;
        }
        TypeName type = type();
        if (type.equals(TypeName.get(byte[].class)) || type.equals(TypeName.get(Byte[].class))) {
            return "cursor.getBlob($L)";
        }
        if (type.equals(TypeName.DOUBLE) || type.equals(TypeName.DOUBLE.box())) {
            return "cursor.getDouble($L)";
        }
        if (type.equals(TypeName.FLOAT) || type.equals(TypeName.FLOAT.box())) {
            return "cursor.getFloat($L)";
        }
        if (type.equals(TypeName.INT) || type.equals(TypeName.INT.box())) {
            return "cursor.getInt($L)";
        }
        if (type.equals(TypeName.LONG) || type.equals(TypeName.LONG.box())) {
            return "cursor.getLong($L)";
        }
        if (type.equals(TypeName.SHORT) || type.equals(TypeName.SHORT.box())) {
            return "cursor.getShort($L)";
        }
        if (type.equals(TypeName.get(String.class))) {
            return "cursor.getString($L)";
        }
        if (type.equals(TypeName.BOOLEAN) || type.equals(TypeName.BOOLEAN.box())) {
            return "cursor.getInt($L) == 1";
        }
        throw new AssertionError(
                String.format("supportedType is true but type %s isn't handled", type));
    }
}
