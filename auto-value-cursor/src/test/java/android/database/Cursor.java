/*
 * Copyright (C) 2006 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package android.database;

import java.io.Closeable;

/**
 * This interface provides random read-write access to the result set returned
 * by a database query.
 * <p>
 * Cursor implementations are not required to be synchronized so code using a Cursor from multiple
 * threads should perform its own synchronization when using the Cursor.
 * </p>
 */
public interface Cursor extends Closeable {
    /**
     * Returns the zero-based index for the given column name, or -1 if the column doesn't exist.
     * If you expect the column to exist use {@link #getColumnIndexOrThrow(String)} instead, which
     * will make the error more clear.
     *
     * @param columnName the name of the target column.
     * @return the zero-based column index for the given column name, or -1 if
     * the column name does not exist.
     * @see #getColumnIndexOrThrow(String)
     */
    int getColumnIndex(String columnName);
    /**
     * Returns the zero-based index for the given column name, or throws
     * {@link IllegalArgumentException} if the column doesn't exist. If you're not sure if
     * a column will exist or not use {@link #getColumnIndex(String)} and check for -1, which
     * is more efficient than catching the exceptions.
     *
     * @param columnName the name of the target column.
     * @return the zero-based column index for the given column name
     * @see #getColumnIndex(String)
     * @throws IllegalArgumentException if the column does not exist
     */
    int getColumnIndexOrThrow(String columnName) throws IllegalArgumentException;
    
    /**
     * Returns the value of the requested column as a byte array.
     *
     * <p>The result and whether this method throws an exception when the
     * column value is null or the column type is not a blob type is
     * implementation-defined.
     *
     * @param columnIndex the zero-based index of the target column.
     * @return the value of that column as a byte array.
     */
    byte[] getBlob(int columnIndex);
    /**
     * Returns the value of the requested column as a String.
     *
     * <p>The result and whether this method throws an exception when the
     * column value is null or the column type is not a string type is
     * implementation-defined.
     *
     * @param columnIndex the zero-based index of the target column.
     * @return the value of that column as a String.
     */
    String getString(int columnIndex);

    /**
     * Returns the value of the requested column as a short.
     *
     * <p>The result and whether this method throws an exception when the
     * column value is null, the column type is not an integral type, or the
     * integer value is outside the range [<code>Short.MIN_VALUE</code>,
     * <code>Short.MAX_VALUE</code>] is implementation-defined.
     *
     * @param columnIndex the zero-based index of the target column.
     * @return the value of that column as a short.
     */
    short getShort(int columnIndex);
    /**
     * Returns the value of the requested column as an int.
     *
     * <p>The result and whether this method throws an exception when the
     * column value is null, the column type is not an integral type, or the
     * integer value is outside the range [<code>Integer.MIN_VALUE</code>,
     * <code>Integer.MAX_VALUE</code>] is implementation-defined.
     *
     * @param columnIndex the zero-based index of the target column.
     * @return the value of that column as an int.
     */
    int getInt(int columnIndex);
    /**
     * Returns the value of the requested column as a long.
     *
     * <p>The result and whether this method throws an exception when the
     * column value is null, the column type is not an integral type, or the
     * integer value is outside the range [<code>Long.MIN_VALUE</code>,
     * <code>Long.MAX_VALUE</code>] is implementation-defined.
     *
     * @param columnIndex the zero-based index of the target column.
     * @return the value of that column as a long.
     */
    long getLong(int columnIndex);
    /**
     * Returns the value of the requested column as a float.
     *
     * <p>The result and whether this method throws an exception when the
     * column value is null, the column type is not a floating-point type, or the
     * floating-point value is not representable as a <code>float</code> value is
     * implementation-defined.
     *
     * @param columnIndex the zero-based index of the target column.
     * @return the value of that column as a float.
     */
    float getFloat(int columnIndex);
    /**
     * Returns the value of the requested column as a double.
     *
     * <p>The result and whether this method throws an exception when the
     * column value is null, the column type is not a floating-point type, or the
     * floating-point value is not representable as a <code>double</code> value is
     * implementation-defined.
     *
     * @param columnIndex the zero-based index of the target column.
     * @return the value of that column as a double.
     */
    double getDouble(int columnIndex);

    boolean isNull (int columnIndex);
}
