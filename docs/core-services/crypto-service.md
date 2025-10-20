# Crypto Service

The `CryptoService` interface in Eclipse Kura provides cryptographic utilities for encryption, decryption, hashing, Base64 encoding/decoding, and keystore password management.

**Purpose:**  
The interface centralizes cryptographic operations and keystore password management for Kura-based IoT applications, ensuring consistent, secure handling of sensitive data.
The interface is implemented by the `CryptoServiceImpl` class that provides a baseline reference for users to generate their own versions of the CryptoService if needed.
A notable use of the `CryptoService` is encrypting the configuration snapshot files and the configuration properties of PASSWORD type.

!!! note
    The default `CryptoServiceImpl` bundled with Kura uses a well-known, default encryption key when no secret is provided. This default is intended only for development and testing purposes. For production deployments, users MUST replace the default secret by setting the Java environment variable:

    - org.eclipse.kura.core.crypto.secretKey

    The secret key must be either 16, 24, or 32 bytes (characters) long. Example (set as JVM argument or environment variable depending on your runtime):

    - As JVM system property: -Dorg.eclipse.kura.core.crypto.secretKey=your-16+char-secret

    Failing to replace the default secret leaves encrypted data vulnerable. Ensure secrets are stored and managed securely in your environment.

    To implement an alternative mechanism for storing the key it is possible to replace the `org.eclipse.kura.core.crypto` bundle with a custom implementation.

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
