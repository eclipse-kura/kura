# Crypto Service

The `CryptoService` interface in Eclipse Kura provides cryptographic utilities for encryption, decryption, hashing, Base64 encoding/decoding, and keystore password management.

**Purpose:**  
The interface centralizes cryptographic operations and keystore password management for Kura-based IoT applications, ensuring consistent, secure handling of sensitive data.
The interface is implemented by the `CryptoServiceImpl` class that provides a baseline reference for users to generate their own versions of the CryptoService if needed.
A notable use of the `CryptoService` is encrypting the configuration snapshot files and the configuration properties of PASSWORD type.

## Encryption key

The default `CryptoServiceImpl` included with Kura utilizes a configurable encryption key for encryption purposes. When no secret key is provided, a commonly known default encryption key is used. This default should only be used in development and testing environments. For production deployments, it is **CRUCIAL** that users replace the default encryption key with a secure one.

!!! warning

    Failing to replace the default secret leaves encrypted data vulnerable. Ensure secrets are stored and managed securely in your environment.

The secret key must be either 16, 24, or 32 bytes (characters) long, it can be specified in the following ways:

### Using a Java system property

The encryption key can be configured with the following Java system property:

- `org.eclipse.kura.core.crypto.secretKey`

Kura default start scripts will set the system property above to the content of the `KURA_CRYPTO_SECRET_KEY` environment variable.

### Using systemd-credentials

Strating from Eclipse Kura 6.0, the default `CryptoService` implementation supports loading the encryption key from a [systemd credential](https://systemd.io/CREDENTIALS/) named `kura_encryption_key`.

It can be speficied using any of the methods supported by systemd. For example it can be stored in encrypted form in a configuration dropin of the `kura.service` unit, as shown below (adapted from Example 2 in [systemd-creds](https://www.freedesktop.org/software/systemd/man/latest/systemd-creds.html) man page)

```
mkdir -p /etc/systemd/system/kura.service.d
systemd-ask-password -n | ( echo "[Service]" && systemd-creds encrypt --name=kura_encryption_key -p - - ) >/etc/systemd/system/kura.service.d/50-encryption-key.conf
```

### Using a custom storage implementation

To implement an alternative mechanism for storing the key it is possible to replace the `org.eclipse.kura.core.crypto` bundle with a custom implementation.

!!! note

    The default CryptoService implementation will load the encryption key from the supported sources in the following order (higher priority first), falling back to the next one if the key cannot be loaded:

    1. Systemd credential
    2. Java system property
    3. Default well-known key


## Key Functional Areas

### AES Encryption/Decryption

- `encryptAes(char[] value)`: Encrypts a char array using AES.
- `decryptAes(char[] encryptedValue)`: Decrypts a char array using AES.
- `aesEncryptingStream(OutputStream destination)`: Returns an OutputStream that performs AES encryption.
- `aesDecryptingStream(InputStream source)`: Returns an InputStream that performs AES decryption.
- (Deprecated) `encryptAes(String value)` / `decryptAes(String encryptedValue)`: AES operations on Strings (deprecated, use char[] methods instead).

### Hash Generation

- `sha1Hash(String s)`: SHA-1 hash of a string.
- `sha256Hash(String s)`: SHA-256 hash of a string.
- `hash(String s, String algorithm)`: Hashes a string using the specified algorithm.

### Base64 Encoding/Decoding

- `encodeBase64(String stringValue)`: Encodes a string in Base64.
- `decodeBase64(String encodedValue)`: Decodes a Base64-encoded string.

### Keystore Password Management

- `getKeyStorePassword(String keyStorePath)`: Retrieves the password for a keystore.
- `setKeyStorePassword(String keyStorePath, char[] password)`: Stores a password for a keystore.
- (Deprecated) `setKeyStorePassword(String keyStorePath, String password)`: Stores a password for a keystore as a String.

### Security Mode

- `isFrameworkSecure()`: Checks if the Kura framework is running in security mode.
