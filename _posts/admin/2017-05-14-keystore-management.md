---
layout: page
title:  "Keystores Management"
categories: [admin]
---

The framework manages different types of cryptographic keys and certificates.
In order to simplify the interaction with those objects, Kura provides a KeystoreService API and a specific section in the Web UI that lists all the available KeystoreService instances.

From the Security section, a user with Security permissions can access the Keystore Configuration section.
A list of all the framework managed keystores will be available to the user with the Service PID that will be used by other components to reference the selected keystore.
Associated to the Service PID, the UI shows the Factory PID that identifies the specific KeystoreService API implementation that is providing the service to the framework.

![]({{ site.baseurl }}/assets/images/admin/KeystoreConfig1.png)

In order to modify the configuration of a specific keystore service instance, the user can select one of the available rows, obtaining the corresponding keystore service configuration.

![]({{ site.baseurl }}/assets/images/admin/KeystoreConfig2.png)

For the **org.eclipse.kura.core.keystore.KeystoreServiceImpl** factory, the user can customise the following options:

- Keystore Path: identifies the path in the filesystem. The value should reference an existing keystore. The value cannot be empty.
- Keystore Password: the corresponding keystore password
- Randomize Password: a boolean flag that allows the user to specify if the keystore password needs to be randomised at the next framework boot. If set true, the framework will try to access the identified keystore and randomise the password. The new password will be persisted in the framework snapshot. Once successfully randomised, the flag will be automatically set to false by the framework.

