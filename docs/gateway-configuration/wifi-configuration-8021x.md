# Wi-Fi 802.1x Configuration
Enterprise Wi-Fi is currently only supported on gateways running our [generic profiles](../../getting-started/install-kura.md). The following is a list of currently supported 802.1x authentication methods.

 - [TTLS-MSCHAPv2](#ttls-mschapv2)
 - [PEAP-MSCHAPv2](#peap-mschapv2)
 - [EAP-TLS](#eap-tls)

## TTLS-MSCHAPv2
 1. Set up gateway Wi-Fi as described in the [Wi-Fi configuration guide](./wifi-configuration.md#wireless-configuration).
 2. Ensure Wireless Security is set to `WPA2/WPA3-Enterprise`
    ![Alt text](images/802-1x-images/wifi-enterprise-enum.png)
 3. select the 802.1x tab
 4. Set Enteprise EAP -> `TTLS`
 5. Set Inner Authentication -> `MSCHAPV2`
 6. Set `Identity (Username)` 
 7. Set `Password`
 6. Press 'Apply'

The configuration should look like the following:
![Alt text](images/802-1x-images/wifi-ttls.png)

## PEAP-MSCHAPv2
 1. Set up gateway Wi-Fi as described in the [Wi-Fi configuration guide](./wifi-configuration.md#wireless-configuration).
 2. Ensure Wireless Security is set to `WPA2/WPA3-Enterprise`
    ![Alt text](images/802-1x-images/wifi-enterprise-enum.png)
 3. select the 802.1x tab
 4. Set Enteprise EAP -> `PEAP`
 5. Set Inner Authentication -> `MSCHAPV2`
 6. Set `Identity (Username)` 
 7. Set `Password`
 6. Press 'Apply'

The configuration should look like the following:
![Alt text](images/802-1x-images/wifi-peap.png)

## EAP-TLS
To connect via EAP-TLS you will need the following items in unencrypted PEM format:

- Certificate Authority (CA) Certificate
- Client Certificate + Private Key (PKCS8)

### Enrolling secrets in the [Keystore service](/gateway-configuration/keystores-management.md/).

 1. Navigate to `Security` under the `System` tab.
 2. Under the `Keystore Configuration` add a new keystore, and keep note of the name. ![Adding a new keystore](images/802-1x-images/wifi-create-keystore.png)
 3. After the Keystore is created, be sure to change the path to a persistent directory. ![Alt text](images/802-1x-images/wifi-create-change-path.png)
 4. Navigate to the `Certificate List` and create a new Certificate. Insert the PEM and Apply, keep note of the name. ![add certificate](images/802-1x-images/wifi-create-keystore-add-certificate.png)
 5. Now press `add` and create a new Private Key. Insert both the certificates in the PEM in the dialogue and press apply. keep note of the name. ![Alt text](images/802-1x-images/wifi-create-keystore-add-privatekey.png)

### Wifi Setup

 1. Set up gateway Wi-Fi as described in the [Wi-Fi configuration guide](./wifi-configuration.md#wireless-configuration).
 2. Ensure Wireless Security is set to `WPA2/WPA3-Enterprise`. ![Alt text](images/802-1x-images/wifi-enterprise-enum.png)
 3. Select the 802.1x tab.
 4. Set Enteprise EAP -> `TLS`.
 5. Set `Identity (Username)`.
 6. Set `Keystore Pid` to the name of the keystore created above.
 7. Set `Certificate Authority Certificate (CA-Cert)` to the name of the certificate created above.
 8. Set the `Client Private Key` to the name of the Private Key created above.

When completed the Wi-Fi configuration should look like the following:
![tls-configuration](images/802-1x-images/wifi-tls.png)
