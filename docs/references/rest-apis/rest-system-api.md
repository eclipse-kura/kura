!!! note
    This API can also be accessed via the RequestHandler with app-id: `SYS-V1`.

The `SystemService` APIs return properties that are divided into 3 categories:

- [**framework** properties](#framework-properties),
- **extended** properties, and
- [**kura** properties](#kura-properties).

Identities with `rest.system` permissions can access these APIs.

#### Framework properties

| Property name | Type |
| - | - |
| biosVersion | String |
| cpuVersion | String |
| deviceName | String |
| modelId | String |
| modelName | String |
| partNumber | String |
| platform | String |
| numberOfProcessors | Integer |
| totalMemory | Integer |
| freeMemory | Integer |
| serialNumber | String |
| javaHome | String |
| javaVendor | String |
| javaVersion | String |
| javaVmInfo | String |
| javaVmName | String |
| javaVmVersion | String |
| osArch | String |
| osDistro | String |
| osDistroVersion | String |
| osName | String |
| osVersion | String |
| isLegacyBluetoothBeaconScan | Boolean |
| isLegacyPPPLoggingEnabled | Boolean |
| primaryMacAddress | String |
| primaryNetworkInterfaceName | String |
| fileSeparator | String |
| firmwareVersion | String |
| kuraDataDirectory | String |
| kuraFrameworkConfigDirectory | String |
| kuraHomeDirectory | String |
| kuraMarketplaceCompatibilityVersion | String |
| kuraSnapshotsCount | Integer |
| kuraSnapshotsDirectory | String |
| kuraStyleDirectory | String |
| kuraTemporaryConfigDirectory | String |
| kuraUserConfigDirectory | String |
| kuraVersion | String |
| kuraHaveWebInterface | Boolean |
| kuraHaveNetAdmin | Boolean |
| kuraWifiTopChannel | Integer |
| kuraDefaultNetVirtualDevicesConfig | String |
| osgiFirmwareName | String |
| osgiFirmwareVersion | String |
| commandUser | String |
| commandZipMaxUploadNumber | Integer |
| commandZipMaxUploadSize | Integer |

#### Kura properties

| Property name | Type |
| - | - |
| kura.platform | String |
| org.osgi.framework.version | String |
| kura.user.config | String |
| kura.name | String |
| file.command.zip.max.number | String |
| kura.legacy.ppp.logging.enabled | String |
| kura.tmp | String |
| kura.packages | String |
| build.version | String |
| kura.log.download.journal.fields | String |
| kura.data | String |
| os.name | String |
| dpa.read.timeout | String |
| file.upload.size.max | String |
| console.device.management.service.ignore | String |
| kura.command.user | String |
| kura.device.name | String |
| kura.partNumber | String |
| kura.project | String |
| kura.company | String |
| java.home | String |
| version | String |
| kura.style.dir | String |
| kura.model.id | String |
| file.separator | String |
| kura.model.name | String |
| kura.serialNumber.provider | String |
| kura.have.web.inter | String |
| kura.legacy.bluetooth.beacon.scan | String |
| java.runtime.version | String |
| kura.bios.version | String |
| kura.marketplace.compatibility.version | String |
| kura.framework.config | String |
| kura.firmware.version | String |
| kura.plugins | String |
| os.version | String |
| kura.version | String |
| org.osgi.framework.vendor | String |
| java.runtime.name | String |
| kura.log.download.sources | String |
| os.distribution | String |
| java.vm.name | String |
| kura.primary.network.interface | String |
| kura.home | String |
| file.command.zip.max.size | String |
| os.arch | String |
| os.distribution.version | String |
| file.upload.in.memory.size.threshold | String |
| kura.net.virtual.devices.config | String |
| kura.snapshots | String |
| java.vm.info | String |
| java.vm.version | String |
| dpa.connection.timeout | String |
| build.number | String |
| ccs.status.notification.url | String |



## GET methods

#### Get framework properties

- Method: GET
- API PATH: `/services/system/v1/properties/framework`

##### Responses

- 200 OK status

```JSON
{
    "biosVersion": "N/A",
    "cpuVersion": "unknown",
    "deviceName": "raspberry",
    "modelId": "raspberry",
    "modelName": "raspberry",
    "partNumber": "raspberry",
    "platform": "aarch64",
    "numberOfProcessors": 4,
    "totalMemory": 506816,
    "freeMemory": 380379,
    "serialNumber": "10000000ba7c7bfd",
    "javaHome": "/usr/lib/jvm/java-8-openjdk-armhf/jre",
    "javaVendor": "OpenJDK Runtime Environment",
    "javaVersion": "1.8.0_312-8u312-b07-1+rpi1-b07",
    "javaVmInfo": "mixed mode",
    "javaVmName": "OpenJDK Client VM",
    "javaVmVersion": "25.312-b07",
    "osArch": "arm",
    "osDistro": "Linux",
    "osDistroVersion": "N/A",
    "osName": "Linux",
    "osVersion": "6.1.21-v8+ #1642 SMP PREEMPT Mon Apr  3 17:24:16 BST 2023",
    "isLegacyBluetoothBeaconScan": false,
    "isLegacyPPPLoggingEnabled": true,
    "primaryMacAddress": "E4:5F:01:35:7F:F4",
    "primaryNetworkInterfaceName": "eth0",
    "fileSeparator": "/",
    "firmwareVersion": "N/A",
    "kuraDataDirectory": "/opt/eclipse/kura/data",
    "kuraFrameworkConfigDirectory": "/opt/eclipse/kura/framework",
    "kuraHomeDirectory": "/opt/eclipse/kura",
    "kuraMarketplaceCompatibilityVersion": "5.4.0.SNAPSHOT",
    "kuraSnapshotsCount": 10,
    "kuraSnapshotsDirectory": "/opt/eclipse/kura/user/snapshots",
    "kuraStyleDirectory": "/opt/eclipse/kura/console/skin",
    "kuraTemporaryConfigDirectory": "/tmp/.kura",
    "kuraUserConfigDirectory": "/opt/eclipse/kura/user",
    "kuraVersion": "KURA_5.4.0-SNAPSHOT",
    "kuraHaveWebInterface": true,
    "kuraHaveNetAdmin": true,
    "kuraWifiTopChannel": 2147483647,
    "kuraDefaultNetVirtualDevicesConfig": "netIPv4StatusUnmanaged",
    "osgiFirmwareName": "Eclipse",
    "osgiFirmwareVersion": "1.10.0",
    "commandUser": "kura",
    "commandZipMaxUploadNumber": 1024,
    "commandZipMaxUploadSize": 100
}
```

- 500 Internal Server Error



#### Get extended properties

- Method: GET
- API PATH: `/services/system/v1/properties/extended`

##### Responses

- 200 OK status

```JSON
{
    "version": "1.0",
    "extendedProperties": {
        "Device Info 1": {
            "exampleKey1": "value1",
            "exampleKey2": "value2"
        },
        "Device Info 2": {
            "key1": "val1",
            "key2": "val2"
        }
    }
}
```

- 500 Internal Server Error



#### Get Kura properties

- Method: GET
- API PATH: `/services/system/v1/properties/kura`

##### Responses

- 200 OK status

```JSON
{
    "kuraProperties": {
        "kura.platform": "aarch64",
        "org.osgi.framework.version": "1.10.0",
        "kura.user.config": "/opt/eclipse/kura/user",
        "kura.name": "Eclipse Kura",
        "file.command.zip.max.number": "1024",
        "kura.legacy.ppp.logging.enabled": "true",
        "kura.tmp": "/tmp/.kura",
        "kura.packages": "/opt/eclipse/kura/packages",
        "build.version": "buildNumber",
        "kura.log.download.journal.fields": "SYSLOG_IDENTIFIER,PRIORITY,MESSAGE,STACKTRACE",
        "kura.data": "/opt/eclipse/kura/data",
        "os.name": "Linux",
        "dpa.read.timeout": "60000",
        "file.upload.size.max": "-1",
        "console.device.management.service.ignore": "org.eclipse.kura.net.admin.NetworkConfigurationService,org.eclipse.kura.net.admin.FirewallConfigurationService",
        "kura.command.user": "kura",
        "kura.device.name": "raspberry",
        "kura.partNumber": "raspberry",
        "kura.project": "generic-arm32",
        "kura.company": "EUROTECH",
        "java.home": "/usr/lib/jvm/java-8-openjdk-armhf/jre",
        "version": "5.4.0-SNAPSHOT",
        "kura.style.dir": "/opt/eclipse/kura/console/skin",
        "kura.model.id": "raspberry",
        "file.separator": "/",
        "kura.model.name": "raspberry",
        "kura.serialNumber.provider": "cat /proc/cpuinfo | grep Serial | cut -d ' ' -f 2",
        "kura.have.web.inter": "true",
        "kura.legacy.bluetooth.beacon.scan": "false",
        "java.runtime.version": "1.8.0_312-8u312-b07-1+rpi1-b07",
        "kura.bios.version": "N/A",
        "kura.marketplace.compatibility.version": "KURA_5.4.0-SNAPSHOT",
        "kura.framework.config": "/opt/eclipse/kura/framework",
        "kura.firmware.version": "N/A",
        "kura.plugins": "/opt/eclipse/kura/plugins",
        "os.version": "6.1.21-v8+ #1642 SMP PREEMPT Mon Apr  3 17:24:16 BST 2023",
        "kura.version": "KURA_5.4.0-SNAPSHOT",
        "org.osgi.framework.vendor": "Eclipse",
        "java.runtime.name": "OpenJDK Runtime Environment",
        "kura.log.download.sources": "/var/log",
        "os.distribution": "Linux",
        "java.vm.name": "OpenJDK Client VM",
        "kura.primary.network.interface": "eth0",
        "kura.home": "/opt/eclipse/kura",
        "file.command.zip.max.size": "100",
        "os.arch": "arm",
        "os.distribution.version": "N/A",
        "file.upload.in.memory.size.threshold": "10240",
        "kura.net.virtual.devices.config": "unmanaged",
        "kura.snapshots": "/opt/eclipse/kura/user/snapshots",
        "java.vm.info": "mixed mode",
        "java.vm.version": "25.312-b07",
        "dpa.connection.timeout": "60000",
        "build.number": "generic-arm32-buildNumber",
        "ccs.status.notification.url": "ccs:log"
    }
}
```

- 500 Internal Server Error



## POST methods

#### Filter framework properties

This method allows to retrieve framework-related properties by their name. Available properties are in [table Framework properties](#framework-properties).

- Method: POST
- API PATH: `/services/system/v1/properties/framework/filter`

##### Request Body

```JSON
{
    "names": ["deviceName", "numberOfProcessors", "kuraHaveNetAdmin"]
}
```

##### Responses

- 200 OK status

```JSON
{
    "deviceName": "RASBPERRY PI 4",
    "numberOfProcessors": 4,
    "kuraHaveNetAdmin": true
}
```

- 500 Internal Server Error



#### Filter extended properties

This method allows to retrieve the extended properties and to filter them by group name.

- Method: POST
- API PATH: `/services/system/v1/properties/extended/filter`

##### Request Body

```JSON
{
    "groupNames": ["Device Info 1"]
}
```

##### Responses

- 200 OK status

```JSON
{
    "extendedProperties": {
        "Device Info 1": {
            "exampleKey1": "value1",
            "exampleKey2": "value2"
        }
    }
}
```

- 500 Internal Server Error



#### Filter kura properties

This method allows to retrieve Kura-related properties (derived from the `kura.properties` file) and filter by their name. Available properties are listed in the [table Kura properties](#kura-properties).

- Method: POST
- API PATH: `/services/system/v1/properties/kura/filter`

##### Request Body

```JSON
{
    "names": ["kura.platform", "file.upload.size.max", "kura.have.net.admin"]
}
```

##### Responses

- 200 OK status

```JSON
{
    "kuraProperties": {
        "kura.platform": "aarch64",
        "kura.have.net.admin": "true",
        "file.upload.size.max": "-1"
    }
}
```

- 500 Internal Server Error