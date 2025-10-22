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

### Option 1: Using systemd-credentials (recommended)

!!! note
    systemd-credentials is available starting from systemd version 250. This method cannot be used on container based deployments.

Strating from Eclipse Kura 6.0, the default `CryptoService` implementation supports loading the encryption key from a [systemd credential](https://systemd.io/CREDENTIALS/) named `kura_encryption_key`.

It can be speficied using any of the methods supported by systemd. For example it can be stored in encrypted form in a configuration dropin of the `kura.service` unit, as shown below (adapted from Example 2 in [systemd-creds](https://www.freedesktop.org/software/systemd/man/latest/systemd-creds.html) man page).

The commands should be run as root.

```sh
mkdir -p /etc/systemd/system/kura.service.d
```

```sh
systemd-ask-password -n | ( echo "[Service]" && systemd-creds encrypt --name=kura_encryption_key -p - - ) >/etc/systemd/system/kura.service.d/50-encryption-key.conf
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

# /etc/systemd/system/kura.service.d/50-encryption-key.conf
[Service]
SetCredentialEncrypted=kura_encryption_key: \
        Whxqht+dQJax1aZeCGLxmiAAAAABAAAADAAAABAAAADQfAnaQJMAVKCEJjcAAAAASk3/B \
        EZuKkHQPNKDXe7zn68bjyhzE7ni2R+g2B9o9aWrtMT9OGztsK+WbpsjTr8ci4FKcFL/dd \
        B1nKZ+O2Zt1Q==

```

!!! note

    The setup above is just an example, systemd offers different ways to pass credentials to services. Please review systemd documentation to understand the different options and chose the most appropriate for your case.

### Using a Java system property

The encryption key can be configured with the following Java system property:

- `org.eclipse.kura.core.crypto.secretKey`

Kura default start scripts (see `/opt/eclipse/kura/bin/`) will set the system property above to the content of the `KURA_CRYPTO_SECRET_KEY` environment variable.

#### Systemd deployments

!!! note

    Consider using systemd-credentials if available on your system

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

### Using a custom storage implementation

To implement an alternative mechanism for key storage, you can replace the `org.eclipse.kura.core.crypto` bundle with a custom implementation.

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
