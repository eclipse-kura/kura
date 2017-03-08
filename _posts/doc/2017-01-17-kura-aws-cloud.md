---
layout: page
title:  "Amazon AWS IoT&trade; platform"
categories: [doc]
---

[Overview](#overview)

[Prerequisites](#prerequisites)

[Device registration](#device-registration)

[Device configuration](#device-configuration)

## Overview

This section provides a guide on connecting an Eclipse Kura&trade; device to the Amazon AWS IoT platform.

## Prerequisites

* In order to connect a device to Amazon AWS IoT Kura version 1.3 or greater is required.
* An Amazon AWS account is also needed.

## Device registration

The first step involves the registration of the new device on AWS, this operation can be done using the AWS Web Console or with the AWS CLI command line tool, in this guide the Web based console will be used.

1. Access the AWS IoT management console.

    This can be done by logging in the AWS console and selecting **AWS IoT** from the services list. You should the following screen:

    ![welcome_screen]({{ site.baseurl }}/assets/images/aws/welcome_screen.png)

    Click on the **Get Started** button in order to access the actual console.

2. Register a new device.

    Devices on the AWS IoT platform are called *things*, in order to register a new thing select **Registry** -> **Things** from the left side menu and then press the **Create** button.

    ![new_thing]({{ site.baseurl }}/assets/images/aws/new_thing.png)

    Enter a name for the new device and then press the **Create thing** button, from now on `kura-gateway` will be used as the device name.

3. Access to the device configuration.

    The configuration of the newly created device can be accessed by selecting **Registry** -> **Things** in the left side panel of the main screen and then clicking on the device name:

    ![device_config]({{ site.baseurl }}/assets/images/aws/device_config.png)

4. Download the device SSL keys.

    The AWS IoT platform uses SSL mutual authentication, for this reason it is necessary to download a public/private key pair for the device and a server certificate. In order to download the keys access the device configuration, select **Security** on the left menu and the press the **Create certificate** button.

    You should see a screen like the following:

    ![keys.png]({{ site.baseurl }}/assets/images/aws/keys.png)

    Download the 3 files listed in the table and store them in a safe place, they will be needed later, also copy the link to the root CA for AWS IoT in order to be able to retrieve it later from the device.

    Press the **Activate** button.

5. Create a default policy for the device.

    Return to the main screen of the console and select **Security** -> **Policies** from the left side menu and then press the **Create a policy** button.

    Fill the form as follows and then press the **Create** button:

    * **Action** -> `iot:Connect, iot:Publish, iot:Subscribe, iot:Receive, iot:UpdateThingShadow, iot:GetThingShadow, iot:DeleteThingShadow`
    * **Resource ARN** -> `*`
    * **Effect** -> `Allow`

    This will create a policy that allows a device to connect to the platform, publish/subscribe on any topic and manage its *thing shadow*.

6. Assign the default policy to the device.

    Enter the device configuration section, click on **Security** on the left panel and then click on the certificate entry (it is identified by an hex code), select **Policies** in the left menu, you should see this screen:
     ![policies]({{ site.baseurl }}/assets/images/aws/policies.png)

    Click on **Actions** in the top left section of the page and then click on **Attach policy**, select the default policy previously created and then press the **Attach** button.

## Device configuration

The following steps should be performed on the device, this guide is based on Kura 2.1.0 version and has been tested on a Raspberry PI 3.

{:start="7"}
7.  Create a Java keystore on the device.

    The first step for using the device keys obtained at the previous step is to create a new Java keystore containing the Root Certificate used by the Amazon IoT platform, this can be done executing the following commands on the device:

    ```
    sudo mkdir /opt/eclipse/security
    cd /opt/eclipse/security
    curl https://www.symantec.com/content/en/us/enterprise/verisign/roots/VeriSign-Class%203-Public-Primary-Certification-Authority-G5.pem > /tmp/root-CA.pem
    sudo keytool -import -trustcacerts -alias verisign -file /tmp/root-CA.pem -keystore cacerts -storepass changeit
    ```

    If the last command reports that the certificate already exist in the system-wide store type `yes` to proceed.

    The code above will generate a new keystore with `changeit` as password, change it if needed.

8.  Configure the SSL parameters using the Kura Web UI.

    1.  Open the Kura Web Console and enter select the **Settings** entry in the left side menu and then click on **SSL Configuration**, you should see this screen:

        ![ssl_config]({{ site.baseurl }}/assets/images/aws/ssl_config.png)

        Change the settings in the form to match the screen above, enter `changeit` as **Keystore Password** (or the password defined at step 7).

    2.  Open the Kura Web Console and enter select the **Settings** entry in the left side menu and then click on **Device SSL Certificate**, you should see this screen:

        ![kura_settings]({{ site.baseurl }}/assets/images/aws/kura_settings.png)

        Enter `aws-ssl` in the **Storage Alias** field.

    3.  The private key needs to be converted to the PKCS8 format, this step can be performed executing the following command on a Linux or OSX based machine:

        ```
        openssl pkcs8 -topk8 -inform PEM -outform PEM -in xxxxxxxxxx-private.pem.key -out outKey.pem -nocrypt
        ```

        where `xxxxxxxxxx-private.pem.key` is the file containing the private key downloaded at step 4.

    4. Paste the contents of the obtained `outKey.pem` in the "Private Key" field.

    5. Paste the contents of `xxxxxxxxxx-certificate.pem.crt` in the **Certificate** field.

        You should see a screen like this

        ![keys_config]({{ site.baseurl }}/assets/images/aws/keys_config.png)

    6. Click the **Apply** button to confirm.

9.  Click on **Cloud Services** in the left panel, you should see this screen:

      ![cloud_services]({{ site.baseurl }}/assets/images/aws/cloud_services.png)

    1. Click on the **New** button at the top of the page and set the following parameters in the dialog:

        * **Factory** -> `org.eclipse.kura.cloud.CloudService`
        * **Cloud Service Pid** -> `org.eclipse.kura.cloud.CloudService-AWS`

        Press the **Create** button to confirm and then select the newly created CloudService instance from the list.

    2. Set the broker URL in the **MqttDataTransport-AWS** tab, it can be obtained from the AWS IoT Web Console clicking on the **Settings** entry in the bottom left section of the page, the URL will look like the following:

        ```
        a1rm1xxxxxxxxx.iot.us-east-1.amazonaws.com
        ```

        The mqtts protocol must be used, the value for the **broker-url** field derived from the URL above is the following:  

        ```
        mqtts://a1rm1xxxxxxxxx.iot.us-east-1.amazonaws.com:8883/
        ```

    3. Clear the value of the **username** and **password** fields.

    4. Set a value for the **topic.context.account-name** and **client-id**.

        * Assign an arbitrary account name to **topic.context.account-name** (for example `aws-test`), this will be used by the CloudClient instances for building the topic structure.

        * Enter the *thing* name in the **client-id** field (in this example `kura-gateway`).

    5. In order for the previously added keys to be used for the SSL connection with the broker enter the **Storage Alias** defined in step 8.2 (e.g `aws-ssl`) as value for the **ssl.certificate.alias** field.

    6. The setting **lwt.topic** under **MqttDataTransport-AWS** needs to be updated as well by entering a value not containing the $ character. This is required because of the fact that AWS IoT does not support topic names starting with $ (except for the $aws/ hierarchy).

    7. Press the **Apply** button in the top left section to commit the changes to the **MqttDataTransport-AWS**.

    8. Enter a name without the $ character for the **topic.control-prefix** setting in the **CloudService-AWS** tab, for example `aws-control`.

    9. The Kura CloudService uses some well-known topics to allow remote device management and to report device state information, this features are not supported by default by AWS IoT, the following settings can be applied in the **CloudService-AWS** tab in order to avoid sending unnecessary messages:

        * **republish.mqtt.birth.cert.on.gps.lock** -> `false`
        * **republish.mqtt.birth.cert.on.modem.detect** -> `false`
        * **enable.default.subscriptions** -> `false`
        * **birth.cert.policy** -> `Disable publishing`


    10. Click the **Apply** button to save the changes.

10.  Make sure the AWS CloudService instance is selected from the list in the top section of the page and click on the **Connect** button, if the connection to AWS IoT platform succeeds the **Status** of the instance will be reported as *Connected*.
