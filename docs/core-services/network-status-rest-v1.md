# Network Status Service V1 REST APIs and MQTT Request Handler
The NET-STATUS-V1 cloud request handler and the corresponding REST APIs allow to retrieve the current status of the network interfaces available on the system.

This cloud request handler and rest API is available only on the systems that provide a NetworkStatusService implementation, at the moment this corresponds to the devices that include the NetworkManager integration.

Accessing the REST APIs requires to use an identity with the `rest.network.status` permission assigned.

  * [Request definitions](#request-definitions)
    * [GET/interfaceIds](#getinterfaceids)
    * [GET/status](#getstatus)
    * [POST/status/byInterfaceId](#poststatusbyinterfaceid)
  * [JSON definitions](#json-definitions)
    * [InterfaceIds](#interfaceids)
    * [InterfaceStatusList](#interfacestatuslist)
    * [LoopbackInterfaceStatus](#loopbackinterfacestatus)
    * [EthernetInterfaceStatus](#ethernetinterfacestatus)
    * [WifiInterfaceStatus](#wifiinterfacestatus)
    * [ModemInterfaceStatus](#modeminterfacestatus)
    * [NetworkInterfaceStatus](#networkinterfacestatus)
    * [IPAddressString](#ipaddressstring)
    * [HardwareAddress](#hardwareaddress)
    * [NetworkInterfaceIpAddress](#networkinterfaceipaddress)
    * [NetworkInterfaceIpAddressStatus](#networkinterfaceipaddressstatus)
    * [WifiChannel](#wifichannel)
    * [WifiAccessPoint](#wifiaccesspoint)
    * [ModemModePair](#modemmodepair)
    * [Sim](#sim)
    * [Bearer](#bearer)
    * [ModemPorts](#modemports)
    * [NetworkInterfaceType](#networkinterfacetype)
    * [NetworkInterfaceState](#networkinterfacestate)
    * [WifiCapability](#wificapability)
    * [WifiMode](#wifimode)
    * [WifiSecurity](#wifisecurity)
    * [ModemPortType](#modemporttype)
    * [ModemCapability](#modemcapability)
    * [ModemMode](#modemmode)
    * [ModemBand](#modemband)
    * [SimType](#simtype)
    * [ESimStatus](#esimstatus)
    * [BearerIpType](#beareriptype)
    * [ModemConnectionType](#modemconnectiontype)
    * [ModemConnectionStatus](#modemconnectionstatus)
    * [AccessTechnology](#accesstechnology)
    * [RegistrationStatus](#registrationstatus)
    * [FailureReport](#failurereport)
    * [GenericFailureReport](#genericfailurereport)

## Request definitions
### GET/interfaceIds
  * **REST API path** : /services/networkStatus/v1/interfaceIds
  * **description** : Returns the identifiers of the network interfaces detected in the system. For Ethernet and WiFi interfaces, the identifier is typically the interface name. For the modems, instead, it is the usb or pci path.
  * **responses** :
      * **200**
          * **description** : The list of the identifiers of the network interfaces detected in the system.
          * **response body** :
              * [InterfaceIds](#interfaceids)
      * **500**
          * **description** : If an unexpected failure occurs while retrieving the interface list.
          * **response body** :
              * [GenericFailureReport](#genericfailurereport)

### GET/status
  * **REST API path** : /services/networkStatus/v1/status
  * **description** : Returns the status for all interfaces currently available on the system. Failures in retrieving the status of specific interfaces will be reported using the `failures` field of the response.
  * **responses** :
      * **200**
          * **description** : The status of the network interfaces in the system.
          * **response body** :
              * [InterfaceStatusList](#interfacestatuslist)
      * **500**
          * **description** : If an unexpected failure occurs while retrieving the interface list.
          * **response body** :
              * [GenericFailureReport](#genericfailurereport)

### POST/status/byInterfaceId
  * **REST API path** : /services/networkStatus/v1/status/byInterfaceId
  * **description** : Returns the status for the network interfaces whose id is specified in the request. Failures in retrieving the status of specific interfaces, or the fact that an interface with the requested id cannot be found will be reported using the `failures` field of the responses.
  * **request body** :
      * [InterfaceIds](#interfaceids)
  * **responses** :
      * **200**
          * **description** : The status of the network interfaces in the system.
          * **response body** :
              * [InterfaceStatusList](#interfacestatuslist)
      * **400**
          * **description** : If the request object does not contain the `interfaceIds` field, it is `null` or if it contains empty or `null` interface ids.
          * **response body** :
              * [GenericFailureReport](#genericfailurereport)
      * **500**
          * **description** : If an unexpected failure occurs while retrieving the interface list.
          * **response body** :
              * [GenericFailureReport](#genericfailurereport)

## JSON definitions
### InterfaceIds
An object containing a list of network interface identifiers

<br>**Properties**:

  * **interfaceIds**: `array` 
      The list of network interface identifiers

      * array element type: `string`
          An interface identifier

  

```json
{
  "interfaceIds": [
    "lo"
  ]
}
```
### InterfaceStatusList
An object reporting a list of network interface status. If a failure occurs retrieving the status for a specific interface, the reason will be reported in the `failures` list.

<br>**Properties**:

  * **interfaces**: `array` 
      A list of network interface status

      * array element type: `variant`
          * **Variants**:
              * **object**
                  * [LoopbackInterfaceStatus](#loopbackinterfacestatus)
              * **object**
                  * [EthernetInterfaceStatus](#ethernetinterfacestatus)
              * **object**
                  * [WifiInterfaceStatus](#wifiinterfacestatus)
              * **object**
                  * [ModemInterfaceStatus](#modeminterfacestatus)
  
  * **failures**: `array` 
      * array element type: `object`
          * [FailureReport](#failurereport)
  

```json
{
  "failures": [],
  "interfaces": [
    {
      "autoConnect": true,
      "driver": "unknown",
      "driverVersion": "",
      "firmwareVersion": "",
      "hardwareAddress": "00:00:00:00:00:00",
      "id": "lo",
      "interfaceIp4Addresses": {
        "addresses": [
          {
            "address": "127.0.0.1",
            "prefix": 8
          }
        ],
        "dnsServerAddresses": []
      },
      "interfaceName": "lo",
      "mtu": 65536,
      "state": "UNMANAGED",
      "type": "LOOPBACK",
      "virtual": true
    }
  ]
}
```
```json
{
  "failures": [
    {
      "interfaceId": "foo",
      "reason": "Not found."
    }
  ],
  "interfaces": []
}
```
### LoopbackInterfaceStatus
Object that contains specific properties to describe the status of an Loopback interface. It contains also all of the properties specified by [NetworkInterfaceStatus](#networkinterfacestatus).

<br>**Properties**:


```json
{
  "autoConnect": true,
  "driver": "unknown",
  "driverVersion": "",
  "firmwareVersion": "",
  "hardwareAddress": "00:00:00:00:00:00",
  "id": "lo",
  "interfaceIp4Addresses": {
    "addresses": [
      {
        "address": "127.0.0.1",
        "prefix": 8
      }
    ],
    "dnsServerAddresses": []
  },
  "interfaceName": "lo",
  "mtu": 65536,
  "state": "UNMANAGED",
  "type": "LOOPBACK",
  "virtual": true
}
```
### EthernetInterfaceStatus
Object that contains specific properties to describe the status of an Ethernet interface. It contains also all of the properties specified by [NetworkInterfaceStatus](#networkinterfacestatus).

<br>**Properties**:

  * **linkUp**: `bool` 
  

```json
{
  "autoConnect": true,
  "driver": "igb",
  "driverVersion": "5.6.0-k",
  "firmwareVersion": "3.25, 0x800005d0",
  "hardwareAddress": "00:E0:C7:0A:5F:89",
  "id": "eno1",
  "interfaceIp4Addresses": {
    "addresses": [
      {
        "address": "172.16.0.1",
        "prefix": 24
      }
    ],
    "dnsServerAddresses": [],
    "gateway": "0.0.0.0"
  },
  "interfaceName": "eno1",
  "linkUp": true,
  "mtu": 1500,
  "state": "ACTIVATED",
  "type": "ETHERNET",
  "virtual": false
}
```
### WifiInterfaceStatus
Object that contains specific properties to describe the status of a WiFi interface. It contains also all of the properties specified by [NetworkInterfaceStatus](#networkinterfacestatus).

<br>**Properties**:

  * **capabilities**: `array` 
      * array element type: `string (enumerated)`
          * [WifiCapability](#wificapability)
  
  * **channels**: `array` 
      * array element type: `object`
          * [WifiChannel](#wifichannel)
  
  * **countryCode**: `string` 
  
  * **mode**: `string (enumerated)` 
      * [WifiMode](#wifimode)
  
  * **activeWifiAccessPoint**: `object` (**optional**)
      * [WifiAccessPoint](#wifiaccesspoint)
  
  * **availableWifiAccessPoints**: `array` 
      * array element type: `object`
          * [WifiAccessPoint](#wifiaccesspoint)
  

```json
{
  "activeWifiAccessPoint": {
    "channel": {
      "channel": 11,
      "frequency": 2462
    },
    "hardwareAddress": "11:22:33:44:55:66",
    "maxBitrate": 130000,
    "mode": "INFRA",
    "rsnSecurity": [
      "GROUP_CCMP",
      "KEY_MGMT_PSK",
      "PAIR_CCMP"
    ],
    "signalQuality": 100,
    "signalStrength": -20,
    "ssid": "MyAccessPoint",
    "wpaSecurity": [
      "NONE"
    ]
  },
  "autoConnect": true,
  "availableWifiAccessPoints": [
    {
      "channel": {
        "channel": 11,
        "frequency": 2462
      },
      "hardwareAddress": "11:22:33:44:55:66",
      "maxBitrate": 130000,
      "mode": "INFRA",
      "rsnSecurity": [
        "GROUP_CCMP",
        "KEY_MGMT_PSK",
        "PAIR_CCMP"
      ],
      "signalQuality": 100,
      "signalStrength": -20,
      "ssid": "MyAccessPoint",
      "wpaSecurity": [
        "NONE"
      ]
    },
    {
      "channel": {
        "channel": 5,
        "frequency": 2432
      },
      "hardwareAddress": "22:33:44:55:66:77",
      "maxBitrate": 270000,
      "mode": "INFRA",
      "rsnSecurity": [
        "GROUP_CCMP",
        "KEY_MGMT_PSK",
        "PAIR_CCMP"
      ],
      "signalQuality": 42,
      "signalStrength": -69,
      "ssid": "OtherSSID",
      "wpaSecurity": [
        "NONE"
      ]
    }
  ],
  "capabilities": [
    "CIPHER_WEP40",
    "WPA",
    "AP",
    "FREQ_VALID",
    "ADHOC",
    "RSN",
    "CIPHER_TKIP",
    "CIPHER_WEP104",
    "CIPHER_CCMP",
    "FREQ_2GHZ"
  ],
  "channels": [
    {
      "attenuation": 20.0,
      "channel": 1,
      "disabled": false,
      "frequency": 2412,
      "noInitiatingRadiation": false,
      "radarDetection": false
    },
    {
      "attenuation": 20.0,
      "channel": 2,
      "disabled": false,
      "frequency": 2417,
      "noInitiatingRadiation": false,
      "radarDetection": false
    }
  ],
  "countryCode": "IT",
  "driver": "brcmfmac",
  "driverVersion": "7.45.98.94",
  "firmwareVersion": "01-3b33decd",
  "hardwareAddress": "44:55:66:77:88:99",
  "id": "wlan0",
  "interfaceIp4Addresses": {
    "addresses": [
      {
        "address": "192.168.0.113",
        "prefix": 24
      }
    ],
    "dnsServerAddresses": []
  },
  "interfaceName": "wlan0",
  "mode": "INFRA",
  "mtu": 1500,
  "state": "ACTIVATED",
  "type": "WIFI",
  "virtual": false
}
```
```json
{
  "activeWifiAccessPoint": {
    "channel": {
      "channel": 1,
      "frequency": 2412
    },
    "hardwareAddress": "44:55:66:77:88:99",
    "maxBitrate": 0,
    "mode": "INFRA",
    "rsnSecurity": [
      "GROUP_CCMP",
      "KEY_MGMT_PSK",
      "PAIR_CCMP"
    ],
    "signalQuality": 0,
    "signalStrength": -104,
    "ssid": "kura_gateway_raspberry_pi",
    "wpaSecurity": [
      "NONE"
    ]
  },
  "autoConnect": true,
  "availableWifiAccessPoints": [
    {
      "channel": {
        "channel": 1,
        "frequency": 2412
      },
      "hardwareAddress": "44:55:66:77:88:99",
      "maxBitrate": 0,
      "mode": "INFRA",
      "rsnSecurity": [
        "GROUP_CCMP",
        "KEY_MGMT_PSK",
        "PAIR_CCMP"
      ],
      "signalQuality": 0,
      "signalStrength": -104,
      "ssid": "kura_gateway_raspberry_pi",
      "wpaSecurity": [
        "NONE"
      ]
    }
  ],
  "capabilities": [
    "CIPHER_WEP40",
    "WPA",
    "AP",
    "FREQ_VALID",
    "ADHOC",
    "RSN",
    "CIPHER_TKIP",
    "CIPHER_WEP104",
    "CIPHER_CCMP",
    "FREQ_2GHZ"
  ],
  "channels": [
    {
      "attenuation": 20.0,
      "channel": 1,
      "disabled": false,
      "frequency": 2412,
      "noInitiatingRadiation": false,
      "radarDetection": false
    },
    {
      "attenuation": 20.0,
      "channel": 2,
      "disabled": false,
      "frequency": 2417,
      "noInitiatingRadiation": false,
      "radarDetection": false
    }
  ],
  "countryCode": "00",
  "driver": "brcmfmac",
  "driverVersion": "7.45.98.94",
  "firmwareVersion": "01-3b33decd",
  "hardwareAddress": "44:55:66:77:88:99",
  "id": "wlan0",
  "interfaceIp4Addresses": {
    "addresses": [
      {
        "address": "172.16.1.1",
        "prefix": 24
      }
    ],
    "dnsServerAddresses": []
  },
  "interfaceName": "wlan0",
  "mode": "MASTER",
  "mtu": 1500,
  "state": "ACTIVATED",
  "type": "WIFI",
  "virtual": false
}
```
### ModemInterfaceStatus
Object that contains specific properties to describe the status of a Modem interface. It contains also all of the properties specified by [NetworkInterfaceStatus](#networkinterfacestatus).

<br>**Properties**:

  * **model**: `string` 
  
  * **manufacturer**: `string` 
  
  * **serialNumber**: `string` 
  
  * **softwareRevision**: `string` 
  
  * **hardwareRevision**: `string` 
  
  * **primaryPort**: `string` 
  
  * **ports**: `object` 
      * [ModemPorts](#modemports)
  
  * **supportedModemCapabilities**: `array` 
      * array element type: `string (enumerated)`
          * [ModemCapability](#modemcapability)
  
  * **currentModemCapabilities**: `array` 
      * array element type: `string (enumerated)`
          * [ModemCapability](#modemcapability)
  
  * **powerState**: `string (enumerated)` 
      * [ModemPowerState](#modempowerstate)
  
  * **supportedModes**: `array` 
      * array element type: `object`
          * [ModemModePair](#modemmodepair)
  
  * **currentModes**: `object` 
      * [ModemModePair](#modemmodepair)
  
  * **supportedBands**: `array` 
      * array element type: `string (enumerated)`
          * [ModemBand](#modemband)
  
  * **currentBands**: `array` 
      * array element type: `string (enumerated)`
          * [ModemBand](#modemband)
  
  * **gpsSupported**: `bool` 
  
  * **availableSims**: `array` 
      * array element type: `object`
          * [Sim](#sim)
  
  * **simLocked**: `bool` 
  
  * **bearers**: `array` 
      * array element type: `object`
          * [Bearer](#bearer)
  
  * **connectionType**: `string (enumerated)` 
      * [ModemConnectionType](#modemconnectiontype)
  
  * **connectionStatus**: `string (enumerated)` 
      * [ModemConnectionStatus](#modemconnectionstatus)
  
  * **accessTechnologies**: `array` 
      * array element type: `string (enumerated)`
          * [AccessTechnology](#accesstechnology)
  
  * **signalQuality**: `number` 
  
  * **signalStrength**: `number` 
  
  * **registrationStatus**: `string (enumerated)` 
      * [RegistrationStatus](#registrationstatus)
  
  * **operatorName**: `string` 
  

```json
{
  "accessTechnologies": [
    "LTE"
  ],
  "autoConnect": true,
  "availableSims": [
    {
      "active": true,
      "primary": true,
      "eSimStatus": "UNKNOWN",
      "eid": "",
      "iccid": "1111111111111111111",
      "imsi": "111111111111111",
      "operatorName": "MyOperator",
      "operatorIdentifier": "12345",
      "simType": "PHYSICAL"
    }
  ],
  "bearers": [
    {
      "apn": "apn.myoperator.com",
      "bytesReceived": 0,
      "bytesTransmitted": 0,
      "connected": true,
      "ipTypes": [
        "IPV4"
      ],
      "name": "wwp11s0f2u3i4"
    }
  ],
  "connectionStatus": "CONNECTED",
  "connectionType": "DirectIP",
  "currentBands": [
    "DCS",
    "EUTRAN_3",
    "EUTRAN_19",
    "EUTRAN_40",
    "EUTRAN_26",
    "EUTRAN_28",
    "EUTRAN_41",
    "UTRAN_6",
    "EUTRAN_13",
    "EUTRAN_25",
    "EUTRAN_5",
    "EUTRAN_7",
    "EUTRAN_8",
    "UTRAN_2",
    "EUTRAN_38",
    "UTRAN_1",
    "EUTRAN_12",
    "UTRAN_8",
    "EUTRAN_18",
    "UTRAN_19",
    "G850",
    "EUTRAN_20",
    "UTRAN_5",
    "EUTRAN_1",
    "EUTRAN_39",
    "EUTRAN_2",
    "EUTRAN_4",
    "EGSM",
    "PCS",
    "UTRAN_4"
  ],
  "currentModemCapabilities": [
    "GSM_UMTS",
    "LTE"
  ],
  "currentModes": {
    "modes": [
      "MODE_2G",
      "MODE_3G",
      "MODE_4G"
    ],
    "preferredMode": "MODE_4G"
  },
  "driver": "qmi_wwan, option1",
  "driverVersion": "",
  "firmwareVersion": "",
  "gpsSupported": true,
  "hardwareAddress": "00:00:00:00:00:00",
  "hardwareRevision": "10000",
  "id": "1-3",
  "interfaceIp4Addresses": {
    "addresses": [
      {
        "address": "1.2.3.4",
        "prefix": 30
      }
    ],
    "dnsServerAddresses": [
      "1.2.3.6",
      "1.2.3.7"
    ],
    "gateway": "1.2.3.5"
  },
  "interfaceName": "wwp11s0f2u3i4",
  "manufacturer": "QUALCOMM INCORPORATED",
  "model": "QUECTEL Mobile Broadband Module",
  "mtu": 1500,
  "operatorName": "MyOperator",
  "ports": {
    "cdc-wdm0": "QMI",
    "ttyUSB0": "QCDM",
    "ttyUSB1": "GPS",
    "ttyUSB2": "AT",
    "ttyUSB3": "AT",
    "wwp11s0f2u3i4": "NET"
  },
  "powerState": "ON",
  "primaryPort": "cdc-wdm0",
  "registrationStatus": "HOME",
  "serialNumber": "111111111111111",
  "signalQuality": 55,
  "signalStrength": -80,
  "simLocked": true,
  "softwareRevision": "EG25GGBR07A08M2G",
  "state": "ACTIVATED",
  "supportedBands": [
    "DCS",
    "EUTRAN_3",
    "EUTRAN_19",
    "EUTRAN_40",
    "EUTRAN_26",
    "EUTRAN_28",
    "EUTRAN_41",
    "UTRAN_6",
    "EUTRAN_13",
    "EUTRAN_25",
    "EUTRAN_5",
    "EUTRAN_7",
    "EUTRAN_8",
    "UTRAN_2",
    "EUTRAN_38",
    "UTRAN_1",
    "EUTRAN_12",
    "UTRAN_8",
    "EUTRAN_18",
    "UTRAN_19",
    "G850",
    "EUTRAN_20",
    "UTRAN_5",
    "EUTRAN_1",
    "EUTRAN_39",
    "EUTRAN_2",
    "EUTRAN_4",
    "EGSM",
    "PCS",
    "UTRAN_4"
  ],
  "supportedModemCapabilities": [
    "NONE"
  ],
  "supportedModes": [
    {
      "modes": [
        "MODE_4G"
      ],
      "preferredMode": "NONE"
    },
    {
      "modes": [
        "MODE_2G",
        "MODE_3G",
        "MODE_4G"
      ],
      "preferredMode": "MODE_4G"
    }
  ],
  "type": "MODEM",
  "virtual": false
}
```
### NetworkInterfaceStatus
This object contains the common properties contained by the status object reported for a network interface. A network interface is identified by Kura using the id field. It is used to internally manage the interface. The interfaceName, instead, is the IP interface as it may appear on the system. For Ethernet and WiFi interfaces the two values coincide (i.e. eth0, wlp1s0, ...). For modems, instead, the id is typically the usb or pci path, while the interfaceName is the IP interface created when they are connected. When the modem is disconnected the interfaceName can have a different value.

<br>**Properties**:

  * **id**: `string` 
  
  * **interfaceName**: `string` 
  
  * **hardwareAddress**: `string` 
      * [HardwareAddress](#hardwareaddress)
  
  * **driver**: `string` 
  
  * **driverVersion**: `string` 
  
  * **firmwareVersion**: `string` 
  
  * **virtual**: `bool` 
  
  * **state**: `string (enumerated)` 
      * [NetworkInterfaceState](#networkinterfacestate)
  
  * **autoConnect**: `bool` 
  
  * **mtu**: `number` 
  
  * **interfaceIp4Addresses**: `array` (**optional**)
      * array element type: `object`
          * [NetworkInterfaceIpAddressStatus](#networkinterfaceipaddressstatus)
  
  * **interfaceIp6Addresses**: `array` (**optional**)
      * array element type: `object`
          * [NetworkInterfaceIpAddressStatus](#networkinterfaceipaddressstatus)
  

### IPAddressString
Represents an [IPv4](https://docs.oracle.com/javase/7/docs/api/java/net/Inet4Address.html#format) or [IPv6](https://docs.oracle.com/javase/7/docs/api/java/net/Inet6Address.html#format) addresses as a string.


### HardwareAddress
Represents an hardware address as its bytes reported as two characters hexadecimal strings separated by the ':' character. e.g. 00:11:22:33:A4:FC:BB.


### NetworkInterfaceIpAddress
This object describes an IP address with its prefix. It can be used for IPv4 or IPv6 addresses.

<br>**Properties**:

  * **address**: `string` 
      * [IPAddressString](#ipaddressstring)
  
  * **prefix**: `number` 
  

### NetworkInterfaceIpAddressStatus
This class describes the IP address status of a network interface: a list of IP addresses, an optional gateway and a list of DNS servers address. It can be used for IPv4 or IPv6 addresses.

<br>**Properties**:

  * **addresses**: `array` 
      * array element type: `object`
          * [NetworkInterfaceIpAddress](#networkinterfaceipaddress)
  
  * **gateway**: `string` (**optional** This field can be missing if the interface has no gateway.)
      * [IPAddressString](#ipaddressstring)
  
  * **dnsServerAddresses**: `array` 
      * array element type: `string`
          * [IPAddressString](#ipaddressstring)
  

### WifiChannel
This class represent a WiFi channel, providing the channel number, frequency, status and other useful information.

<br>**Properties**:

  * **channel**: `number` 
  
  * **frequency**: `number` 
  
  * **disabled**: `bool` (**optional**)
  
  * **attenuation**: `number` (**optional**)
  
  * **noInitiatingRadiation**: `bool` (**optional**)
  
  * **radarDetection**: `bool` (**optional**)
  

### WifiAccessPoint
This object describes a Wifi Access Point. It can be used both for describing a detected AP after a WiFi scan when in Station mode and the provided AP when in Master (or Access Point) mode.

<br>**Properties**:

  * **ssid**: `string` 
      The Service Set IDentifier of the WiFi network

  
  * **hardwareAddress**: `string` 
      * [HardwareAddress](#hardwareaddress)
  
  * **channel**: `object` 
      * [WifiChannel](#wifichannel)
  
  * **mode**: `string (enumerated)` 
      * [WifiMode](#wifimode)
  
  * **maxBitrate**: `number` 
      The maximum bitrate this access point is capable of.

  
  * **signalQuality**: `number` 
      The current signal quality of the access point in percentage.

  
  * **signalStrength**: `number` 
      The current signal strength of the access point in dBm.

  
  * **wpaSecurity**: `array` 
      The WPA capabilities of the access point

      * array element type: `string (enumerated)`
          * [WifiSecurity](#wifisecurity)
  
  * **rsnSecurity**: `array` 
      The RSN capabilities of the access point

      * array element type: `string (enumerated)`
          * [WifiSecurity](#wifisecurity)
  

### ModemModePair
This object represents a pair of Modem Mode list and a preferred one.

<br>**Properties**:

  * **modes**: `array` 
      * array element type: `string (enumerated)`
          * [ModemMode](#modemmode)
  
  * **preferredMode**: `string (enumerated)` 
      * [ModemMode](#modemmode)
  

### Sim
This class contains all relevant properties to describe a SIM (Subscriber Identity Module).

<br>**Properties**:

  * **active**: `bool` 

  * **primary**: `bool`
  
  * **iccid**: `string` 
  
  * **imsi**: `string` 
  
  * **eid**: `string` 
  
  * **operatorName**: `string` 

  * **operatorIdentifier**: `string` 
  
  * **simType**: `string (enumerated)` 
      * [SimType](#simtype)
  
  * **eSimStatus**: `string (enumerated)` 
      * [ESimStatus](#esimstatus)
  

### Bearer
This object describes the Bearer or Context associated to a modem connection.

<br>**Properties**:

  * **name**: `string` 
  
  * **connected**: `bool` 
  
  * **apn**: `string` 
  
  * **ipTypes**: `array` 
      * array element type: `string (enumerated)`
          * [BearerIpType](#beareriptype)
  
  * **bytesTransmitted**: `number` 
  
  * **bytesReceived**: `number` 
  

### ModemPorts
An object representing a set of modem ports. The members of this object represent modem ports, the member names represent the modem port name. This object can have a variable number of members.

<br>**Properties**:

  * **propertyName**: `string (enumerated)` 
      * [ModemPortType](#modemporttype)
  

### NetworkInterfaceType
The type of a network interface.

  * Possible values
      * `UNKNOWN`: The device type is unknown
      * `ETHERNET`: The device is a wired Ethernet device.
      * `WIFI`: The device is an 802.11 WiFi device.
      * `UNUSED1`
      * `UNUSED2`
      * `BT`: The device is a Bluetooth device.
      * `OLPC_MESH`: The device is an OLPC mesh networking device.
      * `WIMAX`: The device is an 802.16e Mobile WiMAX device.
      * `MODEM`: The device is a modem supporting one or more of analog telephone, CDMA/EVDO, GSM/UMTS/HSPA, or LTE standards to access a cellular or wireline data network.
      * `INFINIBAND`: The device is an IP-capable InfiniBand interface.
      * `BOND`: The device is a bond master interface.
      * `VLAN`: The device is a VLAN interface.
      * `ADSL`: The device is an ADSL device.
      * `BRIDGE`: The device is a bridge master interface.
      * `GENERIC`: This is a generic support for unrecognized device types.
      * `TEAM`: The device is a team master interface.
      * `TUN`: The device is a TUN or TAP interface.
      * `TUNNEL`: The device is an IP tunnel interface.
      * `MACVLAN`: The device is a MACVLAN interface.
      * `VXLAN`: The device is a VXLAN interface.
      * `VETH`: The device is a VETH interface.
      * `MACSEC`: The device is a MACsec interface.
      * `DUMMY`: The device is a dummy interface.
      * `PPP`: The device is a PPP interface.
      * `OVS_INTERFACE`: The device is a Open vSwitch interface.
      * `OVS_PORT`: The device is a Open vSwitch port
      * `OVS_BRIDGE`: The device is a Open vSwitch bridge.
      * `WPAN`: The device is a IEEE 802.15.4 (WPAN) MAC Layer Device.
      * `SIXLOWPAN`: The device is a 6LoWPAN interface.
      * `WIREGUARD`: The device is a WireGuard interface. 
      * `WIFI_P2P`: The device is an 802.11 Wi-Fi P2P device.
      * `VRF`: The device is a VRF (Virtual Routing and Forwarding) interface.
      * `LOOPBACK`: The device is a loopback device.

### NetworkInterfaceState
The state of a network interface.

  * Possible values
      * `UNKNOWN`: The device is in an unknown state.
      * `UNMANAGED`: The device cannot be used (carrier off, rfkill, etc).
      * `UNAVAILABLE`: The device is not connected.
      * `DISCONNECTED`: The device is preparing to connect.
      * `PREPARE`: The device is being configured.
      * `CONFIG`: The device is awaiting secrets necessary to continue connection.
      * `NEED_AUTH`: The IP settings of the device are being requested and configured.
      * `IP_CONFIG`: The device's IP connectivity ability is being determined.
      * `IP_CHECK`: The device is waiting for secondary connections to be activated.
      * `SECONDARIES`: The device is waiting for secondary connections to be activated.
      * `ACTIVATED`: The device is active.
      * `DEACTIVATING`: The device's network connection is being turn down.
      * `FAILED`: The device is in a failure state following an attempt to activate it.

### WifiCapability
The capability of a WiFi interface.

  * Possible values
      * `NONE`: The device has no encryption/authentication capabilities
      * `CIPHER_WEP40`: The device supports 40/64-bit WEP encryption.
      * `CIPHER_WEP104`: The device supports 104/128-bit WEP encryption.
      * `CIPHER_TKIP`: The device supports the TKIP encryption.
      * `CIPHER_CCMP`: The device supports the AES/CCMP encryption.
      * `WPA`: The device supports the WPA1 encryption/authentication protocol.
      * `RSN`: The device supports the WPA2/RSN encryption/authentication protocol.
      * `AP`: The device supports Access Point mode.
      * `ADHOC`: The device supports Ad-Hoc mode.
      * `FREQ_VALID`: The device reports frequency capabilities.
      * `FREQ_2GHZ`: The device supports 2.4GHz frequencies.
      * `FREQ_5GHZ`: The device supports 5GHz frequencies.
      * `MESH`: The device supports mesh points.
      * `IBSS_RSN`: The device supports WPA2 in IBSS networks

### WifiMode
Modes of operation for wifi interfaces

  * Possible values
      * `UNKNOWN`: Mode is unknown.
      * `ADHOC`: Uncoordinated network without central infrastructure.
      * `INFRA`: Client mode - Coordinated network with one or more central controllers.
      * `MASTER`: Access Point Mode - Coordinated network with one or more central controllers.

### WifiSecurity
Flags describing the security capabilities of an access point.

  * Possible values
      * `NONE`: None
      * `PAIR_WEP40`: Supports pairwise 40-bit WEP encryption.
      * `PAIR_WEP104`: Supports pairwise 104-bit WEP encryption.
      * `PAIR_TKIP`: Supports pairwise TKIP encryption.
      * `PAIR_CCMP`: Supports pairwise CCMP encryption.
      * `GROUP_WEP40`: Supports a group 40-bit WEP cipher.
      * `GROUP_WEP104`: Supports a group 104-bit WEP cipher.
      * `GROUP_TKIP`: Supports a group TKIP cipher.
      * `GROUP_CCMP`: Supports a group CCMP cipher.
      * `KEY_MGMT_PSK`: Supports PSK key management.
      * `KEY_MGMT_802_1X`: Supports 802.1x key management.
      * `SECURITY_NONE`: Supports no encryption.
      * `SECURITY_WEP`: Supports WEP encryption.
      * `SECURITY_WPA`: Supports WPA encryption.
      * `SECURITY_WPA2`: Supports WPA2 encryption.
      * `SECURITY_WPA_WPA2`: Supports WPA and WPA2 encryption.

### ModemPortType
The type of a modem port.

  * Possible values
      * `UNKNOWN`
      * `NET`
      * `AT`
      * `QCDM`
      * `GPS`
      * `QMI`
      * `MBIM`
      * `AUDIO`
      * `IGNORED`

### ModemCapability
The generic access technologies families supported by a modem.

  * Possible values
      * `NONE`: The modem has no capabilities.
      * `POTS`: The modem supports the Plain Old Telephone Service (analog wired telephone network).
      * `EVDO`: The modem supports EVDO revision 0, A or B.
      * `GSM_UMTS`: The modem supports at least one of GSM, GPRS, EDGE, UMTS, HSDPA, HSUPA or HSPA+ technologies.
      * `LTE`: The modem has LTE capabilities.
      * `IRIDIUM`: The modem supports Iridium technology.
      * `FIVE_GNR`: The modem supports 5GNR.
      * `TDS`: The modem supports TDS.
      * `ANY`: The modem supports all capabilities.

### ModemMode
The generic access mode a modem supports.

  * Possible values
      * `NONE`
      * `CS`
      * `MODE_2G`
      * `MODE_3G`
      * `MODE_4G`
      * `MODE_5G`
      * `ANY`

### ModemBand
The radio bands supported by a modem when connected to a mobile network.

  * Possible values
      * `UNKNOWN`
      * `EGSM`
      * `DCS`
      * `PCS`
      * `G850`
      * `UTRAN_1`
      * `UTRAN_3`
      * `UTRAN_4`
      * `UTRAN_6`
      * `UTRAN_5`
      * `UTRAN_8`
      * `UTRAN_9`
      * `UTRAN_2`
      * `UTRAN_7`
      * `G450`
      * `G480`
      * `G750`
      * `G380`
      * `G410`
      * `G710`
      * `G810`
      * `EUTRAN_1`
      * `EUTRAN_2`
      * `EUTRAN_3`
      * `EUTRAN_4`
      * `EUTRAN_5`
      * `EUTRAN_6`
      * `EUTRAN_7`
      * `EUTRAN_8`
      * `EUTRAN_9`
      * `EUTRAN_10`
      * `EUTRAN_11`
      * `EUTRAN_12`
      * `EUTRAN_13`
      * `EUTRAN_14`
      * `EUTRAN_17`
      * `EUTRAN_18`
      * `EUTRAN_19`
      * `EUTRAN_20`
      * `EUTRAN_21`
      * `EUTRAN_22`
      * `EUTRAN_23`
      * `EUTRAN_24`
      * `EUTRAN_25`
      * `EUTRAN_26`
      * `EUTRAN_27`
      * `EUTRAN_28`
      * `EUTRAN_29`
      * `EUTRAN_30`
      * `EUTRAN_31`
      * `EUTRAN_32`
      * `EUTRAN_33`
      * `EUTRAN_34`
      * `EUTRAN_35`
      * `EUTRAN_36`
      * `EUTRAN_37`
      * `EUTRAN_38`
      * `EUTRAN_39`
      * `EUTRAN_40`
      * `EUTRAN_41`
      * `EUTRAN_42`
      * `EUTRAN_43`
      * `EUTRAN_44`
      * `EUTRAN_45`
      * `EUTRAN_46`
      * `EUTRAN_47`
      * `EUTRAN_48`
      * `EUTRAN_49`
      * `EUTRAN_50`
      * `EUTRAN_51`
      * `EUTRAN_52`
      * `EUTRAN_53`
      * `EUTRAN_54`
      * `EUTRAN_55`
      * `EUTRAN_56`
      * `EUTRAN_57`
      * `EUTRAN_58`
      * `EUTRAN_59`
      * `EUTRAN_60`
      * `EUTRAN_61`
      * `EUTRAN_62`
      * `EUTRAN_63`
      * `EUTRAN_64`
      * `EUTRAN_65`
      * `EUTRAN_66`
      * `EUTRAN_67`
      * `EUTRAN_68`
      * `EUTRAN_69`
      * `EUTRAN_70`
      * `EUTRAN_71`
      * `EUTRAN_85`
      * `CDMA_BC0`
      * `CDMA_BC1`
      * `CDMA_BC2`
      * `CDMA_BC3`
      * `CDMA_BC4`
      * `CDMA_BC5`
      * `CDMA_BC6`
      * `CDMA_BC7`
      * `CDMA_BC8`
      * `CDMA_BC9`
      * `CDMA_BC10`
      * `CDMA_BC11`
      * `CDMA_BC12`
      * `CDMA_BC13`
      * `CDMA_BC14`
      * `CDMA_BC15`
      * `CDMA_BC16`
      * `CDMA_BC17`
      * `CDMA_BC18`
      * `CDMA_BC19`
      * `UTRAN_10`
      * `UTRAN_11`
      * `UTRAN_12`
      * `UTRAN_13`
      * `UTRAN_14`
      * `UTRAN_19`
      * `UTRAN_20`
      * `UTRAN_21`
      * `UTRAN_22`
      * `UTRAN_25`
      * `UTRAN_26`
      * `UTRAN_32`
      * `ANY`
      * `NGRAN_1`
      * `NGRAN_2`
      * `NGRAN_3`
      * `NGRAN_5`
      * `NGRAN_7`
      * `NGRAN_8`
      * `NGRAN_12`
      * `NGRAN_13`
      * `NGRAN_14`
      * `NGRAN_18`
      * `NGRAN_20`
      * `NGRAN_25`
      * `NGRAN_26`
      * `NGRAN_28`
      * `NGRAN_29`
      * `NGRAN_30`
      * `NGRAN_34`
      * `NGRAN_38`
      * `NGRAN_39`
      * `NGRAN_40`
      * `NGRAN_41`
      * `NGRAN_48`
      * `NGRAN_50`
      * `NGRAN_51`
      * `NGRAN_53`
      * `NGRAN_65`
      * `NGRAN_66`
      * `NGRAN_70`
      * `NGRAN_71`
      * `NGRAN_74`
      * `NGRAN_75`
      * `NGRAN_76`
      * `NGRAN_77`
      * `NGRAN_78`
      * `NGRAN_79`
      * `NGRAN_80`
      * `NGRAN_81`
      * `NGRAN_82`
      * `NGRAN_83`
      * `NGRAN_84`
      * `NGRAN_86`
      * `NGRAN_89`
      * `NGRAN_90`
      * `NGRAN_91`
      * `NGRAN_92`
      * `NGRAN_93`
      * `NGRAN_94`
      * `NGRAN_95`
      * `NGRAN_257`
      * `NGRAN_258`
      * `NGRAN_260`
      * `NGRAN_261`

### SimType
The SIM (Subscriber Identity Module) type.

  * Possible values
      * `UNKNOWN`
      * `PHYSICAL`
      * `ESIM`

### ESimStatus
The status of an ESIM.

  * Possible values
      * `UNKNOWN`
      * `NO_PROFILES`
      * `WITH_PROFILES`

### BearerIpType
The type of Bearer or Context associated to a modem connection.

  * Possible values
      * `NONE`
      * `IPV4`
      * `IPV6`
      * `IPV4V6`
      * `NON_IP`
      * `ANY`

### ModemConnectionType
  * Possible values
      * `PPP`: Point to Point Protocol
      * `DirectIP`: Direct IP

### ModemConnectionStatus
The status of a modem.

  * Possible values
      * `FAILED`: The modem is unavailable
      * `UNKNOWN`: The modem is in an unknown state.
      * `INITIALIZING`: The modem is being initialised.
      * `LOCKED`: The modem is locked.
      * `DISABLED`: The modem is disabled and powered off.
      * `DISABLING`: The modem is disabling.
      * `ENABLING`: The modem is enabling..
      * `ENABLED`: The modem is enabled but not registered to a network provider.
      * `SEARCHING`: The modem is searching for a network provider.
      * `REGISTERED`: The modem is registered to a network provider.
      * `DISCONNECTING`: The modem is disconnecting.
      * `CONNECTING`: The modem is connecting.
      * `CONNECTED`: The modem is connected.

### AccessTechnology
The specific technology types used when a modem is connected or registered to a network.

  * Possible values
      * `UNKNOWN`
      * `POTS`
      * `GSM`
      * `GSM_COMPACT`
      * `GPRS`
      * `EDGE`
      * `UMTS`
      * `HSDPA`
      * `HSUPA`
      * `HSPA`
      * `HSPA_PLUS`
      * `ONEXRTT`
      * `EVDO0`
      * `EVDOA`
      * `EVDOB`
      * `LTE`
      * `FIVEGNR`
      * `LTE_CAT_M`
      * `LTE_NB_IOT`
      * `ANY`

### RegistrationStatus
The registration status of a modem when connected to a mobile network.

  * Possible values
      * `IDLE`
      * `HOME`
      * `SEARCHING`
      * `DENIED`
      * `UNKNOWN`
      * `ROAMING`
      * `HOME_SMS_ONLY`
      * `ROAMING_SMS_ONLY`
      * `EMERGENCY_ONLY`
      * `HOME_CSFB_NOT_PREFERRED`
      * `ROAMING_CSFB_NOT_PREFERRED`
      * `ATTACHED_RLOS`

### FailureReport
An object reporting a failure while retrieving the status of a specific network interface

<br>**Properties**:

  * **interfaceId**: `string` 
      The identifier of the interface whose status cannot be retrieved.

  
  * **reason**: `string` 
      A message describing the reason of the failure.

  

### GenericFailureReport
An object reporting a failure message.

<br>**Properties**:

  * **message**: `string` 
      A message describing the failure.

  

