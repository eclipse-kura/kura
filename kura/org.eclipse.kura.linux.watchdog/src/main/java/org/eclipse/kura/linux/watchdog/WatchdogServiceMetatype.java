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
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.osgi.service.metatype.annotations.Icon;

@ObjectClassDefinition(id = "org.eclipse.kura.watchdog.WatchdogService", name = "WatchdogService", description = "The WatchdogService handles the hardware watchdog of the platform.  The parameter define the ping periodicity of the hardware watchdog to ensure it does not reboot. The WatchdogService will reset the watchdog timeout, can disable it (where supported) with the Magic Character, but cannot set the refresh rate of a watchdog device.", icon = @Icon(resource = "WatchdogService", size = 32))
public @interface WatchdogServiceMetatype {

    @AttributeDefinition(name = "Watchdog enable", description = "The WatchdogService monitors CriticalComponents and reboots the system if one of them hangs. Once enabled the WatchdogService starts refreshing the watchdog device, which will reset the system if WatchdogService hangs.")
    boolean enabled() default false;

    @AttributeDefinition(name = "Watchdog refresh interval", max = "60000", description = "WatchdogService's refresh interval in ms of the Watchdog device. The value can be set between 1 and 60 seconds and should not be set to a value greater or equal to the Watchdog device's timeout value")
    int pingInterval() default 10000;

    @AttributeDefinition(name = "Watchdog device path", description = "Watchdog device path e.g. /dev/watchdog.")
    String watchdogDevice() default "/dev/watchdog";

    @AttributeDefinition(name = "Reboot Cause File Path", description = "The path for the file that will contain the reboot cause information.")
    String rebootCauseFilePath() default "/opt/eclipse/kura/data/kura-reboot-cause";

}


