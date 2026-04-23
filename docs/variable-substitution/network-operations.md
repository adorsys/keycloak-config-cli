# Network Operations

Network operations in variable substitution allow you to perform DNS lookups and retrieve content from HTTP/HTTPS URLs, enabling dynamic configuration based on external network resources.

## Overview

Network operations enable you to:
- Perform DNS lookups to resolve domain names
- Retrieve content from HTTP URLs
- Retrieve content from HTTPS URLs
- Read content from local file URLs
- Handle network timeouts and errors
- Work with different character encodings

## DNS Lookup

### Basic DNS Lookup

Resolve a domain name to its IP address:

```json
{
  "realm": "$(dns:address|apache.org)",
  "attributes": {
    "keycloak_server": "$(dns:address|keycloak.example.com)"
  }
}
```

### Syntax

```
$(dns:TYPE|DOMAIN)
```

**Parameters:**
- `TYPE` - DNS record type (e.g., `address`, `cname`, `mx`)
- `DOMAIN` - Domain name to resolve

### DNS Record Types

**Address (A Record):**
```json
{
  "keycloak_server": "$(dns:address|keycloak.example.com)"
}
```

**CNAME (Canonical Name):**
```json
{
  "alias_target": "$(dns:cname|www.example.com)"
}
```

**MX (Mail Exchange):**
```json
{
  "mail_server": "$(dns:mx|example.com)"
}
```

### Examples

**Resolving Keycloak server:**
```json
{
  "realm": "production",
  "attributes": {
    "keycloak_ip": "$(dns:address|keycloak.company.com)",
    "keycloak_cname": "$(dns:cname|keycloak.company.com)"
  }
}
```

**Multiple DNS lookups:**
```json
{
  "realm": "production",
  "clients": [
    {
      "clientId": "app-1",
      "attributes": {
        "backend_ip": "$(dns:address|backend-1.company.com)"
      }
    },
    {
      "clientId": "app-2",
      "attributes": {
        "backend_ip": "$(dns:address|backend-2.company.com)"
      }
    }
  ]
}
```

---

## URL Content (HTTP)

### Basic HTTP Content Retrieval

Retrieve content from an HTTP URL:

```json
{
  "realm": "$(url:UTF-8:http://www.apache.org)",
  "displayName": "$(url:UTF-8:http://config-server.example.com/realm-name)"
}
```

### Syntax

```
$(url:ENCODING:URL)
```

**Parameters:**
- `ENCODING` - Character encoding (e.g., UTF-8, ISO-8859-1)
- `URL` - HTTP URL to retrieve content from

### Examples

**Reading realm name from HTTP endpoint:**
```json
{
  "realm": "$(url:UTF-8:http://config-server.example.com/production/realm-name)"
}
```

**Reading configuration from HTTP:**
```json
{
  "realm": "$(url:UTF-8:http://config.example.com/config/realm)",
  "enabled": true,
  "attributes": {
    "config_source": "$(url:UTF-8:http://config.example.com/config/source)",
    "environment": "$(url:UTF-8:http://config.example.com/config/environment)"
  }
}
```

**Dynamic URL construction:**
```json
{
  "realm": "$(url:UTF-8:http://$(env:CONFIG_SERVER)/realm/$(env:ENVIRONMENT))"
}
```

---

## URL Content (HTTPS)

### Basic HTTPS Content Retrieval

Retrieve content from an HTTPS URL with SSL/TLS:

```json
{
  "realm": "$(url:UTF-8:https://www.apache.org)",
  "secret": "$(url:UTF-8:https://vault.example.com/secret/backend-api)"
}
```

### Syntax

```
$(url:ENCODING:HTTPS_URL)
```

**Parameters:**
- `ENCODING` - Character encoding (e.g., UTF-8, ISO-8859-1)
- `HTTPS_URL` - HTTPS URL to retrieve content from

### Examples

**Reading from secure config server:**
```json
{
  "realm": "$(url:UTF-8:https://config-server.company.com/production/realm-name)",
  "clients": [
    {
      "clientId": "backend-api",
      "secret": "$(url:UTF-8:https://vault.company.com/secret/backend-api)"
    }
  ]
}
```

**Reading from multiple HTTPS endpoints:**
```json
{
  "realm": "production",
  "attributes": {
    "realm_config": "$(url:UTF-8:https://config.company.com/realm/production)",
    "client_config": "$(url:UTF-8:https://config.company.com/clients/production)",
    "user_config": "$(url:UTF-8:https://config.company.com/users/production)"
  }
}
```

**With authentication (via URL):**
```json
{
  "secret": "$(url:UTF-8:https://user:password@vault.example.com/secret/backend-api)"
}
```

---

## URL Content (File)

### File URL Content Retrieval

Read content from local files using the file:// URL scheme:

```json
{
  "realm": "$(url:UTF-8:file:///$(sys:user.dir)/config/realm-name.txt)",
  "secret": "$(url:UTF-8:file:///$(sys:user.home)/.secrets/password.txt)"
}
```

### Syntax

```
$(url:ENCODING:file:///FILE_PATH)
```

**Parameters:**
- `ENCODING` - Character encoding (e.g., UTF-8, ISO-8859-1)
- `FILE_PATH` - Absolute file path

### Examples

**Reading from user home directory:**
```json
{
  "realm": "$(url:UTF-8:file:///$(sys:user.home)/config/realm.txt)"
}
```

**Reading from current directory:**
```json
{
  "realm": "$(url:UTF-8:file:///$(sys:user.dir)/config/realm-name.txt)"
}
```

**Dynamic file path:**
```json
{
  "realm": "$(url:UTF-8:file:///$(env:CONFIG_DIR)/realm.txt)"
}
```

---

## Character Encoding

### UTF-8 Encoding (Recommended)

```json
{
  "realm": "$(url:UTF-8:https://config.example.com/realm-name)"
}
```

### Other Encodings

**ISO-8859-1:**
```json
{
  "realm": "$(url:ISO-8859-1:https://config.example.com/realm-name)"
}
```

**US-ASCII:**
```json
{
  "realm": "$(url:US-ASCII:https://config.example.com/realm-name)"
}
```

---

## Error Handling

### Network Timeout

**Timeout error:**
```
Error: Connection timeout
```

**Solution:** Check network connectivity and URL accessibility

### DNS Resolution Failure

**DNS error:**
```
Error: Unable to resolve hostname
```

**Solution:** Verify domain name and DNS configuration

### HTTP Errors

**404 Not Found:**
```
Error: HTTP 404 Not Found
```

**Solution:** Verify URL and resource existence

**500 Internal Server Error:**
```
Error: HTTP 500 Internal Server Error
```

**Solution:** Check server status and logs

### SSL/TLS Errors

**SSL Certificate Error:**
```
Error: SSL certificate verification failed
```

**Solution:** Verify SSL certificate or configure SSL settings

### Connection Refused

**Connection refused:**
```
Error: Connection refused
```

**Solution:** Verify server is running and port is accessible

---

## Security Considerations

### HTTPS vs HTTP

**Always use HTTPS for sensitive data:**
```json
// ❌ Avoid: HTTP for sensitive data
{
  "secret": "$(url:UTF-8:http://config.example.com/secret)"
}

// ✅ Use: HTTPS for sensitive data
{
  "secret": "$(url:UTF-8:https://config.example.com/secret)"
}
```

### URL Authentication

**Avoid credentials in URLs:**
```json
// ❌ Avoid: Credentials in URL
{
  "secret": "$(url:UTF-8:https://user:password@vault.example.com/secret)"
}

// ✅ Better: Use environment variables or file operations
{
  "secret": "$(file:UTF-8:/run/secrets/password)"
}
```

### DNS Spoofing

**Be aware of DNS spoofing risks:**
- DNS lookups can be manipulated
- Validate resolved IPs if security-critical
- Consider using IP addresses directly for high-security scenarios

### Network Exposure

**Network operations expose configuration dependencies:**
- External URLs become dependencies
- Network failures can break imports
- Document external dependencies

---

## Performance Considerations

### Network Latency

- **DNS Lookups:** Typically 10-100ms
- **HTTP Requests:** Variable based on network and server
- **HTTPS Requests:** Additional SSL handshake overhead

### Optimization Tips

1. **Cache Results:** Cache network operation results when possible
2. **Minimize Requests:** Combine multiple values in single request
3. **Use Local Files:** Prefer file operations over network when possible
4. **Timeout Configuration:** Set appropriate timeouts
5. **Monitor Performance:** Profile network operations in large configurations

### Timeout Configuration

Configure timeouts via environment variables:
```bash
# Connection timeout
export KEYCLOAK_CONNECTTIMEOUT=30s

# Read timeout
export KEYCLOAK_READTIMEOUT=30s
```

---

## Use Cases

### Dynamic Configuration from Config Server

**Reading realm configuration from HTTP:**
```json
{
  "realm": "$(url:UTF-8:http://config-server.example.com/production/realm-name)",
  "enabled": "$(url:UTF-8:http://config-server.example.com/production/enabled)",
  "sslRequired": "$(url:UTF-8:http://config-server.example.com/production/ssl-required)"
}
```

### Service Discovery via DNS

**Dynamic service resolution:**
```json
{
  "realm": "production",
  "clients": [
    {
      "clientId": "backend-api",
      "attributes": {
        "backend_host": "$(dns:address|backend.production.company.com)",
        "backend_ip": "$(dns:address|backend-api.production.company.com)"
      }
    }
  ]
}
```

### Secret Retrieval from Vault

**Reading secrets from HashiCorp Vault:**
```json
{
  "clients": [
    {
      "clientId": "backend-api",
      "secret": "$(url:UTF-8:https://vault.company.com/v1/secret/data/backend-api)"
    }
  ]
}
```

### Environment-Specific Configuration

**Dynamic environment configuration:**
```json
{
  "realm": "$(url:UTF-8:https://config.company.com/$(env:ENVIRONMENT)/realm-name)",
  "attributes": {
    "config_source": "$(env:ENVIRONMENT)",
    "config_url": "https://config.company.com/$(env:ENVIRONMENT)"
  }
}
```

---

## Best Practices

### Network Operations

1. **Use HTTPS:** Always use HTTPS for sensitive data
2. **Handle Timeouts:** Configure appropriate timeouts
3. **Document Dependencies:** Document all external URL dependencies
4. **Monitor Availability:** Monitor external service availability
5. **Implement Fallbacks:** Provide fallback values when possible

### DNS Operations

1. **Validate Results:** Validate DNS resolution results
2. **Use Reliable DNS:** Use reliable DNS servers
3. **Cache Results:** Cache DNS results when appropriate
4. **Document Domains:** Document all domain dependencies

### Error Handling

1. **Implement Retries:** Implement retry logic for transient failures
2. **Provide Defaults:** Provide default values when network fails
3. **Log Errors:** Log network operation errors for debugging
4. **Monitor Health:** Monitor network operation health

### Security

1. **Validate URLs:** Validate URLs before use
2. **Use Authentication:** Use proper authentication for protected resources
3. **Encrypt Sensitive Data:** Encrypt sensitive data in transit
4. **Review Dependencies:** Regularly review external dependencies

---

## Complete Examples

### Multi-Source Configuration

**realm.json:**
```json
{
  "realm": "$(url:UTF-8:https://config.company.com/production/realm-name)",
  "enabled": true,
  "attributes": {
    "config_source": "https://config.company.com",
    "environment": "production",
    "keycloak_dns": "$(dns:address|keycloak.production.company.com)",
    "backend_dns": "$(dns:address|backend.production.company.com)"
  },
  "clients": [
    {
      "clientId": "frontend-app",
      "secret": "$(url:UTF-8:https://vault.company.com/v1/secret/data/frontend-secret)",
      "redirectUris": [
        "$(url:UTF-8:https://config.company.com/production/frontend-redirect-uri)"
      ]
    },
    {
      "clientId": "backend-api",
      "secret": "$(url:UTF-8:https://vault.company.com/v1/secret/data/backend-secret)",
      "attributes": {
        "backend_host": "$(dns:address|backend.production.company.com)"
      }
    }
  ]
}
```

### File-Based Configuration with Network Fallback

**realm.json:**
```json
{
  "realm": "$(url:UTF-8:file:///$(sys:user.dir)/config/realm-name.txt)",
  "fallback_realm": "$(url:UTF-8:https://config.company.com/production/realm-name)",
  "attributes": {
    "local_config": "file:///$(sys:user.dir)/config",
    "remote_config": "https://config.company.com"
  }
}
```

---

## Troubleshooting

### Common Issues

**1. Network Unreachable**
```
Error: Network unreachable
```
**Solution:** Check network connectivity and firewall rules

**2. DNS Resolution Failure**
```
Error: Unable to resolve hostname
```
**Solution:** Verify domain name, DNS configuration, and network connectivity

**3. SSL Certificate Error**
```
Error: SSL certificate verification failed
```
**Solution:** Verify SSL certificate validity or configure SSL settings

**4. HTTP 404 Not Found**
```
Error: HTTP 404 Not Found
```
**Solution:** Verify URL and resource existence

**5. Connection Timeout**
```
Error: Connection timeout
```
**Solution:** Increase timeout or check server availability

### Debug Network Operations

**Test DNS resolution:**
```bash
# Test DNS lookup
nslookup keycloak.example.com
dig keycloak.example.com
host keycloak.example.com
```

**Test HTTP/HTTPS:**
```bash
# Test HTTP endpoint
curl http://config.example.com/realm-name

# Test HTTPS endpoint
curl https://config.example.com/realm-name

# Test with verbose output
curl -v https://config.example.com/realm-name
```

**Test file URLs:**
```bash
# Test file URL
curl file:///path/to/file.txt

# Test file content
cat /path/to/file.txt
```

---

## Next Steps

- [Overview](overview.md) - Variable substitution introduction
- [File Operations](file-operations.md) - File content and properties
- [Encoding & Decoding](encoding-decoding.md) - Base64 and URL operations
- [Environment Variables](environment-variables.md) - Environment variable access
