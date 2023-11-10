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
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import org.eclipse.kura.KuraConnectException;
import org.eclipse.kura.KuraDisconnectException;
import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.KuraRuntimeException;
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

    public void connectCloudEndpoint(String connectionId) throws KuraException {

        try {
            runOnDataService(connectionId, dataService -> {
                int counter = 10;
                try {
                    dataService.connect();
                    while (!dataService.isConnected() && counter > 0) {
                        Thread.sleep(1000);
                        counter--;
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });

            runOnCloudConnectionManager(connectionId, cloudConnectionManager -> {
                try {
                    cloudConnectionManager.connect();
                } catch (KuraConnectException e) {
                    throw new KuraRuntimeException(KuraErrorCode.CONNECTION_FAILED, e,
                            "Error connecting. Please review your configuration.");
                }
            });
        } catch (RuntimeException e) {
            Throwable cause = e.getCause();
            if (cause instanceof KuraConnectException) {
                logger.warn("Error connecting", e);
                throw new KuraException(KuraErrorCode.CONNECTION_FAILED, e,
                        "Error connecting. Please review your configuration.");
            } else if (cause instanceof InterruptedException) {
                logger.warn("Interrupt Exception");
                Thread.currentThread().interrupt();
            } else if (cause instanceof IllegalStateException) {
                logger.warn("Illegal client state", e);
                throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e, "Illegal client state");
            }
        }

    }

    public void disconnectCloudEndpoint(String connectionId) throws KuraException {

        runOnDataService(connectionId, dataService -> dataService.disconnect(10));

        try {
            runOnCloudConnectionManager(connectionId, cloudConnectionManager -> {
                try {
                    cloudConnectionManager.disconnect();
                } catch (KuraDisconnectException e) {
                    throw new KuraRuntimeException(KuraErrorCode.CONNECTION_FAILED);
                }
            });
        } catch (KuraRuntimeException e) {
            logger.warn("Error disconnecting");
            throw new KuraException(KuraErrorCode.CONNECTION_FAILED, e,
                    "Error disconnecting. Please review your configuration.");
        }

    }

    public boolean isConnectedCloudEndpoint(String connectionId) throws KuraException {

        AtomicReference<Boolean> connectionStatusHolder = new AtomicReference<>(false);

        runOnDataService(connectionId, dataService -> connectionStatusHolder.set(dataService.isConnected()));

        runOnCloudConnectionManager(connectionId,
                cloudConnectionManager -> connectionStatusHolder.set(cloudConnectionManager.isConnected()));

        return connectionStatusHolder.get();

    }

    private void runOnDataService(String connectionId, Consumer<DataService> dataServiceConsumer) throws KuraException {
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
                        dataServiceConsumer.accept(dataService);
                    }
                    ServiceUtil.ungetService(this.bundleContext, dataServiceReference);
                }
            }
            ServiceUtil.ungetService(this.bundleContext, cloudServiceReference);
        }
    }

    private void runOnCloudConnectionManager(String connectionId,
            Consumer<CloudConnectionManager> cloudConnectionManagerConsumer) throws KuraException {
        Collection<ServiceReference<CloudConnectionManager>> cloudConnectionManagerReferences = ServiceUtil
                .getServiceReferencesAsCollection(this.bundleContext, CloudConnectionManager.class, null);

        for (ServiceReference<CloudConnectionManager> cloudConnectionManagerReference : cloudConnectionManagerReferences) {
            String cloudConnectionManagerPid = (String) cloudConnectionManagerReference.getProperty(KURA_SERVICE_PID);
            if (cloudConnectionManagerPid.endsWith(connectionId)) {
                CloudConnectionManager cloudConnectionManager = ServiceUtil.getService(this.bundleContext,
                        cloudConnectionManagerReference);

                cloudConnectionManagerConsumer.accept(cloudConnectionManager);
            }
            ServiceUtil.ungetService(this.bundleContext, cloudConnectionManagerReference);
        }
    }

}
