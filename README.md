# AutoValue: Cursor Extension

An extension for Google's [AutoValue][auto] that generates a `createFromCursor(Cursor c)` method for AutoValue annotated objects.


## Usage

Include auto-value-cursor in your project and add a static factory method to your auto-value object.

```java
import com.gabrielittner.auto.value.cursor.ColumnName;

@AutoValue public abstract class User {
  abstract String id();
  abstract String name();
  // use the annotation if column name and field name aren't the same
  @ColumnName("email_address") abstract String email();

  public static User create(Cursor cursor) {
    return AutoValue_User.createFromCursor(cursor);
  }

  // Optional: if your project includes RxJava the extension will generate a Func1<Cursor, User>
  public static Func1<Cursor, User> MAPPER = AutoValue_User.MAPPER;

  // Optional: if your project includes RxJava 2 the extension will generate a Function<Cursor, User>
  public static Function<Cursor, User> MAPPER = AutoValue_User.MAPPER_FUNCTION;

  // Optional: When you include an abstract method that returns ContentValues and doesn't have
  // any parameters the extension will implement it for you
  abstract ContentValues toContentValues();
}
```

**Important:** The extension will only be applied when there is
- a static method that returns your value type (`User` in the example) and takes a `Cursor` as parameter
- and/or a static field of type `Func1<Cursor, YourValueType>`
- and/or a static field of type `Function<Cursor, YourValueType>`

## Custom types

The following types are supported by default:

 * `byte[]`
 * `double`/`Double`
 * `float`/`Float`
 * `int`/`Integer`
 * `long`/`Long`
 * `short`/`Short`
 * `String`
 * `boolean`/`Boolean`

For other types, you need to use the `@ColumnAdapter` annotation and specify a factory
class that implements the `ColumnTypeAdapter` interface.
When you need to map multiple columns to one custom type you can simply ignore the given
`columnName`. Eg.:

`User.java`:

```java
@AutoValue public abstract class User {
  abstract String id();
  abstract String name();
  @ColumnAdapter(AvatarAdapter.class) Avatar avatar();

  public static User createFromCursor(Cursor cursor) {
    return AutoValue_User.createFromCursor(cursor);
  }
}
```

`AvatarAdapter.java`:

```java
public class AvatarAdapter implements ColumnTypeAdapter<Avatar> {
  public Avatar fromCursor(Cursor cursor, String columnName) {
    String smallImageUrl = cursor.getString(cursor.getColumnIndex("small_image_url");
    String largeImageUrl = cursor.getString(cursor.getColumnIndex("large_image_url");
    return new Avatar(smallImageUrl, largeImageUrl);
  }
  public void toContentValues(ContentValues values, String columnName, Avatar value) {
    // leave this empty when you don't use a "toContentValues()" method
    values.putString("small_image_url", value.smallImageUrl);
    values.putString"large_image_url", value.largeImageUrl);
  }
}
```

`Avatar.java`:

```java
public class Avatar {
  final String smallImageUrl;
  final String largeImageUrl;

  public Avatar(String smallImageUrl, String largeImageUrl) {
    this.smallImageUrl = smallImageUrl;
    this.largeImageUrl = largeImageUrl;
  }
}
```

## Download

Add a Gradle dependency:

```groovy
annotationProcessor 'com.gabrielittner.auto.value:auto-value-cursor:2.0.1'
// if you need the @ColumnName or @ColumnAdapter annotations also include this:
implementation 'com.gabrielittner.auto.value:auto-value-cursor-annotations:2.0.1'
```

Snapshots of the development version are available in [Sonatype's `snapshots` repository][snap].

## License

This project is heavily based on [Ryan Harter][ryan]'s [auto-value-gson][auto-gson]

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



 [auto]: https://github.com/google/auto
 [snap]: https://oss.sonatype.org/content/repositories/snapshots/
 [ryan]: https://github.com/rharter/
 [auto-gson]: https://github.com/rharter/auto-value-gson
