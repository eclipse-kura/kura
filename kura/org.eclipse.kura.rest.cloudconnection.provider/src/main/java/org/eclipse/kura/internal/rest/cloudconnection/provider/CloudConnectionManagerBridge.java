/*******************************************************************************
 * Copyright (c) 2023 Eurotech and/or its affiliates and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *  Eurotech
 *******************************************************************************/
package org.eclipse.kura.internal.rest.cloudconnection.provider;

import static org.eclipse.kura.configuration.ConfigurationService.KURA_SERVICE_PID;

import java.util.Collection;

import org.eclipse.kura.KuraConnectException;
import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.cloud.CloudService;
import org.eclipse.kura.cloudconnection.CloudConnectionManager;
import org.eclipse.kura.data.DataService;
import org.eclipse.kura.util.service.ServiceUtil;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("deprecation")
public class CloudConnectionManagerBridge {

    private static final Logger logger = LoggerFactory.getLogger(CloudConnectionManagerBridge.class);

    private static final String DATA_SERVICE_REFERENCE_NAME = "DataService";

    private final BundleContext bundleContext = FrameworkUtil.getBundle(this.getClass()).getBundleContext();

    public void connectDataService(String connectionId) throws KuraException {

        Collection<ServiceReference<CloudService>> cloudServiceReferences = ServiceUtil
                .getServiceReferencesAsCollection(this.bundleContext, CloudService.class, null);

        for (ServiceReference<CloudService> cloudServiceReference : cloudServiceReferences) {
            String cloudServicePid = (String) cloudServiceReference.getProperty(KURA_SERVICE_PID);
            if (cloudServicePid.endsWith(connectionId)) {
                String dataServiceRef = (String) cloudServiceReference
                        .getProperty(DATA_SERVICE_REFERENCE_NAME + ComponentConstants.REFERENCE_TARGET_SUFFIX);
                Collection<ServiceReference<DataService>> dataServiceReferences = ServiceUtil
                        .getServiceReferencesAsCollection(this.bundleContext, DataService.class, dataServiceRef);

                for (ServiceReference<DataService> dataServiceReference : dataServiceReferences) {
                    DataService dataService = ServiceUtil.getService(this.bundleContext, dataServiceReference);
                    if (dataService != null) {
                        KuraException kuraException = null;
                        int counter = 10;
                        try {
                            dataService.connect();
                            while (!dataService.isConnected() && counter > 0) {
                                Thread.sleep(1000);
                                counter--;
                            }
                        } catch (KuraConnectException e) {
                            logger.warn("Error connecting", e);
                            kuraException = new KuraException(KuraErrorCode.CONNECTION_FAILED, e,
                                    "Error connecting. Please review your configuration.");
                        } catch (InterruptedException e) {
                            logger.warn("Interrupt Exception");
                            Thread.currentThread().interrupt();
                        } catch (IllegalStateException e) {
                            logger.warn("Illegal client state", e);
                            kuraException = new KuraException(KuraErrorCode.INTERNAL_ERROR, e, "Illegal client state");
                        }

                        if (kuraException != null) {
                            throw kuraException;
                        }
                    }
                    ServiceUtil.ungetService(this.bundleContext, dataServiceReference);
                }
            }
            ServiceUtil.ungetService(this.bundleContext, cloudServiceReference);
        }

        Collection<ServiceReference<CloudConnectionManager>> cloudConnectionManagerReferences = ServiceUtil
                .getServiceReferencesAsCollection(this.bundleContext, CloudConnectionManager.class, null);

        for (ServiceReference<CloudConnectionManager> cloudConnectionManagerReference : cloudConnectionManagerReferences) {
            String cloudConnectionManagerPid = (String) cloudConnectionManagerReference.getProperty(KURA_SERVICE_PID);
            if (cloudConnectionManagerPid.endsWith(connectionId)) {
                CloudConnectionManager cloudConnectionManager = ServiceUtil.getService(this.bundleContext,
                        cloudConnectionManagerReference);
                try {
                    cloudConnectionManager.connect();
                } catch (KuraException e) {
                    logger.warn("Error connecting");
                    throw new KuraException(KuraErrorCode.CONNECTION_FAILED, e,
                            "Error connecting. Please review your configuration.");
                }
            }
            ServiceUtil.ungetService(this.bundleContext, cloudConnectionManagerReference);
        }
    }

    public void disconnectDataService(String connectionId) throws KuraException {

        Collection<ServiceReference<CloudService>> cloudServiceReferences = ServiceUtil
                .getServiceReferencesAsCollection(this.bundleContext, CloudService.class, null);

        for (ServiceReference<CloudService> cloudServiceReference : cloudServiceReferences) {
            String cloudServicePid = (String) cloudServiceReference.getProperty(KURA_SERVICE_PID);
            if (cloudServicePid.endsWith(connectionId)) {
                String dataServiceRef = (String) cloudServiceReference
                        .getProperty(DATA_SERVICE_REFERENCE_NAME + ComponentConstants.REFERENCE_TARGET_SUFFIX);
                Collection<ServiceReference<DataService>> dataServiceReferences = ServiceUtil
                        .getServiceReferencesAsCollection(this.bundleContext, DataService.class, dataServiceRef);

                for (ServiceReference<DataService> dataServiceReference : dataServiceReferences) {
                    DataService dataService = ServiceUtil.getService(this.bundleContext, dataServiceReference);
                    if (dataService != null) {
                        dataService.disconnect(10);
                    }
                    ServiceUtil.ungetService(this.bundleContext, dataServiceReference);
                }
            }
            ServiceUtil.ungetService(this.bundleContext, cloudServiceReference);
        }

        Collection<ServiceReference<CloudConnectionManager>> cloudConnectionManagerReferences = ServiceUtil
                .getServiceReferencesAsCollection(this.bundleContext, CloudConnectionManager.class, null);

        for (ServiceReference<CloudConnectionManager> cloudConnectionManagerReference : cloudConnectionManagerReferences) {
            String cloudConnectionManagerPid = (String) cloudConnectionManagerReference.getProperty(KURA_SERVICE_PID);
            if (cloudConnectionManagerPid.endsWith(connectionId)) {
                CloudConnectionManager cloudConnectionManager = ServiceUtil.getService(this.bundleContext,
                        cloudConnectionManagerReference);
                try {
                    cloudConnectionManager.disconnect();
                } catch (KuraException e) {
                    logger.warn("Error disconnecting");
                    throw new KuraException(KuraErrorCode.CONNECTION_FAILED, e,
                            "Error disconnecting. Please review your configuration.");
                }
            }
            ServiceUtil.ungetService(this.bundleContext, cloudConnectionManagerReference);
        }
    }

    public boolean isConnected(String connectionId) throws KuraException {

        boolean isConnected = false;

        Collection<ServiceReference<CloudService>> cloudServiceReferences = ServiceUtil
                .getServiceReferencesAsCollection(this.bundleContext, CloudService.class, null);

        for (ServiceReference<CloudService> cloudServiceReference : cloudServiceReferences) {
            String cloudServicePid = (String) cloudServiceReference.getProperty(KURA_SERVICE_PID);
            if (cloudServicePid.endsWith(connectionId)) {
                String dataServiceRef = (String) cloudServiceReference
                        .getProperty(DATA_SERVICE_REFERENCE_NAME + ComponentConstants.REFERENCE_TARGET_SUFFIX);
                Collection<ServiceReference<DataService>> dataServiceReferences = ServiceUtil
                        .getServiceReferencesAsCollection(this.bundleContext, DataService.class, dataServiceRef);

                for (ServiceReference<DataService> dataServiceReference : dataServiceReferences) {
                    DataService dataService = ServiceUtil.getService(this.bundleContext, dataServiceReference);
                    if (dataService != null) {
                        isConnected = dataService.isConnected();
                    }
                    ServiceUtil.ungetService(this.bundleContext, dataServiceReference);
                }
            }
            ServiceUtil.ungetService(this.bundleContext, cloudServiceReference);
        }

        Collection<ServiceReference<CloudConnectionManager>> cloudConnectionManagerReferences = ServiceUtil
                .getServiceReferencesAsCollection(this.bundleContext, CloudConnectionManager.class, null);

        for (ServiceReference<CloudConnectionManager> cloudConnectionManagerReference : cloudConnectionManagerReferences) {
            String cloudConnectionManagerPid = (String) cloudConnectionManagerReference.getProperty(KURA_SERVICE_PID);
            if (cloudConnectionManagerPid.endsWith(connectionId)) {
                CloudConnectionManager cloudConnectionManager = ServiceUtil.getService(this.bundleContext,
                        cloudConnectionManagerReference);

                isConnected = cloudConnectionManager.isConnected();
            }
            ServiceUtil.ungetService(this.bundleContext, cloudConnectionManagerReference);
        }

        return isConnected;
    }

}
