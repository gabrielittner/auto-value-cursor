# AutoValue: Cursor Extension

[![Build Status](https://travis-ci.org/gabrielittner/auto-value-cursor.svg?branch=master)](https://travis-ci.org/gabrielittner/auto-value-cursor)

An extension for Google's [AutoValue](https://github.com/google/auto) that generates a `createFromCursor(Cursor c)` method for AutoValue annotated objects.

**Note**: This is an early version that requires the extension support currently in AutoValue 1.2-SNAPSHOT.

## Usage

Simply include auto-value-cursor in your project and annotate your properties using `@ColumnName` to define an alternate column name.

```java
import com.gabrielittner.auto.value.cursor.ColumnName;

@AutoValue public abstract class Foo {
  abstract String bar();
  @ColumnName("foo_baz") abstract String baz();

  public static Foo create(Cursor cursor) {
    return AutoValue_Foo.createFromCursor(cursor);
  }
}
```

**Note**: Right now `createFromCursor()` is oly generated when at least one method is annotated with `@ColumnName`.

Now build your project and create your Foo from a Cursor.

## Download

Add a Gradle dependency:

```groovy
compile 'com.gabrielittner.auto.cursor:auto-value-cursor:0.1-SNAPSHOT'
```

## License

This project is heavily based on [Ryan Harter](https://github.com/rharter/)'s [auto-value-gson](https://github.com/rharter/auto-value-gson)

```
Copyright 2015 Ryan Harter.
Copyright 2015 Gabriel Ittner.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
