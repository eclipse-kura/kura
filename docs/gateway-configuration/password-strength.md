# Password Strength Configuration

Eclipse Kura allows configuring and enforcing password strength requirements that are applied to new passwords, for example, when a user changes their password at first access.

The password strength-related settings can be configured in the Security -> Password Strength section of the Eclipse Kura web UI.

![Password Strength Options](./images/password-strength.png)

### Minimum password length

The minimum length to be enforced for new passwords. Set to 0 to disable. The default value is set to 8 characters.

### Require digits in new password

If set to true, new passwords will be accepted only if they contain at least one digit. The default value is false.

### Require special characters in new password

If set to true, new passwords will be accepted only if they contain at least one non-alphanumeric character. The default value is false.

### Require uppercase and lowercase characters in new passwords

If set to true, new passwords will be accepted only if they contain both uppercase and lowercase alphanumeric characters. The default value is false.