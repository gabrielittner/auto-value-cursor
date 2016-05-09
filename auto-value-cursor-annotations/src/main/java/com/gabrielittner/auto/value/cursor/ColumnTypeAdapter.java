package com.gabrielittner.auto.value.cursor;

import android.content.ContentValues;
import android.database.Cursor;

public interface ColumnTypeAdapter<T> {
    T fromCursor(Cursor cursor, String columnName);
    void toContentValues(ContentValues values, String columnName, T value);
}
