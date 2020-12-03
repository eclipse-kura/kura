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
 *  Red Hat Inc
 *******************************************************************************/
package org.eclipse.kura.web.server;

import static java.lang.String.format;
import static java.util.Collections.sort;
import static java.util.Comparator.comparing;
import static java.util.Comparator.naturalOrder;
import static java.util.Comparator.nullsFirst;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.eclipse.kura.KuraConnectException;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.cloud.CloudService;
import org.eclipse.kura.cloudconnection.CloudConnectionManager;
import org.eclipse.kura.cloudconnection.CloudEndpoint;
import org.eclipse.kura.configuration.ConfigurationService;
import org.eclipse.kura.data.DataService;
import org.eclipse.kura.data.DataTransportService;
import org.eclipse.kura.position.PositionService;
import org.eclipse.kura.web.server.util.ServiceLocator;
import org.eclipse.kura.web.session.Attributes;
import org.eclipse.kura.web.shared.GwtKuraErrorCode;
import org.eclipse.kura.web.shared.GwtKuraException;
import org.eclipse.kura.web.shared.model.GwtCloudConnectionInfo;
import org.eclipse.kura.web.shared.model.GwtGroupedNVPair;
import org.eclipse.kura.web.shared.model.GwtModemInterfaceConfig;
import org.eclipse.kura.web.shared.model.GwtNetIfConfigMode;
import org.eclipse.kura.web.shared.model.GwtNetIfStatus;
import org.eclipse.kura.web.shared.model.GwtNetIfType;
import org.eclipse.kura.web.shared.model.GwtNetInterfaceConfig;
import org.eclipse.kura.web.shared.model.GwtNetRouterMode;
import org.eclipse.kura.web.shared.model.GwtWifiConfig;
import org.eclipse.kura.web.shared.model.GwtWifiNetInterfaceConfig;
import org.eclipse.kura.web.shared.model.GwtWifiWirelessMode;
import org.eclipse.kura.web.shared.model.GwtXSRFToken;
import org.eclipse.kura.web.shared.service.GwtStatusService;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GwtStatusServiceImpl extends OsgiRemoteServiceServlet implements GwtStatusService {

    private static final String IP_ACQUISITION = "IP Acquisition: ";

    private static final String MODE = "Mode: ";

    private static final String SUBNET_MASK = "Subnet Mask: ";

    private static final String POSITION_STATUS = "positionStatus";

    private static final Logger logger = LoggerFactory.getLogger(GwtStatusServiceImpl.class);
    private static final Logger auditLogger = LoggerFactory.getLogger("AuditLogger");

    private static final long serialVersionUID = 8256280782910423734L;

    private static final String KURA_SERVICE_PID = ConfigurationService.KURA_SERVICE_PID;
    private static final String DATA_SERVICE_REFERENCE_NAME = "DataService";
    private static final String DATA_TRANSPORT_SERVICE_REFERENCE_NAME = "DataTransportService";

    @Override
    public ArrayList<GwtGroupedNVPair> getDeviceConfig(GwtXSRFToken xsrfToken, boolean hasNetAdmin)
            throws GwtKuraException {
        checkXSRFToken(xsrfToken);
        List<GwtGroupedNVPair> pairs = new ArrayList<>();

        pairs.addAll(getCloudStatus());
        if (hasNetAdmin) {
            pairs.addAll(getNetworkStatus());
        }
        pairs.addAll(getPositionStatus());

        return new ArrayList<>(pairs);
    }

    @Override
    public void connectDataService(GwtXSRFToken xsrfToken, String connectionId) throws GwtKuraException {
        checkXSRFToken(xsrfToken);

        final HttpServletRequest request = getThreadLocalRequest();
        final HttpSession session = request.getSession(false);

        Collection<ServiceReference<CloudService>> cloudServiceReferences = ServiceLocator.getInstance()
                .getServiceReferences(CloudService.class, null);

        for (ServiceReference<CloudService> cloudServiceReference : cloudServiceReferences) {
            String cloudServicePid = (String) cloudServiceReference.getProperty(KURA_SERVICE_PID);
            if (cloudServicePid.endsWith(connectionId)) {
                String dataServiceRef = (String) cloudServiceReference
                        .getProperty(DATA_SERVICE_REFERENCE_NAME + ComponentConstants.REFERENCE_TARGET_SUFFIX);
                Collection<ServiceReference<DataService>> dataServiceReferences = ServiceLocator.getInstance()
                        .getServiceReferences(DataService.class, dataServiceRef);

                for (ServiceReference<DataService> dataServiceReference : dataServiceReferences) {
                    DataService dataService = ServiceLocator.getInstance().getService(dataServiceReference);
                    if (dataService != null) {
                        GwtKuraException gwtKuraException = null;
                        int counter = 10;
                        try {
                            dataService.connect();
                            while (!dataService.isConnected() && counter > 0) {
                                Thread.sleep(1000);
                                counter--;
                            }
                        } catch (KuraConnectException e) {
                            logger.warn("Error connecting");
                            gwtKuraException = new GwtKuraException(GwtKuraErrorCode.CONNECTION_FAILURE, e,
                                    "Error connecting. Please review your configuration.");
                        } catch (InterruptedException e) {
                            logger.warn("Interrupt Exception");
                            gwtKuraException = new GwtKuraException(GwtKuraErrorCode.INTERNAL_ERROR, e,
                                    "Interrupt Exception");
                        } catch (IllegalStateException e) {
                            logger.warn("Illegal client state");
                            gwtKuraException = new GwtKuraException(GwtKuraErrorCode.INTERNAL_ERROR, e,
                                    "Illegal client state");
                        }

                        if (gwtKuraException != null) {
                            auditLogger.warn(
                                    "UI Status - Failure - Failed to connect data service for user: {}, session: {}, connection id: {}",
                                    session.getAttribute(Attributes.AUTORIZED_USER.getValue()), session.getId(),
                                    connectionId);
                            throw gwtKuraException;
                        }
                    }
                    ServiceLocator.getInstance().ungetService(dataServiceReference);
                }
            }
            ServiceLocator.getInstance().ungetService(cloudServiceReference);
        }

        Collection<ServiceReference<CloudConnectionManager>> cloudConnectionManagerReferences = ServiceLocator
                .getInstance().getServiceReferences(CloudConnectionManager.class, null);

        for (ServiceReference<CloudConnectionManager> cloudConnectionManagerReference : cloudConnectionManagerReferences) {
            String cloudConnectionManagerPid = (String) cloudConnectionManagerReference.getProperty(KURA_SERVICE_PID);
            if (cloudConnectionManagerPid.endsWith(connectionId)) {
                CloudConnectionManager cloudConnectionManager = ServiceLocator.getInstance()
                        .getService(cloudConnectionManagerReference);
                try {
                    cloudConnectionManager.connect();
                } catch (KuraException e) {
                    logger.warn("Error connecting");
                    auditLogger.warn(
                            "UI Status - Failure - Failed to connect data service for user: {}, session: {}, connection id: {}",
                            session.getAttribute(Attributes.AUTORIZED_USER.getValue()), session.getId(), connectionId);
                    throw new GwtKuraException(GwtKuraErrorCode.CONNECTION_FAILURE, e,
                            "Error connecting. Please review your configuration.");
                }
            }
            ServiceLocator.getInstance().ungetService(cloudConnectionManagerReference);
        }

        auditLogger.info(
                "UI Status - Success - Successfully connected data service for user: {}, session: {}, connection id: {}",
                session.getAttribute(Attributes.AUTORIZED_USER.getValue()), session.getId(), connectionId);
    }

    @Override
    public void disconnectDataService(GwtXSRFToken xsrfToken, String connectionId) throws GwtKuraException {
        checkXSRFToken(xsrfToken);

        final HttpServletRequest request = getThreadLocalRequest();
        final HttpSession session = request.getSession(false);

        Collection<ServiceReference<CloudService>> cloudServiceReferences = ServiceLocator.getInstance()
                .getServiceReferences(CloudService.class, null);

        for (ServiceReference<CloudService> cloudServiceReference : cloudServiceReferences) {
            String cloudServicePid = (String) cloudServiceReference.getProperty(KURA_SERVICE_PID);
            if (cloudServicePid.endsWith(connectionId)) {
                String dataServiceRef = (String) cloudServiceReference
                        .getProperty(DATA_SERVICE_REFERENCE_NAME + ComponentConstants.REFERENCE_TARGET_SUFFIX);
                Collection<ServiceReference<DataService>> dataServiceReferences = ServiceLocator.getInstance()
                        .getServiceReferences(DataService.class, dataServiceRef);

                for (ServiceReference<DataService> dataServiceReference : dataServiceReferences) {
                    DataService dataService = ServiceLocator.getInstance().getService(dataServiceReference);
                    if (dataService != null) {
                        dataService.disconnect(10);
                    }
                    ServiceLocator.getInstance().ungetService(dataServiceReference);
                }
            }
            ServiceLocator.getInstance().ungetService(cloudServiceReference);
        }

        Collection<ServiceReference<CloudConnectionManager>> cloudConnectionManagerReferences = ServiceLocator
                .getInstance().getServiceReferences(CloudConnectionManager.class, null);

        for (ServiceReference<CloudConnectionManager> cloudConnectionManagerReference : cloudConnectionManagerReferences) {
            String cloudConnectionManagerPid = (String) cloudConnectionManagerReference.getProperty(KURA_SERVICE_PID);
            if (cloudConnectionManagerPid.endsWith(connectionId)) {
                CloudConnectionManager cloudConnectionManager = ServiceLocator.getInstance()
                        .getService(cloudConnectionManagerReference);
                try {
                    cloudConnectionManager.disconnect();
                } catch (KuraException e) {
                    logger.warn("Error disconnecting");
                    auditLogger.warn(
                            "UI Status - Failure - Failed to disconnect data service for user: {}, session: {}, connection id: {}",
                            session.getAttribute(Attributes.AUTORIZED_USER.getValue()), session.getId(), connectionId);
                    throw new GwtKuraException(GwtKuraErrorCode.CONNECTION_FAILURE, e,
                            "Error disconnecting. Please review your configuration.");
                }
            }
            ServiceLocator.getInstance().ungetService(cloudConnectionManagerReference);
        }

        auditLogger.info(
                "UI Status - Success - Successfully disconnected data service for user: {}, session: {}, connection id: {}",
                session.getAttribute(Attributes.AUTORIZED_USER.getValue()), session.getId(), connectionId);
    }

    @Override
    public boolean isConnected(GwtXSRFToken xsrfToken, String connectionId) throws GwtKuraException {
        checkXSRFToken(xsrfToken);
        boolean isConnected = false;

        Collection<ServiceReference<CloudService>> cloudServiceReferences = ServiceLocator.getInstance()
                .getServiceReferences(CloudService.class, null);

        for (ServiceReference<CloudService> cloudServiceReference : cloudServiceReferences) {
            String cloudServicePid = (String) cloudServiceReference.getProperty(KURA_SERVICE_PID);
            if (cloudServicePid.endsWith(connectionId)) {
                String dataServiceRef = (String) cloudServiceReference
                        .getProperty(DATA_SERVICE_REFERENCE_NAME + ComponentConstants.REFERENCE_TARGET_SUFFIX);
                Collection<ServiceReference<DataService>> dataServiceReferences = ServiceLocator.getInstance()
                        .getServiceReferences(DataService.class, dataServiceRef);

                for (ServiceReference<DataService> dataServiceReference : dataServiceReferences) {
                    DataService dataService = ServiceLocator.getInstance().getService(dataServiceReference);
                    if (dataService != null) {
                        isConnected = dataService.isConnected();
                    }
                    ServiceLocator.getInstance().ungetService(dataServiceReference);
                }
            }
            ServiceLocator.getInstance().ungetService(cloudServiceReference);
        }

        Collection<ServiceReference<CloudConnectionManager>> cloudConnectionManagerReferences = ServiceLocator
                .getInstance().getServiceReferences(CloudConnectionManager.class, null);

        for (ServiceReference<CloudConnectionManager> cloudConnectionManagerReference : cloudConnectionManagerReferences) {
            String cloudConnectionManagerPid = (String) cloudConnectionManagerReference.getProperty(KURA_SERVICE_PID);
            if (cloudConnectionManagerPid.endsWith(connectionId)) {
                CloudConnectionManager cloudConnectionManager = ServiceLocator.getInstance()
                        .getService(cloudConnectionManagerReference);

                isConnected = cloudConnectionManager.isConnected();
            }
            ServiceLocator.getInstance().ungetService(cloudConnectionManagerReference);
        }

        return isConnected;
    }

    private List<GwtGroupedNVPair> getCloudStatus() throws GwtKuraException {
        final List<GwtGroupedNVPair> pairs = new ArrayList<>();

        final List<GwtCloudConnectionInfo> connectionInfos = new ArrayList<>();

        try {
            final Collection<ServiceReference<CloudEndpoint>> cloudEndpointReferences = ServiceLocator.getInstance()
                    .getServiceReferences(CloudEndpoint.class, null);

            List<ServiceReference<CloudEndpoint>> cloudEndpointReferencesList = new ArrayList<>(
                    cloudEndpointReferences);

            for (ServiceReference<CloudEndpoint> cloudEndpointReference : cloudEndpointReferencesList) {
                GwtCloudConnectionInfo cloudConnectionInfo = new GwtCloudConnectionInfo();

                String cloudServicePid = (String) cloudEndpointReference.getProperty(KURA_SERVICE_PID);
                cloudConnectionInfo.setCloudServicePid(cloudServicePid);

                final String filter = format("(%s=%s)", KURA_SERVICE_PID, cloudServicePid);
                final Collection<ServiceReference<CloudConnectionManager>> cloudConnectionManagerReferences = ServiceLocator
                        .getInstance().getServiceReferences(CloudConnectionManager.class, filter);

                cloudConnectionManagerReferences.forEach(cloudConnectionManagerReference -> {
                    CloudConnectionManager cloudConnectionManager;
                    try {
                        cloudConnectionManager = ServiceLocator.getInstance()
                                .getService(cloudConnectionManagerReference);
                        cloudConnectionInfo.addConnectionProperty("Service Status",
                                cloudConnectionManager.isConnected() ? "CONNECTED" : "DISCONNECTED");
                    } catch (GwtKuraException e) {
                        // endpoint not connection oriented
                    }

                });

                CloudEndpoint cloudEndpoint = ServiceLocator.getInstance().getService(cloudEndpointReference);

                Map<String, String> connectionProps = cloudEndpoint.getInfo();
                connectionProps.forEach(cloudConnectionInfo::addConnectionProperty);

                connectionInfos.add(cloudConnectionInfo);
            }
        } catch (GwtKuraException e) {
            logger.warn("Get cloud status failed", e);
        }

        try {
            final Collection<ServiceReference<CloudService>> cloudServiceReferences = ServiceLocator.getInstance()
                    .getServiceReferences(CloudService.class, null);

            List<ServiceReference<CloudService>> cloudServiceReferencesList = new ArrayList<>(cloudServiceReferences);

            for (ServiceReference<CloudService> cloudServiceReference : cloudServiceReferencesList) {
                String cloudServicePid = (String) cloudServiceReference.getProperty(KURA_SERVICE_PID);

                if (connectionInfos.stream()
                        .noneMatch(connectionInfo -> connectionInfo.getCloudServicePid().equals(cloudServicePid))) {
                    GwtCloudConnectionInfo cloudConnectionInfo = fillFromCloudService(cloudServiceReference,
                            cloudServicePid);
                    connectionInfos.add(cloudConnectionInfo);
                }
            }
        } catch (GwtKuraException e) {
            logger.warn("Get cloud status failed", e);
        }

        connectionInfos.sort((c1, c2) -> c1.getCloudServicePid().compareTo(c2.getCloudServicePid()));

        fillCloudConnectionInfo(pairs, connectionInfos);
        return pairs;
    }

    private void fillCloudConnectionInfo(List<GwtGroupedNVPair> pairs, List<GwtCloudConnectionInfo> connectionInfos) {
        connectionInfos.forEach(connectionInfo -> {
            pairs.add(new GwtGroupedNVPair("cloudStatus", "Connection Name", connectionInfo.getCloudServicePid()));

            Map<String, Object> connectionProperties = connectionInfo.getConnectionProperties();
            connectionProperties
                    .forEach((key, value) -> pairs.add(new GwtGroupedNVPair("cloudStatus", key, (String) value)));
        });
    }

    private GwtCloudConnectionInfo fillFromCloudService(ServiceReference<CloudService> cloudServiceReference,
            String cloudServicePid) throws GwtKuraException {

        GwtCloudConnectionInfo result = new GwtCloudConnectionInfo();
        result.setCloudServicePid(cloudServicePid);

        final CloudService cloudService = ServiceLocator.getInstance().getService(cloudServiceReference);
        try {
            result.addConnectionProperty("Service Status", cloudService.isConnected() ? "CONNECTED" : "DISCONNECTED");

            final String dataServiceRef = (String) cloudServiceReference
                    .getProperty(DATA_SERVICE_REFERENCE_NAME + ComponentConstants.REFERENCE_TARGET_SUFFIX);
            if (dataServiceRef != null) {
                fillFromDataService(result, dataServiceRef);
            }
        } finally {
            ServiceLocator.getInstance().ungetService(cloudServiceReference);
        }
        return result;
    }

    private void fillFromDataService(GwtCloudConnectionInfo cloudConnectionInfo, final String dataServiceRef)
            throws GwtKuraException {
        final Collection<ServiceReference<DataService>> dataServiceReferences = ServiceLocator.getInstance()
                .getServiceReferences(DataService.class, dataServiceRef);

        for (ServiceReference<DataService> dataServiceReference : dataServiceReferences) {
            final DataService dataService = ServiceLocator.getInstance().getService(dataServiceReference);
            try {
                if (dataService != null) {
                    cloudConnectionInfo.addConnectionProperty("Auto-connect",
                            dataService.isAutoConnectEnabled()
                                    ? "ON (Retry Interval is " + Integer.toString(dataService.getRetryInterval()) + "s)"
                                    : "OFF");
                }

                final String dataTransportRef = (String) dataServiceReference.getProperty(
                        DATA_TRANSPORT_SERVICE_REFERENCE_NAME + ComponentConstants.REFERENCE_TARGET_SUFFIX);
                final List<DataTransportService> dataTransportServices = ServiceLocator.getInstance()
                        .getServices(DataTransportService.class, dataTransportRef);
                for (DataTransportService dataTransportService : dataTransportServices) {
                    cloudConnectionInfo.addConnectionProperty("Broker URL", dataTransportService.getBrokerUrl());
                    cloudConnectionInfo.addConnectionProperty("Account", dataTransportService.getAccountName());
                    cloudConnectionInfo.addConnectionProperty("Username", dataTransportService.getUsername());
                    cloudConnectionInfo.addConnectionProperty("Client ID", dataTransportService.getClientId());
                }
            } finally {
                ServiceLocator.getInstance().ungetService(dataServiceReference);
            }
        }
    }

    private List<GwtGroupedNVPair> getNetworkStatus() throws GwtKuraException {
        List<GwtGroupedNVPair> pairs = new ArrayList<>();
        String nl = "<br />";
        String tab = "&nbsp&nbsp&nbsp&nbsp";

        GwtNetworkServiceImpl gwtNetworkService = new GwtNetworkServiceImpl();

        try {
            List<GwtNetInterfaceConfig> gwtNetInterfaceConfigs = gwtNetworkService.findNetInterfaceConfigurations();
            if (gwtNetInterfaceConfigs == null) {
                return Collections.emptyList();
            }

            sort(gwtNetInterfaceConfigs, comparing(GwtNetInterfaceConfig::getName, nullsFirst(naturalOrder())));

            for (GwtNetInterfaceConfig gwtNetInterfaceConfig : gwtNetInterfaceConfigs) {

                String currentAddress = gwtNetInterfaceConfig.getIpAddress();
                String currentSubnetMask = gwtNetInterfaceConfig.getSubnetMask();

                String currentConfigMode;
                if (gwtNetInterfaceConfig.getConfigModeEnum() == GwtNetIfConfigMode.netIPv4ConfigModeDHCP) {
                    currentConfigMode = "DHCP";
                } else {
                    currentConfigMode = "Manual";
                }

                String currentRouterMode;
                if (gwtNetInterfaceConfig.getRouterModeEnum() == GwtNetRouterMode.netRouterDchp) {
                    currentRouterMode = "DHCPD";
                } else if (gwtNetInterfaceConfig.getRouterModeEnum() == GwtNetRouterMode.netRouterNat) {
                    currentRouterMode = "NAT";
                } else if (gwtNetInterfaceConfig.getRouterModeEnum() == GwtNetRouterMode.netRouterDchpNat) {
                    currentRouterMode = "DHCPD & NAT";
                } else {
                    currentRouterMode = "";
                }

                if (gwtNetInterfaceConfig.getHwTypeEnum() == GwtNetIfType.ETHERNET) {
                    if (gwtNetInterfaceConfig.getStatusEnum() == GwtNetIfStatus.netIPv4StatusDisabled
                            || gwtNetInterfaceConfig.getStatusEnum() == GwtNetIfStatus.netIPv4StatusUnmanaged
                            || gwtNetInterfaceConfig.getStatusEnum() == GwtNetIfStatus.netIPv4StatusL2Only) {
                        pairs.add(new GwtGroupedNVPair("networkStatusEthernet", gwtNetInterfaceConfig.getName(),
                                gwtNetInterfaceConfig.getStatusEnum().getValue()));
                    } else {
                        pairs.add(new GwtGroupedNVPair("networkStatusEthernet", gwtNetInterfaceConfig.getName(),
                                currentAddress + nl + tab + SUBNET_MASK + currentSubnetMask + nl + tab + MODE
                                        + gwtNetInterfaceConfig.getStatusEnum().getValue() + nl + tab + IP_ACQUISITION
                                        + currentConfigMode + nl + tab + "Router Mode: " + currentRouterMode));
                    }
                } else if (gwtNetInterfaceConfig.getHwTypeEnum() == GwtNetIfType.WIFI
                        && !gwtNetInterfaceConfig.getName().startsWith("mon")) {
                    String currentWifiMode = ((GwtWifiNetInterfaceConfig) gwtNetInterfaceConfig)
                            .getWirelessModeEnum() == GwtWifiWirelessMode.netWifiWirelessModeStation ? "Station Mode"
                                    : "Access Point";
                    GwtWifiConfig gwtActiveWifiConfig = ((GwtWifiNetInterfaceConfig) gwtNetInterfaceConfig)
                            .getActiveWifiConfig();
                    String currentWifiSsid = null;
                    if (gwtActiveWifiConfig != null) {
                        currentWifiSsid = gwtActiveWifiConfig.getWirelessSsid();
                    }
                    if (gwtNetInterfaceConfig.getStatusEnum() == GwtNetIfStatus.netIPv4StatusDisabled
                            || gwtNetInterfaceConfig.getStatusEnum() == GwtNetIfStatus.netIPv4StatusUnmanaged
                            || gwtNetInterfaceConfig.getStatusEnum() == GwtNetIfStatus.netIPv4StatusL2Only) {
                        pairs.add(new GwtGroupedNVPair("networkStatusWifi", gwtNetInterfaceConfig.getName(),
                                gwtNetInterfaceConfig.getStatusEnum().getValue()));
                    } else {
                        pairs.add(new GwtGroupedNVPair("networkStatusWifi", gwtNetInterfaceConfig.getName(),
                                currentAddress + nl + tab + SUBNET_MASK + currentSubnetMask + nl + tab + MODE
                                        + gwtNetInterfaceConfig.getStatusEnum().getValue() + nl + tab + IP_ACQUISITION
                                        + currentConfigMode + nl + tab + "Router Mode: " + currentRouterMode + nl + tab
                                        + "Wireless Mode:" + currentWifiMode + nl + tab + "SSID: " + currentWifiSsid
                                        + nl));
                    }
                } else if (gwtNetInterfaceConfig.getHwTypeEnum() == GwtNetIfType.MODEM) {
                    String currentModemApn = ((GwtModemInterfaceConfig) gwtNetInterfaceConfig).getApn();
                    String currentModemPppNum = Integer
                            .toString(((GwtModemInterfaceConfig) gwtNetInterfaceConfig).getPppNum());
                    if (gwtNetInterfaceConfig.getStatusEnum() == GwtNetIfStatus.netIPv4StatusDisabled
                            || gwtNetInterfaceConfig.getStatusEnum() == GwtNetIfStatus.netIPv4StatusUnmanaged
                            || gwtNetInterfaceConfig.getStatusEnum() == GwtNetIfStatus.netIPv4StatusL2Only) {
                        pairs.add(new GwtGroupedNVPair("networkStatusModem", gwtNetInterfaceConfig.getName(),
                                gwtNetInterfaceConfig.getStatusEnum().getValue()));
                    } else {
                        pairs.add(new GwtGroupedNVPair("networkStatusModem", gwtNetInterfaceConfig.getName(),
                                currentAddress + nl + SUBNET_MASK + currentSubnetMask + nl + tab + MODE
                                        + gwtNetInterfaceConfig.getStatusEnum().getValue() + nl + tab + IP_ACQUISITION
                                        + currentConfigMode + nl + tab + "APN: " + currentModemApn + nl + tab + "PPP: "
                                        + currentModemPppNum));
                    }
                }
            }
        } catch (GwtKuraException e) {
            logger.warn("Get network status failed");
            throw e;
        }

        return pairs;
    }

    private List<GwtGroupedNVPair> getPositionStatus() throws GwtKuraException {
        final List<GwtGroupedNVPair> pairs = new ArrayList<>();

        ServiceLocator.applyToServiceOptionally(PositionService.class, positionService -> {
            if (positionService != null) {
                pairs.add(new GwtGroupedNVPair(POSITION_STATUS, "Longitude",
                        Double.toString(Math.toDegrees(positionService.getPosition().getLongitude().getValue()))));
                pairs.add(new GwtGroupedNVPair(POSITION_STATUS, "Latitude",
                        Double.toString(Math.toDegrees(positionService.getPosition().getLatitude().getValue()))));
                pairs.add(new GwtGroupedNVPair(POSITION_STATUS, "Altitude",
                        positionService.getPosition().getAltitude().toString()));
            }
            return null;
        });

        return pairs;
    }
}