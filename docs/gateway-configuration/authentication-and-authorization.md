# Authentication and authorization

Kura 5 introduces a centralized authentication and authorization framework based on the OSGi **UserAdmin** specification. This framework introduces the concepts of **identities** and **permissions**:

- **Identity**: A Kura identity is related to authentication, an identity has a name and a set of associated credentials, for example a password.

- **Permission**: A Kura permission is related to authorization. Zero or more permissions can be assigned to a given identity. Each permission allows to access a set of resources and/or perform certain operations. Permissions can be defined by applications.

Examples of applications that use the new authentication and authorization framework are the Web Console and the REST API framework:

- Kura Web Console provides multi user support, Web Console users are mapped to Kura identities, the Web Console also defines a set of permissions that allow to restrict the operations that a given identity is allowed to perform.
- The Kura REST API framework users are now mapped to Kura identities and REST roles are mapped to Kura permissions. The old `ConfigurationService` based user and role definition mechanism has been dropped.

The authentication and authorization framework only allows to define and store identities and permissions, it does not provide implementation of authentication methods and/or session management. These aspects are left to applications.

## Permission and identity representation

Permissions and identities are implemented on top of the **UserAdmin** **User** and **Group** concepts. See OSGi UserAdmin specification for more details on the **Role**, **User** and **Group** concepts.

## IdentityService Java APIs

Kura 5.5 introduces a new set of Java APIs that allow to manage Kura identities, the implementation is based on the **UserAdmin** conventions described in this page, and allows to manipulate Kura identities without interacting directly with the UserAdmin. Please refer to the [Javadoc](https://download.eclipse.org/kura/docs/api/5.5.0/apidocs/org/eclipse/kura/identity/package-frame.html) for more details.

The new APIs also provide the capability to implement and register [IdentityConfigurationExtension](https://download.eclipse.org/kura/docs/api/5.4.0/apidocs/org/eclipse/kura/identity/configuration/extension/IdentityConfigurationExtension.html)s in the framework.

An **IdentityConfigurationExtension** can define additional custom configuration parameters for each identity. The custom configuration can be inspected and modified in the Identities section of Kura Web UI and using the identity/v2 rest APIs and MQTT request handler. The  **IdentityConfigurationExtension** implementation can store the additional configuration in the way that is most suitable for the application, for example by adding custom **UserAdmin** properties or credentials.

### Identities

A Kura identity is represented as a **UserAdmin** **User** with the following properties:

- The **User** name must be in the `kura.user.${identity_name}` form where `${identity_name}` is a non empty string representing the identity name. The name of an **User** representing a Kura identity must start with the `kura.user.` prefix.

- A password may be assigned to an identity by defining a property in **User** **credentials** with the following format:
  - The property name must be `kura.password`
  - The property value must be a string containing SHA256 hash of the password, as computed by the
  ```
  String org.eclipse.kura.crypto.CryptoService.sha256Hash(String)
  ```
    method.

Starting from Kura 5.1, it is possible to force an identity to change the password at next login by setting the following property in **User** **properties**:
  - `kura.need.password.change` : `true` encoded as a JSON string.
  - The property will be cleared automatically after a successful password change on next login.

Starting from Kura 5.5 the following restrictions will be applied by the IdentityService:

  * New Identity Names:
    - must be at least 3 and at most 255 characters long.
    - can only be composed by one or more sequences of alphanumeric characters (`[A-Za-z0-9]+`) separated by the dot or underscore symbols, dot and underscore is not allowed at the beginning or at the end of the permission name, sequences of consecutive dots and/or underscores are not allowed (examples of valid names are `foo1.bAr`, `foo`, `a.b.c`, `foo.bar_baz`).
  * New Passwords:
    - cannot be empty.
    - must satisfy the password strenght requirements configured on the system.
    - the maximum allowed length is 255 characters.
    - cannot contain whitespace characters.

### Permissions

A Kura permission is represented as a **UserAdmin** **Group** with the following properties:

- The **Group** name must be in the `kura.permission.${permission_name}` form where `${permission_name}` is a non empty string representing the permission name. The name of a **Group** representing a Kura permission must start with the `kura.permission.` prefix.

- Assigning a permission to a specific identity can be done by adding the **User** representing the identity to the **basic members** of the **Group** representing the permission.

Starting from Kura 5.5 the following restrictions will be applied by the IdentityService to the name new permissions:

  - must be at least 3 and at most 255 characters long.
  - can only be composed by one or more sequences of alphanumeric characters (`[A-Za-z0-9]+`) separated by the dot symbol, the dot is not allowed at the beginning or at the end of the permission name (examples of valid permission names are `foo1.bAr`, `foo`, `a.b.c`).

## UserAdmin persistence

The `org.eclipse.kura.internal.useradmin.store.RoleRepositoryStoreImpl` Kura service allows to persist the UserAdmin state in Kura configuration snapshot, this includes the defined identities and permissions.

The following configuration properties are used to store the UserAdmin concepts in Json format:

- **Role configuration** (id **roles.config**):

  Stores the **UserAdmin** **Role**s, plain **Role**s are non used for representing Kura identities and permissions.  
  The value must be a JSON array of [Role](#role) elements.

- **User configuration** (id **users.config**):

  Stores the **UserAdmin** **User**s.  
  The value must be a JSON array of [User](#user) elements.

- **Group configuration** (id **groups.config**):

  Stores the **UserAdmin** **Group**s.  
  The value must be a JSON array of [Group](#group) elements.

### JSON representation details

This section describes the JSON format used in `org.eclipse.kura.internal.useradmin.store.RoleRepositoryStoreImpl` configuration.

#### UserAdminDictValue

A value appearing in UserAdmin dictionaries like properties and credentials

- **type** : `variant`  
  <br>**Variants**:

  - a String property
    - **type** : `string`
  - a byte\[] property
    - **type** : `array`, element description:  
      a byte of the array encoded as an unsigned integer
      - **type** : `number`

Examples:

```json
"A string property"
```

```json
[1, 2, 3, 4, 5]
```

#### UserAdminDict

A UserAdmin property dictionary, there are no well known property names, property values must be of [UserAdminDictValue](#useradmindictvalue) type

- **type** : `object`

Example:

```json
{
  "stringProperty": "A string property",
  "byteArrayProperty": [1, 2, 3, 4, 5]
}
```

#### Role

An object describing and **UserAdmin** **Role**

- **type** : `object`  
  <br>**Properties**:

- **name**  
    The role name
  - **type** : `string`

- **properties**
  - **optional** If the dictionary is empty
    - [UserAdminDict](#useradmindict)

#### User

An object describing and **UserAdmin** **User**

- **type** : `object`  
  <br>**Properties**:

- **name**  
    The role name
  - **type** : `string`

- **properties**
  - **optional** If the dictionary is empty
    - [UserAdminDict](#useradmindict)

- **credentials**
  - **optional** If the dictionary is empty
    - [UserAdminDict](#useradmindict)

Example:

```json
{
  "name": "kura.user.appadmin",
  "credentials": {
    "kura.password": "3hPckF8Zc+IF3pVineBvck3zJERUl8itosySULE1hpM="
  }
}
```

#### Group

An object describing and **UserAdmin** **Group**

- **type** : `object`  
  <br>**Properties**:

- **name**  
    The role name
  - **type** : `string`

- **properties**
  - **optional** If the dictionary is empty
    - [UserAdminDict](#useradmindict)

- **credentials**
  - **optional** If the dictionary is empty
    - [UserAdminDict](#useradmindict)

- **basicMembers**
  - **optional** If the list is empty  
    The list of the group basic members
    - **type** : `array`, element description:  
      A role name
      - **type** : `string`

- **requiredMembers**
  - **optional** If the list is empty  
    The list of the group required members
    - **type** : `array`, element description:  
      A role name
      - **type** : `string`

Example:

```json
{
  "name": "kura.permission.kura.wires.admin",
  "basicMembers": ["kura.user.appadmin"]
}
```