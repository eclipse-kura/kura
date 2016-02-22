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

/**
 * Marker interface for all Service Component that wants to expose the Configuration through the ConfigurationService.
 * The Configuration Service tracks all OSGi Components which implement the {@see ConfigurableComponent} marker interface.
 * When a ConfigurableComponent is registered, the Configuration Service will call its "update"
 * method with the latest saved configuration as returned the ConfigurationAdmin or, if none
 * is available, with the Configuration properties fabricated from the default attribute values as 
 * specified in the ObjectClassDefinition of this service.
 * In OSGi terms, this process in similar to the Auto Configuration Service.
 * <b>The ConfigurationService assumes that Meta Type Information XML resource
 * for a given ConfigurableComponent with name abc" to be stored under OSGI-INF/metatype/abc.xml.</b>
 * This is an extra restriction over the OSGi specification: the Meta Type Information XML resource
 * must be named as the name of the Declarative Service Component. 
 * <br>
 */
public interface ConfigurableComponent 
{
}
