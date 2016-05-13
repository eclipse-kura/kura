---
layout: page
title:  "How to Use Watchdog"
date:   2016-05-13 10:30:00
categories: [doc]
---

[Overview](#overview_1)

[Configuration](#configuration_1)

[Code Example](#code_example_1)


<span id="overview" class="anchor"><span id="overview_1" class="anchor"></span></span></span>Overview
==========================================================================================================================================

When enabled, the watchdog is a peripheral monitor that will reboot the system if it is not refreshed during a certain time interval. In ESF, the WatchdogService can be used by critical applications. If the specified application is alive, the service notifies the watchdog; if the application is down, the service stops notifying the watchdog and a hardware reset occurs.

The WatchdogService notifies the kernel watchdog driver using the /dev/watchdog device file. You can verify that the watchdog driver is installed using the following command:

```
ls –l /dev/watchdog
```


<span id="configuration" class="anchor"><span id="configuration_1" class="anchor"></span></span>Configuration
==============================================================================================================================================

To configure the **WatchdogService**, select the WatchdogService option located in the **Services** area as shown in the screen capture below.

The WatchdogService provides the following configuration parameters:

![alt text](http://eclipse.github.io/kura/assets/images/watchdog/watchdog.png "Watchdog Configuration")

* **enabled** - sets whether or not this service is enabled or disabled. If enabled, you must set a pingInterval periodicity compatible with the watchdog driver.

* **pingInterval** - specifies the time between two watchdog notifications. This time is hardware dependent. Generally, the maximum time between two notifications should be between 30 seconds and 1 minute. 10000 milliseconds for the pingInterval is typically a good choice.

<span id="code_example" class="anchor"><span id="code_example_1" class="anchor"></span></span>Code Example
==========================================================================================================================================================

The WatchdogService references a list of Critical Components that correspond to the applications implementing the CriticalComponent interface.

**CriticalComponent** is an interface that can be used to denote a component that is crucial to system operations. If a component implements CriticalComponent, then it must state its name as well as its **criticalComponentTimeout**. The name is a unique identifier in the system. The timeout is the length of time in milliseconds that the CriticalComponent must "check in" with the WatchdogService. If the CriticalComponent extends beyond the period of time specified in this timeout, a system reboot will be performed based on the WatchdogService configuration.

If at least one of the registered CriticalComponents has not "checked in" during the pingInterval time, the WatchdogService stops notifying the watchdog driver. The system reboots when the time interval reaches the hardware time that is programmed for the watchdog. When the WatchdogService is enabled and no application is using it, the service runs silently in the background.

An example of the WatchdogService can be found [here](<https://github.com/eurotech/edc-examples/blob/master/com.eurotech.framework.edcdevkit>).

The following code snippets demonstrate how to implement the CriticalComponent interface:

```java
public class ModbusManager implements ConfigurableComponent, CriticalComponent, CloudClientListener
```

Registration of the class in WatchdogService::

```java
if(m_watchdogService!=null){
  m_watchdogService.registerCriticalComponent(this);
}
```

Periodic call to checkin method of WatchdogService in the main loop (keeps watchdog notification alive):

```java
if(m_watchdogService!=null){
  m_watchdogService.checkin(this);
}
```

