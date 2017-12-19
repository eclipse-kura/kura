/*******************************************************************************
 * Copyright (c) 2017 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.configuration.metatype;

import java.util.List;

import org.eclipse.kura.configuration.ComponentConfiguration;
import org.eclipse.kura.configuration.ConfigurationService;
import org.osgi.annotation.versioning.ProviderType;

/**
 * This Interface provides the capability to get the OCDs for the specified Factories or Service Providers.
 *
 * @noimplement This interface is not intended to be implemented by clients.
 * @since 1.4
 */
@ProviderType
public interface OCDService {

    /**
     * Returns the {@link OCD}s of all registered component factories for which an OSGi MetaType is
     * is registered in the {@link ConfigurationService}.
     * <br>
     * The {@link OCD}s are returned in the form of a {@link ComponentConfiguraion} instance. The component factory pid
     * can be obtained by calling {@link ComponentConfiguration#getPid()} method and the actual {@link OCD} can be
     * obtained by calling the {@link ComponentConfiguration#getDefinition()} method.
     *
     *
     * @return The list of the component factory {@link OCD}s.
     * @since 1.4
     */
    public List<ComponentConfiguration> getFactoryComponentOCDs();

    /**
     * Returns the {@link OCD} of the registered component factory identified by the provided {@code factoryPid} and for
     * which an OSGi MetaType is registered in the {@link ConfigurationService}.
     * <br>
     * The {@link OCD} is returned in the form of a {@link ComponentConfiguraion} instance. The component factory pid
     * can be obtained by calling {@link ComponentConfiguration#getPid()} method and the actual {@link OCD} can be
     * obtained by calling the {@link ComponentConfiguration#getDefinition()} method.
     *
     * @return The {@link OCD} for the requested component factory if available, or null otherwise
     * @since 1.4
     */
    public ComponentConfiguration getFactoryComponentOCD(String factoryPid);

    /**
     * Searches the Declarative Services layer for Components implementing any of the specified services and returns a
     * {@link ComponentConfiguration} instance describing it.
     * If the {@link ConfigurationService} contains a registered OSGi MetaType for a found Component, it will be
     * returned inside the {@link ComponentConfiguration} instance.
     * <br>
     * The {@link ComponentConfiguraion} instances in the returned list contain the following information:
     * <ul>
     * <li>
     * The {@link ComponentConfiguration#getPid()} method returns the Component name.
     * </li>
     * <li>
     * The {@link ComponentConfiguration#getDefinition()} method returns the {@link OCD} for the found Component if
     * it is known to the {@link ConfigurationService}, or {@code null} otherwise.
     * </li>
     * </ul>
     *
     * @param clazzes
     *            The list of service classes to be used as search filter.
     * @return A list of {@link ComponentConfiguration} instances representing the found Components.
     * @since 1.4
     */
    public List<ComponentConfiguration> getServiceProviderOCDs(Class<?>... classes);

    /**
     * @see {@link ConfigurationService#getServiceProviderOCDs(Class...)
     *
     * @param classNames
     *            The list of service class or interface names to be used as search filter.
     * @return A list of {@link ComponentConfiguration} instances representing the found Components.
     * @since 1.4
     */
    public List<ComponentConfiguration> getServiceProviderOCDs(String... classNames);
}
