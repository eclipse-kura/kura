/*******************************************************************************
 * Copyright (c) 2016, 2018 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *     Red Hat Inc
 *******************************************************************************/
package org.eclipse.kura.web.shared.service;

import java.util.List;

import org.eclipse.kura.cloud.CloudService;
import org.eclipse.kura.cloud.factory.CloudServiceFactory;
import org.eclipse.kura.web.shared.GwtKuraException;
import org.eclipse.kura.web.shared.model.GwtCloudComponentFactories;
import org.eclipse.kura.web.shared.model.GwtCloudConnectionEntry;
import org.eclipse.kura.web.shared.model.GwtCloudEntry;
import org.eclipse.kura.web.shared.model.GwtXSRFToken;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

/**
 * This interface provide a set of methods to manage cloud services from the web UI.
 * In particular, it provides a way to generate or delete Cloud Services from a defined factory.
 *
 * Furthermore, it provides a way to get the list of all the cloud services instances {@link #findCloudServices()}, all
 * the registered cloud services factories {@link #findCloudServiceFactories()} or the list of PIDs that compose a
 * specified cloud service stack {@link #findStackPidsByFactory(String, String)}.
 *
 * This interface provides two additional methods that can be used in the client part, during the creation of a new
 * cloud service stack instance. In particular,
 * {@link #findSuggestedCloudServicePid(String)} will return a String representing the suggested cloud service PID, as
 * defined by the factory referenced.
 * {@link #findCloudServicePidRegex(String)} will return, if exists, the regex specified by the referenced factory that
 * will be used to validate the user input.
 *
 */
@RemoteServiceRelativePath("cloudservices")
public interface GwtCloudConnectionService extends RemoteService {

    /**
     * Returns the list of all the {@link org.eclipse.kura.cloud.CloudService} instances registered in the framework. An
     * entry will be added to the returned result only if the {@link org.eclipse.kura.cloud.CloudService} implementation
     * specifies the {@link org.eclipse.kura.cloud.factory.CloudServiceFactory} KURA_CLOUD_SERVICE_FACTORY_PID property.
     *
     * @return the list of {@link GwtCloudConnectionEntry} that represent the
     *         different {@link org.eclipse.kura.cloud.CloudService} instances registered in the framework.
     * @throws GwtKuraException
     *             when service referencing fails
     */
    public List<GwtCloudEntry> findCloudEntries() throws GwtKuraException;

    /**
     * Returns a list of PIDs that compose the cloud stack referenced by the specified factory and cloud service.
     *
     * @param factoryPid
     *            the PID of the {@link CloudServiceFactory} this CloudService was registered with
     * @param cloudServicePid
     *            the PID of the selected Cloud Service
     * @return the list of <i>kura.service.pid</i>s associated with the specified factory component
     *         configuration.
     * @throws GwtKuraException
     *             when service referencing fails
     * @throws GwtKuraException
     *             if the invocation of the corresponding factory method returns an exception
     */
    public List<String> findStackPidsByFactory(String factoryPid, String cloudServicePid) throws GwtKuraException;

    /**
     * Returns a string that represents the suggested cloud service PID, for the specified factory. If the factory does
     * not specify a value, null is returned.
     *
     * @param factoryPid
     *            the factory PID of the Factory Component
     * @return a String that represents the suggested cloud service PID. Null otherwise.
     * @throws GwtKuraException
     *             when service referencing fails
     */
    public String findSuggestedCloudServicePid(String factoryPid) throws GwtKuraException;

    /**
     * Returns a string representing the regex specified by the specified Factory Component.
     *
     * @param factoryPid
     *            the factory PID of the Factory Component
     * @return a String representing the regex to be used to verify the user's input. A null value is returned if the
     *         factory does not provide such value.
     * @throws GwtKuraException
     *             when service referencing fails
     */
    public String findCloudServicePidRegex(String factoryPid) throws GwtKuraException;

    /**
     * Invokes the creation of a new cloud stack by the specified Factory Component, using the specified Cloud Service
     * PID.
     *
     * @param xsrfToken
     *            the cross site request forgery token
     * @param factoryPid
     *            the factory PID of the Factory Component
     * @param cloudServicePid
     *            the Kura persistent identifier, <i>kura.service.pid</i>, of the factory component configuration.
     * @throws GwtKuraException
     *             when service referencing fails
     * @throws GwtKuraException
     *             if the creation and initialization of a {@link CloudService} instance fails.
     */
    public void createCloudServiceFromFactory(GwtXSRFToken xsrfToken, String factoryPid, String cloudServicePid)
            throws GwtKuraException;

    /**
     * Invokes the deletion of the associated {@link CloudService} instance by the specified Factory Component.
     *
     * @param xsrfToken
     *            the cross site request forgery token
     * @param factoryPid
     *            the factory PID of the Factory Component
     * @param cloudServicePid
     *            the Kura persistent identifier, <i>kura.service.pid</i>, of the factory component configuration.
     * @throws GwtKuraException
     *             when service referencing fails
     * @throws GwtKuraException
     *             if the deletion of the specified {@link CloudService} instance fails.
     */
    public void deleteCloudServiceFromFactory(GwtXSRFToken xsrfToken, String factoryPid, String cloudServicePid)
            throws GwtKuraException;

    public GwtCloudComponentFactories getCloudComponentFactories() throws GwtKuraException;

    public void createPubSubInstance(GwtXSRFToken xsrfToken, String pid, String factoryPid, String cloudConnectionPid)
            throws GwtKuraException;

    public void deletePubSubInstance(GwtXSRFToken xsrfToken, String pid) throws GwtKuraException;

}
