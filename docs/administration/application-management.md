# Application Management

## Package Installation

After developing your application and generating a deployment package that contains the bundles to be deployed (refer to the Development section for more information), you may install it on the gateway using the **Packages** option in the **System** area of the Kura Gateway Administration Console as shown below.

![](images/packageInstall.png)

Upon a successful installation, the new component appears in the Services list (shown as the _Heater_ example in these screen captures). Its configuration may be modified according to the defined parameters as shown the _Heater_ display that follows.

![](images/packageConfig.png)

## Eclipse Kura Marketplace

Kura allows the installation and update of running applications via the Eclipse Kura Marketplace.
The **Packages** page has, in the top part of the page a section dedicated to the Eclipse Kura Marketplace.

<figure markdown>
  ![](images/marketplaceInstall.png){ style="border-radius: 7px;"}
  <figcaption></figcaption>
</figure>

Dragging an application reference taken from the Eclipse Kura Marketplace to the specific area of the Kura Web Administrative Console will instruct Kura to download and install the corresponding package, as seen below:

![](images/packageMarketplace.png)

## Package Signature

Once the selected application deployment package (dp) file is installed, it will be listed in the **Packages** page and detailed with the name of the deployment package, the version and the signature status.
The value of the signature field can be **true** if all the bundles contained in the deployment package are digitally signed, or **false** if at least one of the bundles is not signed.

<figure markdown>
  ![](images/dpsignature.png){ style="border-radius: 7px;"}
  <figcaption></figcaption>
</figure>
