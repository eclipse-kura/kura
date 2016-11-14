---
layout: page
title:  "Apache Camel™ integration"
date:   2014-05-16 11:31:11
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
