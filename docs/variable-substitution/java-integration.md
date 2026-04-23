# Java Integration

Java integration in variable substitution allows you to access Java constants and version information, enabling dynamic configuration based on Java runtime environment and standard library values.

## Overview

Java integration enables you to:
- Access Java constants from standard library classes
- Retrieve Java version information
- Use Java system properties
- Reference static final fields from Java classes

## Java Constants

### Basic Java Constant Access

Access static final fields from Java classes:

```json
{
  "realm": "$(const:java.awt.event.KeyEvent.VK_ESCAPE)",
  "attributes": {
    "escape_key": "$(const:java.awt.event.KeyEvent.VK_ESCAPE)"
  }
}
```

### Syntax

```
$(const:FULLY_QUALIFIED_CLASS_NAME.FIELD_NAME)
```

**Parameters:**
- `FULLY_QUALIFIED_CLASS_NAME` - Complete Java class name including package
- `FIELD_NAME` - Name of the static final field

### Common Java Constants

**AWT Event Constants:**
```json
{
  "attributes": {
    "escape_key": "$(const:java.awt.event.KeyEvent.VK_ESCAPE)",
    "enter_key": "$(const:java.awt.event.KeyEvent.VK_ENTER)",
    "space_key": "$(const:java.awt.event.KeyEvent.VK_SPACE)"
  }
}
```

**Character Constants:**
```json
{
  "attributes": {
    "separator": "$(const:java.io.File.separator)",
    "path_separator": "$(const:java.io.File.pathSeparator)"
  }
}
```

**Math Constants:**
```json
{
  "attributes": {
    "pi": "$(const:java.lang.Math.PI)",
    "max_int": "$(const:java.lang.Integer.MAX_VALUE)",
    "min_int": "$(const:java.lang.Integer.MIN_VALUE)"
  }
}
```

### Examples

**Using file separator for cross-platform paths:**
```json
{
  "realm": "production",
  "attributes": {
    "config_path": "config$(const:java.io.File.separator)realm.json",
    "separator": "$(const:java.io.File.separator)"
  }
}
```

**Using character encoding constants:**
```json
{
  "realm": "production",
  "attributes": {
    "encoding": "$(const:java.nio.charset.StandardCharsets.UTF_8)"
  }
}
```

**Using time constants:**
```json
{
  "realm": "production",
  "attributes": {
    "milliseconds_per_second": "$(const:java.util.concurrent.TimeUnit.MILLISECONDS.convert(1,SECONDS))",
    "seconds_per_minute": "60"
  }
}
```

---

## Java Version

### Basic Java Version Access

Retrieve Java version information:

```json
{
  "realm": "$(java:version)",
  "attributes": {
    "java_version": "$(java:version)"
  }
}
```

### Syntax

```
$(java:version)
```

**Returns:** Java runtime version string

### Examples

**Including Java version in realm attributes:**
```json
{
  "realm": "production",
  "attributes": {
    "java_version": "$(java:version)",
    "import_timestamp": "$(date:yyyy-MM-dd HH:mm:ss)"
  }
}
```

**Conditional configuration based on Java version:**
```json
{
  "realm": "production",
  "attributes": {
    "java_version": "$(java:version)",
    "java_major_version": "$(javascript: $(java:version).split('.')[0])"
  }
}
```

---

## System Properties

### Java System Properties

Access Java system properties (also available via `$(sys:PROPERTY_NAME)`):

```json
{
  "realm": "production",
  "attributes": {
    "user_dir": "$(sys:user.dir)",
    "user_home": "$(sys:user.home)",
    "java_home": "$(sys:java.home)",
    "os_name": "$(sys:os.name)"
  }
}
```

### Common System Properties

**File System Properties:**
```json
{
  "attributes": {
    "user_dir": "$(sys:user.dir)",
    "user_home": "$(sys:user.home)",
    "file_separator": "$(sys:file.separator)",
    "path_separator": "$(sys:path.separator)"
  }
}
```

**Java Properties:**
```json
{
  "attributes": {
    "java_version": "$(sys:java.version)",
    "java_home": "$(sys:java.home)",
    "java_vendor": "$(sys:java.vendor)"
  }
}
```

**Operating System Properties:**
```json
{
  "attributes": {
    "os_name": "$(sys:os.name)",
    "os_version": "$(sys:os.version)",
    "os_arch": "$(sys:os.arch)"
  }
}
```

---

## Classpath Requirements

### Available Classes

Java constants require the classes to be available on the classpath:

**Standard Library Classes (Always Available):**
- `java.lang.*` - Core language classes
- `java.io.*` - Input/output classes
- `java.util.*` - Utility classes
- `java.nio.*` - New I/O classes
- `java.awt.*` - AWT classes (if AWT is available)
- `java.math.*` - Mathematical classes

**Third-Party Classes:**
- Must be on the classpath
- May not be available in all environments
- Use with caution

### Custom Classes

To use custom classes, ensure they are on the classpath:

```json
{
  "attributes": {
    "custom_constant": "$(const:com.example.Constants.API_VERSION)"
  }
}
```

**Add to classpath:**
```bash
java -cp /path/to/custom.jar:keycloak-config-cli.jar \
  -jar keycloak-config-cli.jar \
  --import.files.locations=config.json
```

---

## Error Handling

### Class Not Found

**Class not found error:**
```
Error: Class not found: com.example.Constants
```

**Solution:** Ensure class is on classpath

### Field Not Found

**Field not found error:**
```
Error: Field not found: NON_EXISTENT_FIELD in class java.lang.Math
```

**Solution:** Verify field name exists in class

### Access Denied

**Access denied error:**
```
Error: Cannot access private field
```

**Solution:** Use public static final fields only

### Invalid Class Name

**Invalid class name error:**
```
Error: Invalid class name format
```

**Solution:** Use fully qualified class name with proper package

---

## Security Considerations

### Class Access

**Only use trusted classes:**
```json
// ✅ Safe: Standard library classes
{
  "separator": "$(const:java.io.File.separator)"
}

// ⚠️ Caution: Third-party classes
{
  "custom_value": "$(const:com.example.Constants.SECRET)"
}
```

### Field Visibility

**Only public static final fields:**
- Must be `public` for accessibility
- Must be `static` to access without instance
- Must be `final` for constant behavior

### Classpath Security

**Be careful with classpath:**
- Only add trusted JARs to classpath
- Avoid loading arbitrary classes
- Review third-party dependencies

---

## Performance Considerations

### Constant Access Overhead

- **Minimal overhead:** Direct field access is very fast
- **No reflection overhead:** Uses direct field access
- **Cached values:** Static final values are cached by JVM

### Optimization Tips

1. **Use Standard Library:** Prefer standard library classes
2. **Avoid Complex Lookups:** Simple constant access is faster
3. **Cache Results:** Cache constant values if used repeatedly
4. **Profile Usage:** Profile constant access in large configurations

---

## Use Cases

### Cross-Platform Paths

**Using file separator for cross-platform compatibility:**
```json
{
  "realm": "production",
  "attributes": {
    "config_dir": "config$(const:java.io.File.separator)production",
    "log_dir": "logs$(const:java.io.File.separator)production"
  }
}
```

### Version Tracking

**Tracking Java version used for import:**
```json
{
  "realm": "production",
  "attributes": {
    "java_version": "$(java:version)",
    "import_java_version": "$(sys:java.version)",
    "import_timestamp": "$(date:yyyy-MM-dd HH:mm:ss)"
  }
}
```

### Environment Detection

**Detecting operating system:**
```json
{
  "realm": "production",
  "attributes": {
    "os_name": "$(sys:os.name)",
    "is_windows": "$(javascript: '$(sys:os.name)'.toLowerCase().includes('windows'))",
    "is_linux": "$(javascript: '$(sys:os.name)'.toLowerCase().includes('linux'))",
    "is_mac": "$(javascript: '$(sys:os.name)'.toLowerCase().includes('mac'))"
  }
}
```

### Configuration Constants

**Using configuration constants:**
```json
{
  "realm": "production",
  "clients": [
    {
      "clientId": "api-client",
      "attributes": {
        "max_connections": "$(const:java.lang.Integer.MAX_VALUE)",
        "timeout_millis": "30000"
      }
    }
  ]
}
```

---

## Best Practices

### Java Constants

1. **Use Standard Library:** Prefer standard library classes
2. **Verify Field Existence:** Ensure field exists before use
3. **Use Public Fields:** Only use public static final fields
4. **Document Dependencies:** Document custom class dependencies

### Java Version

1. **Track Version:** Track Java version for debugging
2. **Version Compatibility:** Ensure configuration compatible with Java version
3. **Document Requirements:** Document Java version requirements

### System Properties

1. **Use When Appropriate:** Use system properties for environment-specific values
2. **Provide Defaults:** Provide default values when system property may not exist
3. **Document Properties:** Document required system properties

### General Practices

1. **Test Thoroughly:** Test with different Java versions
2. **Handle Errors:** Implement proper error handling
3. **Document Classpath:** Document required classpath entries
4. **Security First:** Only use trusted classes and fields

---

## Complete Examples

### Cross-Platform Configuration

**realm.json:**
```json
{
  "realm": "production",
  "enabled": true,
  "attributes": {
    "file_separator": "$(const:java.io.File.separator)",
    "path_separator": "$(const:java.io.File.pathSeparator)",
    "config_path": "config$(const:java.io.File.separator)production",
    "log_path": "logs$(const:java.io.File.separator)production",
    "java_version": "$(java:version)",
    "os_name": "$(sys:os.name)",
    "user_dir": "$(sys:user.dir)"
  }
}
```

### Environment-Specific Configuration

**realm.json:**
```json
{
  "realm": "production",
  "attributes": {
    "java_version": "$(java:version)",
    "java_home": "$(sys:java.home)",
    "os_name": "$(sys:os.name)",
    "os_arch": "$(sys:os.arch)",
    "user_home": "$(sys:user.home)",
    "environment": "$(env:ENVIRONMENT:production)"
  }
}
```

### Configuration with Constants

**realm.json:**
```json
{
  "realm": "production",
  "clients": [
    {
      "clientId": "api-client",
      "attributes": {
        "max_connections": "$(const:java.lang.Integer.MAX_VALUE)",
        "buffer_size": "$(const:java.lang.Integer.BYTES)",
        "char_set": "$(const:java.nio.charset.StandardCharsets.UTF_8)",
        "line_separator": "$(const:java.lang.System.lineSeparator())"
      }
    }
  ]
}
```

---

## Troubleshooting

### Common Issues

**1. Class Not Found**
```
Error: Class not found: com.example.Constants
```
**Solution:** Ensure class is on classpath and package name is correct

**2. Field Not Found**
```
Error: Field not found: INVALID_FIELD
```
**Solution:** Verify field name exists in class and is public static final

**3. Access Denied**
```
Error: Cannot access private field
```
**Solution:** Use only public fields

**4. Invalid Class Name**
```
Error: Invalid class name format
```
**Solution:** Use fully qualified class name with proper package structure

### Debug Java Integration

**Test class availability:**
```bash
# Test with Java
java -cp keycloak-config-cli.jar com.example.Constants

# List classpath
java -verbose:class -jar keycloak-config-cli.jar

# Check system properties
java -XshowSettings:properties -version
```

**Test constant access:**
```bash
# Test with simple Java program
java -c "
import java.io.File;
public class Test {
  public static void main(String[] args) {
    System.out.println(File.separator);
  }
}
"
```

---

## Next Steps

- [Overview](overview.md) - Variable substitution introduction
- [System Information](system-information.md) - Date and localhost information
- [Environment Variables](environment-variables.md) - Environment variable access
- [JavaScript Substitution](javascript-substitution.md) - Advanced JavaScript evaluation
