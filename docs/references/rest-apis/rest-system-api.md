!!! note

    This API can also be accessed via the RequestHandler with app-id: `SYS-V1`.

#### Get properties

- Method: GET
- API PATH: `/services/system/v1/properties`

##### Responses

- 200 OK status

```JSON
{
    "biosVersion": "not_defined",
    "cpuVersion": "not_defined",
    "deviceName": "raspberry",
    "modelId": "raspberry",
    "modelName": "raspberry",
    "partNumber": "raspberry",
    "platform": "aarch64",
    "numberOfProcessors": 4,
    "totalMemory": 506816,
    "freeMemory": 383985,
    "serialNumber": "not_defined",
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
    "isLegacyPPPLoggingEnabled": false,
    "primaryMacAddress": "E4:5F:01:35:7F:F4",
    "primaryNetworkInterfaceName": "eth0",
    "fileSeparator": "/",
    "firmwareVersion": "not_defined",
    "kuraDataDirectory": "/opt/eclipse/kura/data",
    "kuraFrameworkConfigDirectory": "/opt/eclipse/kura/framework",
    "kuraHomeDirectory": "/opt/eclipse/kura",
    "kuraMarketplaceCompatibilityVersion": "5.4.0.SNAPSHOT",
    "kuraSnapshotsCount": 10,
    "kuraSnapshotsDirectory": "/opt/eclipse/kura/user/snapshots",
    "kuraStyleDirectory": "/opt/eclipse/kura/console/skin",
    "kuraTemporaryConfigDirectory": "/tmp/.kura",
    "kuraUserConfigDirectory": "/opt/eclipse/kura/user",
    "kuraWebEnabled": "true",
    "kuraWifiTopChannel": 2147483647,
    "kuraDefaultNetVirtualDevicesConfig": "netIPv4StatusUnmanaged",
    "osgiFirmwareName": "Eclipse",
    "osgiFirmwareVersion": "1.10.0",
    "commandUser": "kura",
    "commandZipMaxUploadNumber": 1024,
    "commandZipMaxUploadSize": 100,
    "extendedProperties": {
        "version": "1.0",
        "extendedProperties": {
            "Device Management Info": {
                "dm_keystore_name": "DMKeystore",
                "dm_signature_verification": "DISABLED"
            },
            "Security Info": {
                "boot_state": "Normal",
                "maintenance_mode": "Unknown",
                "tamper_status": "UNSUPPORTED",
                "kura_operation_mode": "Development mode",
                "el_operation_mode": "Unknown"
            },
            "Origination Info": {
                "device_id": "Unknown"
            }
        }
    }
}
```

- 500 Internal Server Error

#### Filter properties by property names

- Method: POST
- API PATH: `/services/system/v1/properties/filter`

##### Request Body

```JSON
{
    "names": ["deviceName", "serialNumber", "osArch"]
}
```

##### Responses

- 200 OK status

```JSON
{
    "deviceName": "raspberry",
    "serialNumber": "not_defined",
    "osArch": "arm"
}
```

- 500 Internal Server Error

#### Get bundles

- Method: GET
- API PATH: `/services/system/v1/bundles`

##### Responses

- 200 OK status

```JSON
{
    "bundles": [
        {
            "bundleId": 0,
            "location": "System Bundle",
            "state": "ACTIVE",
            "symbolicName": "org.eclipse.osgi",
            "version": "3.16.0.v20200828-0759",
            "signed": true,
            "lastModified": 1692787497214
        },
        {
            "bundleId": 1,
            "location": "initial@reference:file:plugins/org.eclipse.equinox.cm_1.4.400.v20200422-1833.jar",
            "state": "ACTIVE",
            "symbolicName": "org.eclipse.equinox.cm",
            "version": "1.4.400.v20200422-1833",
            "signed": true,
            "lastModified": 1692787494805
        },
        {
            "bundleId": 2,
            "location": "initial@reference:file:plugins/org.eclipse.equinox.common_3.13.0.v20200828-1034.jar",
            "state": "ACTIVE",
            "symbolicName": "org.eclipse.equinox.common",
            "version": "3.13.0.v20200828-1034",
            "signed": true,
            "lastModified": 1692787494830
        },

        ...
        
		{
            "bundleId": 207,
            "location": "initial@reference:file:plugins/org.tigris.mtoolkit.iagent.rpc_3.0.0.20110411-0918.jar",
            "state": "ACTIVE",
            "symbolicName": "org.tigris.mtoolkit.iagent.rpc",
            "version": "3.0.0.20110411-0918",
            "signed": false,
            "lastModified": 1692787497214
        }
    ]
}
```

- 500 Internal Server Error
