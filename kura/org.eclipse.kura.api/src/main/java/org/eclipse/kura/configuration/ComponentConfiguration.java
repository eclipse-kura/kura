/*******************************************************************************
 * Copyright (c) 2011, 2016 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.configuration;

import java.util.Map;

import org.eclipse.kura.configuration.metatype.OCD;
import org.osgi.annotation.versioning.ProviderType;

/**
 * The ComponentConfiguration groups all the information related to the Configuration of a Component.
 * It provides access to parsed ObjectClassDefintion associated to this Component.
 * ComponentConfiguration does not reuse the OSGi ObjectClassDefinition as the latter
 * does not provide access to certain aspects such as the required attribute,
 * the min and max values. Instead it returns the raw ObjectClassDefintion as parsed
 * from the MetaType Information XML resource associated to this Component.
 * 
 * @noimplement This interface is not intended to be implemented by clients.
 */
@ProviderType
public interface ComponentConfiguration {

    /**
     * Returns the PID (service's persistent identity) of the component
     * associated to this Configuration.
     * For a component created via {@link ConfigurationService#createFactoryConfiguration(String, String, Map, boolean)}
     * ,
     * the service's persistent identity is the value of the second parameter of that method;
     * at runtime, the same value is also available in the {@link ConfigurationService#KURA_SERVICE_PID}
     * property of one of the configurations of the associated Factory Component.
     * Otherwise, the service's persistent identity is defined as the name attribute of the
     * Component Descriptor XML file; at runtime, the same value is also available
     * in the component.name and in the service.pid properties of the Component Configuration.
     * If not already specified by the component, the Configuration Service will automatically
     * set the {@link ConfigurationService#KURA_SERVICE_PID} to the value of service.pid when
     * the component is first updated.
     *
     * @return PID of the component associated to this Configuration.
     */
    public String getPid();

    /**
     * Returns the raw ObjectClassDefinition as parsed from the MetaType
     * Information XML resource associated to this Component.
     *
     * @return
     */
    public OCD getDefinition();

    /**
     * Returns the Dictionary of properties currently used by this component.
     *
     * @return the Component's Configuration properties.
     */
    public Map<String, Object> getConfigurationProperties();

}
