---
layout: page
title:  "Cloud Services"
categories: [config]
---

The Cloud Services section of the Kura Administrative Web UI allows us to create and manage cloud connections.

By default, Kura starts with a single cloud connection, as depicted in the following image:

![kura_cloud_stack]({{ site.baseurl }}/assets/images/config/Kura_cloud_stack.png)

The cloud services page allows to:
- **create** a new cloud connection;
- **delete** an existing cloud connection;
- **connect** a selected cloud stack to the configured cloud platform;
- **disconnect** the selected cloud stack from the connected cloud platform;
- **refresh** the existing cloud connections.

When clicking on the **New** button, a dialog is displayed as depicted in the image below:

![kura_cloud_stack]({{ site.baseurl }}/assets/images/config/Kura_new_cloud_stack.png)

The user can select one of the existing cloud connection factories and give it a name (depending on the implementation, a name format can be suggested or forced).