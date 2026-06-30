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
package org.eclipse.kura.linux.clock;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.osgi.service.metatype.annotations.Icon;
import org.osgi.service.metatype.annotations.Option;

@ObjectClassDefinition(id = "org.eclipse.kura.clock.ClockService", name = "ClockService", description = "ClockService Configuration", icon = @Icon(resource = "ClockService", size = 32))
public @interface ClockServiceOptions {

    @AttributeDefinition(name = "enabled", description = "Whether or not to enable the ClockService")
    boolean enabled() default true;

    @AttributeDefinition(name = "clock.set.hwclock", description = "Whether or not to sync the system hardware clock after the system time gets set")
    boolean clock_set_hwclock() default true;

    @AttributeDefinition(name = "clock.provider", options = { @Option(label = "java-ntp", value = "java-ntp"), @Option(label = "chrony-advanced", value = "chrony-advanced") }, description = "Source for setting the system clock. Verify the availabiliy of the selected provider before activate it. Using chrony-advanced causes all fields, except Chrony Configuration, to be ignored.")
    String clock_provider() default "java-ntp";

    @AttributeDefinition(name = "clock.ntp.host", description = "The hostname that provides the system time via NTP")
    String clock_ntp_host() default "0.pool.ntp.org";

    @AttributeDefinition(name = "clock.ntp.port", min = "1", max = "65535", description = "The port number that provides the system time via NTP")
    int clock_ntp_port() default 123;

    @AttributeDefinition(name = "clock.ntp.timeout", min = "1000", description = "The NTP timeout in milliseconds")
    int clock_ntp_timeout() default 10000;

    @AttributeDefinition(name = "clock.ntp.max-retry", min = "0", description = "The maximum number of retries for the initial synchronization (with interval clock.ntp.retry.interval). If set to 0 the service will retry forever.")
    int clock_ntp_max$_$retry() default 0;

    @AttributeDefinition(name = "clock.ntp.retry.interval", min = "1", description = "When sync fails, interval in seconds between each retry.")
    int clock_ntp_retry_interval() default 5;

    @AttributeDefinition(name = "clock.ntp.refresh-interval", description = "Whether or not to sync the clock and if so, the frequency in seconds.  If less than zero - no update, if equal to zero - sync once at startup, if greater than zero - the frequency in seconds to perform a new clock sync")
    int clock_ntp_refresh$_$interval() default 3600;

    @AttributeDefinition(name = "RTC File Name", description = "The RTC File Name. It defaults to /dev/rtc0. This option is not used if chrony-advanced option is selected in clock.provider.")
    String rtc_filename() default "/dev/rtc0";

    @AttributeDefinition(name = "Chrony Configuration", required = false, description = "Chrony configuration file. All fields above will be ignored.|TextArea")
    String chrony_advanced_config();

}

