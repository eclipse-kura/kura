/**
 * Copyright (c) 2018 Eurotech and/or its affiliates and others
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Eurotech
 */
package org.eclipse.kura.driver.gpio.localization;

import com.github.rodionmoiseev.c10n.annotations.En;

/**
 * {@link GPIOMessages} is a localization resource for
 * {@code GPIODriver} bundle. It contains all the necessary translations for
 * every string literals mentioned in {@code GPIODriver} bundle.
 */
public interface GPIOMessages {

    @En("Error while retrieving GPIO resource {0}")
    public String errorRetrievingResource(String gpioResource);

    @En("Unable to open GPIO resource {0}")
    public String errorOpeningResource(String gpioResource);

    @En("Unable to close GPIO resource {0}")
    public String errorClosingResource(String gpioResource);

    @En("Unable to set listener for pin {}")
    public String errorSettingListener(String gpioResource);

    @En("Unable to unset listener for pin {}")
    public String errorRemovingListener(String gpioResource);

    @En("Error while retrieving value type")
    public String errorRetrievingValueType();

    @En("Error while converting the retrieved value to the defined typed")
    public String errorValueTypeConversion();

    @En("Properties cannot be null")
    public String propertiesNonNull();

    @En("GPIO read operation failed")
    public String readFailed();

    @En("Channel Record cannot be null")
    public String recordNonNull();

    @En("Channel Record list cannot be null")
    public String recordListNonNull();

    @En("Value cannot be null")
    public String valueNonNull();

    @En("Value is null")
    public String valueNull();

    @En("GPIO write operation failed")
    public String writeFailed();

    @En("Driver is busy")
    public String errorDriverBusy();

    @En("Got Bad Status: {0}")
    public String errorBadResultStatus(long statusCode);

    @En("Operation Result cannot be null")
    public String errorNullResult();

    @En("Operation Result Status cannot be null")
    public String errorNullStatus();

}