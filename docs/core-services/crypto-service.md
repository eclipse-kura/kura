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

The encryption key can be configured with the following Java system property:

- `org.eclipse.kura.core.crypto.secretKey`

Kura default start scripts (see `/opt/eclipse/kura/bin/`) will set the system property above to the content of the `KURA_CRYPTO_SECRET_KEY` environment variable.

#### Systemd deployments

A possible way to provide the key is creating an environment file like shown below. The commands must be run as root.

```sh
echo KURA_CRYPTO_SECRET_KEY=$(systemd-ask-password -n) > /etc/kura-key.conf
```

Review the content of the `/etc/kura-key.conf` to verify if it needs escaping as explained in the description of the `EnvironmentFile` parameter in [systemd documentation](https://www.freedesktop.org/software/systemd/man/latest/systemd.exec.html).

```sh
chmod 400 /etc/kura-key.conf
```

```sh
mkdir -p /etc/systemd/system/kura.service.d
```

```sh
( echo "[Service]" && echo "EnvironmentFile=/etc/kura-key.conf" ) >/etc/systemd/system/kura.service.d/50-key-env-file.conf
```

```sh
systemctl daemon-reload
```

The result can be verified with `systemctl cat kura`, it should produce an output similar to the following:

```sh
systemctl cat kura
# /lib/systemd/system/kura.service
[Unit]
Description=Kura
Wants=dbus.service
After=dbus.service

....
kura.service content
....

# /etc/systemd/system/kura.service.d/50-key-env-file.conf
[Service]
EnvironmentFile=/etc/kura-key.conf
```

#### Container based deployments

Specify the environment variable using the `--env` or `--env-file` command line option of the `docker`/`podman` `run` or `create` commands.

For additional information about Eclipse Kura containers see: https://hub.docker.com/r/eclipse/kura/

!!! note

    The default CryptoService implementation will load the encryption key from the Java system property first, falling back to the default well-known key if the property has not been specified or its content is not valid.

### Using a custom storage implementation

To implement an alternative mechanism for key storage, you can replace the `org.eclipse.kura.core.crypto` bundle with a custom implementation.

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
