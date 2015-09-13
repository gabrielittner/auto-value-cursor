# AutoValue: Cursor Extension

An extension for Google's [AutoValue][auto] that generates a `createFromCursor(Cursor c)` method for AutoValue annotated objects.

**Note**: This is an early version that requires the extension support currently in AutoValue 1.2-SNAPSHOT.

## Usage

Include auto-value-cursor in your project and add a static factory method to your auto-value object.

```java
import com.gabrielittner.auto.value.cursor.ColumnName;

@AutoValue public abstract class User {
  abstract String name();
  @ColumnName("email_address") abstract String email();

  public static Foo create(Cursor cursor) {
    return AutoValue_Foo.createFromCursor(cursor);
  }
}
```

## Download

Add a Gradle dependency:

```groovy
provided 'com.gabrielittner.auto.cursor:auto-value-cursor:0.2-SNAPSHOT'
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
 [apt]: https://bitbucket.org/hvisser/android-apt
 [ryan]: https://github.com/rharter/
 [auto-gson]: https://github.com/rharter/auto-value-gson

