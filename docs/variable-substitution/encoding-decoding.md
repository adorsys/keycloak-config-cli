# Encoding & Decoding

Encoding and decoding operations in variable substitution allow you to transform data between different formats, including Base64 encoding/decoding and URL encoding/decoding.

## Overview

Encoding operations enable you to:
- Encode data to Base64 format
- Decode Base64 data back to original format
- Encode special characters for URLs
- Decode URL-encoded strings
- Handle character encoding transformations

## Base64 Encoding

### Basic Base64 Encoding

Encode a string to Base64:

```json
{
  "realm": "$(base64Encoder:HelloWorld!)",
  "secret": "$(base64Encoder:my-secret-password)"
}
```

### Syntax

```
$(base64Encoder:INPUT_STRING)
```

**Parameters:**
- `INPUT_STRING` - String to encode to Base64

### Examples

**Encoding a client secret:**
```json
{
  "clients": [
    {
      "clientId": "my-app",
      "secret": "$(base64Encoder:my-secret-password-123)"
    }
  ]
}
```

**Encoding realm name:**
```json
{
  "realm": "$(base64Encoder:production-realm)"
}
```

**Encoding from environment variable:**
```json
{
  "secret": "$(base64Encoder:$(env:CLIENT_SECRET))"
}
```

**Result:**
- `HelloWorld!` → `SGVsbG9Xb3JsZCE=`
- `my-secret-password` → `bXktc2VjcmV0LXBhc3N3b3Jk`

---

## Base64 Decoding

### Basic Base64 Decoding

Decode a Base64 string back to original format:

```json
{
  "realm": "$(base64Decoder:SGVsbG9Xb3JsZCE=)",
  "secret": "$(base64Decoder:bXktc2VjcmV0LXBhc3N3b3Jk)"
}
```

### Syntax

```
$(base64Decoder:BASE64_STRING)
```

**Parameters:**
- `BASE64_STRING` - Base64 encoded string to decode

### Examples

**Decoding a stored secret:**
```json
{
  "clients": [
    {
      "clientId": "my-app",
      "secret": "$(base64Decoder:$(file:UTF-8:/run/secrets/encoded-secret))"
    }
  ]
}
```

**Decoding from environment variable:**
```json
{
  "realm": "$(base64Decoder:$(env:ENCODED_REALM_NAME))"
}
```

**Result:**
- `SGVsbG9Xb3JsZCE=` → `HelloWorld!`
- `bXktc2VjcmV0LXBhc3N3b3Jk` → `my-secret-password`

---

## URL Encoding

### Basic URL Encoding

Encode special characters for safe use in URLs:

```json
{
  "redirectUri": "$(urlEncoder:Hello World!)",
  "webOrigin": "$(urlEncoder:https://example.com/path with spaces)"
}
```

### Syntax

```
$(urlEncoder:INPUT_STRING)
```

**Parameters:**
- `INPUT_STRING` - String to URL encode

### Examples

**Encoding redirect URIs:**
```json
{
  "clients": [
    {
      "clientId": "my-app",
      "redirectUris": [
        "$(urlEncoder:https://example.com/callback?param=value)",
        "$(urlEncoder:https://example.com/path with spaces)"
      ]
    }
  ]
}
```

**Encoding web origins:**
```json
{
  "clients": [
    {
      "clientId": "my-app",
      "webOrigins": [
        "$(urlEncoder:https://example.com)",
        "$(urlEncoder:https://app.example.com)"
      ]
    }
  ]
}
```

**Result:**
- `Hello World!` → `Hello%20World%21`
- `https://example.com/path with spaces` → `https%3A%2F%2Fexample.com%2Fpath%20with%20spaces`

---

## URL Decoding

### Basic URL Decoding

Decode URL-encoded strings back to original format:

```json
{
  "redirectUri": "$(urlDecoder:Hello%20World%21)",
  "webOrigin": "$(urlDecoder:https%3A%2F%2Fexample.com)"
}
```

### Syntax

```
$(urlDecoder:ENCODED_STRING)
```

**Parameters:**
- `ENCODED_STRING` - URL-encoded string to decode

### Examples

**Decoding redirect URIs:**
```json
{
  "clients": [
    {
      "clientId": "my-app",
      "redirectUris": [
        "$(urlDecoder:https%3A%2F%2Fexample.com%2Fcallback%3Fparam%3Dvalue)"
      ]
    }
  ]
}
```

**Decoding from file:**
```json
{
  "redirectUri": "$(urlDecoder:$(file:UTF-8:config/encoded-uri.txt))"
}
```

**Result:**
- `Hello%20World%21` → `Hello World!`
- `https%3A%2F%2Fexample.com` → `https://example.com`

---

## Common Use Cases

### Base64 for Secrets

**Storing secrets in encoded format:**
```json
{
  "clients": [
    {
      "clientId": "backend-api",
      "secret": "$(base64Decoder:$(env:ENCODED_SECRET))"
    }
  ]
}
```

**Encode secret before deployment:**
```bash
# Encode secret
echo -n "my-secret-password" | base64
# Output: bXktc2VjcmV0LXBhc3N3b3Jk

# Set as environment variable
export ENCODED_SECRET=bXktc2VjcmV0LXBhc3N3b3Jk
```

### URL Encoding for Redirect URIs

**Safe redirect URI handling:**
```json
{
  "clients": [
    {
      "clientId": "my-app",
      "redirectUris": [
        "$(urlEncoder:$(env:APP_URL)/callback)",
        "$(urlEncoder:$(env:APP_URL)/login?return=$(env:RETURN_URL))"
      ]
    }
  ]
}
```

### Chaining Operations

**Encode then decode (for demonstration):**
```json
{
  "original": "Hello World!",
  "encoded": "$(base64Encoder:Hello World!)",
  "decoded": "$(base64Decoder:$(base64Encoder:Hello World!))"
}
```

**URL encode from file:**
```json
{
  "redirectUri": "$(urlEncoder:$(file:UTF-8:config/callback-url.txt))"
}
```

---

## Character Encoding Considerations

### UTF-8 Handling

**Base64 with UTF-8 characters:**
```json
{
  "realm": "$(base64Encoder:Production-Realm-中文)",
  "displayName": "$(base64Encoder:Application-émojis-🎉)"
}
```

**URL encoding with UTF-8:**
```json
{
  "redirectUri": "$(urlEncoder:https://example.com/path?name=José)"
}
```

### Special Characters

**Common special characters:**
- Space → `%20`
- `!` → `%21`
- `#` → `%23`
- `&` → `%26`
- `=` → `%3D`
- `?` → `%3F`

**Example:**
```json
{
  "redirectUri": "$(urlEncoder:https://example.com/callback?param=value&other=test)"
}
```

---

## Error Handling

### Invalid Base64

**Invalid Base64 string:**
```
Error: Invalid Base64 input
```

**Solution:** Ensure Base64 string is properly formatted

### Invalid URL Encoding

**Malformed URL-encoded string:**
```
Error: Invalid URL encoding
```

**Solution:** Verify URL encoding syntax

### Character Encoding Issues

**Encoding/decoding mismatch:**
```
Error: Character encoding mismatch
```

**Solution:** Use consistent character encoding (UTF-8 recommended)

---

## Security Considerations

### Base64 is Not Encryption

**Important:** Base64 is encoding, not encryption:

```json
// ❌ NOT secure - Base64 is easily reversible
{
  "secret": "$(base64Encoder:my-password)"
}

// ✅ Use proper secret management
{
  "secret": "$(file:UTF-8:/run/secrets/password)"
}
```

### Sensitive Data in URLs

**Be careful with sensitive data in URLs:**
```json
// ❌ Avoid: Sensitive data in URL parameters
{
  "redirectUri": "$(urlEncoder:https://example.com/callback?token=secret-token)"
}

// ✅ Better: Use POST or secure headers
{
  "redirectUri": "$(urlEncoder:https://example.com/callback)"
}
```

### Log Exposure

**Be aware of logging:**
- Encoded strings may appear in logs
- Base64 can be easily decoded
- URL-encoded strings are also reversible

---

## Best Practices

### Base64 Usage

1. **For Transmission Only:** Use Base64 for data transmission, not security
2. **Document Encoding:** Clearly document when and why Base64 is used
3. **Consistent Encoding:** Use the same encoding method throughout
4. **Test Decoding:** Verify decoding works after encoding

### URL Encoding Usage

1. **Always Encode URLs:** Always URL-encode user-provided URLs
2. **Encode Components:** Encode URL components, not entire URLs
3. **Validate After Decoding:** Validate URLs after decoding
4. **Use HTTPS:** Always use HTTPS for sensitive redirects

### General Practices

1. **Prefer File Operations:** For secrets, use file operations instead of encoding
2. **Test Thoroughly:** Test encoding/decoding with real data
3. **Handle Errors:** Implement proper error handling
4. **Document Dependencies:** Document external encoding/decoding requirements

---

## Complete Examples

### Client Configuration with Encoding

**realm.json:**
```json
{
  "realm": "production",
  "clients": [
    {
      "clientId": "frontend-app",
      "secret": "$(base64Decoder:$(env:ENCODED_FRONTEND_SECRET))",
      "redirectUris": [
        "$(urlEncoder:$(env:FRONTEND_URL)/callback)",
        "$(urlEncoder:$(env:FRONTEND_URL)/silent-renew)"
      ],
      "webOrigins": [
        "$(urlEncoder:$(env:FRONTEND_URL))"
      ],
      "adminUrl": "$(urlEncoder:$(env:ADMIN_URL))"
    },
    {
      "clientId": "backend-api",
      "secret": "$(base64Decoder:$(env:ENCODED_BACKEND_SECRET))",
      "redirectUris": [
        "$(urlEncoder:$(env:BACKEND_URL)/oauth/callback)"
      ]
    }
  ]
}
```

**Environment setup:**
```bash
# Encode secrets
export FRONTEND_SECRET=$(echo -n "frontend-secret-123" | base64)
export BACKEND_SECRET=$(echo -n "backend-secret-456" | base64)

export ENCODED_FRONTEND_SECRET=$FRONTEND_SECRET
export ENCODED_BACKEND_SECRET=$BACKEND_SECRET

export FRONTEND_URL=https://app.example.com
export BACKEND_URL=https://api.example.com
export ADMIN_URL=https://admin.example.com
```

### Dynamic URL Configuration

**realm.json:**
```json
{
  "realm": "production",
  "clients": [
    {
      "clientId": "multi-tenant-app",
      "redirectUris": [
        "$(urlEncoder:$(env:TENANT_URL)/callback)",
        "$(urlEncoder:$(env:TENANT_URL)/login?return=$(urlEncoder:$(env:RETURN_URL)))"
      ],
      "webOrigins": [
        "$(urlEncoder:$(env:TENANT_URL))"
      ]
    }
  ]
}
```

---

## Performance Considerations

### Encoding Overhead

- **Base64:** ~33% size increase
- **URL Encoding:** Variable size increase depending on special characters
- **Processing Time:** Minimal overhead for typical operations

### Optimization Tips

1. **Encode Once:** Encode values once and store, don't encode on every import
2. **Cache Results:** Cache encoded/decoded values when possible
3. **Batch Operations:** Process multiple values in single operation
4. **Monitor Performance:** Profile encoding/decoding in large configurations

---

## Troubleshooting

### Common Issues

**1. Encoding/Decoding Mismatch**
```
Error: Encoding/decoding mismatch
```
**Solution:** Ensure same encoding method is used for both operations

**2. Invalid Characters**
```
Error: Invalid character in input
```
**Solution:** Validate input before encoding

**3. Trailing Newlines**
```
Error: Unexpected newline in Base64
```
**Solution:** Remove trailing newlines from Base64 strings

**4. URL Too Long**
```
Error: URL exceeds maximum length
```
**Solution:** Use POST instead of GET for large data

### Debug Encoding

**Test encoding/decoding:**
```bash
# Test Base64
echo -n "test" | base64
echo "dGVzdA==" | base64 -d

# Test URL encoding
python3 -c "import urllib.parse; print(urllib.parse.quote('Hello World!'))"
python3 -c "import urllib.parse; print(urllib.parse.unquote('Hello%20World%21'))"
```

---

## Next Steps

- [Overview](overview.md) - Variable substitution introduction
- [File Operations](file-operations.md) - File content and properties
- [Environment Variables](environment-variables.md) - Environment variable access
- [JavaScript Substitution](javascript-substitution.md) - Advanced JavaScript evaluation
