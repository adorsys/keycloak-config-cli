# System Information

System information operations in variable substitution allow you to access date/time information and localhost details, enabling dynamic configuration based on temporal and network context.

## Overview

System information enables you to:
- Format dates and times with custom patterns
- Access current timestamp
- Retrieve localhost information (hostname, canonical name)
- Generate time-based configuration values
- Create environment-specific timestamps

## Date Formatting

### Basic Date Formatting

Format the current date and time:

```json
{
  "realm": "$(date:yyyy-MM-dd)",
  "attributes": {
    "import_date": "$(date:yyyy-MM-dd)",
    "import_time": "$(date:HH:mm:ss)"
  }
}
```

### Syntax

```
$(date:FORMAT_PATTERN)
```

**Parameters:**
- `FORMAT_PATTERN` - Date/time format pattern (see patterns below)

### Date Format Patterns

**Year:**
- `yyyy` - 4-digit year (e.g., 2026)
- `yy` - 2-digit year (e.g., 26)

**Month:**
- `MM` - 2-digit month (e.g., 04)
- `MMM` - Abbreviated month name (e.g., Apr)
- `MMMM` - Full month name (e.g., April)

**Day:**
- `dd` - 2-digit day (e.g., 15)
- `EEE` - Abbreviated day name (e.g., Wed)
- `EEEE` - Full day name (e.g., Wednesday)

**Hour:**
- `HH` - Hour in 24-hour format (00-23)
- `hh` - Hour in 12-hour format (01-12)

**Minute:**
- `mm` - Minute (00-59)

**Second:**
- `ss` - Second (00-59)

**Millisecond:**
- `SSS` - Millisecond (000-999)

**AM/PM:**
- `a` - AM/PM marker

### Examples

**Date in various formats:**
```json
{
  "realm": "production",
  "attributes": {
    "iso_date": "$(date:yyyy-MM-dd)",
    "iso_datetime": "$(date:yyyy-MM-dd'T'HH:mm:ss)",
    "us_date": "$(date:MM/dd/yyyy)",
    "european_date": "$(date:dd.MM.yyyy)",
    "readable_date": "$(date:EEEE, MMMM dd, yyyy)",
    "compact_date": "$(date:yyyyMMdd)"
  }
}
```

**Time in various formats:**
```json
{
  "realm": "production",
  "attributes": {
    "time_24h": "$(date:HH:mm:ss)",
    "time_12h": "$(date:hh:mm:ss a)",
    "hour_only": "$(date:HH)",
    "time_with_millis": "$(date:HH:mm:ss.SSS)"
  }
}
```

**Combined date and time:**
```json
{
  "realm": "production",
  "attributes": {
    "timestamp": "$(date:yyyy-MM-dd HH:mm:ss)",
    "iso_8601": "$(date:yyyy-MM-dd'T'HH:mm:ssXXX)",
    "filename_date": "$(date:yyyyMMdd_HHmmss)"
  }
}
```

---

## Localhost Information

### Basic Localhost Access

Retrieve localhost information:

```json
{
  "realm": "production",
  "attributes": {
    "hostname": "$(localhost:canonical-name)"
  }
}
```

### Syntax

```
$(localhost:PROPERTY)
```

**Parameters:**
- `PROPERTY` - Localhost property to retrieve

### Localhost Properties

**Canonical Name:**
```json
{
  "attributes": {
    "canonical_name": "$(localhost:canonical-name)"
  }
}
```

**Host Name:**
```json
{
  "attributes": {
    "host_name": "$(localhost:host-name)"
  }
}
```

**IP Address:**
```json
{
  "attributes": {
    "ip_address": "$(localhost:host-address)"
  }
}
```

### Examples

**Comprehensive localhost information:**
```json
{
  "realm": "production",
  "attributes": {
    "canonical_name": "$(localhost:canonical-name)",
    "host_name": "$(localhost:host-name)",
    "host_address": "$(localhost:host-address)"
  }
}
```

**Using localhost in client configuration:**
```json
{
  "realm": "production",
  "clients": [
    {
      "clientId": "local-app",
      "redirectUris": [
        "http://$(localhost:canonical-name):8080/callback"
      ],
      "webOrigins": [
        "http://$(localhost:canonical-name):8080"
      ]
    }
  ]
}
```

---

## Use Cases

### Timestamped Realm Names

**Creating timestamped realm names:**
```json
{
  "realm": "production-$(date:yyyyMMdd)",
  "attributes": {
    "creation_date": "$(date:yyyy-MM-dd)",
    "creation_time": "$(date:HH:mm:ss)"
  }
}
```

### Environment-Specific Configuration

**Date-based environment detection:**
```json
{
  "realm": "production",
  "attributes": {
    "current_date": "$(date:yyyy-MM-dd)",
    "day_of_week": "$(date:EEE)",
    "is_weekend": "$(javascript: ['Sat','Sun'].includes('$(date:EEE)'))"
  }
}
```

### Backup and Archive Naming

**Timestamped backup names:**
```json
{
  "realm": "production",
  "attributes": {
    "backup_filename": "realm-backup-$(date:yyyyMMdd_HHmmss).json",
    "archive_date": "$(date:yyyy-MM-dd)"
  }
}
```

### Localhost-Based Configuration

**Dynamic client configuration based on hostname:**
```json
{
  "realm": "production",
  "clients": [
    {
      "clientId": "web-app",
      "redirectUris": [
        "http://$(localhost:canonical-name):3000/callback"
      ],
      "webOrigins": [
        "http://$(localhost:canonical-name):3000"
      ],
      "adminUrl": "http://$(localhost:canonical-name):3000/admin"
    }
  ]
}
```

### Time-Based Feature Flags

**Date-based feature activation:**
```json
{
  "realm": "production",
  "attributes": {
    "current_date": "$(date:yyyy-MM-dd)",
    "maintenance_window": "$(javascript: new Date().getHours() >= 2 && new Date().getHours() < 4)"
  }
}
```

---

## Time Zone Considerations

### Default Time Zone

Date formatting uses the system's default time zone:

```json
{
  "realm": "production",
  "attributes": {
    "timestamp": "$(date:yyyy-MM-dd HH:mm:ss)",
    "timezone": "system-default"
  }
}
```

### UTC Timestamp

To get UTC timestamps, use JavaScript:

```json
{
  "realm": "production",
  "attributes": {
    "utc_timestamp": "$(javascript: new Date().toISOString())",
    "local_timestamp": "$(date:yyyy-MM-dd HH:mm:ss)"
  }
}
```

### Time Zone in Attributes

Store time zone information:

```json
{
  "realm": "production",
  "attributes": {
    "timestamp": "$(date:yyyy-MM-dd HH:mm:ss)",
    "timezone": "$(sys:user.timezone)",
    "timezone_offset": "$(javascript: new Date().getTimezoneOffset())"
  }
}
```

---

## Error Handling

### Invalid Date Pattern

**Invalid pattern error:**
```
Error: Invalid date format pattern
```

**Solution:** Verify date format pattern syntax

### Localhost Resolution Failure

**Localhost resolution error:**
```
Error: Unable to resolve localhost
```

**Solution:** Check network configuration and hostname settings

### Missing Property

**Property not found error:**
```
Error: Localhost property not found
```

**Solution:** Verify property name is correct

---

## Best Practices

### Date Formatting

1. **Use ISO 8601:** Use ISO 8601 format for interoperability
2. **Document Format:** Document date format used in configuration
3. **Consistent Timezone:** Be consistent with timezone handling
4. **Validate Patterns:** Test date patterns before use

### Localhost Information

1. **Use Canonical Name:** Prefer canonical-name for consistency
2. **Test in Different Environments:** Test in various network environments
3. **Document Dependencies:** Document localhost dependencies
4. **Provide Fallbacks:** Provide fallback values when localhost fails

### General Practices

1. **Test Thoroughly:** Test date formatting in different timezones
2. **Handle Errors:** Implement proper error handling
3. **Document Behavior:** Document timezone and locale behavior
4. **Security First:** Avoid exposing sensitive system information

---

## Performance Considerations

### Date Formatting Overhead

- **Minimal overhead:** Date formatting is very fast
- **Cached Results:** Repeated calls with same pattern are cached
- **No Network I/O:** All operations are local

### Optimization Tips

1. **Reuse Patterns:** Define date patterns as constants
2. **Avoid Repeated Calls:** Cache date values if used repeatedly
3. **Simple Patterns:** Use simple patterns for better performance
4. **Profile Usage:** Profile date operations in large configurations

---

## Complete Examples

### Comprehensive System Information

**realm.json:**
```json
{
  "realm": "production",
  "enabled": true,
  "attributes": {
    "import_timestamp": "$(date:yyyy-MM-dd HH:mm:ss)",
    "import_date": "$(date:yyyy-MM-dd)",
    "import_time": "$(date:HH:mm:ss)",
    "iso_8601": "$(date:yyyy-MM-dd'T'HH:mm:ssXXX)",
    "filename_date": "$(date:yyyyMMdd_HHmmss)",
    "localhost_canonical": "$(localhost:canonical-name)",
    "localhost_host": "$(localhost:host-name)",
    "localhost_address": "$(localhost:host-address)",
    "timezone": "$(sys:user.timezone)"
  }
}
```

### Dynamic Client Configuration

**realm.json:**
```json
{
  "realm": "production",
  "clients": [
    {
      "clientId": "web-app",
      "redirectUris": [
        "http://$(localhost:canonical-name):3000/callback",
        "http://$(localhost:canonical-name):3000/silent-renew"
      ],
      "webOrigins": [
        "http://$(localhost:canonical-name):3000"
      ],
      "adminUrl": "http://$(localhost:canonical-name):3000/admin",
      "attributes": {
        "configured_at": "$(date:yyyy-MM-dd HH:mm:ss)",
        "configured_on": "$(localhost:canonical-name)"
      }
    }
  ]
}
```

### Timestamped Backup Configuration

**realm.json:**
```json
{
  "realm": "production",
  "attributes": {
    "backup_filename": "realm-backup-$(date:yyyyMMdd_HHmmss).json",
    "backup_date": "$(date:yyyy-MM-dd)",
    "backup_time": "$(date:HH:mm:ss)",
    "iso_timestamp": "$(date:yyyy-MM-dd'T'HH:mm:ssXXX)"
  }
}
```

---

## Troubleshooting

### Common Issues

**1. Invalid Date Pattern**
```
Error: Invalid date format pattern
```
**Solution:** Verify date format pattern syntax and use valid pattern characters

**2. Timezone Issues**
```
Date is in wrong timezone
```
**Solution:** Check system timezone settings or use JavaScript for UTC

**3. Localhost Not Resolving**
```
Error: Unable to resolve localhost
```
**Solution:** Check /etc/hosts and network configuration

**4. Pattern Not Matching Expected Format**
```
Date format doesn't match expected pattern
```
**Solution:** Test pattern with simple Java date formatter

### Debug System Information

**Test date formatting:**
```bash
# Test with Java
java -c "
import java.text.SimpleDateFormat;
import java.util.Date;
public class Test {
  public static void main(String[] args) {
    System.out.println(new SimpleDateFormat(\"yyyy-MM-dd\").format(new Date()));
  }
}
"
```

**Test localhost resolution:**
```bash
# Test hostname
hostname
hostname -f

# Test canonical name
nslookup localhost
dig localhost

# Test IP address
hostname -I
```

**Test timezone:**
```bash
# Check timezone
echo $TZ
timedatectl
date
```

---

## Next Steps

- [Overview](overview.md) - Variable substitution introduction
- [Java Integration](java-integration.md) - Java constants and version
- [Environment Variables](environment-variables.md) - Environment variable access
- [JavaScript Substitution](javascript-substitution.md) - Advanced JavaScript evaluation
