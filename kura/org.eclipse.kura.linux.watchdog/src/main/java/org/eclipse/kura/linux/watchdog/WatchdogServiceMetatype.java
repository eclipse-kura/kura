/*******************************************************************************
 * Copyright (c) 2011, 2026 Eurotech and/or its affiliates and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *  Eurotech
 ******************************************************************************/
package org.eclipse.kura.linux.watchdog;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Icon;
import org.osgi.service.metatype.annotations.Option;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(id = "org.eclipse.kura.watchdog.WatchdogService", name = "WatchdogService", description = "The WatchdogService handles the hardware watchdog of the platform. It defines the ping periodicity of the hardware watchdog to ensure it does not reboot. The WatchdogService will reset the watchdog timeout, can disable it (where supported) with the Magic Character, but cannot set the refresh rate of a watchdog device.", icon = @Icon(resource = "WatchdogService", size = 32))
public @interface WatchdogServiceMetatype {

    @AttributeDefinition(name = "Watchdog enable", description = "The WatchdogService monitors CriticalComponents and reboots the system if one of them hangs. Once enabled the WatchdogService starts refreshing the watchdog device, which will reset the system if WatchdogService hangs.")
    boolean enabled() default false;

    @AttributeDefinition(name = "Watchdog refresh interval", max = "60000", description = "WatchdogService's refresh interval in ms of the Watchdog device. The value can be set between 1 and 60 seconds. If it is set to a value greater than half of the Watchdog device's timeout value, the WatchdogService will clamp the interval to half of the timeout.")
    int pingInterval() default 10000;

    @AttributeDefinition(name = "Watchdog mode", options = { @Option(label = "systemd (sd_notify)", value = "systemd"), @Option(label = "Direct (/dev/watchdog)", value = "direct") }, description = "Selects the WatchdogService operating mode. 'systemd' (default): Kura relies on the watchdog of the systemd service manager and sends it regular keep-alive notifications through the sd_notify protocol; Kura does not access the hardware watchdog device, which stays managed by systemd. 'direct': Kura opens the watchdog device configured below and refreshes it itself, taking exclusive access to it; in order to use this mode, the hardware watchdog must not be in use by other processes. If the selected mode cannot be activated (for example the systemd notification socket or the watchdog device is not available), the WatchdogService degrades to a software reboot on critical component failure, without hardware watchdog protection.")
    String watchdogMode() default "systemd";

    @AttributeDefinition(name = "Watchdog device path", description = "Watchdog device path e.g. /dev/watchdog. Only used when watchdogMode is 'direct'.")
    String watchdogDevice() default "/dev/watchdog";

    @AttributeDefinition(name = "Reboot Cause File Path", description = "The path for the file that will contain the reboot cause information.")
    String rebootCauseFilePath() default "/opt/eclipse/kura/data/kura-reboot-cause";

    @AttributeDefinition(name = "Fail on unavailable device", description = "Only used when watchdogMode is 'direct'. If enabled and the watchdog device does not exist or cannot be opened, the WatchdogService requests a system reboot instead of degrading to a software reboot on critical component failure. The device is checked again at every refresh interval for a grace period of 5 minutes, as for a critical component timeout: if it becomes available, supervision starts and no reboot is requested. The reboot cause is written to the reboot cause file.")
    boolean failOnUnavailableDevice() default false;

}
