---
layout: page
title:  "Device Configuration Changes"
categories: [builtin]
---

Eclipse Kura can detect changes to the components and publish them using a selected Cloud Publisher. 
There are two main components that enable this:

- `org.eclipse.kura.configuration.change.manager`, and
- `org.eclipse.kura.event.publisher`

The `org.eclipse.kura.configuration.change.manager` is responsible for detecting changes to any of the configurations currently running on the system and to publish a notification to a user-defined cloud publisher. By default, the `org.eclipse.kura.event.publisher` is used.

## Configuration Change Manager

The configuration change manager allows to collection of the PIDs of the components that have changed in the system. The configurable properties of this component are:

- **Enable**: whether to enable or not this component.
- **CloudPublisher Target Filter**: allows to specify the cloud publisher to use for sending the produced messages.
- **Notification send delay (sec)**: allows to stack the changed PIDs for a given time before sending. In other words, it allows to group together sequences of PIDs that are changed in that time frame. This is to prevent message flooding at reboot or rollback.

The collected PIDs are sent to the Cloud Publisher as a `KuraMessage` with a payload body in JSON format. The timestamp of the `KuraMessage` is set to the last detected configuration change event. An example of message body is:

```JSON
[
    {
        “pid”: “org.eclipse.kura.clock.ClockService”
    },
    {
        “pid”: “org.eclipse.kura.log.filesystem.provider.FilesystemLogProvider”
    }
]
```

In the example above, a *ClockService* update triggered the delay timer, which was then reset by a configuration update on the *FilesystemLogProvider*. Afterward, no configuration updates reset the timer so the message containing the two PIDs was sent after expiration.

## Event Publisher

By default, the `org.eclipse.kura.event.publisher` used by the configuration change manager requires the actual publishing on a user-defined topic of the form:

`$EVT/#account_id/#client_id/CONF/V1/CHANGE`

Where the `$EVT` and the `CONF/V1/CHANGE` parts can be customized according to user needs by tweaking the **Topic prefix** and **Topic** properties.

The `#account_id` and `#client_id` fields are substituted at runtime with the account name and client ID at the Data Transport Service layer of the associated Cloud Connection.

The **Qos**, **Retain**, and **Priority** properties are the usual ones defined in the standard Cloud Publisher.
