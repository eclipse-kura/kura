/*******************************************************************************
 * Copyright (c) 2016 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.web.server;

import static org.eclipse.kura.cloud.factory.CloudServiceFactory.KURA_CLOUD_SERVICE_FACTORY_PID;
import static org.eclipse.kura.configuration.ConfigurationService.KURA_SERVICE_PID;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.cloud.CloudService;
import org.eclipse.kura.cloud.factory.CloudServiceFactory;
import org.eclipse.kura.web.server.util.ServiceLocator;
import org.eclipse.kura.web.shared.GwtKuraErrorCode;
import org.eclipse.kura.web.shared.GwtKuraException;
import org.eclipse.kura.web.shared.model.GwtCloudConnectionEntry;
import org.eclipse.kura.web.shared.model.GwtGroupedNVPair;
import org.eclipse.kura.web.shared.model.GwtXSRFToken;
import org.eclipse.kura.web.shared.service.GwtCloudService;
import org.osgi.framework.ServiceReference;

public class GwtCloudServiceImpl extends OsgiRemoteServiceServlet implements GwtCloudService {

    private static final long serialVersionUID = 2595835826149606703L;

    private static final String COMPONENT_NAME = "component.name";
    private static final String SERVICE_FACTORY_PID = "service.factoryPid";
    private static final String KURA_UI_CSF_PID_DEFAULT = "kura.ui.csf.pid.default";
    private static final String KURA_UI_CSF_PID_REGEX = "kura.ui.csf.pid.regex";

    @Override
    public List<GwtCloudConnectionEntry> getCloudServices() throws GwtKuraException {
        List<GwtCloudConnectionEntry> pairs = new ArrayList<GwtCloudConnectionEntry>();
        Collection<ServiceReference<CloudService>> cloudServiceReferences = ServiceLocator.getInstance()
                .getServiceReferences(CloudService.class, null);

        for (ServiceReference<CloudService> cloudServiceReference : cloudServiceReferences) {
            String cloudServicePid = (String) cloudServiceReference.getProperty(KURA_SERVICE_PID);
            String factoryPid = (String) cloudServiceReference.getProperty(SERVICE_FACTORY_PID);
            CloudService cloudService = ServiceLocator.getInstance().getService(cloudServiceReference);

            if (cloudServiceReference.getProperty(KURA_CLOUD_SERVICE_FACTORY_PID) != null) {
                GwtCloudConnectionEntry cloudConnectionEntry = new GwtCloudConnectionEntry();
                cloudConnectionEntry.setConnectionStatus(cloudService.isConnected());
                cloudConnectionEntry.setCloudFactoryPid(factoryPid);
                cloudConnectionEntry.setCloudServicePid(cloudServicePid);
                pairs.add(cloudConnectionEntry);
            }
            ServiceLocator.getInstance().ungetService(cloudServiceReference);
        }
        return pairs;
    }

    @Override
    public List<GwtGroupedNVPair> getCloudServiceFactories() throws GwtKuraException {
        List<GwtGroupedNVPair> pairs = new ArrayList<GwtGroupedNVPair>();
        Collection<ServiceReference<CloudServiceFactory>> cloudServiceFactoryReferences = ServiceLocator.getInstance()
                .getServiceReferences(CloudServiceFactory.class, null);

        for (ServiceReference<CloudServiceFactory> cloudServiceFactoryReference : cloudServiceFactoryReferences) {
            CloudServiceFactory cloudServiceFactory = ServiceLocator.getInstance()
                    .getService(cloudServiceFactoryReference);
            pairs.add(new GwtGroupedNVPair("cloudFactories", "factoryPid", cloudServiceFactory.getFactoryPid()));

            ServiceLocator.getInstance().ungetService(cloudServiceFactoryReference);
        }
        return pairs;
    }

    @Override
    public List<String> getStackPidsByFactory(String factoryPid, String cloudServicePid) throws GwtKuraException {
        List<String> componentPids = new ArrayList<String>();
        Collection<ServiceReference<CloudServiceFactory>> cloudServiceFactoryReferences = ServiceLocator.getInstance()
                .getServiceReferences(CloudServiceFactory.class, null);

        for (ServiceReference<CloudServiceFactory> cloudServiceFactoryReference : cloudServiceFactoryReferences) {
            if (!cloudServiceFactoryReference.getProperty(COMPONENT_NAME).equals(factoryPid)) {
                continue;
            }
            CloudServiceFactory cloudServiceFactory = ServiceLocator.getInstance()
                    .getService(cloudServiceFactoryReference);
            try {
                componentPids.addAll(cloudServiceFactory.getStackComponentsPids(cloudServicePid));
            } catch (KuraException e) {
                throw new GwtKuraException(GwtKuraErrorCode.INTERNAL_ERROR, e);
            } finally {
                ServiceLocator.getInstance().ungetService(cloudServiceFactoryReference);
            }
        }
        return componentPids;
    }

    @Override
    public void createCloudServiceFromFactory(GwtXSRFToken xsrfToken, String factoryPid, String cloudServicePid)
            throws GwtKuraException {
        checkXSRFToken(xsrfToken);
        if (factoryPid == null || factoryPid.trim().isEmpty() || cloudServicePid == null
                || cloudServicePid.trim().isEmpty()) {
            throw new GwtKuraException(GwtKuraErrorCode.ILLEGAL_NULL_ARGUMENT);
        }

        Collection<ServiceReference<CloudServiceFactory>> cloudServiceFactoryReferences = ServiceLocator.getInstance()
                .getServiceReferences(CloudServiceFactory.class, null);

        for (ServiceReference<CloudServiceFactory> cloudServiceFactoryReference : cloudServiceFactoryReferences) {
            CloudServiceFactory cloudServiceFactory = ServiceLocator.getInstance()
                    .getService(cloudServiceFactoryReference);
            try {
                if (!cloudServiceFactory.getFactoryPid().equals(factoryPid)) {
                    continue;
                }
                cloudServiceFactory.createConfiguration(cloudServicePid);
            } catch (KuraException e) {
                throw new GwtKuraException(GwtKuraErrorCode.INTERNAL_ERROR, e);
            } finally {
                ServiceLocator.getInstance().ungetService(cloudServiceFactoryReference);
            }
        }
    }

    @Override
    public void deleteCloudServiceFromFactory(GwtXSRFToken xsrfToken, String factoryPid, String cloudServicePid)
            throws GwtKuraException {
        if (factoryPid == null || factoryPid.trim().isEmpty() || cloudServicePid == null
                || cloudServicePid.trim().isEmpty()) {
            throw new GwtKuraException(GwtKuraErrorCode.ILLEGAL_NULL_ARGUMENT);
        }

        Collection<ServiceReference<CloudServiceFactory>> cloudServiceFactoryReferences = ServiceLocator.getInstance()
                .getServiceReferences(CloudServiceFactory.class, null);

        for (ServiceReference<CloudServiceFactory> cloudServiceFactoryReference : cloudServiceFactoryReferences) {
            CloudServiceFactory cloudServiceFactory = ServiceLocator.getInstance()
                    .getService(cloudServiceFactoryReference);
            try {
                if (!cloudServiceFactory.getFactoryPid().equals(factoryPid)) {
                    continue;
                }
                cloudServiceFactory.deleteConfiguration(cloudServicePid);
            } catch (KuraException e) {
                throw new GwtKuraException(GwtKuraErrorCode.INTERNAL_ERROR, e);
            } finally {
                ServiceLocator.getInstance().ungetService(cloudServiceFactoryReference);
            }
        }
    }

    @Override
    public String getSuggestedCloudServicePid(String factoryPid) throws GwtKuraException {
        Collection<ServiceReference<CloudServiceFactory>> cloudServiceFactoryReferences = ServiceLocator.getInstance()
                .getServiceReferences(CloudServiceFactory.class, null);

        for (ServiceReference<CloudServiceFactory> cloudServiceFactoryReference : cloudServiceFactoryReferences) {
            CloudServiceFactory cloudServiceFactory = ServiceLocator.getInstance()
                    .getService(cloudServiceFactoryReference);
            if (!cloudServiceFactory.getFactoryPid().equals(factoryPid)) {
                continue;
            }
            Object propertyObject = cloudServiceFactoryReference.getProperty(KURA_UI_CSF_PID_DEFAULT);
            ServiceLocator.getInstance().ungetService(cloudServiceFactoryReference);
            if (propertyObject != null) {
                return (String) propertyObject;
            }
        }
        return null;
    }

    @Override
    public String getCloudServicePidRegex(String factoryPid) throws GwtKuraException {
        Collection<ServiceReference<CloudServiceFactory>> cloudServiceFactoryReferences = ServiceLocator.getInstance()
                .getServiceReferences(CloudServiceFactory.class, null);

        for (ServiceReference<CloudServiceFactory> cloudServiceFactoryReference : cloudServiceFactoryReferences) {
            CloudServiceFactory cloudServiceFactory = ServiceLocator.getInstance()
                    .getService(cloudServiceFactoryReference);
            if (!cloudServiceFactory.getFactoryPid().equals(factoryPid)) {
                continue;
            }
            Object propertyObject = cloudServiceFactoryReference.getProperty(KURA_UI_CSF_PID_REGEX);
            ServiceLocator.getInstance().ungetService(cloudServiceFactoryReference);
            if (propertyObject != null) {
                return (String) propertyObject;
            }
        }
        return null;
    }

}
