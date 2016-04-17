Change Log
==========

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
