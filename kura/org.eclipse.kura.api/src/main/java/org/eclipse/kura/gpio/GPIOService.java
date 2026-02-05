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
package org.eclipse.kura.gpio;

import java.util.List;
import java.util.Map;

import org.osgi.annotation.versioning.ProviderType;

/**
 * The GPIOService is used to access available GPIO resources on the system.<br>
 * {@link KuraGPIOPin}s can be accessed by its name or by its controller and line offset.<br>
 * <br>
 * Operations on the pins can be done using the acquired {@link KuraGPIOPin} class.
 *
 * @noimplement This interface is not intended to be implemented by clients.
 */
@ProviderType
public interface GPIOService {

    /**
     * Get a GPIO pin by its name.
     * 
     * For example, to get the pin named "GPIO22", call:
     * <pre>
     * KuraGPIOPin pin = gpioService.getPinByName("GPIO22");
     * </pre>
     * 
     * @param pinName the name of the pin
     * 
     * @return the KuraGPIOPin instance
     * @deprecated Use {@link #getPins(Map)} and select the desired pin from the returned list.
     */
    @Deprecated(since = "3.0", forRemoval = true)
    public KuraGPIOPin getPinByName(String pinName);

    /**
     * Get a GPIO pin by its name, with the specified direction, mode and trigger.
     * 
     * For example, to get an output open-drain pin with no trigger named "GPIO22", call:
     * <pre>
     * KuraGPIOPin pin = gpioService.getPinByName("GPIO22", KuraGPIODirection.OUTPUT, KuraGPIOMode.OUTPUT_OPEN_DRAIN, KuraGPIOTrigger.NONE);
     * </pre>
     * 
     * @param pinName the name of the pin
     * @param direction the direction of the pin
     * @param mode the mode of the pin
     * @param trigger the trigger of the pin
     * 
     * @return the KuraGPIOPin instance
     * @deprecated Use {@link #getPins(Map, KuraGPIODirection, KuraGPIOMode, KuraGPIOTrigger)} and 
     * select the desired pin from the returned list.
     */
    @Deprecated(since = "3.0", forRemoval = true)
    public KuraGPIOPin getPinByName(String pinName, KuraGPIODirection direction, KuraGPIOMode mode,
            KuraGPIOTrigger trigger);

    /**
     * Get a GPIO pin by its terminal index.
     * 
     * @param terminal the terminal index of the pin
     * 
     * @return the KuraGPIOPin instance
     * @deprecated Use {@link #getPins(Map)} instead.
     */
    @Deprecated(since = "3.0", forRemoval = true)
    public KuraGPIOPin getPinByTerminal(int terminal);

    /** 
     * Get a GPIO pin by its terminal index, with the specified direction, mode and trigger.
     * 
     * @param terminal the terminal index of the pin
     * @param direction the direction of the pin
     * @param mode the mode of the pin
     * @param trigger the trigger of the pin
     * 
     * @return the KuraGPIOPin instance
     * @deprecated Use {@link #getPins(Map, KuraGPIODirection, KuraGPIOMode, KuraGPIOTrigger)} instead.
     */
    @Deprecated(since = "3.0", forRemoval = true)
    public KuraGPIOPin getPinByTerminal(int terminal, KuraGPIODirection direction, KuraGPIOMode mode,
            KuraGPIOTrigger trigger);

    /** 
     * Get a map of available GPIO pins.
     * 
     * @return a map of available GPIO pins, where the key is the terminal index and the value is the pin name
     * @deprecated Use {@link #getAvailablePinDescriptions()} instead.
     */
    @Deprecated(since = "3.0", forRemoval = true)
    public Map<Integer, String> getAvailablePins();
    
    /**
     * Get the GPIO pins that match the given description properties.
     * 
     * The description map is interpreted as a set of filter criteria:
     * only the properties present in the map are considered when selecting
     * pins. Pins that match all provided properties are returned; properties
     * that are not specified are ignored.
     * 
     * For example, to get all pins with a specific name, you can call:
     * <pre>
     * Map<String, String> description = new HashMap<>();
     * description.put("name", "GPIO22");
     * List<KuraGPIOPin> pins = gpioService.getPins(description);
     * </pre>
     * 
     * To get all pins on a specific name and line, you can call:
     * <pre>
     * Map<String, String> description = new HashMap<>();
     * description.put("name", "GPIO22");
     * description.put("line", "5");
     * List<KuraGPIOPin> pins = gpioService.getPins(description);
     * </pre>
     * 
     * @param description a map of property names to values used to filter pins; may contain
     *        a subset of all supported properties
     * 
     * @return a list of {@code KuraGPIOPin} instances matching the specified properties
     * @since 3.0
     */
    public List<KuraGPIOPin> getPins(Map<String, String> description);
    
    /**
     * Get the GPIO pins that match the given description properties, setting the direction, mode and trigger.
     * 
     * The description map is interpreted as a set of filter criteria:
     * only the properties present in the map are considered when selecting
     * pins. Pins that match all provided properties are returned; properties
     * that are not specified are ignored.
     * 
     * For example, to get all pins with a specific name, you can call:
     * <pre>
     * Map<String, String> description = new HashMap<>();
     * description.put("name", "GPIO22");
     * List<KuraGPIOPin> pins = gpioService.getPins(description, direction, mode, trigger);
     * </pre>
     * 
     * To get all pins on a specific name and line, you can call:
     * <pre>
     * Map<String, String> description = new HashMap<>();
     * description.put("name", "GPIO22");
     * description.put("line", "5");
     * List<KuraGPIOPin> pins = gpioService.getPins(description, direction, mode, trigger);
     * </pre>
     * @param description a map of properties describing the pin
     * @param direction the direction of the pin
     * @param mode the mode of the pin
     * @param trigger the trigger of the pin
     * 
     * @return a list of KuraGPIOPin instances
     * @since 3.0
     */
    public List<KuraGPIOPin> getPins(Map<String, String> description, KuraGPIODirection direction, KuraGPIOMode mode,
            KuraGPIOTrigger trigger);
    
    /**
     * Get the {@link KuraGPIODescription}s of all available GPIO pins.
     * 
     * @return a list of available GPIO descriptions
     * @since 3.0
     */
    public List<KuraGPIODescription> getAvailablePinDescriptions();
    

}
