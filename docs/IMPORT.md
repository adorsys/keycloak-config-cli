# Import settings

You can use `--import.path` setting or `IMPORT_PATH` environment variable to choose which configuration files to import in Keycloak.

### `import.path`

`--import.path` setting make use
of [PathMatchingResourcePatternResolver](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/core/io/support/PathMatchingResourcePatternResolver.html)
to collect one or many resources from a String or an Ant-style pattern.

If a string is given, it generates a single resource entry, but if an Ant-style pattern is given, it generates many resource entries.

You can also use `--import.path` setting many times in your command line, or use `,` separator in `IMPORT_PATH` environment variable.

Each resource entry is then processed by a [ResourceExtractor](../src/main/java/de/adorsys/keycloak/config/provider/ResourceExtractor.java) instance,
based on the resource type.

- If it's a file, the file is read and included into imported
  files ([FileResourceExtractor](../src/main/java/de/adorsys/keycloak/config/provider/FileResourceExtractor.java))

- If it's a directory, all files is contains and read and included into imported
  files ([DirectoryResourceExtractor](../src/main/java/de/adorsys/keycloak/config/provider/DirectoryResourceExtractor.java))

- If it's an URL, the resource is read as a file through Java standard way to load files from URL and included into imported
  files ([UrlResourceExtractor](../src/main/java/de/adorsys/keycloak/config/provider/UrlResourceExtractor.java))

### `import.hidden-files`

By default, hidden files will be excluded from import. To include them, use `--import.exclude=true` flag or `IMPORT_EXCLUDE=true` environment
variable.

### `import.exclude`

`--import.exclude` flag or `IMPORT_EXCLUDE` environment variable can be used to exclude some files, using Ant-style pattern.
