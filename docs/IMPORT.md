# Import settings

The CLI option `--import.files.location` support multiple locations of files. In general, all resource location supported by Springs RessourceLoader and
[PathMatchingResourcePatternResolver](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/core/io/support/PathMatchingResourcePatternResolver.html)
are supported. This includes remote locations and zip files as well.

## Ant-style path patterns.

Part of this mapping code has been kindly borrowed from [Apache Ant](https://ant.apache.org/).

The path patten using the following rules:

* `?` matches one character
* `*` matches zero or more characters
* `**` matches zero or more directories in a path
* `{label:regex}` matches regex pattern

## Examples

| Example                                        | Description                                                                                                                   |
|------------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------|
| `realm.json`                                   | realm.json from current work dir                                                                                              |
| `path/*.json`                                  | All files from directory `path` that ends with `.json`                                                                        |
| `path/realm_?.json`                            | All files from directory `path` that with name `realm_*.json`. `*` can be any single character.                               |
| `path/**/a*.json`                              | All files **recursively** from directory `path` that begins with `a` and ends with `.json`                                    |
| `path/{filename:[abc]+}.json`                  | All files from directory `path` that matches regex pattern `[abc]+`.                                                          |
| `https://example.com/realm.json`               | Load file from `https://example.com/realm.json`.                                                                              |
| `https://user:password@example.com/realm.json` | Load file from `https://example.com/realm.json` and authenticate with auth basic. Preemptive authentication is not supported. |
| `zip:file:path/file.zip!/*`                    | All files from zip archive `path/file.zip`                                                                                    |
| `zip:file:path/file.zip!/**/*.yaml`            | All files **recursively** from zip archive `path/file.zip` that ends with `.yaml`                                             |
