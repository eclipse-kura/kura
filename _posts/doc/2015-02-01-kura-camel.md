---
layout: page
title:  "Apache Camel™ integration"
categories: [doc]
---

# Kura Camel overview

{% include note.html message="This document describes the Camel integration for Kura 2.1.0" %}

Kura provides two main integration points for Camel:

 * Camel as a Kura application
 * Camel as a Kura cloud service
 
The first allows one to configure Camel to provide data and receive commands from any CloudService instance
which is configured in Kura. For example the default CloudService instance which is backed by MQTT.

The second approach allows one to create a custom CloudService implementation and route data coming from other
Kura applications with the routes provided by this Camel context.

## Deploying additional Camel components

Kura comes with the following Camel components pre-installed:

* camel-core
* camel-core-osgi
* camel-stream

If additional Camel components are required, they can be installed using
deployment packages (DP), as common with Kura.

There are pre-packaged DPs available for e.g. AMQP, OPC UA, MQTT and other
Camel components outside of the Kura project.
 