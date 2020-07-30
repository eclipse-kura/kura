---
layout: page
title:  "Deployment Service"
categories: [builtin]
---

The Deployment Service allows user to download files to the gateway and to perform actions on them. In the configuration tab it is possible to specify which is the directory that has to be used to store the downloaded files and the list of actions declared as deployment hooks that will be invoked when a corresponding metric is received with the download request.

![deployment_service]({{ site.baseurl }}/assets/images/builtin/deployment_service.png)

The configuration requires to specify two parameters:

  - **downloads.directory** - The directory to be used to store the downloaded files;
  - **deployment.hook.associations** - The list of DeploymentHook associations in the form \<request_type\>=\<hook_pid\>, where \<hook_pid\> is the Kura Service Pid of a DeploymentHook instance and \<request_type\> is the value of the request.type metric received with the request.