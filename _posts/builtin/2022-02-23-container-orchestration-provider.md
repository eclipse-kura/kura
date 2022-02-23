---
layout: page
title:  "Container Orchestration provider"
categories: [builtin]
---

#What is the Container Orchestration provider?
The Container Orchestration provider allows Kura to manage Docker. With this tool you can arbitrarily pull and deploy containerized software packages and run them on your gateway. This Provider allows the user to create, configure, start, and stop containers all from the browser. The bundle will also restart containers, if the gateway is restarted.

The feature is composed by two bundles, one that exposes APIs for container management and one that implements those APIs. This API is exposed so that you can leverage it to implement containerization in your own Kura plugins.

#How to use the Container Orchestration Provider
