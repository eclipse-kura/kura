/*******************************************************************************
 * Copyright (c) 2011, 2021 WinWinIt and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *  WinWinIt
 *******************************************************************************/
package org.eclipse.kura.core.extendedproperties;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.eclipse.kura.system.ExtendedProperties;
import org.eclipse.kura.system.ExtendedPropertiesEvent;
import org.eclipse.kura.system.ExtendedPropertiesHolder;
import org.eclipse.kura.system.ExtendedPropertiesHolder.Property;
import org.eclipse.kura.system.ExtendedPropertiesService;
import org.eclipse.kura.system.ExtendedPropertyGroup;
import org.osgi.service.event.EventAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of the {@link ExtendedPropertiesService} which collects MQTT birth certificate extended properties
 * from all registered {@link ExtendedPropertiesHolder} services.
 */
public class ExtendedPropertiesServiceImpl implements ExtendedPropertiesService {

    /** Logger. */
    private static final Logger logger = LoggerFactory.getLogger(ExtendedPropertiesServiceImpl.class);

    /** Mutex. */
    private final Object mutex = new Object();

    /** Event admin instance. */
    private EventAdmin eventAdmin;

    /** Currently registered {@link ExtendedPropertiesHolder} services. */
    private final List<ExtendedPropertiesHolder> extendedPropertiesHolders = new ArrayList<>();

    /**
     * Sets the {@link EventAdmin} instance.
     * 
     * @param eventAdmin
     *                       {@link EventAdmin} instance.
     */
    public void setEventAdmin(EventAdmin eventAdmin) {
        synchronized (this.mutex) {
            this.eventAdmin = eventAdmin;
        }
    }

    /**
     * Unsets the {@link EventAdmin} instance.
     * 
     * @param eventAdmin
     *                       {@link EventAdmin} instance.
     */
    public void unsetEventAdmin(EventAdmin eventAdmin) {
        synchronized (this.mutex) {
            this.eventAdmin = null;
        }
    }

    /**
     * Adds an {@link ExtendedPropertiesHolder}.
     * 
     * @param holder
     *                   {@link ExtendedPropertiesHolder} instance.
     */
    public void addExtendedPropertiesHolder(ExtendedPropertiesHolder holder) {
        synchronized (this.mutex) {
            this.extendedPropertiesHolders.add(holder);
            tryPostPropertiesChangedEvent();
        }
    }

    /**
     * Removes an {@link ExtendedPropertiesHolder}.
     * 
     * @param holder
     *                   {@link ExtendedPropertiesHolder} instance.
     */
    public void removeExtendedPropertiesHolder(ExtendedPropertiesHolder holder) {
        synchronized (this.mutex) {
            this.extendedPropertiesHolders.remove(holder);
            tryPostPropertiesChangedEvent();
        }
    }

    /**
     * Sends an extended properties changed event. Any error will be logged and suppressed.
     */
    private void tryPostPropertiesChangedEvent() {
        synchronized (mutex) {
            if (this.eventAdmin != null) {
                try {
                    this.eventAdmin.sendEvent(new ExtendedPropertiesEvent());
                } catch (Exception e) {
                    logger.error("Error sending extended properties changed event", e);
                }
            }
        }
    }

    @Override
    public Optional<ExtendedProperties> getExtendedProperties() {

        // Get extended properties holder services
        List<ExtendedPropertiesHolder> services;
        synchronized (extendedPropertiesHolders) {
            services = new ArrayList<>(extendedPropertiesHolders);
        }

        // Retrieve properties
        List<ExtendedPropertiesHolder.Property> properties = new ArrayList<>();
        for (ExtendedPropertiesHolder service : services) {
            try {
                properties.addAll(service.getProperties());
            } catch (Exception e) {
                logger.error("Error retrieving extended properties from service {}. Properties will be ignored.",
                        service.getClass().getSimpleName(), e);
            }
        }

        // Convert to ExtendedProperties
        ExtendedProperties extendedProperties = null;
        try {
            extendedProperties = toExtendedProperties(properties);
        } catch (Exception e) {
            logger.error("Error converting properties to ExtendedProperties. Extended properties will be ignored.", e);
        }

        // Return result
        return extendedProperties != null ? Optional.of(extendedProperties) : Optional.empty();

    }

    /**
     * Converts a list of flat {@link Property} into an {@link ExtendedProperties} object.
     * 
     * @param properties
     *                       Properties.
     * @return Equivalent {@link ExtendedProperties}.
     */
    private ExtendedProperties toExtendedProperties(List<ExtendedPropertiesHolder.Property> properties) {

        // Build a map having the group name as the key and a properties name - value map as the value
        Map<String, Map<String, String>> propertiesGroups = properties.stream()
                .collect(Collectors.groupingBy(Property::getGroupName, Collectors.mapping(g -> g,
                        Collectors.toMap(Property::getName, p -> p.getValue() != null ? p.getValue() : ""))));

        // Convert to ExtendedPropertyGroup
        List<ExtendedPropertyGroup> propsGroups = propertiesGroups.entrySet().stream()
                .map(e -> new ExtendedPropertyGroup(e.getKey(), e.getValue())).collect(Collectors.toList());

        // Check if at least one property exists
        if (propsGroups.isEmpty()) {
            return null;
        }

        // Build and return extended properties final object
        return new ExtendedProperties("1.0.0", propsGroups);
    }

}
