/*******************************************************************************
 * Copyright (c) 2011, 2020 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.configuration;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.kura.KuraException;
import org.osgi.annotation.versioning.ProviderType;
import org.osgi.framework.Filter;

/**
 * The Configuration Service is used to manage the configuration of OSGi Declarative Services
 * implementing the {@link ConfigurableComponent} or the {@link SelfConfiguringComponent}
 * interface.
 * It works in concert with the OSGi Configuration Admin and the OSGi Meta Type services.
 * What it provides over the native OSGi services is the ability to easily access the current
 * configuration of a Configurable Component or a SelfConfiguring Component together with
 * their full Meta Type Object Class Definition,
 * the ability to take snapshots of the current configuration of all managed
 * services or rollback to older snapshots and, finally, the ability to access
 * and manage configurations remotely through the Cloud Service.
 * <br>
 * <br>
 * The Configuration Service operates on a subset of all the services registered
 * under the OSGi container.
 * It tracks only OSGi services which implement the ConfigurableComponent
 * or the SelfConfiguringComponent interface.
 * More explicitly it does not manage the following services of the OSGi specification:
 * <ul>
 * <li>Managed Service
 * <li>Managed Service Factory
 * <li>Component Factory of the Declarative Service specification
 * </ul>
 * When a ConfigurableComponent or a SelfConfiguringComponent is tracked,
 * the Configuration Service will update its Configuration,
 * as returned by the Configuration Admin, with properties fabricated
 * from the default attribute values specified in its Meta Type Object Class Definition.
 * <br>
 * The configuration properties will be passed in the <i>activate</i> or <i>update</i> methods
 * of the Component definition.
 * <br>
 * In OSGi terms, this process in similar to the Auto Configuration Service.
 * <br>
 * <br>
 * The Configuration Service assumes the Meta Type Information XML resource
 * for a given Declarative Service with name "abc" to be stored under OSGI-INF/metatype/abc.xml.
 * <br>
 * This is an extra restriction over the OSGi specification:
 * the Meta Type Information XML resource
 * must be named as the name of the Declarative Service Component.
 * <br>
 * <br>
 * The Configuration Service has the ability to create a snapshot for the current configuration
 * of all the tracked components. The snapshot is saved in the form of an
 * XML file stored under $kura.snapshots/snapshot_epoch.xml where epoch is replaced
 * with the epoch timestamp at the time of the snapshot creation.
 * The Configuration Service also has the ability to rollback the configuration of
 * tracked components taking them back to a previous stored snapshot.
 * <br>
 * <br>
 * The Configuration Service also allows creation of new ConfigurableComponents instances
 * through OSGi Declarative Services by creating a new component Configuration via the OSGi Configuration Admin.
 * The approach is <a href=
 * "http://www.jeremias-maerki.ch/wordpress/de/2010/04/22/osgi-ds-configuring-multiple-instances-of-a-component/">using
 * OSGi Declative Services to configure multiple instances of a component</a>.
 * <br>
 * <br>
 * In order to manage the configuration of multiple component instances from the same configuration factory,
 * Kura relies on the following configuration properties:
 * <ul>
 * <li><b>service.factoryPid</b> is set by the Configuration Admin when the component configuration is first created.
 * Its value has to match the name of the targeted Declarative Service Component
 * <li><b>kura.service.pid</b> is the persistent identity assigned by Kura when the component configuration
 * is first created calling {@link #createFactoryConfiguration(String, String, Map, boolean)}.
 * </ul>
 * Both properties are stored in snapshots to recreate the component instances and restore their configuration
 * at every framework restart.
 *
 * @noimplement This interface is not intended to be implemented by clients.
 */
@ProviderType
public interface ConfigurationService {

    /**
     * The name of the the Kura persistent identity property.
     *
     * @since 1.0.8
     */
    public static final String KURA_SERVICE_PID = "kura.service.pid";

    /**
     * Returns the list of Meta Type factory PIDs tracked by the Configuration Service.
     *
     * @return the list of Meta Type factory PIDs.
     *
     * @since 1.0.8
     */
    public Set<String> getFactoryComponentPids();

    /**
     * Creates a new ConfigurableComponent instance by creating a new configuration from a
     * Configuration Admin factory.
     * The Configuration Service will use the Configuration Admin
     * to create the component configuration and will update the component configuration with the provided optional
     * properties.
     *
     * @param factoryPid
     *            the ID of the factory.
     * @param pid
     *            the value of the {@link ConfigurationService#KURA_SERVICE_PID}
     *            that will be set in the component configuration properties.
     * @param properties
     *            an optional map of properties describing the component configuration.
     *            The Configuration Service will first create an initial map of properties
     *            with the property {@link ConfigurationService#KURA_SERVICE_PID}
     *            set to the provided <i>pid</i> parameter and
     *            with the default component configuration from its OSGi Meta Type.
     *            If <i>properties</i> is not null, the Configuration Service will first remove
     *            {@link ConfigurationService#KURA_SERVICE_PID}
     *            from the provided properties and then it will merge them with the initial map.
     * @param takeSnapshot
     *            if set to true a snapshot will be taken.
     * @throws KuraException
     *             if pid is null, it already exists or creation fails.
     *
     * @since 1.0.8
     */
    public void createFactoryConfiguration(String factoryPid, String pid, Map<String, Object> properties,
            boolean takeSnapshot) throws KuraException;

    /**
     * Deletes the ConfigurableComponent instance having the specified PID.
     * Removes the component configuration and takes a new snapshot.
     *
     * @param pid
     *            the PID of the component instance to delete.
     * @param takeSnapshot
     *            if set to true a snapshot will be taken.
     * @throws KuraException
     *             if the PID is not found or if the component instance was not created
     *             via {@link #createFactoryConfiguration(String, String, Map, boolean)}.
     *
     * @since 1.0.8
     */
    public void deleteFactoryConfiguration(String pid, boolean takeSnapshot) throws KuraException;

    /**
     * Return the PIDs for all the services that
     * implements the ConfigurableComponent Maker interface and registered themselves
     * with the container.
     *
     * @return list of PIDs for registered ConfigurableComponents
     */
    public Set<String> getConfigurableComponentPids();

    /**
     * Returns the list of ConfigurableComponents currently registered with the ConfigurationService.
     *
     * @return list of registered ConfigurableComponents
     */
    public List<ComponentConfiguration> getComponentConfigurations() throws KuraException;

    /**
     * Returns the list of ConfigurableComponents currently registered with the ConfigurationService that match the
     * provided OSGi filter.
     *
     * @param filter
     *            the filter to be applied
     * @return list of registered ConfigurableComponents
     *
     * @since 2.1
     */
    public List<ComponentConfiguration> getComponentConfigurations(Filter filter) throws KuraException;

    /**
     * Returns the ComponentConfiguration for the component identified with specified PID.
     *
     * @param pid
     *            The ID of the component whose configuration is requested.
     * @return ComponentConfiguration of the requested Component.
     */
    public ComponentConfiguration getComponentConfiguration(String pid) throws KuraException;

    /**
     * Returns the default ComponentConfiguration
     * for the component having the specified PID.
     *
     * @param pid
     *            The ID of the component whose configuration is requested.
     * @return the ComponentConfiguration of the requested Component.
     * @throws KuraException
     *
     * @since 1.0.8
     */
    public ComponentConfiguration getDefaultComponentConfiguration(String pid) throws KuraException;

    /**
     * Updates the Configuration of the registered component with the specified PID.
     * Using the OSGi ConfigurationAdmin, it retrieves the Configuration of the
     * component with the specified PID and then sends an update using the
     * specified properties.
     * <br>
     * If the component to be updated is not yet registered with the ConfigurationService,
     * it is first registered and then it is updated with the specified properties.
     * Before updating the component, the specified properties are validated against
     * the ObjectClassDefinition associated to the Component. The Configuration Service
     * is fully compliant with the OSGi MetaType Information and the validation happens
     * through the OSGi MetaType Service.
     * <br>
     * The Configuration Service is compliant with the OSGi MetaType Service so
     * it accepts all attribute types defined in the OSGi Compendium Specifications.
     * <br>
     *
     * @param pid
     *            The PID of the component whose configuration is requested.
     * @param properties
     *            Properties to be used as the new Configuration for the specified Component.
     * @throws KuraException
     *             if the properties specified do not pass the validation of the ObjectClassDefinition
     */
    public void updateConfiguration(String pid, Map<String, Object> properties) throws KuraException;

    /**
     * Updates the Configuration of the registered component with the specified pid.
     * Using the OSGi ConfigurationAdmin, it retrieves the Configuration of the
     * component with the specified PID and then send an update using the
     * specified properties.
     * <br>
     * If the component to be updated is not yet registered with the ConfigurationService,
     * it is first registered and then it is updated with the specified properties.
     * Before updating the component, the specified properties are validated against
     * the ObjectClassDefinition associated to the Component. The Configuration Service
     * is fully compliant with the OSGi MetaType Information and the validation happens
     * through the OSGi MetaType Service.
     * <br>
     * The Configuration Service is compliant with the OSGi MetaType Service so
     * it accepts all attribute types defined in the OSGi Compendium Specifications.
     * <br>
     *
     * @param pid
     *            The PID of the component whose configuration is requested.
     * @param properties
     *            Properties to be used as the new Configuration for the specified Component.
     * @param takeSnapshot
     *            defines whether or not this configuration update should trigger a snapshot.
     * @throws KuraException
     *             if the properties specified do not pass the validation of the ObjectClassDefinition.
     *
     * @since 1.0.8
     */
    public void updateConfiguration(String pid, Map<String, Object> properties, boolean takeSnapshot)
            throws KuraException;

    /**
     * Updates the Configuration of the registered components.
     * Using the OSGi ConfigurationAdmin, it retrieves the Configuration of the
     * component with the specified PID and then send an update using the
     * specified properties.
     * <br>
     * If the component to be updated is not yet registered with the ConfigurationService,
     * it is first registered and then it is updated with the specified properties.
     * Before updating the component, the specified properties are validated against
     * the ObjectClassDefinition associated to the Component. The Configuration Service
     * is fully compliant with the OSGi MetaType Information and the validation happens
     * through the OSGi MetaType Service.
     * <br>
     * The Configuration Service is compliant with the OSGi MetaType Service so
     * it accepts all attribute types defined in the OSGi Compendium Specifications.
     * <br>
     *
     * @param configs
     *            The list of ComponentConfiguration whose update is requested.
     * @throws KuraException
     *             if the properties specified do not pass the validation of the ObjectClassDefinition
     */
    public void updateConfigurations(List<ComponentConfiguration> configs) throws KuraException;

    /**
     * Updates the Configuration of the registered components.
     * Using the OSGi ConfigurationAdmin, it retrieves the Configuration of the
     * component with the specified PID and then send an update using the
     * specified properties.
     * <br>
     * If the component to be updated is not yet registered with the ConfigurationService,
     * it is first registered and then it is updated with the specified properties.
     * Before updating the component, the specified properties are validated against
     * the ObjectClassDefinition associated to the Component. The Configuration Service
     * is fully compliant with the OSGi MetaType Information and the validation happens
     * through the OSGi MetaType Service.
     * <br>
     * The Configuration Service is compliant with the OSGi MetaType Service so
     * it accepts all attribute types defined in the OSGi Compendium Specifications.
     * <br>
     *
     * @param configs
     *            The list of ComponentConfiguration whose update is requested.
     * @param takeSnapshot
     *            defines whether or not this configuration update should trigger a snapshot.
     * @throws KuraException
     *             if the properties specified do not pass the validation of the ObjectClassDefinition
     *
     * @since 1.0.8
     */
    public void updateConfigurations(List<ComponentConfiguration> configs, boolean takeSnapshot) throws KuraException;

    /**
     * Returns the ID of all the snapshots taken by the ConfigurationService.
     * The snapshot ID is the epoch time at which the snapshot was taken.
     * The snapshots are stored in the KuraHome/snapshots/ directory.
     * This API will return all the snpashot files available in that location.
     *
     * @return IDs of the snapshots available.
     * @throws KuraException
     */
    public Set<Long> getSnapshots() throws KuraException;

    /**
     * Loads a snapshot given its ID and return the component configurations stored in that snapshot.
     *
     * @param sid
     *            - ID of the snapshot to be loaded
     * @return List of ComponentConfigurations contained in the snapshot
     * @throws KuraException
     */
    public List<ComponentConfiguration> getSnapshot(long sid) throws KuraException;

    /**
     * Takes a new snapshot of the current configuration of all the registered ConfigurableCompoenents.
     * It returns the ID of a snapshot as the epoch time at which the snapshot was taken.
     *
     * @return the ID of the snapshot.
     * @throws KuraException
     */
    public long snapshot() throws KuraException;

    /**
     * Rolls back to the last saved snapshot if available.
     *
     * @return the ID of the snapshot it rolled back to
     * @throws KuraException
     *             if no snapshots are available or
     */
    public long rollback() throws KuraException;

    /**
     * Rolls back to the specified snapshot id.
     *
     * @param id
     *            ID of the snapshot we need to rollback to
     * @throws KuraException
     *             if the snapshot is not found
     */
    public void rollback(long id) throws KuraException;
}
