package com.gabrielittner.auto.value;

import com.gabrielitter.auto.value.contentvalues.ValuesAdapter;
import com.gabrielittner.auto.value.cursor.ColumnName;
import com.gabrielittner.auto.value.cursor.CursorAdapter;
import com.gabrielittner.auto.value.util.Property;
import com.google.auto.value.extension.AutoValueExtension;
import com.google.common.collect.ImmutableList;
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

    private final String columnName;

    public ColumnProperty(String humanName, ExecutableElement element) {
        super(humanName, element);
        columnName = (String) getAnnotationValue(element, ColumnName.class, "value");
    }

    public String columnName() {
        return columnName != null ? columnName : humanName();
    }

    public TypeMirror cursorAdapter() {
        return (TypeMirror) getAnnotationValue(element(), CursorAdapter.class, "value");
    }

    public TypeMirror valuesAdapter() {
        return (TypeMirror) getAnnotationValue(element(), ValuesAdapter.class, "value");
    }
}
