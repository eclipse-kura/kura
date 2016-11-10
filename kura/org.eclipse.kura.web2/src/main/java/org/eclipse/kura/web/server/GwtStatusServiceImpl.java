/*******************************************************************************
 * Copyright (c) 2011, 2016 Eurotech and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *     Red Hat Inc - Fix logging calls
 *          - Fix possible NPE, Fix generics, Fix issue #599
 *******************************************************************************/
package org.eclipse.kura.web.server;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.eclipse.kura.KuraConnectException;
import org.eclipse.kura.cloud.CloudService;
import org.eclipse.kura.configuration.ConfigurationService;
import org.eclipse.kura.data.DataService;
import org.eclipse.kura.data.DataTransportService;
import org.eclipse.kura.position.PositionService;
import org.eclipse.kura.web.server.util.ServiceLocator;
import org.eclipse.kura.web.server.util.ServiceLocator.ServiceFunction;
import org.eclipse.kura.web.shared.GwtKuraErrorCode;
import org.eclipse.kura.web.shared.GwtKuraException;
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

    private static final Logger s_logger = LoggerFactory.getLogger(GwtNetworkServiceImpl.class);

    private static final long serialVersionUID = 8256280782910423734L;

    private static final String KURA_SERVICE_PID = ConfigurationService.KURA_SERVICE_PID;
    private static final String DATA_SERVICE_REFERENCE_NAME = "DataService";
    private static final String DATA_TRANSPORT_SERVICE_REFERENCE_NAME = "DataTransportService";

    @Override
    public ArrayList<GwtGroupedNVPair> getDeviceConfig(GwtXSRFToken xsrfToken, boolean hasNetAdmin)
            throws GwtKuraException {
        checkXSRFToken(xsrfToken);
        List<GwtGroupedNVPair> pairs = new ArrayList<GwtGroupedNVPair>();

        pairs.addAll(getCloudStatus());
        if (hasNetAdmin) {
            pairs.addAll(getNetworkStatus());
        }
        pairs.addAll(getPositionStatus());

        return new ArrayList<GwtGroupedNVPair>(pairs);
    }

    @Override
    public void connectDataService(GwtXSRFToken xsrfToken, String connectionId) throws GwtKuraException {
        checkXSRFToken(xsrfToken);

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
                        int counter = 10;
                        try {
                            dataService.connect();
                            while (!dataService.isConnected() && counter > 0) {
                                Thread.sleep(1000);
                                counter--;
                            }
                        } catch (KuraConnectException e) {
                            s_logger.warn("Error connecting", e);
                            throw new GwtKuraException(GwtKuraErrorCode.INTERNAL_ERROR, e, "Error connecting");
                        } catch (InterruptedException e) {
                            s_logger.warn("Interrupt Exception", e);
                            throw new GwtKuraException(GwtKuraErrorCode.INTERNAL_ERROR, e, "Interrupt Exception");
                        } catch (IllegalStateException e) {
                            s_logger.warn("Illegal client state", e);
                            throw new GwtKuraException(GwtKuraErrorCode.INTERNAL_ERROR, e, "Illegal client state");
                        }
                    }
                    ServiceLocator.getInstance().ungetService(dataServiceReference);
                }
            }
            ServiceLocator.getInstance().ungetService(cloudServiceReference);
        }
    }

    @Override
    public void disconnectDataService(GwtXSRFToken xsrfToken, String connectionId) throws GwtKuraException {
        checkXSRFToken(xsrfToken);

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
    }

    private List<GwtGroupedNVPair> getCloudStatus() throws GwtKuraException {
        final List<GwtGroupedNVPair> pairs = new ArrayList<GwtGroupedNVPair>();

        try {
            final Collection<ServiceReference<CloudService>> cloudServiceReferences = ServiceLocator.getInstance()
                    .getServiceReferences(CloudService.class, null);
            List<ServiceReference<CloudService>> cloudServiceReferencesList = new ArrayList<ServiceReference<CloudService>>(
                    cloudServiceReferences);
            sortCloudServiceServiceReferences(cloudServiceReferencesList);
            for (ServiceReference<CloudService> cloudServiceReference : cloudServiceReferencesList) {
                String cloudServicePid = (String) cloudServiceReference.getProperty(KURA_SERVICE_PID);
                if (cloudServicePid != null) {
                    fillFromCloudService(pairs, cloudServiceReference, cloudServicePid);
                }
            }
        } catch (GwtKuraException e) {
            s_logger.warn("Get cloud status failed", e);
        }

        return pairs;
    }

    private void fillFromCloudService(List<GwtGroupedNVPair> pairs,
            ServiceReference<CloudService> cloudServiceReference, String cloudServicePid) throws GwtKuraException {
        pairs.add(new GwtGroupedNVPair("cloudStatus", "Connection Name", cloudServicePid));

        final CloudService cloudService = ServiceLocator.getInstance().getService(cloudServiceReference);
        try {
            pairs.add(new GwtGroupedNVPair("cloudStatus", "Service Status",
                    cloudService.isConnected() ? "CONNECTED" : "DISCONNECTED"));

            final String dataServiceRef = (String) cloudServiceReference
                    .getProperty(DATA_SERVICE_REFERENCE_NAME + ComponentConstants.REFERENCE_TARGET_SUFFIX);
            if (dataServiceRef != null) {
                fillFromDataService(pairs, dataServiceRef);
            }
        } finally {
            ServiceLocator.getInstance().ungetService(cloudServiceReference);
        }
    }

    private void fillFromDataService(List<GwtGroupedNVPair> pairs, final String dataServiceRef)
            throws GwtKuraException {
        final Collection<ServiceReference<DataService>> dataServiceReferences = ServiceLocator.getInstance()
                .getServiceReferences(DataService.class, dataServiceRef);

        for (ServiceReference<DataService> dataServiceReference : dataServiceReferences) {
            final DataService dataService = ServiceLocator.getInstance().getService(dataServiceReference);
            try {
                if (dataService != null) {
                    pairs.add(new GwtGroupedNVPair("cloudStatus", "Auto-connect",
                            dataService.isAutoConnectEnabled()
                                    ? "ON (Retry Interval is " + Integer.toString(dataService.getRetryInterval()) + "s)"
                                    : "OFF"));
                }

                final String dataTransportRef = (String) dataServiceReference.getProperty(
                        DATA_TRANSPORT_SERVICE_REFERENCE_NAME + ComponentConstants.REFERENCE_TARGET_SUFFIX);
                final List<DataTransportService> dataTransportServices = ServiceLocator.getInstance()
                        .getServices(DataTransportService.class, dataTransportRef);
                for (DataTransportService dataTransportService : dataTransportServices) {
                    pairs.add(new GwtGroupedNVPair("cloudStatus", "Broker URL", dataTransportService.getBrokerUrl()));
                    pairs.add(new GwtGroupedNVPair("cloudStatus", "Account", dataTransportService.getAccountName()));
                    pairs.add(new GwtGroupedNVPair("cloudStatus", "Username", dataTransportService.getUsername()));
                    pairs.add(new GwtGroupedNVPair("cloudStatus", "Client ID", dataTransportService.getClientId()));
                }
            } finally {
                ServiceLocator.getInstance().ungetService(dataServiceReference);
            }
        }
    }

    private List<GwtGroupedNVPair> getNetworkStatus() throws GwtKuraException {
        List<GwtGroupedNVPair> pairs = new ArrayList<GwtGroupedNVPair>();
        String nl = "<br />";
        String tab = "&nbsp&nbsp&nbsp&nbsp";

        GwtNetworkServiceImpl gwtNetworkService = new GwtNetworkServiceImpl();

        try {
            List<GwtNetInterfaceConfig> gwtNetInterfaceConfigs = gwtNetworkService.findNetInterfaceConfigurations();
            if (gwtNetInterfaceConfigs == null) {
                return Collections.emptyList();
            }

            for (GwtNetInterfaceConfig gwtNetInterfaceConfig : gwtNetInterfaceConfigs) {

                String currentAddress = gwtNetInterfaceConfig.getIpAddress();
                String currentSubnetMask = gwtNetInterfaceConfig.getSubnetMask();
                String currentStatus = gwtNetInterfaceConfig.getStatusEnum() == GwtNetIfStatus.netIPv4StatusDisabled
                        ? "Disabled"
                        : gwtNetInterfaceConfig.getStatusEnum() == GwtNetIfStatus.netIPv4StatusEnabledLAN ? "LAN"
                                : "WAN";
                String currentConfigMode = gwtNetInterfaceConfig
                        .getConfigModeEnum() == GwtNetIfConfigMode.netIPv4ConfigModeDHCP ? "DHCP" : "Manual";
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
                    if (currentStatus.equals("Disabled")) {
                        pairs.add(new GwtGroupedNVPair("networkStatusEthernet", gwtNetInterfaceConfig.getName(),
                                currentStatus));
                    } else {
                        pairs.add(new GwtGroupedNVPair("networkStatusEthernet", gwtNetInterfaceConfig.getName(),
                                currentAddress + nl + tab + "Subnet Mask: " + currentSubnetMask + nl + tab + "Mode: "
                                        + currentStatus + nl + tab + "IP Acquisition: " + currentConfigMode + nl + tab
                                        + "Router Mode: " + currentRouterMode));
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
                    if (currentStatus.equals("Disabled")) {
                        pairs.add(new GwtGroupedNVPair("networkStatusWifi", gwtNetInterfaceConfig.getName(),
                                currentStatus));
                    } else {
                        pairs.add(new GwtGroupedNVPair("networkStatusWifi", gwtNetInterfaceConfig.getName(),
                                currentAddress + nl + tab + "Subnet Mask: " + currentSubnetMask + nl + tab + "Mode: "
                                        + currentStatus + nl + tab + "IP Acquisition: " + currentConfigMode + nl + tab
                                        + "Router Mode: " + currentRouterMode + nl + tab + "Wireless Mode:"
                                        + currentWifiMode + nl + tab + "SSID: " + currentWifiSsid + nl));
                    }
                } else if (gwtNetInterfaceConfig.getHwTypeEnum() == GwtNetIfType.MODEM) {
                    String currentModemApn = ((GwtModemInterfaceConfig) gwtNetInterfaceConfig).getApn();
                    String currentModemPppNum = Integer
                            .toString(((GwtModemInterfaceConfig) gwtNetInterfaceConfig).getPppNum());
                    if (currentStatus.equals("Disabled")) {
                        pairs.add(new GwtGroupedNVPair("networkStatusModem", gwtNetInterfaceConfig.getName(),
                                currentStatus));
                    } else {
                        pairs.add(new GwtGroupedNVPair("networkStatusModem", gwtNetInterfaceConfig.getName(),
                                currentAddress + nl + "Subnet Mask: " + currentSubnetMask + nl + tab + "Mode: "
                                        + currentStatus + nl + tab + "IP Acquisition: " + currentConfigMode + nl + tab
                                        + "APN: " + currentModemApn + nl + tab + "PPP: " + currentModemPppNum));
                    }
                }
            }
        } catch (GwtKuraException e) {
            s_logger.warn("Get network status failed", e);
            throw e;
        }

        return pairs;
    }

    private List<GwtGroupedNVPair> getPositionStatus() {
        final List<GwtGroupedNVPair> pairs = new ArrayList<GwtGroupedNVPair>();

        ServiceLocator.withOptionalService(PositionService.class, new ServiceFunction<PositionService, Void>() {

            @Override
            public Void apply(PositionService positionService) {
                if (positionService != null) {
                    pairs.add(new GwtGroupedNVPair("positionStatus", "Longitude",
                            Double.toString(Math.toDegrees(positionService.getPosition().getLongitude().getValue()))));
                    pairs.add(new GwtGroupedNVPair("positionStatus", "Latitude",
                            Double.toString(Math.toDegrees(positionService.getPosition().getLatitude().getValue()))));
                    pairs.add(new GwtGroupedNVPair("positionStatus", "Altitude",
                            positionService.getPosition().getAltitude().toString()));
                }
                return null;
            }

        });

        return pairs;
    }

    private void sortCloudServiceServiceReferences(List<ServiceReference<CloudService>> configs) {
        Collections.sort(configs, new Comparator<ServiceReference<CloudService>>() {

            @Override
            public int compare(ServiceReference<CloudService> arg0, ServiceReference<CloudService> arg1) {
                Object object0 = arg0.getProperty(KURA_SERVICE_PID);
                Object object1 = arg1.getProperty(KURA_SERVICE_PID);
                if (object0 != null && object1 != null) {
                    String name0 = (String) object0;
                    String name1 = (String) object1;
                    return name0.compareTo(name1);
                }
                return 0;
            }
        });
    }
}