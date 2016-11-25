Change Log
==========

Version 1.0.1 *(2016-11-25)*
----------------------------

#### Supports: AutoValue 1.3

- `@Nullable` properties don't require their column to be present

Version 1.0.0 *(2016-09-09)*
----------------------------

#### Supports: AutoValue 1.3

- support for AutoValue 1.3

Version 1.0.0-rc1 *(2016-06-13)*
----------------------------

#### Supports: AutoValue 1.3-rc1

- support for AutoValue 1.3-rc1

Version 0.5.0 *(2016-06-02)*
----------------------------

#### Supports: AutoValue 1.2

- new `@ColumnAdapter` annotation used in combination with `ColumnTypeAdapter`
    - unified annotation/adapter for `Cursor` and `ContentValues`
    - allows to reuse your custom type adapters (e.g. a DateAdapter)
- BREAKING: removed `@CursorAdapter` and `@ValuesAdapter`
- added support for AutoValue classes that use `get` and `is` prefixes in properties
- removed dependency on AutoService

Version 0.4.0 *(2016-04-17)*
----------------------------

#### Supports: AutoValue 1.2

- added `@ValuesAdapter` to support custom type mapping for `ContentValues`
- when a column is `null` and the property is annotated with `@Nullable` set the property to null instead of using the cursor's default value

Version 0.3.1 *(2016-04-03)*
----------------------------

- fix opt-in using static method returning Func1


Version 0.3.0 *(2016-04-02)*
----------------------------

- optionally generate a `toContentValues()` method


Version 0.2.0 *(2016-03-22)*
----------------------------

Initial release. Only guaranteed to support AutoValue 1.2-rc1.
