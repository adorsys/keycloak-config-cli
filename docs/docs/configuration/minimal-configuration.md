## Getting Minimal Import After Realm Export

This script is designed to clean up a Keycloak realm configuration file (in JSON format) by removing unnecessary fields, including all `id` fields, from the configuration. It is useful for simplifying the export of Keycloak realm data, especially when certain details like IDs are not needed for sharing or backup purposes.

### Features

- **Removes unnecessary default fields** such as `accessTokenLifespan`, `offlineSessionIdleTimeout`, and others that are typically not needed for a reimport.
- **Simplifies the realm configuration** while retaining all necessary properties for further processing or importing into another Keycloak instance.

### Requirements

- **jq**: This script requires `jq`, a command-line JSON processor, to manipulate the JSON data.

  You can install `jq` using the following commands:

    - On Ubuntu/Debian:
      ```bash
      sudo apt-get install jq
      ```
    - On macOS (with Homebrew):
      ```bash
      brew install jq
      ```

### Usage

Ensure you have the Keycloak realm configuration file (in JSON format) that you want to clean. The file should be named `realm-config.json` or you can modify the script to use your desired file path.

### Download or Copy the Script

```bash
#!/bin/bash

INPUT_FILE="realm-config.json"
OUTPUT_FILE="keycloak-realm-export-minimal.json"

jq 'del(
  .id, .containerId, .accessTokenLifespanForImplicitFlow,
  .accessTokenLifespanForWebApps, .accessTokenLifespan, .offlineSessionIdleTimeout,
  .accessTokenLifespanInSeconds, .ssoSessionIdleTimeout, .ssoSessionMaxLifespan,
  .ssoSessionIdleTimeoutRememberMe, .ssoSessionMaxLifespanRememberMe,
  .accessCodeLifespan, .accessCodeLifespanLogin, .accessCodeLifespanUserAction,
  .accessCodeLifespanMobile, .notBefore, .registrationAllowed,
  .registrationEmailAsUsername, .rememberMe, .verifyEmail, .resetPasswordFlow,
  .editUsernameAllowed, .bruteForceProtected, .permanentLockout, .maxFailureWaitSeconds,
  .minimumQuickLoginWaitSeconds, .waitIncrementSeconds, .quickLoginCheckMilliSeconds,
  .maxDeltaTimeSeconds, .failureFactor, .requiredCredentials, .otpPolicyType,
  .otpPolicyAlgorithm, .otpPolicyInitialCounter, .otpPolicyDigits, .otpPolicyLookAheadWindow,
  .otpPolicyPeriod, .otpSupportedApplications, .webAuthnPolicyRpEntityName,
  .webAuthnPolicyAttestationConveyancePreference, .webAuthnPolicyAuthenticatorAttachment,
  .webAuthnPolicyRequireResidentKey, .webAuthnPolicyUserVerificationRequirement,
  .webAuthnPolicyCreateTimeout, .webAuthnPolicyAssertionTimeout,
  .webAuthnPolicyRegistrationRecoveryEnabled, .webAuthnPolicyRegistrationRecoveryCodesQuantity,
  .webAuthnPolicyRegistrationTokenBindingRequired, .webAuthnPolicyRegistrationAttestationConveyancePreference,
  .webAuthnPolicyRegistrationAuthenticatorSelectionCriteria, .keys
) 
| walk(if type == "object" then del(.id) else . end)' < "$INPUT_FILE" > "$OUTPUT_FILE"

echo "Minimal export saved to $OUTPUT_FILE"
```

### make the script executable
```bash
    chmod +x clean-realm-config.sh
```

Now execute the script, making sure that you have inputed the correct file paths.