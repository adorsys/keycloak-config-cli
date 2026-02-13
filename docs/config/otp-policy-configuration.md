# OTP Policy Configuration

When configuring One-Time Password (OTP) policies in Keycloak realms, users often encounter issues where certain OTP policy settings appear to be ignored during import. Understanding how to properly configure OTP policies in keycloak-config-cli is essential for ensuring multi-factor authentication works as expected.

Related issues: [#847](https://github.com/adorsys/keycloak-config-cli/issues/847)

## The Problem

Users often encounter confusion when configuring OTP policies because:
- Setting only `otpPolicyAlgorithm` in the configuration doesn't apply the change
- Some OTP policy fields appear to be ignored during import
- It's unclear which OTP policy fields must be configured together
- Default values may override explicitly set values if the configuration is incomplete
- The relationship between different OTP policy settings is not obvious

## What is OTP Policy?

OTP (One-Time Password) policy in Keycloak controls how Time-based One-Time Passwords (TOTP) and HMAC-based One-Time Passwords (HOTP) are generated and validated for multi-factor authentication.

**Key OTP Policy Settings:**
- **Algorithm**: Hash algorithm used (HmacSHA1, HmacSHA256, HmacSHA512)
- **Digits**: Number of digits in the OTP (6 or 8)
- **Period**: Time window for TOTP validity (in seconds)
- **Initial Counter**: Starting counter value for HOTP
- **Look Ahead Window**: Number of intervals to check for validation

## Why otpPolicyAlgorithm Gets Ignored

The `otpPolicyAlgorithm` field is ignored when:

1. **Incomplete OTP Policy Configuration**: Setting only one field without others causes Keycloak to use default values
2. **Missing Required Fields**: OTP policy requires a complete set of related fields to be valid
3. **Type Mismatch**: Using `hotp` type without proper counter configuration
4. **Implicit Defaults**: Keycloak applies defaults when the policy is partially configured

## Usage

### Complete OTP Policy Configuration (TOTP)

**Scenario:** Configure a realm to use SHA-256 algorithm for TOTP with 6-digit codes.
```yaml
realm: "myrealm"
otpPolicyType: "totp"
otpPolicyAlgorithm: "HmacSHA256"
otpPolicyDigits: 6
otpPolicyInitialCounter: 0
otpPolicyLookAheadWindow: 1
otpPolicyPeriod: 30
```

**Result:** 
- TOTP uses HmacSHA256 algorithm
- Generates 6-digit codes
- 30-second validity window
- Looks ahead 1 time window for validation

**Important:** All OTP policy fields should be specified together for consistent behavior.

---

### Incomplete Configuration (Problematic)

**Scenario:** Attempting to change only the algorithm.

**Problematic Configuration:**
```yaml
realm: "myrealm"
otpPolicyAlgorithm: "HmacSHA256"  # Only this field set
```

**What happens:**
- The `otpPolicyAlgorithm` may be ignored
- Keycloak uses default values for other fields
- Results in inconsistent OTP configuration
- May revert to default HmacSHA1 algorithm

**Why it fails:** Keycloak's OTP policy expects a complete configuration. Partial updates may not be applied correctly.

---

### HOTP Configuration

**Scenario:** Configure HMAC-based OTP instead of time-based.
```yaml
realm: "myrealm"
otpPolicyType: "hotp"
otpPolicyAlgorithm: "HmacSHA1"
otpPolicyDigits: 6
otpPolicyInitialCounter: 0
otpPolicyLookAheadWindow: 10
```

**Result:**
- Uses counter-based OTP (HOTP)
- HmacSHA1 algorithm
- 6-digit codes
- Looks ahead 10 counter values for validation

**Note:** For HOTP, the `otpPolicyPeriod` field is not used.

---

### High Security OTP Configuration

**Scenario:** Maximum security setup with SHA-512 and 8-digit codes.
```yaml
realm: "myrealm"
otpPolicyType: "totp"
otpPolicyAlgorithm: "HmacSHA512"
otpPolicyDigits: 8
otpPolicyInitialCounter: 0
otpPolicyLookAheadWindow: 1
otpPolicyPeriod: 30
otpPolicySupportedApplications:
  - "totpAppFreeOTPName"
  - "totpAppGoogleName"
```

**Result:**
- Most secure hash algorithm (SHA-512)
- 8-digit codes for additional security
- Compatible with Google Authenticator and FreeOTP

---

## OTP Policy Fields Reference

| Field | Type | Values | Description | Required |
|-------|------|--------|-------------|----------|
| `otpPolicyType` | String | `totp`, `hotp` | Type of OTP | Yes |
| `otpPolicyAlgorithm` | String | `HmacSHA1`, `HmacSHA256`, `HmacSHA512` | Hash algorithm | Yes |
| `otpPolicyDigits` | Integer | `6`, `8` | Number of digits in OTP | Yes |
| `otpPolicyInitialCounter` | Integer | `0` or higher | Initial counter (mainly for HOTP) | Yes |
| `otpPolicyLookAheadWindow` | Integer | `1` or higher | Validation window size | Yes |
| `otpPolicyPeriod` | Integer | `30` (seconds) | TOTP time window | For TOTP |

---

## Common Pitfalls

### 1. Setting Only Algorithm

**Problem:**
```yaml
realm: "myrealm"
otpPolicyAlgorithm: "HmacSHA256"
# Other fields missing
```

**What happens:** Algorithm change is ignored, other fields use defaults.

**Solution:** Always specify all OTP policy fields together:
```yaml
realm: "myrealm"
otpPolicyType: "totp"
otpPolicyAlgorithm: "HmacSHA256"
otpPolicyDigits: 6
otpPolicyInitialCounter: 0
otpPolicyLookAheadWindow: 1
otpPolicyPeriod: 30
```

---

### 2. Mixing TOTP and HOTP Settings

**Problem:**
```yaml
realm: "myrealm"
otpPolicyType: "hotp"
otpPolicyPeriod: 30  # Period is for TOTP, not HOTP
```

**What happens:** Conflicting configuration, unpredictable behavior.

**Solution:** Use appropriate fields for the OTP type:

**For TOTP:**
```yaml
otpPolicyType: "totp"
otpPolicyPeriod: 30  # Include period
```

**For HOTP:**
```yaml
otpPolicyType: "hotp"
# Don't include otpPolicyPeriod
otpPolicyLookAheadWindow: 10  # Important for HOTP
```

---

### 3. Incorrect Algorithm Names

**Problem:**
```yaml
otpPolicyAlgorithm: "SHA256"  # Wrong!
```

**Correct values:**
```yaml
otpPolicyAlgorithm: "HmacSHA256"  # Correct
# or
otpPolicyAlgorithm: "HmacSHA1"    # Correct
# or
otpPolicyAlgorithm: "HmacSHA512"  # Correct
```

**Note:** Must include the `Hmac` prefix.

---

### 4. Exporting and Missing Fields

**Problem:** Exporting a realm and re-importing without all OTP policy fields.

**When exporting:**
```json
{
  "realm": "myrealm",
  "otpPolicyAlgorithm": "HmacSHA256",
  // Other fields may be missing from export
}
```

**Solution:** Ensure exported configuration includes all OTP policy fields, or add them manually:
```yaml
realm: "myrealm"
otpPolicyType: "totp"
otpPolicyAlgorithm: "HmacSHA256"
otpPolicyDigits: 6
otpPolicyInitialCounter: 0
otpPolicyLookAheadWindow: 1
otpPolicyPeriod: 30
```

---

## Best Practices

1. **Always Configure Complete OTP Policy**: Include all related fields in a single configuration block
2. **Use Standard Values First**: Start with standard settings (SHA256, 6 digits, 30 seconds) unless security requirements dictate otherwise
3. **Document Your Choices**: Add comments explaining why specific OTP settings were chosen
4. **Test Authentication**: After applying OTP policy changes, test with authenticator apps (Google Authenticator, FreeOTP)
5. **Version Control**: Store OTP policy configurations in version control to track security policy changes
6. **Coordinate with Users**: Changing OTP settings may require users to re-register their authenticators

---

## Configuration Template

Here's a complete template for OTP policy configuration:
```yaml
realm: "myrealm"
enabled: true

# OTP Policy Configuration
otpPolicyType: "totp"                    # totp or hotp
otpPolicyAlgorithm: "HmacSHA256"         # HmacSHA1, HmacSHA256, or HmacSHA512
otpPolicyDigits: 6                       # 6 or 8
otpPolicyInitialCounter: 0               # Starting counter
otpPolicyLookAheadWindow: 1              # Validation window
otpPolicyPeriod: 30                      # Time window in seconds (TOTP only)

# Optional: Specify supported authenticator apps
otpPolicySupportedApplications:
  - "totpAppFreeOTPName"
  - "totpAppGoogleName"
  - "totpAppMicrosoftAuthenticatorName"
```

---

## Verifying OTP Policy Configuration

After applying the configuration, verify in Keycloak Admin Console:

1. Navigate to: **Realm Settings** → **Security defenses** → **OTP Policy**
2. Verify all settings match your configuration:
   - OTP Type
   - OTP Hash Algorithm
   - Number of Digits
   - Look Ahead Window
   - OTP Token Period

---

## Algorithm Comparison

| Algorithm | Security Level | Compatibility | Recommendation |
|-----------|---------------|---------------|----------------|
| HmacSHA1 | Basic | Universal | Legacy systems only |
| HmacSHA256 | Strong | Modern apps | **Recommended** |
| HmacSHA512 | Maximum | Modern apps | High security environments |

---

## Migration Guide

### From SHA1 to SHA256

**Scenario:** Upgrading from legacy SHA1 to more secure SHA256.

**Steps:**

1. **Announce the change** to users (they'll need to re-register authenticators)

2. **Update configuration:**
```yaml
realm: "myrealm"
otpPolicyType: "totp"
otpPolicyAlgorithm: "HmacSHA256"  # Changed from HmacSHA1
otpPolicyDigits: 6
otpPolicyInitialCounter: 0
otpPolicyLookAheadWindow: 1
otpPolicyPeriod: 30
```

3. **Apply the configuration** using keycloak-config-cli

4. **Communicate to users:**
   - Existing OTP credentials will need to be re-configured
   - Users must remove old authenticator entries
   - Users must re-scan QR code with authenticator app

**Important:** This is a breaking change for existing users. Plan accordingly.

---

## Troubleshooting

### OTP Algorithm Not Applying

**Symptom:** After import, OTP algorithm remains as default (HmacSHA1)

**Diagnosis:**
```bash
# Check realm configuration
curl -s "http://localhost:8080/admin/realms/myrealm" \
  -H "Authorization: Bearer $TOKEN" | jq '.otpPolicy'
```

**Possible causes:**
1. Incomplete OTP policy configuration
2. Missing required fields
3. Configuration file not being imported
4. Import validation errors

**Solution:** Use complete OTP policy configuration with all fields.

---

### Users Can't Authenticate After OTP Change

**Symptom:** Users receive "Invalid authenticator code" errors

**Cause:** OTP algorithm change requires users to re-register their authenticators

**Solution:**
1. Users must delete old authenticator entry
2. In Keycloak, remove old OTP credential
3. User re-registers with new QR code
4. Test authentication with new code

---

## Configuration Options
```bash
# Validate configuration before import
--import.validate=true

# Check what will be changed
--import.remote-state.enabled=true
```

---

## Consequences

When configuring OTP policies in keycloak-config-cli:

1. **Complete Configuration Required**: All OTP policy fields must be specified together for changes to apply correctly
2. **Algorithm Changes Break Existing Credentials**: Users must re-register authenticators after algorithm changes
3. **Type Changes Are Disruptive**: Switching between TOTP and HOTP requires user action
4. **Defaults May Override**: Partial configurations may result in unexpected default values being applied
5. **No Backward Compatibility**: Old authenticator entries won't work after policy changes

---

## Security Considerations

1. **SHA1 is Deprecated**: Avoid HmacSHA1 for new deployments; considered weak by modern standards
2. **8-digit Codes**: More secure but less user-friendly; use for high-security environments
3. **Period Duration**: 30 seconds is standard; shorter periods increase security but reduce usability
4. **Look Ahead Window**: Larger windows reduce failed authentications but slightly decrease security

---

## Related Issues

- [#847 - otpPolicyAlgorithm ignored](https://github.com/adorsys/keycloak-config-cli/issues/847)

---

## Additional Resources

- [Keycloak OTP Policy Documentation](https://www.keycloak.org/docs/latest/server_admin/#otp-policies)
- [TOTP RFC 6238](https://datatracker.ietf.org/doc/html/rfc6238)
- [HOTP RFC 4226](https://datatracker.ietf.org/doc/html/rfc4226)
- [Import Configuration](../import-settings.md)# OTP Policy Configuration

When configuring One-Time Password (OTP) policies in Keycloak realms, users often encounter issues where certain OTP policy settings appear to be ignored during import. Understanding how to properly configure OTP policies in keycloak-config-cli is essential for ensuring multi-factor authentication works as expected.

Related issues: [#847](https://github.com/adorsys/keycloak-config-cli/issues/847)

## The Problem

Users often encounter confusion when configuring OTP policies because:
- Setting only `otpPolicyAlgorithm` in the configuration doesn't apply the change
- Some OTP policy fields appear to be ignored during import
- It's unclear which OTP policy fields must be configured together
- Default values may override explicitly set values if the configuration is incomplete
- The relationship between different OTP policy settings is not obvious

## What is OTP Policy?

OTP (One-Time Password) policy in Keycloak controls how Time-based One-Time Passwords (TOTP) and HMAC-based One-Time Passwords (HOTP) are generated and validated for multi-factor authentication.

**Key OTP Policy Settings:**
- **Algorithm**: Hash algorithm used (HmacSHA1, HmacSHA256, HmacSHA512)
- **Digits**: Number of digits in the OTP (6 or 8)
- **Period**: Time window for TOTP validity (in seconds)
- **Initial Counter**: Starting counter value for HOTP
- **Look Ahead Window**: Number of intervals to check for validation

## Why otpPolicyAlgorithm Gets Ignored

The `otpPolicyAlgorithm` field is ignored when:

1. **Incomplete OTP Policy Configuration**: Setting only one field without others causes Keycloak to use default values
2. **Missing Required Fields**: OTP policy requires a complete set of related fields to be valid
3. **Type Mismatch**: Using `hotp` type without proper counter configuration
4. **Implicit Defaults**: Keycloak applies defaults when the policy is partially configured

## Usage

### Complete OTP Policy Configuration (TOTP)

**Scenario:** Configure a realm to use SHA-256 algorithm for TOTP with 6-digit codes.
```yaml
realm: "myrealm"
otpPolicyType: "totp"
otpPolicyAlgorithm: "HmacSHA256"
otpPolicyDigits: 6
otpPolicyInitialCounter: 0
otpPolicyLookAheadWindow: 1
otpPolicyPeriod: 30
```

**Result:** 
- TOTP uses HmacSHA256 algorithm
- Generates 6-digit codes
- 30-second validity window
- Looks ahead 1 time window for validation

**Important:** All OTP policy fields should be specified together for consistent behavior.

---

### Incomplete Configuration (Problematic)

**Scenario:** Attempting to change only the algorithm.

**Problematic Configuration:**
```yaml
realm: "myrealm"
otpPolicyAlgorithm: "HmacSHA256"  # Only this field set
```

**What happens:**
- The `otpPolicyAlgorithm` may be ignored
- Keycloak uses default values for other fields
- Results in inconsistent OTP configuration
- May revert to default HmacSHA1 algorithm

**Why it fails:** Keycloak's OTP policy expects a complete configuration. Partial updates may not be applied correctly.

---

### HOTP Configuration

**Scenario:** Configure HMAC-based OTP instead of time-based.
```yaml
realm: "myrealm"
otpPolicyType: "hotp"
otpPolicyAlgorithm: "HmacSHA1"
otpPolicyDigits: 6
otpPolicyInitialCounter: 0
otpPolicyLookAheadWindow: 10
```

**Result:**
- Uses counter-based OTP (HOTP)
- HmacSHA1 algorithm
- 6-digit codes
- Looks ahead 10 counter values for validation

**Note:** For HOTP, the `otpPolicyPeriod` field is not used.

---

### High Security OTP Configuration

**Scenario:** Maximum security setup with SHA-512 and 8-digit codes.
```yaml
realm: "myrealm"
otpPolicyType: "totp"
otpPolicyAlgorithm: "HmacSHA512"
otpPolicyDigits: 8
otpPolicyInitialCounter: 0
otpPolicyLookAheadWindow: 1
otpPolicyPeriod: 30
otpPolicySupportedApplications:
  - "totpAppFreeOTPName"
  - "totpAppGoogleName"
```

**Result:**
- Most secure hash algorithm (SHA-512)
- 8-digit codes for additional security
- Compatible with Google Authenticator and FreeOTP

---

## OTP Policy Fields Reference

| Field | Type | Values | Description | Required |
|-------|------|--------|-------------|----------|
| `otpPolicyType` | String | `totp`, `hotp` | Type of OTP | Yes |
| `otpPolicyAlgorithm` | String | `HmacSHA1`, `HmacSHA256`, `HmacSHA512` | Hash algorithm | Yes |
| `otpPolicyDigits` | Integer | `6`, `8` | Number of digits in OTP | Yes |
| `otpPolicyInitialCounter` | Integer | `0` or higher | Initial counter (mainly for HOTP) | Yes |
| `otpPolicyLookAheadWindow` | Integer | `1` or higher | Validation window size | Yes |
| `otpPolicyPeriod` | Integer | `30` (seconds) | TOTP time window | For TOTP |

---

## Common Pitfalls

### 1. Setting Only Algorithm

**Problem:**
```yaml
realm: "myrealm"
otpPolicyAlgorithm: "HmacSHA256"
# Other fields missing
```

**What happens:** Algorithm change is ignored, other fields use defaults.

**Solution:** Always specify all OTP policy fields together:
```yaml
realm: "myrealm"
otpPolicyType: "totp"
otpPolicyAlgorithm: "HmacSHA256"
otpPolicyDigits: 6
otpPolicyInitialCounter: 0
otpPolicyLookAheadWindow: 1
otpPolicyPeriod: 30
```

---

### 2. Mixing TOTP and HOTP Settings

**Problem:**
```yaml
realm: "myrealm"
otpPolicyType: "hotp"
otpPolicyPeriod: 30  # Period is for TOTP, not HOTP
```

**What happens:** Conflicting configuration, unpredictable behavior.

**Solution:** Use appropriate fields for the OTP type:

**For TOTP:**
```yaml
otpPolicyType: "totp"
otpPolicyPeriod: 30  # Include period
```

**For HOTP:**
```yaml
otpPolicyType: "hotp"
# Don't include otpPolicyPeriod
otpPolicyLookAheadWindow: 10  # Important for HOTP
```

---

### 3. Incorrect Algorithm Names

**Problem:**
```yaml
otpPolicyAlgorithm: "SHA256"  # Wrong!
```

**Correct values:**
```yaml
otpPolicyAlgorithm: "HmacSHA256"  # Correct
# or
otpPolicyAlgorithm: "HmacSHA1"    # Correct
# or
otpPolicyAlgorithm: "HmacSHA512"  # Correct
```

**Note:** Must include the `Hmac` prefix.

---

### 4. Exporting and Missing Fields

**Problem:** Exporting a realm and re-importing without all OTP policy fields.

**When exporting:**
```json
{
  "realm": "myrealm",
  "otpPolicyAlgorithm": "HmacSHA256",
  // Other fields may be missing from export
}
```

**Solution:** Ensure exported configuration includes all OTP policy fields, or add them manually:
```yaml
realm: "myrealm"
otpPolicyType: "totp"
otpPolicyAlgorithm: "HmacSHA256"
otpPolicyDigits: 6
otpPolicyInitialCounter: 0
otpPolicyLookAheadWindow: 1
otpPolicyPeriod: 30
```

---

## Best Practices

1. **Always Configure Complete OTP Policy**: Include all related fields in a single configuration block
2. **Use Standard Values First**: Start with standard settings (SHA256, 6 digits, 30 seconds) unless security requirements dictate otherwise
3. **Document Your Choices**: Add comments explaining why specific OTP settings were chosen
4. **Test Authentication**: After applying OTP policy changes, test with authenticator apps (Google Authenticator, FreeOTP)
5. **Version Control**: Store OTP policy configurations in version control to track security policy changes
6. **Coordinate with Users**: Changing OTP settings may require users to re-register their authenticators

---

## Configuration Template

Here's a complete template for OTP policy configuration:
```yaml
realm: "myrealm"
enabled: true

# OTP Policy Configuration
otpPolicyType: "totp"                    # totp or hotp
otpPolicyAlgorithm: "HmacSHA256"         # HmacSHA1, HmacSHA256, or HmacSHA512
otpPolicyDigits: 6                       # 6 or 8
otpPolicyInitialCounter: 0               # Starting counter
otpPolicyLookAheadWindow: 1              # Validation window
otpPolicyPeriod: 30                      # Time window in seconds (TOTP only)

# Optional: Specify supported authenticator apps
otpPolicySupportedApplications:
  - "totpAppFreeOTPName"
  - "totpAppGoogleName"
  - "totpAppMicrosoftAuthenticatorName"
```

---

## Verifying OTP Policy Configuration

After applying the configuration, verify in Keycloak Admin Console:

1. Navigate to: **Realm Settings** → **Security defenses** → **OTP Policy**
2. Verify all settings match your configuration:
   - OTP Type
   - OTP Hash Algorithm
   - Number of Digits
   - Look Ahead Window
   - OTP Token Period

---

## Algorithm Comparison

| Algorithm | Security Level | Compatibility | Recommendation |
|-----------|---------------|---------------|----------------|
| HmacSHA1 | Basic | Universal | Legacy systems only |
| HmacSHA256 | Strong | Modern apps | **Recommended** |
| HmacSHA512 | Maximum | Modern apps | High security environments |

---

## Migration Guide

### From SHA1 to SHA256

**Scenario:** Upgrading from legacy SHA1 to more secure SHA256.

**Steps:**

1. **Announce the change** to users (they'll need to re-register authenticators)

2. **Update configuration:**
```yaml
realm: "myrealm"
otpPolicyType: "totp"
otpPolicyAlgorithm: "HmacSHA256"  # Changed from HmacSHA1
otpPolicyDigits: 6
otpPolicyInitialCounter: 0
otpPolicyLookAheadWindow: 1
otpPolicyPeriod: 30
```

3. **Apply the configuration** using keycloak-config-cli

4. **Communicate to users:**
   - Existing OTP credentials will need to be re-configured
   - Users must remove old authenticator entries
   - Users must re-scan QR code with authenticator app

**Important:** This is a breaking change for existing users. Plan accordingly.

---

## Troubleshooting

### OTP Algorithm Not Applying

**Symptom:** After import, OTP algorithm remains as default (HmacSHA1)

**Diagnosis:**
```bash
# Check realm configuration
curl -s "http://localhost:8080/admin/realms/myrealm" \
  -H "Authorization: Bearer $TOKEN" | jq '.otpPolicy'
```

**Possible causes:**
1. Incomplete OTP policy configuration
2. Missing required fields
3. Configuration file not being imported
4. Import validation errors

**Solution:** Use complete OTP policy configuration with all fields.

---

### Users Can't Authenticate After OTP Change

**Symptom:** Users receive "Invalid authenticator code" errors

**Cause:** OTP algorithm change requires users to re-register their authenticators

**Solution:**
1. Users must delete old authenticator entry
2. In Keycloak, remove old OTP credential
3. User re-registers with new QR code
4. Test authentication with new code

---

## Configuration Options
```bash
# Validate configuration before import
--import.validate=true

# Check what will be changed
--import.remote-state.enabled=true
```

---

## Consequences

When configuring OTP policies in keycloak-config-cli:

1. **Complete Configuration Required**: All OTP policy fields must be specified together for changes to apply correctly
2. **Algorithm Changes Break Existing Credentials**: Users must re-register authenticators after algorithm changes
3. **Type Changes Are Disruptive**: Switching between TOTP and HOTP requires user action
4. **Defaults May Override**: Partial configurations may result in unexpected default values being applied
5. **No Backward Compatibility**: Old authenticator entries won't work after policy changes

---

## Security Considerations

1. **SHA1 is Deprecated**: Avoid HmacSHA1 for new deployments; considered weak by modern standards
2. **8-digit Codes**: More secure but less user-friendly; use for high-security environments
3. **Period Duration**: 30 seconds is standard; shorter periods increase security but reduce usability
4. **Look Ahead Window**: Larger windows reduce failed authentications but slightly decrease security

---

## Related Issues

- [#847 - otpPolicyAlgorithm ignored](https://github.com/adorsys/keycloak-config-cli/issues/847)

---

## Additional Resources

- [Keycloak OTP Policy Documentation](https://www.keycloak.org/docs/latest/server_admin/#otp-policies)
- [TOTP RFC 6238](https://datatracker.ietf.org/doc/html/rfc6238)
- [HOTP RFC 4226](https://datatracker.ietf.org/doc/html/rfc4226)
- [Import Configuration](../import-settings.md)